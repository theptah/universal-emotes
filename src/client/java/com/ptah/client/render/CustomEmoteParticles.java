package com.ptah.client.render;

import com.ptah.client.compat.WorldRenderCompat;
import com.ptah.client.compat.MathCompat;
import com.ptah.client.compat.ClientCompat;
import com.ptah.client.compat.VertexCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CustomEmoteParticles {
    private static final List<Billboard> PARTICLES = new ArrayList<>();
    private static final float HALF_SIZE = 0.2f;
    private static final int LIFETIME = 20;
    private static final int MAX_PARTICLES = 4096;

    private CustomEmoteParticles() {}

    public static void spawn(ResourceLocation texture, double x, double y, double z,
                             double vx, double vy, double vz) {
        if (PARTICLES.size() >= MAX_PARTICLES) return;
        PARTICLES.add(new Billboard(texture, x, y, z, vx, vy, vz));
    }

    public static void clear() { PARTICLES.clear(); }

    public static void tick() {
        Iterator<Billboard> it = PARTICLES.iterator();
        while (it.hasNext()) {
            Billboard b = it.next();
            if (b.age++ >= LIFETIME) { it.remove(); continue; }
            b.px = b.x; b.py = b.y; b.pz = b.z;
            b.vx *= 0.96; b.vy *= 0.96; b.vz *= 0.96;
            b.x += b.vx; b.y += b.vy; b.z += b.vz;
        }
    }

    public static void render(WorldRenderCompat.Context ctx) {
        if (PARTICLES.isEmpty()) return;
        PoseStack poseStack = ctx.poseStack();
        float partial = ClientCompat.partialTick();
        Vec3 cam = ctx.cameraPos();
        Quaternionf camRot = ctx.cameraRotation();

        Frustum frustum = ctx.frustum();

        MultiBufferSource.BufferSource buffers = ClientCompat.bufferSource();

        for (Billboard b : PARTICLES) {
            double ix = b.px + (b.x - b.px) * partial;
            double iy = b.py + (b.y - b.py) * partial;
            double iz = b.pz + (b.z - b.pz) * partial;
            if (frustum != null && !frustum.isVisible(new AABB(
                    ix - HALF_SIZE, iy - HALF_SIZE, iz - HALF_SIZE,
                    ix + HALF_SIZE, iy + HALF_SIZE, iz + HALF_SIZE))) {
                continue;
            }
            int alpha = (int) (Math.max(0.0f, Math.min(1.0f, b.alpha(partial))) * 255.0f);
            if (alpha <= 0) continue;
            VertexConsumer vc = buffers.getBuffer(ClientCompat.entityTranslucent(b.texture));
            poseStack.pushPose();
            poseStack.translate(ix - cam.x, iy - cam.y, iz - cam.z);
            MathCompat.rotate(poseStack, camRot);
            PoseStack.Pose pose = poseStack.last();
            int light = LightTexture.FULL_BRIGHT;
            vertex(vc, pose, -HALF_SIZE, -HALF_SIZE, 0.0f, 1.0f, alpha, light);
            vertex(vc, pose, -HALF_SIZE, HALF_SIZE, 0.0f, 0.0f, alpha, light);
            vertex(vc, pose, HALF_SIZE, HALF_SIZE, 1.0f, 0.0f, alpha, light);
            vertex(vc, pose, HALF_SIZE, -HALF_SIZE, 1.0f, 1.0f, alpha, light);
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose,
                               float x, float y, float u, float v, int alpha, int light) {
        VertexCompat.entity(vc, pose, x, y, 0.0f, 255, 255, 255, alpha, u, v,
                OverlayTexture.NO_OVERLAY, light, 0.0f, 0.0f, 1.0f);
    }

    private static final class Billboard {
        final ResourceLocation texture;
        double x, y, z, px, py, pz, vx, vy, vz;
        int age;
        Billboard(ResourceLocation texture, double x, double y, double z, double vx, double vy, double vz) {
            this.texture = texture;
            this.x = this.px = x;
            this.y = this.py = y;
            this.z = this.pz = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
        }
        float alpha(float partial) {
            float t = (this.age + partial) / (float) LIFETIME;
            return t < 0.5f ? 1.0f : Math.max(0.0f, 1.0f - (t - 0.5f) * 2.0f);
        }
    }
}
