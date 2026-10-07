package com.ptah.network;

import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ServerEmoteState {
    public record Entry(ResourceLocation emoteId, long startTick, long seed, ResourceLocation dimension,
                        boolean suppressMusic, UUID partner) {
        public Entry(ResourceLocation emoteId, long startTick, long seed, ResourceLocation dimension) {
            this(emoteId, startTick, seed, dimension, false, null);
        }
    }
    private final Map<UUID, Entry> active = new HashMap<>();
    public void start(UUID player, Entry entry) { active.put(player, entry); }
    public Entry stop(UUID player) { return active.remove(player); }
    public Map<UUID, Entry> entries() { return active; }
}
