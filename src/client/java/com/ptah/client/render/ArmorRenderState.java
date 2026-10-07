package com.ptah.client.render;

public final class ArmorRenderState {
    private static final ThreadLocal<Boolean> RENDERING_ARMOR = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ArmorRenderState() { }

    public static void set(boolean value) {
        RENDERING_ARMOR.set(value);
    }

    public static void clear() {
        RENDERING_ARMOR.set(Boolean.FALSE);
    }

    public static boolean isRenderingArmor() {
        return RENDERING_ARMOR.get();
    }
}
