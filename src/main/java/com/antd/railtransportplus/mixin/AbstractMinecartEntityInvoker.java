package com.antd.railtransportplus.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractMinecartEntity.class)
public interface AbstractMinecartEntityInvoker {

    @Invoker("getMaxSpeed")
    double invokeGetMaxSpeed();

    @Invoker("moveOffRail")
    void invokeMoveOffRail();

    @Invoker("moveOnRail")
    void invokeMoveOnRail(BlockPos pos, BlockState state);
}
