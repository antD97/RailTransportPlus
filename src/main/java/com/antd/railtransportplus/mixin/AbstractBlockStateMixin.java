package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.mixininterface.PoweredRailIgnorable;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.block.AbstractBlock$AbstractBlockState")
public class AbstractBlockStateMixin implements PoweredRailIgnorable {

    private boolean ignorePoweredRail = false;

/* ------------------------------------------- Inject ------------------------------------------- */

    @Inject(at = @At("RETURN"), method = "isOf(Lnet/minecraft/block/Block;)Z", cancellable = true)
    public void isOf(Block block, CallbackInfoReturnable<Boolean> cir) {
        if (ignorePoweredRail
                && ((AbstractBlock.AbstractBlockState) (Object) this).getBlock() == Blocks.POWERED_RAIL) {
            cir.setReturnValue(false);
        }
    }

/* ----------------------------------- Powered Rail Ignorable ----------------------------------- */

    @Override
    public void setIgnorePoweredRail(boolean ignorePoweredRail) {
        this.ignorePoweredRail = ignorePoweredRail;
    }
}
