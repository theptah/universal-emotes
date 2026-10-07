package com.ptah.client.mixin;

//? if <1.21.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.ptah.client.render.ArmorRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class ArmorBendSuppressMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void universalEmotes$beginArmor(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                            LivingEntity entity, float limbSwing, float limbSwingAmount,
                                            float partialTicks, float ageInTicks, float netHeadYaw,
                                            float headPitch, CallbackInfo ci) {
        ArmorRenderState.set(true);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void universalEmotes$endArmor(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                          LivingEntity entity, float limbSwing, float limbSwingAmount,
                                          float partialTicks, float ageInTicks, float netHeadYaw,
                                          float headPitch, CallbackInfo ci) {
        ArmorRenderState.clear();
    }
}
//?}
