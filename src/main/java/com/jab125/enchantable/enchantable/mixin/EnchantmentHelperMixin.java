package com.jab125.enchantable.enchantable.mixin;

import com.jab125.enchantable.enchantable.enchantment.ModEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.AbstractRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    private static Enchantment e;
    @SuppressWarnings("all")
    @ModifyVariable(method = "getPossibleEntries", at = @At(value = "STORE"))
    private static Enchantment enchantable$captureLocals(Enchantment enchantment) {
        e = enchantment;
        return enchantment;
    }

    @Redirect(method = "getPossibleEntries", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentTarget;isAcceptableItem(Lnet/minecraft/item/Item;)Z"))
    private static boolean enchantable$getPossibleEntries(EnchantmentTarget instance, Item item) {
        if (e instanceof ModEnchantment m) {
            return m.canApplyAtEnchantingTable(new ItemStack(item));
        }
        return e.type.isAcceptableItem(item);
    }
}
