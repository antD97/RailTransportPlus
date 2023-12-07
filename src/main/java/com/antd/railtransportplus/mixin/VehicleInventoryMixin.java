package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.interfaceinject.IRtpStorageMinecartEntity;
import com.antd.railtransportplus.interfaceinject.IRtpVehicleInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleInventory.class)
public interface VehicleInventoryMixin extends IRtpVehicleInventory {

    @Inject(at = @At("HEAD"), method = "open", cancellable = true)
    default void open(PlayerEntity player, CallbackInfoReturnable<ActionResult> cir) {
        if (!player.world.isClient) {
            final var rtpStorageCart = ((IRtpStorageMinecartEntity) this);

            if (rtpStorageCart.getSkipNextOpen()) {
                rtpStorageCart.resetSkipNextOpen();
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }

    @Override
    default void skipNextOpen() {
        if (this instanceof StorageMinecartEntity) {
            final var rtpStorageCart = ((IRtpStorageMinecartEntity) this);
            rtpStorageCart.skipNextOpen();
        }
    }
}
