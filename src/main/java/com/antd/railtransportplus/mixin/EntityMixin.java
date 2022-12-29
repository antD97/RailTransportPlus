/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.mixininterface.LinkableCart;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(at = @At("RETURN"), method = "remove(Lnet/minecraft/entity/Entity$RemovalReason;)V")
    public void remove(Entity.RemovalReason reason, CallbackInfo ci) {
        final var thisEntity = (Entity) (Object) this;

        if (!thisEntity.world.isClient() && this instanceof final LinkableCart thisCart) {

            // unlink carts
            thisCart.railtransportplus$unlinkBothCarts();
        }
    }
}
