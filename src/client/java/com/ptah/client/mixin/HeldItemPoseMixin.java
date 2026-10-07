package com.ptah.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ptah.client.compat.MathCompat;
import com.ptah.client.render.HeldItemPose;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

//? if >=1.21.9 {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState;
*///?}

@Mixin(ItemInHandLayer.class)
public abstract class HeldItemPoseMixin {
    //? if >=1.21.9 {
    /*@WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/ArmedModel;translateToHand(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void universalEmotes$followItemBone(ArmedModel model, EntityRenderState state, HumanoidArm arm,
                                                PoseStack poseStack, Operation<Void> original) {
        original.call(model, state, arm, poseStack);
        universalEmotes$applyItemBone(model, arm, poseStack);
    }
    *///?} else {
    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/ArmedModel;translateToHand(Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void universalEmotes$followItemBone(ArmedModel model, HumanoidArm arm, PoseStack poseStack,
                                                Operation<Void> original) {
        original.call(model, arm, poseStack);
        universalEmotes$applyItemBone(model, arm, poseStack);
    }
    //?}

    @Unique
    private static void universalEmotes$applyItemBone(Object model, HumanoidArm arm, PoseStack poseStack) {
        if (!(model instanceof HeldItemPose pose)) return;
        Matrix4f delta = pose.universalEmotes$heldItemDelta(arm == HumanoidArm.RIGHT);
        if (delta != null) MathCompat.mul(poseStack, delta);
    }
}
