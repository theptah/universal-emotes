package com.ptah.animation;

import java.util.Map;
import java.util.function.Supplier;

public final class AnimationClip {
    private final String formatVersion;
    private final String key;
    private final boolean loop;
    private final float length;

    private final float loopStart;
    private final Supplier<Map<String, BoneAnimation>> loader;
    private volatile Map<String, BoneAnimation> bones;

    public AnimationClip(String formatVersion, String key, boolean loop, float length, float loopStart,
                         Map<String, BoneAnimation> bones) {
        this.formatVersion = formatVersion;
        this.key = key;
        this.loop = loop;
        this.length = length;
        this.loopStart = sanitizeLoopStart(loop, length, loopStart);
        this.loader = null;
        this.bones = Map.copyOf(bones);
    }

    private AnimationClip(String formatVersion, String key, boolean loop, float length, float loopStart,
                          Supplier<Map<String, BoneAnimation>> loader) {
        this.formatVersion = formatVersion;
        this.key = key;
        this.loop = loop;
        this.length = length;
        this.loopStart = sanitizeLoopStart(loop, length, loopStart);
        this.loader = loader;
        this.bones = null;
    }

    public static AnimationClip lazy(String formatVersion, String key, boolean loop, float length, float loopStart,
                                     Supplier<Map<String, BoneAnimation>> loader) {
        return new AnimationClip(formatVersion, key, loop, length, loopStart, loader);
    }

    private static float sanitizeLoopStart(boolean loop, float length, float loopStart) {
        if (!loop || !Float.isFinite(loopStart) || loopStart <= 0f || loopStart >= length - 1e-3f) return 0f;
        return loopStart;
    }

    public String formatVersion() { return formatVersion; }

    public String key() { return key; }

    public boolean loop() { return loop; }

    public float length() { return length; }

    public float loopStart() { return loopStart; }

    public float localTime(double elapsed) {
        if (elapsed <= 0) return 0f;
        if (!loop) return (float) Math.min(elapsed, length);
        if (elapsed < length) return (float) elapsed;
        double cycle = length - loopStart;
        return (float) (loopStart + (elapsed - length) % cycle);
    }

    public Map<String, BoneAnimation> bones() {
        Map<String, BoneAnimation> local = bones;
        if (local == null) {
            synchronized (this) {
                local = bones;
                if (local == null) {
                    Map<String, BoneAnimation> loaded = loader.get();
                    local = loaded == null ? Map.of() : Map.copyOf(loaded);
                    bones = local;
                }
            }
        }
        return local;
    }
}
