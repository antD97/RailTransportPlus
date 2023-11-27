/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.interfaceinject;

public interface IRtpFurnaceMinecartEntity {

    /**
     * Updates pushX & pushZ values according to cart links.
     */
    void railtransportplus$updatePush();

    double railtransportplus$getBoostAmount();

    void railtransportplus$setBoostAmount(double boostAmount);
}
