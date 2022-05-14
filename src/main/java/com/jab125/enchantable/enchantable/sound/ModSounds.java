package com.jab125.enchantable.enchantable.sound;

import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModSounds {
    public static final SoundEvent STOMP = Registry.register(Registry.SOUND_EVENT, new Identifier("enchantable", "stomp"), new SoundEvent(new Identifier("enchantable", "entity.player.stomp")));

    public static void registerSounds() {}
}
