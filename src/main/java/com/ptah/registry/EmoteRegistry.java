package com.ptah.registry;

import com.ptah.bundle.EmoteDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class EmoteRegistry {
    private final Map<ResourceLocation, EmoteDefinition> entries = new LinkedHashMap<>();
    public boolean register(EmoteDefinition emote) { return entries.putIfAbsent(emote.id(), emote) == null; }
    public void put(EmoteDefinition emote) { entries.put(emote.id(), emote); }
    public Optional<EmoteDefinition> get(ResourceLocation id) { return Optional.ofNullable(entries.get(id)); }
    public Collection<EmoteDefinition> values() { return java.util.List.copyOf(entries.values()); }
    public void clear() { entries.clear(); }
}
