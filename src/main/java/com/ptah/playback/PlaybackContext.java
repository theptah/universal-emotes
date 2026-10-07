package com.ptah.playback;

import com.ptah.event.EventSink;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlaybackContext {
    private final UUID playerId;
    private final EventSink sink;

    private final boolean suppressMusic;
    private MusicHandle music;

    private ResourceLocation pendingMusic;
    private float pendingVolume;
    private boolean pendingDmca;
    private double currentSeconds;

    private final List<MusicHandle> sounds = new ArrayList<>();
    public PlaybackContext(UUID playerId, EventSink sink, boolean suppressMusic) {
        this.playerId = playerId; this.sink = sink; this.suppressMusic = suppressMusic;
    }
    public UUID playerId() { return playerId; }
    public EventSink sink() { return sink; }
    public boolean suppressMusic() { return suppressMusic; }

    public double currentSeconds() { return currentSeconds; }
    public boolean hasPlayingMusic() {
        if (music != null && !music.isPlaying()) music = null;
        return music != null;
    }
    public void setMusic(MusicHandle music) { this.music = music == null || !music.isPlaying() ? null : music; }

    public void queueMusic(ResourceLocation assetId, float volume) { queueMusic(assetId, volume, false); }

    public void queueMusic(ResourceLocation assetId, float volume, boolean dmca) {
        if (music != null && music.queueNext(assetId, volume, dmca)) { pendingMusic = null; return; }
        this.pendingMusic = assetId; this.pendingVolume = volume; this.pendingDmca = dmca;
    }

    public void addSound(MusicHandle sound) {
        if (sound != null) sounds.add(sound);
    }

    public void tick(double currentSeconds) {
        this.currentSeconds = currentSeconds;
        sounds.removeIf(sound -> !sound.isPlaying());
        if (music != null && !music.isPlaying()) music = null;
        if (music == null && pendingMusic != null && !suppressMusic) {
            ResourceLocation next = pendingMusic;
            pendingMusic = null;
            setMusic(sink.playMusic(playerId, next, pendingVolume, 0.0, pendingDmca));
        }
    }
    public void cleanup() {
        pendingMusic = null;
        if (music != null) { music.stop(); music = null; }
        for (MusicHandle sound : sounds) sound.stop();
        sounds.clear();
    }
}
