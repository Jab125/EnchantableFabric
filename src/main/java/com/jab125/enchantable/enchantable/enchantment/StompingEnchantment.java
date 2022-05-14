package com.jab125.enchantable.enchantable.enchantment;

import com.jab125.enchantable.enchantable.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Map;

public class StompingEnchantment extends ModEnchantment {
    protected StompingEnchantment() {
        super(Enchantment.Rarity.UNCOMMON, EnchantmentTarget.ARMOR_FEET, new EquipmentSlot[]{EquipmentSlot.FEET});
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int getMinPower(int level) {
        return level * 10;
    }

    @Override
    public int getMaxPower(int level) {
        return this.getMinPower(level) + 15;
    }

    @Override
    protected boolean canAccept(Enchantment other) {
        if (other instanceof ProtectionEnchantment protectionEnchantment) {
            return protectionEnchantment.protectionType != ProtectionEnchantment.Type.FALL;
        }
        return super.canAccept(other);
    }

    public static float playerFallDamage(LivingEntity entity, DamageSource source, float amount) {
        if(source == DamageSource.FALL) {
            if(entity instanceof PlayerEntity player) {
                ItemStack stack = player.getEquippedStack(EquipmentSlot.FEET);
                if(!stack.isEmpty()) {
                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
                    if(enchantments.containsKey(ModEnchantments.STOMPING)) {
                        int level = enchantments.get(ModEnchantments.STOMPING);
                        float strengthFactor = 0.8F * (level / 4.0F);

                        /* Finds entities in a five block radius around the player */
                        List<LivingEntity> entities = player.world.getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class), player.getBoundingBox().expand(5, 0, 5), Entity::isAlive);
                        entities.remove(player); //Remove ourselves as it should apply stomping to the player causing it

                        if (entities.size() > 0) {
                            float fallDamage = amount;

                            /* Reduce the damage of the fall as it has redirected it to the stomped mobs */
                            amount = Math.max(0F, fallDamage - fallDamage * strengthFactor);

                            for (LivingEntity livingEntity : entities) {
                                /* If PVP is not enabled, prevent stomping from damaging players */
                                if (livingEntity instanceof PlayerEntity) {
                                    MinecraftServer server = livingEntity.getServer();
                                    if(!server.isPvpEnabled()) {
                                        continue;
                                    }
                                }

                                /* Spawns particles and plays a stomp sound at the location of the living entity */
                                if(livingEntity.world instanceof ServerWorld serverWorld)
                                {
                                    BlockState state = livingEntity.world.getBlockState(livingEntity.getBlockPos().down());
                                    serverWorld.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), 50, 0, 0, 0, 0.15F);
                                    serverWorld.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), ModSounds.STOMP, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                }

                                /* Cause the entity to bop up into the air */
                                double stompStrength = 0.3 * (level / 4.0);
                                Vec3d direction = new Vec3d(livingEntity.getX() - player.getX(), 0, livingEntity.getZ() - player.getZ()).normalize();
                                livingEntity.setVelocity(direction.x * stompStrength, stompStrength, direction.z * stompStrength);
                                livingEntity.addVelocity(direction.x * stompStrength, stompStrength, direction.z * stompStrength);
                                livingEntity.velocityModified = true;

                                /* Damage is applied last so mobs will still fly into air even when dead. It just looks better! */
                                float distance = livingEntity.distanceTo(player);
                                float distanceFactor = Math.max(0.5F, 1.0F - distance / 5.0F);
                                livingEntity.damage(DamageSource.GENERIC, fallDamage * strengthFactor * distanceFactor * 2.0F);
                                livingEntity.setAttacker(player);
                            }

                            /* Damages boots by the amount of mobs that were stomped */
                            stack.damage(entities.size(), player, entity1 -> entity1.sendEquipmentBreakStatus(EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, EquipmentSlot.FEET.getEntitySlotId())));
                        }
                    }
                }
            }
        }
        return amount;
    }
}
