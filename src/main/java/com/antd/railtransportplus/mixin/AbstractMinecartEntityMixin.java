/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.CartVisualState;
import com.antd.railtransportplus.LinkResult;
import com.antd.railtransportplus.interfaceinject.RtpAbstractMinecartEntity;
import com.antd.railtransportplus.interfaceinject.RtpFurnaceMinecartEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.*;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedList;
import java.util.UUID;

import static com.antd.railtransportplus.RailTransportPlus.*;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends Entity implements RtpAbstractMinecartEntity {

    private static final int MAX_LINK_DISTANCE = 8;

    private AbstractMinecartEntity nextCart = null;
    private AbstractMinecartEntity prevCart = null;
    private LinkedList<AbstractMinecartEntity> train = new LinkedList<>();

    private boolean isTicked = false;

    private UUID onLoadNextCart = null;

    private CartVisualState visualState = CartVisualState.REGULAR;

    private boolean ignorePassenger = false;

    private boolean skipMove = false;

    public AbstractMinecartEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

/* ------------------------------------------------ Invoker/Accessor ------------------------------------------------ */

    @Shadow public abstract void onActivatorRail(int x, int y, int z, boolean powered);

    @Shadow protected abstract void moveOnRail(BlockPos pos, BlockState state);

    @Shadow protected abstract double getMaxSpeed();

    @Shadow protected abstract void moveOffRail();

/* ----------------------------------------------------- Inject ----------------------------------------------------- */

    @Inject(at = @At("RETURN"), method = "<init>(Lnet/minecraft/entity/EntityType;" +
        "Lnet/minecraft/world/World;)V")
    public void constructor(CallbackInfo ci) {
        train.add((AbstractMinecartEntity) (Object) this);
    }

    @Inject(at = @At("RETURN"), method = "getMaxSpeed()D", cancellable = true)
    public void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        final var boostMpt = worldConfig.maxBoostedSpeed / 20.0;

        if (nextCart != null) cir.setReturnValue(boostMpt * 2.0); // x2 to let trailing cars catch up
    }

    @Inject(at = @At("HEAD"), method = "tick()V")
    public void tickHead(CallbackInfo ci) {
        if (!this.world.isClient()) {
            isTicked = false;
            if (prevCart != null || nextCart != null) skipMove = true;
        }
    }

    @Inject(at = @At("RETURN"), method = "tick()V")
    public void tickReturn(CallbackInfo ci) {

        if (!this.world.isClient() && (prevCart != null || nextCart != null)) {
            isTicked = true;

            // if all carts were ticked
            if (train.stream().allMatch(c -> ((RtpAbstractMinecartEntity) c).railtransportplus$isTicked())) {

                for (var cart : train) {
                    final var rtpCart = (RtpAbstractMinecartEntity) cart;
                    final var next = rtpCart.railtransportplus$getNextCart();

                    ((AbstractMinecartEntityMixin) (Object) cart).railtransportplus$resetSkipMove();

                    // head cart
                    if (next == null) {
                        ((AbstractMinecartEntityMixin) (Object) cart).railtransportplus$move(false);
                    }

                    // trailing carts...
                    if (next != null) {

                        final var distanceToCart = cart.getPos().distanceTo(next.getPos());

                        // too far, unlink
                        if (distanceToCart > MAX_LINK_DISTANCE) {
                            rtpCart.railtransportplus$unlinkCart(next);
                        }
                        // move towards next cart
                        else {

                            // calculate cart pull velocity
                            final var vectorToCart = next.getPos().subtract(cart.getPos());

                            // limit how close the cart can move to the target
                            var speedLimit = Math.max(vectorToCart.horizontalLength() - 1.67, 0);
                            // diagonal speed limit
                            if (vectorToCart.x != 0 && vectorToCart.z != 0) {
                                speedLimit = Math.sqrt((speedLimit * speedLimit) / 2); // pythagoras
                            }

                            final var horizontalSpeed = Math.min(
                                    ((AbstractMinecartEntityMixin) (Object) cart).getMaxSpeed(),
                                    speedLimit
                            );

                            double xVel = 0;
                            if (vectorToCart.x > 0) xVel = horizontalSpeed;
                            else if (vectorToCart.x < 0) xVel = -horizontalSpeed;

                            double zVel = 0;
                            if (vectorToCart.z > 0) zVel = horizontalSpeed;
                            else if (vectorToCart.z < 0) zVel = -horizontalSpeed;

                            // apply cart pull velocity
                            cart.setVelocity(new Vec3d(xVel, cart.getVelocity().y, zVel));

                            ((AbstractMinecartEntityMixin) (Object) cart).railtransportplus$move(true);
                        }

                        rtpCart.railtransportplus$resetTicked();
                    }
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "moveOnRail(Lnet/minecraft/util/math/BlockPos;" +
            "Lnet/minecraft/block/BlockState;)V", cancellable = true)
    public void moveOnRailHead(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (skipMove) ci.cancel();
        if (nextCart != null) ignorePassenger = true;
    }

    @Inject(at = @At("HEAD"), method = "moveOffRail()V", cancellable = true)
    public void moveOffRail(CallbackInfo ci) {
        if (skipMove) ci.cancel();
        ignorePassenger = false;

        if ((Object) this instanceof FurnaceMinecartEntity) {
            final var thisRtpFurnaceCart = (RtpFurnaceMinecartEntity) this;
            thisRtpFurnaceCart.railtransportplus$setBoostAmount(
                    Math.max(thisRtpFurnaceCart.railtransportplus$getBoostAmount() - 0.025, 0) // 1.0 -> 0.0 in 2s
            );

            // update train visual states
            final var thisRtpCart = (RtpAbstractMinecartEntity) this;

            final var oldVisualState = thisRtpCart.railtransportplus$getVisualState();
            thisRtpCart.railtransportplus$updateVisualState();

            // if visual state changed, update the entire train
            if (oldVisualState != thisRtpCart.railtransportplus$getVisualState()) {
                for (final var cart : ((RtpAbstractMinecartEntity) this).railtransportplus$getTrain()) {
                    ((RtpAbstractMinecartEntity) cart).railtransportplus$updateVisualState();
                }
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V")
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nextCart != null) nbt.putUuid("nextCart", nextCart.getUuid());
    }

    @Inject(at = @At("RETURN"), method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V")
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        final var thisCart = (AbstractMinecartEntity) (Object) this;
        final var world = (ServerWorld) thisCart.world;

        if (nbt.containsUuid("nextCart")) {
            final var loadedUuid = nbt.getUuid("nextCart");
            final var loadedCart = world.getEntity(loadedUuid);

            if (loadedCart != null) { // not sure if this is possible
                ((RtpAbstractMinecartEntity) loadedCart).railtransportplus$linkCart(thisCart);
            } else {
                // load with entity on load listener
                onLoadNextCart = loadedUuid;
            }
        }
    }

/* ----------------------------------------------- Interface Injection ---------------------------------------------- */

    @Override
    public LinkResult railtransportplus$linkCart(AbstractMinecartEntity otherCart) {

        // check self
        if (otherCart == (Object) this) return LinkResult.SAME_CART;

        // check removed
        if (this.isRemoved() || otherCart.isRemoved()) return LinkResult.CART_REMOVED;

        // check too far
        if (this.getPos().distanceTo(otherCart.getPos()) > MAX_LINK_DISTANCE) {
            return LinkResult.TOO_FAR;
        }

        // check already has previous cart
        if (this.prevCart != null) return LinkResult.HAS_PREV_CART;

        // linked cart front check
        if (((RtpAbstractMinecartEntity) otherCart).railtransportplus$getNextCart() != null)
            return LinkResult.LINKED_CART_NOT_FRONT;

        final var otherCartTrain = ((RtpAbstractMinecartEntity) otherCart).railtransportplus$getTrain();

        // check already linked
        if (this.train.contains(otherCart) || otherCartTrain.contains(this)) {
            return LinkResult.SAME_TRAIN;
        }

        var furnaceCartCount = this.train.stream()
                .filter((cart) -> cart instanceof FurnaceMinecartEntity).count();
        furnaceCartCount += otherCartTrain.stream()
                .filter((cart) -> cart instanceof FurnaceMinecartEntity).count();

        if (otherCart instanceof FurnaceMinecartEntity) {
            // furnace cart at front only check
            if (!((Object) this instanceof FurnaceMinecartEntity)) {
                return LinkResult.FURNACE_FRONT_ONLY;
            }
            else {
                // furnace cart count check
                if (furnaceCartCount > worldConfig.maxFurnaceCartsPerTrain) {
                    return LinkResult.FURNACE_CART_LIMIT;
                }
            }
        } else {
            // carts per furnace cart check
            final var cartLimit = furnaceCartCount > 0 ?
                    worldConfig.maxCartsPerFurnaceCart * furnaceCartCount
                    : worldConfig.maxCartsPerFurnaceCart + 1;

            if (this.train.size() + otherCartTrain.size() - furnaceCartCount > cartLimit) {
                return LinkResult.CART_LIMIT;
            }
        }

        // link carts
        railtransportplus$setPrevCart(otherCart);
        ((RtpAbstractMinecartEntity) otherCart).railtransportplus$setNextCart(
                (AbstractMinecartEntity) (Object) this
        );

        // update train
        this.train = railtransportplus$createTrain();
        for (var cart : this.train) ((RtpAbstractMinecartEntity) cart).railtransportplus$setTrain(this.train);

        // update front furnace cart push
        if (this.train.getFirst() instanceof FurnaceMinecartEntity) {
            ((RtpFurnaceMinecartEntity) this.train.getFirst()).railtransportplus$updatePush();
        }

        return LinkResult.SUCCESS;
    }

    @Override
    public void railtransportplus$unlinkCart(AbstractMinecartEntity cart) {

        final AbstractMinecartEntity unlinkedCart;
        if (this.nextCart == cart) {
            // unlink
            unlinkedCart = this.nextCart;
            railtransportplus$setNextCart(null);
            ((RtpAbstractMinecartEntity) unlinkedCart).railtransportplus$setPrevCart(null);

            // item drop
            if (this.world.getGameRules().get(GameRules.DO_ENTITY_DROPS).get()) {
                this.dropItem(Items.CHAIN);
            }

            // sound
            ((AbstractMinecartEntity) (Object) this)
                    .playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);

        } else if (this.prevCart == cart) {
            // unlink
            unlinkedCart = this.prevCart;
            railtransportplus$setPrevCart(null);
            ((RtpAbstractMinecartEntity) unlinkedCart).railtransportplus$setNextCart(null);

            // item drop
            if (this.world.getGameRules().get(GameRules.DO_ENTITY_DROPS).get()) {
                this.dropItem(Items.CHAIN);
            }

            // sound
            ((AbstractMinecartEntity) (Object) this)
                    .playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);
        }
        else return;

        railtransportplus$updateCartTrain((AbstractMinecartEntity) (Object) this);
        railtransportplus$updateCartTrain(unlinkedCart);
    }

    @Override
    public AbstractMinecartEntity railtransportplus$getNextCart() {
        return this.nextCart;
    }

    @Override
    public void railtransportplus$setNextCart(AbstractMinecartEntity nextCart) {
        this.nextCart = nextCart;
        railtransportplus$updateVisualState();
    }

    @Override
    public AbstractMinecartEntity railtransportplus$getPrevCart() {
        return this.prevCart;
    }

    @Override
    public void railtransportplus$setPrevCart(AbstractMinecartEntity prevCart) {
        this.prevCart = prevCart;
        railtransportplus$updateVisualState();
    }

    @Override
    public LinkedList<AbstractMinecartEntity> railtransportplus$getTrain() {
        return this.train;
    }

    @Override
    public void railtransportplus$setTrain(LinkedList<AbstractMinecartEntity> train) {
        this.train = train;
    }

    @Override
    public boolean railtransportplus$isTicked() {
        return this.isTicked;
    }

    @Override
    public void railtransportplus$resetTicked() {
        this.isTicked = false;
    }

    @Override
    public void railtransportplus$resetSkipMove() {
        this.skipMove = false;
    }

    /** Creates an updated train list with all the linked carts. */
    @Override
    public LinkedList<AbstractMinecartEntity> railtransportplus$createTrain() {
        final var train = new LinkedList<AbstractMinecartEntity>();

        // find front cart
        var frontCart = (AbstractMinecartEntity) (Object) this;
        while (((RtpAbstractMinecartEntity)frontCart).railtransportplus$getNextCart() != null) {
            frontCart = ((RtpAbstractMinecartEntity) frontCart).railtransportplus$getNextCart();
        }

        train.add(frontCart);

        // add previous carts to train
        var prev = frontCart;
        while (((RtpAbstractMinecartEntity) prev).railtransportplus$getPrevCart() != null) {
            prev = ((RtpAbstractMinecartEntity) prev).railtransportplus$getPrevCart();
            train.add(prev);
        }

        return train;
    }

    @Override
    public UUID railtransportplus$getOnLoadNextCart() {
        return onLoadNextCart;
    }

    @Override
    public void railtransportplus$resetOnLoadNextCart() {
        onLoadNextCart = null;
    }

    @Override
    public CartVisualState railtransportplus$getVisualState() {
        return visualState;
    }

    @Override
    public void railtransportplus$setCartVisualState(CartVisualState visualState) {
        this.visualState = visualState;
    }

    @Override
    public void railtransportplus$updateVisualState() {
        final var oldVisualState = visualState;

        // trailing
        if (nextCart != null) {
            this.visualState = CartVisualState.TRAILING;

            // check boosted
            final var frontCart = this.train.getFirst();
            if (frontCart instanceof FurnaceMinecartEntity) {
                if (((RtpFurnaceMinecartEntity) frontCart).railtransportplus$getBoostAmount() > 0) {
                    this.visualState = CartVisualState.TRAILING_BOOST;
                }
            }
        }
        // front cart
        else {
            var isBoosted = false;
            if ((Object) this instanceof FurnaceMinecartEntity) {
                if (((RtpFurnaceMinecartEntity) this).railtransportplus$getBoostAmount() > 0) {
                    isBoosted = true;
                }
            }

            final var hasTail = this.prevCart != null;

            if (hasTail && isBoosted) this.visualState = CartVisualState.FRONT_BOOST;
            else if (hasTail && !isBoosted) this.visualState = CartVisualState.FRONT;
            else if (!hasTail && isBoosted) {
                this.visualState = CartVisualState.FRONT_TAILLESS_BOOST;
            } else this.visualState = CartVisualState.REGULAR;
        }

        // send update to clients
        if (oldVisualState != this.visualState) {
            for (var player : ((ServerWorld) this.world).getPlayers()) {

                final var buf = PacketByteBufs.create();
                buf.writeUuid(this.getUuid());
                buf.writeByte(this.visualState.ordinal());

                ServerPlayNetworking.send(player, CART_VISUAL_STATE_PACKET_ID, buf);
            }
        }
    }

    @Override
    public void railtransportplus$move(boolean isTrailing) {
        var i = MathHelper.floor(this.getX());
        var j = MathHelper.floor(this.getY());
        var k = MathHelper.floor(this.getZ());
        if (this.world.getBlockState(new BlockPos(i, j - 1, k)).isIn(BlockTags.RAILS)) {
            --j;
        }

        final var blockPos = new BlockPos(i, j, k);
        final var blockState = this.world.getBlockState(blockPos);
        if (AbstractRailBlock.isRail(blockState)) {

            if (isTrailing) { // trailing carts ignore powered rails
                final var block = (AbstractRailBlock) blockState.getBlock();

                final var railBlock = (RailBlock) Blocks.RAIL;

                final var overrideBlockState = railBlock.getDefaultState()
                        .with(railBlock.getShapeProperty(), blockState.get(block.getShapeProperty()));

                // remove velocity from slope
                double g = 0.0078125;
                if (this.isTouchingWater()) g *= 0.2;
                RailShape railShape = blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty());
                switch (railShape) {
                    case ASCENDING_EAST -> this.setVelocity(this.getVelocity().subtract(-g, 0.0, 0.0));
                    case ASCENDING_WEST -> this.setVelocity(this.getVelocity().subtract(g, 0.0, 0.0));
                    case ASCENDING_NORTH -> this.setVelocity(this.getVelocity().subtract(0.0, 0.0, g));
                    case ASCENDING_SOUTH -> this.setVelocity(this.getVelocity().subtract(0.0, 0.0, -g));
                }

                this.moveOnRail(blockPos, overrideBlockState);

            } else { // head carts interact with powered rails like normal
                this.moveOnRail(blockPos, blockState);
            }

            if (blockState.isOf(Blocks.ACTIVATOR_RAIL)) {
                this.onActivatorRail(i, j, k, blockState.get(PoweredRailBlock.POWERED));
            }
        } else {
            // gravity
            this.setVelocity(this.getVelocity().add(0.0, this.isTouchingWater() ? -0.005 : -0.04, 0.0));
            this.moveOffRail();
        }
    }

    @Override
    public boolean railtransportplus$getIgnorePassenger() {
        return ignorePassenger;
    }

/* ----------------------------------------------------- Helpers ---------------------------------------------------- */

    /**
     * Rebuilds cart's train,
     * updates all carts in that train with that train,
     * updates the push state of the leading furnace cart,
     * and does a carts per furnace cart check.
     */
    private void railtransportplus$updateCartTrain(AbstractMinecartEntity cart) {
        final var rtpCart = (RtpAbstractMinecartEntity) cart;

        // update train
        final var updatedTrain = rtpCart.railtransportplus$createTrain();
        for (var c : updatedTrain) {
            ((RtpAbstractMinecartEntity) c).railtransportplus$setTrain(updatedTrain);
        }

        // update front furnace cart push
        if (updatedTrain.getFirst() instanceof FurnaceMinecartEntity) {
            ((RtpFurnaceMinecartEntity) updatedTrain.getFirst()).railtransportplus$updatePush();
        }

        // carts per furnace cart check
        final var furnaceCartCount = updatedTrain.stream()
                .filter((c) -> c instanceof FurnaceMinecartEntity).count();
        final var cartLimit = furnaceCartCount > 0 ?
                worldConfig.maxCartsPerFurnaceCart * furnaceCartCount
                : worldConfig.maxFurnaceCartsPerTrain + 1;

        if (updatedTrain.size() - furnaceCartCount > cartLimit) {
            for (var c : updatedTrain) {
                ((RtpAbstractMinecartEntity) c).railtransportplus$unlinkBothCarts();
            }
        }
    }
}
