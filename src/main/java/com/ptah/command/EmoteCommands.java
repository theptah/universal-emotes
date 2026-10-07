package com.ptah.command;

import com.ptah.compat.ServerCompat;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.ptah.UniversalEmotesMod;
import com.ptah.network.ServerBundleSync;
import com.ptah.system.EmoteSystem;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class EmoteCommands {
    public static final int RELOAD_PERMISSION_LEVEL = 4;

    private EmoteCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("universal-emotes")
                        .then(Commands.literal("reload")
                                .requires(source -> ServerCompat.hasPermission(source, RELOAD_PERMISSION_LEVEL))
                                .executes(EmoteCommands::reload))));
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();

        EmoteSystem.reload();
        int bundles = EmoteSystem.BUNDLES.size();
        int emotes = EmoteSystem.EMOTES.values().size();

        int players = server.getPlayerList().getPlayers().size();
        ServerBundleSync.resendManifestToAll(server);

        UniversalEmotesMod.LOGGER.info(
                "Emote bundles reloaded via command: {} bundles, {} emotes; re-synced {} players",
                bundles, emotes, players);

        ServerCompat.sendSuccess(source, Component.translatable(
                "commands.universal-emotes.reload.success", bundles, emotes, players), true);
        return Command.SINGLE_SUCCESS;
    }
}
