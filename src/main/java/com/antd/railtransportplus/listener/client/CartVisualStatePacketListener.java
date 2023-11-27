/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener.client;

import com.antd.railtransportplus.CartVisualState;
import com.antd.railtransportplus.interfaceinject.IRtpAbstractMinecartEntity;
import com.antd.railtransportplus.mixin.ClientWorldAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

public class CartVisualStatePacketListener implements ClientPlayNetworking.PlayChannelHandler {

    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {

        // update cart visual states
        final var cartUuid = buf.readUuid();
        final var cartType = CartVisualState.values()[buf.readByte()];

        if (client.world != null) {
            final var cart = (IRtpAbstractMinecartEntity) ((ClientWorldAccessor) client.world).invokeGetEntityLookup().get(cartUuid);
            if (cart != null) cart.setCartVisualState(cartType);
        }
    }
}
