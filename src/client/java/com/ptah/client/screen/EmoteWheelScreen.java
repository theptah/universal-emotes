package com.ptah.client.screen;

import com.ptah.client.compat.GuiCompat;
import com.ptah.compat.Ids;
import com.ptah.client.compat.ClientCompat;
import com.ptah.bundle.EmoteDefinition;
import com.ptah.bundle.Rarity;
import com.ptah.client.UniversalEmotesClient;
import com.ptah.client.UniversalEmotesConfig;
import com.ptah.client.config.ClientConfigManager;
import com.ptah.client.config.SyncedServerConfig;
import com.ptah.client.network.EmoteClientNetwork;
import com.ptah.client.playback.ClientPlaybackRuntime;
import com.ptah.client.render.HudHeader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.ptah.system.EmoteSystem;
import com.ptah.registry.ModSounds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EmoteWheelScreen extends Screen {
    private static final String[] FILTERS = {"ALL", "FAVORITES", "LEGENDARY", "RARE", "UNCOMMON", "COMMON", "COMPLEMENTARY"};
    private static final int[] FILTER_COLORS = {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFF4C4C, 0xFFD24CFF, 0xFF4CA8FF, 0xFF55FF4C, 0xFFBDBDBD};
    private static final int FILTER_PANEL_X = 174;
    private static final int FILTER_PANEL_Y = 243;
    private static final int FILTER_BUTTON_WIDTH = 120;
    private static final int FILTER_BUTTON_HEIGHT = 22;
    private static final int FILTER_BUTTON_STEP = 32;
    private static final float MINECRAFT_FONT_HEIGHT = 9.0f;
    private static final float WHEEL_EMOTE_TEXT_SCALE = 1.38f;

    private static final float WHEEL_META_TEXT_SCALE = WHEEL_EMOTE_TEXT_SCALE - (3.0f / MINECRAFT_FONT_HEIGHT);

    private static final int[] WHEEL_NAME_MAX_WIDTH = { 63, 50, 44, 44, 63, 44, 44, 50 };
    private static final int[] WHEEL_NAME_MAX_LINES = { 2, 1, 4, 3, 2, 3, 4, 1 };
    private static final ResourceLocation WHEEL_BASE = Ids.of("universal-emotes", "textures/gui/wheel_base.png");

    private static final ResourceLocation INDICATOR = Ids.of("universal-emotes", "textures/gui/indicator.png");
    private static final int INDICATOR_WIDTH = 24;
    private static final int INDICATOR_HEIGHT = 104;
    private static final float INDICATOR_RADIUS = 90.0f;

    private static final float INDICATOR_ANGLE_OFFSET_DEG = 1.57f;

    private static final float INDICATOR_TIP_FRAC_X = 0.70f;
    private static final float INDICATOR_TIP_FRAC_Y = 0.53f;

    private static final float INDICATOR_ROTATE_SPEED = 10.0f;

    private static final float UI_SOUND_VOLUME = 1.0f;
    private static final ResourceLocation LOCK_ICON = Ids.of("universal-emotes", "textures/gui/lock_icon.png");
    private static final int LOCK_ICON_NATIVE = 120;

    private static final float LOCK_OVERLAY_OPACITY = 0.95f;
    private static final int LOCK_TINT_R = 0x32;
    private static final int LOCK_TINT_G = 0x32;
    private static final int LOCK_TINT_B = 0x32;

    private static final ResourceLocation[] WHEEL_MASKS = new ResourceLocation[8];

    static {
        for (int i = 0; i < WHEEL_MASKS.length; i++) {
            WHEEL_MASKS[i] = Ids.of("universal-emotes", "textures/gui/wheel_mask_" + (i + 1) + ".png");
        }
    }

    private final KeyMapping openKey;
    private final KeyMapping lockKey;

    private final Set<String> favorites;
    private int selected = -1;

    private boolean confirmed;

    private static int lastFilter = 0;
    private static int lastPage = 1;
    private int activeFilter = lastFilter;
    private int page = lastPage;
    private boolean lockKeyHeld;
    private int hoveredEmote = -1;

    private String lastHoverSoundEmoteId;
    private final float[] hoverOpacities = new float[8];
    private long lastHoverFrameNanos = System.nanoTime();

    private float indicatorAngle;
    private boolean indicatorAngleInit;
    private long lastIndicatorNanos = System.nanoTime();

    private float scale;
    private float originX;
    private float originY;
    private float wheelX;
    private float wheelY;

    public EmoteWheelScreen(KeyMapping openKey, KeyMapping lockKey) {
        super(Component.literal("Emote Wheel"));
        this.openKey = openKey;
        this.lockKey = lockKey;
        this.favorites = new HashSet<>(UniversalEmotesConfig.loadFavorites());
    }

    private List<EmoteDefinition> filteredEmotesFor(int filterIndex) {
        List<EmoteDefinition> all = new ArrayList<>(EmoteSystem.EMOTES.values());
        if (filterIndex == 0) {
            return all;
        }
        if (filterIndex == 1) {
            List<EmoteDefinition> favorited = new ArrayList<>();
            for (EmoteDefinition emote : all) {
                if (favorites.contains(emote.id().toString())) favorited.add(emote);
            }
            return favorited;
        }

        Rarity rarity = Rarity.values()[filterIndex - 2];
        List<EmoteDefinition> byRarity = new ArrayList<>();
        for (EmoteDefinition emote : all) {
            if (emote.rarity() == rarity) byRarity.add(emote);
        }
        return byRarity;
    }

    private List<EmoteDefinition> currentFiltered() {
        List<EmoteDefinition> filtered = new ArrayList<>(filteredEmotesFor(activeFilter));
        filtered.sort(Comparator.comparingInt(emote -> isLocked(emote) ? 1 : 0));
        return filtered;
    }

    private int totalPages() {
        return Math.max(1, (currentFiltered().size() + 7) / 8);
    }

    private EmoteDefinition emoteAt(int slot) {
        if (slot < 0 || slot >= 8) return null;
        List<EmoteDefinition> filtered = currentFiltered();
        int index = (page - 1) * 8 + slot;
        return index >= 0 && index < filtered.size() ? filtered.get(index) : null;
    }

    private void clampPage() {
        int pages = totalPages();
        if (page > pages) page = pages;
        if (page < 1) page = 1;
    }

    private static int rarityColor(Rarity rarity) {
        return FILTER_COLORS[rarity.ordinal() + 2];
    }

    private int missingOpLevel(EmoteDefinition emote) {
        LocalPlayer player = minecraft != null ? minecraft.player : null;
        return player == null ? emote.op() : SyncedServerConfig.get().missingOpLevel(player, emote);
    }

    private int missingLevel(EmoteDefinition emote) {
        LocalPlayer player = minecraft != null ? minecraft.player : null;
        return player == null ? 0 : SyncedServerConfig.get().missingLevel(player, emote);
    }

    private boolean isLocked(EmoteDefinition emote) {
        return emote != null && (missingOpLevel(emote) > 0 || missingLevel(emote) > 0);
    }

    private String lockLabel(EmoteDefinition emote) {
        int op = missingOpLevel(emote);
        if (op > 0) return "OP Level " + op;
        int level = missingLevel(emote);
        return level > 0 ? "LEVEL " + level : "";
    }

    private void drawLockIcon(GuiGraphics g, float centerX, float centerY, int size) {
        GuiCompat.enableBlend();
        float s = size / (float) LOCK_ICON_NATIVE;
        GuiCompat.push(g);
        GuiCompat.translate(g, centerX, centerY, 20.0f);
        GuiCompat.scale(g, s, s);
        GuiCompat.blit(g, LOCK_ICON, -LOCK_ICON_NATIVE / 2, -LOCK_ICON_NATIVE / 2, 0.0f, 0.0f,
                LOCK_ICON_NATIVE, LOCK_ICON_NATIVE, LOCK_ICON_NATIVE, LOCK_ICON_NATIVE);
        GuiCompat.pop(g);
    }

    //? if >=1.20.2 {
    /*@Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    *///?}
    //? if <1.20 {
    /*@Override
    public void render(com.mojang.blaze3d.vertex.PoseStack poseStack, int mouseX, int mouseY, float delta) {
        render(new GuiGraphics(poseStack), mouseX, mouseY, delta);
    }

    *///?} else
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        updateLayout();
        clampPage();
        updateSelection(mouseX, mouseY);
        updateHoverSound();
        updateHoverAnimation();

        renderInfo(graphics);

        GuiCompat.push(graphics);
        GuiCompat.translate(graphics, originX, originY, 0);
        GuiCompat.scale(graphics, scale, scale);

        renderFilters(graphics, mouseX, mouseY);
        renderWheel(graphics);
        renderIndicator(graphics, mouseX, mouseY);

        GuiCompat.pop(graphics);

        renderHoveredPreview(graphics);

        //? if <1.20 {
        /*super.render(graphics.pose(), mouseX, mouseY, delta);
        *///?} else
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderHoveredPreview(GuiGraphics g) {
        LocalPlayer player = minecraft != null ? minecraft.player : null;
        EmoteDefinition emote = hoveredEmote >= 0 ? emoteAt(hoveredEmote) : null;
        if (player == null || emote == null || isLocked(emote)) {
            EmoteWheelPreview.clear();
            EmoteWheelPreviewEffects.clear();
            return;
        }

        EmoteWheelPreview.setHovered(emote);

        try {
            EmoteWheelPreviewEffects.update(emote);
        } catch (Throwable ignored) {
        }

        float centerX = originX + wheelX * scale;
        float centerY = originY + wheelY * scale;

        int size = Math.max(30, Math.round(58.0f * scale));
        int anchorX = Math.round(centerX);
        int anchorY = Math.round(centerY + size * 0.9f);

        int boxHalfW = Math.round(size * 1.15f);
        int boxHalfH = Math.round(size * 1.7f);
        int boxX0 = Math.round(centerX) - boxHalfW;
        int boxY0 = Math.round(centerY) - boxHalfH;
        int boxX1 = Math.round(centerX) + boxHalfW;
        int boxY1 = Math.round(centerY) + boxHalfH;

        EmoteWheelPreview.beginRender();
        g.enableScissor(boxX0, boxY0, boxX1, boxY1);
        try {
            ClientCompat.renderEntityFacingViewer(g, boxX0, boxY0, boxX1, boxY1, size, player);

            try {
                EmoteWheelPreviewEffects.render(g, boxX0, boxY0, boxX1, boxY1, anchorX, anchorY, size);
            } catch (Throwable ignored) {
            }
        } finally {
            g.disableScissor();
            EmoteWheelPreview.endRender();
        }
    }

    private void updateLayout() {
        scale = Math.min(width / 1000.0f, height / 700.0f);

        double guiScale = minecraft != null ? minecraft.getWindow().getGuiScale() : 3.0;
        if (guiScale < 1.5) {
            scale *= 1.18f;
        } else if (guiScale < 2.5) {
            scale *= 1.10f;
        }

        scale = Math.max(0.55f, scale);
        originX = (width - 1000.0f * scale) / 2.0f;
        originY = (height - 700.0f * scale) / 2.0f;
        wheelX = 500.0f;
        wheelY = 350.0f;
    }

    private void updateSelection(double mouseX, double mouseY) {
        double x = (mouseX - originX) / scale - wheelX;
        double y = (mouseY - originY) / scale - wheelY;
        double distance = Math.sqrt(x * x + y * y);

        if (distance < 104.0 || distance > 178.0) {
            hoveredEmote = -1;
            selected = -1;
            return;
        }

        double mouseAngle = Math.atan2(y, x);
        int candidate = Math.floorMod((int) Math.round((Math.PI / 2.0 - mouseAngle) / (Math.PI / 4.0)), 8);
        double buttonCenter = Math.toRadians(90.0 - candidate * 45.0);
        double angleDelta = Math.atan2(
                Math.sin(mouseAngle - buttonCenter),
                Math.cos(mouseAngle - buttonCenter)
        );

        if (Math.abs(angleDelta) > Math.toRadians(20.9)) {
            hoveredEmote = -1;
            selected = -1;
            return;
        }

        EmoteDefinition candidateEmote = emoteAt(candidate);
        if (candidateEmote == null || isLocked(candidateEmote)) {
            hoveredEmote = -1;
            selected = -1;
            return;
        }

        selected = candidate;
        hoveredEmote = candidate;
    }

    private void updateHoverSound() {
        EmoteDefinition hovered = hoveredEmote >= 0 ? emoteAt(hoveredEmote) : null;
        String hoveredId = hovered != null ? hovered.id().toString() : null;
        if (hoveredId != null && !hoveredId.equals(lastHoverSoundEmoteId)) {
            playUiSound(ModSounds.WHEEL_HOVER);
        }
        lastHoverSoundEmoteId = hoveredId;
    }

    private void playUiSound(SoundEvent event) {
        if (minecraft != null && event != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0f, UI_SOUND_VOLUME));
        }
    }

    private void updateHoverAnimation() {
        long now = System.nanoTime();
        float elapsedSeconds = Math.min((now - lastHoverFrameNanos) / 1_000_000_000.0f, 0.05f);
        lastHoverFrameNanos = now;

        for (int i = 0; i < hoverOpacities.length; i++) {
            float target = i == hoveredEmote ? 0.75f : 0.0f;
            float speed = target > hoverOpacities[i] ? 5.5f : 4.0f;
            float step = speed * elapsedSeconds;

            if (hoverOpacities[i] < target) {
                hoverOpacities[i] = Math.min(target, hoverOpacities[i] + step);
            } else if (hoverOpacities[i] > target) {
                hoverOpacities[i] = Math.max(target, hoverOpacities[i] - step);
            }
        }
    }

    private void renderInfo(GuiGraphics g) {
        String lockKeyName = lockKey.getTranslatedKeyMessage().getString();
        HudHeader.draw(g, UniversalEmotesClient.isSelectionLocked()
                ? "[MOUSE] Selection locked \u2022 [" + lockKeyName + "] Click to unlock"
                : "[MOUSE] Move to select \u2022 [" + lockKeyName + "] Click to lock");
    }

    private int getFilterCount(int filterIndex) {
        return filteredEmotesFor(filterIndex).size();
    }

    private void renderFilters(GuiGraphics g, int rawMouseX, int rawMouseY) {
        int x = FILTER_PANEL_X;
        int y = FILTER_PANEL_Y;
        int mx = Math.round((rawMouseX - originX) / scale);
        int my = Math.round((rawMouseY - originY) / scale);
        for (int i = 0; i < FILTERS.length; i++) {
            int yy = y + i * FILTER_BUTTON_STEP;
            boolean hover = mx >= x && mx < x + FILTER_BUTTON_WIDTH
                    && my >= yy && my < yy + FILTER_BUTTON_HEIGHT;
            int bg = i == activeFilter ? 0xF04A4A4A : (hover ? 0xE6404040 : 0xE0323232);
            g.fill(x, yy, x + FILTER_BUTTON_WIDTH, yy + FILTER_BUTTON_HEIGHT, bg);
            String filterLabel = FILTERS[i] + " [" + getFilterCount(i) + "]";
            drawCentered(g, filterLabel, x + FILTER_BUTTON_WIDTH / 2, yy + 6, FILTER_COLORS[i], 1.12f);
        }
    }

    private void renderWheel(GuiGraphics g) {
        int wheelLeft = Math.round(wheelX - 178);
        int wheelTop = Math.round(wheelY - 178);

        GuiCompat.enableBlend();
        GuiCompat.blit(g, WHEEL_BASE, wheelLeft, wheelTop, 0.0f, 0.0f, 356, 356, 356, 356);
        GuiCompat.flush(g);

        for (int i = 0; i < 8; i++) {
            double center = Math.toRadians(90.0 - i * 45.0);
            float tx = wheelX + (float) Math.cos(center) * 142;
            float ty = wheelY + (float) Math.sin(center) * 142;
            EmoteDefinition emote = emoteAt(i);
            if (emote == null) continue;

            drawWrappedCentered(g, emote.name(), Math.round(tx), Math.round(ty - 7), 0xFFFFFFFF,
                    WHEEL_EMOTE_TEXT_SCALE, WHEEL_NAME_MAX_WIDTH[i], WHEEL_NAME_MAX_LINES[i]);
        }

        drawCentered(g, "PAGE [" + page + " / " + totalPages() + "]", Math.round(wheelX), Math.round(wheelY - 74), 0xFFFFFFFF, WHEEL_META_TEXT_SCALE);
        drawCentered(g, "[MOUSE SCROLL]", Math.round(wheelX), Math.round(wheelY - 62), 0xFFFFFFFF, WHEEL_META_TEXT_SCALE);
        EmoteDefinition selectedEmote = selected >= 0 ? emoteAt(selected) : null;
        if (selectedEmote != null) {
            drawCenteredSelectedEmote(g, selectedEmote.name(), rarityColor(selectedEmote.rarity()), Math.round(wheelX), Math.round(wheelY + 58), WHEEL_META_TEXT_SCALE);

            drawCentered(g, "MMB [FAVORITE]" + (favorites.contains(selectedEmote.id().toString()) ? " \u2605" : ""), Math.round(wheelX), Math.round(wheelY + 70), 0xFFFFFFFF, WHEEL_META_TEXT_SCALE);
        }

        int accent = ClientConfigManager.get().accentColor;
        int accentR = (accent >> 16) & 0xFF;
        int accentG = (accent >> 8) & 0xFF;
        int accentB = accent & 0xFF;
        for (int i = 0; i < hoverOpacities.length; i++) {
            if (hoverOpacities[i] > 0.001f) {
                drawTintedTexture(g, WHEEL_MASKS[i], wheelLeft, wheelTop, 356, 356, hoverOpacities[i], accentR, accentG, accentB);
            }
        }

        for (int i = 0; i < 8; i++) {
            EmoteDefinition emote = emoteAt(i);
            if (!isLocked(emote)) continue;
            drawTintedTexture(g, WHEEL_MASKS[i], wheelLeft, wheelTop, 356, 356,
                    LOCK_OVERLAY_OPACITY, LOCK_TINT_R, LOCK_TINT_G, LOCK_TINT_B);

            double center = Math.toRadians(90.0 - i * 45.0);
            float tx = wheelX + (float) Math.cos(center) * 142;
            float ty = wheelY + (float) Math.sin(center) * 142;

            drawLockIcon(g, tx, ty - 8, 22);
            drawCentered(g, lockLabel(emote), Math.round(tx), Math.round(ty + 9), 0xFFFFFFFF, WHEEL_META_TEXT_SCALE);
        }
        GuiCompat.disableBlend();
    }

    private static void drawTintedTexture(GuiGraphics g, ResourceLocation texture,
                                          int x, int y, int width, int height,
                                          float opacity, int red, int green, int blue) {
        GuiCompat.flush(g);
        GuiCompat.enableBlend();
        GuiCompat.depthTest(false);
        int alpha = Math.round(255.0f * opacity);
        GuiCompat.push(g);
        GuiCompat.translate(g, 0.0f, 0.0f, 30.0f);
        GuiCompat.blitTinted(g, texture, x, y, 0.0f, 0.0f, width, height, width, height,
                alpha << 24 | red << 16 | green << 8 | blue);
        GuiCompat.pop(g);
        GuiCompat.flush(g);
        GuiCompat.depthTest(true);
    }

    private void drawCenteredSelectedEmote(GuiGraphics g, String emoteName, int nameColor, int x, int y, float textScale) {
        int openingWidth = font.width("[");
        int emoteWidth = font.width(emoteName);
        int totalWidth = openingWidth + emoteWidth + font.width("]");
        int cursor = -totalWidth / 2;

        GuiCompat.push(g);
        GuiCompat.translate(g, x, y, 10);
        GuiCompat.scale(g, textScale, textScale);
        g.drawString(font, "[", cursor, 0, 0xFFFFFFFF, false);
        cursor += openingWidth;

        g.drawString(font, emoteName, cursor, 0, nameColor, false);
        cursor += emoteWidth;
        g.drawString(font, "]", cursor, 0, 0xFFFFFFFF, false);
        GuiCompat.pop(g);
    }

    private void renderIndicator(GuiGraphics g, double mouseX, double mouseY) {
        double localX = (mouseX - originX) / scale;
        double localY = (mouseY - originY) / scale;
        double dx = localX - wheelX;
        double dy = localY - wheelY;

        if (dx == 0.0 && dy == 0.0) return;
        float targetAngle = (float) Math.atan2(dy, dx);

        long now = System.nanoTime();
        float dt = Math.min((now - lastIndicatorNanos) / 1_000_000_000.0f, 0.1f);
        lastIndicatorNanos = now;
        if (!indicatorAngleInit) {
            indicatorAngle = targetAngle;
            indicatorAngleInit = true;
        } else {
            float diff = (float) Math.atan2(Math.sin(targetAngle - indicatorAngle),
                    Math.cos(targetAngle - indicatorAngle));
            float t = 1.0f - (float) Math.exp(-INDICATOR_ROTATE_SPEED * dt);
            indicatorAngle += diff * t;
            indicatorAngle = (float) Math.atan2(Math.sin(indicatorAngle), Math.cos(indicatorAngle));
        }
        float angle = indicatorAngle;

        int w = INDICATOR_WIDTH;
        int h = INDICATOR_HEIGHT;
        int tipX = Math.round(INDICATOR_TIP_FRAC_X * w);
        int tipY = Math.round(INDICATOR_TIP_FRAC_Y * h);

        int accent = ClientConfigManager.get().accentColor;

        GuiCompat.enableBlend();

        GuiCompat.push(g);
        GuiCompat.translate(g, wheelX, wheelY, 50.0f);
        GuiCompat.rotateZ(g, angle);
        GuiCompat.translate(g, INDICATOR_RADIUS, 0.0f, 0.0f);
        GuiCompat.rotateZ(g, (float) Math.toRadians(INDICATOR_ANGLE_OFFSET_DEG));

        GuiCompat.blitTinted(g, INDICATOR, -tipX, -tipY, 0.0f, 0.0f, w, h, w, h, 0xFF000000 | (accent & 0xFFFFFF));
        GuiCompat.pop(g);

        GuiCompat.flush(g);
        GuiCompat.disableBlend();
    }

    private void drawWrappedCentered(GuiGraphics g, String text, int x, int y, int color,
                                     float textScale, int maxWidth, int maxLines) {
        List<String> lines = wrapAndClamp(text, maxWidth, Math.max(1, maxLines));
        float lineHeight = MINECRAFT_FONT_HEIGHT;
        float startY = -((lines.size() - 1) * lineHeight) / 2.0f;
        GuiCompat.push(g);
        GuiCompat.translate(g, x, y, 10);
        GuiCompat.scale(g, textScale, textScale);
        for (int li = 0; li < lines.size(); li++) {
            String line = lines.get(li);
            g.drawString(font, line, -font.width(line) / 2, Math.round(startY + li * lineHeight), color, false);
        }
        GuiCompat.pop(g);
    }

    private List<String> wrapAndClamp(String text, int maxWidth, int maxLines) {
        int safeWidth = Math.max(1, maxWidth);
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (current.length() > 0 && font.width(current.toString() + c) > safeWidth) {
                out.add(current.toString());
                current.setLength(0);

                if (c == ' ') continue;
            }
            current.append(c);
        }
        if (current.length() > 0 || out.isEmpty()) out.add(current.toString());
        if (out.size() > maxLines) {
            String last = out.get(maxLines - 1);
            out = new ArrayList<>(out.subList(0, maxLines));
            out.set(maxLines - 1, ellipsize(last, safeWidth));
        }
        return out;
    }

    private String ellipsize(String line, int maxWidth) {
        String ell = "...";
        if (font.width(line + ell) <= maxWidth) return line + ell;
        String trimmed = line;
        while (!trimmed.isEmpty() && font.width(trimmed + ell) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.trim() + ell;
    }

    private void drawCentered(GuiGraphics g, String text, int x, int y, int color, float textScale) {
        GuiCompat.push(g);
        GuiCompat.translate(g, x, y, 10);
        GuiCompat.scale(g, textScale, textScale);
        g.drawString(font, text, -font.width(text) / 2, 0, color, false);
        GuiCompat.pop(g);
    }

    //? if >=1.21.9 {
    /*@Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return handleMouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return handleKeyPressed(event.key(), event.scancode()) || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
        return handleKeyReleased(event.key(), event.scancode()) || super.keyReleased(event);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return handleMouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return handleKeyPressed(keyCode, scanCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return handleKeyReleased(keyCode, scanCode) || super.keyReleased(keyCode, scanCode, modifiers);
    }
    //?}

    private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        int mx = Math.round((float) ((mouseX - originX) / scale));
        int my = Math.round((float) ((mouseY - originY) / scale));
        int filterX = FILTER_PANEL_X;
        int filterY = FILTER_PANEL_Y;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && mx >= filterX && mx < filterX + FILTER_BUTTON_WIDTH) {
            for (int i = 0; i < FILTERS.length; i++) {
                int yy = filterY + i * FILTER_BUTTON_STEP;
                if (my >= yy && my < yy + FILTER_BUTTON_HEIGHT) {
                    activeFilter = i;
                    page = 1;
                    return true;
                }
            }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && selected >= 0) {
            EmoteDefinition emote = emoteAt(selected);
            if (emote != null) {
                String favoriteId = emote.id().toString();
                boolean added = favorites.add(favoriteId);
                if (!added) favorites.remove(favoriteId);
                UniversalEmotesConfig.saveFavorites(favorites);

                if (added) playUiSound(ModSounds.WHEEL_FAVORITE);
                else playUiSound(ModSounds.WHEEL_UNFAVORITE);
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && selected >= 0) {
            confirmSelection();
            UniversalEmotesClient.suppressOpenUntilKeyRelease();
            onClose();
            return true;
        }
        return false;
    }

    @Override
    //? if >=1.20.2 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int pages = totalPages();
        if (amount < 0) {
            page = page >= pages ? 1 : page + 1;
        } else {
            page = page <= 1 ? pages : page - 1;
        }
        return true;
    }

    public void confirmSelection() {
        if (selected < 0) return;
        EmoteDefinition emote = emoteAt(selected);
        if (emote == null) return;
        EmoteClientNetwork.requestPlay(emote.id());
        confirmed = true;
    }

    @Override
    public void removed() {
        lastFilter = activeFilter;
        lastPage = page;
        super.removed();
    }

    @Override
    public void onClose() {
        if (!confirmed) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null
                    && ClientPlaybackRuntime.MANAGER.active(client.player.getUUID()).isPresent()) {
                EmoteClientNetwork.requestStop();
            }
        }
        EmoteWheelPreview.clear();
        EmoteWheelPreviewEffects.clear();
        super.onClose();
    }

    private boolean handleKeyReleased(int keyCode, int scanCode) {
        if (ClientCompat.matchesKey(openKey, keyCode, scanCode)) {
            confirmSelection();
            onClose();
            return true;
        }
        if (ClientCompat.matchesKey(lockKey, keyCode, scanCode)) {
            lockKeyHeld = false;
            return true;
        }
        return false;
    }

    private boolean handleKeyPressed(int keyCode, int scanCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (ClientCompat.matchesKey(lockKey, keyCode, scanCode)) {
            if (!lockKeyHeld) {
                lockKeyHeld = true;

                UniversalEmotesClient.toggleSelectionLock();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
