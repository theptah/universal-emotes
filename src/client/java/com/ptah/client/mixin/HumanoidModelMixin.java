package com.ptah.client.mixin;

import com.ptah.animation.AnimationClip;
import com.ptah.animation.BendSample;
import com.ptah.animation.BoneAnimation;
import com.ptah.animation.Vec3;
import com.ptah.bundle.EmoteDefinition;
import com.ptah.client.bend.BendDir;
import com.ptah.client.bend.BendModelPart;
import com.ptah.client.compat.ClientCompat;
import com.ptah.client.playback.ClientPlaybackRuntime;
import com.ptah.client.render.ArmorRenderState;
import com.ptah.client.render.CapeFollowState;
import com.ptah.client.render.EmoteBoneFrames;
import com.ptah.client.render.FirstPersonEmoteState;
import com.ptah.client.render.HeldItemPose;
import com.ptah.client.render.R15Model;
import com.ptah.client.render.R15RenderState;
import com.ptah.client.screen.EmoteWheelPreview;
import com.ptah.playback.ActiveEmote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

//? if >=1.21.2 {
/*import com.ptah.client.render.EmoteRenderState;
import net.minecraft.client.model.PlayerCapeModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
*///?} else {
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
//?}

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin implements HeldItemPose {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart hat;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;

    @Unique private static final int ROLE_SKIP = 0;
    @Unique private static final int ROLE_POSE = 1;
    @Unique private static final int ROLE_FULL = 2;

    @Unique private Matrix4f universalEmotes$rightItemPose;
    @Unique private Matrix4f universalEmotes$leftItemPose;
    @Unique private boolean universalEmotes$itemSlim;

    //? if >=1.21.2 {
    /*@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("HEAD"))
    private void universalEmotes$resetPersistentPose(HumanoidRenderState state, CallbackInfo callbackInfo) {
        EmoteRenderState data = (EmoteRenderState) state;
        universalEmotes$begin(universalEmotes$role(), data.universalEmotes$playerId() != null);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void universalEmotes$applyAnimation(HumanoidRenderState state, CallbackInfo callbackInfo) {
        EmoteRenderState data = (EmoteRenderState) state;
        universalEmotes$apply(universalEmotes$role(), data.universalEmotes$playerId(),
                data.universalEmotes$isLocalPlayer(), data.universalEmotes$isPreview(), data.universalEmotes$isSlim());
    }

    @Unique
    private int universalEmotes$role() {
        Object self = this;
        if (self instanceof PlayerCapeModel) return ROLE_SKIP;
        if (self instanceof PlayerModel) return ROLE_FULL;
        return ROLE_POSE;
    }
    *///?} else {
    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void universalEmotes$resetPersistentPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                                     float ageInTicks, float netHeadYaw, float headPitch,
                                                     CallbackInfo callbackInfo) {
        universalEmotes$begin(ROLE_FULL, entity instanceof Player);
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void universalEmotes$applyAnimation(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                                float ageInTicks, float netHeadYaw, float headPitch,
                                                CallbackInfo callbackInfo) {
        Minecraft client = Minecraft.getInstance();
        universalEmotes$apply(ROLE_FULL, entity.getUUID(), entity == client.player,
                EmoteWheelPreview.isRendering(),
                entity instanceof AbstractClientPlayer player && ClientCompat.isSlim(player));
    }
    //?}

    @Unique
    private void universalEmotes$begin(int role, boolean player) {
        if (role == ROLE_SKIP) return;
        universalEmotes$rightItemPose = null;
        universalEmotes$leftItemPose = null;
        if (role == ROLE_FULL) {
            R15RenderState.clear();
            ArmorRenderState.clear();
            CapeFollowState.clear();
        }
        universalEmotes$resetParts();

        if (player && role == ROLE_FULL) {
            head.visible = true;
            hat.visible = true;
        }
    }

    @Unique
    private void universalEmotes$apply(int role, UUID playerId, boolean localPlayer, boolean preview, boolean slim) {
        if (role == ROLE_SKIP) return;
        Minecraft client = Minecraft.getInstance();

        if (preview && localPlayer) {
            EmoteDefinition hovered = EmoteWheelPreview.hovered();
            if (hovered != null) {
                float previewTime = EmoteWheelPreview.timeFor(hovered.animation());
                if ("r15".equalsIgnoreCase(hovered.rigType())) {
                    if (role == ROLE_FULL) R15RenderState.set(hovered.animation(), previewTime, false, slim, this);
                } else {
                    universalEmotes$applyBones(hovered.animation(), previewTime, hovered.rigType(), role == ROLE_FULL);
                }
                if (role == ROLE_FULL) universalEmotes$poseHeldItems(hovered.animation(), previewTime, hovered.rigType(), slim);
            }
            return;
        }

        if (client.level == null || playerId == null) return;

        ActiveEmote active = ClientPlaybackRuntime.MANAGER.active(playerId).orElse(null);
        if (active == null) return;

        universalEmotes$resetParts();

        float time = active.localAnimationTime(client.level.getGameTime() + ClientCompat.partialTick());
        boolean firstPerson = localPlayer && FirstPersonEmoteState.isActive();
        if ("r15".equalsIgnoreCase(active.emote().rigType())) {
            if (role == ROLE_FULL) {
                R15RenderState.set(active.emote().animation(), time, firstPerson, slim, this);
            }

            universalEmotes$poseVanillaForR15(active.emote().animation(), time, slim);

            if (role == ROLE_FULL) {
                CapeFollowState.set(R15Model.get(slim).capeFollowMatrix(active.emote().animation(), time));
            }
        } else {
            universalEmotes$applyBones(active.emote().animation(), time, active.emote().rigType(), role == ROLE_FULL);

            if (role == ROLE_FULL) CapeFollowState.set(universalEmotes$bodyCapeDelta());
        }

        if (role == ROLE_FULL) universalEmotes$poseHeldItems(active.emote().animation(), time, active.emote().rigType(), slim);

        if (role == ROLE_FULL && firstPerson) {
            head.visible = false;

            hat.visible = false;
        }
    }

    @Unique
    private void universalEmotes$poseHeldItems(AnimationClip clip, float time, String rigType, boolean slim) {
        universalEmotes$itemSlim = slim;
        universalEmotes$rightItemPose = EmoteBoneFrames.heldItemPose(clip, time, rigType, slim, true);
        universalEmotes$leftItemPose = EmoteBoneFrames.heldItemPose(clip, time, rigType, slim, false);
    }

    @Override
    public Matrix4f universalEmotes$heldItemDelta(boolean right) {
        Matrix4f target = right ? universalEmotes$rightItemPose : universalEmotes$leftItemPose;
        if (target == null) return null;
        ModelPart arm = right ? rightArm : leftArm;
        float shift = universalEmotes$itemSlim ? (right ? 0.5f : -0.5f) : 0f;
        Matrix4f delta = new Matrix4f()
                .translate(arm.x + shift, arm.y, arm.z)
                .rotateZ(arm.zRot)
                .rotateY(arm.yRot)
                .rotateX(arm.xRot)
                .invert()
                .mul(target);
        delta.m30(delta.m30() / 16f);
        delta.m31(delta.m31() / 16f);
        delta.m32(delta.m32() / 16f);
        return delta;
    }

    @Unique
    private void universalEmotes$resetParts() {
        for (ModelPart part : new ModelPart[]{head, body, rightArm, leftArm, rightLeg, leftLeg}) {
            part.resetPose();
            universalEmotes$clearPartBend(part);
        }
        universalEmotes$syncOverlayBends();
    }

    @Unique
    private void universalEmotes$applyBones(AnimationClip clip, float time, String rigType, boolean bend) {
        clip.bones().forEach((bone, animation) -> {
            ModelPart part = switch (bone) {
                case "Head" -> head;
                case "Torso" -> body;
                case "RightArm" -> rightArm;
                case "LeftArm" -> leftArm;
                case "RightLeg" -> rightLeg;
                case "LeftLeg" -> leftLeg;
                default -> null;
            };
            if (part != null) {
                apply(part, animation, time);
                if (bend) universalEmotes$applyBend(part, bone, animation, time, rigType);
            }
        });

        universalEmotes$applyRoot(clip, time);

        universalEmotes$syncHat();
        universalEmotes$syncOverlayBends();
    }

    @Unique
    private void universalEmotes$syncHat() {
        //? if <1.21.2
        this.hat.copyFrom(this.head);
    }

    @Unique
    private void universalEmotes$syncOverlayBends() {
        //? if >=1.21.2 {
        /*if ((Object) this instanceof PlayerModel player && !((Object) this instanceof PlayerCapeModel)) {
            universalEmotes$copyBend(player.leftSleeve, leftArm);
            universalEmotes$copyBend(player.rightSleeve, rightArm);
            universalEmotes$copyBend(player.leftPants, leftLeg);
            universalEmotes$copyBend(player.rightPants, rightLeg);
            universalEmotes$copyBend(player.jacket, body);
        }
        *///?}
    }

    @Unique
    private static void universalEmotes$copyBend(ModelPart target, ModelPart source) {
        ((BendModelPart) (Object) target).universalEmotes$copyBendFrom((BendModelPart) (Object) source);
    }

    private void universalEmotes$applyRoot(AnimationClip clip, float time) {
        BoneAnimation root = clip.bones().get("Root");
        if (root == null) {
            return;
        }
        boolean hasRotation = root.rotation() != null && !root.rotation().keyframes().isEmpty();
        boolean hasPosition = root.position() != null && !root.position().keyframes().isEmpty();
        if (!hasRotation && !hasPosition) {
            return;
        }
        Matrix4f rootMatrix = new Matrix4f();
        if (hasPosition) {
            Vec3 position = root.position().sample(time);
            rootMatrix.translate((float) position.x(), (float) -position.y(), (float) position.z());
        }

        rootMatrix.translate(0f, 12f, 0f);
        if (hasRotation) {
            Vec3 rotation = root.rotation().sample(time);
            rootMatrix.rotateZ((float) Math.toRadians(rotation.z()));
            rootMatrix.rotateY((float) Math.toRadians(rotation.y()));
            rootMatrix.rotateX((float) Math.toRadians(rotation.x()));
        }
        rootMatrix.translate(0f, -12f, 0f);
        universalEmotes$applyRootToPart(head, rootMatrix);
        universalEmotes$applyRootToPart(body, rootMatrix);
        universalEmotes$applyRootToPart(rightArm, rootMatrix);
        universalEmotes$applyRootToPart(leftArm, rootMatrix);
        universalEmotes$applyRootToPart(rightLeg, rootMatrix);
        universalEmotes$applyRootToPart(leftLeg, rootMatrix);
    }

    private static void universalEmotes$applyRootToPart(ModelPart part, Matrix4f rootMatrix) {
        Matrix4f partMatrix = new Matrix4f()
                .translate(part.x, part.y, part.z)
                .rotateZ(part.zRot)
                .rotateY(part.yRot)
                .rotateX(part.xRot);
        Matrix4f full = new Matrix4f(rootMatrix).mul(partMatrix);
        Vector3f translation = full.getTranslation(new Vector3f());
        part.x = translation.x;
        part.y = translation.y;
        part.z = translation.z;

        float r20 = full.m02();
        float yRot = (float) Math.asin(Math.max(-1f, Math.min(1f, -r20)));
        float xRot;
        float zRot;
        if (Math.abs(r20) < 0.99999f) {
            xRot = (float) Math.atan2(full.m12(), full.m22());
            zRot = (float) Math.atan2(full.m01(), full.m00());
        } else {
            zRot = 0f;
            xRot = (float) Math.atan2(-full.m21(), full.m11());
        }
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    private static BendDir universalEmotes$bendDir(String bone) {
        return switch (bone) {
            case "Torso" -> BendDir.DOWN;
            case "RightArm", "LeftArm", "RightLeg", "LeftLeg" -> BendDir.UP;
            default -> null;
        };
    }

    private void universalEmotes$applyBend(ModelPart part, String bone, BoneAnimation animation, float time, String rigType) {
        if ("r15".equalsIgnoreCase(rigType)) {
            universalEmotes$clearPartBend(part);
            return;
        }
        BendDir dir = universalEmotes$bendDir(bone);
        if (dir == null || animation.bend().isEmpty()) {
            return;
        }

        BendSample sample = animation.bend().sample(time);
        float value = (float) Math.toRadians(sample.value());
        float axis = (float) Math.toRadians(sample.axis());

        if (Math.abs(value) < 1.0e-4f) {
            universalEmotes$clearPartBend(part);
            return;
        }
        ((BendModelPart) (Object) part).universalEmotes$setBend(dir, axis, value);
    }

    private static void universalEmotes$clearPartBend(ModelPart part) {
        ((BendModelPart) (Object) part).universalEmotes$clearBend();
    }

    private static void apply(ModelPart part, BoneAnimation animation, float time) {
        if (!animation.rotation().keyframes().isEmpty()) {
            Vec3 rotation = animation.rotation().sample(time);
            part.xRot = (float) Math.toRadians(rotation.x());
            part.yRot = (float) Math.toRadians(rotation.y());
            part.zRot = (float) Math.toRadians(rotation.z());
        }
        if (!animation.position().keyframes().isEmpty()) {
            Vec3 position = animation.position().sample(time);
            part.x += position.x();
            part.y -= position.y();
            part.z += position.z();
        }
    }

    private void universalEmotes$poseVanillaForR15(AnimationClip clip, float time, boolean slim) {
        if (clip == null) return;

        R15Model model = R15Model.get(slim);
        Map<String, Matrix4f> mats = model.computeModelMatrices(clip, time);

        universalEmotes$poseR15Part(head, model, mats, "Head");
        universalEmotes$poseR15Part(body, model, mats, "UpperTorso");
        universalEmotes$poseR15Part(rightArm, model, mats, "RightUpperArm");
        universalEmotes$poseR15Part(leftArm, model, mats, "LeftUpperArm");
        universalEmotes$poseR15Part(rightLeg, model, mats, "RightUpperLeg");
        universalEmotes$poseR15Part(leftLeg, model, mats, "LeftUpperLeg");
        universalEmotes$syncHat();
    }

    private static void universalEmotes$poseR15Part(ModelPart part, R15Model model,
                                               Map<String, Matrix4f> mats, String bone) {
        Matrix4f m = mats.get(bone);
        if (m == null) return;
        Vector3f piv = model.pivotOf(bone);
        Vector3f rest = model.restModelPos(bone);
        if (piv == null || rest == null) return;
        Vector3f cur = m.transformPosition(new Vector3f(piv));
        part.x += cur.x - rest.x;
        part.y += cur.y - rest.y;
        part.z += cur.z - rest.z;
        Vector3f euler = new Matrix4f(m).scale(-1f, -1f, 1f)
                .getEulerAnglesZYX(new Vector3f());
        part.xRot = euler.x;
        part.yRot = euler.y;
        part.zRot = euler.z;
    }

    private Matrix4f universalEmotes$bodyCapeDelta() {
        Matrix4f d = new Matrix4f();
        d.translate(body.x / 16f, body.y / 16f, body.z / 16f);
        if (body.xRot != 0f || body.yRot != 0f || body.zRot != 0f) {
            d.rotateZYX(body.zRot, body.yRot, body.xRot);
        }
        return d;
    }
}
