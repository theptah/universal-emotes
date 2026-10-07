package com.ptah.network;

import com.ptah.animation.*;
import com.ptah.bundle.EmoteDefinition;
import com.ptah.bundle.Rarity;
import com.ptah.event.EmoteEvent;
import com.ptah.event.EventTimeline;
import com.ptah.event.type.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EmoteDefinitionCodec {
    private static final int MAX_BONES = 16;
    private static final int MAX_FRAMES = 20_000;
    private static final int MAX_TOTAL_KEYFRAMES = 60_000;
    private static final int MAX_EVENTS = 10_000;
    private static final int MAX_MODEL_KEYFRAMES = 1024;
    private EmoteDefinitionCodec() { }

    public static void write(FriendlyByteBuf out, EmoteDefinition value) {
        out.writeResourceLocation(value.id()); out.writeResourceLocation(value.bundleId());
        out.writeUtf(value.localId()); out.writeUtf(value.animationKey()); out.writeUtf(value.name());
        out.writeUtf(value.description()); out.writeVarInt(value.op()); out.writeEnum(value.rarity());
        out.writeUtf(value.rigType());
        AnimationClip clip = value.animation();
        out.writeUtf(clip.formatVersion()); out.writeUtf(clip.key()); out.writeBoolean(clip.loop()); out.writeFloat(clip.length()); out.writeFloat(clip.loopStart());
        out.writeVarInt(clip.bones().size());
        clip.bones().forEach((name, bone) -> {
            out.writeUtf(name); writeTrack(out, bone.rotation()); writeTrack(out, bone.position()); writeBendTrack(out, bone.bend());
        });
        out.writeVarInt(value.events().events().size());
        for (EmoteEvent event : value.events().events()) writeEvent(out, event);
    }

    public static EmoteDefinition read(FriendlyByteBuf in) {
        ResourceLocation id = in.readResourceLocation(), bundleId = in.readResourceLocation();
        String localId = in.readUtf(256), animationKey = in.readUtf(512), name = in.readUtf(512), description = in.readUtf(4096);
        int op = in.readVarInt(); Rarity rarity = in.readEnum(Rarity.class);
        String rigType = in.readUtf(16);
        String format = in.readUtf(64), key = in.readUtf(512); boolean loop = in.readBoolean(); float length = in.readFloat(); float loopStart = in.readFloat();
        int boneCount = bounded(in.readVarInt(), MAX_BONES, "bones");
        Map<String, BoneAnimation> bones = new LinkedHashMap<>();
        int totalKeyframes = 0;
        for (int i = 0; i < boneCount; i++) {
            String bone = in.readUtf(64);
            BoneAnimation animation = new BoneAnimation(bone, readTrack(in), readTrack(in), readBendTrack(in));
            totalKeyframes += animation.rotation().keyframes().size()
                    + animation.position().keyframes().size()
                    + animation.bend().keyframes().size();
            if (totalKeyframes > MAX_TOTAL_KEYFRAMES) {
                throw new IllegalArgumentException("Too many synced keyframes: " + totalKeyframes);
            }
            bones.put(bone, animation);
        }
        int eventCount = bounded(in.readVarInt(), MAX_EVENTS, "events");
        List<EmoteEvent> events = new ArrayList<>(eventCount);
        for (int i = 0; i < eventCount; i++) events.add(readEvent(in));
        return new EmoteDefinition(id, bundleId, localId, animationKey, name, description, op, rarity,
                new AnimationClip(format, key, loop, length, loopStart, bones), new EventTimeline(events), rigType);
    }

    private static void writeTrack(FriendlyByteBuf out, KeyframeTrack track) {
        out.writeVarInt(track.keyframes().size());
        for (Keyframe frame : track.keyframes()) {
            out.writeFloat(frame.time()); out.writeFloat(frame.value().x()); out.writeFloat(frame.value().y()); out.writeFloat(frame.value().z());
            out.writeByte(frame.interpolation().ordinal());
        }
    }
    private static KeyframeTrack readTrack(FriendlyByteBuf in) {
        int count = bounded(in.readVarInt(), MAX_FRAMES, "keyframes");
        List<Keyframe> frames = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float time = in.readFloat();
            Vec3 value = new Vec3(in.readFloat(), in.readFloat(), in.readFloat());
            frames.add(new Keyframe(time, value, interpolation(in.readByte())));
        }
        return new KeyframeTrack(frames);
    }
    private static void writeBendTrack(FriendlyByteBuf out, BendTrack track) {
        out.writeVarInt(track.keyframes().size());
        for (BendKeyframe frame : track.keyframes()) {
            out.writeFloat(frame.time()); out.writeFloat(frame.value()); out.writeFloat(frame.axis());
            out.writeByte(frame.interpolation().ordinal());
        }
    }
    private static BendTrack readBendTrack(FriendlyByteBuf in) {
        int count = bounded(in.readVarInt(), MAX_FRAMES, "bend keyframes");
        List<BendKeyframe> frames = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            frames.add(new BendKeyframe(in.readFloat(), in.readFloat(), in.readFloat(), interpolation(in.readByte())));
        }
        return new BendTrack(frames);
    }
    private static Interpolation interpolation(byte ordinal) {
        Interpolation[] values = Interpolation.values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Invalid interpolation id: " + ordinal);
        }
        return values[ordinal];
    }
    private static void writeEvent(FriendlyByteBuf out, EmoteEvent event) {
        out.writeUtf(event.type()); out.writeFloat(event.time());
        if (event instanceof PlayMusicEvent e) { out.writeResourceLocation(e.assetId()); out.writeFloat(e.volume()); writeDmca(out, e.dmca(), e.dmcaAlt()); }
        else if (event instanceof PlaySoundEvent e) { out.writeResourceLocation(e.assetId()); out.writeFloat(e.volume()); out.writeFloat(e.pitch()); writeDmca(out, e.dmca(), e.dmcaAlt()); }
        else if (event instanceof SpawnParticleEvent e) {
            writeNullableUtf(out, e.bone()); out.writeResourceLocation(e.assetId()); out.writeVarInt(e.amount());
            writeNullableVec3(out, e.pos()); writeNullableVec3(out, e.rot());
        }
        else if (event instanceof SetSkinEvent e) { out.writeResourceLocation(e.assetId()); out.writeUtf(e.model() == null ? "" : e.model()); }
        else if (event instanceof ResetSkinEvent e) {  }
        else if (event instanceof RenderModelEvent e) {
            out.writeResourceLocation(e.assetId());
            writeNullableUtf(out, e.texture());
            out.writeUtf(e.attachMode());
            writeNullableUtf(out, e.bone());
            writeNullableVec3(out, e.pos());
            writeNullableVec3(out, e.rot());
            out.writeFloat(e.scale());
            out.writeBoolean(e.end() != null); if (e.end() != null) out.writeFloat(e.end());

            out.writeVarInt(e.keyframes().size());
            for (RenderModelEvent.ModelKeyframe k : e.keyframes()) {
                out.writeFloat(k.time()); writeNullableVec3(out, k.pos()); writeNullableVec3(out, k.rot());
                out.writeFloat(k.scale()); out.writeByte(k.interpolation().ordinal());
            }
        }
        else throw new IllegalArgumentException("Unsupported event " + event.type());
    }
    private static EmoteEvent readEvent(FriendlyByteBuf in) {
        String type = in.readUtf(32); float time = in.readFloat();
        return switch (type) {
            case "play_music" -> {
                ResourceLocation asset = in.readResourceLocation(); float volume = in.readFloat();
                boolean dmca = in.readBoolean(); ResourceLocation alt = dmca && in.readBoolean() ? in.readResourceLocation() : null;
                yield new PlayMusicEvent(time, asset, volume, dmca, alt);
            }
            case "play_sound" -> {
                ResourceLocation asset = in.readResourceLocation(); float volume = in.readFloat(); float pitch = in.readFloat();
                boolean dmca = in.readBoolean(); ResourceLocation alt = dmca && in.readBoolean() ? in.readResourceLocation() : null;
                yield new PlaySoundEvent(time, asset, volume, pitch, dmca, alt);
            }
            case "spawn_particle" -> new SpawnParticleEvent(time, readNullableUtf(in, 64), in.readResourceLocation(), in.readVarInt(),
                    readNullableVec3(in), readNullableVec3(in));
            case "set_skin" -> new SetSkinEvent(time, in.readResourceLocation(), emptyToNull(in.readUtf(16)));
            case "reset_skin" -> new ResetSkinEvent(time);
            case "render_model" -> new RenderModelEvent(time, in.readResourceLocation(), readNullableUtf(in, 256), in.readUtf(32),
                    readNullableUtf(in, 64), readNullableVec3(in), readNullableVec3(in), in.readFloat(),
                    in.readBoolean() ? in.readFloat() : null, readModelKeyframes(in));
            default -> throw new IllegalArgumentException("Unknown synced event " + type);
        };
    }
    private static List<RenderModelEvent.ModelKeyframe> readModelKeyframes(FriendlyByteBuf in) {
        int count = bounded(in.readVarInt(), MAX_MODEL_KEYFRAMES, "model keyframes");
        List<RenderModelEvent.ModelKeyframe> frames = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float time = in.readFloat();
            Vec3 pos = readNullableVec3(in), rot = readNullableVec3(in);
            float scale = in.readFloat();
            frames.add(new RenderModelEvent.ModelKeyframe(time, pos, rot, scale, interpolation(in.readByte())));
        }
        return frames;
    }
    private static void writeNullableUtf(FriendlyByteBuf out, String value) {
        out.writeBoolean(value != null); if (value != null) out.writeUtf(value);
    }
    private static String readNullableUtf(FriendlyByteBuf in, int max) {
        return in.readBoolean() ? in.readUtf(max) : null;
    }
    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
    private static void writeNullableVec3(FriendlyByteBuf out, Vec3 v) {
        out.writeBoolean(v != null); if (v != null) { out.writeFloat(v.x()); out.writeFloat(v.y()); out.writeFloat(v.z()); }
    }
    private static Vec3 readNullableVec3(FriendlyByteBuf in) {
        return in.readBoolean() ? new Vec3(in.readFloat(), in.readFloat(), in.readFloat()) : null;
    }
    private static int bounded(int value, int max, String name) {
        if (value < 0 || value > max) throw new IllegalArgumentException("Invalid synced " + name + " count: " + value);
        return value;
    }

    private static void writeDmca(FriendlyByteBuf out, boolean dmca, ResourceLocation alt) {
        out.writeBoolean(dmca);
        if (dmca) {
            out.writeBoolean(alt != null);
            if (alt != null) out.writeResourceLocation(alt);
        }
    }
}
