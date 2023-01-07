/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener.server;

import com.antd.railtransportplus.LinkResult;
import com.antd.railtransportplus.interfaceinject.RtpAbstractMinecartEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import static com.antd.railtransportplus.RailTransportPlus.LOGGER;

public class ServerEntityLoadListener implements ServerEntityEvents.Load {

    /**
     * Key: id of cart that needs to load; Value: cart to link
     */
    private final Map<UUID, AbstractMinecartEntity> waitingForUnloadedCarts = new HashMap<>();

    /**
     * Key: cart to connect to link to; Value: cart to link
     */
    private final Map<AbstractMinecartEntity, AbstractMinecartEntity> waitingForCartLimitCarts = new HashMap<>();

    @Override
    public void onLoad(Entity entity, ServerWorld world) {

        if (entity instanceof final AbstractMinecartEntity cart) {
            final var rtpCart = (RtpAbstractMinecartEntity) entity;

            if (rtpCart.railtransportplus$getOnLoadNextCart() != null) {
                final var loadedEntity = world.getEntity(rtpCart.railtransportplus$getOnLoadNextCart());

                // link cart
                if (loadedEntity != null) {

                    final var result = ((RtpAbstractMinecartEntity) loadedEntity).railtransportplus$linkCart(cart);

                    if (result == LinkResult.CART_LIMIT) {
                        waitingForCartLimitCarts.put((AbstractMinecartEntity) loadedEntity, cart);
                    } else if (result == LinkResult.SUCCESS) {
                        checkCartLimitCarts(rtpCart.railtransportplus$getTrain());
                    } else {
                        LOGGER.warn("Failed ot link cart on load: " + result);
                    }
                }
                // link cart when the cart to link to is loaded
                else {
                    waitingForUnloadedCarts.put(rtpCart.railtransportplus$getOnLoadNextCart(), cart);
                }
            }

            // link cart that was waiting for this one to load
            final var waitingCart = waitingForUnloadedCarts.remove(cart.getUuid());
            if (waitingCart != null) {

                final var result = rtpCart.railtransportplus$linkCart(waitingCart);

                if (result == LinkResult.CART_LIMIT) {
                    waitingForCartLimitCarts.put(waitingCart, cart);
                } else if (result == LinkResult.SUCCESS) {
                    checkCartLimitCarts(rtpCart.railtransportplus$getTrain());
                } else {
                    LOGGER.info("Failed to link waiting cart on load: " + result);
                }
            }
        }
    }

    private void checkCartLimitCarts(LinkedList<AbstractMinecartEntity> train) {
        if (waitingForCartLimitCarts.containsKey(train.getLast())) {

            final var cartToLink = waitingForCartLimitCarts.get(train.getLast());

            final var result = ((RtpAbstractMinecartEntity) train.getLast()).railtransportplus$linkCart(cartToLink);

            if (result == LinkResult.SUCCESS) {
                waitingForCartLimitCarts.remove(train.getLast());
                checkCartLimitCarts(((RtpAbstractMinecartEntity) cartToLink).railtransportplus$getTrain());
            }
        }
    }
}
