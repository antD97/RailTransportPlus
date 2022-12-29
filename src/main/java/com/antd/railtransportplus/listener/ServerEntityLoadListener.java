/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener;

import com.antd.railtransportplus.RailTransportPlus;
import com.antd.railtransportplus.mixininterface.LinkableCart;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.world.ServerWorld;

public class ServerEntityLoadListener implements ServerEntityEvents.Load {

    @Override
    public void onLoad(Entity entity, ServerWorld world) {

        if (entity instanceof final AbstractMinecartEntity cart) {
            final var linkableCart = (LinkableCart) entity;

            // link next cart
            if (linkableCart.getLoadNextCart() != null) {
                final var loadedCart = world.getEntity(linkableCart.getLoadNextCart());
                if (loadedCart != null) {
                    ((LinkableCart) loadedCart).railtransportplus$linkCart(cart);
                }
            }

            // link previous cart
            if (linkableCart.getLoadPrevCart() != null) {
                final var loadedCart = world.getEntity(linkableCart.getLoadPrevCart());
                if (loadedCart != null) {
                    linkableCart.railtransportplus$linkCart((AbstractMinecartEntity) loadedCart);
                }
            }
        }
    }
}
