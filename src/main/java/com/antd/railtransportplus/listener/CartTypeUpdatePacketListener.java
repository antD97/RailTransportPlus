/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener;

import com.antd.railtransportplus.ClientCartType;
import com.antd.railtransportplus.RailTransportPlus;
import com.antd.railtransportplus.mixin.ClientWorldAccessor;
import com.antd.railtransportplus.mixininterface.LinkableCart;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

public class CartTypeUpdatePacketListener implements ClientPlayNetworking.PlayChannelHandler {

    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        final var cartUuid = buf.readUuid();
        final var cartType = ClientCartType.values()[buf.readByte()];

        final var cart = (LinkableCart) ((ClientWorldAccessor) client.world)
                .invokeGetEntityLookup().get(cartUuid);

        if (cart != null) cart.railtransportplus$clientSetCartType(cartType);
        else RailTransportPlus.LOGGER.warn("Could not find cart.");
    }
}
