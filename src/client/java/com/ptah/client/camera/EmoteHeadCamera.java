package com.ptah.client.camera;

import com.ptah.client.compat.ClientCompat;
import com.ptah.animation.AnimationClip;
import com.ptah.animation.BoneAnimation;
import com.ptah.animation.KeyframeTrack;
import com.ptah.client.playback.ClientPlaybackRuntime;
import com.ptah.client.render.R15Model;
import com.ptah.playback.ActiveEmote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;

public final class EmoteHeadCamera {
    private static final double PIXEL_TO_BLOCK = 0.9375 / 16.0;

    private static final double HEAD_DEPTH_PIXELS = 8.0;

    private static final double EYE_UP_PIXELS = 4.0;

    private static final double FRONT_MARGIN_PIXELS = 4.0;

    private EmoteHeadCamera() {
    }

    public static Vec3 offset(float partialTick) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return Vec3.ZERO;
        }
        if (!client.options.getCameraType().isFirstPerson()) {
            return Vec3.ZERO;
        }
        ActiveEmote active = ClientPlaybackRuntime.MANAGER.active(player.getUUID()).orElse(null);
        if (active == null) {
            return Vec3.ZERO;
        }

        float time = active.localAnimationTime(client.level.getGameTime() + ClientCompat.partialTick());
        AnimationClip clip = active.emote().animation();
        double halfDepth = HEAD_DEPTH_PIXELS / 2.0 + FRONT_MARGIN_PIXELS;

        Vector3f offsetModel;
        if ("r15".equalsIgnoreCase(active.emote().rigType())) {
            offsetModel = r15HeadOffset(clip, time, halfDepth, isSlim(player));
        } else {
            offsetModel = r6HeadOffset(clip, time, halfDepth);
        }
        if (offsetModel == null) {
            return Vec3.ZERO;
        }

        double localX = -offsetModel.x * PIXEL_TO_BLOCK;
        double localY = -offsetModel.y * PIXEL_TO_BLOCK;
        double localZ = offsetModel.z * PIXEL_TO_BLOCK;

        if (localX == 0.0 && localY == 0.0 && localZ == 0.0) {
            return Vec3.ZERO;
        }

        double yaw = Math.toRadians(180.0 - Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot));
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double worldX = localX * cos + localZ * sin;
        double worldZ = -localX * sin + localZ * cos;

        return new Vec3(worldX, localY, worldZ);
    }

    private static Vector3f r15HeadOffset(AnimationClip clip, float time, double halfDepth, boolean slim) {
        R15Model model = R15Model.get(slim);
        Map<String, Matrix4f> mats = model.computeModelMatrices(clip, time);
        Matrix4f headMatrix = mats.get("Head");
        Vector3f pivot = model.pivotOf("Head");
        if (headMatrix == null || pivot == null) {
            return null;
        }

        Vector3f eyeBB = new Vector3f(pivot.x, pivot.y + (float) EYE_UP_PIXELS, pivot.z - (float) halfDepth);
        Vector3f refBB = new Vector3f(pivot.x, pivot.y + (float) EYE_UP_PIXELS, pivot.z);
        Vector3f eyeModel = headMatrix.transformPosition(new Vector3f(eyeBB));
        Vector3f refModel = R15Model.BB_TO_MODEL.transformPosition(new Vector3f(refBB));
        return eyeModel.sub(refModel);
    }

    private static Vector3f r6HeadOffset(AnimationClip clip, float time, double halfDepth) {
        BoneAnimation head = clip.bones().get("Head");
        Vector3f eyeAnchor = new Vector3f(0.0f, (float) -EYE_UP_PIXELS, (float) -halfDepth);
        Vector3f eye = animatedPoint(head, time, eyeAnchor);

        return new Vector3f(eye.x, eye.y + (float) EYE_UP_PIXELS, eye.z);
    }

    private static Vector3f animatedPoint(BoneAnimation bone, float time, Vector3f anchor) {
        Vector3f out = new Vector3f(anchor);
        if (bone == null) {
            return out;
        }

        KeyframeTrack rotationTrack = bone.rotation();
        if (rotationTrack != null && !rotationTrack.keyframes().isEmpty()) {
            com.ptah.animation.Vec3 r = rotationTrack.sample(time);
            float xr = (float) Math.toRadians(r.x());
            float yr = (float) Math.toRadians(r.y());
            float zr = (float) Math.toRadians(r.z());
            new Quaternionf().rotationZYX(zr, yr, xr).transform(out);
        }

        KeyframeTrack position = bone.position();
        if (position != null && !position.keyframes().isEmpty()) {
            com.ptah.animation.Vec3 s = position.sample(time);
            out.add((float) s.x(), (float) -s.y(), (float) s.z());
        }

        return out;
    }

    private static boolean isSlim(LocalPlayer player) {
        return ClientCompat.isSlim(player);
    }
}
