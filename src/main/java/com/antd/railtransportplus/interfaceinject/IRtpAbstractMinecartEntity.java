/*
 * Copyright © 2021-2023 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.interfaceinject;

import com.antd.railtransportplus.CartVisualState;
import com.antd.railtransportplus.LinkResult;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;

import java.util.LinkedList;
import java.util.UUID;

public interface IRtpAbstractMinecartEntity {

    /**
     * Links two carts with this as head and the other cart as the tail.
     */
    LinkResult linkCart(AbstractMinecartEntity otherCart, boolean force);

    /**
     * Unlink a specified cart.
     */
    void unlinkCart(AbstractMinecartEntity cart);

    /**
     * Unlinks both next and previous carts.
     */
    default void unlinkBothCarts() {
        final var nextCart = getNextCart();
        final var prevCart = getPrevCart();
        if (nextCart != null) unlinkCart(nextCart);
        if (prevCart != null) unlinkCart(prevCart);
    }

    AbstractMinecartEntity getNextCart();

    void setNextCart(AbstractMinecartEntity nextCart);

    AbstractMinecartEntity getPrevCart();

    void setPrevCart(AbstractMinecartEntity prevCart);

    /**
     * Sets the train cart list for this cart.
     */
    LinkedList<AbstractMinecartEntity> getTrain();

    /**
     * Sets the train cart list for this linked cart.
     */
    void setTrain(LinkedList<AbstractMinecartEntity> train);

    /**
     * Checks whether this cart has completed its Entity.tick() method.
     */
    boolean isTicked();

    /**
     * Sets this carts tick status back to false.
     */
    void resetTicked();

    /**
     * Sets this skip move status back to false.
     */
    void resetSkipMove();

    /**
     * Creates an updated train list with all the linked carts.
     */
    LinkedList<AbstractMinecartEntity> createTrain();

    /**
     * Gets the UUID of the next cart to link once this cart loads.
     */
    UUID getOnLoadNextCart();

    /**
     * Resets the UUID of the on-load next cart to null.
     */
    void resetOnLoadNextCart();

    /**
     * Gets the UUID of the previous cart to link once this cart loads.
     */
    UUID getOnLoadPrevCart();

    /**
     * Resets the UUID of the on-load previous cart to null.
     */
    void resetOnLoadPrevCart();

    CartVisualState getVisualState();

    void setCartVisualState(CartVisualState type);

    /**
     * Updates this cart's visual state and sends updates to clients if the state changed.
     */
    void updateVisualState();

    boolean getIgnorePassenger();

    void move(boolean isTrailing);
}
