package net.visemesx;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.visemesx.audio.AudioCapture;
import net.visemesx.audio.PhonemeProcessor;
import net.visemesx.audio.VisemeMapper;
import net.visemesx.network.VisemeNetwork;

public class VisemesX implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "visemesx";
    public boolean pushtotalk = false;
    public static final AudioCapture AUDIO_CAPTURE = new AudioCapture();
    public static final VisemeMapper VISEME_MAPPER = new VisemeMapper();
    public static final PhonemeProcessor PHONEME_PROCESSOR = new PhonemeProcessor();

    private static VisemesX instance;

    public VisemesX() {
        instance = this;
    }

    public static VisemesX getInstance() {
        return instance;
    }
    public void restartAudioCapture() {
        AUDIO_CAPTURE.stopCapture();
        AUDIO_CAPTURE.startCapture();
    }

    public AudioCapture getAudioCapture() {
        return AUDIO_CAPTURE;
    }

    public VisemeMapper getVisemeMapper() {
        return VISEME_MAPPER;
    }

    public PhonemeProcessor getPhonemeProcessor() {
        return PHONEME_PROCESSOR;
    }
    @Override
    public void onInitialize() {
        //VisemeNetwork.registerPackets();
        KeyBindHandler.registerKeybinds();

        System.out.println("[VisemesX] Network packets registered.");
    }

    @Override
    public void onInitializeClient() {

        KeyBindHandler.registerKeybinds();
        System.out.println("[VisemesX] Client initialization.");
    }
}