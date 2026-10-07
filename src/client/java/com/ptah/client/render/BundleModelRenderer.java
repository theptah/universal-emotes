package com.ptah.client.render;

import com.ptah.client.compat.WorldRenderCompat;
import com.ptah.client.compat.MathCompat;
import com.ptah.client.compat.ClientCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ptah.event.type.RenderModelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BundleModelRenderer {
    private static final int MAX_INSTANCES = 256;

    private static final float PLAYER_MODEL_SCALE = 0.9375f;
    private static final List<Instance> INSTANCES = new CopyOnWriteArrayList<>();

    private BundleModelRenderer() {}

    private static final class Instance {
        final UUID player;
        final BundleModelGeometry geo;

        final RenderModelEvent source;
        final int lifetime;
        int age;
        final double wx, wy, wz;
        final float wyaw;

        Instance(UUID player, BundleModelGeometry geo, RenderModelEvent source, int lifetime,
                 double wx, double wy, double wz, float wyaw) {
            this.player = player; this.geo = geo; this.source = source; this.lifetime = lifetime;
            this.wx = wx; this.wy = wy; this.wz = wz; this.wyaw = wyaw;
        }

        String attach() { return source.attachMode() == null ? "world" : source.attachMode(); }
    }

    public static void spawn(UUID player, BundleModelGeometry geo, RenderModelEvent source, int lifetime,
                             double wx, double wy, double wz, float wyaw) {
        if (geo == null || geo.isEmpty() || source == null) return;

        List<Instance> old = new ArrayList<>();
        for (Instance in : INSTANCES) if (in.player.equals(player) && source.equals(in.source)) old.add(in);
        INSTANCES.removeAll(old);
        if (INSTANCES.size() >= MAX_INSTANCES) INSTANCES.remove(0);
        INSTANCES.add(new Instance(player, geo, source, lifetime, wx, wy, wz, wyaw));
    }

    public static void tick() {
        List<Instance> dead = new ArrayList<>();
        for (Instance in : INSTANCES) {
            in.age++;
            if (in.lifetime >= 0 && in.age > in.lifetime) dead.add(in);
        }
        INSTANCES.removeAll(dead);
    }

    public static void clearFor(UUID player) {
        List<Instance> dead = new ArrayList<>();
        for (Instance in : INSTANCES) if (in.player.equals(player)) dead.add(in);
        INSTANCES.removeAll(dead);
    }

    public static void clearAll() { INSTANCES.clear(); }

    public static void render(WorldRenderCompat.Context ctx) {
        if (INSTANCES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        PoseStack ps = ctx.poseStack();
        Vec3 cam = ctx.cameraPos();
        float partial = ClientCompat.partialTick();
        MultiBufferSource.BufferSource buffers = ClientCompat.bufferSource();

        Frustum frustum = ctx.frustum();

        for (Instance in : INSTANCES) {
            double ax, ay, az;
            float yaw;
            String attach = in.attach();
            boolean root = "model_root".equals(attach) || "bone".equals(attach);
            Matrix4f boneFrame = null;
            if (root) {
                Player p = mc.level.getPlayerByUUID(in.player);
                if (p == null) continue;
                if ("bone".equals(attach)) boneFrame = EmoteBoneFrames.find(EmoteBoneFrames.forPlayer(p, partial), in.source.bone());
                ax = Mth.lerp(partial, p.xOld, p.getX());
                ay = Mth.lerp(partial, p.yOld, p.getY());
                az = Mth.lerp(partial, p.zOld, p.getZ());
                yaw = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
            } else {
                ax = in.wx; ay = in.wy; az = in.wz; yaw = in.wyaw;
            }

            if (frustum != null && !frustum.isVisible(new AABB(
                    ax - 2.0, ay - 1.0, az - 2.0,
                    ax + 2.0, ay + 4.0, az + 2.0))) {
                continue;
            }

            ps.pushPose();
            ps.translate(ax - cam.x, ay - cam.y, az - cam.z);

            MathCompat.rotateYDegrees(ps, 180.0f - yaw);
            if (applyLocalTransform(ps, in.source, in.source.time() + (in.age + partial) / 20.0f, boneFrame)) {
                in.geo.draw(buffers, ps.last());
            }
            ps.popPose();
        }
        buffers.endBatch();
    }

    public static boolean applyLocalTransform(PoseStack ps, RenderModelEvent source, float emoteSeconds) {
        return applyLocalTransform(ps, source, emoteSeconds, null);
    }

    public static boolean applyLocalTransform(PoseStack ps, RenderModelEvent source, float emoteSeconds, Matrix4f boneFrame) {
        String attach = source.attachMode() == null ? "world" : source.attachMode();

        if ("model_root".equals(attach) || "bone".equals(attach)) {
            ps.scale(PLAYER_MODEL_SCALE, PLAYER_MODEL_SCALE, PLAYER_MODEL_SCALE);
        }
        if ("bone".equals(attach) && boneFrame != null) {
            Matrix4f frame = new Matrix4f(boneFrame);
            frame.m30(frame.m30() / 16f);
            frame.m31(frame.m31() / 16f);
            frame.m32(frame.m32() / 16f);
            MathCompat.mul(ps, frame);
        } else if ("bone".equals(attach)) {
            float[] off = boneOffset(source.bone());
            ps.translate(off[0], off[1], off[2]);
        }

        RenderModelEvent.Pose t = source.sample(emoteSeconds);
        if (!t.visible()) return false;

        ps.translate(t.px() / 16.0f, t.py() / 16.0f, t.pz() / 16.0f);
        MathCompat.rotateZDegrees(ps, t.rz());
        MathCompat.rotateYDegrees(ps, t.ry());
        MathCompat.rotateXDegrees(ps, t.rx());
        ps.scale(t.scale(), t.scale(), t.scale());
        return true;
    }

    private static float[] boneOffset(String bone) {
        if (bone == null) return new float[]{0, 0, 0};

        return switch (bone) {
            case "Head" -> new float[]{0f, 1.5f, 0f};
            case "Torso", "Root", "LowerTorso", "UpperTorso" -> new float[]{0f, 1.0f, 0f};
            case "RightArm", "RightUpperArm" -> new float[]{-0.375f, 1.1f, 0f};
            case "LeftArm", "LeftUpperArm" -> new float[]{0.375f, 1.1f, 0f};
            case "RightLeg", "RightUpperLeg" -> new float[]{-0.125f, 0.375f, 0f};
            case "LeftLeg", "LeftUpperLeg" -> new float[]{0.125f, 0.375f, 0f};
            default -> new float[]{0f, 1.0f, 0f};
        };
    }
}
