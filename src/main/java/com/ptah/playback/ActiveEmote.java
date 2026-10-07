package com.ptah.playback;

import com.ptah.bundle.EmoteDefinition;
import com.ptah.event.EmoteEvent;
import com.ptah.event.EventDispatcher;

import java.util.UUID;

public final class ActiveEmote {
    private final UUID playerId;
    private final EmoteDefinition emote;
    private final long startTick;
    private final PlaybackContext context;
    private final EventDispatcher dispatcher = new EventDispatcher();
    private double previousSeconds = -0.000001;

    public ActiveEmote(UUID playerId, EmoteDefinition emote, long startTick, PlaybackHooks hooks, boolean suppressMusic) {
        this.playerId = playerId; this.emote = emote; this.startTick = startTick;
        this.context = new PlaybackContext(playerId, hooks, suppressMusic);
    }
    public UUID playerId() { return playerId; }
    public EmoteDefinition emote() { return emote; }
    public float localAnimationTime(double gameTick) {
        return emote.animation().localTime(Math.max(0, (gameTick - startTick) / 20.0));
    }
    public boolean tick(long gameTick) {
        double current = Math.max(0, (gameTick - startTick) / 20.0);

        context.tick(emote.animation().localTime(current));
        for (EmoteEvent event : emote.events().crossed(previousSeconds, current, emote.animation())) {
            dispatcher.dispatch(event, context);
        }
        previousSeconds = current;
        return emote.animation().loop() || current < emote.animation().length();
    }
    public void cleanup() { context.cleanup(); }
}
