package com.ptah.client.playback;

import com.ptah.playback.MusicHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;

final class EmoteSoundHandle implements MusicHandle {
    private static final long STARTUP_GRACE_NANOS = 500_000_000L;

    private final SoundManager manager;
    private final SoundInstance sound;
    private final long startedNanos = System.nanoTime();
    private boolean stopped;

    EmoteSoundHandle(Minecraft client, SoundInstance sound) {
        this.manager = client.getSoundManager();
        this.sound = sound;
    }

    @Override public boolean isPlaying() {
        if (stopped) return false;
        if (System.nanoTime() - startedNanos < STARTUP_GRACE_NANOS) return true;
        return manager.isActive(sound);
    }

    @Override public void stop() {
        stopped = true;
        manager.stop(sound);
    }
}
