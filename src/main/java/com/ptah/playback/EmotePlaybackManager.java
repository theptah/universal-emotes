package com.ptah.playback;

import com.ptah.registry.EmoteRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EmotePlaybackManager {
    private final EmoteRegistry registry;
    private final PlaybackHooks hooks;
    private final Map<UUID, ActiveEmote> active = new HashMap<>();

    public EmotePlaybackManager(EmoteRegistry registry, PlaybackHooks hooks) {
        this.registry = registry;
        this.hooks = hooks;
    }

    public boolean start(UUID playerId, ResourceLocation emoteId, long startTick, long seed, boolean suppressMusic) {
        var definition = registry.get(emoteId);
        if (definition.isEmpty()) return false;
        stop(playerId, StopReason.REPLACED);
        ActiveEmote instance = new ActiveEmote(playerId, definition.get(), startTick, hooks, suppressMusic);
        active.put(playerId, instance);
        hooks.onStart(playerId, definition.get(), startTick, seed);
        return true;
    }

    public void tick(long gameTick) {
        var iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveEmote emote = iterator.next().getValue();
            if (!emote.tick(gameTick)) {
                emote.cleanup();
                hooks.onStop(emote.playerId(), emote.emote(), StopReason.FINISHED);
                iterator.remove();
            }
        }
    }

    public void stop(UUID playerId, StopReason reason) {
        ActiveEmote removed = active.remove(playerId);
        if (removed != null) {
            removed.cleanup();
            hooks.onStop(removed.playerId(), removed.emote(), reason);
        }
    }

    public void stopAll(StopReason reason) {
        for (ActiveEmote value : active.values()) {
            value.cleanup();
            hooks.onStop(value.playerId(), value.emote(), reason);
        }
        active.clear();
    }

    public Optional<ActiveEmote> active(UUID playerId) {
        return Optional.ofNullable(active.get(playerId));
    }
}
