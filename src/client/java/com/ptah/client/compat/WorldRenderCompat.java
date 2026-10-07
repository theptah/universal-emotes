package com.ptah.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.function.Consumer;

//? if >=1.21.9 {
/*import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.state.CameraRenderState;
*///?} else
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class WorldRenderCompat {
    public record Context(PoseStack poseStack, Vec3 cameraPos, Quaternionf cameraRotation, @Nullable Frustum frustum) { }

    private WorldRenderCompat() { }

    public static void afterEntities(Consumer<Context> handler) {
        //? if >=1.21.9 {
        /*WorldRenderEvents.BEFORE_TRANSLUCENT.register(ctx -> {
            CameraRenderState camera = ctx.worldState().cameraRenderState;
            handler.accept(new Context(ctx.matrices(), camera.pos, new Quaternionf(camera.orientation), null));
        });
        *///?} else {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (ctx.camera() == null || ctx.matrixStack() == null) return;
            handler.accept(new Context(ctx.matrixStack(), ClientCompat.cameraPosition(ctx.camera()),
                    MathCompat.cameraRotation(ctx.camera()), ctx.frustum()));
        });
        //?}
    }

    public static void frameStart(Runnable handler) {
        //? if >=1.21.9 {
        /*WorldRenderEvents.START_MAIN.register(ctx -> handler.run());
        *///?} else
        WorldRenderEvents.START.register(ctx -> handler.run());
    }
}
