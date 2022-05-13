package com.jab125.enchantable.enchantable.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ModEnchantment extends Enchantment {
    private final Acceptable acceptable;
    protected ModEnchantment(Rarity weight, Acceptable acceptable, EquipmentSlot[] slotTypes) {
        super(weight, EnchantmentTarget.BREAKABLE, slotTypes);
        this.acceptable = acceptable;
    }

    protected ModEnchantment(Rarity weight, EnchantmentTarget target, EquipmentSlot[] slotTypes) {
        super(weight, target, slotTypes);
        this.acceptable = target::isAcceptableItem;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return canApplyAtEnchantingTable(stack);
    }

    public static interface Acceptable {
        public abstract boolean isAcceptableItem(Item stack);
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return this.acceptable.isAcceptableItem(stack.getItem());
    }

    public final boolean isAllowedOnBooks() {
        return true;
    }
}
