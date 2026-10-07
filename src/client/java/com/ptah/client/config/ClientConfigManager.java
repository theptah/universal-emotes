package com.ptah.client.config;

import com.ptah.UniversalEmotesMod;
import com.ptah.config.UniversalEmotesPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ClientConfig current = new ClientConfig();

    private ClientConfigManager() { }

    private static Path file() {
        return UniversalEmotesPaths.settings().resolve("client.json");
    }

    public static ClientConfig get() {
        return current;
    }

    public static synchronized void load() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            current = new ClientConfig();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
            current = loaded != null ? loaded : new ClientConfig();
        } catch (Exception exception) {
            UniversalEmotesMod.LOGGER.warn("Could not load client config, using defaults", exception);
            current = new ClientConfig();
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(UniversalEmotesPaths.settings());
            try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                GSON.toJson(current, writer);
            }
        } catch (Exception exception) {
            UniversalEmotesMod.LOGGER.warn("Could not save client config", exception);
        }
    }
}
