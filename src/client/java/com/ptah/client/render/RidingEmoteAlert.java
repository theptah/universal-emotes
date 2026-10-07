package com.ptah.client.render;

import com.ptah.client.compat.ClientCompat;
import com.ptah.client.compat.GuiCompat;
import com.ptah.compat.Ids;
import com.ptah.registry.ModSounds;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.Optional;

public final class RidingEmoteAlert {
    private static final ResourceLocation ALERT_BOX =
            Ids.of("universal-emotes", "textures/gui/alert_box.png");
    private static final ResourceLocation BLOCKED_ICON =
            Ids.of("universal-emotes", "textures/gui/blocked_icon.png");
    private static final String MESSAGE = "You can't use emotes while riding!";

    private static final int BOX_TEX_W = 1084;
    private static final int BOX_TEX_H = 88;
    private static final int ICON_TEX = 512;

    private static final int BOX_W = 229;
    private static final int BOX_H = 19;
    private static final int ICON_SIZE = 9;
    private static final int TOP_GAP = 40;
    private static final int ICON_TEXT_GAP = 6;

    private static final float FONT_SCALE = 0.8f;

    private static final double REFERENCE_GUI_SCALE = 3.0;

    private static final int ICON_MIP_LEVELS = 4;

    private static final float SLIDE_IN_MS = 450.0f;
    private static final float HOLD_MS = 2000.0f;
    private static final float SLIDE_OUT_MS = 260.0f;

    private static boolean active;
    private static long startMillis;

    private static boolean iconPrepared;
    private static ResourceLocation iconTexture;
    private static int iconTexSize;

    private RidingEmoteAlert() {
    }

    public static void show() {
        active = true;
        startMillis = System.currentTimeMillis();

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && ModSounds.RIDING_ALERT != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.RIDING_ALERT, 1.0f, 1.0f));
        }
    }

    public static void render(GuiGraphics graphics) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        long elapsed = System.currentTimeMillis() - startMillis;

        Window window = mc.getWindow();
        double guiScale = window.getGuiScale();
        float comp = guiScale > 0.0 ? (float) (REFERENCE_GUI_SCALE / guiScale) : 1.0f;

        int screenW = graphics.guiWidth();

        float boxW = BOX_W;
        float boxH = BOX_H;

        float restY = TOP_GAP * comp;
        float hiddenY = (-boxH - 4.0f) * comp;

        float y;
        if (elapsed < SLIDE_IN_MS) {
            float t = elapsed / SLIDE_IN_MS;
            y = lerp(hiddenY, restY, easeOutCubic(t));
        } else if (elapsed < SLIDE_IN_MS + HOLD_MS) {
            y = restY;
        } else if (elapsed < SLIDE_IN_MS + HOLD_MS + SLIDE_OUT_MS) {
            float t = (elapsed - SLIDE_IN_MS - HOLD_MS) / SLIDE_OUT_MS;
            y = lerp(restY, hiddenY, easeInCubic(t));
        } else {
            active = false;
            return;
        }

        float boxX = (screenW - boxW * comp) / 2.0f;

        prepareIcon(mc);

        GuiCompat.enableBlend();

        GuiCompat.push(graphics);

        GuiCompat.translate(graphics, boxX, y, 0.0f);
        GuiCompat.scale(graphics, comp, comp);

        int boxWi = Math.round(boxW);
        int boxHi = Math.round(boxH);

        GuiCompat.blitStretched(graphics, ALERT_BOX, 0, 0, boxWi, boxHi,
                0.0f, 0.0f, BOX_TEX_W, BOX_TEX_H, BOX_TEX_W, BOX_TEX_H);

        Font font = mc.font;
        int iconSize = ICON_SIZE;
        float textWidth = font.width(MESSAGE) * FONT_SCALE;
        float pairWidth = iconSize + ICON_TEXT_GAP + textWidth;

        float centerX = boxW / 2.0f;
        float centerY = boxH / 2.0f;
        float startX = centerX - pairWidth / 2.0f;

        ResourceLocation iconTex = iconTexture != null ? iconTexture : BLOCKED_ICON;
        int iconRegion = iconTexture != null ? iconTexSize : ICON_TEX;
        int iconX = Math.round(startX);
        int iconY = Math.round(centerY - iconSize / 2.0f);
        GuiCompat.blitStretched(graphics, iconTex, iconX, iconY, iconSize, iconSize,
                0.0f, 0.0f, iconRegion, iconRegion, iconRegion, iconRegion);

        float textX = startX + iconSize + ICON_TEXT_GAP;
        float textY = centerY - (font.lineHeight * FONT_SCALE) / 2.0f;
        GuiCompat.push(graphics);
        GuiCompat.translate(graphics, textX, textY, 0.0f);
        GuiCompat.scale(graphics, FONT_SCALE, FONT_SCALE);
        graphics.drawString(font, MESSAGE, 0, 0, 0xFFFFFFFF, false);
        GuiCompat.pop(graphics);

        GuiCompat.pop(graphics);

        GuiCompat.disableBlend();
    }

    private static void prepareIcon(Minecraft mc) {
        if (iconPrepared) return;
        iconPrepared = true;
        try {
            Optional<net.minecraft.server.packs.resources.Resource> resOpt =
                    mc.getResourceManager().getResource(BLOCKED_ICON);
            if (resOpt.isEmpty()) return;

            NativeImage base;
            try (InputStream is = resOpt.get().open()) {
                base = NativeImage.read(is);
            }

            NativeImage[] mips = ClientCompat.mipLevels(base, ICON_MIP_LEVELS);
            NativeImage small = mips[mips.length - 1];

            iconTexSize = small.getWidth();
            iconTexture = ClientCompat.registerDynamicTexture("blocked_icon_mip", small);

            for (int i = 0; i < mips.length - 1; i++) {
                mips[i].close();
            }
        } catch (Exception e) {
            iconTexture = null;
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float easeOutCubic(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }
}
