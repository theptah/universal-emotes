package com.ptah.bundle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ModelTextures {
    private static final String[] FACE_NAMES = {"north", "south", "east", "west", "up", "down"};

    private ModelTextures() { }

    public record Usage(Set<String> explicit, boolean usesBase) { }

    public static String namespaceOf(String raw) {
        if (raw == null) return null;
        int colon = raw.indexOf(':');
        return colon > 0 ? raw.substring(0, colon).trim().toLowerCase(Locale.ROOT) : null;
    }

    public static boolean vanilla(String raw) {
        return "minecraft".equals(namespaceOf(raw));
    }

    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim().replace('\\', '/');
        int colon = s.indexOf(':');
        if (colon >= 0) s = s.substring(colon + 1);
        while (s.startsWith("./")) s = s.substring(2);
        while (s.startsWith("/")) s = s.substring(1);
        if (s.startsWith("assets/textures/")) s = s.substring("assets/textures/".length());
        else if (s.startsWith("textures/")) s = s.substring("textures/".length());
        if (s.toLowerCase(Locale.ROOT).endsWith(".png")) s = s.substring(0, s.length() - 4);
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s.isEmpty() ? null : s;
    }

    public static String baseName(String raw) {
        String s = normalize(raw);
        if (s == null) return null;
        int slash = s.lastIndexOf('/');
        s = slash >= 0 ? s.substring(slash + 1) : s;
        return s.isEmpty() ? null : s;
    }

    public static List<String> candidates(String raw) {
        Set<String> out = new LinkedHashSet<>();
        String full = normalize(raw);
        if (full != null) out.add(full);
        String base = baseName(raw);
        if (base != null) out.add(base);
        return new ArrayList<>(out);
    }

    public static Usage usage(JsonObject model) {
        Set<String> explicit = new LinkedHashSet<>();
        boolean[] base = {false};
        if (model == null) return new Usage(explicit, false);
        JsonArray bones = bedrockBones(model);
        if (bones != null) {
            for (JsonElement element : bones) {
                if (!element.isJsonObject()) continue;
                JsonObject bone = element.getAsJsonObject();
                boolean geometry = (bone.has("cubes") && bone.get("cubes").isJsonArray() && bone.getAsJsonArray("cubes").size() > 0)
                        || (bone.has("poly_mesh") && bone.get("poly_mesh").isJsonObject());
                if (!geometry) continue;
                String texture = string(bone.get("texture"));
                if (texture != null && !texture.isBlank()) explicit.add(texture);
                else base[0] = true;
            }
            return new Usage(explicit, base[0]);
        }
        if (model.has("elements") && model.get("elements").isJsonArray()) {
            Map<String, String> textures = javaTextures(model);
            for (JsonElement element : model.getAsJsonArray("elements")) {
                if (!element.isJsonObject()) continue;
                JsonObject el = element.getAsJsonObject();
                if (!el.has("from") || !el.has("to") || !el.has("faces") || !el.get("faces").isJsonObject()) continue;
                JsonObject faces = el.getAsJsonObject("faces");
                for (String face : FACE_NAMES) {
                    if (!faces.has(face) || !faces.get(face).isJsonObject()) continue;
                    JsonObject f = faces.getAsJsonObject(face);
                    String resolved = f.has("texture") ? resolveSlot(string(f.get("texture")), textures) : null;
                    if (resolved != null) explicit.add(resolved);
                    else base[0] = true;
                }
            }
        }
        return new Usage(explicit, base[0]);
    }

    public static JsonArray bedrockBones(JsonObject model) {
        if (model.has("minecraft:geometry") && model.get("minecraft:geometry").isJsonArray()) {
            JsonArray geometries = model.getAsJsonArray("minecraft:geometry");
            if (geometries.size() > 0 && geometries.get(0).isJsonObject()) {
                JsonObject geometry = geometries.get(0).getAsJsonObject();
                return geometry.has("bones") && geometry.get("bones").isJsonArray() ? geometry.getAsJsonArray("bones") : new JsonArray();
            }
        }
        for (Map.Entry<String, JsonElement> entry : model.entrySet()) {
            if (entry.getKey().startsWith("geometry.") && entry.getValue().isJsonObject()) {
                JsonObject geometry = entry.getValue().getAsJsonObject();
                if (geometry.has("bones") && geometry.get("bones").isJsonArray()) return geometry.getAsJsonArray("bones");
            }
        }
        return null;
    }

    public static Map<String, String> javaTextures(JsonObject model) {
        Map<String, String> textures = new HashMap<>();
        if (model.has("textures") && model.get("textures").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : model.getAsJsonObject("textures").entrySet()) {
                String value = string(entry.getValue());
                if (value != null) textures.put(entry.getKey(), value);
            }
        }
        return textures;
    }

    public static String resolveSlot(String ref, Map<String, String> textures) {
        if (ref == null) return null;
        String key = ref.startsWith("#") ? ref.substring(1) : ref;
        String value = textures.get(key);
        int guard = 0;
        while (value != null && value.startsWith("#") && guard++ < 6) value = textures.get(value.substring(1));
        if (value == null || value.startsWith("#") || value.isBlank()) return null;
        return value;
    }

    private static String string(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? element.getAsString() : null;
    }
}
