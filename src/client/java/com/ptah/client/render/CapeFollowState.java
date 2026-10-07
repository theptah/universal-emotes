package com.ptah.client.render;

import org.joml.Matrix4f;

public final class CapeFollowState {
    private static final ThreadLocal<Matrix4f> DELTA = new ThreadLocal<>();

    private CapeFollowState() { }

    public static void set(Matrix4f delta) {
        if (delta == null) { DELTA.remove(); return; }
        DELTA.set(delta);
    }

    public static void clear() { DELTA.remove(); }

    public static Matrix4f get() { return DELTA.get(); }
}
