package com.ptah.client;

import com.ptah.UniversalEmotesMod;
import com.ptah.config.UniversalEmotesPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class UniversalEmotesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = UniversalEmotesPaths.settings().resolve("favorites.json");

    private UniversalEmotesConfig() {
    }

    public static Set<String> loadFavorites() {
        Set<String> result = new LinkedHashSet<>();
        if (!Files.isRegularFile(CONFIG_PATH)) return result;

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data != null && data.favorites != null) {
                for (String emoteId : data.favorites) {
                    if (emoteId != null && !emoteId.isBlank()) result.add(emoteId);
                }
            }
        } catch (Exception exception) {
            UniversalEmotesMod.LOGGER.warn("Could not load Emote Wheel favorites", exception);
        }
        return result;
    }

    public static void saveFavorites(Set<String> favorites) {
        Data data = new Data();
        data.favorites = favorites.stream()
                .filter(emoteId -> emoteId != null && !emoteId.isBlank())
                .sorted()
                .toList();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException exception) {
            UniversalEmotesMod.LOGGER.warn("Could not save Emote Wheel favorites", exception);
        }
    }

    private static final class Data {
        private List<String> favorites = new ArrayList<>();
    }
}
