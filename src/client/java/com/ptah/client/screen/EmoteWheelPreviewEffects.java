package com.ptah.client.screen;

import com.ptah.client.compat.GuiCompat;
import com.ptah.client.compat.ClientCompat;
import com.ptah.client.compat.VertexCompat;
import com.ptah.compat.Ids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
//? if <1.21.6
import com.mojang.blaze3d.platform.Lighting;
import com.ptah.animation.AnimationClip;
import com.ptah.bundle.EmoteDefinition;
import com.ptah.client.render.BundleModelGeometry;
import com.ptah.client.render.BundleModelRenderer;
import com.ptah.client.render.EmoteBoneFrames;
import com.ptah.event.EmoteEvent;
import com.ptah.event.type.RenderModelEvent;
import com.ptah.event.type.SpawnParticleEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class EmoteWheelPreviewEffects {
    private static final float BLOCKS_TO_PX_FACTOR = 1.0f;
    private static final float PARTICLE_HALF_BLOCKS = 0.15f;
    private static final float PARTICLE_BASE_Y = 1.53f;
    private static final float EFFECT_Z = 200.0f;
    private static final float ENTITY_Z = 50.0f;
    private static final float PARTICLE_DEPTH_BLOCKS = 2.0f;
    private static final double PARTICLE_LIFE_SECONDS = 1.0;

    private static final double START_EPSILON = 0.001;

    private static String currentEmoteId;
    private static EmoteDefinition currentEmote;
    private static long startNanos;
    private static double lastTime;
    private static double currentUpdateTime;

    private static final List<PreviewParticle> PARTICLES = new ArrayList<>();
    private static final List<PreviewModel> MODELS = new ArrayList<>();

    private EmoteWheelPreviewEffects() {
    }

    public static void clear() {
        currentEmoteId = null;
        currentEmote = null;
        lastTime = 0.0;
        PARTICLES.clear();
        MODELS.clear();
    }

    public static void update(EmoteDefinition emote) {
        if (emote == null) {
            clear();
            return;
        }
        AnimationClip clip = emote.animation();
        if (clip == null) {
            return;
        }
        float length = clip.length();
        if (length <= 0f) {
            return;
        }

        String id = emote.id().toString();
        currentEmote = emote;
        double now;
        if (currentEmoteId == null || !currentEmoteId.equals(id)) {
            currentEmoteId = id;
            startNanos = System.nanoTime();

            lastTime = -START_EPSILON;
            PARTICLES.clear();
            MODELS.clear();
            now = 0.0;
        } else {
            now = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        }
        currentUpdateTime = now;

        double prev = Math.min(lastTime, now);
        List<EmoteEvent> fired;
        try {
            fired = emote.events().crossed(prev, now, clip);
        } catch (Throwable t) {
            fired = List.of();
        }
        lastTime = now;

        for (EmoteEvent event : fired) {
            if (event instanceof SpawnParticleEvent particle) {
                spawnParticle(particle);
            } else if (event instanceof RenderModelEvent model) {
                spawnModel(model, length);
            }
        }
        expire();
    }

    private static void spawnParticle(SpawnParticleEvent event) {
        ResourceLocation asset = event.assetId();
        if (asset == null) {
            return;
        }

        if ("minecraft".equals(asset.getNamespace())) {
            return;
        }
        ResourceLocation texture = Ids.of(asset.getNamespace(), "textures/" + asset.getPath() + ".png");
        float lx = 0f, ly = 0f, lz = 0f;
        if (event.pos() != null) {
            lx = (float) (event.pos().x() / 16.0);
            ly = (float) (event.pos().y() / 16.0);
            lz = (float) (event.pos().z() / 16.0);
        }
        float baseY = PARTICLE_BASE_Y;
        Matrix4f boneFrame = event.bone() == null ? null : EmoteBoneFrames.find(previewFrames(), event.bone());
        if (boneFrame != null) {
            Vector3f local = event.pos() == null ? new Vector3f()
                    : new Vector3f((float) event.pos().x(), (float) event.pos().y(), (float) event.pos().z());
            Vector3f point = boneFrame.transformPosition(local).mul(0.9375f / 16f);
            lx = -point.x;
            ly = point.y;
            lz = point.z;
            baseY = 0f;
        }
        int amount = Math.max(1, event.amount());
        for (int i = 0; i < amount; i++) {
            float jx = (float) ((Math.random() - 0.5) * 0.12);
            float jy = (float) ((Math.random() - 0.5) * 0.12);
            PARTICLES.add(new PreviewParticle(texture, lx + jx, baseY + ly + jy, lz));
        }
    }

    private static Map<String, Matrix4f> previewFrames() {
        EmoteDefinition emote = currentEmote;
        if (emote == null || emote.animation() == null) return Map.of();
        Minecraft client = Minecraft.getInstance();
        boolean slim = client.player != null && ClientCompat.isSlim(client.player);
        return EmoteBoneFrames.compute(emote.animation(), EmoteWheelPreview.timeFor(emote.animation()), emote.rigType(), slim);
    }

    private static Matrix4f boneFrame(Map<String, Matrix4f> frames, RenderModelEvent source) {
        return "bone".equals(source.attachMode()) ? EmoteBoneFrames.find(frames, source.bone()) : null;
    }

    private static void spawnModel(RenderModelEvent event, float length) {
        BundleModelGeometry geo = BundleModelGeometry.load(event.assetId(), event.texture());
        if (geo == null) {
            return;
        }

        double life = event.end() != null ? Math.max(0.05, event.end() - event.time()) : length;

        MODELS.removeIf(m -> event.equals(m.source));
        MODELS.add(new PreviewModel(geo, event, life));
    }

    private static void expire() {
        Iterator<PreviewParticle> pit = PARTICLES.iterator();
        while (pit.hasNext()) {
            PreviewParticle p = pit.next();
            if (currentUpdateTime - p.bornSeconds >= p.lifeSeconds) {
                pit.remove();
            }
        }
        Iterator<PreviewModel> mit = MODELS.iterator();
        while (mit.hasNext()) {
            PreviewModel m = mit.next();
            if (currentUpdateTime - m.bornSeconds >= m.lifeSeconds) {
                mit.remove();
            }
        }
    }

    public static void render(GuiGraphics g, int x0, int y0, int x1, int y1, int feetX, int feetY, int size) {
        if (PARTICLES.isEmpty() && MODELS.isEmpty()) {
            return;
        }
        float pxPerBlock = size * BLOCKS_TO_PX_FACTOR;
        //? if >=1.21.6 {
        /*
        *///?} else {
        MultiBufferSource.BufferSource buffers = ClientCompat.bufferSource();
        PoseStack ps = g.pose();

        if (!MODELS.isEmpty()) {
            Lighting.setupForEntityInInventory();
            Map<String, Matrix4f> frames = previewFrames();
            for (PreviewModel m : MODELS) {
                ps.pushPose();

                ps.translate(feetX, feetY, ENTITY_Z);
                ps.scale(-pxPerBlock, -pxPerBlock, -pxPerBlock);

                if (BundleModelRenderer.applyLocalTransform(ps, m.source,
                        (float) (m.source.time() + (currentUpdateTime - m.bornSeconds)), boneFrame(frames, m.source))) {
                    m.geo.draw(buffers, ps.last());
                }
                ps.popPose();
            }
            GuiCompat.flush(g);
            Lighting.setupFor3DItems();
        }

        for (PreviewParticle p : PARTICLES) {
            ps.pushPose();
            ps.translate(feetX + p.x * pxPerBlock, feetY - p.y * pxPerBlock, EFFECT_Z);
            float half = PARTICLE_HALF_BLOCKS * pxPerBlock;
            PoseStack.Pose pose = ps.last();
            VertexConsumer vc = buffers.getBuffer(ClientCompat.entityTranslucent(p.texture));
            vertex(vc, pose, -half, -half, 0f, 1f);
            vertex(vc, pose, -half, half, 0f, 0f);
            vertex(vc, pose, half, half, 1f, 0f);
            vertex(vc, pose, half, -half, 1f, 1f);
            ps.popPose();
        }
        GuiCompat.flush(g);
        //?}
    }

    static void renderInBlockSpace(PoseStack ps, MultiBufferSource.BufferSource buffers, float feetOffset) {
        Map<String, Matrix4f> frames = MODELS.isEmpty() ? Map.of() : previewFrames();
        for (PreviewModel m : MODELS) {
            ps.pushPose();
            ps.translate(0.0f, feetOffset, 0.0f);
            ps.scale(-1.0f, -1.0f, 1.0f);
            if (BundleModelRenderer.applyLocalTransform(ps, m.source,
                    (float) (m.source.time() + (currentUpdateTime - m.bornSeconds)), boneFrame(frames, m.source))) {
                m.geo.draw(buffers, ps.last());
            }
            ps.popPose();
        }
        buffers.endBatch();

        for (PreviewParticle p : PARTICLES) {
            ps.pushPose();
            ps.translate(p.x, feetOffset - p.y, -PARTICLE_DEPTH_BLOCKS);
            PoseStack.Pose pose = ps.last();
            VertexConsumer vc = buffers.getBuffer(ClientCompat.entityTranslucent(p.texture));
            vertex(vc, pose, -PARTICLE_HALF_BLOCKS, -PARTICLE_HALF_BLOCKS, 0f, 1f);
            vertex(vc, pose, -PARTICLE_HALF_BLOCKS, PARTICLE_HALF_BLOCKS, 0f, 0f);
            vertex(vc, pose, PARTICLE_HALF_BLOCKS, PARTICLE_HALF_BLOCKS, 1f, 0f);
            vertex(vc, pose, PARTICLE_HALF_BLOCKS, -PARTICLE_HALF_BLOCKS, 1f, 1f);
            ps.popPose();
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose,
                               float x, float y, float u, float v) {
        VertexCompat.entity(vc, pose, x, y, 0f, 255, 255, 255, 255, u, v,
                OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0f, 0f, 1f);
    }

    private static final class PreviewParticle {
        final ResourceLocation texture;
        final float x, y, z;
        final double bornSeconds;
        final double lifeSeconds;

        PreviewParticle(ResourceLocation texture, float x, float y, float z) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.z = z;
            this.bornSeconds = currentUpdateTime;
            this.lifeSeconds = PARTICLE_LIFE_SECONDS;
        }
    }

    private static final class PreviewModel {
        final BundleModelGeometry geo;
        final RenderModelEvent source;
        final double bornSeconds;
        final double lifeSeconds;

        PreviewModel(BundleModelGeometry geo, RenderModelEvent source, double lifeSeconds) {
            this.geo = geo;
            this.source = source;
            this.bornSeconds = currentUpdateTime;
            this.lifeSeconds = lifeSeconds;
        }
    }
}
