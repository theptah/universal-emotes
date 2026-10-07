package com.ptah.event;

import com.ptah.animation.Vec3;
import com.ptah.event.type.RenderModelEvent;
import com.ptah.playback.MusicHandle;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public interface EventSink {
    MusicHandle playMusic(UUID playerId, ResourceLocation assetId, float volume, double offsetSeconds);

    default MusicHandle playMusic(UUID playerId, ResourceLocation assetId, float volume, double offsetSeconds, boolean dmca) {
        return playMusic(playerId, assetId, volume, offsetSeconds);
    }

    default boolean dmcaMode() { return false; }
    void playSound(UUID playerId, ResourceLocation assetId, float volume, float pitch);

    default MusicHandle playSoundTracked(UUID playerId, ResourceLocation assetId, float volume, float pitch) {
        playSound(playerId, assetId, volume, pitch);
        return null;
    }
    void spawnParticle(UUID playerId, String bone, ResourceLocation assetId, int amount, Vec3 pos, Vec3 rot);
    void setSkin(UUID playerId, ResourceLocation assetId, String model);
    void resetSkin(UUID playerId);
    void renderModel(UUID playerId, RenderModelEvent model);
}
