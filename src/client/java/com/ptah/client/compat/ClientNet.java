package com.ptah.client.compat;

import com.ptah.compat.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class ClientNet {
    @FunctionalInterface
    public interface ClientHandler {
        void handle(Minecraft client, FriendlyByteBuf buf);
    }

    private ClientNet() { }

    public static void receive(ResourceLocation channel, ClientHandler handler) {
        //? if >=1.20.5 {
        /*ClientPlayNetworking.registerGlobalReceiver(Net.type(channel), (payload, context) ->
                handler.handle(context.client(), payload.buf()));
        *///?} else {
        ClientPlayNetworking.registerGlobalReceiver(channel, (client, listener, buf, sender) ->
                handler.handle(client, buf));
        //?}
    }

    public static boolean canSend(ResourceLocation channel) {
        return ClientPlayNetworking.canSend(channel);
    }

    public static void send(ResourceLocation channel, FriendlyByteBuf buf) {
        //? if >=1.20.5 {
        /*ClientPlayNetworking.send(Net.payload(channel, buf));
        *///?} else
        ClientPlayNetworking.send(channel, buf);
    }
}
