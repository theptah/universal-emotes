package com.ptah.config;

import com.ptah.UniversalEmotesMod;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class UniversalEmotesPaths {
    private UniversalEmotesPaths() { }

    public static Path root() {
        return FabricLoader.getInstance().getConfigDir().resolve(UniversalEmotesMod.MOD_ID);
    }

    public static Path cache() {
        return root().resolve("cache");
    }

    public static Path settings() {
        return root().resolve("settings");
    }

    public static Path bundles() {
        return root().resolve("bundles");
    }
}
