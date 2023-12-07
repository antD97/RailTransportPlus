/*
 * Copyright © 2021-2023 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.interfaceinject.IRtpStorageMinecartEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StorageMinecartEntity.class)
public abstract class StorageMinecartEntityMixin implements IRtpStorageMinecartEntity {

    private boolean skipNextOpen = false;

    @Override
    public void skipNextOpen() {
        skipNextOpen = true;
    }

    @Override
    public boolean getSkipNextOpen() {
        return skipNextOpen;
    }

    @Override
    public void resetSkipNextOpen() {
        skipNextOpen = false;
    }
}
