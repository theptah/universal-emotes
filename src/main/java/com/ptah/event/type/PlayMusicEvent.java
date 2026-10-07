package com.ptah.event.type;

import com.ptah.event.EmoteEvent;
import net.minecraft.resources.ResourceLocation;

public record PlayMusicEvent(float time, ResourceLocation assetId, float volume, boolean dmca, ResourceLocation dmcaAlt)
        implements EmoteEvent {
    public PlayMusicEvent(float time, ResourceLocation assetId, float volume) { this(time, assetId, volume, false, null); }
    public String type() { return "play_music"; }
}
