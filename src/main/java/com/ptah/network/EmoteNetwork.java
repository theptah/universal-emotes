package com.ptah.network;

import com.ptah.compat.ServerCompat;
import com.ptah.compat.Ids;
import com.ptah.UniversalEmotesMod;
import com.ptah.bundle.EmoteDefinition;
import com.ptah.config.ServerConfig;
import com.ptah.config.ServerConfigManager;
import com.ptah.playback.StopReason;
import com.ptah.system.EmoteSystem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.ptah.compat.Net;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.UUID;

public final class EmoteNetwork {
    public static final ResourceLocation PLAY_REQUEST = Net.c2s("play_request");
    public static final ResourceLocation STOP_REQUEST = Net.c2s("stop_request");
    public static final ResourceLocation PLAY_BROADCAST = Net.s2c("play_broadcast");
    public static final ResourceLocation STOP_BROADCAST = Net.s2c("stop_broadcast");

    public static final ResourceLocation DANCE_TOGETHER_REQUEST = Net.c2s("dance_together_request");

    private static final double DANCE_TOGETHER_START_DISTANCE = 9.0;
    private static final double DANCE_TOGETHER_CANCEL_DISTANCE = 10.0;
    private static final ServerEmoteState STATE = new ServerEmoteState();
    private EmoteNetwork() { }

    public static void initializeServer() {
        ServerBundleSync.initialize();
        ServerConfigNetwork.initialize();
        EmoteVisibilityNetwork.initialize();
        Net.receive(PLAY_REQUEST, (server, player, buffer) -> {
            ResourceLocation id = buffer.readResourceLocation();
            server.execute(() -> authorizeAndStart(server, player, id));
        });
        Net.receive(STOP_REQUEST, (server, player, buffer) ->
                server.execute(() -> stop(server, player.getUUID(), StopReason.MANUAL)));

        Net.receive(DANCE_TOGETHER_REQUEST, (server, player, buffer) -> {
            UUID targetId = buffer.readUUID();
            server.execute(() -> authorizeAndStartDanceTogether(server, player, targetId));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                stop(server, handler.player.getUUID(), StopReason.DISCONNECT));

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (amount > 0 && entity instanceof ServerPlayer player
                    && STATE.entries().containsKey(player.getUUID())
                    && !ServerConfigManager.get().disallowInterruptOnDamage) {
                MinecraftServer server = ServerCompat.server(player);
                if (server != null) server.execute(() -> stop(server, player.getUUID(), StopReason.DAMAGE));
            }
            return true;
        });
        ServerTickEvents.END_SERVER_TICK.register(EmoteNetwork::tick);
    }

    private static void authorizeAndStart(MinecraftServer server, ServerPlayer player, ResourceLocation id) {
        var found = EmoteSystem.EMOTES.get(id);
        if (found.isEmpty()) {
            UniversalEmotesMod.LOGGER.warn("REJECT player={} emote={} reason=UNKNOWN_EMOTE", player.getUUID(), id);
            return;
        }
        var emote = found.get();
        if (!player.isAlive() || player.isSpectator()) {
            UniversalEmotesMod.LOGGER.warn("REJECT player={} emote={} reason=INVALID_PLAYER_STATE", player.getUUID(), id);
            return;
        }

        if (player.isPassenger()) {
            UniversalEmotesMod.LOGGER.warn("REJECT player={} emote={} reason=RIDING", player.getUUID(), id);
            return;
        }
        if (!passesGates(player, emote, id, "")) return;

        long startTick = ServerCompat.level(player).getGameTime();
        long seed = player.getRandom().nextLong();
        STATE.start(player.getUUID(), new ServerEmoteState.Entry(id, startTick, seed, ServerCompat.level(player).dimension().location()));
        broadcastPlay(player, id, startTick, seed, false);
    }

    private static void authorizeAndStartDanceTogether(MinecraftServer server, ServerPlayer player, UUID targetId) {
        if (player.getUUID().equals(targetId)) return;

        ServerEmoteState.Entry targetState = STATE.entries().get(targetId);
        if (targetState == null) {
            UniversalEmotesMod.LOGGER.warn("REJECT player={} reason=PARTNER_NOT_EMOTING target={}", player.getUUID(), targetId);
            return;
        }
        ServerPlayer target = server.getPlayerList().getPlayer(targetId);
        if (target == null) return;

        ResourceLocation id = targetState.emoteId();
        var found = EmoteSystem.EMOTES.get(id);
        if (found.isEmpty()) return;
        var emote = found.get();

        if (!player.isAlive() || player.isSpectator() || player.isPassenger()) {
            UniversalEmotesMod.LOGGER.warn("REJECT player={} emote={} reason=INVALID_PLAYER_STATE (dance_together)", player.getUUID(), id);
            return;
        }

        if (ServerCompat.level(player) != ServerCompat.level(target)) return;
        if (player.distanceTo(target) > DANCE_TOGETHER_START_DISTANCE) {
            UniversalEmotesMod.LOGGER.warn("REJECT player={} reason=PARTNER_TOO_FAR target={}", player.getUUID(), targetId);
            return;
        }

        if (!passesGates(player, emote, id, " (dance_together)")) return;

        long startTick = targetState.startTick();
        long seed = targetState.seed();
        STATE.start(player.getUUID(), new ServerEmoteState.Entry(id, startTick, seed,
                ServerCompat.level(player).dimension().location(), true, targetId));
        broadcastPlay(player, id, startTick, seed, true);
    }

    private static boolean passesGates(ServerPlayer player, EmoteDefinition emote, ResourceLocation id, String context) {
        ServerConfig config = ServerConfigManager.get();
        int op = config.missingOpLevel(player, emote);
        if (op > 0) {
            UniversalEmotesMod.LOGGER.warn("REJECT player={} emote={} reason=OP_REQUIRED_{}{}", player.getUUID(), id, op, context);
            return false;
        }
        int level = config.missingLevel(player, emote);
        if (level > 0) {
            UniversalEmotesMod.LOGGER.warn("REJECT player={} emote={} reason=LEVEL_REQUIRED_{}_HAS_{}{}",
                    player.getUUID(), id, level, player.experienceLevel, context);
            return false;
        }
        return true;
    }

    private static void tick(MinecraftServer server) {
        for (var mapEntry : new ArrayList<>(STATE.entries().entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(mapEntry.getKey());
            ServerEmoteState.Entry state = mapEntry.getValue();
            if (player == null) {
                stop(server, mapEntry.getKey(), StopReason.DISCONNECT);
                continue;
            }
            if (!player.isAlive()) {
                stop(server, mapEntry.getKey(), StopReason.DEATH);
                continue;
            }
            if (player.isPassenger()) {
                stop(server, mapEntry.getKey(), StopReason.RIDING);
                continue;
            }
            if (!ServerCompat.level(player).dimension().location().equals(state.dimension())) {
                stop(server, mapEntry.getKey(), StopReason.DIMENSION_CHANGE);
                continue;
            }

            if (state.partner() != null) {
                ServerPlayer partner = server.getPlayerList().getPlayer(state.partner());
                if (partner == null || ServerCompat.level(partner) != ServerCompat.level(player)
                        || player.distanceTo(partner) > DANCE_TOGETHER_CANCEL_DISTANCE) {
                    stop(server, mapEntry.getKey(), StopReason.DANCE_PARTNER_LEFT);
                    continue;
                }
            }
            EmoteSystem.EMOTES.get(state.emoteId()).ifPresentOrElse(emote -> {
                if (!emote.animation().loop()
                        && ServerCompat.level(player).getGameTime() - state.startTick() >= Math.ceil(emote.animation().length() * 20.0)) {
                    stop(server, mapEntry.getKey(), StopReason.FINISHED);
                }
            }, () -> stop(server, mapEntry.getKey(), StopReason.SERVER));
        }
    }

    private static void stop(MinecraftServer server, UUID playerId, StopReason reason) {
        if (STATE.stop(playerId) == null) return;
        FriendlyByteBuf buffer = Net.buf();
        buffer.writeUUID(playerId);
        buffer.writeEnum(reason);
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            Net.send(target, STOP_BROADCAST, new FriendlyByteBuf(buffer.copy()));
        }
    }

    private static void broadcastPlay(ServerPlayer player, ResourceLocation id, long startTick, long seed, boolean suppressMusic) {
        for (ServerPlayer target : PlayerLookup.world(ServerCompat.level(player))) {
            sendPlay(target, player.getUUID(), id, startTick, seed, suppressMusic);
        }
    }

    static void syncActiveTo(ServerPlayer target) {
        MinecraftServer server = ServerCompat.server(target);
        if (server == null) return;
        for (var entry : STATE.entries().entrySet()) {
            ServerPlayer actor = server.getPlayerList().getPlayer(entry.getKey());
            if (actor != null && ServerCompat.level(actor) == ServerCompat.level(target)) {
                var state = entry.getValue();
                sendPlay(target, actor.getUUID(), state.emoteId(), state.startTick(), state.seed(), state.suppressMusic());
            }
        }
    }

    private static void sendPlay(ServerPlayer target, UUID playerId, ResourceLocation id, long startTick, long seed, boolean suppressMusic) {
        FriendlyByteBuf buffer = Net.buf();
        buffer.writeUUID(playerId); buffer.writeResourceLocation(id); buffer.writeLong(startTick); buffer.writeLong(seed); buffer.writeBoolean(suppressMusic);
        Net.send(target, PLAY_BROADCAST, buffer);
    }
}
