package com.ptah.bundle;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public record BundleDescriptor(BundleInfo info, Path sourceZip,
                               Map<ResourceLocation, EmoteDefinition> emotes, Set<String> entries,
                               Set<String> externalEntries, JsonObject manifest) {
    public BundleDescriptor {
        emotes = Map.copyOf(emotes);
        entries = Set.copyOf(entries);
        externalEntries = Set.copyOf(externalEntries);
        manifest = manifest.deepCopy();
    }

    public JsonObject manifest() { return manifest.deepCopy(); }
}
