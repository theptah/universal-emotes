package com.ptah.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class VertexCompat {
    private VertexCompat() { }

    public static void entity(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                              int red, int green, int blue, int alpha, float u, float v,
                              int overlay, int light, float nx, float ny, float nz) {
        //? if >=1.21 {
        /*vc.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
        *///?} else {
        vc.vertex(pose.pose(), x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                //? if >=1.20.5 {
                /*.normal(pose, nx, ny, nz)
                *///?} else
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
        //?}
    }

    public static void entity(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                              float red, float green, float blue, float alpha, float u, float v,
                              int overlay, int light, float nx, float ny, float nz) {
        entity(vc, pose, x, y, z, toByte(red), toByte(green), toByte(blue), toByte(alpha), u, v, overlay, light, nx, ny, nz);
    }

    public static void raw(VertexConsumer vc, float x, float y, float z,
                           float red, float green, float blue, float alpha, float u, float v,
                           int overlay, int light, float nx, float ny, float nz) {
        //? if >=1.21 {
        /*int color = toByte(alpha) << 24 | toByte(red) << 16 | toByte(green) << 8 | toByte(blue);
        vc.addVertex(x, y, z, color, u, v, overlay, light, nx, ny, nz);
        *///?} else
        vc.vertex(x, y, z, red, green, blue, alpha, u, v, overlay, light, nx, ny, nz);
    }

    private static int toByte(float value) {
        return Math.max(0, Math.min(255, (int) (value * 255.0f)));
    }
}
