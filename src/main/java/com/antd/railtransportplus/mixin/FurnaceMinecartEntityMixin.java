package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.RailTransportPlus;
import com.antd.railtransportplus.mixininterface.LinkableCart;
import com.antd.railtransportplus.mixininterface.TrainEngineable;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FurnaceMinecartEntity.class)
public abstract class FurnaceMinecartEntityMixin extends AbstractMinecartEntity
        implements TrainEngineable {

    @Shadow @Final private static Ingredient ACCEPTABLE_FUEL;
    @Shadow protected abstract void setLit(boolean lit);

    private boolean boosted = false;

    protected FurnaceMinecartEntityMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

/* ------------------------------------------- Inject ------------------------------------------- */

    @Inject(at = @At("RETURN"), method = "tick()V")
    public void tick(CallbackInfo ci) {
        final var thisAccessor = (FurnaceMinecartEntityAccessor) this;

        if (!this.world.isClient()) {

            // try refueling from chest cart
            if (thisAccessor.getFuel() <= 0) {
                final var thisLinkableCart = (LinkableCart) this;

                // find chest cart immediately after furnace carts
                final var firstChestCart = thisLinkableCart.railtransportplus$getTrain().stream()
                        .filter((c) -> c instanceof StorageMinecartEntity)
                        .findFirst();

                if (firstChestCart.isPresent()
                        && ((LinkableCart) firstChestCart.get())
                        .railtransportplus$getNextCart() instanceof FurnaceMinecartEntity) {

                    // find first acceptable fuel
                    final var cartInventory =
                            ((ChestMinecartEntity) firstChestCart.get()).getInventory();

                    final var firstFuelStack = cartInventory.stream()
                            .filter((i) -> ACCEPTABLE_FUEL.test(i)).findFirst();

                    if (firstFuelStack.isPresent()) {

                        // refuel
                        firstFuelStack.get().decrement(1);
                        thisAccessor.setFuel(thisAccessor.getFuel() + 3600);

                        railtransportplus$updatePush();

                        this.setLit(true);
                    }
                }

                // haven't refueled
                if (thisAccessor.getFuel() <= 0) {
                    // update push to stop the train
                    railtransportplus$updatePush();
                }
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "getMaxSpeed()D", cancellable = true)
    public void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        final var boostMultiplier = RailTransportPlus.worldConfig.maxBoostedSpeed / 4.0;

        if (((LinkableCart) this).railtransportplus$getNextCart() != null)
            cir.setReturnValue(cir.getReturnValue() * boostMultiplier * 2.0);
        else if (this.boosted) {
            cir.setReturnValue(cir.getReturnValue() * boostMultiplier);
        }
    }

    @Inject(at = @At("RETURN"), method = "interact(Lnet/minecraft/entity/player/PlayerEntity;" +
            "Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;")
    public void interact(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (((FurnaceMinecartEntityAccessor) this).getFuel() > 0) railtransportplus$updatePush();
    }

/* -------------------------------------- Train Engineable -------------------------------------- */

    @Override
    public void railtransportplus$updatePush() {
        final var thisLinkableCart = (LinkableCart) this;
        final var thisFurnaceCart = (FurnaceMinecartEntity) (Object) this;
        final var nextCart = thisLinkableCart.railtransportplus$getNextCart();
        final var prevCart = thisLinkableCart.railtransportplus$getPrevCart();

        // front cart
        if (nextCart == null) {

            // has a trailing cart
            if (prevCart != null) {

                // all carts fueled
                if (((LinkableCart) this).railtransportplus$getTrain().stream()
                        .filter((c) -> c instanceof FurnaceMinecartEntity)
                        .allMatch((c) -> ((FurnaceMinecartEntityAccessor) c).getFuel() > 0)) {
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
                ((FurnaceMinecartEntityAccessor) this).setFuel(0);
            }
        }
        // trailing cart
        else if (nextCart != null) {
            thisFurnaceCart.pushX = 0;
            thisFurnaceCart.pushZ = 0;

            // update push of front furnace minecart
            ((TrainEngineable) thisLinkableCart.railtransportplus$getTrain().getFirst())
                    .railtransportplus$updatePush();
        }
    }
}
