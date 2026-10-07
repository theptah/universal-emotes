package com.ptah.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ptah.client.bend.BendCube;
import com.ptah.client.bend.BendModelPart;
import com.ptah.client.bend.BendDir;
import com.ptah.client.render.ArmorRenderState;
import com.ptah.client.bend.BendableCuboid;
import com.ptah.client.bend.CubeData;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements BendModelPart {
    @Shadow @Final private List<ModelPart.Cube> cubes;

    @Unique
    private boolean universalEmotes$bendActive;
    @Unique
    private BendDir universalEmotes$bendDir = BendDir.UP;
    @Unique
    private float universalEmotes$bendAxis;
    @Unique
    private float universalEmotes$bendValue;

    @Unique
    private BendableCuboid[] universalEmotes$bendables;
    @Unique
    private BendDir universalEmotes$bendablesDir;

    @Override
    public void universalEmotes$setBend(BendDir dir, float axis, float value) {
        this.universalEmotes$bendActive = true;
        this.universalEmotes$bendDir = dir;
        this.universalEmotes$bendAxis = axis;
        this.universalEmotes$bendValue = value;
    }

    @Override
    public void universalEmotes$clearBend() {
        this.universalEmotes$bendActive = false;
    }

    @Override
    public void universalEmotes$copyBendFrom(BendModelPart other) {
        ModelPartMixin source = (ModelPartMixin) other;
        if (source.universalEmotes$bendActive) {
            universalEmotes$setBend(source.universalEmotes$bendDir, source.universalEmotes$bendAxis, source.universalEmotes$bendValue);
        } else {
            universalEmotes$clearBend();
        }
    }

    //? if <1.21.9 {
    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void universalEmotes$copyBend(ModelPart modelPart, CallbackInfo ci) {
        universalEmotes$copyBendFrom((BendModelPart) (Object) modelPart);
    }
    //?}

    //? if >=1.21 {
    /*@Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void universalEmotes$compileBent(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                                             int color, CallbackInfo ci) {
        universalEmotes$renderBent(pose, consumer, light, overlay, ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f, (color & 0xFF) / 255.0f, ((color >>> 24) & 0xFF) / 255.0f, ci);
    }
    *///?} else {
    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void universalEmotes$compileBent(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                                             float red, float green, float blue, float alpha, CallbackInfo ci) {
        universalEmotes$renderBent(pose, consumer, light, overlay, red, green, blue, alpha, ci);
    }
    //?}

    @Unique
    private void universalEmotes$renderBent(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                                            float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!this.universalEmotes$bendActive || this.cubes.isEmpty() || ArmorRenderState.isRenderingArmor()) {
            return;
        }
        BendableCuboid[] bendables = universalEmotes$bendables();
        if (bendables.length == 0) {
            return;
        }
        for (BendableCuboid cuboid : bendables) {
            cuboid.applyBendSmooth(this.universalEmotes$bendAxis, this.universalEmotes$bendValue);
            cuboid.render(pose, consumer, light, overlay, red, green, blue, alpha);
        }
        ci.cancel();
    }

    @Unique
    private BendableCuboid[] universalEmotes$bendables() {
        if (this.universalEmotes$bendables != null && this.universalEmotes$bendablesDir == this.universalEmotes$bendDir) {
            return this.universalEmotes$bendables;
        }
        List<BendableCuboid> built = new ArrayList<>(this.cubes.size());
        for (ModelPart.Cube cube : this.cubes) {
            if (cube instanceof BendCube bendCube) {
                CubeData data = bendCube.universalEmotes$getCubeData();
                if (data != null) {
                    built.add(BendableCuboid.build(data, this.universalEmotes$bendDir));
                }
            }
        }
        this.universalEmotes$bendables = built.toArray(new BendableCuboid[0]);
        this.universalEmotes$bendablesDir = this.universalEmotes$bendDir;
        return this.universalEmotes$bendables;
    }
}
