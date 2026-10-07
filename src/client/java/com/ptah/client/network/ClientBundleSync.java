package com.ptah.client.network;

import com.ptah.UniversalEmotesMod;
import com.ptah.config.UniversalEmotesPaths;
import com.ptah.bundle.EmoteDefinition;
import com.ptah.client.resource.BundleResourcePack;
import com.ptah.network.EmoteDefinitionCodec;
import com.ptah.network.ServerBundleSync;
import com.ptah.system.EmoteSystem;
import io.netty.buffer.Unpooled;
import com.ptah.client.compat.ClientNet;
import com.ptah.compat.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClientBundleSync {
    public record CachedAsset(String kind, ResourceLocation id, Path file, String hash) { }
    private static final long MAX_ASSET = 100L * 1024L * 1024L;
    private static final long MAX_SESSION = 250L * 1024L * 1024L;
    private static final int MAX_DEF_BYTES = 8 * 1024 * 1024;
    private static final int MAX_MANIFEST = 200_000;
    private static final Map<UUID, Incoming> INCOMING = new HashMap<>();
    private static final List<CachedAsset> ASSETS = new ArrayList<>();
    private static final List<Runnable> PENDING_PLAYS = new ArrayList<>();
    private static boolean syncing;
    private static long sessionBytes;

    private ClientBundleSync() { }

    public static void initialize() {
        ClientNet.receive(ServerBundleSync.MANIFEST, (client, buffer) -> {
            int protocol = buffer.readVarInt();
            List<ManifestDef> defs = readDefs(buffer);
            List<ManifestAsset> assets = readAssets(buffer);
            client.execute(() -> handleManifest(protocol, defs, assets));
        });
        ClientNet.receive(ServerBundleSync.DEFINITION, (client, buffer) -> {
            String hash = buffer.readUtf(64);
            byte[] payload = buffer.readByteArray(MAX_DEF_BYTES);
            client.execute(() -> acceptDefinition(hash, payload));
        });
        ClientNet.receive(ServerBundleSync.ASSET_START, (client, buffer) -> {
            UUID transfer = buffer.readUUID(); String kind = buffer.readUtf(16); ResourceLocation id = buffer.readResourceLocation();
            long size = buffer.readVarLong(); String hash = buffer.readUtf(64);
            client.execute(() -> start(transfer, kind, id, size, hash));
        });
        ClientNet.receive(ServerBundleSync.ASSET_CHUNK, (client, buffer) -> {
            UUID transfer = buffer.readUUID(); int sequence = buffer.readVarInt(); byte[] bytes = buffer.readByteArray(ServerBundleSync.CHUNK_BYTES);
            client.execute(() -> chunk(transfer, sequence, bytes));
        });
        ClientNet.receive(ServerBundleSync.ASSET_END, (client, buffer) -> {
            UUID transfer = buffer.readUUID(); int chunks = buffer.readVarInt();
            client.execute(() -> end(transfer, chunks));
        });
        ClientNet.receive(ServerBundleSync.COMPLETE, (client, buffer) ->
                client.execute(ClientBundleSync::complete));
    }

    public static void runWhenReady(Runnable play) {
        if (syncing) PENDING_PLAYS.add(play); else play.run();
    }

    public static void clear() {
        syncing = false; sessionBytes = 0; INCOMING.clear(); ASSETS.clear(); PENDING_PLAYS.clear();
        BundleResourcePack.setServerAssets(List.of());
        EmoteSystem.reload();
    }

    private static void handleManifest(int protocol, List<ManifestDef> defs, List<ManifestAsset> assets) {
        if (protocol != ServerBundleSync.PROTOCOL) {
            UniversalEmotesMod.LOGGER.error("[Emote Sync] Protocol mismatch server={} client={}", protocol, ServerBundleSync.PROTOCOL);
            return;
        }
        syncing = true; sessionBytes = 0; INCOMING.clear(); ASSETS.clear(); PENDING_PLAYS.clear();
        BundleResourcePack.setServerAssets(List.of());
        EmoteSystem.reload();

        List<String> missingDefs = new ArrayList<>();
        for (ManifestDef def : defs) {
            EmoteDefinition definition = loadCachedDef(def.hash());
            if (definition != null) EmoteSystem.EMOTES.put(definition);
            else missingDefs.add(def.hash());
        }

        List<String> missingAssets = new ArrayList<>();
        for (ManifestAsset asset : assets) {
            Path file = objectPath(asset.hash(), asset.kind());
            if (file != null && Files.isRegularFile(file)) ASSETS.add(new CachedAsset(asset.kind(), asset.id(), file, asset.hash()));
            else missingAssets.add(asset.hash());
        }

        FriendlyByteBuf request = Net.buf();
        writeHashList(request, missingDefs);
        writeHashList(request, missingAssets);
        ClientNet.send(ServerBundleSync.REQUEST, request);

        UniversalEmotesMod.LOGGER.info("[Emote Sync] Manifest: {} defs ({} cached), {} assets ({} cached) → requesting {} defs + {} assets",
                defs.size(), defs.size() - missingDefs.size(), assets.size(), assets.size() - missingAssets.size(),
                missingDefs.size(), missingAssets.size());
    }

    private static void acceptDefinition(String hash, byte[] payload) {
        try {
            if (payload.length > MAX_DEF_BYTES) throw new IllegalStateException("Definition too large");
            String actual = sha256(payload);
            if (!actual.equals(hash)) throw new IllegalStateException("Definition hash mismatch");
            EmoteDefinition definition = EmoteDefinitionCodec.read(new FriendlyByteBuf(Unpooled.wrappedBuffer(payload)));
            EmoteSystem.EMOTES.put(definition);
            writeCachedDef(hash, payload);
        } catch (Exception exception) {
            UniversalEmotesMod.LOGGER.error("[Emote Sync] Invalid definition", exception);
        }
    }

    private static void start(UUID transfer, String kind, ResourceLocation id, long size, String hash) {
        if (!isKnownKind(kind) || size < 0 || size > MAX_ASSET || hash.length() != 64) return;
        if (sessionBytes + size > MAX_SESSION) return;
        sessionBytes += size;
        INCOMING.put(transfer, new Incoming(kind, id, size, hash));
    }

    private static void chunk(UUID transfer, int sequence, byte[] bytes) {
        Incoming incoming = INCOMING.get(transfer);
        if (incoming == null || sequence != incoming.nextSequence || incoming.data.size() + bytes.length > incoming.size) {
            INCOMING.remove(transfer); return;
        }
        incoming.data.writeBytes(bytes); incoming.nextSequence++;
    }

    private static void end(UUID transfer, int chunks) {
        Incoming incoming = INCOMING.remove(transfer);
        if (incoming == null || incoming.nextSequence != chunks || incoming.data.size() != incoming.size) return;
        try {
            byte[] bytes = incoming.data.toByteArray();
            String actual = sha256(bytes);
            if (!actual.equals(incoming.hash)) throw new IllegalStateException("Asset hash mismatch");
            Path file = objectPath(actual, incoming.kind);
            if (file == null) throw new IllegalStateException("Unknown asset kind: " + incoming.kind);
            Files.createDirectories(file.getParent());
            if (!Files.isRegularFile(file)) Files.write(file, bytes);
            ASSETS.add(new CachedAsset(incoming.kind, incoming.id, file, actual));
        } catch (Exception exception) { UniversalEmotesMod.LOGGER.error("[Emote Sync] Asset cache failed: {}", incoming.id, exception); }
    }

    private static void complete() {
        BundleResourcePack.setServerAssets(List.copyOf(ASSETS));
        BundleResourcePack.reload().whenComplete((count, error) -> Minecraft.getInstance().execute(() -> {
            syncing = false;
            if (error != null) UniversalEmotesMod.LOGGER.error("[Emote Sync] Resource reload failed", error);
            else UniversalEmotesMod.LOGGER.info("[Emote Sync] Ready: {} definitions, {} assets", EmoteSystem.EMOTES.values().size(), ASSETS.size());
            List<Runnable> pending = List.copyOf(PENDING_PLAYS); PENDING_PLAYS.clear(); pending.forEach(Runnable::run);
        }));
    }

    private static boolean isKnownKind(String kind) {
        return kind.equals("audio") || kind.equals("texture") || kind.equals("model");
    }

    private static Path cacheRoot() {
        return UniversalEmotesPaths.cache();
    }

    private static Path objectPath(String hash, String kind) {
        if (hash == null || hash.length() != 64) return null;
        String extension = switch (kind) {
            case "audio" -> ".ogg";
            case "texture" -> ".png";
            case "model" -> ".json";
            default -> null;
        };
        if (extension == null) return null;
        return cacheRoot().resolve("objects").resolve(hash + extension);
    }

    private static EmoteDefinition loadCachedDef(String hash) {
        if (hash == null || hash.length() != 64) return null;
        Path file = cacheRoot().resolve("defs").resolve(hash + ".bin");
        try {
            if (!Files.isRegularFile(file)) return null;
            byte[] payload = Files.readAllBytes(file);
            if (!sha256(payload).equals(hash)) return null;
            return EmoteDefinitionCodec.read(new FriendlyByteBuf(Unpooled.wrappedBuffer(payload)));
        } catch (Exception exception) {
            return null;
        }
    }

    private static void writeCachedDef(String hash, byte[] payload) {
        try {
            Path dir = cacheRoot().resolve("defs");
            Files.createDirectories(dir);
            Path file = dir.resolve(hash + ".bin");
            if (!Files.isRegularFile(file)) Files.write(file, payload);
        } catch (Exception exception) { UniversalEmotesMod.LOGGER.error("[Emote Sync] Def cache write failed: {}", hash, exception); }
    }

    private static List<ManifestDef> readDefs(FriendlyByteBuf in) {
        int count = in.readVarInt();
        if (count < 0 || count > MAX_MANIFEST) throw new IllegalArgumentException("Invalid manifest def count: " + count);
        List<ManifestDef> defs = new ArrayList<>(Math.min(count, 1024));
        for (int i = 0; i < count; i++) defs.add(new ManifestDef(in.readResourceLocation(), in.readUtf(64)));
        return defs;
    }

    private static List<ManifestAsset> readAssets(FriendlyByteBuf in) {
        int count = in.readVarInt();
        if (count < 0 || count > MAX_MANIFEST) throw new IllegalArgumentException("Invalid manifest asset count: " + count);
        List<ManifestAsset> assets = new ArrayList<>(Math.min(count, 1024));
        for (int i = 0; i < count; i++) {
            String kind = in.readUtf(16); ResourceLocation id = in.readResourceLocation();
            String hash = in.readUtf(64); long size = in.readVarLong();
            assets.add(new ManifestAsset(kind, id, hash, size));
        }
        return assets;
    }

    private static void writeHashList(FriendlyByteBuf out, List<String> hashes) {
        out.writeVarInt(hashes.size());
        for (String hash : hashes) out.writeUtf(hash);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private record ManifestDef(ResourceLocation id, String hash) { }
    private record ManifestAsset(String kind, ResourceLocation id, String hash, long size) { }

    private static final class Incoming {
        final String kind; final ResourceLocation id; final long size; final String hash;
        final ByteArrayOutputStream data = new ByteArrayOutputStream(); int nextSequence;
        Incoming(String kind, ResourceLocation id, long size, String hash) { this.kind = kind; this.id = id; this.size = size; this.hash = hash; }
    }
}
