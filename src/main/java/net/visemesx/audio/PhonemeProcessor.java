package net.visemesx.audio;

import net.minecraft.client.MinecraftClient;


import net.visemesx.VisemesX;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.geometry.partitioning.Transform;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.jtransforms.fft.DoubleFFT_1D;


import javax.sound.sampled.AudioFormat;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.*;

public class PhonemeProcessor {

    public static double micsensitivity = 0.97;
    public static boolean talking = false;
    public static void processAudioChunk(byte[] audioChunk, AudioFormat format) throws IOException {
        if (audioChunk.length == 0) {
            System.out.println("[VisemeDetector] No audio data to process.");
            return;
        }
        if (isSilence(audioChunk, format)) {

           // System.out.println("[VisemeDetector] DEBUGCONVERT");
            double[] audioData = convertToDoubleArray(audioChunk, format);

          //  System.out.println("[VisemeDetector] DEBUGNOIST");
            audioData = applyNoiseReduction(audioData);

           // System.out.println("[VisemeDetector] DEBUGANALYZE");
            analyzeFrequencies(audioData, format, audioChunk);

           // System.out.println("[VisemeDetector] TESTAFTER");
            //System.out.println("[VisemeDetector] FOUND AUDIO data to process.");
            //VisemeMapper.updateViseme("o",MinecraftClient.getInstance().player);
        } else {
            //System.out.println("[VisemeDetector] SET TO NORMAL audio data to process.");
            if (!Objects.equals(VisemesX.getInstance().getVisemeMapper().currentviseme, "closed")) {
                VisemesX.getInstance().getVisemeMapper().updateViseme("normall", MinecraftClient.getInstance().player);
            }
        }
    }

    private static double[] convertToDoubleArray(byte[] audioChunk, AudioFormat format) {
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
        int numSamples = audioChunk.length / sampleSizeInBytes;
        double[] audioData = new double[numSamples];

        for (int i = 0; i < numSamples; i++) {
            double sample = 0;
            for (int j = 0; j < sampleSizeInBytes; j++) {
                int byteValue = audioChunk[i * sampleSizeInBytes + j] & 0xFF;
                if (format.isBigEndian()) {
                    sample = ((int) sample << 8) | byteValue;
                } else {
                    sample = (int) sample | byteValue << (8 * j);
                }
            }

            if (format.getSampleSizeInBits() == 16) {
                sample /= 32768.0;
            } else if (format.getSampleSizeInBits() == 8) {
                sample /= 128.0;
                sample -= 1.0;
            }
            audioData[i] = sample;
        }
        return audioData;
    }


    public static boolean isSilence(byte[] audioData, AudioFormat format) {

        if(VisemesX.getInstance().pushtotalk){
            System.out.println("[VisemeDetector] PTT" );
            if(talking){
                return true;
            } else {
                return false;
            }

        }
        if (audioData == null || audioData.length == 0 || format == null) {
            return false; // Consider empty or null data as silence
        }

        double[] normalizedSamples = convertToDoubleArray(audioData, format);
        double rms = 0;
        for (double sample : normalizedSamples) {
            rms += sample * sample;
        }
        rms = Math.sqrt(rms / normalizedSamples.length);
        rms /= 2;
        // Define a threshold for silence. This value might need adjustment based on your environment.

        System.out.println("[VisemeDetector] RMS = " + rms );
        if(rms >= micsensitivity){

            return false;
        }

        if (containsSpeechFrequencies(normalizedSamples, format)) {
            return true; // Likely speech (not silence)
        }
        return false;
    }

    private static boolean containsSpeechFrequencies(double[] samples, AudioFormat format) {
        int numSamples = samples.length;
        if (numSamples < 2) return false;

        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] fftResult = fft.transform(samples, TransformType.FORWARD);

        double sampleRate = format.getSampleRate();
        double frequencyResolution = sampleRate / numSamples;
        double energyInSpeechRange = 0;

        float lowerSpeechFrequency = 300;
        float upperSpeechFrequency = 3000;
        int startIndex = (int) Math.round(lowerSpeechFrequency / frequencyResolution);
        int endIndex = (int) Math.round(upperSpeechFrequency / frequencyResolution);

        int spectrumSize = numSamples;

        for (int i = startIndex; i <= Math.min(endIndex, spectrumSize); i++) {
            if (i > 0) {
                double magnitudeSquared = fftResult[i].abs(); // Get the squared magnitude (power)
                energyInSpeechRange += magnitudeSquared;
            }
        }
        System.out.println("[VisemeDetector] ENERGY = " + energyInSpeechRange + "SPEECH THRESHOLD " +550 );
        // Increase the threshold significantly
        return energyInSpeechRange > 550; // Increased threshold
    }

    private static double calculateRMS(double[] samples) {
        if (samples == null || samples.length == 0) {
            return 0;
        }

        double sum = 0;
        for (double sample : samples) {
            sum += sample * sample;
        }
        return Math.sqrt(sum / samples.length);
    }

    private static double[] applyNoiseReduction(double[] audioData) {
        double[] smoothed = new double[audioData.length];
        int smoothRange = 3; // Simple moving average for smoothing

        for (int i = 0; i < audioData.length; i++) {
            double sum = 0;
            int count = 0;
            for (int j = -smoothRange; j <= smoothRange; j++) {
                if (i + j >= 0 && i + j < audioData.length) {
                    sum += audioData[i + j];
                    count++;
                }
            }
            smoothed[i] = sum / count;
        }
        return smoothed;
    }

    private static int getFormantFrequency(double[] fftData, int minFreq, int maxFreq, AudioFormat format) {
        int indexMin = (int) (minFreq * fftData.length / format.getSampleRate());
        int indexMax = (int) (maxFreq * fftData.length / format.getSampleRate());
        int maxIndex = indexMin;
        double maxAmplitude = 0;

        for (int i = indexMin; i < indexMax; i++) {
            double amplitude = Math.sqrt(fftData[2 * i] * fftData[2 * i] + fftData[2 * i + 1] * fftData[2 * i + 1]);
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude;
                maxIndex = i;
            }
        }

        return (maxIndex * (int) format.getSampleRate()) / fftData.length;
    }

    public static String classifyVowel(Map<String, Integer> phonemeProbabilities) {
        if (phonemeProbabilities == null || phonemeProbabilities.isEmpty()) {
            return null;
        }

        String bestPhoneme = null;
        int highestProbability = -1;

        for (Map.Entry<String, Integer> entry : phonemeProbabilities.entrySet()) {
            String phoneme = entry.getKey();
            int probability = entry.getValue();

            if (probability > highestProbability) {
                highestProbability = probability;
                bestPhoneme = phoneme;
            }
        }

        return bestPhoneme;
    }


    private static void analyzeFrequencies(double[] audioData, AudioFormat format, byte[] audiob) {
        int frameSize = audioData.length;

        //System.out.println("[VisemeDetector] TESTSTART");
        // Apply Hamming Window
        double[] windowedData = applyHammingWindow(audioData);
        DoubleFFT_1D fft = new DoubleFFT_1D(frameSize);
        double[] fftData = new double[frameSize * 2];
        System.arraycopy(windowedData, 0, fftData, 0, frameSize);
        fft.realForward(fftData);

        // Extract dominant frequencies (formants)

        //System.out.println("[VisemeDetector] TEST3");
        // 1. Power Spectrum
        double[] powerSpectrum = new double[frameSize / 2];
        for (int i = 0; i < powerSpectrum.length; i++) {
            double real = fftData[i * 2];
            double imag = fftData[i * 2 + 1];
            powerSpectrum[i] = real * real + imag * imag; // Power spectrum
        }

        //System.out.println("[VisemeDetector] TEST3");
        // 2. Autocorrelation
        int lpcOrder = 12; // Adjust as needed
        double[] autocorrelation = calculateAutocorrelation(powerSpectrum, lpcOrder);

        //System.out.println("[VisemeDetector] TEST3");
        // 3. Levinson-Durbin
        double[] lpcCoefficients = levinsonDurbin(autocorrelation);

        // System.out.println("[VisemeDetector] TEST3");
        // 4. Root Finding and Formant Extraction
        Complex[] roots = ComplexRootFinder.findRoots(lpcCoefficients);
        if (roots != null) {
            //    System.out.println("[VisemeDetector] TEST3");
            double sampleRate = format.getSampleRate();
            double[] formantFrequencies = new double[roots.length];

            //   System.out.println("[VisemeDetector] TEST3");
            for (int i = 0; i < roots.length; i++) {
                double real = roots[i].getReal();
                double imag = roots[i].getImaginary();
                double angle = Math.atan2(imag, real);
                formantFrequencies[i] = (sampleRate / (2 * Math.PI)) * angle;
            }

            //  System.out.println("[VisemeDetector] TEST3");
            // Filter Formant Frequencies (Keep only positive frequencies)
            double[] positiveFormants = Arrays.stream(formantFrequencies)
                    .filter(f -> f > 0)
                    .toArray();

            //  System.out.println("[VisemeDetector] TEST3");
            // Sort the positive formant frequencies
            Arrays.sort(positiveFormants);

            int f1 = 0;
            int f2 = 0;
            int f3 = 0;
            int f4 = 0;

            // System.out.println("[VisemeDetector] TEST3");

            // Get F1 and F2 (if available)
            if (positiveFormants.length > 0) {
                f1 = (int) positiveFormants[0]; // First positive formant
            }

            //  System.out.println("[VisemeDetector] TEST3");
            if (positiveFormants.length > 1) {
                f2 = (int) positiveFormants[1]; // Second positive formant
            }

            // System.out.println("[VisemeDetector] TEST3");
            if (positiveFormants.length > 2) {
                f3 = (int) positiveFormants[2]; // Second positive formant
            }
            // System.out.println("[VisemeDetector] TEST3");
            if (positiveFormants.length > 3) {
                f3 = (int) positiveFormants[2]; // Second positive formant
            }

            //  System.out.println("[VisemeDetector] F1 " + f1 + ".");
            // System.out.println("[VisemeDetector] F2 " + f2 + ".");

            Map<String, Integer> probabilities_i = deducePhonemeProbability(f1, f2, f3, f4, audioData, format);
            // System.out.println("[VisemeDetector] TEST");
            System.out.println("Probabilities for F1=" + f1 + ", F2=" + f2 + ", F3=" + f3 + ", F4=" + f4 +  ": " + probabilities_i);
            String vowel = classifyVowel(probabilities_i);
             System.out.println("[VisemeDetector] Vowel" + vowel + ".");
            if (!vowel.equals("Unknown / Noise Detected")) {
                VisemesX.getInstance().getVisemeMapper().updateViseme(vowel.toLowerCase(), MinecraftClient.getInstance().player);
            }
            if (vowel.equals("Unknown / Noise Detected")) {
                VisemesX.getInstance().getVisemeMapper().updateViseme("normall", MinecraftClient.getInstance().player);
            }

            System.out.println("[VisemeDetector] TESTDEDUCE");
        } else {
            VisemesX.getInstance().getVisemeMapper().updateViseme("normall", MinecraftClient.getInstance().player);

        }

    }

    public static Map<String, Integer> deducePhonemeProbability(int F1, int F2, int F3, int F4, double[] audioData, AudioFormat format) {
        Map<String, Integer> probabilities = new HashMap<>();

        // Define very basic and rough formant ranges (in Hz) for the phonemes, including F3 and F4.
        // These are highly simplified and for demonstration purposes only.
        // Real formant analysis is much more complex and context-dependent.

        // Phoneme I (as in "bee")
        int f1_i_low = 850;
        int f1_i_high = 940;
        int f2_i_low = 2300;
        int f2_i_high = 2600;
        int f3_i_low = 3600;
        int f3_i_high = 3800; // Hypothetical
        int f4_i_low = 3500;
        int f4_i_high = 4200; // Hypothetical

        // Phoneme A (as in "father")
        int f1_a_low = 930;
        int f1_a_high = 850;
        int f2_a_low = 2500;
        int f2_a_high = 2700;
        int f3_a_low = 3300;
        int f3_a_high = 4200; // Hypothetical
        int f4_a_low = 3300;
        int f4_a_high = 3900; // Hypothetical

        // Phoneme O (as in "boat")
        int f1_o_low = 1280;
        int f1_o_high = 1500;
        int f2_o_low = 1400;
        int f2_o_high = 1600;
        int f3_o_low = 3800;
        int f3_o_high = 4500; // Hypothetical
        int f4_o_low = 3300;
        int f4_o_high = 3600; // Hypothetical

        // Phoneme OW (as in "go") - Treating similar to O for simplicity
        int f1_ow_low = 1280;
        int f1_ow_high = 1500;
        int f2_ow_low = 1400;
        int f2_ow_high = 1600;
        int f3_ow_low = 3800;
        int f3_ow_high = 4500; // Hypothetical
        int f4_ow_low = 3100;
        int f4_ow_high = 3500; // Hypothetical

        // Phoneme R (as in "red") - F3 is typically very low for R, but we'll use F1 and F2 more heavily here
        int f1_r_low = 840;
        int f1_r_high = 1300;
        int f2_r_low = 2000;
        int f2_r_high = 2490;
        int f3_r_low = 4000;
        int f3_r_high = 4400; // Hypothetical - F3 is usually lower
        int f4_r_low = 3400;
        int f4_r_high = 3900; // Hypothetical

        // Phoneme S (as in "sun") - Still primarily noise-based, formants are less relevant


        // Phoneme U (as in "boot")
        int f1_u_low = 700;
        int f1_u_high = 950;
        int f2_u_low = 2500;
        int f2_u_high = 2650;
        int f3_u_low = 3200;
        int f3_u_high = 3850; // Hypothetical
        int f4_u_low = 3400;
        int f4_u_high = 3900; // Hypothetical

        int f1_f_low = 800;
        int f1_f_high = 1000;
        int f2_f_low = 2100;
        int f2_f_high = 2650;
        int f3_f_low = 3600;
        int f3_f_high = 3950; // Hypothetical
        int f4_f_low = 3400;
        int f4_f_high = 3900;

        // Assign probabilities based on how well the input formants match the ranges.
        probabilities.put("I", calculateProbability(F1, F2, F3, F4,"i", f1_i_low, f1_i_high, f2_i_low, f2_i_high, f3_i_low, f3_i_high, f4_i_low, f4_i_high));
        probabilities.put("A", calculateProbability(F1, F2, F3, F4,"a", f1_a_low, f1_a_high, f2_a_low, f2_a_high, f3_a_low, f3_a_high, f4_a_low, f4_a_high));
        probabilities.put("O", calculateProbability(F1, F2, F3, F4, "o",f1_o_low, f1_o_high, f2_o_low, f2_o_high, f3_o_low, f3_o_high, f4_o_low, f4_o_high));
        probabilities.put("OW", calculateProbability(F1, F2, F3, F4, "ow",f1_ow_low, f1_ow_high, f2_ow_low, f2_ow_high, f3_ow_low, f3_ow_high, f4_ow_low, f4_ow_high));
        probabilities.put("R", calculateProbability(F1, F2, F3, F4, "r",f1_r_low, f1_r_high, f2_r_low, f2_r_high, f3_r_low, f3_r_high, f4_r_low, f4_r_high));
        probabilities.put("F", calculateProbability(F1, F2, F3, F4, "f",f1_f_low, f1_f_high, f2_f_low, f2_f_high, f3_f_low, f3_f_high, f4_f_low, f4_f_high));
        // Lower probability for 'S'
        probabilities.put("U", calculateProbability(F1, F2, F3, F4, "u",f1_u_low, f1_u_high, f2_u_low, f2_u_high, f3_u_low, f3_u_high, f4_u_low, f4_u_high));
        int s_probability = 0;
        if (audioData != null && audioData.length > 0 && format != null) {
            double sampleRate = format.getSampleRate();
            FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
            Complex[] fftResult = ((FastFourierTransformer) fft).transform(audioData, TransformType.FORWARD);
            int spectrumLength = fftResult.length / 2;
            double highFrequencyEnergy = 0;
            double totalEnergy = 0;
            int highFrequencyStartBin = (int) (4000.0 / (sampleRate / audioData.length));

            for (int i = 1; i < spectrumLength; i++) {
                double magnitude = fftResult[i].abs();
                totalEnergy += magnitude;
                if (i >= highFrequencyStartBin) {
                    highFrequencyEnergy += magnitude;
                }
            }

            if (totalEnergy > 0 && highFrequencyEnergy / totalEnergy > 0.6) {
                s_probability = Math.max(s_probability, 80);
            }
        }
        // No formant-based fallback for 'S' as per user request

        String lastVowel = VisemesX.getInstance().getVisemeMapper().currentvowel;
        double sMultiplier = getVowelTransitionMultiplier(lastVowel, "s");
        probabilities.put("S", (int) (s_probability * sMultiplier));

        return probabilities;
    }

    private static double getVowelTransitionMultiplier(String lastVowel, String currentVowel) {
        double multiplier = 1.0;
        if (lastVowel == null || currentVowel == null) {
            return multiplier;
        }

        switch (lastVowel) {
            case "ee":
                if (currentVowel.equals("i")) multiplier = 1.15; // Slight shift in vowel
                else if (currentVowel.equals("ee")) multiplier = 1.25; // Sustained sound
                else if (currentVowel.equals("a")) multiplier = 1.0; // More distinct vowel change
                else if (currentVowel.equals("r")) multiplier = 1.2; // Common "eer" sound
                else if (currentVowel.equals("s")) multiplier = 1.2; // Common "ees" sound
                else if (currentVowel.equals("f")) multiplier = 1.05; // Less common
                break;
            case "i":
                if (currentVowel.equals("a")) multiplier = 1.15;
                else if (currentVowel.equals("ee")) multiplier = 1.2; // Shift towards longer 'ee'
                else if (currentVowel.equals("i")) multiplier = 1.2; // Sustained sound
                else if (currentVowel.equals("r")) multiplier = 1.1; // "ir" sound
                else if (currentVowel.equals("s")) multiplier = 1.15; // "is" sound
                else if (currentVowel.equals("f")) multiplier = 1.05;
                break;
            case "o":
                if (currentVowel.equals("o")) multiplier = 1.35; // Sustained sound
                else if (currentVowel.equals("a")) multiplier = 1.05;
                else if (currentVowel.equals("u")) multiplier = 1.15; // Similar rounded vowel
                else if (currentVowel.equals("r")) multiplier = 1.15; // "or" sound
                else if (currentVowel.equals("s")) multiplier = 1.1; // "os" sound
                else if (currentVowel.equals("f")) multiplier = 1.05;
                break;
            case "ow":
                if (currentVowel.equals("o")) multiplier = 1.2; // Moving towards the core vowel
                else if (currentVowel.equals("r")) multiplier = 1.0; // Less common
                else if (currentVowel.equals("s")) multiplier = 1.1; // "ows" sound
                else if (currentVowel.equals("f")) multiplier = 1.05;
                break;
            case "a":
                if (currentVowel.equals("i")) multiplier = 1.1;
                else if (currentVowel.equals("o")) multiplier = 1.1;
                else if (currentVowel.equals("u")) multiplier = 1.1;
                else if (currentVowel.equals("a")) multiplier = 1.2; // Sustained sound
                else if (currentVowel.equals("r")) multiplier = 1.25; // Common "ar" sound
                else if (currentVowel.equals("s")) multiplier = 1.15; // "as" sound
                else if (currentVowel.equals("f")) multiplier = 1.05;
                break;
            case "u":
                if (currentVowel.equals("i")) multiplier = 1.1;
                else if (currentVowel.equals("u")) multiplier = 1.2; // Sustained sound
                else if (currentVowel.equals("a")) multiplier = 1.05;
                else if (currentVowel.equals("o")) multiplier = 1.15; // Similar rounded vowel
                else if (currentVowel.equals("r")) multiplier = 1.1; // "ur" sound
                else if (currentVowel.equals("s")) multiplier = 1.1; // "us" sound
                else if (currentVowel.equals("f")) multiplier = 1.05;
                break;
            case "r":
                if (currentVowel.equals("ee") || currentVowel.equals("i")) multiplier = 1.2; // "ree", "ri" sounds
                else if (currentVowel.equals("o") || currentVowel.equals("ow")) multiplier = 1.15; // "ro", "row" sounds
                else if (currentVowel.equals("a")) multiplier = 1.15; // "ra" sound
                else if (currentVowel.equals("u")) multiplier = 1.1; // "ru" sound
                else if (currentVowel.equals("r")) multiplier = 1.25; // Sustained sound
                else if (currentVowel.equals("s")) multiplier = 1.1; // "rs" sound
                else if (currentVowel.equals("f")) multiplier = 1.05;
                break;
            case "s":
                if (currentVowel.equals("ee") || currentVowel.equals("i")) multiplier = 1.2; // "see", "si" sounds
                else if (currentVowel.equals("o") || currentVowel.equals("ow")) multiplier = 1.15; // "so", "sow" sounds
                else if (currentVowel.equals("a")) multiplier = 1.15; // "sa" sound
                else if (currentVowel.equals("u")) multiplier = 1.1; // "su" sound
                else if (currentVowel.equals("r")) multiplier = 1.1; // "sr" sound
                else if (currentVowel.equals("s")) multiplier = 1.25; // Sustained sound
                else if (currentVowel.equals("f")) multiplier = 1.05;
                break;
            case "f":
                if (currentVowel.equals("ee") || currentVowel.equals("i")) multiplier = 1.15; // "fee", "fi" sounds
                else if (currentVowel.equals("o") || currentVowel.equals("ow")) multiplier = 1.1; // "fo", "flow" sounds
                else if (currentVowel.equals("a")) multiplier = 1.1; // "fa" sound
                else if (currentVowel.equals("u")) multiplier = 1.1; // "fu" sound
                else if (currentVowel.equals("r")) multiplier = 1.05;
                else if (currentVowel.equals("s")) multiplier = 1.05;
                else if (currentVowel.equals("f")) multiplier = 1.2; // Sustained sound
                break;
        }
        return multiplier;
    }

    public static int calculateProbability(int f1, int f2, int f3, int f4, String expectedVowel,
                                           int f1Low, int f1High, int f2Low, int f2High,
                                           int f3Low, int f3High, int f4Low, int f4High) {

        String lastVowel = VisemesX.getInstance().getVisemeMapper().currentvowel;

        // Check if the expected vowel is in our list of vowels
        if (!VisemesX.getInstance().getVisemeMapper().phonemeToViseme.containsKey(expectedVowel)) {
            return 0; // Deduct all points if the expected vowel is not in the list
        }

        double totalScore = 0;

        // Check F1
        if (f1 >= f1Low && f1 <= f1High)
            totalScore += 1;
        else if (f1 >= Math.max(f1Low - 100, 0) && f1 <= f1High + 100)
            totalScore += 0.5;

        // Midpoint check for F1
        int f1Mid = (f1Low + f1High) / 2;
        if (Math.abs(f1 - f1Mid) <= (f1High - f1Low) * 0.1)
            totalScore += 0.25;  // Bonus points if close to the midpoint

        // Check F2
        if (f2 >= f2Low && f2 <= f2High)
            totalScore += 1;
        else if (f2 >= Math.max(f2Low - 200, 0) && f2 <= f2High + 200)
            totalScore += 0.5;

        // Midpoint check for F2
        int f2Mid = (f2Low + f2High) / 2;
        if (Math.abs(f2 - f2Mid) <= (f2High - f2Low) * 0.1)
            totalScore += 0.25;  // Bonus points if close to the midpoint

        // Check F3
        if (f3 >= f3Low && f3 <= f3High)
            totalScore += 1;
        else if (f3 >= Math.max(f3Low - 300, 0) && f3 <= f3High + 300)
            totalScore += 0.5;

        // Midpoint check for F3
        int f3Mid = (f3Low + f3High) / 2;
        if (Math.abs(f3 - f3Mid) <= (f3High - f3Low) * 0.1)
            totalScore += 0.25;  // Bonus points if close to the midpoint

        // Check F4
        if (f4 >= f4Low && f4 <= f4High)
            totalScore += 1;
        else if (f4 >= Math.max(f4Low - 400, 0) && f4 <= f4High + 400)
            totalScore += 0.5;

        // Midpoint check for F4
        int f4Mid = (f4Low + f4High) / 2;
        if (Math.abs(f4 - f4Mid) <= (f4High - f4Low) * 0.1)
            totalScore += 0.25;  // Bonus points if close to the midpoint

        // Probability scoring based on total score
        int baseProbability;
        if (totalScore >= 5) baseProbability = 95;
        else if (totalScore >= 4.5) baseProbability = 85;
        else if (totalScore >= 4) baseProbability = 75;
        else if (totalScore >= 3.5) baseProbability = 60;
        else if (totalScore >= 3) baseProbability = 45;
        else if (totalScore >= 2.5) baseProbability = 30;
        else if (totalScore >= 2) baseProbability = 15;
        else if (totalScore >= 1) baseProbability = 5;
        else baseProbability = 0;

        // Get the multiplier based on the transition from the last vowel to the expected vowel
        double multiplier = getVowelTransitionMultiplier(lastVowel, expectedVowel);

        // Apply the multiplier to the base probability
        return (int) (baseProbability * multiplier);
    }

    private static double[] applyHammingWindow(double[] audioData) {
        int frameSize = audioData.length;
        double[] windowedData = new double[frameSize];
        for (int i = 0; i < frameSize; i++) {
            windowedData[i] = audioData[i] * (0.54 - 0.46 * Math.cos(2 * Math.PI * i / (frameSize - 1)));
        }
        return windowedData;
    }

    private static double[] calculateAutocorrelation(double[] powerSpectrum, int order) {
        double[] autocorrelation = new double[order + 1];
        for (int i = 0; i <= order; i++) {
            double sum = 0;
            for (int j = 0; j < powerSpectrum.length - i; j++) {
                sum += powerSpectrum[j] * powerSpectrum[j + i];
            }
            autocorrelation[i] = sum;
        }
        return autocorrelation;
    }

    private static double[] levinsonDurbin(double[] autocorrelation) {
        int order = autocorrelation.length - 1;
        double[] coefficients = new double[order];
        double[] reflectionCoefficients = new double[order];
        double[] tempCoefficients = new double[order];

        double error = autocorrelation[0];
        for (int i = 1; i <= order; i++) {
            double k = 0;
            for (int j = 0; j < i - 1; j++) {
                k -= tempCoefficients[j] * autocorrelation[i - 1 - j];
            }
            k -= autocorrelation[i];
            k /= error;
            reflectionCoefficients[i - 1] = k;

            tempCoefficients[i - 1] = k;
            for (int j = 0; j < (i - 1) / 2; j++) {
                double temp = tempCoefficients[j];
                tempCoefficients[j] += k * tempCoefficients[i - 2 - j];
                tempCoefficients[i - 2 - j] += k * temp;
            }
            error *= (1 - k * k);
        }
        System.arraycopy(tempCoefficients, 0, coefficients, 0, order);
        return coefficients;
    }


    private static String extractPhoneme(String result) {
        return result.replaceAll("[^a-zA-Z]", "").toLowerCase(); // Extract only phonemes
    }
}
