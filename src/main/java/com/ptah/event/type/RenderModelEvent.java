package com.ptah.event.type;
import com.ptah.animation.Interpolation;
import com.ptah.animation.Interpolations;
import com.ptah.animation.Vec3;
import com.ptah.event.EmoteEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record RenderModelEvent(float time, ResourceLocation assetId, String texture, String attachMode,
                              String bone, Vec3 pos, Vec3 rot, float scale, Float end,
                              List<ModelKeyframe> keyframes) implements EmoteEvent {
    public record ModelKeyframe(float time, Vec3 pos, Vec3 rot, float scale, Interpolation interpolation) { }

    public record Pose(float px, float py, float pz, float rx, float ry, float rz, float scale) {
        public boolean visible() { return scale > 1.0e-4f; }
    }

    private static final Vec3 ZERO = new Vec3(0f, 0f, 0f);

    public RenderModelEvent {
        keyframes = keyframes == null ? List.of() : List.copyOf(keyframes);
    }

    public String type() { return "render_model"; }

    public boolean animated() { return keyframes.size() > 1; }

    public Pose sample(float seconds) {
        if (!animated()) {
            Vec3 p = pos == null ? ZERO : pos, r = rot == null ? ZERO : rot;
            return new Pose(p.x(), p.y(), p.z(), r.x(), r.y(), r.z(), scale < 0 ? 1f : scale);
        }
        List<ModelKeyframe> k = keyframes;
        int n = k.size();
        if (seconds <= k.get(0).time()) return pose(k.get(0));
        if (seconds >= k.get(n - 1).time()) return pose(k.get(n - 1));
        int i = 0;
        while (i < n - 2 && k.get(i + 1).time() <= seconds) i++;
        ModelKeyframe a = k.get(i), b = k.get(i + 1);
        float span = b.time() - a.time();
        float alpha = span > 1e-6f ? Math.min(1f, Math.max(0f, (seconds - a.time()) / span)) : 1f;
        Interpolation mode = a.interpolation() == null ? Interpolation.LINEAR : a.interpolation();
        switch (mode) {
            case STEP:
                return pose(a);
            case CATMULLROM: {
                ModelKeyframe p0 = i > 0 ? k.get(i - 1) : a;
                ModelKeyframe p3 = i + 2 < n ? k.get(i + 2) : b;
                Vec3 pp = Vec3.catmullRom(v(p0.pos()), v(a.pos()), v(b.pos()), v(p3.pos()), alpha);
                Vec3 rr = Vec3.catmullRom(v(p0.rot()), v(a.rot()), v(b.rot()), v(p3.rot()), alpha);
                float s = Interpolations.catmullRom(sc(p0), sc(a), sc(b), sc(p3), alpha);
                return new Pose(pp.x(), pp.y(), pp.z(), rr.x(), rr.y(), rr.z(), Math.max(0f, s));
            }
            default: {
                Vec3 pp = Vec3.lerp(v(a.pos()), v(b.pos()), alpha);
                Vec3 rr = Vec3.lerp(v(a.rot()), v(b.rot()), alpha);
                float s = Interpolations.lerp(sc(a), sc(b), alpha);
                return new Pose(pp.x(), pp.y(), pp.z(), rr.x(), rr.y(), rr.z(), Math.max(0f, s));
            }
        }
    }

    private static Vec3 v(Vec3 value) { return value == null ? ZERO : value; }
    private static float sc(ModelKeyframe f) { return f.scale() < 0 ? 1f : f.scale(); }
    private static Pose pose(ModelKeyframe f) {
        Vec3 p = v(f.pos()), r = v(f.rot());
        return new Pose(p.x(), p.y(), p.z(), r.x(), r.y(), r.z(), sc(f));
    }
}
