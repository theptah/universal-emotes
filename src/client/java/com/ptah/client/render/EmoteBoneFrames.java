package com.ptah.client.render;

import com.ptah.animation.AnimationClip;
import com.ptah.animation.BendSample;
import com.ptah.animation.BoneAnimation;
import com.ptah.animation.Vec3;
import com.ptah.client.bend.BendDir;
import com.ptah.client.bend.BendableCuboid;
import com.ptah.client.compat.ClientCompat;
import com.ptah.client.playback.ClientPlaybackRuntime;
import com.ptah.playback.ActiveEmote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EmoteBoneFrames {
    private static final Matrix4f MODEL_TO_BB = new Matrix4f(R15Model.BB_TO_MODEL).invert();
    private static final Matrix4f FLIP = new Matrix4f().scaling(-1f, -1f, 1f);
    private static final Vector3f RIGHT_ITEM = new Vector3f(-1f, 10f, -2f);
    private static final Vector3f LEFT_ITEM = new Vector3f(1f, 10f, -2f);
    private static final String[] R6_PARTS = {"Head", "Torso", "RightArm", "LeftArm", "RightLeg", "LeftLeg"};

    private EmoteBoneFrames() { }

    public static Map<String, Matrix4f> compute(AnimationClip clip, float time, String rigType, boolean slim) {
        if (clip == null) return Map.of();
        if ("r15".equalsIgnoreCase(rigType)) return R15Model.get(slim).boneFrames(clip, time);
        Map<String, Matrix4f> out = new LinkedHashMap<>();
        r6ModelFrames(clip, time, slim).forEach((name, frame) -> out.put(name, toBlockbench(frame)));
        return out;
    }

    public static Map<String, Matrix4f> forPlayer(Player player, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (player == null || client.level == null) return Map.of();
        ActiveEmote active = ClientPlaybackRuntime.MANAGER.active(player.getUUID()).orElse(null);
        if (active == null) return Map.of();
        float time = active.localAnimationTime(client.level.getGameTime() + partialTick);
        boolean slim = player instanceof AbstractClientPlayer clientPlayer && ClientCompat.isSlim(clientPlayer);
        return compute(active.emote().animation(), time, active.emote().rigType(), slim);
    }

    public static Matrix4f find(Map<String, Matrix4f> frames, String bone) {
        if (bone == null || frames == null || frames.isEmpty()) return null;
        Matrix4f direct = frames.get(bone);
        if (direct != null) return direct;
        String alias = alias(bone);
        return alias == null ? null : frames.get(alias);
    }

    public static Matrix4f heldItemPose(AnimationClip clip, float time, String rigType, boolean slim, boolean right) {
        if (clip == null) return null;
        String name = right ? "RightItem" : "LeftItem";
        Matrix4f item;
        if ("r15".equalsIgnoreCase(rigType)) {
            Matrix4f frame = R15Model.get(slim).boneFrames(clip, time).get(name);
            if (frame == null) return null;
            item = toModel(frame);
        } else {
            if (!affectsHeldItem(clip, right)) return null;
            item = r6ModelFrames(clip, time, slim).get(name);
            if (item == null) return null;
            float shift = slim ? (right ? 0.5f : -0.5f) : 0f;
            item = new Matrix4f().translate(shift, 0f, 0f).mul(item);
        }
        Vector3f offset = right ? RIGHT_ITEM : LEFT_ITEM;
        return new Matrix4f(item).translate(-offset.x, -offset.y, -offset.z);
    }

    private static boolean affectsHeldItem(AnimationClip clip, boolean right) {
        BoneAnimation item = clip.bones().get(right ? "RightItem" : "LeftItem");
        if (item != null && (!item.rotation().keyframes().isEmpty() || !item.position().keyframes().isEmpty())) return true;
        BoneAnimation arm = clip.bones().get(right ? "RightArm" : "LeftArm");
        return arm != null && !arm.bend().isEmpty();
    }

    private static Map<String, Matrix4f> r6ModelFrames(AnimationClip clip, float time, boolean slim) {
        Map<String, Matrix4f> out = new LinkedHashMap<>();
        Matrix4f root = rootMatrix(clip.bones().get("Root"), time);
        out.put("Root", new Matrix4f(root).translate(0f, 12f, 0f));
        for (String name : R6_PARTS) {
            Vector3f rest = rest(name, slim);
            Matrix4f frame = new Matrix4f(root).translate(rest.x, rest.y, rest.z);
            apply(frame, clip.bones().get(name), time);
            out.put(name, frame);
        }
        out.put("RightItem", item(out.get("RightArm"), clip, time, slim, true));
        out.put("LeftItem", item(out.get("LeftArm"), clip, time, slim, false));
        return out;
    }

    private static Matrix4f item(Matrix4f arm, AnimationClip clip, float time, boolean slim, boolean right) {
        Matrix4f frame = new Matrix4f(arm);
        BoneAnimation armAnimation = clip.bones().get(right ? "RightArm" : "LeftArm");
        if (armAnimation != null && !armAnimation.bend().isEmpty()) {
            BendSample sample = armAnimation.bend().sample(time);
            float value = (float) Math.toRadians(sample.value());
            if (Math.abs(value) >= 1.0e-4f) {
                float center = (right ? -1f : 1f) * (slim ? 0.5f : 1f);
                Matrix4f bend = BendableCuboid.freeEndTransform(BendDir.UP, -2f, 10f, center, 0f,
                        (float) Math.toRadians(sample.axis()), value);
                if (bend != null) frame.mul(bend);
            }
        }
        Vector3f offset = right ? RIGHT_ITEM : LEFT_ITEM;
        frame.translate(offset.x, offset.y, offset.z);
        apply(frame, clip.bones().get(right ? "RightItem" : "LeftItem"), time);
        return frame;
    }

    private static Matrix4f rootMatrix(BoneAnimation root, float time) {
        Matrix4f matrix = new Matrix4f();
        if (root == null) return matrix;
        if (!root.position().keyframes().isEmpty()) {
            Vec3 position = root.position().sample(time);
            matrix.translate(position.x(), -position.y(), position.z());
        }
        matrix.translate(0f, 12f, 0f);
        if (!root.rotation().keyframes().isEmpty()) {
            Vec3 rotation = root.rotation().sample(time);
            matrix.rotateZ((float) Math.toRadians(rotation.z()));
            matrix.rotateY((float) Math.toRadians(rotation.y()));
            matrix.rotateX((float) Math.toRadians(rotation.x()));
        }
        return matrix.translate(0f, -12f, 0f);
    }

    private static void apply(Matrix4f frame, BoneAnimation animation, float time) {
        if (animation == null) return;
        if (!animation.position().keyframes().isEmpty()) {
            Vec3 position = animation.position().sample(time);
            frame.translate(position.x(), -position.y(), position.z());
        }
        if (!animation.rotation().keyframes().isEmpty()) {
            Vec3 rotation = animation.rotation().sample(time);
            frame.rotateZ((float) Math.toRadians(rotation.z()));
            frame.rotateY((float) Math.toRadians(rotation.y()));
            frame.rotateX((float) Math.toRadians(rotation.x()));
        }
    }

    private static Vector3f rest(String name, boolean slim) {
        return switch (name) {
            case "RightArm" -> new Vector3f(-5f, slim ? 2.5f : 2f, 0f);
            case "LeftArm" -> new Vector3f(5f, slim ? 2.5f : 2f, 0f);
            case "RightLeg" -> new Vector3f(-1.9f, 12f, 0f);
            case "LeftLeg" -> new Vector3f(1.9f, 12f, 0f);
            default -> new Vector3f();
        };
    }

    private static Matrix4f toBlockbench(Matrix4f modelFrame) {
        return new Matrix4f(MODEL_TO_BB).mul(modelFrame).mul(FLIP);
    }

    private static Matrix4f toModel(Matrix4f blockbenchFrame) {
        return new Matrix4f(R15Model.BB_TO_MODEL).mul(blockbenchFrame).mul(FLIP);
    }

    private static String alias(String bone) {
        return switch (bone) {
            case "Torso" -> "UpperTorso";
            case "LowerTorso", "UpperTorso" -> "Torso";
            case "RightArm" -> "RightUpperArm";
            case "LeftArm" -> "LeftUpperArm";
            case "RightLeg" -> "RightUpperLeg";
            case "LeftLeg" -> "LeftUpperLeg";
            case "RightUpperArm", "RightLowerArm", "RightHand" -> "RightArm";
            case "LeftUpperArm", "LeftLowerArm", "LeftHand" -> "LeftArm";
            case "RightUpperLeg", "RightLowerLeg", "RightFoot" -> "RightLeg";
            case "LeftUpperLeg", "LeftLowerLeg", "LeftFoot" -> "LeftLeg";
            default -> null;
        };
    }
}
