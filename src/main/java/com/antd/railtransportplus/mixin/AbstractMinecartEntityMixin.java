/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.ClientCartType;
import com.antd.railtransportplus.LinkResult;
import com.antd.railtransportplus.RailTransportPlus;
import com.antd.railtransportplus.mixininterface.LinkableCart;
import com.antd.railtransportplus.mixininterface.PoweredRailIgnorable;
import com.antd.railtransportplus.mixininterface.TrainEngineable;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RailBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedList;
import java.util.UUID;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends Entity implements LinkableCart {

    private static final int MAX_LINK_DISTANCE = 8;

    private AbstractMinecartEntity nextCart = null;
    private AbstractMinecartEntity prevCart = null;
    private LinkedList<AbstractMinecartEntity> train = new LinkedList<>();

    private boolean isTicked = false;

    private UUID loadNextCart = null;
    private UUID loadPrevCart = null;

    private ClientCartType clientCartType = ClientCartType.REGULAR;

    public AbstractMinecartEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

/* -------------------------------------- Invoker/Accessor -------------------------------------- */

    @Invoker("getMaxSpeed")
    public abstract double invokeGetMaxSpeed();

    @Invoker("moveOffRail")
    public abstract void invokeMoveOffRail();

    @Invoker("moveOnRail")
    public abstract void invokeMoveOnRail(BlockPos pos, BlockState state);

/* ------------------------------------------- Inject ------------------------------------------- */

    @Inject(at = @At("RETURN"), method = "<init>(Lnet/minecraft/entity/EntityType;" +
        "Lnet/minecraft/world/World;)V")
    public void constructor(CallbackInfo ci) {
    train.add((AbstractMinecartEntity) (Object) this);
    }

    @Inject(at = @At("RETURN"), method = "getMaxSpeed()D", cancellable = true)
    public void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        final var boostMultiplier = RailTransportPlus.worldConfig.maxBoostedSpeed / 8.0;

        if (nextCart != null) cir.setReturnValue(cir.getReturnValue() * boostMultiplier * 2.0);
    }

    @Inject(at = @At("RETURN"), method = "tick()V")
    public void tick(CallbackInfo ci) {

        if (!this.world.isClient()) {
            isTicked = true;

            // if all carts were ticked
            if (train.stream().allMatch(c -> ((LinkableCart) c).railtransportplus$isTicked())) {
                // all trailing carts...
                for (var cart : train) {
                    final var linkableCart = (LinkableCart) cart;
                    final var next = linkableCart.railtransportplus$getNextCart();
                    if (next != null) {

                        final var originalVelocity = cart.getVelocity();
                        final var distanceToCart = cart.getPos().distanceTo(next.getPos());

                        // too far, unlink
                        if (distanceToCart > MAX_LINK_DISTANCE) {
                            linkableCart.railtransportplus$unlinkCart(next);
                        }
                        // move towards next cart
                        else if (distanceToCart > 1.67) {

                            // calculate cart pull velocity
                            final var vectorToCart = next.getPos().subtract(cart.getPos());

                            // limit how close the cart can move to the target
                            var speedLimit = Math.max(
                                    vectorToCart.horizontalLength() - 1.67,
                                    0
                            );
                            // diagonal speed limit
                            if (vectorToCart.x != 0 && vectorToCart.z != 0) {
                                speedLimit = Math.sqrt((speedLimit * speedLimit) / 2); // pythagoras
                            }

                            final var horizontalSpeed = Math.min(
                                    ((AbstractMinecartEntityMixin) (Object) cart).invokeGetMaxSpeed(),
                                    speedLimit
                            );

                            double xVel = 0;
                            if (vectorToCart.x > 0) xVel = horizontalSpeed;
                            else if (vectorToCart.x < 0) xVel = -horizontalSpeed;

                            double zVel = 0;
                            if (vectorToCart.z > 0) zVel = horizontalSpeed;
                            else if (vectorToCart.z < 0) zVel = -horizontalSpeed;

                            // apply cart pull velocity
                            cart.setVelocity(new Vec3d(xVel, 0, zVel));

                            int i = MathHelper.floor(cart.getX());
                            int j = MathHelper.floor(cart.getY());
                            int k = MathHelper.floor(cart.getZ());
                            if (cart.world.getBlockState(new BlockPos(i, j - 1, k)).isIn(BlockTags.RAILS)) {
                                --j;
                            }

                            BlockPos blockPos = new BlockPos(i, j, k);
                            BlockState blockState = cart.world.getBlockState(blockPos);
                            if (AbstractRailBlock.isRail(blockState)) {
                                final var block = (AbstractRailBlock) blockState.getBlock();

                                final var railBlock = (RailBlock) Blocks.RAIL;

                                final var railState =
                                        railBlock.getDefaultState().with(railBlock.getShapeProperty(),
                                                blockState.get(block.getShapeProperty()));

                                ((AbstractMinecartEntityMixin) (Object) cart).invokeMoveOnRail(blockPos,
                                        railState);
                            } else {
                                ((AbstractMinecartEntityMixin) (Object) cart).invokeMoveOffRail();
                            }
                        }

                        // reset ticked & restore original velocity
                        linkableCart.railtransportplus$resetTicked();
                        cart.setVelocity(originalVelocity);
                    }
                }
            }

            // debug log
            if (((AbstractMinecartEntity) (Object) this).getScoreboardTags().contains("debug")) {
                RailTransportPlus.LOGGER.info("--------------------");
                if (((LinkableCart)(Object)this).railtransportplus$getNextCart() != null) {
                    RailTransportPlus.LOGGER.info(((AbstractMinecartEntity) (Object) this).getPos().distanceTo(nextCart.getPos()) + "");
                }
//                for (var cart : this.train) {
////                    RailTransportPlus.LOGGER.info(cart.toString());
//
//                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "moveOnRail(Lnet/minecraft/util/math/BlockPos;" +
            "Lnet/minecraft/block/BlockState;)V")
    public void moveOnRailHead(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (nextCart != null) ((PoweredRailIgnorable) state).railtransportplus$setIgnorePoweredRail(true);
    }

    @Inject(at = @At("RETURN"), method = "moveOnRail(Lnet/minecraft/util/math/BlockPos;" +
            "Lnet/minecraft/block/BlockState;)V")
    public void moveOnRailReturn(BlockPos pos, BlockState state, CallbackInfo ci) {
        ((PoweredRailIgnorable) state).railtransportplus$setIgnorePoweredRail(false);
    }

    @Inject(at = @At("RETURN"), method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V")
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nextCart != null) nbt.putUuid("nextCart", nextCart.getUuid());
        if (prevCart != null) nbt.putUuid("prevCart", prevCart.getUuid());
    }

    @Inject(at = @At("RETURN"), method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V")
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        final var thisCart = (AbstractMinecartEntity) (Object) this;
        final var thisLinkableCart = (LinkableCart) this;
        final var world = (ServerWorld) thisCart.world;

        if (nbt.containsUuid("nextCart")) {
            final var loadedUuid = nbt.getUuid("nextCart");
            final var loadedCart = world.getEntity(loadedUuid);

            if (loadedCart != null) { // not sure if this is possible
                ((LinkableCart) loadedCart).railtransportplus$linkCart(thisCart);
            } else {
                // load with entity on load listener
                loadNextCart = loadedUuid;
            }
        }

        if (nbt.containsUuid("prevCart")) {
            final var loadedUuid = nbt.getUuid("prevCart");
            final var loadedCart = world.getEntity(loadedUuid);

            if (loadedCart != null) { // not sure if this is possible
                thisLinkableCart.railtransportplus$linkCart((AbstractMinecartEntity) loadedCart);
            } else {
                // load with entity on load listener
                loadPrevCart = loadedUuid;
            }
        }
    }

/* ---------------------------------------- Linkable Cart --------------------------------------- */

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
        if (((LinkableCart) otherCart).railtransportplus$getNextCart() != null)
            return LinkResult.LINKED_CART_NOT_FRONT;

        final var otherCartTrain = ((LinkableCart) otherCart).railtransportplus$getTrain();

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
                return LinkResult.FURNACE_HEAD_ONLY;
            }
            else {
                // furnace cart count check
                if (furnaceCartCount > RailTransportPlus.worldConfig.maxFurnaceCartsPerTrain) {
                    return LinkResult.FURNACE_CART_LIMIT;
                }
            }
        } else {
            // carts per furnace cart check
            final var cartLimit = furnaceCartCount > 0 ?
                    RailTransportPlus.worldConfig.maxCartsPerFurnaceCart * furnaceCartCount
                    : RailTransportPlus.worldConfig.maxCartsPerFurnaceCart + 1;

            if (this.train.size() + otherCartTrain.size() - furnaceCartCount > cartLimit) {
                return LinkResult.CART_LIMIT;
            }
        }

        // link carts
        railtransportplus$setPrevCart(otherCart);
        ((LinkableCart) otherCart).railtransportplus$setNextCart(
                (AbstractMinecartEntity) (Object) this
        );

        // update train
        this.train = railtransportplus$createTrain();
        for (var cart : this.train) ((LinkableCart) cart).railtransportplus$setTrain(this.train);

        // update front furnace cart push
        if (this.train.getFirst() instanceof FurnaceMinecartEntity) {
            ((TrainEngineable) this.train.getFirst()).railtransportplus$updatePush();
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
            ((LinkableCart) unlinkedCart).railtransportplus$setPrevCart(null);

            ((AbstractMinecartEntity) (Object) this)
                    .playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);
        }
        else if (this.prevCart == cart) {
            // unlink
            unlinkedCart = this.prevCart;
            railtransportplus$setPrevCart(null);
            ((LinkableCart) unlinkedCart).railtransportplus$setNextCart(null);

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
        railtransportplus$updateClientCartType();
    }

    @Override
    public AbstractMinecartEntity railtransportplus$getPrevCart() {
        return this.prevCart;
    }

    @Override
    public void railtransportplus$setPrevCart(AbstractMinecartEntity prevCart) {
        this.prevCart = prevCart;
        railtransportplus$updateClientCartType();
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

    /** Creates an updated train list with all the linked carts. */
    @Override
    public LinkedList<AbstractMinecartEntity> railtransportplus$createTrain() {
        final var train = new LinkedList<AbstractMinecartEntity>();

        // find front cart
        var frontCart = (AbstractMinecartEntity) (Object) this;
        while (((LinkableCart)frontCart).railtransportplus$getNextCart() != null) {
            frontCart = ((LinkableCart) frontCart).railtransportplus$getNextCart();
        }

        train.add(frontCart);

        // add previous carts to train
        var prev = frontCart;
        while (((LinkableCart) prev).railtransportplus$getPrevCart() != null) {
            prev = ((LinkableCart) prev).railtransportplus$getPrevCart();
            train.add(prev);
        }

        return train;
    }

    @Override
    public UUID railtransportplus$getLoadNextCart() {
        return loadNextCart;
    }

    @Override
    public void railtransportplus$resetLoadNextCart() {
        loadNextCart = null;
    }

    @Override
    public UUID railtransportplus$getLoadPrevCart() {
        return loadPrevCart;
    }

    @Override
    public void railtransportplus$resetLoadPrevCart() {
        loadPrevCart = null;
    }

    @Override
    public ClientCartType railtransportplus$clientGetCartType() {
        return clientCartType;
    }

    @Override
    public void railtransportplus$clientSetCartType(ClientCartType type) {
        this.clientCartType = type;
    }

/* ------------------------------------------- Helpers ------------------------------------------ */

    /**
     * Rebuilds cart's train,
     * updates all carts in that train with that train,
     * updates the push state of the leading furnace cart,
     * and does a carts per furnace cart check.
     */
    private void railtransportplus$updateCartTrain(AbstractMinecartEntity cart) {
        final var linkableCart = (LinkableCart) cart;

        // update train
        final var updatedTrain = linkableCart.railtransportplus$createTrain();
        for (var c : updatedTrain) {
            ((LinkableCart) c).railtransportplus$setTrain(updatedTrain);
        }

        // update front furnace cart push
        if (updatedTrain.getFirst() instanceof FurnaceMinecartEntity) {
            ((TrainEngineable) updatedTrain.getFirst()).railtransportplus$updatePush();
        }

        // carts per furnace cart check
        final var furnaceCartCount = updatedTrain.stream()
                .filter((c) -> c instanceof FurnaceMinecartEntity).count();
        final var cartLimit = furnaceCartCount > 0 ?
                RailTransportPlus.worldConfig.maxCartsPerFurnaceCart * furnaceCartCount
                : RailTransportPlus.worldConfig.maxFurnaceCartsPerTrain + 1;

        if (updatedTrain.size() - furnaceCartCount > cartLimit) {
            for (var c : updatedTrain) ((LinkableCart) c).railtransportplus$unlinkBothCarts();
        }
    }

    private void railtransportplus$updateClientCartType() {
        final var oldClientCartType = clientCartType;

        // trailing
        if (nextCart != null) {
            this.clientCartType = ClientCartType.TRAILING;

            // check boosted
            final var frontCart = this.train.getFirst();
            if (frontCart instanceof FurnaceMinecartEntity) {
                if (((TrainEngineable) frontCart).railtransportplus$isReceivingBoost()) {
                    this.clientCartType = ClientCartType.TRAILING_BOOST;
                }
            }
        }
        // front cart
        else {
            var isReceivingBoost = false;
            if ((Object) this instanceof FurnaceMinecartEntity) {
                if (((TrainEngineable) this).railtransportplus$isReceivingBoost()) {
                    isReceivingBoost = true;
                }
            }

            final var hasTail = this.prevCart != null;

            if (hasTail && isReceivingBoost) this.clientCartType = ClientCartType.FRONT_BOOST;
            else if (hasTail && !isReceivingBoost) this.clientCartType = ClientCartType.FRONT;
            else if (!hasTail && isReceivingBoost) {
                this.clientCartType = ClientCartType.FRONT_TAILLESS_BOOST;
            } else this.clientCartType = ClientCartType.REGULAR;
        }

        // send update to clients
        if (oldClientCartType != this.clientCartType) {
            for (var player : ((ServerWorld) this.world).getPlayers()) {

                final var buf = PacketByteBufs.create();
                buf.writeUuid(this.getUuid());
                buf.writeByte(this.clientCartType.ordinal());

                ServerPlayNetworking.send(player, RailTransportPlus.CART_TYPE_PACKET_ID, buf);
            }
        }
    }
}
