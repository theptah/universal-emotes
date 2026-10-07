package com.ptah.animation;

import java.util.Locale;

public enum Interpolation {
    LINEAR,
    CATMULLROM,
    STEP;

    public static Interpolation fromId(String id) {
        if (id == null) return LINEAR;
        return switch (id.trim().toLowerCase(Locale.ROOT)) {
            case "catmullrom", "smooth" -> CATMULLROM;
            case "step" -> STEP;
            default -> LINEAR;
        };
    }
}
