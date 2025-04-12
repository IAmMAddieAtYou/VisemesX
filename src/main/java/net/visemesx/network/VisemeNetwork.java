package net.visemesx.network;
import net.minecraft.network.packet.CustomPayload.Id;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

public class VisemeNetwork {
    private static final Identifier EXPRESSION_SYNC_PACKET = Identifier.of("visemesx", "expression_sync");



    public static void registerPackets() {
        // Register the global receiver for the packet
        Id<CustomPayload> id = new Id<>(EXPRESSION_SYNC_PACKET);
        ServerPlayNetworking.registerGlobalReceiver(id, (buf, context) -> {
            String viseme = buf.toString(); // Read the string payload from the buffer
            System.out.println("[VisemesX] Received viseme: " + viseme);

            // Modify the world on the server thread (must execute on the server thread)
            context.server().execute(() -> {
                broadcastExpressionUpdate(viseme, (ServerPlayerEntity) context.player());
            });
        });

        // Confirm the packet registration
        System.out.println("[VisemesX] Expression sync packet registered.");
    }



    private static void broadcastExpressionUpdate(String viseme, ServerPlayerEntity sender) {
        PacketByteBuf buf = new PacketByteBuf(PacketByteBufs.create());
        buf.writeString(viseme);

        // Broadcast the viseme to all players connected to the server
        for (ServerPlayerEntity player : sender.getServer().getPlayerManager().getPlayerList()) {
            System.out.println("[VisemesX] Broadcasting viseme: " + viseme);
            ServerPlayNetworking.send(player, (CustomPayload) buf);
        }
    }

    public static void sendExpressionUpdate(PacketByteBuf buf) {
        System.out.println("[VisemesX] Sending viseme update.");
        ClientPlayNetworking.send((CustomPayload) buf);
    }
}
