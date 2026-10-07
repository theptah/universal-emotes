package com.ptah.client.mixin;

//? if >=1.21.2 {
/*import com.ptah.client.compat.ClientCompat;
import com.ptah.client.render.EmoteRenderState;
import com.ptah.client.screen.EmoteWheelPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void universalEmotes$captureEmoteData(LivingEntity entity, LivingEntityRenderState state, float partialTick,
                                                  CallbackInfo ci) {
        boolean player = entity instanceof Player;
        ((EmoteRenderState) state).universalEmotes$capture(
                player ? entity.getUUID() : null,
                entity == Minecraft.getInstance().player,
                EmoteWheelPreview.isRendering(),
                entity instanceof AbstractClientPlayer clientPlayer && ClientCompat.isSlim(clientPlayer));
    }
}
*///?}
