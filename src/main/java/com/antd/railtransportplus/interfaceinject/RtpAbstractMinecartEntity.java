/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.interfaceinject;

import com.antd.railtransportplus.CartVisualState;
import com.antd.railtransportplus.LinkResult;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;

import java.util.LinkedList;
import java.util.UUID;

public interface RtpAbstractMinecartEntity {

    /**
     * Links two carts with this as head and the other cart as the tail.
     */
    LinkResult railtransportplus$linkCart(AbstractMinecartEntity otherCart);

    /**
     * Unlink a specified cart.
     */
    void railtransportplus$unlinkCart(AbstractMinecartEntity cart);

    /**
     * Unlinks both next and previous carts.
     */
    default void railtransportplus$unlinkBothCarts() {
        final var nextCart = railtransportplus$getNextCart();
        final var prevCart = railtransportplus$getPrevCart();
        if (nextCart != null) railtransportplus$unlinkCart(nextCart);
        if (prevCart != null) railtransportplus$unlinkCart(prevCart);
    }

    AbstractMinecartEntity railtransportplus$getNextCart();

    void railtransportplus$setNextCart(AbstractMinecartEntity nextCart);

    AbstractMinecartEntity railtransportplus$getPrevCart();

    void railtransportplus$setPrevCart(AbstractMinecartEntity prevCart);

    /**
     * Sets the train cart list for this cart.
     */
    LinkedList<AbstractMinecartEntity> railtransportplus$getTrain();

    /**
     * Sets the train cart list for this linked cart.
     */
    void railtransportplus$setTrain(LinkedList<AbstractMinecartEntity> train);

    /**
     * Checks whether this cart has completed its Entity.tick() method.
     */
    boolean railtransportplus$isTicked();

    /**
     * Sets this carts tick status back to false.
     */
    void railtransportplus$resetTicked();

    /**
     * Creates an updated train list with all the linked carts.
     */
    LinkedList<AbstractMinecartEntity> railtransportplus$createTrain();

    /**
     * Gets the UUID of the next cart to link once this cart loads.
     */
    UUID railtransportplus$getOnLoadNextCart();

    /**
     * Resets the UUID of the on-load next cart to null.
     */
    void railtransportplus$resetOnLoadNextCart();

    /**
     * Gets the UUID of the previous cart to link once this cart loads.
     */
    UUID railtransportplus$getOnLoadPrevCart();

    /**
     * Resets the UUID of the on-load previous cart to null.
     */
    void railtransportplus$resetOnLoadPrevCart();

    CartVisualState railtransportplus$getCartVisualState();

    void railtransportplus$setCartVisualState(CartVisualState type);
}