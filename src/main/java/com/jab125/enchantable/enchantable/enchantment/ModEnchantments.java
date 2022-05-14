package com.jab125.enchantable.enchantable.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModEnchantments {
    public static final StompingEnchantment STOMPING = Registry.register(Registry.ENCHANTMENT, new Identifier("enchantable", "stomping"), new StompingEnchantment());
    public static final CultivatorEnchantment CULTIVATOR = Registry.register(Registry.ENCHANTMENT, new Identifier("enchantable", "cultivator"), new CultivatorEnchantment());
    public static final ExcavatorEnchantment EXCAVATOR = Registry.register(Registry.ENCHANTMENT, new Identifier("enchantable", "excavator"), new ExcavatorEnchantment());
    public static final Enchantment ORE_EATER = null;//Registry.register(Registry.ENCHANTMENT, new Identifier("enchantable", "ore_eater"), new OreEaterEnchantment());

    public static void registerEnchantments() {}
}
