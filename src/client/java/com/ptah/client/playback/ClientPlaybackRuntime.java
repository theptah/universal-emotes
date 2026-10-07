package com.ptah.client.playback;

import com.ptah.playback.EmotePlaybackManager;
import com.ptah.system.EmoteSystem;

public final class ClientPlaybackRuntime {
    public static final EmotePlaybackManager MANAGER =
            new EmotePlaybackManager(EmoteSystem.EMOTES, new ClientPlaybackHooks());

    private ClientPlaybackRuntime() {
    }
}
