package com.ptah.event.type;

import com.ptah.event.EmoteEvent;
import net.minecraft.resources.ResourceLocation;

public record PlaySoundEvent(float time, ResourceLocation assetId, float volume, float pitch, boolean dmca, ResourceLocation dmcaAlt)
        implements EmoteEvent {
    public PlaySoundEvent(float time, ResourceLocation assetId, float volume, float pitch) { this(time, assetId, volume, pitch, false, null); }
    public String type() { return "play_sound"; }
}
