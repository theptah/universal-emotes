package com.ptah.config;

import com.ptah.UniversalEmotesMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ServerConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ServerConfig current = new ServerConfig();

    private ServerConfigManager() { }

    private static Path file() {
        return UniversalEmotesPaths.settings().resolve("server.json");
    }

    public static ServerConfig get() {
        return current;
    }

    public static synchronized void set(ServerConfig config) {
        current = config != null ? config : new ServerConfig();
        current.sanitize();
    }

    public static synchronized void load() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            current = new ServerConfig();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ServerConfig loaded = GSON.fromJson(reader, ServerConfig.class);
            current = loaded != null ? loaded : new ServerConfig();
        } catch (Exception exception) {
            UniversalEmotesMod.LOGGER.warn("Could not load server config, using defaults", exception);
            current = new ServerConfig();
        }
        current.sanitize();
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(UniversalEmotesPaths.settings());
            try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                GSON.toJson(current, writer);
            }
        } catch (Exception exception) {
            UniversalEmotesMod.LOGGER.warn("Could not save server config", exception);
        }
    }
}
