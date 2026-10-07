package com.ptah.client;

import com.ptah.compat.Ids;
import com.ptah.client.compat.WorldRenderCompat;
import com.ptah.client.compat.ClientCompat;
import com.ptah.client.camera.EmoteCameraController;
import com.ptah.client.config.ClientConfigManager;
import com.ptah.client.resource.BundleResourcePack;
import com.ptah.client.network.EmoteClientNetwork;
import com.ptah.client.playback.ClientPlaybackRuntime;
import com.ptah.client.screen.EmoteWheelScreen;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.ptah.client.render.RidingEmoteAlert;
import com.ptah.client.render.DanceTogetherPrompt;
import com.ptah.client.render.CustomEmoteParticles;
import com.ptah.client.render.BundleModelRenderer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class UniversalEmotesClient implements ClientModInitializer {
    //? if >=1.21.9 {
    /*private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Ids.mod("wheel"));
    *///?} else
    private static final String KEY_CATEGORY = "category.universal-emotes";

    private static final KeyMapping OPEN_WHEEL = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.universal-emotes.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            KEY_CATEGORY
    ));

    private static final KeyMapping LOCK_SELECTION = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.universal-emotes.lock",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            KEY_CATEGORY
    ));

    private static boolean selectionLocked;
    private static boolean suppressOpenUntilReleased;

    private static boolean cancelSent;

    public static boolean isSelectionLocked() {
        return selectionLocked;
    }

    public static void toggleSelectionLock() {
        selectionLocked = !selectionLocked;

        ClientConfigManager.get().selectionLocked = selectionLocked;
        ClientConfigManager.save();
    }

    public static void suppressOpenUntilKeyRelease() {
        suppressOpenUntilReleased = true;
        while (OPEN_WHEEL.consumeClick()) {
        }
    }

    private static boolean isOpenKeyPhysicallyDown(Minecraft client) {
        long window = ClientCompat.windowHandle();

        for (int keyCode = GLFW.GLFW_KEY_SPACE; keyCode <= GLFW.GLFW_KEY_LAST; keyCode++) {
            int scanCode = GLFW.glfwGetKeyScancode(keyCode);
            if (ClientCompat.matchesKey(OPEN_WHEEL, keyCode, scanCode)) {
                return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            }
        }

        for (int button = 0; button <= GLFW.GLFW_MOUSE_BUTTON_LAST; button++) {
            if (ClientCompat.matchesMouse(OPEN_WHEEL, button)) {
                return GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
            }
        }

        return OPEN_WHEEL.isDown();
    }

    @Override
    public void onInitializeClient() {
        com.ptah.client.dev.MixinAudit.registerIfRequested();
        //? if >=1.21.6
        /*com.ptah.client.screen.PreviewEffectsPip.register();*/
        ClientConfigManager.load();
        selectionLocked = ClientConfigManager.get().selectionLocked;
        EmoteClientNetwork.initialize();

        WorldRenderCompat.afterEntities(CustomEmoteParticles::render);

        WorldRenderCompat.afterEntities(BundleModelRenderer::render);

        WorldRenderCompat.frameStart(com.ptah.client.playback.EmoteMusicHandle::frameAll);

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            //? if <1.20 {
            /*var guiGraphics = new com.ptah.client.compat.legacy.GuiGraphics(graphics);
            *///?} else
            var guiGraphics = graphics;
            RidingEmoteAlert.render(guiGraphics);
            DanceTogetherPrompt.render(guiGraphics, OPEN_WHEEL, ClientCompat.partialTick());
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) { CustomEmoteParticles.clear(); BundleModelRenderer.clearAll(); }
            else { CustomEmoteParticles.tick(); BundleModelRenderer.tick(); }
        });

        Minecraft.getInstance().execute(() -> BundleResourcePack.reload());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (suppressOpenUntilReleased) {
                while (OPEN_WHEEL.consumeClick()) {
                }
                if (!isOpenKeyPhysicallyDown(client)) {
                    suppressOpenUntilReleased = false;
                    OPEN_WHEEL.setDown(false);
                }
            } else {
                while (OPEN_WHEEL.consumeClick()) {
                    if (client.screen == null) {
                        if (DanceTogetherPrompt.tryStart(client)) {
                            suppressOpenUntilKeyRelease();
                        } else {
                            client.setScreen(new EmoteWheelScreen(OPEN_WHEEL, LOCK_SELECTION));
                        }
                    }
                }
            }

            while (LOCK_SELECTION.consumeClick()) {
                if (!(client.screen instanceof EmoteWheelScreen)) {
                    toggleSelectionLock();
                }
            }

            tickCancelOnInput(client);

            EmoteCameraController.clientTick();
        });
    }

    private static void tickCancelOnInput(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.screen != null || selectionLocked) {
            cancelSent = false;
            return;
        }
        if (ClientPlaybackRuntime.MANAGER.active(player.getUUID()).isEmpty()) {
            cancelSent = false;
            return;
        }
        if (cancelSent) {
            return;
        }

        var options = client.options;
        boolean gameplayInput = options.keyUp.isDown()
                || options.keyDown.isDown()
                || options.keyLeft.isDown()
                || options.keyRight.isDown()
                || options.keyJump.isDown()
                || options.keyShift.isDown()
                || options.keyAttack.isDown()
                || options.keyUse.isDown();

        if (gameplayInput) {
            cancelSent = EmoteClientNetwork.requestStop();
        }
    }
}
