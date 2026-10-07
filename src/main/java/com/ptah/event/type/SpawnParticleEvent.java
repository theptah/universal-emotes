package com.ptah.event.type;
import com.ptah.animation.Vec3;
import com.ptah.event.EmoteEvent;
import net.minecraft.resources.ResourceLocation;

public record SpawnParticleEvent(float time, String bone, ResourceLocation assetId, int amount, Vec3 pos, Vec3 rot) implements EmoteEvent {
    public String type() { return "spawn_particle"; }
}
