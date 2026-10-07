package com.ptah.playback;

import com.ptah.bundle.EmoteDefinition;
import com.ptah.event.EventSink;

import java.util.UUID;

public interface PlaybackHooks extends EventSink {
    default void onStart(UUID playerId, EmoteDefinition emote, long startTick, long seed) {
    }

    default void onStop(UUID playerId, EmoteDefinition emote, StopReason reason) {
    }
}
