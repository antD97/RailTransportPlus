/*
 * Copyright © 2021-2023 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus;

import com.antd.railtransportplus.listener.client.CartVisualStatePacketListener;
import com.antd.railtransportplus.listener.client.ClientEntityLoadListener;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import static com.antd.railtransportplus.RailTransportPlus.CART_VISUAL_STATE_PACKET_ID;

/** Client entrypoint. */
public class RailTransportPlusClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(CART_VISUAL_STATE_PACKET_ID, new CartVisualStatePacketListener());
        ClientEntityEvents.ENTITY_LOAD.register(new ClientEntityLoadListener());
    }
}
