package com.ptah.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ServerCompat {
    private ServerCompat() { }

    public static ServerLevel level(ServerPlayer player) {
        //? if >=1.21.6 {
        /*return player.level();
        *///?} elif >=1.20 {
        return player.serverLevel();
        //?} else
        /*return player.getLevel();*/
    }

    public static MinecraftServer server(ServerPlayer player) {
        //? if >=1.21.9 {
        /*return player.level().getServer();
        *///?} else
        return player.getServer();
    }

    public static boolean isSingleplayerOwner(MinecraftServer server, ServerPlayer player) {
        //? if >=1.21.9 {
        /*return server.isSingleplayerOwner(new net.minecraft.server.players.NameAndId(player.getGameProfile()));
        *///?} else
        return server.isSingleplayerOwner(player.getGameProfile());
    }

    public static boolean hasPermission(Player player, int level) {
        //? if >=1.21.11 {
        /*return level <= 0 || player.permissions().hasPermission(permission(level));
        *///?} else
        return player.hasPermissions(level);
    }

    public static boolean hasPermission(CommandSourceStack source, int level) {
        //? if >=1.21.11 {
        /*return level <= 0 || source.permissions().hasPermission(permission(level));
        *///?} else
        return source.hasPermission(level);
    }

    //? if >=1.21.11 {
    /*private static net.minecraft.server.permissions.Permission permission(int level) {
        return switch (Math.min(level, 4)) {
            case 1 -> net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR;
            case 2 -> net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER;
            case 3 -> net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN;
            default -> net.minecraft.server.permissions.Permissions.COMMANDS_OWNER;
        };
    }
    *///?}

    public static void sendSuccess(CommandSourceStack source, Component message, boolean broadcast) {
        //? if >=1.20 {
        source.sendSuccess(() -> message, broadcast);
        //?} else
        /*source.sendSuccess(message, broadcast);*/
    }
}
