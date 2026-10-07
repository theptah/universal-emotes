package com.ptah.network;

import com.ptah.compat.Ids;
import com.ptah.UniversalEmotesMod;
import com.ptah.compat.Net;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EmoteVisibilityNetwork {
    public static final ResourceLocation UPDATE = Net.c2s("visibility_update");
    public static final ResourceLocation SYNC = Net.s2c("visibility_sync");

    private static final Map<UUID, boolean[]> STATE = new HashMap<>();

    private EmoteVisibilityNetwork() { }

    public static void initialize() {
        Net.receive(UPDATE, (server, player, buffer) -> {
            boolean hideArmor = buffer.readBoolean();
            boolean hideHeldItems = buffer.readBoolean();
            server.execute(() -> {
                STATE.put(player.getUUID(), new boolean[]{hideArmor, hideHeldItems});
                broadcast(server, player.getUUID(), hideArmor, hideHeldItems);
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> syncAllTo(handler.player)));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUUID();
            server.execute(() -> {
                if (STATE.remove(id) != null) broadcast(server, id, false, false);
            });
        });
    }

    private static void syncAllTo(ServerPlayer target) {
        for (Map.Entry<UUID, boolean[]> entry : STATE.entrySet()) {
            sendTo(target, entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
        }
    }

    private static void broadcast(MinecraftServer server, UUID id, boolean hideArmor, boolean hideHeldItems) {
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            sendTo(target, id, hideArmor, hideHeldItems);
        }
    }

    private static void sendTo(ServerPlayer target, UUID id, boolean hideArmor, boolean hideHeldItems) {
        if (!Net.canSend(target, SYNC)) return;
        FriendlyByteBuf buffer = Net.buf();
        buffer.writeUUID(id);
        buffer.writeBoolean(hideArmor);
        buffer.writeBoolean(hideHeldItems);
        Net.send(target, SYNC, buffer);
    }
}
