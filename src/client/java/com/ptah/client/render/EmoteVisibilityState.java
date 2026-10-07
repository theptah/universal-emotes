package com.ptah.client.render;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EmoteVisibilityState {
    private static final Map<UUID, boolean[]> STATE = new ConcurrentHashMap<>();

    private EmoteVisibilityState() { }

    public static void set(UUID playerId, boolean hideArmor, boolean hideHeldItems) {
        STATE.put(playerId, new boolean[]{hideArmor, hideHeldItems});
    }

    public static void clearAll() { STATE.clear(); }

    public static boolean hideArmor(UUID playerId) {
        boolean[] flags = STATE.get(playerId);
        return flags != null && flags[0];
    }

    public static boolean hideHeldItems(UUID playerId) {
        boolean[] flags = STATE.get(playerId);
        return flags != null && flags[1];
    }
}
