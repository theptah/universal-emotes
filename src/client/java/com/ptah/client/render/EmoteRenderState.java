package com.ptah.client.render;

import java.util.UUID;

public interface EmoteRenderState {
    UUID universalEmotes$playerId();

    boolean universalEmotes$isLocalPlayer();

    boolean universalEmotes$isPreview();

    boolean universalEmotes$isSlim();

    void universalEmotes$capture(UUID playerId, boolean localPlayer, boolean preview, boolean slim);
}
