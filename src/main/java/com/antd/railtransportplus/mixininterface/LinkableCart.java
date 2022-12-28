/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.mixininterface;

import com.antd.railtransportplus.LinkResult;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;

import java.util.LinkedList;

/** All methods expected to be called on the server thread. */
public interface LinkableCart {

    /** Links two carts with this as head and the other cart as the tail. */
    LinkResult railtransportplus$linkCart(AbstractMinecartEntity otherCart);

    /** Unlinks all carts. */
    void railtransportplus$unlinkCarts();

    AbstractMinecartEntity railtransportplus$getNextCart();

    void railtransportplus$setNextCart(AbstractMinecartEntity nextCart);

    AbstractMinecartEntity railtransportplus$getPrevCart();

    void railtransportplus$setPrevCart(AbstractMinecartEntity prevCart);

    /** Sets the train cart list for this cart. */
    LinkedList<AbstractMinecartEntity> railtransportplus$getTrain();

    /** Sets the train cart list for this linked cart. */
    void railtransportplus$setTrain(LinkedList<AbstractMinecartEntity> train);

    /** Checks whether this cart has completed its Entity.tick() method. */
    boolean railtransportplus$isTicked();

    /** Sets this carts tick status back to false. */
    void railtransportplus$resetTicked();

    /** Creates an updated train list with all the linked carts. */
    LinkedList<AbstractMinecartEntity> railtransportplus$createTrain();
}
