package com.ptah.bundle;

import com.ptah.UniversalEmotesMod;
import com.ptah.compat.Ids;
import com.ptah.animation.AnimationClip;
import com.ptah.animation.AnimationParser;
import com.ptah.animation.BoneAnimation;
import com.ptah.event.EventParser;
import com.ptah.event.EventTimeline;
import com.ptah.event.EmoteEvent;
import com.ptah.event.type.PlayMusicEvent;
import com.ptah.event.type.PlaySoundEvent;
import com.ptah.event.type.RenderModelEvent;
import com.ptah.event.type.SetSkinEvent;
import com.ptah.event.type.SpawnParticleEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class BundleLoader {
    private static final Set<String> PACK_KEYS = Set.of("schema_version", "min_mod_version", "bundle");
    private static final Set<String> BUNDLE_KEYS = Set.of("namespace", "id", "title", "author", "version", "description", "rig_type", "collection");
    private static final Set<String> INFO_KEYS = Set.of("id", "animation", "name", "description", "rarity", "op");
    private final BundleLimits limits;
    private final String currentModVersion;
    public BundleLoader(BundleLimits limits, String currentModVersion) { this.limits = limits; this.currentModVersion = currentModVersion; }

    public BundleLoadResult load(Path path) {
        List<BundleDiagnostic> diagnostics = new ArrayList<>();
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > limits.maxZipBytes()) throw new IOException("ZIP missing or larger than limit");
            try (ZipFile zip = new ZipFile(path.toFile())) {
                Set<String> entries = index(zip, diagnostics, path.toString());
                if (!entries.contains("pack.json")) diagnostics.add(BundleDiagnostic.error(path.toString(), "pack.json must be at ZIP root"));
                if (!entries.contains("icon.png")) diagnostics.add(BundleDiagnostic.warning(path.toString(), "icon.png is missing"));
                JsonObject pack = json(zip, "pack.json", diagnostics);
                if (pack == null) return new BundleLoadResult(Optional.empty(), diagnostics);
                BundleValidator.unknownKeys(pack, PACK_KEYS, "pack.json", diagnostics);
                int schema = BundleValidator.requiredInteger(pack, "schema_version", 1, 1, "pack.json", diagnostics);
                String minMod = BundleValidator.string(pack, "min_mod_version", "pack.json", diagnostics);
                BundleValidator.semanticVersion(minMod, "min_mod_version", "pack.json", diagnostics);
                if (pack.has("bundle") && !pack.get("bundle").isJsonObject()) diagnostics.add(BundleDiagnostic.error("pack.json", "bundle must be an object"));
                JsonObject bundle = pack.has("bundle") && pack.get("bundle").isJsonObject() ? pack.getAsJsonObject("bundle") : new JsonObject();
                BundleValidator.unknownKeys(bundle, BUNDLE_KEYS, "pack.json#bundle", diagnostics);
                String namespace = BundleValidator.string(bundle, "namespace", "pack.json#bundle", diagnostics);
                String localBundleId = BundleValidator.string(bundle, "id", "pack.json#bundle", diagnostics);
                ResourceLocation bundleId = BundleValidator.id(namespace, localBundleId, "pack.json#bundle", diagnostics);
                String title = BundleValidator.string(bundle, "title", "pack.json#bundle", diagnostics);
                String author = BundleValidator.string(bundle, "author", "pack.json#bundle", diagnostics);
                String version = BundleValidator.string(bundle, "version", "pack.json#bundle", diagnostics);
                BundleValidator.semanticVersion(version, "bundle.version", "pack.json#bundle", diagnostics);
                String description = BundleValidator.optionalString(bundle, "description", "pack.json#bundle", diagnostics);
                String rigType = readRigType(bundle, diagnostics);
                BundleCollection collection = readCollection(bundle, diagnostics);
                Set<String> externalEntries = collection == null ? null : new TreeSet<>();
                if (compareSemver(currentModVersion, minMod) < 0) diagnostics.add(BundleDiagnostic.error("pack.json", "Requires mod " + minMod + ", current is " + currentModVersion));

                Map<String, JsonObject> models = models(zip, entries, diagnostics);
                for (Map.Entry<String, JsonObject> model : models.entrySet()) {
                    for (String texture : ModelTextures.usage(model.getValue()).explicit()) {
                        requireTexture(namespace, texture, entries, externalEntries, model.getKey(), diagnostics);
                    }
                }

                SortedSet<String> emoteFolders = new TreeSet<>();
                for (String entry : entries) if (entry.startsWith("emotes/") && entry.endsWith("/info.json")) {
                    String folder = entry.substring("emotes/".length(), entry.length() - "/info.json".length());
                    if (!folder.isBlank() && !folder.contains("/")) emoteFolders.add(folder);
                }
                if (emoteFolders.size() > limits.maxEmotes()) diagnostics.add(BundleDiagnostic.error(path.toString(), "Too many emotes"));
                Map<ResourceLocation, EmoteDefinition> emotes = new LinkedHashMap<>();
                for (String folder : emoteFolders.stream().limit(limits.maxEmotes()).toList()) {
                    String base = "emotes/" + folder + "/";
                    JsonObject info = json(zip, base + "info.json", diagnostics);
                    JsonObject animationJson = json(zip, base + "animation.json", diagnostics);
                    if (info == null || animationJson == null) continue;
                    BundleValidator.unknownKeys(info, INFO_KEYS, base + "info.json", diagnostics);
                    String localId = BundleValidator.string(info, "id", base + "info.json", diagnostics);
                    String animationKey = BundleValidator.string(info, "animation", base + "info.json", diagnostics);
                    String name = BundleValidator.string(info, "name", base + "info.json", diagnostics);
                    String emoteDescription = BundleValidator.optionalString(info, "description", base + "info.json", diagnostics);
                    int op = BundleValidator.integer(info, "op", 0, 0, 4, base + "info.json", diagnostics);
                    Rarity rarity;
                    try { rarity = Rarity.parse(BundleValidator.string(info, "rarity", base + "info.json", diagnostics)); }
                    catch (Exception exception) { diagnostics.add(BundleDiagnostic.error(base + "info.json", "Invalid rarity")); rarity = Rarity.COMMON; }
                    ResourceLocation emoteId = BundleValidator.id(namespace, localId, base + "info.json", diagnostics);
                    String animationEntry = base + "animation.json";

                    AnimationClip validated = AnimationParser.parse(animationJson, animationKey, limits, animationEntry, diagnostics);
                    AnimationClip clip = AnimationClip.lazy(validated.formatVersion(), validated.key(),
                            validated.loop(), validated.length(), validated.loopStart(),
                            () -> loadBones(path, animationEntry, animationKey));
                    JsonObject eventsJson = entries.contains(base + "events.json") ? json(zip, base + "events.json", diagnostics) : null;
                    EventTimeline timeline = EventParser.parse(eventsJson, validated.length(), limits, base + "events.json", diagnostics);
                    validateEventAssets(namespace, timeline, entries, externalEntries, models, base + "events.json", diagnostics);
                    EmoteDefinition previous = emotes.putIfAbsent(emoteId, new EmoteDefinition(emoteId, bundleId, localId, animationKey, name, emoteDescription, op, rarity, clip, timeline, rigType));
                    if (previous != null) diagnostics.add(BundleDiagnostic.error(base + "info.json", "Duplicate emote id: " + emoteId));
                }
                if (emotes.isEmpty() && collection == null) diagnostics.add(BundleDiagnostic.error(path.toString(), "Bundle contains no valid emotes"));
                if (BundleValidator.hasErrors(diagnostics)) return new BundleLoadResult(Optional.empty(), diagnostics);
                BundleInfo bundleInfo = new BundleInfo(bundleId, namespace, title, author, version, description, schema, minMod, rigType, collection);
                return new BundleLoadResult(Optional.of(new BundleDescriptor(bundleInfo, path, emotes, entries,
                        externalEntries == null ? Set.of() : externalEntries, manifest(pack))), diagnostics);
            }
        } catch (Exception exception) {
            diagnostics.add(BundleDiagnostic.error(path.toString(), exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()));
            return new BundleLoadResult(Optional.empty(), diagnostics);
        }
    }

    private String readRigType(JsonObject bundle, List<BundleDiagnostic> diagnostics) {
        if (!bundle.has("rig_type") || bundle.get("rig_type").isJsonNull()) return "r6";
        try {
            String value = bundle.get("rig_type").getAsString().trim().toLowerCase(Locale.ROOT);
            if (value.equals("r6") || value.equals("r15")) return value;
            diagnostics.add(BundleDiagnostic.warning("pack.json#bundle", "Unknown rig_type '" + value + "', defaulting to r6"));
        } catch (Exception exception) {
            diagnostics.add(BundleDiagnostic.warning("pack.json#bundle", "Invalid rig_type, defaulting to r6"));
        }
        return "r6";
    }

    private BundleCollection readCollection(JsonObject bundle, List<BundleDiagnostic> diagnostics) {
        if (!bundle.has("collection") || bundle.get("collection").isJsonNull()) return null;
        String source = "pack.json#bundle.collection";
        if (!bundle.get("collection").isJsonObject()) {
            diagnostics.add(BundleDiagnostic.error(source, "collection must be an object"));
            return null;
        }
        JsonObject collection = bundle.getAsJsonObject("collection");
        for (String key : collection.keySet()) {
            if (!key.equals("total") && !key.equals("order")) diagnostics.add(BundleDiagnostic.warning(source, "Unknown collection key: " + key));
        }
        int total = BundleValidator.requiredInteger(collection, "total", 1, limits.maxCollectionParts(), source, diagnostics);
        int order = BundleValidator.requiredInteger(collection, "order", 1, total, source, diagnostics);
        return new BundleCollection(order, total);
    }

    private JsonObject manifest(JsonObject pack) {
        JsonObject copy = pack.deepCopy();
        if (copy.has("bundle") && copy.get("bundle").isJsonObject()) {
            JsonObject bundle = copy.getAsJsonObject("bundle");
            if (bundle.has("collection") && bundle.get("collection").isJsonObject()) {
                bundle.getAsJsonObject("collection").remove("order");
            }
        }
        return copy;
    }

    private void requireEntry(String entry, Set<String> entries, Set<String> externalEntries, String source,
                              List<BundleDiagnostic> diagnostics) {
        if (entries.contains(entry)) return;
        if (externalEntries != null) externalEntries.add(entry);
        else diagnostics.add(BundleDiagnostic.error(source, "Referenced asset is missing: " + entry));
    }

    private Map<String, JsonObject> models(ZipFile zip, Set<String> entries, List<BundleDiagnostic> diagnostics) {
        Map<String, JsonObject> models = new TreeMap<>();
        for (String entry : new TreeSet<>(entries)) {
            if (!entry.startsWith("assets/models/") || !entry.endsWith(".json")) continue;
            ZipEntry zipEntry = zip.getEntry(entry);
            if (zipEntry == null || zipEntry.isDirectory()) continue;
            if (zipEntry.getSize() > limits.maxJsonBytes()) {
                diagnostics.add(BundleDiagnostic.warning(entry, "Model is larger than the JSON limit; its texture references were not checked"));
                continue;
            }
            JsonObject model = json(zip, entry, diagnostics);
            if (model != null) models.put(entry, model);
        }
        return models;
    }

    private void requireTexture(String namespace, String raw, Set<String> entries, Set<String> externalEntries,
                                String source, List<BundleDiagnostic> diagnostics) {
        if (raw == null || raw.isBlank() || ModelTextures.vanilla(raw)) return;
        List<String> candidates = new ArrayList<>();
        for (String path : ModelTextures.candidates(raw)) {
            try {
                Ids.of(namespace, "textures/" + path + ".png");
                candidates.add("assets/textures/" + path + ".png");
            } catch (Exception ignored) { }
        }
        if (candidates.isEmpty()) {
            diagnostics.add(BundleDiagnostic.error(source, "Invalid texture reference: " + raw));
            return;
        }
        for (String candidate : candidates) if (entries.contains(candidate)) return;
        if (externalEntries != null) externalEntries.add(String.join("|", candidates));
        else diagnostics.add(BundleDiagnostic.error(source, "Referenced texture is missing: " + String.join(" or ", candidates)));
    }

    private void validateEventAssets(String namespace, EventTimeline timeline, Set<String> entries,
                                     Set<String> externalEntries, Map<String, JsonObject> models, String source,
                                     List<BundleDiagnostic> diagnostics) {
        for (EmoteEvent event : timeline.events()) {
            ResourceLocation asset = null; String entry = null;

            ResourceLocation alt = event instanceof PlayMusicEvent m ? m.dmcaAlt() : event instanceof PlaySoundEvent so ? so.dmcaAlt() : null;
            if (alt != null && !alt.getNamespace().equals("minecraft")) {
                if (!alt.getNamespace().equals(namespace)) {
                    diagnostics.add(BundleDiagnostic.error(source, "Cross-bundle asset reference is not allowed: " + alt));
                } else {
                    requireEntry("assets/audio/" + alt.getPath() + ".ogg", entries, externalEntries, source, diagnostics);
                }
            }
            if (event instanceof PlayMusicEvent music) { asset = music.assetId(); entry = "assets/audio/" + asset.getPath() + ".ogg"; }
            else if (event instanceof PlaySoundEvent sound) { asset = sound.assetId(); entry = "assets/audio/" + asset.getPath() + ".ogg"; }
            else if (event instanceof RenderModelEvent model) {
                asset = model.assetId(); entry = "assets/models/" + asset.getPath() + ".json";
                if (asset.getNamespace().equals(namespace)) {
                    JsonObject geometry = models.get(entry);
                    if (model.texture() != null) {
                        if (geometry == null || ModelTextures.usage(geometry).usesBase()) {
                            requireTexture(namespace, model.texture(), entries, externalEntries, source, diagnostics);
                        }
                    } else if (geometry != null && ModelTextures.usage(geometry).usesBase()) {
                        requireTexture(namespace, asset.getPath(), entries, externalEntries, source, diagnostics);
                    }
                }
            }
            else if (event instanceof SetSkinEvent skin) { asset = skin.assetId(); entry = "assets/textures/" + asset.getPath() + ".png"; }
            else if (event instanceof SpawnParticleEvent particle) { asset = particle.assetId(); entry = "assets/textures/" + asset.getPath() + ".png"; }
            if (asset == null || asset.getNamespace().equals("minecraft")) continue;
            if (!asset.getNamespace().equals(namespace)) {
                diagnostics.add(BundleDiagnostic.error(source, "Cross-bundle asset reference is not allowed: " + asset));
            } else if (entry != null) {
                requireEntry(entry, entries, externalEntries, source, diagnostics);
            }
        }
    }

    private Set<String> index(ZipFile zip, List<BundleDiagnostic> diagnostics, String source) {
        Set<String> names = new HashSet<>(); long total = 0;
        Enumeration<? extends ZipEntry> enumeration = zip.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement(); String name = entry.getName();
            if (!safe(name)) { diagnostics.add(BundleDiagnostic.error(source, "Unsafe ZIP entry: " + name)); continue; }
            if (!names.add(name)) diagnostics.add(BundleDiagnostic.error(source, "Duplicate ZIP entry: " + name));
            if (!entry.isDirectory() && entry.getSize() < 0) diagnostics.add(BundleDiagnostic.error(source, "ZIP entry has unknown size: " + name));
            if (entry.getSize() > 0) total += entry.getSize();
            long compressed = entry.getCompressedSize();
            if (entry.getSize() > 1024L * 1024L && compressed > 0 && entry.getSize() / compressed > 200L) {
                diagnostics.add(BundleDiagnostic.error(source, "Suspicious compression ratio: " + name));
            }
            if (total > limits.maxExpandedBytes()) { diagnostics.add(BundleDiagnostic.error(source, "Expanded ZIP exceeds limit")); break; }
        }
        return names;
    }
    private boolean safe(String name) {
        if (name.isBlank() || name.startsWith("/") || name.startsWith("\\") || name.contains("\\") || name.indexOf('\0') >= 0) return false;
        String checked = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
        if (checked.isBlank()) return false;
        try { return Path.of(checked).normalize().toString().replace('\\', '/').equals(checked) && !checked.startsWith("../") && !checked.contains("/../"); }
        catch (Exception exception) { return false; }
    }

    private Map<String, BoneAnimation> loadBones(Path zipPath, String entryName, String key) {
        List<BundleDiagnostic> ignored = new ArrayList<>();
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            JsonObject json = json(zip, entryName, ignored);
            if (json == null) return Map.of();
            return AnimationParser.parse(json, key, limits, entryName, ignored).bones();
        } catch (Exception exception) {
            UniversalEmotesMod.LOGGER.warn("Could not reload animation bones from {} ({})", zipPath, exception.toString());
            return Map.of();
        }
    }

    private JsonObject json(ZipFile zip, String name, List<BundleDiagnostic> diagnostics) {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null || entry.isDirectory()) { diagnostics.add(BundleDiagnostic.error(name, "Required JSON missing")); return null; }
        try (InputStream input = zip.getInputStream(entry)) {
            byte[] bytes = readLimited(input, limits.maxJsonBytes());
            return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) { diagnostics.add(BundleDiagnostic.error(name, "Invalid JSON: " + exception.getMessage())); return null; }
    }
    private byte[] readLimited(InputStream input, long limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int read; long total = 0;
        while ((read = input.read(buffer)) >= 0) { total += read; if (total > limit) throw new IOException("File exceeds limit"); output.write(buffer, 0, read); }
        return output.toByteArray();
    }
    /**
     * SemVer 2.0.0 precedence: build metadata (+...) is ignored, a pre-release
     * (-alpha.3, -beta.1, -rc.1) is lower than the matching release, and
     * pre-release identifiers are compared field by field (numeric fields
     * numerically, others lexically; numeric < alphanumeric; shorter prefix is lower).
     * Example: 1.0.0-alpha.3 < 1.0.0-beta.1 < 1.0.0-beta.2 < 1.0.0-beta.10 < 1.0.0-rc.1 < 1.0.0 < 1.1.0
     */
    static int compareSemver(String a, String b) {
        try {
            String av = a.trim(), bv = b.trim();
            int plusA = av.indexOf('+'); if (plusA >= 0) av = av.substring(0, plusA);
            int plusB = bv.indexOf('+'); if (plusB >= 0) bv = bv.substring(0, plusB);
            int dashA = av.indexOf('-'), dashB = bv.indexOf('-');
            String coreA = dashA >= 0 ? av.substring(0, dashA) : av;
            String coreB = dashB >= 0 ? bv.substring(0, dashB) : bv;
            String preA = dashA >= 0 ? av.substring(dashA + 1) : null;
            String preB = dashB >= 0 ? bv.substring(dashB + 1) : null;
            String[] partsA = coreA.split("\\."), partsB = coreB.split("\\.");
            for (int i = 0; i < 3; i++) {
                long x = i < partsA.length ? Long.parseLong(partsA[i]) : 0;
                long y = i < partsB.length ? Long.parseLong(partsB[i]) : 0;
                if (x != y) return Long.compare(x, y);
            }
            if (preA == null && preB == null) return 0;
            if (preA == null) return 1;
            if (preB == null) return -1;
            String[] idsA = preA.split("\\."), idsB = preB.split("\\.");
            int n = Math.min(idsA.length, idsB.length);
            for (int i = 0; i < n; i++) {
                String x = idsA[i], y = idsB[i];
                boolean xNum = x.matches("\\d+"), yNum = y.matches("\\d+");
                int cmp;
                if (xNum && yNum) cmp = new java.math.BigInteger(x).compareTo(new java.math.BigInteger(y));
                else if (xNum) cmp = -1;
                else if (yNum) cmp = 1;
                else cmp = x.compareTo(y);
                if (cmp != 0) return cmp;
            }
            return Integer.compare(idsA.length, idsB.length);
        } catch (Exception ignored) { }
        return 0;
    }
}
