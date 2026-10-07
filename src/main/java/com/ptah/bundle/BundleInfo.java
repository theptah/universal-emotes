package com.ptah.bundle;

import net.minecraft.resources.ResourceLocation;

public record BundleInfo(ResourceLocation id, String namespace, String title, String author,
                         String version, String description, int schemaVersion, String minModVersion,
                         String rigType, BundleCollection collection) {
    public boolean isCollection() { return collection != null; }
    public int order() { return collection == null ? 1 : collection.order(); }
    public int total() { return collection == null ? 1 : collection.total(); }
}
