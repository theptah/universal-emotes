package com.ptah.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

//? if <1.19.3 {
/*import java.nio.FloatBuffer;
*///?}

public final class MathCompat {
    private MathCompat() { }

    public static Matrix4f pose(PoseStack.Pose pose) {
        //? if >=1.19.3 {
        return pose.pose();
        //?} else {
        /*float[] values = new float[16];
        pose.pose().store(FloatBuffer.wrap(values));
        return new Matrix4f().set(values);
        *///?}
    }

    public static Matrix3f normal(PoseStack.Pose pose) {
        //? if >=1.19.3 {
        return pose.normal();
        //?} else {
        /*float[] values = new float[9];
        pose.normal().store(FloatBuffer.wrap(values));
        return new Matrix3f().set(values);
        *///?}
    }

    public static void mul(PoseStack stack, Matrix4f matrix) {
        //? if >=1.19.3 {
        stack.last().pose().mul(matrix);
        stack.last().normal().mul(new Matrix3f(matrix));
        //?} else {
        /*com.mojang.math.Matrix4f pose = new com.mojang.math.Matrix4f();
        pose.load(FloatBuffer.wrap(matrix.get(new float[16])));
        stack.last().pose().multiply(pose);
        com.mojang.math.Matrix3f normal = new com.mojang.math.Matrix3f();
        normal.load(FloatBuffer.wrap(new Matrix3f(matrix).get(new float[9])));
        stack.last().normal().mul(normal);
        *///?}
    }

    public static void rotate(PoseStack stack, Quaternionf rotation) {
        //? if >=1.19.3 {
        stack.mulPose(rotation);
        //?} else
        /*stack.mulPose(new com.mojang.math.Quaternion(rotation.x, rotation.y, rotation.z, rotation.w));*/
    }

    public static void rotateXDegrees(PoseStack stack, float degrees) {
        rotate(stack, new Quaternionf().rotationX((float) Math.toRadians(degrees)));
    }

    public static void rotateYDegrees(PoseStack stack, float degrees) {
        rotate(stack, new Quaternionf().rotationY((float) Math.toRadians(degrees)));
    }

    public static void rotateZDegrees(PoseStack stack, float degrees) {
        rotate(stack, new Quaternionf().rotationZ((float) Math.toRadians(degrees)));
    }

    public static Quaternionf cameraRotation(Camera camera) {
        //? if >=1.19.3 {
        return new Quaternionf(camera.rotation());
        //?} else {
        /*com.mojang.math.Quaternion q = camera.rotation();
        return new Quaternionf(q.i(), q.j(), q.k(), q.r());
        *///?}
    }
}
