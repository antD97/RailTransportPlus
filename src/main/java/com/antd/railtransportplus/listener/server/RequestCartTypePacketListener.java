/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener.server;

import com.antd.railtransportplus.interfaceinject.IRtpAbstractMinecartEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.antd.railtransportplus.RailTransportPlus.CART_VISUAL_STATE_PACKET_ID;

public class RequestCartTypePacketListener implements ServerPlayNetworking.PlayChannelHandler {

    @Override
    public void receive(
            MinecraftServer server,
            ServerPlayerEntity player,
            ServerPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender responseSender
    ) {

        // respond to request for cart visual state
        final var cart = (AbstractMinecartEntity) player.getWorld().getEntity(buf.readUuid());
        final var rtpCart = (IRtpAbstractMinecartEntity) cart;

        final var resBuf = PacketByteBufs.create();
        resBuf.writeUuid(cart.getUuid());
        resBuf.writeByte(rtpCart.railtransportplus$getVisualState().ordinal());

        responseSender.sendPacket(CART_VISUAL_STATE_PACKET_ID, resBuf);
    }
}
