package com.antd.railtransportplus.mixin;

import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FurnaceMinecartEntity.class)
public interface FurnaceMinecartEntityAccessor {

    @Accessor("fuel")
    int getFuel();

    @Accessor("fuel")
    void setFuel(int fuel);
}
