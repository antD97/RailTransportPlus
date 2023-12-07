/*
 * Copyright © 2021-2023 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.CartVisualState;
import com.antd.railtransportplus.LinkResult;
import com.antd.railtransportplus.RailTransportPlus;
import com.antd.railtransportplus.interfaceinject.IRtpAbstractMinecartEntity;
import com.antd.railtransportplus.interfaceinject.IRtpFurnaceMinecartEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.*;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKeys;
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
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedList;
import java.util.UUID;

import static com.antd.railtransportplus.RailTransportPlus.CART_VISUAL_STATE_PACKET_ID;
import static com.antd.railtransportplus.RailTransportPlus.worldConfig;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends Entity implements IRtpAbstractMinecartEntity {

    private static final int MAX_LINK_DISTANCE = 5;

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
            if (train.stream().allMatch(c -> ((IRtpAbstractMinecartEntity) c).isTicked())) {

                final var ridingEntities = train.stream()
                        .filter(Entity::hasPassengers)
                        .map(Entity::getFirstPassenger)
                        .toList();

                for (var cart : train) {
                    final var rtpCart = (IRtpAbstractMinecartEntity) cart;
                    final var next = rtpCart.getNextCart();

                    ((AbstractMinecartEntityMixin) (Object) cart).resetSkipMove();

                    // head cart
                    if (next == null) ((AbstractMinecartEntityMixin) (Object) cart).move(false);

                    // trailing carts...
                    if (next != null) {

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

                        // prevent overshoot on corners (moving past the following cart on either axis)
                        final double cartX = cart.getPos().x;
                        final double nextX = next.getPos().x;
                        if ((cartX < nextX && cartX + xVel > nextX)
                                || (cartX > nextX && cartX + xVel < nextX)) {
                            xVel = nextX - cartX;
                        }
                        final double cartZ = cart.getPos().z;
                        final double nextZ = next.getPos().z;
                        if ((cartZ < nextZ && cartZ + zVel > nextZ)
                                || (cartZ > nextZ && cartZ + zVel < nextZ)) {
                            zVel = nextZ - cartZ;
                        }

                        // apply cart pull velocity
                        cart.setVelocity(new Vec3d(xVel, cart.getVelocity().y, zVel));

                        ((AbstractMinecartEntityMixin) (Object) cart).move(true);

                        rtpCart.resetTicked();

                        // too far, unlink
                        if (cart.getPos().distanceTo(next.getPos()) > MAX_LINK_DISTANCE) {
                            rtpCart.unlinkCart(next);
                        }
                    }

                    // velocity in m/s
                    final var vel = ((AbstractMinecartEntityMixin) (Object) train.getFirst()).getMaxSpeed() * 20;

                    // damage colliding entities
                    if (vel > 10) {

                        final var collidingEntities = cart.world
                                .getOtherEntities(
                                        this,
                                        cart.getBoundingBox().expand(0.05).stretch(this.getVelocity()),
                                        EntityPredicates.VALID_LIVING_ENTITY)
                                .stream().filter(e -> !ridingEntities.contains(e))
                                .map(e -> (LivingEntity) e)
                                .toList();

                        // 1 damage at 10 m/s, 20 damage at 60 m/s
                        final var damage = 0.38f * vel + 2.8f;

                        for (final var e : collidingEntities) {
                            e.takeKnockback(vel * 0.1, cart.getX() - e.getX(), cart.getZ() - e.getZ());
                            e.damage(new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(RailTransportPlus.TRAIN_DAMAGE)), (float) damage);
                        }
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
            final var thisRtpFurnaceCart = (IRtpFurnaceMinecartEntity) this;

            thisRtpFurnaceCart.setBoostAmount(Math.max(
                    thisRtpFurnaceCart.getBoostAmount() - (1.0 / (20.0 * worldConfig.unpoweredRailTimeToNoBoost)), 0));

            // update train visual states
            final var thisRtpCart = (IRtpAbstractMinecartEntity) this;

            final var oldVisualState = thisRtpCart.getVisualState();
            thisRtpCart.updateVisualState();

            // if visual state changed, update the entire train
            if (oldVisualState != thisRtpCart.getVisualState()) {
                for (final var cart : ((IRtpAbstractMinecartEntity) this).getTrain()) {
                    ((IRtpAbstractMinecartEntity) cart).updateVisualState();
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
                ((IRtpAbstractMinecartEntity) loadedCart).linkCart(thisCart, false);
            } else {
                // load with entity on load listener
                onLoadNextCart = loadedUuid;
            }
        }
    }

    /**
     * Overrides the speed cap in the [moveOnRail] method.
     * Selector must be checked whenever the Minecraft version changes.
     */
    @ModifyConstant(method = "moveOnRail", constant = @Constant(doubleValue = 2.0, ordinal = 0))
    private double modifyMinVelConst(double value) {
        return Math.max(value, this.getMaxSpeed());
    }

/* ----------------------------------------------- Interface Injection ---------------------------------------------- */

    @Override
    public LinkResult linkCart(AbstractMinecartEntity otherCart, boolean force) {

        if (!force) {
            // check self
            if (otherCart == (Object) this) return LinkResult.SAME_CART;

            // check removed
            if (this.isRemoved() || otherCart.isRemoved()) return LinkResult.CART_REMOVED;

            // check too far
            if (this.getPos().distanceTo(otherCart.getPos()) > MAX_LINK_DISTANCE) return LinkResult.TOO_FAR;

            // check already has previous cart
            if (this.prevCart != null) return LinkResult.HAS_PREV_CART;

            // linked cart front check
            if (((IRtpAbstractMinecartEntity) otherCart).getNextCart() != null) return LinkResult.LINKED_CART_NOT_FRONT;

            final var otherCartTrain = ((IRtpAbstractMinecartEntity) otherCart).getTrain();

            // check already linked
            if (this.train.contains(otherCart) || otherCartTrain.contains(this)) return LinkResult.SAME_TRAIN;

            var furnaceCartCount = this.train.stream().filter((cart) -> cart instanceof FurnaceMinecartEntity).count();
            furnaceCartCount += otherCartTrain.stream().filter((cart) -> cart instanceof FurnaceMinecartEntity).count();

            if (otherCart instanceof FurnaceMinecartEntity) {
                // furnace cart at front only check
                if (!((Object) this instanceof FurnaceMinecartEntity)) return LinkResult.FURNACE_FRONT_ONLY;
                // furnace cart count check
                else if (furnaceCartCount > worldConfig.maxFurnaceCartsPerTrain) return LinkResult.FURNACE_CART_LIMIT;
            } else {
                // carts per furnace cart check
                final var cartLimit = furnaceCartCount > 0 ?
                        worldConfig.maxCartsPerFurnaceCart * furnaceCartCount
                        : worldConfig.maxCartsPerFurnaceCart + 1;

                if (this.train.size() + otherCartTrain.size() - furnaceCartCount > cartLimit) {
                    return LinkResult.CART_LIMIT;
                }
            }
        }

        // link carts
        setPrevCart(otherCart);
        ((IRtpAbstractMinecartEntity) otherCart).setNextCart((AbstractMinecartEntity) (Object) this);

        // update train
        this.train = createTrain();
        for (var cart : this.train) ((IRtpAbstractMinecartEntity) cart).setTrain(this.train);

        // update front furnace cart push
        if (this.train.getFirst() instanceof FurnaceMinecartEntity) {
            ((IRtpFurnaceMinecartEntity) this.train.getFirst()).updatePush();
        }

        return LinkResult.SUCCESS;
    }

    @Override
    public void unlinkCart(AbstractMinecartEntity cart) {

        final AbstractMinecartEntity unlinkedCart;
        if (this.nextCart == cart) {
            // unlink
            unlinkedCart = this.nextCart;
            setNextCart(null);
            ((IRtpAbstractMinecartEntity) unlinkedCart).setPrevCart(null);

            // item drop
            if (this.world.getGameRules().get(GameRules.DO_ENTITY_DROPS).get()) this.dropItem(Items.CHAIN);

            // sound
            ((AbstractMinecartEntity) (Object) this).playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);

        } else if (this.prevCart == cart) {
            // unlink
            unlinkedCart = this.prevCart;
            setPrevCart(null);
            ((IRtpAbstractMinecartEntity) unlinkedCart).setNextCart(null);

            // item drop
            if (this.world.getGameRules().get(GameRules.DO_ENTITY_DROPS).get()) this.dropItem(Items.CHAIN);

            // sound
            ((AbstractMinecartEntity) (Object) this).playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);
        }
        else return;

        updateCartTrain((AbstractMinecartEntity) (Object) this);
        updateCartTrain(unlinkedCart);
    }

    @Override
    public AbstractMinecartEntity getNextCart() {
        return this.nextCart;
    }

    @Override
    public void setNextCart(AbstractMinecartEntity nextCart) {
        this.nextCart = nextCart;
        updateVisualState();
    }

    @Override
    public AbstractMinecartEntity getPrevCart() {
        return this.prevCart;
    }

    @Override
    public void setPrevCart(AbstractMinecartEntity prevCart) {
        this.prevCart = prevCart;
        updateVisualState();
    }

    @Override
    public LinkedList<AbstractMinecartEntity> getTrain() {
        return this.train;
    }

    @Override
    public void setTrain(LinkedList<AbstractMinecartEntity> train) {
        this.train = train;
    }

    @Override
    public boolean isTicked() {
        return this.isTicked;
    }

    @Override
    public void resetTicked() {
        this.isTicked = false;
    }

    @Override
    public void resetSkipMove() {
        this.skipMove = false;
    }

    /** Creates an updated train list with all the linked carts. */
    @Override
    public LinkedList<AbstractMinecartEntity> createTrain() {
        final var train = new LinkedList<AbstractMinecartEntity>();

        // find front cart
        var frontCart = (AbstractMinecartEntity) (Object) this;
        while (((IRtpAbstractMinecartEntity)frontCart).getNextCart() != null) {
            frontCart = ((IRtpAbstractMinecartEntity) frontCart).getNextCart();
        }

        train.add(frontCart);

        // add previous carts to train
        var prev = frontCart;
        while (((IRtpAbstractMinecartEntity) prev).getPrevCart() != null) {
            prev = ((IRtpAbstractMinecartEntity) prev).getPrevCart();
            train.add(prev);
        }

        return train;
    }

    @Override
    public UUID getOnLoadNextCart() {
        return onLoadNextCart;
    }

    @Override
    public void resetOnLoadNextCart() {
        onLoadNextCart = null;
    }

    @Override
    public CartVisualState getVisualState() {
        return visualState;
    }

    @Override
    public void setCartVisualState(CartVisualState visualState) {
        this.visualState = visualState;
    }

    @Override
    public void updateVisualState() {
        final var oldVisualState = visualState;

        // trailing
        if (nextCart != null) {
            this.visualState = CartVisualState.TRAILING;

            // check boosted
            final var frontCart = this.train.getFirst();
            if (frontCart instanceof FurnaceMinecartEntity) {
                if (((IRtpFurnaceMinecartEntity) frontCart).getBoostAmount() > 0) {
                    this.visualState = CartVisualState.TRAILING_BOOST;
                }
            }
        }
        // front cart
        else {
            var isBoosted = false;
            if ((Object) this instanceof FurnaceMinecartEntity
                    && ((IRtpFurnaceMinecartEntity) this).getBoostAmount() > 0) {
                isBoosted = true;
            }

            final var hasTail = this.prevCart != null;

            if (hasTail && isBoosted) this.visualState = CartVisualState.FRONT_BOOST;
            else if (hasTail && !isBoosted) this.visualState = CartVisualState.FRONT;
            else if (!hasTail && isBoosted) this.visualState = CartVisualState.FRONT_TAILLESS_BOOST;
            else this.visualState = CartVisualState.REGULAR;
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
    public void move(boolean isTrailing) {
        var i = MathHelper.floor(this.getX());
        var j = MathHelper.floor(this.getY());
        var k = MathHelper.floor(this.getZ());
        if (this.world.getBlockState(new BlockPos(i, j - 1, k)).isIn(BlockTags.RAILS)) --j;

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
    public boolean getIgnorePassenger() {
        return ignorePassenger;
    }

/* ----------------------------------------------------- Helpers ---------------------------------------------------- */

    /**
     * Rebuilds cart's train,
     * updates all carts in that train with that train,
     * updates the push state of the leading furnace cart,
     * and does a carts per furnace cart check.
     */
    private void updateCartTrain(AbstractMinecartEntity cart) {
        final var rtpCart = (IRtpAbstractMinecartEntity) cart;

        // update train
        final var updatedTrain = rtpCart.createTrain();
        for (var c : updatedTrain) ((IRtpAbstractMinecartEntity) c).setTrain(updatedTrain);

        // update front furnace cart push
        if (updatedTrain.getFirst() instanceof FurnaceMinecartEntity) {
            ((IRtpFurnaceMinecartEntity) updatedTrain.getFirst()).updatePush();
        }

        // carts per furnace cart check
        final var furnaceCartCount = updatedTrain.stream()
                .filter((c) -> c instanceof FurnaceMinecartEntity).count();
        final var cartLimit = furnaceCartCount > 0 ?
                worldConfig.maxCartsPerFurnaceCart * furnaceCartCount
                : worldConfig.maxFurnaceCartsPerTrain + 1;

        if (updatedTrain.size() - furnaceCartCount > cartLimit) {
            for (var c : updatedTrain) ((IRtpAbstractMinecartEntity) c).unlinkBothCarts();
        }
    }
}
