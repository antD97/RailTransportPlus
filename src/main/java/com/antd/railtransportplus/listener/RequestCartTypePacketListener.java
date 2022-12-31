/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener;

import com.antd.railtransportplus.mixininterface.LinkableCart;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.antd.railtransportplus.RailTransportPlus.CART_TYPE_PACKET_ID;

public class RequestCartTypePacketListener implements ServerPlayNetworking.PlayChannelHandler {

    @Override
    public void receive(
            MinecraftServer server,
            ServerPlayerEntity player,
            ServerPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender responseSender
    ) {
        final var cart = (AbstractMinecartEntity) player.getWorld().getEntity(buf.readUuid());
        final var linkableCart = (LinkableCart) cart;

        final var resBuf = PacketByteBufs.create();
        resBuf.writeUuid(cart.getUuid());
        resBuf.writeByte(linkableCart.railtransportplus$clientGetCartType().ordinal());

        responseSender.sendPacket(CART_TYPE_PACKET_ID, resBuf);
    }
}
