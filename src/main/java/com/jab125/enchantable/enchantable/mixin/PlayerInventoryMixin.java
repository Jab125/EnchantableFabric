package com.jab125.enchantable.enchantable.mixin;

import com.jab125.enchantable.enchantable.enchantment.ModEnchantments;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.jab125.enchantable.enchantable.enchantment.ExcavatorEnchantment.onPlayerMineSpeed;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void enchantable$getBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        if (!EnchantmentHelper.get(((PlayerInventory)(Object)this).player.getMainHandStack()).containsKey(ModEnchantments.EXCAVATOR)) return;
        var l = onPlayerMineSpeed(((PlayerInventory)(Object)this).player, block, cir.getReturnValue());
        if (cir.getReturnValue().floatValue() != l) cir.setReturnValue(l);
    }
}
