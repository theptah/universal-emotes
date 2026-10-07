package com.ptah.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ptah.client.render.R15Model;
import com.ptah.client.render.R15RenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.21.2 {
/*@Mixin(net.minecraft.client.model.Model.class)
*///?} else
@Mixin(net.minecraft.client.model.AgeableListModel.class)
public abstract class AgeableModelRenderMixin {
    //? if >=1.21 {
    /*@Inject(method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"), cancellable = true)
    private void universalEmotes$renderR15(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                                           int color, CallbackInfo ci) {
        universalEmotes$render(poseStack, consumer, light, overlay, ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f, (color & 0xFF) / 255.0f, ((color >>> 24) & 0xFF) / 255.0f, ci);
    }
    *///?} else {
    @Inject(method = "renderToBuffer", at = @At("HEAD"), cancellable = true)
    private void universalEmotes$renderR15(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                                           float red, float green, float blue, float alpha, CallbackInfo ci) {
        universalEmotes$render(poseStack, consumer, light, overlay, red, green, blue, alpha, ci);
    }
    //?}

    @Unique
    private void universalEmotes$render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                                        float red, float green, float blue, float alpha, CallbackInfo ci) {
        R15RenderState.Request req = R15RenderState.consume(this);
        if (req == null) return;
        R15Model.get(req.slim).render(req.clip, req.time, req.hideHead, poseStack, consumer, light, overlay, red, green, blue, alpha);
        ci.cancel();
    }
}
