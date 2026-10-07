package com.ptah.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ptah.client.compat.MathCompat;
import com.ptah.client.render.CapeFollowState;
import com.ptah.client.screen.EmoteWheelPreview;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.21.9 {
/*import com.ptah.client.render.EmoteRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
*///?} elif >=1.21.2 {
/*import com.ptah.client.render.EmoteRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?} else {
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
//?}

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    @Unique
    private boolean universalEmotes$capePushed;

    //? if >=1.21.9 {
    /*@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void universalEmotes$capeFollowStart(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                                 AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
        universalEmotes$begin(poseStack, ((EmoteRenderState) state).universalEmotes$isPreview(), ci);
    }

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("RETURN"))
    private void universalEmotes$capeFollowEnd(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                               AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
        universalEmotes$end(poseStack);
    }
    *///?} elif >=1.21.2 {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void universalEmotes$capeFollowStart(PoseStack poseStack, MultiBufferSource buffer, int light,
                                                 PlayerRenderState state, float yRot, float xRot, CallbackInfo ci) {
        universalEmotes$begin(poseStack, ((EmoteRenderState) state).universalEmotes$isPreview(), ci);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V",
            at = @At("RETURN"))
    private void universalEmotes$capeFollowEnd(PoseStack poseStack, MultiBufferSource buffer, int light,
                                               PlayerRenderState state, float yRot, float xRot, CallbackInfo ci) {
        universalEmotes$end(poseStack);
    }
    *///?} else {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void universalEmotes$capeFollowStart(PoseStack poseStack, MultiBufferSource buffer, int light,
                                                 AbstractClientPlayer entity, float limbSwing, float limbSwingAmount,
                                                 float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
                                                 CallbackInfo ci) {
        universalEmotes$begin(poseStack, EmoteWheelPreview.isRendering(), ci);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V", at = @At("RETURN"))
    private void universalEmotes$capeFollowEnd(PoseStack poseStack, MultiBufferSource buffer, int light,
                                               AbstractClientPlayer entity, float limbSwing, float limbSwingAmount,
                                               float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
                                               CallbackInfo ci) {
        universalEmotes$end(poseStack);
    }
    //?}

    @Unique
    private void universalEmotes$begin(PoseStack poseStack, boolean preview, CallbackInfo ci) {
        universalEmotes$capePushed = false;

        if (preview) {
            ci.cancel();
            return;
        }
        Matrix4f delta = CapeFollowState.get();
        if (delta == null) return;
        poseStack.pushPose();
        MathCompat.mul(poseStack, delta);
        universalEmotes$capePushed = true;
    }

    @Unique
    private void universalEmotes$end(PoseStack poseStack) {
        if (universalEmotes$capePushed) {
            poseStack.popPose();
            universalEmotes$capePushed = false;
        }
    }
}
