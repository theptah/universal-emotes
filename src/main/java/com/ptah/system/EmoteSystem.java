package com.ptah.system;

import com.ptah.UniversalEmotesMod;
import com.ptah.bundle.*;
import com.ptah.registry.BundleRegistry;
import com.ptah.registry.EmoteRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class EmoteSystem {
    public static final BundleRegistry BUNDLES = new BundleRegistry();
    public static final EmoteRegistry EMOTES = new EmoteRegistry();
    private EmoteSystem() { }

    public static synchronized void reload() {
        BUNDLES.clear(); EMOTES.clear();
        List<BundleDiagnostic> all = new ArrayList<>();
        String version = FabricLoader.getInstance().getModContainer(UniversalEmotesMod.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("0.0.0");
        BundleLoader loader = new BundleLoader(BundleLimits.DEFAULT, version);
        Map<ResourceLocation, List<BundleDescriptor>> groups = new LinkedHashMap<>();
        try {
            for (Path path : BundleDiscovery.discover()) {
                BundleLoadResult result = loader.load(path); all.addAll(result.diagnostics());
                result.bundle().ifPresent(bundle -> groups.computeIfAbsent(bundle.info().id(), id -> new ArrayList<>()).add(bundle));
            }
            for (List<BundleDescriptor> group : groups.values()) registerGroup(group, all);
        } catch (Exception exception) { all.add(BundleDiagnostic.error(BundleDiscovery.directory().toString(), exception.getMessage())); }
        all.forEach(d -> {
            if (d.severity() == BundleDiagnostic.Severity.ERROR) UniversalEmotesMod.LOGGER.error("Bundle [{}] {}", d.source(), d.message());
            else if (d.severity() == BundleDiagnostic.Severity.WARNING) UniversalEmotesMod.LOGGER.warn("Bundle [{}] {}", d.source(), d.message());
        });
        UniversalEmotesMod.LOGGER.info("Loaded {} bundles ({} files) and {} emotes",
                BUNDLES.size(), BUNDLES.values().size(), EMOTES.values().size());
    }

    private static void registerGroup(List<BundleDescriptor> group, List<BundleDiagnostic> all) {
        BundleDescriptor head = group.get(0);
        boolean collection = head.info().isCollection();
        List<BundleDescriptor> accepted = new ArrayList<>();
        for (BundleDescriptor bundle : group) {
            boolean sameKind = bundle.info().isCollection() == collection;
            if (sameKind && (collection || accepted.isEmpty())) accepted.add(bundle);
            else all.add(BundleDiagnostic.error(bundle.sourceZip().toString(), "Duplicate bundle id: " + bundle.info().id()));
        }
        List<BundleDescriptor> parts = collection ? validateCollection(accepted, all) : accepted;
        if (parts == null) return;
        if (!BUNDLES.register(parts)) {
            parts.forEach(part -> all.add(BundleDiagnostic.error(part.sourceZip().toString(), "Duplicate bundle id: " + part.info().id())));
            return;
        }
        for (BundleDescriptor part : parts) {
            part.emotes().values().forEach(emote -> {
                if (!EMOTES.register(emote)) all.add(BundleDiagnostic.error(part.sourceZip().toString(), "Duplicate emote id: " + emote.id()));
            });
        }
    }

    private static List<BundleDescriptor> validateCollection(List<BundleDescriptor> parts, List<BundleDiagnostic> all) {
        ResourceLocation id = parts.get(0).info().id();
        String source = "collection " + id;
        List<BundleDiagnostic> errors = new ArrayList<>();
        Map<Integer, BundleDescriptor> byOrder = new TreeMap<>();
        for (BundleDescriptor part : parts) {
            BundleDescriptor previous = byOrder.putIfAbsent(part.info().order(), part);
            if (previous != null) errors.add(BundleDiagnostic.error(part.sourceZip().toString(),
                    "Duplicate collection part " + part.info().order() + " (also " + previous.sourceZip().getFileName() + ")"));
        }
        List<BundleDescriptor> sorted = new ArrayList<>(byOrder.values());
        sorted.sort(Comparator.comparingInt(part -> part.info().order()));
        BundleDescriptor reference = sorted.get(0);
        for (BundleDescriptor part : sorted) {
            if (part != reference && !part.manifest().equals(reference.manifest())) {
                errors.add(BundleDiagnostic.error(part.sourceZip().toString(),
                        "pack.json of collection part " + part.info().order() + " differs from part " + reference.info().order()));
            }
        }
        int total = reference.info().total();
        Set<Integer> missing = new TreeSet<>();
        for (int order = 1; order <= total; order++) if (!byOrder.containsKey(order)) missing.add(order);
        if (!missing.isEmpty()) errors.add(BundleDiagnostic.error(source, "Collection is incomplete, missing part(s): " + missing + " of " + total));

        Map<String, BundleDescriptor> assetOwners = new HashMap<>();
        Set<String> union = new HashSet<>();
        for (BundleDescriptor part : sorted) {
            union.addAll(part.entries());
            for (String entry : part.entries()) {
                if (!entry.startsWith("assets/") || entry.endsWith("/")) continue;
                BundleDescriptor owner = assetOwners.putIfAbsent(entry, part);
                if (owner != null) errors.add(BundleDiagnostic.error(part.sourceZip().toString(),
                        "Asset " + entry + " is already provided by collection part " + owner.info().order()));
            }
        }
        for (BundleDescriptor part : sorted) {
            for (String entry : part.externalEntries()) {
                String[] alternatives = entry.split("\\|");
                if (java.util.Arrays.stream(alternatives).noneMatch(union::contains)) errors.add(BundleDiagnostic.error(part.sourceZip().toString(),
                        "Referenced asset is missing in collection: " + String.join(" or ", alternatives)));
            }
        }
        Map<ResourceLocation, BundleDescriptor> emoteOwners = new HashMap<>();
        for (BundleDescriptor part : sorted) {
            for (ResourceLocation emoteId : part.emotes().keySet()) {
                BundleDescriptor owner = emoteOwners.putIfAbsent(emoteId, part);
                if (owner != null) errors.add(BundleDiagnostic.error(part.sourceZip().toString(),
                        "Duplicate emote id " + emoteId + " (also in collection part " + owner.info().order() + ")"));
            }
        }
        if (emoteOwners.isEmpty()) errors.add(BundleDiagnostic.error(source, "Collection contains no valid emotes"));
        if (!errors.isEmpty()) {
            all.addAll(errors);
            all.add(BundleDiagnostic.error(source, "Collection rejected"));
            return null;
        }
        return List.copyOf(sorted);
    }
}
