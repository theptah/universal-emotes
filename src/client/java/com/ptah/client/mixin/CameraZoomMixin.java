package com.ptah.client.mixin;

import com.ptah.client.camera.EmoteCameraController;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraZoomMixin {
    @Inject(method = "getMaxZoom", at = @At("RETURN"), cancellable = true)
    //? if >=1.21 {
    /*private void universalEmotes$scaleEmoteZoom(float startingDistance, CallbackInfoReturnable<Float> cir) {
        if (EmoteCameraController.isControllingZoom()) {
            cir.setReturnValue((float) (cir.getReturnValue() * EmoteCameraController.zoomFactor()));
        }
    }
    *///?} else {
    private void universalEmotes$scaleEmoteZoom(double startingDistance, CallbackInfoReturnable<Double> cir) {
        if (EmoteCameraController.isControllingZoom()) {
            cir.setReturnValue(cir.getReturnValue() * EmoteCameraController.zoomFactor());
        }
    }
    //?}
}
