/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.interfaceinject.RtpAbstractMinecartEntity;
import com.antd.railtransportplus.interfaceinject.RtpFurnaceMinecartEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.antd.railtransportplus.RailTransportPlus.worldConfig;

@Mixin(FurnaceMinecartEntity.class)
public abstract class FurnaceMinecartEntityMixin extends AbstractMinecartEntity
        implements RtpFurnaceMinecartEntity {

    @Shadow @Final private static Ingredient ACCEPTABLE_FUEL;
    @Shadow protected abstract void setLit(boolean lit);

    private double boostAmount = 0.0;
    private boolean isReceivingBoost = false;

    protected FurnaceMinecartEntityMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

/* ------------------------------------------------ Invoker/Accessor ------------------------------------------------ */

    @Accessor("fuel")
    public abstract int getFuel();

    @Accessor("fuel")
    public abstract void setFuel(int fuel);

/* ----------------------------------------------------- Inject ----------------------------------------------------- */

    @Inject(at = @At("RETURN"), method = "tick()V")
    public void tick(CallbackInfo ci) {

        if (!this.world.isClient()) {

            // try refueling from chest cart
            if (this.getFuel() <= 0) {
                final var thisRtpCart = (RtpAbstractMinecartEntity) this;

                // find chest cart immediately after furnace carts
                final var firstChestCart = thisRtpCart.railtransportplus$getTrain().stream()
                        .filter((c) -> c instanceof StorageMinecartEntity)
                        .findFirst();

                if (firstChestCart.isPresent()
                        && ((RtpAbstractMinecartEntity) firstChestCart.get())
                        .railtransportplus$getNextCart() instanceof FurnaceMinecartEntity) {

                    // find first acceptable fuel
                    final var cartInventory =
                            ((ChestMinecartEntity) firstChestCart.get()).getInventory();

                    final var firstFuelStack = cartInventory.stream()
                            .filter((i) -> ACCEPTABLE_FUEL.test(i)).findFirst();

                    if (firstFuelStack.isPresent()) {

                        // refuel
                        firstFuelStack.get().decrement(1);
                        this.setFuel(this.getFuel() + 3600);

                        railtransportplus$updatePush();

                        this.setLit(true);
                    }
                }

                // haven't refueled
                if (this.getFuel() <= 0) {
                    // update push to stop the train
                    railtransportplus$updatePush();
                }
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "getMaxSpeed()D", cancellable = true)
    public void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        final var defaultMaxSpeed = cir.getReturnValue();
        // mpt (meters per tick)
        final var boostMpt = worldConfig.maxBoostedSpeed / 20.0;

        if (((RtpAbstractMinecartEntity) this).railtransportplus$getNextCart() != null)
            cir.setReturnValue(boostMpt * 2.0);
        else  {
            cir.setReturnValue(defaultMaxSpeed + (boostMpt - defaultMaxSpeed) * boostAmount);
        }
    }

    @Inject(at = @At("RETURN"), method = "interact(Lnet/minecraft/entity/player/PlayerEntity;" +
            "Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;")
    public void interact(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (this.getFuel() > 0) railtransportplus$updatePush();
    }

/* ----------------------------------------------- Interface Injection ---------------------------------------------- */

    @Override
    public void railtransportplus$updatePush() {
        final var thisRtpCart = (RtpAbstractMinecartEntity) this;
        final var thisFurnaceCart = (FurnaceMinecartEntity) (Object) this;
        final var nextCart = thisRtpCart.railtransportplus$getNextCart();
        final var prevCart = thisRtpCart.railtransportplus$getPrevCart();

        // front cart
        if (nextCart == null) {

            // has a trailing cart
            if (prevCart != null) {

                // all carts fueled
                if (((RtpAbstractMinecartEntity) this).railtransportplus$getTrain().stream()
                        .filter((c) -> c instanceof FurnaceMinecartEntity)
                        .allMatch((c) -> ((FurnaceMinecartEntityMixin) c).getFuel() > 0)) {
                    thisFurnaceCart.pushX = this.getX() - prevCart.getX();
                    thisFurnaceCart.pushZ = this.getZ() - prevCart.getZ();
                } else {
                    thisFurnaceCart.pushX = 0;
                    thisFurnaceCart.pushZ = 0;
                }
            }
            // has no current push & no previous cart to update push from
            else if (thisFurnaceCart.pushX == 0 & thisFurnaceCart.pushZ == 0) {
                // unfuel
                this.setFuel(0);
            }
        }
        // trailing cart
        else {
            thisFurnaceCart.pushX = 0;
            thisFurnaceCart.pushZ = 0;

            // update push of front furnace minecart
            ((RtpFurnaceMinecartEntity) thisRtpCart.railtransportplus$getTrain().getFirst())
                    .railtransportplus$updatePush();
        }
    }

    @Override
    public boolean railtransportplus$isReceivingBoost() {
        return isReceivingBoost;
    }
}
