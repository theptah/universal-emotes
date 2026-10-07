package com.ptah.client.render;

import com.ptah.client.config.SyncedServerConfig;
import com.ptah.client.network.EmoteClientNetwork;
import com.ptah.client.playback.ClientPlaybackRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public final class DanceTogetherPrompt {
    private static final double START_DISTANCE = 8.0;

    private static final double HITBOX_INFLATE = 0.35;

    private DanceTogetherPrompt() { }

    public static UUID computeTarget(Minecraft client, float partialTick) {
        if (client == null) return null;
        LocalPlayer self = client.player;
        if (self == null || client.level == null) return null;

        if (client.screen != null) return null;
        if (self.isSpectator() || self.isPassenger()) return null;

        Vec3 eye = self.getEyePosition(partialTick);
        Vec3 view = self.getViewVector(partialTick);
        Vec3 end = eye.add(view.x * START_DISTANCE, view.y * START_DISTANCE, view.z * START_DISTANCE);

        UUID best = null;
        double bestDistanceSqr = Double.MAX_VALUE;
        for (AbstractClientPlayer other : client.level.players()) {
            if (other == self || other.isSpectator()) continue;
            UUID id = other.getUUID();
            Optional<com.ptah.playback.ActiveEmote> active = ClientPlaybackRuntime.MANAGER.active(id);
            if (active.isEmpty()) continue;

            if (!SyncedServerConfig.get().canUse(self, active.get().emote())) continue;

            AABB box = other.getBoundingBox().inflate(HITBOX_INFLATE);
            boolean inside = box.contains(eye);
            Optional<Vec3> hit = box.clip(eye, end);
            if (!inside && hit.isEmpty()) continue;
            double distanceSqr = inside ? 0.0 : eye.distanceToSqr(hit.get());
            if (distanceSqr < bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                best = id;
            }
        }
        return best;
    }

    public static boolean tryStart(Minecraft client) {
        UUID target = computeTarget(client, 1.0f);
        if (target == null) return false;
        return EmoteClientNetwork.requestDanceTogether(target);
    }

    public static void render(GuiGraphics graphics, KeyMapping openKey, float partialTick) {
        if (computeTarget(Minecraft.getInstance(), partialTick) == null) return;
        HudHeader.draw(graphics, "[" + openKey.getTranslatedKeyMessage().getString() + "] Press to dance together");
    }
}
