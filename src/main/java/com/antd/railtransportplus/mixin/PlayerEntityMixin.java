/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.LinkResult;
import com.antd.railtransportplus.mixininterface.CartLinker;
import com.antd.railtransportplus.mixininterface.LinkableCart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements CartLinker {

    private AbstractMinecartEntity linkingCart = null;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

/* ------------------------------------------- Inject ------------------------------------------- */

    @Inject(at = @At("HEAD"), method = "tick()V")
    public void tick(CallbackInfo ci) {

        if (!this.world.isClient()) {
            final var thisPlayer = (ServerPlayerEntity) (Object) this;

            // linking cart message
            if (linkingCart != null) {
                if (thisPlayer.getStackInHand(Hand.MAIN_HAND).isOf(Items.CHAIN)
                        || thisPlayer.getStackInHand(Hand.OFF_HAND).isOf(Items.CHAIN)) {
                    thisPlayer.sendMessage(Text.of("Linking..."), true);
                } else {
                    linkingCart = null;
                    thisPlayer.sendMessage(Text.of(""), true);
                }
            }
        }
    }

/* ----------------------------------------- Cart Linker ---------------------------------------- */

    @Override
    public boolean railtransportplus$linkCart(AbstractMinecartEntity cart) {
        final var thisPlayer = (ServerPlayerEntity) (Object) this;

        var linked = false;

        if (linkingCart == null) linkingCart = cart;
        else {
            final var result = ((LinkableCart) linkingCart).railtransportplus$linkCart(cart);
            linkingCart = null;
            thisPlayer.sendMessage(Text.of(result.message), true);
            if (result == LinkResult.SUCCESS) linked = true;
        }

        return linked;
    }
}
