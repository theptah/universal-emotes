package com.ptah.animation;

public record Vec3(float x, float y, float z) {
    public static Vec3 lerp(Vec3 from, Vec3 to, float alpha) {
        return new Vec3(from.x + (to.x - from.x) * alpha, from.y + (to.y - from.y) * alpha, from.z + (to.z - from.z) * alpha);
    }

    public static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        return new Vec3(
                Interpolations.catmullRom(p0.x, p1.x, p2.x, p3.x, t),
                Interpolations.catmullRom(p0.y, p1.y, p2.y, p3.y, t),
                Interpolations.catmullRom(p0.z, p1.z, p2.z, p3.z, t));
    }
}
