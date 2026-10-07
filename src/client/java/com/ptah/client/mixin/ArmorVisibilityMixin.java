package com.ptah.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ptah.client.playback.ClientPlaybackRuntime;
import com.ptah.client.render.EmoteVisibilityState;
import com.ptah.client.screen.EmoteWheelPreview;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

//? if >=1.21.2 {
/*import com.ptah.client.render.EmoteRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
*///?} else {
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
//?}
//? if >=1.21.9 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else
import net.minecraft.client.renderer.MultiBufferSource;

@Mixin(HumanoidArmorLayer.class)
public abstract class ArmorVisibilityMixin {
    //? if >=1.21.9 {
    /*@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void universalEmotes$hideArmorDuringEmote(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                                                      HumanoidRenderState state, float yRot, float xRot, CallbackInfo ci) {
        EmoteRenderState data = (EmoteRenderState) state;
        if (universalEmotes$shouldHide(data.universalEmotes$isPreview(), data.universalEmotes$playerId())) ci.cancel();
    }
    *///?} elif >=1.21.2 {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void universalEmotes$hideArmorDuringEmote(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                      HumanoidRenderState state, float yRot, float xRot, CallbackInfo ci) {
        EmoteRenderState data = (EmoteRenderState) state;
        if (universalEmotes$shouldHide(data.universalEmotes$isPreview(), data.universalEmotes$playerId())) ci.cancel();
    }
    *///?} else {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void universalEmotes$hideArmorDuringEmote(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                      LivingEntity entity, float limbSwing, float limbSwingAmount,
                                                      float partialTicks, float ageInTicks, float netHeadYaw,
                                                      float headPitch, CallbackInfo ci) {
        if (universalEmotes$shouldHide(EmoteWheelPreview.isRendering(),
                entity instanceof Player ? entity.getUUID() : null)) {
            ci.cancel();
        }
    }
    //?}

    @Unique
    private static boolean universalEmotes$shouldHide(boolean preview, UUID playerId) {
        if (preview) return true;
        return playerId != null
                && EmoteVisibilityState.hideArmor(playerId)
                && ClientPlaybackRuntime.MANAGER.active(playerId).isPresent();
    }
}
