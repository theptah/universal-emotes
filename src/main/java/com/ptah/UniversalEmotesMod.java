package com.ptah;

import com.ptah.command.EmoteCommands;
import com.ptah.config.ServerConfigManager;
import com.ptah.network.EmoteNetwork;
import com.ptah.registry.ModSounds;
import com.ptah.system.EmoteSystem;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UniversalEmotesMod implements ModInitializer {
    public static final String MOD_ID = "universal-emotes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModSounds.register();
        ServerConfigManager.load();
        EmoteSystem.reload();
        EmoteNetwork.initializeServer();
        EmoteCommands.register();
        LOGGER.info("Universal Emotes core initialized");
    }
}
