package com.ptah.event;

import com.ptah.event.type.*;
import com.ptah.playback.PlaybackContext;
import net.minecraft.resources.ResourceLocation;

public final class EventDispatcher {
    public void dispatch(EmoteEvent event, PlaybackContext context) {
        EventSink sink = context.sink();
        if (event instanceof PlayMusicEvent music) {
            if (context.suppressMusic()) return;

            ResourceLocation asset = music.assetId();
            boolean dmca = music.dmca();
            if (dmca && sink.dmcaMode()) {
                if (music.dmcaAlt() == null) return;
                asset = music.dmcaAlt();
                dmca = false;
            }

            if (context.hasPlayingMusic()) context.queueMusic(asset, music.volume(), dmca);
            else context.setMusic(sink.playMusic(context.playerId(), asset, music.volume(),
                    Math.max(0.0, context.currentSeconds() - music.time()), dmca));
        } else if (event instanceof PlaySoundEvent sound) {
            ResourceLocation asset = sound.assetId();
            if (sound.dmca() && sink.dmcaMode()) {
                if (sound.dmcaAlt() == null) return;
                asset = sound.dmcaAlt();
            }
            context.addSound(sink.playSoundTracked(context.playerId(), asset, sound.volume(), sound.pitch()));
        } else if (event instanceof SpawnParticleEvent particle) {
            sink.spawnParticle(context.playerId(), particle.bone(), particle.assetId(), particle.amount(), particle.pos(), particle.rot());
        } else if (event instanceof SetSkinEvent skin) {
            sink.setSkin(context.playerId(), skin.assetId(), skin.model());
        } else if (event instanceof ResetSkinEvent reset) {
            sink.resetSkin(context.playerId());
        } else if (event instanceof RenderModelEvent model) {
            sink.renderModel(context.playerId(), model);
        }
    }
}
