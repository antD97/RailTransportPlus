/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.interfaceinject;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;

public interface IRtpPlayerEntity {

    /**
     * Marks a cart for linking or links two carts if a cart is already marked (server side only).
     *
     * @return true if the two carts were linked
     */
    boolean railtransportplus$linkCart(AbstractMinecartEntity cart);
}
