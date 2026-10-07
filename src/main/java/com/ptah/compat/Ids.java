package com.ptah.compat;

import com.ptah.UniversalEmotesMod;
import net.minecraft.resources.ResourceLocation;

public final class Ids {
    private Ids() { }

    public static ResourceLocation mod(String path) {
        return of(UniversalEmotesMod.MOD_ID, path);
    }

    public static ResourceLocation of(String namespace, String path) {
        //? if >=1.21 {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
        *///?} else
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation parse(String id) {
        //? if >=1.21 {
        /*return ResourceLocation.parse(id);
        *///?} else
        return new ResourceLocation(id);
    }
}
