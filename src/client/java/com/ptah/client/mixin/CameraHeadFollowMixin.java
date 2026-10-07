package com.ptah.client.mixin;

import com.ptah.client.camera.EmoteHeadCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
//? if >=1.21.11 {
/*import net.minecraft.world.level.Level;
*///?} else
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraHeadFollowMixin {
    @Shadow private Vec3 position;

    @Inject(method = "setup", at = @At("TAIL"))
    private void universalEmotes$followHead(
                                       //? if >=1.21.11 {
                                       /*Level level,
                                       *///?} else
                                       BlockGetter level,
                                        Entity entity, boolean detached,
                                       boolean thirdPersonReverse, float partialTick,
                                       CallbackInfo callbackInfo) {
        Vec3 offset = EmoteHeadCamera.offset(partialTick);
        if (offset.x != 0.0 || offset.y != 0.0 || offset.z != 0.0) {
            this.position = this.position.add(offset);
        }
    }
}
