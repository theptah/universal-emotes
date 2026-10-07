package com.ptah.client.network;

import com.ptah.client.config.SyncedServerConfig;
import com.ptah.config.ServerConfig;
import com.ptah.network.ServerConfigNetwork;
import com.ptah.client.compat.ClientNet;
import com.ptah.compat.Net;
import net.minecraft.network.FriendlyByteBuf;

public final class ClientConfigNetwork {
    private static volatile boolean editAllowedByServer;

    private ClientConfigNetwork() { }

    public static void initialize() {
        ClientNet.receive(ServerConfigNetwork.CONFIG_SYNC, (client, buffer) -> {
            ServerConfig config = ServerConfigNetwork.read(buffer);
            boolean canEdit = buffer.readBoolean();
            client.execute(() -> {
                SyncedServerConfig.set(config);
                editAllowedByServer = canEdit;
            });
        });
    }

    public static boolean isEditAllowedByServer() {
        return editAllowedByServer;
    }

    public static boolean sendUpdate(ServerConfig config) {
        if (!ClientNet.canSend(ServerConfigNetwork.CONFIG_UPDATE)) return false;
        FriendlyByteBuf buffer = Net.buf();
        ServerConfigNetwork.write(buffer, config);
        ClientNet.send(ServerConfigNetwork.CONFIG_UPDATE, buffer);
        return true;
    }

    public static void reset() {
        editAllowedByServer = false;
        SyncedServerConfig.reset();
    }
}
