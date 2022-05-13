package com.jab125.enchantable.enchantable.enchantment;

import com.jab125.enchantable.enchantable.Enchantable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Map;

// TODO: Hoes
// TODO: Cultivate crops
public class CultivatorEnchantment extends ModEnchantment {
    protected CultivatorEnchantment() {
        super(Rarity.VERY_RARE, Enchantable.TILLABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return stack.getItem() instanceof HoeItem || stack.getItem() instanceof ShovelItem;
    }

    @Override
    public int getMinPower(int level) {
        return 15;
    }

    @Override
    public int getMaxPower(int level) {
        return super.getMinPower(level) + 50;
    }

    public static boolean onRightClickBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        if (!(player.getMainHandStack().getItem() instanceof ShovelItem))
            return false;

        int affectedBlocks = till(player.world, hitResult.getBlockPos(), hitResult.getSide(), player.getMainHandStack(), player, ShovelItem.PATH_STATES);
        if (affectedBlocks > 0) {
            player.getMainHandStack().damage(affectedBlocks, player, player2 -> player.sendToolBreakStatus(hand));
            player.swingHand(hand);
            Enchantable.sendTimeToDamageTill(affectedBlocks, hand);
            return true;
        }
        return false;
    }

    private static int till(World world, BlockPos pos, Direction face, ItemStack stack, PlayerEntity player, Map<Block, BlockState> replacementMap) {
        if (stack.isEmpty())
            return 0;

        if (!EnchantmentHelper.get(stack).containsKey(ModEnchantments.CULTIVATOR))
            return 0;

        pos = pos.add(-1, 0, -1);
        if (face != Direction.DOWN) {
            int maxBlocks = stack.getMaxDamage() - stack.getDamage();
            int affectedBlocks = 0;
            for(int i = 0; i < 9 && i < maxBlocks; i++)
            {
                BlockPos groundPos = pos.add(i / 3, 0, i % 3);
                boolean air = world.isAir(groundPos.up());
                boolean replaceable = world.getBlockState(groundPos.up()).getMaterial().isReplaceable();
                if(air || replaceable)
                {
                    BlockState groundState = replacementMap.get(world.getBlockState(groundPos).getBlock());
                    if(groundState != null)
                    {
                        world.setBlockState(groundPos, groundState, 11);
                        affectedBlocks++;
                        if(!air) {
                            world.setBlockState(groundPos.up(), Blocks.AIR.getDefaultState());
                        }
                        Enchantable.sendBlockTilledResult(groundPos);
                    }
                }
            }
            if(affectedBlocks > 0)
            {
                world.playSound(player, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return affectedBlocks;
            }
        }
        return 0;
    }
}
