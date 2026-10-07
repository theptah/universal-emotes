package com.ptah.client.compat;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.NativeImage;
import com.ptah.compat.Ids;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

//? if >=1.21.11 {
/*import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
*///?} else
import net.minecraft.client.renderer.RenderType;

public final class ClientCompat {
    private ClientCompat() { }

    public static float partialTick() {
        //? if >=1.21.2 {
        /*return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        *///?} elif >=1.21 {
        /*return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        *///?} else
        return Minecraft.getInstance().getFrameTime();
    }

    public static long windowHandle() {
        //? if >=1.21.9 {
        /*return Minecraft.getInstance().getWindow().handle();
        *///?} else
        return Minecraft.getInstance().getWindow().getWindow();
    }

    public static Vec3 cameraPosition(Camera camera) {
        //? if >=1.21.11 {
        /*return camera.position();
        *///?} else
        return camera.getPosition();
    }

    public static MultiBufferSource.BufferSource bufferSource() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    public static RenderType entityTranslucent(ResourceLocation texture) {
        //? if >=1.21.11 {
        /*return RenderTypes.entityTranslucent(texture);
        *///?} else
        return RenderType.entityTranslucent(texture);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation texture) {
        //? if >=1.21.11 {
        /*return RenderTypes.entityCutoutNoCull(texture);
        *///?} else
        return RenderType.entityCutoutNoCull(texture);
    }

    public static ParticleType<?> particleType(ResourceLocation id) {
        //? if >=1.21.2 {
        /*return net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getValue(id);
        *///?} elif >=1.19.3 {
        return net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(id);
        //?} else
        /*return net.minecraft.core.Registry.PARTICLE_TYPE.get(id);*/
    }

    public static NativeImage[] mipLevels(NativeImage base, int levels) {
        //? if >=1.21.11 {
        /*return MipmapGenerator.generateMipLevels(Ids.mod("mipmap"), new NativeImage[]{base}, levels,
                net.minecraft.client.renderer.texture.MipmapStrategy.MEAN, 0.0f);
        *///?} elif >=1.19.3 {
        return MipmapGenerator.generateMipLevels(new NativeImage[]{base}, levels);
        //?} else
        /*return MipmapGenerator.generateMipLevels(base, levels);*/
    }

    public static ResourceLocation registerDynamicTexture(String name, NativeImage image) {
        var textures = Minecraft.getInstance().getTextureManager();
        //? if >=1.21.5 {
        /*DynamicTexture texture = new DynamicTexture(() -> name, image);
        *///?} else
        DynamicTexture texture = new DynamicTexture(image);
        //? if <1.21.11
        texture.setFilter(true, false);
        //? if >=1.21.4 {
        /*ResourceLocation id = Ids.mod("dynamic/" + name);
        textures.register(id, texture);
        return id;
        *///?} else
        return textures.register(name, texture);
    }

    public static boolean matchesKey(KeyMapping mapping, int keyCode, int scanCode) {
        //? if >=1.21.9 {
        /*return mapping.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0));
        *///?} else
        return mapping.matches(keyCode, scanCode);
    }

    public static boolean matchesMouse(KeyMapping mapping, int button) {
        //? if >=1.21.9 {
        /*return mapping.matchesMouse(new net.minecraft.client.input.MouseButtonEvent(0.0, 0.0,
                new net.minecraft.client.input.MouseButtonInfo(button, 0)));
        *///?} else
        return mapping.matchesMouse(button);
    }

    public static int resourcePackFormat() {
        //? if >=1.21.9 {
        /*return net.minecraft.SharedConstants.getCurrentVersion()
                .packVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).major();
        *///?} elif >=1.21.6 {
        /*try {
            return net.minecraft.SharedConstants.getCurrentVersion()
                    .packVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES);
        } catch (Throwable renamedInThisPatch) {
            return 64;
        }
        *///?} elif >=1.19.4 {
        return net.minecraft.SharedConstants.getCurrentVersion()
                .getPackVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES);
        //?} else
        /*return net.minecraft.SharedConstants.RESOURCE_PACK_FORMAT;*/
    }

    public static boolean isSlim(AbstractClientPlayer player) {
        //? if >=1.21.9 {
        /*return player.getSkin().model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;
        *///?} elif >=1.20.2 {
        /*return player.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
        *///?} else
        return "slim".equals(player.getModelName());
    }

    public static void renderEntityFacingViewer(GuiGraphics g, int x0, int y0, int x1, int y1, int size, LivingEntity entity) {
        int centerX = (x0 + x1) / 2;
        int centerY = (y0 + y1) / 2;
        //? if >=1.20.2 {
        /*InventoryScreen.renderEntityInInventoryFollowsMouse(g, x0, y0, x1, y1, size, 0.0f, centerX, centerY, entity);
        *///?} elif >=1.20 {
        int feetY = Math.round(centerY + size * entity.getBbHeight() / 2.0f);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, centerX, feetY, size, 0.0f, 0.0f, entity);
        //?} elif >=1.19.4 {
        /*int feetY = Math.round(centerY + size * entity.getBbHeight() / 2.0f);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g.pose(), centerX, feetY, size, 0.0f, 0.0f, entity);
        *///?} else {
        /*int feetY = Math.round(centerY + size * entity.getBbHeight() / 2.0f);
        InventoryScreen.renderEntityInInventory(centerX, feetY, size, 0.0f, 0.0f, entity);
        *///?}
    }
}
