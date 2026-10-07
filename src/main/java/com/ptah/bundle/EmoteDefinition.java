package com.ptah.bundle;

import com.ptah.animation.AnimationClip;
import com.ptah.event.EventTimeline;
import net.minecraft.resources.ResourceLocation;

public record EmoteDefinition(ResourceLocation id, ResourceLocation bundleId, String localId,
                              String animationKey, String name, String description, int op,
                              Rarity rarity, AnimationClip animation, EventTimeline events,
                              String rigType) { }
