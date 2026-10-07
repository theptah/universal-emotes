package com.ptah.bundle;

import java.util.Locale;

public enum Rarity {
    LEGENDARY, RARE, UNCOMMON, COMMON, COMPLEMENTARY;
    public static Rarity parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
