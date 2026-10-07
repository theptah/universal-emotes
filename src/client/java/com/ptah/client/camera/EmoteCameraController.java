package com.ptah.client.camera;

import com.ptah.client.playback.ClientPlaybackRuntime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;

public final class EmoteCameraController {
    private static final float TRANSITION_MILLIS = 250f;

    private enum Phase { IDLE, TRANSITION, HELD }

    private static Phase phase = Phase.IDLE;
    private static long transitionStartMs;
    private static float fromFactor;
    private static float toFactor;
    private static float cachedFactor = 1f;

    private static CameraType targetType = CameraType.FIRST_PERSON;

    private static boolean pendingSideSwitch;

    private static boolean restoring;

    private static boolean sessionActive;

    private static CameraType preEmotePerspective = CameraType.FIRST_PERSON;

    private static CameraType emoteExitPerspective = null;

    private EmoteCameraController() {
    }

    public static void onEmoteStart(boolean forceEveryEmote) {
        Options options = Minecraft.getInstance().options;
        if (options == null) {
            return;
        }

        if (sessionActive && !restoring) {
            return;
        }

        if (!sessionActive) {
            preEmotePerspective = options.getCameraType();
        }

        sessionActive = true;
        restoring = false;

        CameraType target;
        if (forceEveryEmote) {
            target = CameraType.THIRD_PERSON_BACK;
        } else if (emoteExitPerspective != null) {
            target = emoteExitPerspective;
        } else {
            target = CameraType.THIRD_PERSON_BACK;
        }
        transitionTo(options, target);
    }

    public static void onEmoteStop() {
        if (!sessionActive) {
            return;
        }
        Options options = Minecraft.getInstance().options;
        if (options == null) {
            finishIdle();
            return;
        }

        emoteExitPerspective = options.getCameraType();
        restoring = true;
        transitionTo(options, preEmotePerspective);
    }

    public static void clientTick() {
        if (!sessionActive || restoring) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Options options = client.options;
        LocalPlayer player = client.player;
        if (options == null || player == null) {
            return;
        }
        if (ClientPlaybackRuntime.MANAGER.active(player.getUUID()).isEmpty()) {
            emoteExitPerspective = options.getCameraType();
            restoring = true;
            transitionTo(options, preEmotePerspective);
        }
    }

    private static void transitionTo(Options options, CameraType target) {
        targetType = target;
        pendingSideSwitch = false;
        CameraType current = options.getCameraType();
        if (current == target) {
            settleAt(target);
            if (restoring) {
                finishIdle();
            }
            return;
        }
        boolean currentThird = isThirdPerson(current);
        boolean targetThird = isThirdPerson(target);
        if (!currentThird && targetThird) {
            options.setCameraType(target);
            startGrow();
        } else if (currentThird && !targetThird) {
            startShrink();
        } else {
            pendingSideSwitch = true;
            startShrink();
        }
    }

    private static void startGrow() {
        fromFactor = 0f;
        toFactor = 1f;
        cachedFactor = 0f;
        transitionStartMs = System.currentTimeMillis();
        phase = Phase.TRANSITION;
    }

    private static void startShrink() {
        fromFactor = cachedFactor;
        toFactor = 0f;
        transitionStartMs = System.currentTimeMillis();
        phase = Phase.TRANSITION;
    }

    private static void settleAt(CameraType type) {
        phase = Phase.HELD;
        cachedFactor = isThirdPerson(type) ? 1f : 0f;
    }

    private static void finishIdle() {
        phase = Phase.IDLE;
        sessionActive = false;
        restoring = false;
        pendingSideSwitch = false;
        cachedFactor = isThirdPerson(targetType) ? 1f : 0f;
    }

    private static boolean isThirdPerson(CameraType type) {
        return type == CameraType.THIRD_PERSON_BACK || type == CameraType.THIRD_PERSON_FRONT;
    }

    public static boolean isControllingZoom() {
        return phase == Phase.TRANSITION;
    }

    public static float zoomFactor() {
        if (phase != Phase.TRANSITION) {
            return phase == Phase.HELD ? (isThirdPerson(targetType) ? 1f : 0f) : cachedFactor;
        }
        float t = (System.currentTimeMillis() - transitionStartMs) / TRANSITION_MILLIS;
        if (t < 1f) {
            cachedFactor = fromFactor + (toFactor - fromFactor) * easeInOut(t);
            return cachedFactor;
        }

        cachedFactor = toFactor;
        if (pendingSideSwitch) {
            pendingSideSwitch = false;
            Options options = Minecraft.getInstance().options;
            if (options != null) {
                options.setCameraType(targetType);
            }
            startGrow();
            return cachedFactor;
        }
        if (!isThirdPerson(targetType)) {
            Options options = Minecraft.getInstance().options;
            if (options != null) {
                options.setCameraType(targetType);
            }
        }
        if (restoring) {
            finishIdle();
        } else {
            settleAt(targetType);
        }
        return cachedFactor;
    }

    private static float easeInOut(float t) {
        return t < 0.5f
                ? 2f * t * t
                : 1f - (float) Math.pow(-2f * t + 2f, 2.0) / 2f;
    }
}
