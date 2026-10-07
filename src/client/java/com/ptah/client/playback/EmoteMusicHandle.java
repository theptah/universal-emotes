package com.ptah.client.playback;

import com.ptah.client.compat.ClientCompat;
import com.ptah.UniversalEmotesMod;
import com.ptah.bundle.OggInfo;
import com.ptah.client.config.ClientConfigManager;
import com.ptah.client.mixin.ChannelAccessor;
import com.ptah.client.mixin.SoundEngineAccessor;
import com.ptah.client.mixin.SoundManagerAccessor;
import com.ptah.playback.MusicHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EmoteMusicHandle implements MusicHandle {
    private static final double LATENCY_STATIC = 0.010;

    private static final double LATENCY_STREAM = 0.060;

    private static final double SEEK_IF_LATE = 0.030;

    private static final double MAX_CLOCK_DISAGREEMENT = 0.5;

    private static final double END_MARGIN = 0.25;

    private static final List<EmoteMusicHandle> LIVE = new ArrayList<>();
    private static final Map<ResourceLocation, TrackInfo> TRACKS = new HashMap<>();
    private static long lastFrameNanos = System.nanoTime();
    private static double frameSeconds = 1.0 / 60.0;

    private final Minecraft client;
    private final SoundManager manager;
    private final UUID playerId;
    private final float volume;
    private ResourceLocation assetId;
    private double timelineOriginSeconds;
    private SoundInstance sound;
    private TrackInfo track;
    private int startupGraceTicks;
    private boolean stopped;
    private boolean muted;

    private volatile double anchorNanos = Double.NaN;
    private volatile long lastPollNanos;

    private volatile long pollSentNanos;

    private ResourceLocation nextId;
    private float nextVolume;

    private boolean dmca;
    private boolean nextDmca;
    private float currentVolume = Float.NaN;

    EmoteMusicHandle(Minecraft client, UUID playerId, ResourceLocation assetId, float volume, double initialOffset, boolean dmca) {
        this.client = client; this.manager = client.getSoundManager(); this.playerId = playerId;
        this.assetId = assetId; this.volume = volume; this.dmca = dmca;
        this.timelineOriginSeconds = gameSeconds() - Math.max(0.0, initialOffset);
        startAt(Math.max(0.0, initialOffset));
        LIVE.add(this);
    }

    @Override public boolean queueNext(ResourceLocation id, float vol) { return queueNext(id, vol, false); }

    @Override public boolean queueNext(ResourceLocation id, float vol, boolean isDmca) {
        if (stopped) return false;
        nextId = id; nextVolume = vol; nextDmca = isDmca;
        prewarm(client, id);
        return true;
    }

    @Override public boolean isPlaying() {
        if (stopped) return false;
        if (ClientConfigManager.get().muteEmoteSounds || (dmca && ClientConfigManager.get().dmcaMode)) {
            if (sound != null && manager.isActive(sound)) { MusicSeek.cancel(sound); manager.stop(sound); }
            muted = true;
            return true;
        }
        if (!audible()) { muted = true; return true; }
        if (muted) {
            muted = false;
            if (sound == null || !manager.isActive(sound)) startAt(Math.max(0.0, gameSeconds() - timelineOriginSeconds));
            return true;
        }
        if (nextId != null) return true;
        if (startupGraceTicks-- > 0) return true;

        double left = remainingByClock();
        if (!Double.isInfinite(left) && left > END_MARGIN) return true;
        boolean active = sound != null && manager.isActive(sound);
        if (!active) LIVE.remove(this);
        return active;
    }

    @Override public void stop() {
        if (stopped) return;
        stopped = true; nextId = null;
        LIVE.remove(this);
        if (sound != null) { MusicSeek.cancel(sound); manager.stop(sound); }
    }

    public static void frameAll() {
        long now = System.nanoTime();
        double dt = (now - lastFrameNanos) / 1e9;
        lastFrameNanos = now;
        if (dt > 0 && dt < 0.5) frameSeconds = frameSeconds * 0.9 + dt * 0.1;
        for (EmoteMusicHandle handle : new ArrayList<>(LIVE)) handle.frame(now);
    }

    static void prewarm(Minecraft client, ResourceLocation id) {
        TrackInfo info = info(client, id);
        if (info == null || info.streamed) return;
        try {
            SoundEngineAccessor engine = (SoundEngineAccessor) ((SoundManagerAccessor) client.getSoundManager()).universalEmotes$getSoundEngine();
            engine.universalEmotes$getSoundBuffers().getCompleteBuffer(info.path);
        } catch (RuntimeException e) {
            UniversalEmotesMod.LOGGER.debug("Music prewarm failed for {}", id, e);
        }
    }

    public static void clearCache() { TRACKS.clear(); }

    private void frame(long now) {
        if (stopped) return;
        if (sound == null || muted) {
            if (nextId != null && track != null && !Double.isNaN(track.duration)
                    && gameSeconds() - timelineOriginSeconds >= track.duration) advance(null, 0.0);
            return;
        }
        poll(now);
        if (nextId == null) return;
        double anchor = anchorNanos;
        if (track != null && !Double.isNaN(track.duration) && !Double.isNaN(anchor)) {
            double end = anchor + track.duration * 1e9;
            double clockEnd = now + remainingByClock() * 1e9;
            if (end < clockEnd - MAX_CLOCK_DISAGREEMENT * 1e9) end = clockEnd;
            TrackInfo nextInfo = info(client, nextId);
            double latency = nextInfo != null && nextInfo.streamed ? LATENCY_STREAM : LATENCY_STATIC;

            if (now + frameSeconds * 0.5e9 >= end - latency * 1e9) {
                double late = (now + latency * 1e9 - end) / 1e9;
                advance(nextId, late);
            }
        } else if (startupGraceTicks <= 0 && !manager.isActive(sound)) {
            double left = remainingByClock();
            if (Double.isInfinite(left) || left <= END_MARGIN) advance(nextId, 0.0);
        }
    }

    private void advance(ResourceLocation id, double lateSeconds) {
        ResourceLocation next = nextId;
        float vol = nextVolume;

        double oldLeft = remainingByClock();
        if (id != null && sound != null && !Double.isInfinite(oldLeft) && oldLeft > END_MARGIN) {
            MusicSeek.cancel(sound);
            manager.stop(sound);
        }
        dmca = nextDmca;
        nextId = null;

        assetId = next;
        timelineOriginSeconds = gameSeconds() - Math.max(0.0, lateSeconds);
        if (id == null) { sound = null; track = info(client, next); anchorNanos = Double.NaN; return; }
        currentVolume = vol;
        startAt(lateSeconds > SEEK_IF_LATE ? lateSeconds : 0.0);
    }

    private void startAt(double offset) {
        float vol = Float.isNaN(currentVolume) ? volume : currentVolume;
        sound = ClientPlaybackHooks.soundInstance(client, playerId, assetId, SoundSource.RECORDS, vol, 1.0f);
        track = info(client, assetId);
        anchorNanos = Double.NaN;
        pollSentNanos = 0;
        lastPollNanos = 0;
        manager.play(sound);
        MusicSeek.seekWhenReady(manager, sound, offset);
        startupGraceTicks = 20;
    }

    private void poll(long now) {
        if (pollSentNanos != 0 && now - pollSentNanos < 250_000_000L) return;
        SoundEngineAccessor engine = (SoundEngineAccessor) ((SoundManagerAccessor) manager).universalEmotes$getSoundEngine();
        ChannelAccess.ChannelHandle handle = engine.universalEmotes$getInstanceToChannel().get(sound);
        if (handle == null) return;
        boolean streamed = track != null && track.streamed;
        pollSentNanos = now;
        handle.execute(channel -> {
            try {
                int source = ((ChannelAccessor) channel).universalEmotes$getSource();
                int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                long t = System.nanoTime();
                if (state == AL10.AL_PLAYING) {
                    if (!streamed || Double.isNaN(anchorNanos)) {
                        float pos = AL10.alGetSourcef(source, AL11.AL_SEC_OFFSET);
                        anchorNanos = t - pos * 1e9;
                    }
                } else if (state == AL10.AL_PAUSED && !Double.isNaN(anchorNanos) && lastPollNanos != 0) {
                    anchorNanos = anchorNanos + (t - lastPollNanos);
                }
                lastPollNanos = t;
            } finally {
                pollSentNanos = 0;
            }
        });
    }

    private double gameSeconds() { return client.level == null ? 0.0 : client.level.getGameTime() / 20.0; }

    private double remainingByClock() {
        if (track == null || Double.isNaN(track.duration)) return Double.POSITIVE_INFINITY;
        double now = client.level == null ? 0.0 : (client.level.getGameTime() + ClientCompat.partialTick()) / 20.0;
        return track.duration - (now - timelineOriginSeconds);
    }

    private boolean audible() {
        return client.options.getSoundSourceVolume(SoundSource.MASTER) > 0.0001f
                && client.options.getSoundSourceVolume(SoundSource.RECORDS) > 0.0001f;
    }

    private record TrackInfo(ResourceLocation path, boolean streamed, double duration) { }

    private static TrackInfo info(Minecraft client, ResourceLocation id) {
        if (id == null) return null;
        TrackInfo cached = TRACKS.get(id);
        if (cached != null) return cached;
        WeighedSoundEvents events = client.getSoundManager().getSoundEvent(id);
        if (events == null) return null;
        Sound s = events.getSound(RandomSource.create());
        if (s == null) return null;
        double duration = Double.NaN;
        Optional<Resource> resource = client.getResourceManager().getResource(s.getPath());
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open()) {
                duration = OggInfo.durationSeconds(in);
            } catch (Exception e) {
                UniversalEmotesMod.LOGGER.debug("Could not read the length of {}", id, e);
            }
        }
        TrackInfo info = new TrackInfo(s.getPath(), s.shouldStream(), duration);
        TRACKS.put(id, info);
        return info;
    }
}
