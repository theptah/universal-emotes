package com.ptah.client.resource;

import com.ptah.compat.Ids;
import com.ptah.client.compat.ClientCompat;
import com.ptah.UniversalEmotesMod;
import com.ptah.bundle.BundleDescriptor;
import com.ptah.client.render.BundleModelGeometry;
import com.ptah.client.network.ClientBundleSync.CachedAsset;
import com.ptah.system.EmoteSystem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class BundleResourcePack {
    private static final String PACK_FOLDER = "universal-emotes-bundle-audio";
    private static final String PACK_ID = "file/" + PACK_FOLDER;

    private static final String SIGNATURE_FILE = PACK_FOLDER + ".signature";

    private static final long STREAM_THRESHOLD_BYTES = 512L * 1024L;
    private static final int SKIPPED = -1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<CachedAsset> serverAssets = List.of();

    private BundleResourcePack() {
    }

    public static void setServerAssets(Collection<CachedAsset> assets) {
        serverAssets = List.copyOf(assets);
    }

    public static CompletableFuture<Integer> reload() {
        Minecraft client = Minecraft.getInstance();
        try {
            Path resourcePacks = client.gameDirectory.toPath().resolve("resourcepacks");
            Path packDir = resourcePacks.resolve(PACK_FOLDER);
            Path signatureFile = resourcePacks.resolve(SIGNATURE_FILE);

            String localSignature = computeSignature(false);
            String fullSignature = computeSignature(true);
            String[] persisted = readSignature(signatureFile);
            PackRepository repository = client.getResourcePackRepository();
            boolean packReady = Files.isDirectory(packDir)
                    && client.options.resourcePacks.contains(PACK_ID);

            if (packReady
                    && (fullSignature.equals(persisted[1])
                        || (serverAssets.isEmpty() && localSignature.equals(persisted[0])))) {
                repository.reload();
                BundleModelGeometry.clearCache();
                UniversalEmotesMod.LOGGER.info("Bundle sound resources unchanged; skipped resource reload");
                return CompletableFuture.completedFuture(SKIPPED);
            }

            int soundCount = buildPack(client);
            repository.reload();
            ArrayList<String> selected = new ArrayList<>(repository.getSelectedIds());
            selected.remove(PACK_ID);
            selected.add(PACK_ID);
            repository.setSelected(selected);
            List<String> selectedIds = new ArrayList<>(repository.getSelectedIds());
            if (!selectedIds.equals(client.options.resourcePacks)) {
                client.options.resourcePacks = selectedIds;
                client.options.save();
            }
            writeSignature(signatureFile, localSignature, fullSignature);
            return client.reloadResourcePacks().thenApply(ignored -> {
                BundleModelGeometry.clearCache();
                com.ptah.client.playback.EmoteMusicHandle.clearCache();
                UniversalEmotesMod.LOGGER.info("Loaded {} bundle sound resources", soundCount);
                return soundCount;
            });
        } catch (Exception exception) {
            UniversalEmotesMod.LOGGER.error("Failed to prepare bundle sound resources", exception);
            return CompletableFuture.failedFuture(exception);
        }
    }

    private static String computeSignature(boolean includeServerAssets) {
        List<String> parts = new ArrayList<>();
        parts.add("pack_format:" + ClientCompat.resourcePackFormat());
        for (BundleDescriptor bundle : EmoteSystem.BUNDLES.values()) {
            String namespace = bundle.info().namespace();
            try {
                Path zip = bundle.sourceZip();
                parts.add("bundle:" + namespace + ":" + zip + ":" + Files.size(zip)
                        + ":" + Files.getLastModifiedTime(zip).toMillis());
            } catch (Exception exception) {
                parts.add("bundle:" + namespace + ":unavailable");
            }
        }
        if (includeServerAssets) {
            for (CachedAsset cached : serverAssets) {
                parts.add("asset:" + cached.kind() + ":" + cached.id() + ":" + cached.hash());
            }
        }
        Collections.sort(parts);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.join("\n", parts).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            return Integer.toHexString(parts.hashCode());
        }
    }

    private static String[] readSignature(Path file) {
        try {
            if (Files.isRegularFile(file)) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                String local = lines.size() > 0 ? lines.get(0).trim() : "";
                String full = lines.size() > 1 ? lines.get(1).trim() : "";
                return new String[] {local, full};
            }
        } catch (IOException exception) {
        }
        return new String[] {"", ""};
    }

    private static void writeSignature(Path file, String localSignature, String fullSignature) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, localSignature + "\n" + fullSignature, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            UniversalEmotesMod.LOGGER.warn("Could not persist bundle resource signature", exception);
        }
    }

    private static int buildPack(Minecraft client) throws IOException {
        Path resourcePacks = client.gameDirectory.toPath().resolve("resourcepacks");
        Path target = resourcePacks.resolve(PACK_FOLDER);
        Path temporary = resourcePacks.resolve(PACK_FOLDER + ".tmp");
        Files.createDirectories(resourcePacks);
        deleteTree(temporary);
        Files.createDirectories(temporary);

        JsonObject packRoot = new JsonObject();
        JsonObject pack = new JsonObject();
        int format = ClientCompat.resourcePackFormat();
        if (format >= 65) {
            pack.addProperty("min_format", format);
            pack.addProperty("max_format", format);
        } else {
            pack.addProperty("pack_format", format);
        }
        pack.addProperty("description", "Generated Emote Wheel bundle audio");
        packRoot.add("pack", pack);
        Files.writeString(temporary.resolve("pack.mcmeta"), GSON.toJson(packRoot), StandardCharsets.UTF_8);

        Map<String, JsonObject> soundIndexes = new HashMap<>();
        Set<ResourceLocation> emitted = new HashSet<>();
        int count = 0;

        for (BundleDescriptor bundle : EmoteSystem.BUNDLES.values()) {
            String namespace = bundle.info().namespace();
            try (ZipFile zip = new ZipFile(bundle.sourceZip().toFile())) {
                for (String entryName : bundle.entries()) {
                    if (!entryName.startsWith("assets/audio/") || !entryName.endsWith(".ogg")) continue;
                    String path = entryName.substring("assets/audio/".length(), entryName.length() - ".ogg".length());
                    ResourceLocation soundId = Ids.of(namespace, path);
                    if (!emitted.add(soundId)) {
                        UniversalEmotesMod.LOGGER.warn("Duplicate bundle audio id {}; first file wins", soundId);
                        continue;
                    }

                    ZipEntry entry = zip.getEntry(entryName);
                    if (entry == null || entry.isDirectory()) continue;
                    try (InputStream input = zip.getInputStream(entry)) {
                        Files.copy(input, packFile(temporary, namespace, "sounds", path + ".ogg"), StandardCopyOption.REPLACE_EXISTING);
                    }
                    registerSound(soundIndexes, soundId, entry.getSize());
                    count++;
                }
            }
        }

        copyLocalEntries(temporary, "assets/textures/", ".png", "textures");
        copyLocalEntries(temporary, "assets/models/", ".json", "models");

        for (CachedAsset cached : serverAssets) {
            ResourceLocation id = cached.id();
            if (cached.kind().equals("audio")) {
                Files.copy(cached.file(), packFile(temporary, id.getNamespace(), "sounds", id.getPath() + ".ogg"),
                        StandardCopyOption.REPLACE_EXISTING);
                registerSound(soundIndexes, id, Files.size(cached.file()));
                count++;
            } else if (cached.kind().equals("texture")) {
                Files.copy(cached.file(), packFile(temporary, id.getNamespace(), "textures", id.getPath() + ".png"),
                        StandardCopyOption.REPLACE_EXISTING);
            } else if (cached.kind().equals("model")) {
                Files.copy(cached.file(), packFile(temporary, id.getNamespace(), "models", id.getPath() + ".json"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }

        for (Map.Entry<String, JsonObject> index : soundIndexes.entrySet()) {
            Path namespaceRoot = temporary.resolve("assets").resolve(index.getKey());
            Files.createDirectories(namespaceRoot);
            Files.writeString(namespaceRoot.resolve("sounds.json"), GSON.toJson(index.getValue()), StandardCharsets.UTF_8);
        }

        deleteTree(target);
        Files.move(temporary, target);
        return count;
    }

    private static void registerSound(Map<String, JsonObject> soundIndexes, ResourceLocation id, long oggBytes) {
        JsonObject sound = new JsonObject();
        sound.addProperty("name", id.toString());

        sound.addProperty("stream", oggBytes < 0 || oggBytes > STREAM_THRESHOLD_BYTES);
        JsonArray sounds = new JsonArray();
        sounds.add(sound);
        JsonObject registration = new JsonObject();
        registration.addProperty("replace", true);
        registration.add("sounds", sounds);
        soundIndexes.computeIfAbsent(id.getNamespace(), ignored -> new JsonObject()).add(id.getPath(), registration);
    }

    private static void copyLocalEntries(Path pack, String prefix, String ext, String folder) throws IOException {
        for (BundleDescriptor bundle : EmoteSystem.BUNDLES.values()) {
            String namespace = bundle.info().namespace();
            try (ZipFile zip = new ZipFile(bundle.sourceZip().toFile())) {
                for (String entryName : bundle.entries()) {
                    if (!entryName.startsWith(prefix) || !entryName.endsWith(ext)) continue;
                    ZipEntry entry = zip.getEntry(entryName);
                    if (entry == null || entry.isDirectory()) continue;
                    String path = entryName.substring(prefix.length(), entryName.length() - ext.length());
                    try (InputStream input = zip.getInputStream(entry)) {
                        Files.copy(input, packFile(pack, namespace, folder, path + ext), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private static Path packFile(Path pack, String namespace, String folder, String relative) throws IOException {
        Path root = pack.resolve("assets").resolve(namespace).resolve(folder).normalize();
        Path output = root.resolve(relative).normalize();
        if (!output.startsWith(root)) throw new IOException("Unsafe bundle asset path: " + namespace + ":" + relative);
        Files.createDirectories(output.getParent());
        return output;
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
