package com.ptah.client.mixin;

import com.ptah.client.render.EmoteSkinOverrides;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=1.21.9 {
/*import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
*///?} elif >=1.20.2 {
/*import net.minecraft.client.resources.PlayerSkin;
*///?}

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    //? if >=1.20.2 {
    /*@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void universalEmotes$overrideSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        if (EmoteSkinOverrides.isEmpty()) return;
        java.util.UUID id = ((AbstractClientPlayer) (Object) this).getUUID();
        ResourceLocation texture = EmoteSkinOverrides.texture(id);
        String model = EmoteSkinOverrides.model(id);
        if (texture == null && model == null) return;

        PlayerSkin skin = cir.getReturnValue();
        //? if >=1.21.9 {
        /^cir.setReturnValue(new PlayerSkin(
                texture != null ? new ClientAsset.ResourceTexture(texture, texture) : skin.body(),
                skin.cape(), skin.elytra(),
                model != null ? ("slim".equals(model) ? PlayerModelType.SLIM : PlayerModelType.WIDE) : skin.model(),
                skin.secure()));
        ^///?} else {
        cir.setReturnValue(new PlayerSkin(
                texture != null ? texture : skin.texture(), skin.textureUrl(),
                skin.capeTexture(), skin.elytraTexture(),
                model != null ? PlayerSkin.Model.byName(model) : skin.model(),
                skin.secure()));
        //?}
    }
    *///?} else {
    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
    private void universalEmotes$overrideSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        if (EmoteSkinOverrides.isEmpty()) return;
        ResourceLocation override = EmoteSkinOverrides.texture(((AbstractClientPlayer) (Object) this).getUUID());
        if (override != null) cir.setReturnValue(override);
    }

    @Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
    private void universalEmotes$overrideModel(CallbackInfoReturnable<String> cir) {
        if (EmoteSkinOverrides.isEmpty()) return;
        String model = EmoteSkinOverrides.model(((AbstractClientPlayer) (Object) this).getUUID());
        if (model != null) cir.setReturnValue(model);
    }
    //?}
}
