package com.ptah.animation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BendTrack {
    private final List<BendKeyframe> keyframes;

    public BendTrack(List<BendKeyframe> keyframes) {
        List<BendKeyframe> sorted = keyframes.stream()
                .sorted(Comparator.comparingDouble(BendKeyframe::time))
                .toList();
        List<BendKeyframe> unwrapped = new ArrayList<>(sorted.size());
        float previousAxis = 0f;
        for (int i = 0; i < sorted.size(); i++) {
            BendKeyframe frame = sorted.get(i);
            float axis = frame.axis();
            if (i > 0) {
                float delta = axis - previousAxis;
                delta -= 360f * Math.round(delta / 360f);
                axis = previousAxis + delta;
            }
            previousAxis = axis;
            unwrapped.add(new BendKeyframe(frame.time(), frame.value(), axis, frame.interpolation()));
        }
        this.keyframes = List.copyOf(unwrapped);
    }

    public List<BendKeyframe> keyframes() { return keyframes; }

    public boolean isEmpty() { return keyframes.isEmpty(); }

    public BendSample sample(float time) {
        int n = keyframes.size();
        if (n == 0) return new BendSample(0f, 0f);
        BendKeyframe first = keyframes.get(0);
        if (time <= first.time()) return new BendSample(first.value(), first.axis());
        BendKeyframe last = keyframes.get(n - 1);
        if (time >= last.time()) return new BendSample(last.value(), last.axis());

        int lo = 0;
        int hi = n - 1;
        while (lo + 1 < hi) {
            int mid = (lo + hi) >>> 1;
            if (keyframes.get(mid).time() <= time) lo = mid; else hi = mid;
        }
        BendKeyframe before = keyframes.get(lo);
        BendKeyframe after = keyframes.get(lo + 1);
        if (before.interpolation() == Interpolation.STEP) {
            return new BendSample(before.value(), before.axis());
        }
        float span = after.time() - before.time();
        float alpha = span <= 0f ? 0f : (time - before.time()) / span;
        boolean catmull = before.interpolation() == Interpolation.CATMULLROM
                || after.interpolation() == Interpolation.CATMULLROM;
        if (!catmull) {
            return new BendSample(
                    Interpolations.lerp(before.value(), after.value(), alpha),
                    Interpolations.lerp(before.axis(), after.axis(), alpha));
        }
        BendKeyframe p0 = keyframes.get(Math.max(lo - 1, 0));
        BendKeyframe p3 = keyframes.get(Math.min(lo + 2, n - 1));
        return new BendSample(
                Interpolations.catmullRom(p0.value(), before.value(), after.value(), p3.value(), alpha),
                Interpolations.catmullRom(p0.axis(), before.axis(), after.axis(), p3.axis(), alpha));
    }
}
