/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener;

import com.antd.railtransportplus.RailTransportPlus;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;

public class ClientEntityLoadListener implements ClientEntityEvents.Load {

    @Override
    public void onLoad(Entity entity, ClientWorld world) {
        if (entity instanceof AbstractMinecartEntity) {
            final var buf = PacketByteBufs.create();
            buf.writeUuid(entity.getUuid());
            ClientPlayNetworking.send(RailTransportPlus.CART_TYPE_PACKET_ID, buf);
        }
    }
}
