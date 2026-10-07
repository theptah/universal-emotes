package com.ptah.config;

import com.ptah.compat.ServerCompat;
import com.ptah.bundle.EmoteDefinition;
import com.ptah.bundle.Rarity;
import net.minecraft.world.entity.player.Player;

public final class ServerConfig {
    public boolean ignoreAnimationsOpLevel = false;

    public boolean enableLevelRequirement = true;

    public boolean disallowInterruptOnDamage = false;

    public int legendaryLevel = 50;
    public int rareLevel = 25;
    public int uncommonLevel = 12;
    public int commonLevel = 5;
    public int complementaryLevel = 0;

    public int levelFor(Rarity rarity) {
        return switch (rarity) {
            case LEGENDARY -> legendaryLevel;
            case RARE -> rareLevel;
            case UNCOMMON -> uncommonLevel;
            case COMMON -> commonLevel;
            case COMPLEMENTARY -> complementaryLevel;
        };
    }

    public void setLevelFor(Rarity rarity, int level) {
        switch (rarity) {
            case LEGENDARY -> legendaryLevel = level;
            case RARE -> rareLevel = level;
            case UNCOMMON -> uncommonLevel = level;
            case COMMON -> commonLevel = level;
            case COMPLEMENTARY -> complementaryLevel = level;
        }
    }

    public int missingOpLevel(Player player, EmoteDefinition emote) {
        return (!ignoreAnimationsOpLevel && emote.op() > 0 && !ServerCompat.hasPermission(player, emote.op())) ? emote.op() : 0;
    }

    public int missingLevel(Player player, EmoteDefinition emote) {
        if (!enableLevelRequirement) return 0;
        int required = levelFor(emote.rarity());
        return (required > 0 && player.experienceLevel < required) ? required : 0;
    }

    public boolean canUse(Player player, EmoteDefinition emote) {
        return missingOpLevel(player, emote) == 0 && missingLevel(player, emote) == 0;
    }

    public void sanitize() {
        legendaryLevel = clampLevel(legendaryLevel);
        rareLevel = clampLevel(rareLevel);
        uncommonLevel = clampLevel(uncommonLevel);
        commonLevel = clampLevel(commonLevel);
        complementaryLevel = clampLevel(complementaryLevel);
    }

    private static int clampLevel(int value) {
        if (value < 0) return 0;

        return Math.min(value, 21_863);
    }

    public ServerConfig copy() {
        ServerConfig copy = new ServerConfig();
        copy.ignoreAnimationsOpLevel = ignoreAnimationsOpLevel;
        copy.enableLevelRequirement = enableLevelRequirement;
        copy.disallowInterruptOnDamage = disallowInterruptOnDamage;
        copy.legendaryLevel = legendaryLevel;
        copy.rareLevel = rareLevel;
        copy.uncommonLevel = uncommonLevel;
        copy.commonLevel = commonLevel;
        copy.complementaryLevel = complementaryLevel;
        return copy;
    }
}
