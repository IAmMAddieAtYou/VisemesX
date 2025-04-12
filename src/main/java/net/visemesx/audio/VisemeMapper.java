package net.visemesx.audio;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.visemesx.network.VisemeNetwork;

import java.util.HashMap;
import java.util.Map;

public class VisemeMapper {
    public static final Map<String, String> phonemeToViseme = new HashMap<>();
    public String currentviseme = "default";
    public String currentvowel = "default";
    static {
        phonemeToViseme.put("p", "closed");
        phonemeToViseme.put("b", "closed");
        phonemeToViseme.put("m", "closed");
        phonemeToViseme.put("r", "teeth");
        phonemeToViseme.put("v", "teeth");
        phonemeToViseme.put("th", "tongue");
        phonemeToViseme.put("t", "tongue");
        phonemeToViseme.put("d", "tongue");
        phonemeToViseme.put("s", "teeth");
        phonemeToViseme.put("z", "teeth");
        phonemeToViseme.put("ee", "a");
        phonemeToViseme.put("i", "a");
        phonemeToViseme.put("o", "round");
        phonemeToViseme.put("ow", "round");
        phonemeToViseme.put("a", "open");
        phonemeToViseme.put("u", "round");
        phonemeToViseme.put("f", "teeth");
    }

    public void updateViseme(String phoneme, ClientPlayerEntity player) {
        this.currentvowel = phoneme;
        String viseme = phonemeToViseme.getOrDefault(phoneme, "closed");
        this.currentviseme = viseme;
        //System.out.println("[VisemesX] Mapping phoneme '" + phoneme + "' to viseme '" + viseme + "'.");
        if (player != null) {
            //PacketByteBuf buf = PacketByteBufs.create();
            //buf.writeString(viseme);
            //VisemeNetwork.sendExpressionUpdate(buf);
        }
    }
}
