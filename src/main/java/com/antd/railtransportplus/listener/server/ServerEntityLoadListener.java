/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener.server;

import com.antd.railtransportplus.LinkResult;
import com.antd.railtransportplus.interfaceinject.IRtpAbstractMinecartEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.antd.railtransportplus.RailTransportPlus.LOGGER;

public class ServerEntityLoadListener implements ServerEntityEvents.Load {

    /**
     * Key: id of cart that needs to load; Value: cart to link
     */
    private final Map<UUID, AbstractMinecartEntity> waitingForUnloadedCarts = new HashMap<>();

    @Override
    public void onLoad(Entity entity, ServerWorld world) {
        if (entity instanceof final AbstractMinecartEntity cart) {
            final var rtpCart = (IRtpAbstractMinecartEntity) entity;

            if (rtpCart.getOnLoadNextCart() != null) {
                final var loadedEntity = world.getEntity(rtpCart.getOnLoadNextCart());

                // link cart
                if (loadedEntity != null) {
                    final var result = ((IRtpAbstractMinecartEntity) loadedEntity).linkCart(cart, true);
                    if (result != LinkResult.SUCCESS) LOGGER.warn("Failed ot link cart on load: " + result);
                }
                // link cart when the cart to link to is loaded
                else waitingForUnloadedCarts.put(rtpCart.getOnLoadNextCart(), cart);
            }

            // link cart that was waiting for this one to load
            final var waitingCart = waitingForUnloadedCarts.remove(cart.getUuid());
            if (waitingCart != null) {
                final var result = rtpCart.linkCart(waitingCart, true);
                if (result != LinkResult.SUCCESS) LOGGER.warn("Failed ot link cart on load: " + result);
            }
        }
    }
}
