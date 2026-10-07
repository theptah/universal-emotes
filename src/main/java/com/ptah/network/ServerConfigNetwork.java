package com.ptah.network;

import com.ptah.compat.ServerCompat;
import com.ptah.compat.Ids;
import com.ptah.UniversalEmotesMod;
import com.ptah.config.ServerConfig;
import com.ptah.config.ServerConfigManager;
import com.ptah.compat.Net;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerConfigNetwork {
    public static final ResourceLocation CONFIG_SYNC = Net.s2c("config_sync");
    public static final ResourceLocation CONFIG_UPDATE = Net.c2s("config_update");

    public static final int EDIT_PERMISSION_LEVEL = 2;

    private ServerConfigNetwork() { }

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> sendTo(handler.player)));

        Net.receive(CONFIG_UPDATE, (server, player, buffer) -> {
            ServerConfig incoming = read(buffer);
            server.execute(() -> {
                if (!canEdit(server, player)) {
                    UniversalEmotesMod.LOGGER.warn("REJECT config update from {} reason=NOT_AUTHORIZED", player.getScoreboardName());

                    sendTo(player);
                    return;
                }
                ServerConfigManager.set(incoming);
                ServerConfigManager.save();
                broadcast(server);
                UniversalEmotesMod.LOGGER.info("Server config updated by {}", player.getScoreboardName());
            });
        });
    }

    public static boolean canEdit(MinecraftServer server, ServerPlayer player) {
        return ServerCompat.isSingleplayerOwner(server, player)
                || ServerCompat.hasPermission(player, EDIT_PERMISSION_LEVEL);
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayer target : server.getPlayerList().getPlayers()) sendTo(target);
    }

    public static void sendTo(ServerPlayer target) {
        if (!Net.canSend(target, CONFIG_SYNC)) return;
        FriendlyByteBuf buffer = Net.buf();
        write(buffer, ServerConfigManager.get());

        MinecraftServer server = ServerCompat.server(target);
        buffer.writeBoolean(server != null && canEdit(server, target));
        Net.send(target, CONFIG_SYNC, buffer);
    }

    public static void write(FriendlyByteBuf out, ServerConfig config) {
        out.writeBoolean(config.ignoreAnimationsOpLevel);
        out.writeBoolean(config.enableLevelRequirement);
        out.writeBoolean(config.disallowInterruptOnDamage);
        out.writeVarInt(config.legendaryLevel);
        out.writeVarInt(config.rareLevel);
        out.writeVarInt(config.uncommonLevel);
        out.writeVarInt(config.commonLevel);
        out.writeVarInt(config.complementaryLevel);
    }

    public static ServerConfig read(FriendlyByteBuf in) {
        ServerConfig config = new ServerConfig();
        config.ignoreAnimationsOpLevel = in.readBoolean();
        config.enableLevelRequirement = in.readBoolean();
        config.disallowInterruptOnDamage = in.readBoolean();
        config.legendaryLevel = in.readVarInt();
        config.rareLevel = in.readVarInt();
        config.uncommonLevel = in.readVarInt();
        config.commonLevel = in.readVarInt();
        config.complementaryLevel = in.readVarInt();
        config.sanitize();
        return config;
    }
}
