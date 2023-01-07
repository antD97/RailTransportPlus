/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.interfaceinject;

public interface RtpAbstractBlockState {

    /**
     * If this.getBlock() is a Blocks.POWERED_RAIL, return false to this.isOf(Blocks.POWERED_RAIL) when
     * ignorePoweredRail is true.
     */
    void railtransportplus$setIgnorePoweredRail(boolean ignorePoweredRail);
}
