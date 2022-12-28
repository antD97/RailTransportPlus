package com.antd.railtransportplus.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StorageMinecartEntity.class)
public class StorageMinecartEntityMixin {

    @Inject(at = @At("HEAD"), method = "interact(Lnet/minecraft/entity/player/PlayerEntity;" +
            "Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", cancellable = true)
    public void interact(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player.isSneaking() && (player.getStackInHand(Hand.MAIN_HAND).isOf(Items.CHAIN)
                || player.getStackInHand(Hand.OFF_HAND).isOf(Items.CHAIN))) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
