package com.ptah.registry;

import com.ptah.bundle.BundleDescriptor;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BundleRegistry {
    private final Map<ResourceLocation, List<BundleDescriptor>> entries = new LinkedHashMap<>();
    public boolean register(BundleDescriptor bundle) { return register(List.of(bundle)); }
    public boolean register(List<BundleDescriptor> parts) {
        if (parts.isEmpty()) return false;
        return entries.putIfAbsent(parts.get(0).info().id(), List.copyOf(parts)) == null;
    }
    public boolean contains(ResourceLocation id) { return entries.containsKey(id); }
    public List<BundleDescriptor> parts(ResourceLocation id) { return entries.getOrDefault(id, List.of()); }
    public Collection<BundleDescriptor> values() {
        List<BundleDescriptor> all = new ArrayList<>();
        entries.values().forEach(all::addAll);
        return List.copyOf(all);
    }
    public int size() { return entries.size(); }
    public void clear() { entries.clear(); }
}
