package com.ptah.client.network;

import com.ptah.UniversalEmotesMod;
import com.ptah.client.render.RidingEmoteAlert;
import com.ptah.client.playback.MusicSeek;
import com.ptah.client.playback.ClientPlaybackRuntime;
import com.ptah.network.EmoteNetwork;
import com.ptah.playback.StopReason;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.ptah.client.compat.ClientNet;
import com.ptah.compat.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class EmoteClientNetwork {
    private EmoteClientNetwork() { }
    public static boolean requestPlay(ResourceLocation emoteId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.isPassenger()) {
            RidingEmoteAlert.show();
            return false;
        }
        if (!ClientNet.canSend(EmoteNetwork.PLAY_REQUEST)) return false;
        var buffer = Net.buf(); buffer.writeResourceLocation(emoteId);
        ClientNet.send(EmoteNetwork.PLAY_REQUEST, buffer); return true;
    }
    public static boolean requestStop() {
        if (!ClientNet.canSend(EmoteNetwork.STOP_REQUEST)) return false;
        ClientNet.send(EmoteNetwork.STOP_REQUEST, Net.buf()); return true;
    }

    public static boolean requestDanceTogether(UUID targetId) {
        Minecraft client = Minecraft.getInstance();
        if (targetId == null) return false;
        if (client.player != null && client.player.isPassenger()) {
            RidingEmoteAlert.show();
            return false;
        }
        if (!ClientNet.canSend(EmoteNetwork.DANCE_TOGETHER_REQUEST)) return false;
        var buffer = Net.buf(); buffer.writeUUID(targetId);
        ClientNet.send(EmoteNetwork.DANCE_TOGETHER_REQUEST, buffer); return true;
    }
    public static void initialize() {
        ClientBundleSync.initialize();
        ClientConfigNetwork.initialize();
        ClientVisibilityNetwork.initialize();
        ClientNet.receive(EmoteNetwork.PLAY_BROADCAST, (client, buffer) -> {
            UUID player = buffer.readUUID(); var emote = buffer.readResourceLocation(); long tick = buffer.readLong(); long seed = buffer.readLong(); boolean suppressMusic = buffer.readBoolean();
            client.execute(() -> ClientBundleSync.runWhenReady(() -> {
                if (!ClientPlaybackRuntime.MANAGER.start(player, emote, tick, seed, suppressMusic)) {
                    UniversalEmotesMod.LOGGER.warn("Client rejected broadcast because emote is missing locally: {}", emote);
                }
            }));
        });
        ClientNet.receive(EmoteNetwork.STOP_BROADCAST, (client, buffer) -> {
            UUID player = buffer.readUUID();
            StopReason reason = buffer.readEnum(StopReason.class);
            client.execute(() -> ClientPlaybackRuntime.MANAGER.stop(player, reason));
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MusicSeek.tick();
            if (client.level != null) ClientPlaybackRuntime.MANAGER.tick(client.level.getGameTime());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientPlaybackRuntime.MANAGER.stopAll(StopReason.DISCONNECT);
            ClientBundleSync.clear();
            ClientConfigNetwork.reset();
        });
    }
}
