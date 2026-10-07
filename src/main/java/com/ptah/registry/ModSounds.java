package com.ptah.registry;

import com.ptah.compat.Ids;
import com.ptah.UniversalEmotesMod;
import net.minecraft.core.Registry;
//? if >=1.19.3
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final ResourceLocation WHEEL_HOVER_ID = Ids.mod("ui_hover");
    public static final ResourceLocation WHEEL_FAVORITE_ID = Ids.mod("ui_favorite");
    public static final ResourceLocation WHEEL_UNFAVORITE_ID = Ids.mod("ui_unfavorite");
    public static final ResourceLocation RIDING_ALERT_ID = Ids.mod("ui_alert");

    public static SoundEvent WHEEL_HOVER;
    public static SoundEvent WHEEL_FAVORITE;
    public static SoundEvent WHEEL_UNFAVORITE;
    public static SoundEvent RIDING_ALERT;

    private ModSounds() {
    }

    public static void register() {
        WHEEL_HOVER = register(WHEEL_HOVER_ID);
        WHEEL_FAVORITE = register(WHEEL_FAVORITE_ID);
        WHEEL_UNFAVORITE = register(WHEEL_UNFAVORITE_ID);
        RIDING_ALERT = register(RIDING_ALERT_ID);
    }

    private static SoundEvent register(ResourceLocation id) {
        //? if >=1.19.3 {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
        //?} else
        /*return Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id));*/
    }
}
