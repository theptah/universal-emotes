package com.ptah.client.screen;

import com.ptah.animation.AnimationClip;
import com.ptah.bundle.EmoteDefinition;

public final class EmoteWheelPreview {
    private static EmoteDefinition hovered;
    private static long startNanos;
    private static boolean rendering;

    private EmoteWheelPreview() {
    }

    public static void setHovered(EmoteDefinition emote) {
        if (emote == null) {
            hovered = null;
            return;
        }
        if (hovered == null || !hovered.id().equals(emote.id())) {
            startNanos = System.nanoTime();
        }
        hovered = emote;
    }

    public static void clear() {
        hovered = null;
    }

    public static EmoteDefinition hovered() {
        return hovered;
    }

    public static void beginRender() {
        rendering = true;
    }

    public static void endRender() {
        rendering = false;
    }

    public static boolean isRendering() {
        return rendering;
    }

    public static float timeFor(AnimationClip clip) {
        float length = clip.length();
        if (length <= 0f) {
            return 0f;
        }
        float elapsed = (System.nanoTime() - startNanos) / 1_000_000_000.0f;
        return clip.localTime(elapsed);
    }
}
