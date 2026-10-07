package com.ptah.network;

import com.ptah.compat.Ids;
import com.ptah.UniversalEmotesMod;
import com.ptah.bundle.BundleDescriptor;
import com.ptah.bundle.EmoteDefinition;
import com.ptah.system.EmoteSystem;
import com.ptah.compat.Net;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ServerBundleSync {
    public static final int PROTOCOL = 1;
    public static final int CHUNK_BYTES = 256 * 1024;

    public static final ResourceLocation MANIFEST = Net.s2c("sync_manifest");
    public static final ResourceLocation REQUEST = Net.c2s("sync_request");
    public static final ResourceLocation DEFINITION = Net.s2c("sync_definition");
    public static final ResourceLocation ASSET_START = Net.s2c("sync_asset_start");
    public static final ResourceLocation ASSET_CHUNK = Net.s2c("sync_asset_chunk");
    public static final ResourceLocation ASSET_END = Net.s2c("sync_asset_end");
    public static final ResourceLocation COMPLETE = Net.s2c("sync_complete");

    private static final long MAX_ASSET = 100L * 1024L * 1024L;
    private static final long MAX_SESSION = 250L * 1024L * 1024L;
    private static final int MAX_LIST = 200_000;

    private static final Map<UUID, SyncIndex> SESSIONS = new ConcurrentHashMap<>();

    private static volatile SyncIndex cachedIndex;

    private ServerBundleSync() { }

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> sendManifest(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                SESSIONS.remove(handler.player.getUUID()));
        Net.receive(REQUEST, (server, player, buffer) -> {
            List<String> defHashes = readHashList(buffer);
            List<String> assetHashes = readHashList(buffer);
            server.execute(() -> serveRequest(player, defHashes, assetHashes));
        });
    }

    public static void resendManifestToAll(MinecraftServer server) {
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            sendManifest(target);
        }
    }

    private static void sendManifest(ServerPlayer target) {
        if (!Net.canSend(target, MANIFEST)) {
            UniversalEmotesMod.LOGGER.info("[Emote Sync] Client {} has no bundle sync receiver", target.getScoreboardName());
            return;
        }
        SyncIndex index = syncIndex();
        SESSIONS.put(target.getUUID(), index);

        FriendlyByteBuf out = Net.buf();
        out.writeVarInt(PROTOCOL);
        out.writeVarInt(index.defs.size());
        for (DefEntry def : index.defs) { out.writeResourceLocation(def.id()); out.writeUtf(def.hash()); }
        out.writeVarInt(index.assets.size());
        for (AssetRef ref : index.assets) {
            out.writeUtf(ref.kind()); out.writeResourceLocation(ref.assetId()); out.writeUtf(ref.hash()); out.writeVarLong(ref.size());
        }
        Net.send(target, MANIFEST, out);
        UniversalEmotesMod.LOGGER.info("[Emote Sync] Manifest -> {}: {} defs, {} assets",
                target.getScoreboardName(), index.defs.size(), index.assets.size());
    }

    private static void serveRequest(ServerPlayer target, List<String> defHashes, List<String> assetHashes) {
        SyncIndex index = SESSIONS.remove(target.getUUID());
        if (index == null) {
            UniversalEmotesMod.LOGGER.warn("[Emote Sync] Delta request from {} without a pending manifest",
                    target.getScoreboardName());
            return;
        }
        long sentBytes = 0;
        int sentDefs = 0, sentAssets = 0;
        try {
            for (String hash : defHashes) {
                byte[] bytes = index.defByHash.get(hash);
                if (bytes == null) continue;
                FriendlyByteBuf data = Net.buf();
                data.writeUtf(hash); data.writeByteArray(bytes);
                Net.send(target, DEFINITION, data);
                sentBytes += bytes.length; sentDefs++;
            }

            Map<Path, List<AssetRef>> byZip = new LinkedHashMap<>();
            for (String hash : assetHashes) {
                AssetRef ref = index.assetByHash.get(hash);
                if (ref != null) byZip.computeIfAbsent(ref.zip(), k -> new ArrayList<>()).add(ref);
            }
            for (Map.Entry<Path, List<AssetRef>> group : byZip.entrySet()) {
                try (ZipFile zip = new ZipFile(group.getKey().toFile())) {
                    for (AssetRef ref : group.getValue()) {
                        ZipEntry entry = zip.getEntry(ref.entryName());
                        if (entry == null || entry.isDirectory()) continue;
                        byte[] bytes;
                        try (InputStream input = zip.getInputStream(entry)) { bytes = readLimited(input, MAX_ASSET); }
                        sentBytes += bytes.length;
                        if (sentBytes > MAX_SESSION) throw new IllegalStateException("Sync delta exceeds session limit");
                        sendAsset(target, ref.kind(), ref.assetId(), bytes);
                        sentAssets++;
                    }
                }
            }

            Net.send(target, COMPLETE, Net.buf());
            EmoteNetwork.syncActiveTo(target);
            UniversalEmotesMod.LOGGER.info("[Emote Sync] Delta -> {}: {} defs, {} assets, {} bytes",
                    target.getScoreboardName(), sentDefs, sentAssets, sentBytes);
        } catch (Exception exception) {
            UniversalEmotesMod.LOGGER.error("[Emote Sync] Delta failed for {}", target.getScoreboardName(), exception);
        }
    }

    private static SyncIndex syncIndex() {
        String signature = bundleSignature();
        SyncIndex current = cachedIndex;
        if (current != null && current.signature.equals(signature)) return current;
        SyncIndex rebuilt = buildIndex(signature);
        cachedIndex = rebuilt;
        return rebuilt;
    }

    private static String bundleSignature() {
        List<BundleDescriptor> bundles = new ArrayList<>(EmoteSystem.BUNDLES.values());
        bundles.sort(Comparator.comparing((BundleDescriptor b) -> b.info().id().toString()).thenComparingInt(b -> b.info().order()));
        StringBuilder sb = new StringBuilder();
        for (BundleDescriptor b : bundles) {
            sb.append(b.info().id()).append('/').append(b.info().order()).append('@').append(b.sourceZip()).append('#').append(b.entries().size()).append(';');
        }

        sb.append("emotes=").append(EmoteSystem.EMOTES.values().size());
        return sb.toString();
    }

    private static SyncIndex buildIndex(String signature) {
        List<DefEntry> defs = new ArrayList<>();
        Map<String, byte[]> defByHash = new LinkedHashMap<>();
        for (EmoteDefinition emote : EmoteSystem.EMOTES.values()) {
            byte[] bytes = serialize(emote);
            String hash = sha256(bytes);
            defs.add(new DefEntry(emote.id(), hash));
            defByHash.putIfAbsent(hash, bytes);
        }

        List<AssetRef> assets = new ArrayList<>();
        Map<String, AssetRef> assetByHash = new LinkedHashMap<>();
        Set<ResourceLocation> emittedAudio = new HashSet<>(), emittedTextures = new HashSet<>(), emittedModels = new HashSet<>();
        try {
            for (BundleDescriptor bundle : EmoteSystem.BUNDLES.values()) {
                try (ZipFile zip = new ZipFile(bundle.sourceZip().toFile())) {
                    for (String entryName : bundle.entries()) {
                        String kind, path;
                        Set<ResourceLocation> emitted;
                        if (entryName.startsWith("assets/audio/") && entryName.endsWith(".ogg")) {
                            kind = "audio"; path = entryName.substring(13, entryName.length() - 4); emitted = emittedAudio;
                        } else if (entryName.startsWith("assets/textures/") && entryName.endsWith(".png")) {
                            kind = "texture"; path = entryName.substring(16, entryName.length() - 4); emitted = emittedTextures;
                        } else if (entryName.startsWith("assets/models/") && entryName.endsWith(".json")) {
                            kind = "model"; path = entryName.substring(14, entryName.length() - 5); emitted = emittedModels;
                        } else continue;
                        ResourceLocation assetId = Ids.of(bundle.info().namespace(), path);
                        if (!emitted.add(assetId)) continue;
                        ZipEntry entry = zip.getEntry(entryName);
                        if (entry == null || entry.isDirectory()) continue;
                        byte[] bytes;
                        try (InputStream input = zip.getInputStream(entry)) { bytes = readLimited(input, MAX_ASSET); }
                        String hash = sha256(bytes);
                        AssetRef ref = new AssetRef(kind, assetId, hash, bytes.length, bundle.sourceZip(), entryName);
                        assets.add(ref);
                        assetByHash.putIfAbsent(hash, ref);
                    }
                }
            }
        } catch (Exception exception) {
            UniversalEmotesMod.LOGGER.error("[Emote Sync] Failed to index assets", exception);
        }
        UniversalEmotesMod.LOGGER.info("[Emote Sync] Indexed {} defs, {} assets", defs.size(), assets.size());
        return new SyncIndex(signature, List.copyOf(defs), List.copyOf(assets), Map.copyOf(defByHash), Map.copyOf(assetByHash));
    }

    private static void sendAsset(ServerPlayer target, String kind, ResourceLocation id, byte[] bytes) {
        UUID transfer = UUID.randomUUID();
        String hash = sha256(bytes);
        FriendlyByteBuf start = Net.buf();
        start.writeUUID(transfer); start.writeUtf(kind); start.writeResourceLocation(id); start.writeVarLong(bytes.length); start.writeUtf(hash);
        Net.send(target, ASSET_START, start);
        int sequence = 0;
        for (int offset = 0; offset < bytes.length; offset += CHUNK_BYTES) {
            byte[] part = Arrays.copyOfRange(bytes, offset, Math.min(bytes.length, offset + CHUNK_BYTES));
            FriendlyByteBuf chunk = Net.buf();
            chunk.writeUUID(transfer); chunk.writeVarInt(sequence++); chunk.writeByteArray(part);
            Net.send(target, ASSET_CHUNK, chunk);
        }
        FriendlyByteBuf end = Net.buf(); end.writeUUID(transfer); end.writeVarInt(sequence);
        Net.send(target, ASSET_END, end);
    }

    private static byte[] serialize(EmoteDefinition emote) {
        FriendlyByteBuf buffer = Net.buf();
        EmoteDefinitionCodec.write(buffer, emote);
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static List<String> readHashList(FriendlyByteBuf in) {
        int count = in.readVarInt();
        if (count < 0 || count > MAX_LIST) throw new IllegalArgumentException("Invalid sync request count: " + count);
        List<String> list = new ArrayList<>(Math.min(count, 1024));
        for (int i = 0; i < count; i++) list.add(in.readUtf(64));
        return list;
    }

    private static byte[] readLimited(InputStream input, long limit) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int read; long total = 0;
        while ((read = input.read(buffer)) >= 0) { total += read; if (total > limit) throw new IllegalStateException("Asset exceeds sync limit"); output.write(buffer, 0, read); }
        return output.toByteArray();
    }

    private static ResourceLocation id(String path) { return Ids.mod(path); }

    private record DefEntry(ResourceLocation id, String hash) { }
    private record AssetRef(String kind, ResourceLocation assetId, String hash, long size, Path zip, String entryName) { }

    private static final class SyncIndex {
        final String signature;
        final List<DefEntry> defs;
        final List<AssetRef> assets;
        final Map<String, byte[]> defByHash;
        final Map<String, AssetRef> assetByHash;
        SyncIndex(String signature, List<DefEntry> defs, List<AssetRef> assets,
                  Map<String, byte[]> defByHash, Map<String, AssetRef> assetByHash) {
            this.signature = signature; this.defs = defs; this.assets = assets;
            this.defByHash = defByHash; this.assetByHash = assetByHash;
        }
    }
}
