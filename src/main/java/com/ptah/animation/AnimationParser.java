package com.ptah.animation;

import com.ptah.bundle.BundleDiagnostic;
import com.ptah.bundle.BundleLimits;
import com.ptah.bundle.BundleValidator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AnimationParser {
    private static final Set<String> BONES = Set.of(

            "Root", "Head", "RightItem", "LeftItem",

            "Torso", "RightArm", "LeftArm", "RightLeg", "LeftLeg",

            "LowerTorso", "UpperTorso",
            "RightUpperArm", "RightLowerArm", "RightHand",
            "LeftUpperArm", "LeftLowerArm", "LeftHand",
            "RightUpperLeg", "RightLowerLeg", "RightFoot",
            "LeftUpperLeg", "LeftLowerLeg", "LeftFoot");

    private static final Set<String> SUPPORTED_FORMATS = Set.of("1.8.0");
    private static final Set<String> ROOT_KEYS = Set.of("format_version", "animations");
    private static final Set<String> ANIMATION_KEYS = Set.of("loop", "animation_length", "loop_start", "bones");
    private static final Set<String> BONE_KEYS = Set.of("rotation", "position", "bend");
    private static final Set<String> KEYFRAME_KEYS = Set.of("vector", "pre", "post", "lerp_mode");
    private static final Set<String> LERP_MODES = Set.of("linear", "catmullrom", "smooth", "step");
    private AnimationParser() { }

    public static boolean isBone(String name) { return name != null && BONES.contains(name); }

    public static AnimationClip parse(JsonObject root, String key, BundleLimits limits, String source,
                                      List<BundleDiagnostic> diagnostics) {
        BundleValidator.unknownKeys(root, ROOT_KEYS, source, diagnostics);
        JsonElement formatElement = root.get("format_version");
        String format = formatElement != null && formatElement.isJsonPrimitive() ? formatElement.getAsString() : "";
        if (formatElement != null && !formatElement.isJsonNull() && !formatElement.isJsonPrimitive()) {
            diagnostics.add(BundleDiagnostic.error(source, "format_version must be a string"));
        } else if (format.isBlank()) {
            diagnostics.add(BundleDiagnostic.error(source, "Missing format_version"));
        } else if (!SUPPORTED_FORMATS.contains(format)) {
            diagnostics.add(BundleDiagnostic.warning(source,
                    "Unrecognized format_version '" + format + "' (supported: " + SUPPORTED_FORMATS + "); parsing anyway"));
        }
        JsonObject animations = object(root, "animations", source, diagnostics);
        JsonObject animation = animations != null && animations.has(key) && animations.get(key).isJsonObject()
                ? animations.getAsJsonObject(key) : null;
        if (animation == null) {
            diagnostics.add(BundleDiagnostic.error(source, "Animation key not found: " + key));
            return new AnimationClip(format, key, false, 1, 0, Map.of());
        }
        BundleValidator.unknownKeys(animation, ANIMATION_KEYS, source, diagnostics);
        boolean loop = false;
        if (animation.has("loop") && !animation.get("loop").isJsonNull()) {
            JsonElement loopElement = animation.get("loop");
            if (loopElement.isJsonPrimitive() && loopElement.getAsJsonPrimitive().isBoolean()) {
                loop = loopElement.getAsBoolean();
            } else if (!(loopElement.isJsonPrimitive() && "hold_on_last_frame".equals(loopElement.getAsString()))) {
                diagnostics.add(BundleDiagnostic.warning(source, "loop must be true or false; got " + loopElement + ", treated as false"));
            }
        }
        float length = number(animation, "animation_length", -1);
        if (!Float.isFinite(length) || length <= 0 || length > limits.maxAnimationSeconds()) {
            diagnostics.add(BundleDiagnostic.error(source, "animation_length must be > 0 and <= " + limits.maxAnimationSeconds()));
            length = 1;
        }

        float loopStart = number(animation, "loop_start", 0);
        if (loopStart != 0 && (!loop || !Float.isFinite(loopStart) || loopStart < 0 || loopStart >= length)) {
            diagnostics.add(BundleDiagnostic.warning(source, "loop_start ignored (needs a looping clip and 0 <= loop_start < animation_length)"));
            loopStart = 0;
        }
        Map<String, BoneAnimation> bones = new LinkedHashMap<>();
        JsonObject bonesJson = animation.has("bones") && animation.get("bones").isJsonObject() ? animation.getAsJsonObject("bones") : new JsonObject();
        for (Map.Entry<String, JsonElement> entry : bonesJson.entrySet()) {
            if (!BONES.contains(entry.getKey())) {
                diagnostics.add(BundleDiagnostic.warning(source, "Unsupported bone skipped: " + entry.getKey()));
                continue;
            }
            if (!entry.getValue().isJsonObject()) {
                diagnostics.add(BundleDiagnostic.error(source, "Bone must be an object: " + entry.getKey()));
                continue;
            }
            JsonObject bone = entry.getValue().getAsJsonObject();
            for (String channel : bone.keySet()) {
                if (BONE_KEYS.contains(channel)) continue;
                diagnostics.add(BundleDiagnostic.warning(source + "#" + entry.getKey(),
                        channel.equals("scale") ? "Unsupported channel ignored: scale" : "Unknown key: " + channel));
            }
            KeyframeTrack rotation = track(bone, "rotation", length, limits, source + "#" + entry.getKey(), diagnostics);
            KeyframeTrack position = track(bone, "position", length, limits, source + "#" + entry.getKey(), diagnostics);
            BendTrack bend = bendTrack(bone, length, limits, source + "#" + entry.getKey(), diagnostics);
            bones.put(entry.getKey(), new BoneAnimation(entry.getKey(), rotation, position, bend));
        }
        return new AnimationClip(format, key, loop, length, loopStart, bones);
    }

    private static KeyframeTrack track(JsonObject bone, String channel, float length, BundleLimits limits,
                                       String source, List<BundleDiagnostic> diagnostics) {
        if (!bone.has(channel)) return new KeyframeTrack(List.of());
        if (!bone.get(channel).isJsonObject()) {
            diagnostics.add(BundleDiagnostic.error(source, channel + " must be a keyframe object"));
            return new KeyframeTrack(List.of());
        }
        List<Keyframe> frames = new ArrayList<>();
        KeyframeNotes notes = new KeyframeNotes();
        for (Map.Entry<String, JsonElement> entry : bone.getAsJsonObject(channel).entrySet()) {
            if (frames.size() >= limits.maxKeyframesPerBone()) {
                diagnostics.add(BundleDiagnostic.error(source, "Too many keyframes in " + channel));
                break;
            }
            try {
                float time = Float.parseFloat(entry.getKey());

                if (!Float.isFinite(time) || time < 0 || time > limits.maxAnimationSeconds()) throw new IllegalArgumentException();
                RawKeyframe raw = rawKeyframe(entry.getValue(), notes);
                if (raw == null || raw.vector().size() != 3) throw new IllegalArgumentException();
                notes.time(time, length);
                float x = raw.vector().get(0).getAsFloat(), y = raw.vector().get(1).getAsFloat(), z = raw.vector().get(2).getAsFloat();
                if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) throw new IllegalArgumentException();
                frames.add(new Keyframe(time, new Vec3(x, y, z), raw.interpolation()));
            } catch (Exception exception) {
                diagnostics.add(BundleDiagnostic.error(source, "Invalid " + channel + " keyframe at " + entry.getKey()));
            }
        }
        notes.report(channel, source, diagnostics);
        return new KeyframeTrack(frames);
    }

    private static BendTrack bendTrack(JsonObject bone, float length, BundleLimits limits,
                                       String source, List<BundleDiagnostic> diagnostics) {
        if (!bone.has("bend")) return new BendTrack(List.of());
        if (!bone.get("bend").isJsonObject()) {
            diagnostics.add(BundleDiagnostic.error(source, "bend must be a keyframe object"));
            return new BendTrack(List.of());
        }
        List<BendKeyframe> frames = new ArrayList<>();
        KeyframeNotes notes = new KeyframeNotes();
        for (Map.Entry<String, JsonElement> entry : bone.getAsJsonObject("bend").entrySet()) {
            if (frames.size() >= limits.maxKeyframesPerBone()) {
                diagnostics.add(BundleDiagnostic.error(source, "Too many keyframes in bend"));
                break;
            }
            try {
                float time = Float.parseFloat(entry.getKey());

                if (!Float.isFinite(time) || time < 0 || time > limits.maxAnimationSeconds()) throw new IllegalArgumentException();
                RawKeyframe raw = rawKeyframe(entry.getValue(), notes);
                if (raw == null || raw.vector().size() != 2) throw new IllegalArgumentException();
                notes.time(time, length);
                float value = raw.vector().get(0).getAsFloat(), axis = raw.vector().get(1).getAsFloat();
                if (!Float.isFinite(value) || !Float.isFinite(axis)) throw new IllegalArgumentException();
                frames.add(new BendKeyframe(time, value, axis, raw.interpolation()));
            } catch (Exception exception) {
                diagnostics.add(BundleDiagnostic.error(source, "Invalid bend keyframe at " + entry.getKey()));
            }
        }
        notes.report("bend", source, diagnostics);
        return new BendTrack(frames);
    }

    private record RawKeyframe(JsonArray vector, Interpolation interpolation) { }

    private static final class KeyframeNotes {
        final Map<String, Integer> lerpModes = new LinkedHashMap<>();
        final Set<String> keys = new java.util.TreeSet<>();
        final Set<Float> times = new java.util.HashSet<>();
        final Set<Float> duplicates = new java.util.TreeSet<>();
        int afterEnd;

        void time(float time, float length) {
            if (!times.add(time)) duplicates.add(time);
            if (time > length + 1.0e-4f) afterEnd++;
        }

        void report(String channel, String source, List<BundleDiagnostic> diagnostics) {
            lerpModes.forEach((mode, count) -> diagnostics.add(BundleDiagnostic.warning(source,
                    "Unsupported lerp_mode " + mode + " in " + channel + " (" + count + (count == 1 ? " keyframe" : " keyframes")
                            + "), using linear")));
            for (String key : keys) diagnostics.add(BundleDiagnostic.warning(source, "Unknown key in " + channel + " keyframe: " + key));
            for (Float time : duplicates) diagnostics.add(BundleDiagnostic.warning(source, "Duplicate " + channel + " keyframe time: " + time));
            if (afterEnd > 0) diagnostics.add(BundleDiagnostic.warning(source, afterEnd + " " + channel
                    + (afterEnd == 1 ? " keyframe is" : " keyframes are") + " after animation_length and never reached"));
        }
    }

    private static RawKeyframe rawKeyframe(JsonElement element, KeyframeNotes notes) {
        if (element.isJsonArray()) {
            return new RawKeyframe(element.getAsJsonArray(), Interpolation.LINEAR);
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (String key : object.keySet()) if (!KEYFRAME_KEYS.contains(key)) notes.keys.add(key);
            JsonElement vector = object.has("vector") ? object.get("vector")
                    : object.has("post") ? object.get("post")
                    : object.has("pre") ? object.get("pre") : null;
            if (vector == null || !vector.isJsonArray()) return null;
            Interpolation interpolation = Interpolation.LINEAR;
            if (object.has("lerp_mode") && !object.get("lerp_mode").isJsonNull()) {
                JsonElement mode = object.get("lerp_mode");
                if (mode.isJsonPrimitive() && mode.getAsJsonPrimitive().isString()
                        && LERP_MODES.contains(mode.getAsString().trim().toLowerCase(java.util.Locale.ROOT))) {
                    interpolation = Interpolation.fromId(mode.getAsString());
                } else {
                    notes.lerpModes.merge(mode.isJsonPrimitive() ? "'" + mode.getAsString() + "'" : mode.toString(), 1, Integer::sum);
                }
            }
            return new RawKeyframe(vector.getAsJsonArray(), interpolation);
        }
        return null;
    }

    private static JsonObject object(JsonObject root, String key, String source, List<BundleDiagnostic> diagnostics) {
        if (!root.has(key) || !root.get(key).isJsonObject()) {
            diagnostics.add(BundleDiagnostic.error(source, "Missing object: " + key));
            return null;
        }
        return root.getAsJsonObject(key);
    }
    private static float number(JsonObject object, String key, float fallback) {
        try { return object.get(key).getAsFloat(); } catch (Exception ignored) { return fallback; }
    }
}
