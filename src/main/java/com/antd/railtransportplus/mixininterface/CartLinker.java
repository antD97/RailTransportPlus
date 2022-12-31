/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.mixininterface;

import com.antd.railtransportplus.ClientCartType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.Pair;

import java.util.List;
import java.util.UUID;

/** All methods expected to be called on the server thread. */
public interface CartLinker {

    /**
     * Marks a cart for linking or links two carts if a cart is already marked.
     *
     * @return true if two carts were linked
     */
    boolean railtransportplus$linkCart(AbstractMinecartEntity cart);
}
