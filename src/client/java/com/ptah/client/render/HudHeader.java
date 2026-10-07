package com.ptah.client.render;

import com.ptah.client.compat.GuiCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class HudHeader {
    private static final double REFERENCE_GUI_SCALE = 3.0;
    private static final float TEXT_SCALE = 0.62f;
    private static final float TOP_GAP_PX = 48.0f;
    private static final float PAD_X_PX = 28.0f;
    private static final float PAD_Y_PX = 12.0f;
    private static final int HORIZONTAL_GAP = 12;
    private static final int BACKGROUND = 0x66000000;

    private HudHeader() { }

    public static void draw(GuiGraphics graphics, String text) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int screenWidth = graphics.guiWidth();
        int naturalTextWidth = Math.max(1, font.width(text));

        double guiScale = client.getWindow().getGuiScale();
        float scale = TEXT_SCALE * (guiScale > 0.0 ? (float) (REFERENCE_GUI_SCALE / guiScale) : 1.0f);

        scale = Math.min(scale, Math.max(0.10f, (screenWidth - HORIZONTAL_GAP * 2.0f - 14.0f) / naturalTextWidth));

        float gs = (float) (guiScale > 0.0 ? guiScale : REFERENCE_GUI_SCALE);
        int topGap = Math.round(TOP_GAP_PX / gs);
        int padX = Math.round(PAD_X_PX / gs);
        int padY = Math.round(PAD_Y_PX / gs);

        int boxWidth = Math.round(naturalTextWidth * scale) + padX * 2;
        int boxHeight = Math.round(font.lineHeight * scale) + padY * 2;
        int x = (screenWidth - boxWidth) / 2;

        GuiCompat.enableBlend();
        graphics.fill(x, topGap, x + boxWidth, topGap + boxHeight, BACKGROUND);
        GuiCompat.push(graphics);
        GuiCompat.translate(graphics, screenWidth / 2.0f, topGap + padY, 10.0f);
        GuiCompat.scale(graphics, scale, scale);
        graphics.drawString(font, text, -font.width(text) / 2, 0, 0xFFFFFFFF, false);
        GuiCompat.pop(graphics);
        GuiCompat.disableBlend();
    }
}
