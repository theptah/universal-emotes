package com.ptah.playback;

import net.minecraft.resources.ResourceLocation;
public interface MusicHandle {
    boolean isPlaying();
    void stop();

    default boolean queueNext(ResourceLocation assetId, float volume) { return false; }

    default boolean queueNext(ResourceLocation assetId, float volume, boolean dmca) { return queueNext(assetId, volume); }
}
