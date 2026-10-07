package com.ptah.client.config;

import com.ptah.config.ServerConfig;

public final class SyncedServerConfig {
    private static volatile ServerConfig current = new ServerConfig();

    private SyncedServerConfig() { }

    public static ServerConfig get() {
        return current;
    }

    public static void set(ServerConfig config) {
        current = config != null ? config : new ServerConfig();
    }

    public static void reset() {
        current = new ServerConfig();
    }
}
