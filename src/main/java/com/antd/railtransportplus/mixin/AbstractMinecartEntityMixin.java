package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.LinkResult;
import com.antd.railtransportplus.RailTransportPlus;
import com.antd.railtransportplus.mixininterface.LinkableCart;
import com.antd.railtransportplus.mixininterface.PoweredRailIgnorable;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RailBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedList;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends Entity implements LinkableCart {

    private AbstractMinecartEntity nextCart = null;
    private AbstractMinecartEntity prevCart = null;
    private LinkedList<AbstractMinecartEntity> train = new LinkedList<>();

    private boolean isTicked = false;

    public AbstractMinecartEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

/* ------------------------------------------- Inject ------------------------------------------- */

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/entity/EntityType;" +
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
    public void tickReturn(CallbackInfo ci) {

        if (!this.world.isClient()) {
            isTicked = true;

            // if all carts were ticked
            if (train.stream().allMatch(c -> ((LinkableCart) c).railtransportplus$isTicked())) {
                // all trailing carts...
                for (var cart : train) {
                    final var linkableCart = (LinkableCart) cart;
                    final var nextCart = linkableCart.railtransportplus$getNextCart();
                    if (nextCart != null) {

                        final var originalVelocity = cart.getVelocity();

                        if (cart.getPos().distanceTo(nextCart.getPos()) > 1.67) {

                            // calculate cart pull velocity
                            final var vectorToCart = nextCart.getPos().subtract(cart.getPos());

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
                                    ((AbstractMinecartEntityInvoker) cart).invokeGetMaxSpeed(),
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

                                ((AbstractMinecartEntityInvoker) cart).invokeMoveOnRail(blockPos,
                                        railState);
                            } else {
                                ((AbstractMinecartEntityInvoker) cart).invokeMoveOffRail();
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
        if (nextCart != null) ((PoweredRailIgnorable) state).setIgnorePoweredRail(true);
    }

    @Inject(at = @At("RETURN"), method = "moveOnRail(Lnet/minecraft/util/math/BlockPos;" +
            "Lnet/minecraft/block/BlockState;)V")
    public void moveOnRailReturn(BlockPos pos, BlockState state, CallbackInfo ci) {
        ((PoweredRailIgnorable) state).setIgnorePoweredRail(false);
    }

/* ---------------------------------------- Linkable Cart --------------------------------------- */

    @Override
    public LinkResult railtransportplus$linkCart(AbstractMinecartEntity otherCart) {

        // check self
        if (otherCart == (Object) this) return LinkResult.SAME_CART;

        // check removed
        if (this.isRemoved() || otherCart.isRemoved()) return LinkResult.CART_REMOVED;

        // check already has previous cart
        if (this.prevCart != null) return LinkResult.HAS_PREV_CART;

        // linked cart front check
        if (((LinkableCart) otherCart).railtransportplus$getNextCart() != null)
            return LinkResult.LINKED_CART_NOT_FRONT;

        final var otherCartTrain = ((LinkableCart) otherCart).railtransportplus$getTrain();

        // check already linked
        if (this.train.contains(otherCart) || otherCartTrain.contains(this)) return LinkResult.SAME_TRAIN;

        var furnaceCartCount = this.train.stream()
                .filter((cart) -> cart instanceof FurnaceMinecartEntity).count();
        furnaceCartCount += otherCartTrain.stream()
                .filter((cart) -> cart instanceof FurnaceMinecartEntity).count();

        if (otherCart instanceof FurnaceMinecartEntity) {
            // furnace cart at front only check
            if (!((Object) this instanceof FurnaceMinecartEntity)) return LinkResult.FURNACE_HEAD_ONLY;
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
        this.prevCart = otherCart;
        ((LinkableCart) otherCart).railtransportplus$setNextCart(
                (AbstractMinecartEntity) (Object) this
        );

        // update train
        this.train = railtransportplus$createTrain();
        for (var cart : this.train) ((LinkableCart) cart).railtransportplus$setTrain(this.train);

        return LinkResult.SUCCESS;
    }

    @Override
    public void railtransportplus$unlinkCarts() {

        // unlink next cart
        if (this.nextCart != null) {
            final var tempCart = this.nextCart;
            final var tempLinkableCart = (LinkableCart) tempCart;

            this.nextCart = null;
            tempLinkableCart.railtransportplus$setPrevCart(null);

            // update the unlinked cart's train
            final var updatedTrain = tempLinkableCart.railtransportplus$createTrain();
            for (var cart : updatedTrain) {
                ((LinkableCart) cart).railtransportplus$setTrain(updatedTrain);
            }

            // carts per furnace cart check
            final var furnaceCartCount = updatedTrain.stream()
                    .filter((cart) -> cart instanceof FurnaceMinecartEntity).count();
            final var cartLimit = furnaceCartCount > 0 ?
                    RailTransportPlus.worldConfig.maxCartsPerFurnaceCart * furnaceCartCount
                    : RailTransportPlus.worldConfig.maxCartsPerFurnaceCart + 1;

            if (updatedTrain.size() - furnaceCartCount > cartLimit) {
                for (var cart : updatedTrain) ((LinkableCart) cart).railtransportplus$unlinkCarts();
            }
        }

        // unlink previous cart
        if (this.prevCart != null) {
            final var tempCart = this.prevCart;
            final var tempLinkableCart = (LinkableCart) tempCart;

            this.prevCart = null;
            tempLinkableCart.railtransportplus$setNextCart(null);

            // update the unlinked cart's train
            final var updatedTrain = tempLinkableCart.railtransportplus$createTrain();
            for (var cart : updatedTrain) {
                ((LinkableCart) cart).railtransportplus$setTrain(updatedTrain);
            }

            // carts per furnace cart check
            final var furnaceCartCount = updatedTrain.stream()
                    .filter((cart) -> cart instanceof FurnaceMinecartEntity).count();
            final var cartLimit = furnaceCartCount > 0 ?
                    RailTransportPlus.worldConfig.maxCartsPerFurnaceCart * furnaceCartCount
                    : RailTransportPlus.worldConfig.maxCartsPerFurnaceCart + 1;

            if (updatedTrain.size() - furnaceCartCount > cartLimit) {
                for (var cart : updatedTrain) ((LinkableCart) cart).railtransportplus$unlinkCarts();
            }
        }

        // update this train
        this.train = railtransportplus$createTrain();
        for (var cart : this.train) ((LinkableCart) cart).railtransportplus$setTrain(this.train);
    }

    @Override
    public AbstractMinecartEntity railtransportplus$getNextCart() {
        return this.nextCart;
    }

    @Override
    public void railtransportplus$setNextCart(AbstractMinecartEntity nextCart) {
        this.nextCart = nextCart;
    }

    @Override
    public AbstractMinecartEntity railtransportplus$getPrevCart() {
        return this.prevCart;
    }

    @Override
    public void railtransportplus$setPrevCart(AbstractMinecartEntity prevCart) {
        this.prevCart = prevCart;
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
        var prevCart = frontCart;
        while (((LinkableCart) prevCart).railtransportplus$getPrevCart() != null) {
            prevCart = ((LinkableCart) prevCart).railtransportplus$getPrevCart();
            train.add(prevCart);
        }

        return train;
    }
}
