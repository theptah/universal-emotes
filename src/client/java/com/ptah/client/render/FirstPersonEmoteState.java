package com.ptah.client.render;

import com.ptah.client.playback.ClientPlaybackRuntime;
import net.minecraft.client.Minecraft;

public final class FirstPersonEmoteState {
    private FirstPersonEmoteState() {
    }

    public static boolean isActive() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }
        if (!client.options.getCameraType().isFirstPerson()) {
            return false;
        }
        return ClientPlaybackRuntime.MANAGER.active(client.player.getUUID()).isPresent();
    }
}
