package com.ptah.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ptah.client.playback.ClientPlaybackRuntime;
import com.ptah.client.render.EmoteVisibilityState;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

//? if >=1.21.4 {
/*import com.ptah.client.render.EmoteRenderState;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
*///?} elif >=1.21.2 {
/*import com.ptah.client.render.EmoteRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
*///?} else {
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
//?}
//? if >=1.21.9 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else
import net.minecraft.client.renderer.MultiBufferSource;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandVisibilityMixin {
    //? if >=1.21.9 {
    /*@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void universalEmotes$hideHeldItemsDuringEmote(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                                                          ArmedEntityRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (universalEmotes$shouldHide(((EmoteRenderState) state).universalEmotes$playerId())) ci.cancel();
    }
    *///?} elif >=1.21.4 {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void universalEmotes$hideHeldItemsDuringEmote(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                          ArmedEntityRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (universalEmotes$shouldHide(((EmoteRenderState) state).universalEmotes$playerId())) ci.cancel();
    }
    *///?} elif >=1.21.2 {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void universalEmotes$hideHeldItemsDuringEmote(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                          LivingEntityRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (universalEmotes$shouldHide(((EmoteRenderState) state).universalEmotes$playerId())) ci.cancel();
    }
    *///?} else {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void universalEmotes$hideHeldItemsDuringEmote(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                          LivingEntity entity, float limbSwing, float limbSwingAmount,
                                                          float partialTicks, float ageInTicks, float netHeadYaw,
                                                          float headPitch, CallbackInfo ci) {
        if (entity instanceof Player && universalEmotes$shouldHide(entity.getUUID())) ci.cancel();
    }
    //?}

    @Unique
    private static boolean universalEmotes$shouldHide(UUID playerId) {
        return playerId != null
                && EmoteVisibilityState.hideHeldItems(playerId)
                && ClientPlaybackRuntime.MANAGER.active(playerId).isPresent();
    }
}
