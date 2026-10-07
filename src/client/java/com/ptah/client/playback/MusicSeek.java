package com.ptah.client.playback;

import com.ptah.UniversalEmotesMod;
import com.ptah.client.mixin.ChannelAccessor;
import com.ptah.client.mixin.SoundEngineAccessor;
import com.ptah.client.mixin.SoundManagerAccessor;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundManager;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public final class MusicSeek {
    private static final Map<SoundInstance, Request> PENDING = new IdentityHashMap<>();
    private MusicSeek() { }

    public static void seekWhenReady(SoundManager manager, SoundInstance sound, double seconds) {
        if (seconds <= 0.02) return;
        PENDING.put(sound, new Request(manager, (float) seconds, 80));
    }

    public static void tick() {
        Iterator<Map.Entry<SoundInstance, Request>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SoundInstance, Request> entry = iterator.next();
            Request request = entry.getValue();
            SoundEngineAccessor engine = (SoundEngineAccessor) ((SoundManagerAccessor) request.manager).universalEmotes$getSoundEngine();
            ChannelAccess.ChannelHandle handle = engine.universalEmotes$getInstanceToChannel().get(entry.getKey());
            if (handle != null) {
                float offset = request.seconds;
                handle.execute(channel -> AL10.alSourcef(((ChannelAccessor) channel).universalEmotes$getSource(), AL11.AL_SEC_OFFSET, offset));
                iterator.remove();
            } else if (--request.retries <= 0) {
                iterator.remove();
                UniversalEmotesMod.LOGGER.warn("MUSIC SEEK channel was not created");
            }
        }
    }

    public static void cancel(SoundInstance sound) { PENDING.remove(sound); }

    private static final class Request {
        final SoundManager manager; final float seconds; int retries;
        Request(SoundManager manager, float seconds, int retries) { this.manager = manager; this.seconds = seconds; this.retries = retries; }
    }
}
