package com.ptah.client.mixin;

//? if >=1.21.6 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ptah.client.screen.EmoteWheelPreview;
import com.ptah.client.screen.PreviewEffectsPip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsPreviewMixin {
    @WrapOperation(method = "submitEntityRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitPicturesInPictureState(Lnet/minecraft/client/gui/render/state/pip/PictureInPictureRenderState;)V"))
    private void universalEmotes$wrapWheelPreview(GuiRenderState renderState, PictureInPictureRenderState state,
                                                  Operation<Void> original) {
        if (EmoteWheelPreview.isRendering() && state instanceof GuiEntityRenderState entity) {
            state = new PreviewEffectsPip.State(entity);
        }
        original.call(renderState, state);
    }
}
*///?}
