package com.ptah.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.ptah.client.render.FirstPersonEmoteState;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class SelfRenderMixin {
    @ModifyExpressionValue(
            //? if >=1.21.9 {
            /*method = "extractVisibleEntities",
            *///?} elif >=1.21.2 {
            /*method = "collectVisibleEntities",
            *///?} else
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"))
    private boolean universalEmotes$forceSelfRender(boolean original) {
        return original || FirstPersonEmoteState.isActive();
    }
}
