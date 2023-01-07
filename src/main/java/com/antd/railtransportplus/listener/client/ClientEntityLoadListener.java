/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;

import static com.antd.railtransportplus.RailTransportPlus.CART_VISUAL_STATE_PACKET_ID;

public class ClientEntityLoadListener implements ClientEntityEvents.Load {

    @Override
    public void onLoad(Entity entity, ClientWorld world) {

        // request cart visual state from server on client entity load
        if (entity instanceof AbstractMinecartEntity) {
            final var buf = PacketByteBufs.create();
            buf.writeUuid(entity.getUuid());
            ClientPlayNetworking.send(CART_VISUAL_STATE_PACKET_ID, buf);
        }
    }
}
