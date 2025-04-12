package net.visemesx.audio;

import net.visemesx.VisemesX;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioCapture {
    private TargetDataLine microphone;
    public volatile boolean capturing = false;
    private AudioFormat microphoneFormat;
    public int selectedindex;


    public java.util.List<String> getCaptureDeviceNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixerInfos) {
            if (mixerInfo.getName().toLowerCase().contains("capture") || mixerInfo.getName().toLowerCase().contains("mic") || mixerInfo.getName().toLowerCase().contains("microphone") || mixerInfo.getName().toLowerCase().contains("input")) {
                names.add(mixerInfo.getName());
            }
        }
        return names;
    }
    public int getCaptureDeviceIndex(String name) {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        int index = 0;
        int captureIndex = 0;
        for (Mixer.Info mixerInfo : mixerInfos) {
            if (mixerInfo.getName().toLowerCase().contains("capture") || mixerInfo.getName().toLowerCase().contains("mic") || mixerInfo.getName().toLowerCase().contains("microphone") || mixerInfo.getName().toLowerCase().contains("input")) {
                if (mixerInfo.getName().equals(name)) {
                    return captureIndex;
                }
                captureIndex++;
            }
            index++;
        }
        return -1; // Device not found
    }
    public void startCapture() {
        int micIndex = selectedindex;
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        System.out.println("Available Mixers:");
        java.util.List<Mixer.Info> captureMixers = new java.util.ArrayList<>();
        for (int i = 0; i < mixerInfos.length; i++) {
            if (mixerInfos[i].getName().toLowerCase().contains("capture") ||
                    mixerInfos[i].getName().toLowerCase().contains("mic") ||
                    mixerInfos[i].getName().toLowerCase().contains("microphone") ||
                    mixerInfos[i].getName().toLowerCase().contains("input")) {
                captureMixers.add(mixerInfos[i]);
                System.out.println(i + ": " + mixerInfos[i].getName() + " - " + mixerInfos[i].getDescription());
            }
        }

        if (captureMixers.isEmpty()) {
            System.out.println("No suitable audio capture devices found.");
            return;
        }

        if (micIndex < 0 || micIndex >= captureMixers.size()) {
            System.out.println("Invalid microphone index provided: " + micIndex + " for " + captureMixers.size() + " capture devices.");
            System.out.println("Using index 0 of capture devices.");
            micIndex = 0;
        }

        Mixer.Info selectedMixerInfo = captureMixers.get(micIndex);
        Mixer selectedMixer = null;
        try {
            selectedMixer = AudioSystem.getMixer(selectedMixerInfo);
            selectedMixer.open(); // Open the mixer to check its lines
        } catch (LineUnavailableException e) {
            System.err.println("Error opening mixer: " + selectedMixerInfo.getName() + ". Please check if it's available.");
            e.printStackTrace();
            return;
        }

        System.out.println("Selected Mixer: " + selectedMixerInfo.getName());

        Line.Info[] targetLineInfos = selectedMixer.getTargetLineInfo();
        if (targetLineInfos.length == 0) {
            System.out.println("No target lines available on the selected mixer.");
            if (selectedMixer.isOpen()) {
                selectedMixer.close();
            }
            return;
        }

        for (Line.Info lineInfo : targetLineInfos) {
            System.out.println("  Line Info: " + lineInfo);

            if (lineInfo instanceof DataLine.Info) {
                DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                AudioFormat[] supportedFormats = dataLineInfo.getFormats();
                System.out.println("    Supported Formats: " + Arrays.toString(supportedFormats));

                microphoneFormat = null;

                // Define the expected format: 16000 Hz, 16-bit, mono, signed PCM
                AudioFormat expectedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000.0f, 16, 1, 2, 16000.0f, false);

                // First, try to find the explicitly expected format
                for (AudioFormat format : supportedFormats) {
                    if (format.matches(expectedFormat)) {
                        microphoneFormat = format;
                        System.out.println("    Found expected format in supported formats: " + microphoneFormat);
                        break;
                    }
                }

                // If the expected format wasn't found, then try the user's specified format (48000 Hz)
                if (microphoneFormat == null) {
                    AudioFormat userSpecifiedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000.0f, 16, 1, 2, 48000.0f, false);
                    for (AudioFormat format : supportedFormats) {
                        if (format.matches(userSpecifiedFormat)) {
                            microphoneFormat = format;
                            System.out.println("    Found user-specified format (48000 Hz) in supported formats: " + microphoneFormat);
                            break;
                        }
                    }
                }

                // If neither was found, try to find any 16-bit mono signed PCM format and default to 16000 Hz if rate is flexible
                if (microphoneFormat == null) {
                    for (AudioFormat format : supportedFormats) {
                        if (format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED &&
                                format.getSampleSizeInBits() == 16 &&
                                format.getChannels() == 1) {
                            microphoneFormat = format;
                            System.out.println("    Found a compatible 16-bit mono signed PCM format: " + microphoneFormat);
                            if (microphoneFormat.getFrameRate() == AudioSystem.NOT_SPECIFIED) {
                                System.out.println("    Frame rate is not specified, defaulting to 16000 Hz.");
                                microphoneFormat = new AudioFormat(microphoneFormat.getEncoding(), 16000.0f,
                                        microphoneFormat.getSampleSizeInBits(), microphoneFormat.getChannels(),
                                        microphoneFormat.getFrameSize(), 16000.0f, microphoneFormat.isBigEndian());
                                System.out.println("    Trying format with forced 16000 Hz: " + microphoneFormat);
                            }
                            break; // Found a suitable format, no need to check further
                        }
                    }
                }

                // Last resort: If no specific format matches, try the first supported format if available and log a warning
                if (microphoneFormat == null && supportedFormats.length > 0) {
                    microphoneFormat = supportedFormats[0];
                    System.out.println("    Warning: Using the first supported format as no preferred format was found: " + microphoneFormat);
                    // Consider if any adjustments (like forcing sample rate) are needed here based on your application's requirements.
                    // Forcing blindly might lead to unexpected behavior if the format is significantly different.
                    if (microphoneFormat.getFrameRate() == AudioSystem.NOT_SPECIFIED) {
                        System.out.println("    Frame rate of the first supported format is not specified, defaulting to 16000 Hz.");
                        microphoneFormat = new AudioFormat(microphoneFormat.getEncoding(), 16000.0f,
                                microphoneFormat.getSampleSizeInBits(), microphoneFormat.getChannels(),
                                microphoneFormat.getFrameSize(), 16000.0f, microphoneFormat.isBigEndian());
                        System.out.println("    Trying format with forced 16000 Hz: " + microphoneFormat);
                    }
                } else if (microphoneFormat == null) {
                    System.out.println("    Error: No suitable audio format found for the microphone on this line.");
                    continue; // Skip to the next line info
                }

                if (microphoneFormat != null) {
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, microphoneFormat);

                    if (selectedMixer.isLineSupported(info)) {
                        try {
                            microphone = (TargetDataLine) selectedMixer.getLine(info);
                            microphone.open(microphoneFormat);
                            microphone.start();
                            this.capturing = true;
                            System.out.println("    Successfully opened microphone with format: " + microphoneFormat);
                            
                            CompletableFuture.runAsync(this::processAudio);


                        } catch (LineUnavailableException e) {
                            System.err.println("    Error: Line unavailable for format: " + microphoneFormat + " on mixer " + selectedMixerInfo.getName() + ". Please check if another application is using it.");
                            e.printStackTrace();
                        } catch (SecurityException e) {
                            System.err.println("    Error: Security exception while accessing the microphone: " + e.getMessage());
                            e.printStackTrace();
                        } catch (IllegalArgumentException e) {
                            System.err.println("    Error: Illegal argument exception while opening the microphone with format: " + microphoneFormat + ". This might indicate an issue with the format or the audio system.");
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("    Error: Line not supported by the selected mixer for format: " + microphoneFormat);
                    }
                }
            }
        }


        //PhonemeProcessor.processAudioChunk(null, microphoneFormat);

        //System.out.println("No suitable TargetDataLine found in selected mixer.");

    }

    public void stopCapture() {
        capturing = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
    }

    private void processAudio() {

        byte[] buffer = new byte[256];
        // Capture audio until stopped
        if(microphone.isRunning()){
            microphone.stop();
            microphone.start();
        } else{
            microphone.start();
        }
        while (this.capturing) {

            int bytesRead = microphone.read(buffer, 0, buffer.length);

            if (bytesRead > 0) {
                try {
                    //System.err.println("SENDING OVER");
                    byte[] convertedBuffer = convertTo16Bit16kHzMono(buffer, bytesRead, microphoneFormat);
                    //System.err.println("EPIC");
                    VisemesX.getInstance().getPhonemeProcessor().processAudioChunk(convertedBuffer,microphoneFormat);
                } catch (java.io.IOException ignored) {
                    System.err.println("IO EXCEPTION failed." + ignored.toString());
                }
            }
        }
    }

    private byte[] convertTo16Bit16kHzMono(byte[] inputBuffer, int bytesRead, AudioFormat inputFormat) {
        AudioFormat targetFormat = new AudioFormat(16000, 16, 1, true, false);
        AudioInputStream inputStream = new AudioInputStream(
                new java.io.ByteArrayInputStream(inputBuffer, 0, bytesRead),
                inputFormat,
                bytesRead / inputFormat.getFrameSize());

        AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, inputStream);

        if (convertedStream == null) {
            System.err.println("Conversion failed.");
            return new byte[0];
        }

        byte[] convertedBuffer = new byte[bytesRead * 2]; // Assuming 16 bit conversion, double the size
        try {
            int convertedBytesRead = convertedStream.read(convertedBuffer);
            if (convertedBytesRead < 0) return new byte[0];
            return convertedBuffer;
        } catch (java.io.IOException e) {
            System.err.println(e.toString());
            return new byte[0];
        }
    }
}


