package com.ptah.client.mixin;

//? if >=1.21.2 {
/*import com.ptah.client.render.EmoteRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements EmoteRenderState {
    @Unique private UUID universalEmotes$playerId;
    @Unique private boolean universalEmotes$localPlayer;
    @Unique private boolean universalEmotes$preview;
    @Unique private boolean universalEmotes$slim;

    @Override
    public UUID universalEmotes$playerId() {
        return universalEmotes$playerId;
    }

    @Override
    public boolean universalEmotes$isLocalPlayer() {
        return universalEmotes$localPlayer;
    }

    @Override
    public boolean universalEmotes$isPreview() {
        return universalEmotes$preview;
    }

    @Override
    public boolean universalEmotes$isSlim() {
        return universalEmotes$slim;
    }

    @Override
    public void universalEmotes$capture(UUID playerId, boolean localPlayer, boolean preview, boolean slim) {
        this.universalEmotes$playerId = playerId;
        this.universalEmotes$localPlayer = localPlayer;
        this.universalEmotes$preview = preview;
        this.universalEmotes$slim = slim;
    }
}
*///?}
