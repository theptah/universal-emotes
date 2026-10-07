package com.ptah.animation;

import java.util.Comparator;
import java.util.List;

public final class KeyframeTrack {
    private final List<Keyframe> keyframes;

    public KeyframeTrack(List<Keyframe> keyframes) {
        this.keyframes = keyframes.stream()
                .sorted(Comparator.comparingDouble(Keyframe::time))
                .toList();
    }

    public List<Keyframe> keyframes() { return keyframes; }

    public Vec3 sample(float time) {
        int n = keyframes.size();
        if (n == 0) return new Vec3(0, 0, 0);
        Keyframe first = keyframes.get(0);
        if (time <= first.time()) return first.value();
        Keyframe last = keyframes.get(n - 1);
        if (time >= last.time()) return last.value();

        int lo = 0;
        int hi = n - 1;
        while (lo + 1 < hi) {
            int mid = (lo + hi) >>> 1;
            if (keyframes.get(mid).time() <= time) lo = mid; else hi = mid;
        }
        Keyframe before = keyframes.get(lo);
        Keyframe after = keyframes.get(lo + 1);
        if (before.interpolation() == Interpolation.STEP) {
            return before.value();
        }
        float span = after.time() - before.time();
        float alpha = span <= 0f ? 0f : (time - before.time()) / span;
        boolean catmull = before.interpolation() == Interpolation.CATMULLROM
                || after.interpolation() == Interpolation.CATMULLROM;
        if (!catmull) {
            return Vec3.lerp(before.value(), after.value(), alpha);
        }
        Vec3 p0 = keyframes.get(Math.max(lo - 1, 0)).value();
        Vec3 p3 = keyframes.get(Math.min(lo + 2, n - 1)).value();
        return Vec3.catmullRom(p0, before.value(), after.value(), p3, alpha);
    }
}
