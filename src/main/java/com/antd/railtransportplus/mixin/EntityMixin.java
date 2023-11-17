/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.interfaceinject.RtpAbstractMinecartEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(at = @At("RETURN"), method = "remove(Lnet/minecraft/entity/Entity$RemovalReason;)V")
    public void remove(Entity.RemovalReason reason, CallbackInfo ci) {
        final var thisEntity = (Entity) (Object) this;

        if (!thisEntity.world.isClient() && this instanceof final RtpAbstractMinecartEntity thisCart) {
            thisCart.railtransportplus$unlinkBothCarts();
        }
    }

    @Inject(at = @At("HEAD"), method = "hasPassengers()Z", cancellable = true)
    public void hasPassengers(CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof final RtpAbstractMinecartEntity thisRtpCart) {
            if (thisRtpCart.railtransportplus$getIgnorePassenger()) {
                cir.setReturnValue(false);
            }
        }
    }
}
