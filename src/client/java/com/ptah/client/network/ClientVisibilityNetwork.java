package com.ptah.client.network;

import com.ptah.client.config.ClientConfig;
import com.ptah.client.config.ClientConfigManager;
import com.ptah.client.render.EmoteVisibilityState;
import com.ptah.network.EmoteVisibilityNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.ptah.client.compat.ClientNet;
import com.ptah.compat.Net;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public final class ClientVisibilityNetwork {
    private ClientVisibilityNetwork() { }

    public static void initialize() {
        ClientNet.receive(EmoteVisibilityNetwork.SYNC, (client, buffer) -> {
            UUID id = buffer.readUUID();
            boolean hideArmor = buffer.readBoolean();
            boolean hideHeldItems = buffer.readBoolean();
            client.execute(() -> EmoteVisibilityState.set(id, hideArmor, hideHeldItems));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(ClientVisibilityNetwork::sendCurrent));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EmoteVisibilityState.clearAll());
    }

    public static boolean sendCurrent() {
        if (!ClientNet.canSend(EmoteVisibilityNetwork.UPDATE)) return false;
        ClientConfig config = ClientConfigManager.get();
        FriendlyByteBuf buffer = Net.buf();
        buffer.writeBoolean(config.hideArmorDuringEmotes);
        buffer.writeBoolean(config.hideHeldItemsDuringEmotes);
        ClientNet.send(EmoteVisibilityNetwork.UPDATE, buffer);
        return true;
    }
}
