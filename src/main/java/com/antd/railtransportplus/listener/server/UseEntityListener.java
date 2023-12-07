/*
 * Copyright © 2021-2023 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.listener.server;

import com.antd.railtransportplus.interfaceinject.IRtpPlayerEntity;
import com.antd.railtransportplus.interfaceinject.IRtpStorageMinecartEntity;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class UseEntityListener implements UseEntityCallback {

    @Override
    public ActionResult interact(
            PlayerEntity player,
            World world,
            Hand hand,
            Entity entity,
            @Nullable EntityHitResult hitResult)
    {

        // use chain on cart
        if (!world.isClient()
                && hitResult != null
                && entity instanceof AbstractMinecartEntity
                && player.isSneaking()
                && player.getStackInHand(hand).isOf(Items.CHAIN)
                // ignore chain in offhand if there's chain in main hand
                && !(hand == Hand.OFF_HAND && player.getStackInHand(Hand.MAIN_HAND).isOf(Items.CHAIN))) {

            if (((IRtpPlayerEntity) player).linkCart((AbstractMinecartEntity) entity)) {
                entity.playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);
                if (!player.isCreative()) player.getStackInHand(hand).decrement(1);
            }

            if (entity instanceof StorageMinecartEntity) {
                ((IRtpStorageMinecartEntity) entity).skipNextOpen();
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
