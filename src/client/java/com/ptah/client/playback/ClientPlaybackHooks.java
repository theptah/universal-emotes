package com.ptah.client.playback;

import com.ptah.client.compat.ClientCompat;
import com.ptah.compat.Ids;
import com.ptah.animation.Vec3;
import com.ptah.bundle.EmoteDefinition;
import com.ptah.client.camera.EmoteCameraController;
import com.ptah.client.config.ClientConfigManager;
import com.ptah.client.render.EmoteSkinOverrides;
import com.ptah.client.render.CustomEmoteParticles;
import com.ptah.client.render.BundleModelGeometry;
import com.ptah.client.render.BundleModelRenderer;
import com.ptah.client.render.EmoteBoneFrames;

import com.ptah.event.EmoteEvent;
import com.ptah.event.type.PlayMusicEvent;
import com.ptah.event.type.RenderModelEvent;
import com.ptah.playback.MusicHandle;
import com.ptah.playback.PlaybackHooks;
import com.ptah.playback.StopReason;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.UUID;

public final class ClientPlaybackHooks implements PlaybackHooks {
    @Override public void onStart(UUID playerId, EmoteDefinition emote, long startTick, long seed) {
        Minecraft client = Minecraft.getInstance();

        for (EmoteEvent event : emote.events().events()) {
            if (event instanceof PlayMusicEvent music) {
                boolean alt = music.dmca() && music.dmcaAlt() != null && ClientConfigManager.get().dmcaMode;
                EmoteMusicHandle.prewarm(client, alt ? music.dmcaAlt() : music.assetId());
            }
        }
        if (client.player != null && client.player.getUUID().equals(playerId)) {
            EmoteCameraController.onEmoteStart(ClientConfigManager.get().turnToTpsAfterEveryEmote);
        }
    }
    @Override public void onStop(UUID playerId, EmoteDefinition emote, StopReason reason) {
        EmoteSkinOverrides.clear(playerId);

        BundleModelRenderer.clearFor(playerId);

        if (reason == StopReason.REPLACED) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.getUUID().equals(playerId)) {
            EmoteCameraController.onEmoteStop();
        }
    }

    @Override
    public MusicHandle playMusic(UUID playerId, ResourceLocation assetId, float volume, double offsetSeconds) {
        return playMusic(playerId, assetId, volume, offsetSeconds, false);
    }

    @Override
    public MusicHandle playMusic(UUID playerId, ResourceLocation assetId, float volume, double offsetSeconds, boolean dmca) {
        return new EmoteMusicHandle(Minecraft.getInstance(), playerId, assetId, volume, offsetSeconds, dmca);
    }

    @Override public boolean dmcaMode() { return ClientConfigManager.get().dmcaMode; }

    @Override public void playSound(UUID playerId, ResourceLocation assetId, float volume, float pitch) {
        if (ClientConfigManager.get().muteEmoteSounds) return;
        Minecraft client = Minecraft.getInstance();
        client.getSoundManager().play(soundInstance(client, playerId, assetId, SoundSource.PLAYERS, volume, pitch));
    }

    @Override public MusicHandle playSoundTracked(UUID playerId, ResourceLocation assetId, float volume, float pitch) {
        if (ClientConfigManager.get().muteEmoteSounds) return null;
        Minecraft client = Minecraft.getInstance();
        SimpleSoundInstance instance = soundInstance(client, playerId, assetId, SoundSource.PLAYERS, volume, pitch);
        client.getSoundManager().play(instance);
        return new EmoteSoundHandle(client, instance);
    }

    @Override public void spawnParticle(UUID playerId, String bone, ResourceLocation assetId, int amount, Vec3 pos, Vec3 rot) {
        if (ClientConfigManager.get().hideParticleEffects) return;
        Minecraft client = Minecraft.getInstance(); ClientLevel level = client.level;
        if (level == null) return;
        Player player = level.getPlayerByUUID(playerId);
        if (player == null) return;

        ParticleType<?> vanillaType = ClientCompat.particleType(assetId);
        SimpleParticleType vanilla = vanillaType instanceof SimpleParticleType simple ? simple : null;
        if (vanilla == null && assetId.getNamespace().equals("minecraft")) return;
        ResourceLocation customTexture = vanilla == null
                ? Ids.of(assetId.getNamespace(), "textures/" + assetId.getPath() + ".png")
                : null;
        RandomSource random = client.player == null ? RandomSource.create() : client.player.getRandom();

        Matrix4f boneFrame = bone == null ? null : EmoteBoneFrames.find(EmoteBoneFrames.forPlayer(player, 0f), bone);
        if (boneFrame != null) {
            spawnAtBone(level, player, boneFrame, vanilla, customTexture, random, amount, pos, rot);
            return;
        }

        double baseX = player.getX();
        double baseY = player.getY() + player.getBbHeight() * 0.85;
        double baseZ = player.getZ();

        double yaw = Math.toRadians(player.getYRot());
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        if (pos != null) {
            double lx = pos.x() / 16.0, ly = pos.y() / 16.0, lz = pos.z() / 16.0;
            double forwardX = -sin, forwardZ = cos;
            double rightX = cos, rightZ = sin;
            baseX += rightX * lx + forwardX * lz;
            baseY += ly;
            baseZ += rightZ * lx + forwardZ * lz;
        }

        double vx = 0.0, vy = 0.03, vz = 0.0;
        if (rot != null) {
            double pitchR = Math.toRadians(rot.x());
            double yawR = Math.toRadians(rot.y()) + yaw;
            double speed = 0.15;
            vx = -Math.sin(yawR) * Math.cos(pitchR) * speed;
            vy = -Math.sin(pitchR) * speed;
            vz = Math.cos(yawR) * Math.cos(pitchR) * speed;
        }

        int count = Math.max(1, amount);
        double jitter = (pos != null) ? 0.06 : 0.35;
        for (int i = 0; i < count; i++) {
            double sx = baseX + (random.nextDouble() - 0.5) * jitter;
            double sy = baseY + (random.nextDouble() - 0.5) * (pos != null ? jitter : 0.3);
            double sz = baseZ + (random.nextDouble() - 0.5) * jitter;
            if (vanilla != null) {
                level.addParticle(vanilla, sx, sy, sz, vx, vy, vz);
            } else {
                CustomEmoteParticles.spawn(customTexture, sx, sy, sz, vx, vy, vz);
            }
        }
    }

    private static void spawnAtBone(ClientLevel level, Player player, Matrix4f boneFrame, SimpleParticleType vanilla,
                                    ResourceLocation customTexture, RandomSource random, int amount, Vec3 pos, Vec3 rot) {
        Vector3f local = pos == null ? new Vector3f() : new Vector3f((float) pos.x(), (float) pos.y(), (float) pos.z());
        Vector3f point = boneFrame.transformPosition(local).mul(0.9375f / 16f);
        Vector3f direction = new Vector3f(0f, 0f, -1f);
        if (rot != null) {
            direction.rotateX((float) Math.toRadians(-rot.x()));
            direction.rotateY((float) Math.toRadians(-rot.y()));
            direction.rotateZ((float) Math.toRadians(rot.z()));
        }
        boneFrame.transformDirection(direction);
        if (direction.lengthSquared() > 1.0e-8f) direction.normalize();
        float yaw = (float) Math.toRadians(180.0 - player.yBodyRot);
        point.rotateY(yaw);
        direction.rotateY(yaw);
        double speed = rot == null ? 0.0 : 0.15;
        double vx = rot == null ? 0.0 : direction.x * speed;
        double vy = rot == null ? 0.03 : direction.y * speed;
        double vz = rot == null ? 0.0 : direction.z * speed;
        double jitter = 0.06;
        int count = Math.max(1, amount);
        for (int i = 0; i < count; i++) {
            double sx = player.getX() + point.x + (random.nextDouble() - 0.5) * jitter;
            double sy = player.getY() + point.y + (random.nextDouble() - 0.5) * jitter;
            double sz = player.getZ() + point.z + (random.nextDouble() - 0.5) * jitter;
            if (vanilla != null) {
                level.addParticle(vanilla, sx, sy, sz, vx, vy, vz);
            } else {
                CustomEmoteParticles.spawn(customTexture, sx, sy, sz, vx, vy, vz);
            }
        }
    }

    @Override public void setSkin(UUID playerId, ResourceLocation assetId, String model) {
        ResourceLocation texture = Ids.of(assetId.getNamespace(), "textures/" + assetId.getPath() + ".png");
        EmoteSkinOverrides.set(playerId, texture, model);
    }
    @Override public void resetSkin(UUID playerId) {
        EmoteSkinOverrides.clear(playerId);
    }
    @Override public void renderModel(UUID playerId, RenderModelEvent model) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) return;
        Player player = level.getPlayerByUUID(playerId);
        if (player == null) return;
        BundleModelGeometry geo = BundleModelGeometry.load(model.assetId(), model.texture());
        if (geo == null) return;

        int lifetime = -1;
        if (model.end() != null) lifetime = Math.max(1, Math.round((model.end() - model.time()) * 20.0f));
        BundleModelRenderer.spawn(playerId, geo, model, lifetime,
                player.getX(), player.getY(), player.getZ(), player.yBodyRot);
    }

    static SimpleSoundInstance soundInstance(Minecraft client, UUID playerId, ResourceLocation assetId,
                                                      SoundSource source, float volume, float pitch) {
        Player player = client.level == null ? null : client.level.getPlayerByUUID(playerId);
        boolean relative = client.player != null && client.player.getUUID().equals(playerId);
        double x = relative || player == null ? 0.0 : player.getX();
        double y = relative || player == null ? 0.0 : player.getY();
        double z = relative || player == null ? 0.0 : player.getZ();
        return new SimpleSoundInstance(assetId, source, volume, pitch, RandomSource.create(), false, 0,
                relative ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR, x, y, z, relative);
    }
}
