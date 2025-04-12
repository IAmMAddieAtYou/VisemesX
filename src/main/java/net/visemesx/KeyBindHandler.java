package net.visemesx;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.visemesx.gui.VisemesXGUI;

public class KeyBindHandler {
    public static KeyBinding OPEN_GUI;
    public static KeyBinding PushtoTalk;
    public static void registerKeybinds() {
        OPEN_GUI = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open Menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "VisemesX Misc"
        ));

        PushtoTalk = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Push to Talk",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                "VisemesX Misc"
        ));



        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUI.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new VisemesXGUI());
            }


            VisemesX.getInstance().getPhonemeProcessor().talking = PushtoTalk.isPressed();
        });
    }
}