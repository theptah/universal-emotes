package com.ptah.client.render;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EmoteSkinOverrides {
    public record Override(ResourceLocation texture, String model) { }
    private static final Map<UUID, Override> OVERRIDES = new ConcurrentHashMap<>();
    private EmoteSkinOverrides() { }
    public static void set(UUID playerId, ResourceLocation texture, String model) {
        OVERRIDES.put(playerId, new Override(texture, model));
    }
    public static void clear(UUID playerId) { OVERRIDES.remove(playerId); }
    public static ResourceLocation texture(UUID playerId) {
        Override o = OVERRIDES.get(playerId);
        return o == null ? null : o.texture();
    }
    public static String model(UUID playerId) {
        Override o = OVERRIDES.get(playerId);
        return o == null ? null : o.model();
    }
    public static boolean isEmpty() { return OVERRIDES.isEmpty(); }
}
