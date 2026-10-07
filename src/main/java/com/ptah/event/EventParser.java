package com.ptah.event;

import com.ptah.compat.Ids;
import com.ptah.animation.AnimationParser;
import com.ptah.animation.Interpolation;
import com.ptah.animation.Vec3;
import com.ptah.bundle.BundleDiagnostic;
import com.ptah.bundle.BundleLimits;
import com.ptah.bundle.BundleValidator;
import com.ptah.event.type.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EventParser {
    private static final Set<String> ROOT_KEYS = Set.of("events");
    private static final Set<String> MODEL_KEYFRAME_KEYS = Set.of("time", "pos", "rot", "scale", "lerp_mode");
    private static final Set<String> LERP_MODES = Set.of("linear", "catmullrom", "smooth", "step");
    private static final Set<String> ATTACH_MODES = Set.of("bone", "model_root", "world");
    private static final Map<String, Set<String>> EVENT_KEYS = Map.of(
            "play_music", Set.of("time", "type", "asset_id", "volume", "dmca"),
            "play_sound", Set.of("time", "type", "asset_id", "volume", "pitch", "dmca"),
            "spawn_particle", Set.of("time", "type", "asset_id", "bone", "amount", "pos", "rot"),
            "set_skin", Set.of("time", "type", "asset_id", "model"),
            "reset_skin", Set.of("time", "type"),
            "render_model", Set.of("time", "type", "asset_id", "texture", "attach_mode", "bone", "pos", "rot",
                    "scale", "end", "keyframes"));

    private EventParser() { }
    public static EventTimeline parse(JsonObject root, float animationLength, BundleLimits limits,
                                      String source, List<BundleDiagnostic> diagnostics) {
        if (root == null) return new EventTimeline(List.of());
        if (!root.has("events") || !root.get("events").isJsonArray()) {
            diagnostics.add(BundleDiagnostic.error(source, "events must be an array"));
            return new EventTimeline(List.of());
        }
        BundleValidator.unknownKeys(root, ROOT_KEYS, source, diagnostics);
        JsonArray array = root.getAsJsonArray("events");
        if (array.size() > limits.maxEvents()) {
            diagnostics.add(BundleDiagnostic.error(source, "Too many events: " + array.size()));
            return new EventTimeline(List.of());
        }
        List<EmoteEvent> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            String at = source + "#events[" + i + "]";
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) { diagnostics.add(BundleDiagnostic.error(at, "Event must be an object")); continue; }
            JsonObject event = element.getAsJsonObject();
            try {
                float time = number(event, "time", "Missing or invalid time");
                if (!Float.isFinite(time) || time < 0 || time > animationLength) throw new IllegalArgumentException("time must be between 0 and animation_length (" + animationLength + ")");
                JsonElement typeElement = event.get("type");
                if (typeElement == null || !typeElement.isJsonPrimitive() || !typeElement.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Missing or invalid type");
                }
                String type = typeElement.getAsString();
                Set<String> known = EVENT_KEYS.get(type);
                if (known == null) throw new IllegalArgumentException("Unknown event type: " + type);
                BundleValidator.unknownKeys(event, known, at, diagnostics);
                float volume = event.has("volume") ? number(event, "volume", "Invalid volume") : 1.0f;
                if (!Float.isFinite(volume) || volume < 0 || volume > 4) throw new IllegalArgumentException("volume must be between 0 and 4");
                result.add(switch (type) {
                    case "play_music" -> {
                        Dmca d = dmca(event, at, diagnostics);
                        yield new PlayMusicEvent(time, id(event, "asset_id", at, diagnostics), volume, d.flag, d.alt);
                    }
                    case "play_sound" -> {
                        Dmca d = dmca(event, at, diagnostics);
                        yield new PlaySoundEvent(time, id(event, "asset_id", at, diagnostics), volume, pitch(event, at, diagnostics), d.flag, d.alt);
                    }
                    case "spawn_particle" -> new SpawnParticleEvent(time, bone(event, at, diagnostics),
                            id(event, "asset_id", at, diagnostics),
                            BundleValidator.integer(event, "amount", 1, 1, 1024, at, diagnostics),
                            vec3(event, "pos", at, diagnostics), vec3(event, "rot", at, diagnostics));
                    case "set_skin" -> new SetSkinEvent(time, id(event, "asset_id", at, diagnostics), skinModel(event, at, diagnostics));
                    case "reset_skin" -> new ResetSkinEvent(time);
                    case "render_model" -> new RenderModelEvent(time, id(event, "asset_id", at, diagnostics),
                            optString(event, "texture"), attachMode(event, at, diagnostics), bone(event, at, diagnostics),
                            vec3(event, "pos", at, diagnostics), vec3(event, "rot", at, diagnostics),
                            modelScale(event, at, diagnostics), modelEnd(event, time, animationLength, at, diagnostics),
                            modelKeyframes(event, animationLength, at, diagnostics));
                    default -> throw new IllegalArgumentException("Unknown event type: " + type);
                });
            } catch (Exception exception) {
                diagnostics.add(BundleDiagnostic.error(at, exception.getMessage() == null ? "Invalid event" : exception.getMessage()));
            }
        }
        return new EventTimeline(result);
    }
    private record Dmca(boolean flag, ResourceLocation alt) { }

    private static Dmca dmca(JsonObject event, String source, List<BundleDiagnostic> diagnostics) {
        if (!event.has("dmca") || event.get("dmca").isJsonNull()) return new Dmca(false, null);
        JsonElement v = event.get("dmca");
        if (v.isJsonPrimitive()) {
            var p = v.getAsJsonPrimitive();
            if (p.isBoolean()) return new Dmca(p.getAsBoolean(), null);
            if (p.isNumber()) return new Dmca(p.getAsDouble() != 0, null);
            String raw = p.getAsString().trim();
            if (raw.isEmpty() || raw.equalsIgnoreCase("false")) return new Dmca(false, null);
            int split = raw.indexOf(':');
            if (split > 0 && split < raw.length() - 1) {
                return new Dmca(true, BundleValidator.id(raw.substring(0, split), raw.substring(split + 1), source, diagnostics));
            }
            if (!raw.equalsIgnoreCase("true")) {
                diagnostics.add(BundleDiagnostic.warning(source, "dmca '" + raw + "' is not namespace:path; treated as true without an alternative"));
            }
            return new Dmca(true, null);
        }
        diagnostics.add(BundleDiagnostic.warning(source, "dmca must be true, false or namespace:path; treated as true"));
        return new Dmca(true, null);
    }

    private static ResourceLocation id(JsonObject object, String key, String source, List<BundleDiagnostic> diagnostics) {
        String raw = BundleValidator.string(object, key, source, diagnostics);
        int split = raw.indexOf(':');
        if (split <= 0 || split == raw.length() - 1) {
            diagnostics.add(BundleDiagnostic.error(source, key + " must be namespace:path"));
            return Ids.of("universal-emotes", "invalid");
        }
        return BundleValidator.id(raw.substring(0, split), raw.substring(split + 1), source, diagnostics);
    }
    private static String optString(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return null;
        try { String s = object.get(key).getAsString(); return s.isBlank() ? null : s; } catch (Exception e) { return null; }
    }
    private static float number(JsonObject object, String key, String message) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || element.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(message);
        }
        try { return element.getAsFloat(); } catch (Exception exception) { throw new IllegalArgumentException(message); }
    }
    private static String bone(JsonObject object, String source, List<BundleDiagnostic> diagnostics) {
        String bone = optString(object, "bone");
        if (object.has("bone") && !object.get("bone").isJsonNull() && bone == null) {
            diagnostics.add(BundleDiagnostic.warning(source, "bone must be a bone name; ignored"));
        }
        if (bone != null && !AnimationParser.isBone(bone)) {
            diagnostics.add(BundleDiagnostic.warning(source, "Unknown bone '" + bone + "'; using the default position"));
            return null;
        }
        return bone;
    }
    private static String skinModel(JsonObject object, String source, List<BundleDiagnostic> diagnostics) {
        String raw = optString(object, "model");
        if (raw == null) return null;
        return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "thin", "slim", "alex" -> "slim";
            case "wide", "classic", "steve" -> "default";
            case "default", "auto" -> null;
            default -> {
                diagnostics.add(BundleDiagnostic.warning(source, "Unknown skin model '" + raw + "' (use slim or wide); using the player's own model"));
                yield null;
            }
        };
    }
    private static String attachMode(JsonObject object, String source, List<BundleDiagnostic> diagnostics) {
        String m = optString(object, "attach_mode");
        if (m == null) return "bone";
        String mode = m.trim().toLowerCase(java.util.Locale.ROOT);
        if (ATTACH_MODES.contains(mode)) return mode;
        diagnostics.add(BundleDiagnostic.warning(source, "Unknown attach_mode '" + m + "' (use bone, model_root or world); using bone"));
        return "bone";
    }
    private static float pitch(JsonObject object, String source, List<BundleDiagnostic> diagnostics) {
        if (!object.has("pitch") || object.get("pitch").isJsonNull()) return 1.0f;
        float p = floatOr(object, "pitch", Float.NaN);
        if (!Float.isFinite(p) || p <= 0 || p > 4) {
            diagnostics.add(BundleDiagnostic.warning(source, "pitch must be > 0 and <= 4; got " + object.get("pitch") + ", using 1.0"));
            return 1.0f;
        }
        return p;
    }
    private static float modelScale(JsonObject object, String source, List<BundleDiagnostic> diagnostics) {
        if (!object.has("scale") || object.get("scale").isJsonNull()) return 1.0f;
        float scale = floatOr(object, "scale", Float.NaN);
        if (!Float.isFinite(scale) || scale < 0) {
            diagnostics.add(BundleDiagnostic.warning(source, "scale must be >= 0; got " + object.get("scale") + ", using 1.0"));
            return 1.0f;
        }
        return scale;
    }
    private static Float modelEnd(JsonObject object, float time, float animationLength, String source, List<BundleDiagnostic> diagnostics) {
        if (!object.has("end") || object.get("end").isJsonNull()) return null;
        Float end = optFloat(object, "end");
        if (end == null) {
            diagnostics.add(BundleDiagnostic.warning(source, "end must be a number; model stays until the emote ends"));
            return null;
        }
        if (end <= time) {
            diagnostics.add(BundleDiagnostic.warning(source, "end must be after time; the model is shown for a single tick"));
        }
        return end;
    }
    private static float floatOr(JsonObject object, String key, float fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) return fallback;
        try { float v = object.get(key).getAsFloat(); return Float.isFinite(v) ? v : fallback; } catch (Exception e) { return fallback; }
    }
    private static Float optFloat(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return null;
        try { float v = object.get(key).getAsFloat(); return Float.isFinite(v) ? v : null; } catch (Exception e) { return null; }
    }

    private static final int MAX_MODEL_KEYFRAMES = 1024;

    private static List<RenderModelEvent.ModelKeyframe> modelKeyframes(JsonObject event, float animationLength,
                                                                        String source, List<BundleDiagnostic> diagnostics) {
        if (!event.has("keyframes") || event.get("keyframes").isJsonNull()) return List.of();
        if (!event.get("keyframes").isJsonArray()) {
            diagnostics.add(BundleDiagnostic.warning(source, "keyframes must be an array; model stays static"));
            return List.of();
        }
        JsonArray array = event.getAsJsonArray("keyframes");
        if (array.size() > MAX_MODEL_KEYFRAMES) {
            diagnostics.add(BundleDiagnostic.warning(source, "Too many model keyframes (" + array.size() + "); model stays static"));
            return List.of();
        }
        List<RenderModelEvent.ModelKeyframe> frames = new ArrayList<>(array.size());
        for (int k = 0; k < array.size(); k++) {
            String at = source + ".keyframes[" + k + "]";
            JsonElement element = array.get(k);
            if (!element.isJsonObject()) { diagnostics.add(BundleDiagnostic.warning(at, "Keyframe must be an object; skipped")); continue; }
            JsonObject frame = element.getAsJsonObject();
            BundleValidator.unknownKeys(frame, MODEL_KEYFRAME_KEYS, at, diagnostics);
            Float time = optFloat(frame, "time");
            if (time == null || time < 0 || time > animationLength) {
                diagnostics.add(BundleDiagnostic.warning(at, "Invalid keyframe time; skipped"));
                continue;
            }
            float scale = modelScale(frame, at, diagnostics);
            String lerp = optString(frame, "lerp_mode");
            if (frame.has("lerp_mode") && !frame.get("lerp_mode").isJsonNull()
                    && (lerp == null || !LERP_MODES.contains(lerp.trim().toLowerCase(java.util.Locale.ROOT)))) {
                diagnostics.add(BundleDiagnostic.warning(at, "Unsupported lerp_mode " + frame.get("lerp_mode") + ", using linear"));
                lerp = null;
            }
            frames.add(new RenderModelEvent.ModelKeyframe(time, vec3(frame, "pos", at, diagnostics), vec3(frame, "rot", at, diagnostics), scale,
                    Interpolation.fromId(lerp)));
        }
        frames.sort(java.util.Comparator.comparingDouble(RenderModelEvent.ModelKeyframe::time));
        return frames;
    }
    private static Vec3 vec3(JsonObject object, String key, String source, List<BundleDiagnostic> diagnostics) {
        if (!object.has(key) || object.get(key).isJsonNull()) return null;
        Vec3 value = null;
        if (object.get(key).isJsonArray() && object.getAsJsonArray(key).size() >= 3) {
            JsonArray a = object.getAsJsonArray(key);
            try {
                float x = a.get(0).getAsFloat(), y = a.get(1).getAsFloat(), z = a.get(2).getAsFloat();
                if (Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)) value = new Vec3(x, y, z);
            } catch (Exception ignored) { }
        }
        if (value == null) diagnostics.add(BundleDiagnostic.warning(source, key + " must be [x, y, z] numbers; ignored"));
        return value;
    }
}
