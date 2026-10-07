package com.ptah.compat;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

//? if >=1.20.5 {
/*import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
*///?}

public final class Net {
    @FunctionalInterface
    public interface ServerHandler {
        void handle(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf);
    }

    //? if >=1.20.5 {
    /*public record RawPayload(CustomPacketPayload.Type<RawPayload> type, byte[] data) implements CustomPacketPayload {
        public FriendlyByteBuf buf() {
            return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        }
    }

    private static final Map<ResourceLocation, CustomPacketPayload.Type<RawPayload>> TYPES = new HashMap<>();

    private static CustomPacketPayload.Type<RawPayload> register(ResourceLocation id, boolean toClient) {
        CustomPacketPayload.Type<RawPayload> type = new CustomPacketPayload.Type<>(id);
        StreamCodec<FriendlyByteBuf, RawPayload> codec = StreamCodec.of(
                (buf, payload) -> buf.writeBytes(payload.data()),
                buf -> {
                    byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    return new RawPayload(type, data);
                });
        if (toClient) PayloadTypeRegistry.playS2C().register(type, codec);
        else PayloadTypeRegistry.playC2S().register(type, codec);
        TYPES.put(id, type);
        return type;
    }

    public static CustomPacketPayload.Type<RawPayload> type(ResourceLocation id) {
        CustomPacketPayload.Type<RawPayload> type = TYPES.get(id);
        if (type == null) throw new IllegalStateException("Unregistered channel: " + id);
        return type;
    }

    public static RawPayload payload(ResourceLocation id, FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);
        return new RawPayload(type(id), data);
    }
    *///?}

    private Net() { }

    public static ResourceLocation s2c(String path) {
        ResourceLocation id = Ids.mod(path);
        //? if >=1.20.5
        /*register(id, true);*/
        return id;
    }

    public static ResourceLocation c2s(String path) {
        ResourceLocation id = Ids.mod(path);
        //? if >=1.20.5
        /*register(id, false);*/
        return id;
    }

    public static FriendlyByteBuf buf() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public static void receive(ResourceLocation channel, ServerHandler handler) {
        //? if >=1.20.5 {
        /*ServerPlayNetworking.registerGlobalReceiver(type(channel), (payload, context) ->
                handler.handle(server(context.player()), context.player(), payload.buf()));
        *///?} else {
        ServerPlayNetworking.registerGlobalReceiver(channel, (server, player, listener, buf, sender) ->
                handler.handle(server, player, buf));
        //?}
    }

    public static boolean canSend(ServerPlayer player, ResourceLocation channel) {
        return ServerPlayNetworking.canSend(player, channel);
    }

    public static void send(ServerPlayer player, ResourceLocation channel, FriendlyByteBuf buf) {
        //? if >=1.20.5 {
        /*ServerPlayNetworking.send(player, payload(channel, buf));
        *///?} else
        ServerPlayNetworking.send(player, channel, buf);
    }

    private static MinecraftServer server(ServerPlayer player) {
        return ServerCompat.server(player);
    }
}
