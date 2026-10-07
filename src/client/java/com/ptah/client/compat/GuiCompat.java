package com.ptah.client.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

//? if <1.21.5
import com.mojang.blaze3d.systems.RenderSystem;
//? if >=1.21.6 {
/*import net.minecraft.client.renderer.RenderPipelines;
*///?} elif >=1.21.2
/*import net.minecraft.client.renderer.RenderType;*/
//? if <1.21.6
import org.joml.Quaternionf;

public final class GuiCompat {
    private GuiCompat() { }

    public static void push(GuiGraphics g) {
        //? if >=1.21.6 {
        /*g.pose().pushMatrix();
        *///?} else
        g.pose().pushPose();
    }

    public static void pop(GuiGraphics g) {
        //? if >=1.21.6 {
        /*g.pose().popMatrix();
        *///?} else
        g.pose().popPose();
    }

    public static void translate(GuiGraphics g, float x, float y, float z) {
        //? if >=1.21.6 {
        /*g.pose().translate(x, y);
        *///?} else
        g.pose().translate(x, y, z);
    }

    public static void scale(GuiGraphics g, float x, float y) {
        //? if >=1.21.6 {
        /*g.pose().scale(x, y);
        *///?} else
        g.pose().scale(x, y, 1.0f);
    }

    public static void rotateZ(GuiGraphics g, float radians) {
        //? if >=1.21.6 {
        /*g.pose().rotate(radians);
        *///?} else
        MathCompat.rotate(g.pose(), new Quaternionf().rotationZ(radians));
    }

    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v,
                            int width, int height, int textureWidth, int textureHeight) {
        //? if >=1.21.6 {
        /*g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        *///?} elif >=1.21.2 {
        /*g.blit(RenderType::guiTextured, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        *///?} else
        g.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void blitStretched(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
                                     float u, float v, int regionWidth, int regionHeight,
                                     int textureWidth, int textureHeight) {
        //? if >=1.21.6 {
        /*g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
        *///?} elif >=1.21.2 {
        /*g.blit(RenderType::guiTextured, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
        *///?} else
        g.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    public static void blitTinted(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v,
                                  int width, int height, int textureWidth, int textureHeight, int argb) {
        //? if >=1.21.6 {
        /*g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight, argb);
        *///?} elif >=1.21.2 {
        /*g.blit(RenderType::guiTextured, texture, x, y, u, v, width, height, textureWidth, textureHeight, argb);
        *///?} else {
        g.setColor(((argb >> 16) & 0xFF) / 255.0f, ((argb >> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f, ((argb >>> 24) & 0xFF) / 255.0f);
        g.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        //?}
    }

    public static void flush(GuiGraphics g) {
        //? if <1.21.6
        g.flush();
    }

    public static void enableBlend() {
        //? if <1.21.5 {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        //?}
    }

    public static void disableBlend() {
        //? if <1.21.5
        RenderSystem.disableBlend();
    }

    public static void depthTest(boolean enabled) {
        //? if <1.21.5 {
        if (enabled) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
        //?}
    }
}
