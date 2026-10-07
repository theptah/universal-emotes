package com.ptah.bundle;

import com.ptah.compat.Ids;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class BundleValidator {
    private static final Pattern SEMVER = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:[-+][0-9A-Za-z.-]+)?$");
    private BundleValidator() { }

    public static String string(JsonObject object, String key, String source, List<BundleDiagnostic> diagnostics) {
        JsonElement value = object.get(key);
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isString() || primitive.getAsString().isBlank()) {
            diagnostics.add(BundleDiagnostic.error(source, "Missing or invalid string: " + key));
            return "";
        }
        return primitive.getAsString();
    }

    public static String optionalString(JsonObject object, String key, String source, List<BundleDiagnostic> diagnostics) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return "";
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isString()) {
            diagnostics.add(BundleDiagnostic.error(source, key + " must be a string"));
            return "";
        }
        return primitive.getAsString();
    }

    public static void unknownKeys(JsonObject object, Set<String> known, String source, List<BundleDiagnostic> diagnostics) {
        if (object == null) return;
        for (String key : object.keySet()) {
            if (!known.contains(key)) diagnostics.add(BundleDiagnostic.warning(source, "Unknown key: " + key));
        }
    }

    public static int integer(JsonObject object, String key, int fallback, int min, int max,
                              String source, List<BundleDiagnostic> diagnostics) {
        try {
            int value = object.has(key) ? object.get(key).getAsInt() : fallback;
            if (value < min || value > max) throw new IllegalArgumentException();
            return value;
        } catch (Exception exception) {
            diagnostics.add(BundleDiagnostic.error(source, "Invalid integer " + key + " (expected " + min + ".." + max + ")"));
            return fallback;
        }
    }

    public static int requiredInteger(JsonObject object, String key, int min, int max,
                                      String source, List<BundleDiagnostic> diagnostics) {
        if (!object.has(key)) {
            diagnostics.add(BundleDiagnostic.error(source, "Missing required integer: " + key));
            return min;
        }
        return integer(object, key, min, min, max, source, diagnostics);
    }

    public static ResourceLocation id(String namespace, String path, String source, List<BundleDiagnostic> diagnostics) {
        try { return Ids.of(namespace, path); }
        catch (ResourceLocationException exception) {
            diagnostics.add(BundleDiagnostic.error(source, "Invalid resource id: " + namespace + ":" + path));
            return Ids.of("universal-emotes", "invalid");
        }
    }

    public static void semanticVersion(String value, String key, String source, List<BundleDiagnostic> diagnostics) {
        if (!SEMVER.matcher(value).matches()) diagnostics.add(BundleDiagnostic.error(source, key + " must be semantic version x.y.z"));
    }

    public static boolean hasErrors(List<BundleDiagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(d -> d.severity() == BundleDiagnostic.Severity.ERROR);
    }
}
