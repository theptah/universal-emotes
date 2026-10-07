package com.ptah.event.type;
import com.ptah.event.EmoteEvent;
import net.minecraft.resources.ResourceLocation;

public record SetSkinEvent(float time, ResourceLocation assetId, String model) implements EmoteEvent {
    public String type() { return "set_skin"; }
}
