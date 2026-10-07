package com.ptah.client.mixin;

import com.ptah.client.render.FirstPersonEmoteState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
//? if >=1.21.9 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class HandRenderMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void universalEmotes$hideHandsDuringEmote(float partialTicks, PoseStack poseStack,
                                                 //? if >=1.21.9 {
                                                 /*SubmitNodeCollector buffer,
                                                 *///?} else
                                                 MultiBufferSource.BufferSource buffer,
                                                 LocalPlayer player, int combinedLight, CallbackInfo callbackInfo) {
        if (FirstPersonEmoteState.isActive()) {
            callbackInfo.cancel();
        }
    }
}
