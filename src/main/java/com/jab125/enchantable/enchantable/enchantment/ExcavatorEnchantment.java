package com.jab125.enchantable.enchantable.enchantment;

import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolItem;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class ExcavatorEnchantment extends ModEnchantment {
    public static final int BASE_SIZE = 3;


    protected ExcavatorEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentTarget.DIGGER, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    public static float onPlayerMineSpeed(PlayerEntity player, BlockState state, float speed)
    {
        float effectiveSpeed = getEffectiveDigSpeed(player, state);
        if(effectiveSpeed > 0)
        {
            return effectiveSpeed;
        }
        return speed;
    }

    /**
     * Gets the effective speed when using the excavator enchantment. Essentially gets the average
     * speed and divides it by the number of blocks it can effectively mine.
     *
     * @param player the player mining the blocks
     * @param state    the block state being targeted
     * @return the effective speed
     */
    private static float getEffectiveDigSpeed(PlayerEntity player, BlockState state) {
        ItemStack heldItem = player.getMainHandStack();
        if(heldItem.isEmpty()) return 0;

        if(!EnchantmentHelper.get(heldItem).containsKey(ModEnchantments.EXCAVATOR)) {
            return 0;
        }

        int level = EnchantmentHelper.get(heldItem).get(ModEnchantments.EXCAVATOR);
        int size = BASE_SIZE + Math.max(0, level - 1) * 2;

        World world = player.getEntityWorld();
        Direction direction = Direction.getEntityFacingOrder(player)[0];
        double reach = 5.0F;
        HitResult result = player.raycast(reach, 0, false);
        if(result.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult blockResult = (BlockHitResult) result;
            direction = blockResult.getSide();
        }

        Set<TagKey<Block>> toolTypes = new HashSet<>();
        if(heldItem.getItem() instanceof MiningToolItem miningToolItem)
        {
            toolTypes = Collections.singleton(miningToolItem.effectiveBlocks);
        }

        if(!true) { //forge method was here
            return 0;
        }

        if(state.isIn(ConventionalBlockTags.ORES))
        {
            return 0;
        }

        Function<Pair<Integer, Integer>, BlockPos> function = null;
        if(direction.getAxis().isHorizontal())
        {
            Direction finalDirection = direction.rotateYClockwise();
            if (result instanceof BlockHitResult blockHitResult)
            function = pair -> blockHitResult.getBlockPos().add(finalDirection.getAxis().choose(pair.left() - (size - 1) / 2, 0, 0), pair.right() - (size - 1) / 2, finalDirection.getAxis().choose(0, 0, pair.left() - (size - 1) / 2));
        }
        else
        {
            if (result instanceof BlockHitResult blockHitResult)
            function = pair -> blockHitResult.getBlockPos().add(pair.left() - (size - 1) / 2, 0, pair.right() - (size - 1) / 2);
        }
        if (function == null) return 0;

        Pair<Float, Integer> pair = getDestroySpeed(world, player, size, toolTypes, heldItem, function);
        float totalDigSpeed = pair.left();
        int totalBlocks = pair.right();
        if(totalBlocks <= 0)
        {
            return 0;
        }

        StatusEffectInstance instance = player.getStatusEffect(StatusEffects.HASTE);
        if(instance != null)
        {
            totalDigSpeed *= (instance.getAmplifier() + 1);
        }

        return (totalDigSpeed / (float) totalBlocks) / (float) totalBlocks;
    }

    private static Pair<Float, Integer> getDestroySpeed(World world, PlayerEntity player, int size, Set<TagKey<Block>> toolTypes, ItemStack stack, Function<Pair<Integer, Integer>, BlockPos> function)
    {
        int durability = stack.getMaxDamage() - stack.getDamage();
        float totalDigSpeed = 0;
        int totalBlocks = 0;
        for(int i = 0; i < size; i++)
        {
            for(int j = 0; j < size; j++)
            {
                BlockPos blockPos = function.apply(Pair.of(i, j));
                BlockState blockState = world.getBlockState(blockPos);
                if(blockState.isAir())
                {
                    continue;
                }
                if(isToolEffective(toolTypes, blockState, player, world, blockPos))
                {
                    if(blockState.isIn(ConventionalBlockTags.ORES))
                    {
                        continue;
                    }
                    totalDigSpeed += getDigSpeed(player, blockState);
                    totalBlocks++;

                    if(totalBlocks >= durability)
                    {
                        return Pair.of(totalDigSpeed, totalBlocks);
                    }
                }
            }
        }
        return Pair.of(totalDigSpeed, totalBlocks);
    }

    @Override
    public int getMinPower(int level) {
        return 15;
    }

    @Override
    public int getMaxPower(int level) {
        return super.getMinPower(level) + 50;
    }

    @Override
    protected boolean canAccept(Enchantment other) {
        return super.canAccept(other) && other != Enchantments.FORTUNE && other != ModEnchantments.ORE_EATER;
    }

    public static void onPlayerBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        ItemStack heldItem = player.getMainHandStack();
        if(heldItem.isEmpty()) return;

        if (!EnchantmentHelper.get(heldItem).containsKey(ModEnchantments.EXCAVATOR)) {
            return;
        }

        int level = EnchantmentHelper.get(heldItem).get(ModEnchantments.EXCAVATOR);
        int size = BASE_SIZE + Math.max(0, level - 1) * 2;

        Direction direction = Direction.getEntityFacingOrder(player)[0];
        double reach = 5.0;
        HitResult result = player.raycast(reach, 0, false);
        if (result.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockResult = (BlockHitResult) result;
            direction = blockResult.getSide();
        }

        Set<TagKey<Block>> toolTypes = new HashSet<>();
        if(heldItem.getItem() instanceof MiningToolItem miningToolItem)
        {
            toolTypes = Collections.singleton(miningToolItem.effectiveBlocks);
        }

        BlockState blockState = world.getBlockState(pos);
        if(!isToolEffective(toolTypes, blockState, player, world, pos))
        {
            return;
        }

        if(blockState.isIn(ConventionalBlockTags.ORES))
        {
            return;
        }

        Function<Pair<Integer, Integer>, BlockPos> function;
        if(direction.getAxis().isHorizontal())
        {
            Direction finalDirection = direction.rotateYClockwise();
            function = pair -> pos.add(finalDirection.getAxis().choose(pair.left() - (size - 1) / 2, 0, 0), pair.right() - (size - 1) / 2, finalDirection.getAxis().choose(0, 0, pair.left() - (size - 1) / 2));
        }
        else
        {
            function = pair -> pos.add(pair.left() - (size - 1) / 2, 0, pair.right() - (size - 1) / 2);
        }

        int durability = heldItem.getMaxDamage() - heldItem.getDamage();
        if(durability > 1) //No point breaking blocks if only one durability left
        {
            int damageAmount = destroyBlocks(world, pos, player, size, toolTypes, heldItem, function);

            /* Handles applying damage to the tool and considers if it has an unbreaking enchantment */
            heldItem.damage(damageAmount, player, player1 -> player1.sendToolBreakStatus(Hand.MAIN_HAND));
        }
    }

    private static int destroyBlocks(World world, BlockPos source, PlayerEntity player, int size, Set<TagKey<Block>> toolTypes, ItemStack stack, Function<Pair<Integer, Integer>, BlockPos> function) {
        int durability = stack.getMaxDamage() - stack.getDamage();
        int damageAmount = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                BlockPos newPos = function.apply(Pair.of(i, j));
                if (newPos.equals(source)) {
                    continue;
                }
                if (destroyBlock(world, toolTypes, newPos, true, stack, player)) {
                    damageAmount++;
                }
                if (damageAmount >= durability) {
                    return damageAmount;
                }
            }
        }
        return damageAmount;
    }

    private static boolean destroyBlock(World world, Set<TagKey<Block>> toolTypes, BlockPos pos, @SuppressWarnings("SameParameterValue") boolean spawnDrops, ItemStack stack, PlayerEntity player) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isAir()) {
            return false;
        }
        if (isToolEffective(toolTypes, blockState, player, world, pos)) {
            if (blockState.isIn(ConventionalBlockTags.ORES)) {
                return false;
            }
            FluidState fluidState = world.getFluidState(pos);
            if (spawnDrops) {
                BlockEntity blockEntity = blockState.hasBlockEntity() ? world.getBlockEntity(pos) : null;
                Block.dropStacks(blockState, world, pos, blockEntity, player, stack);
            }
            return world.setBlockState(pos, fluidState.getBlockState(), 3);
        }
        return false;
    }

    private static boolean isInTags(BlockState block, Set<TagKey<Block>> tags) {
        for (TagKey<Block> tag : tags) {
            if (block.isIn(tag)) return true;
        }
        return false;
    }

    public static boolean isToolEffective(Set<TagKey<Block>> toolTypes, BlockState blockState, PlayerEntity player, World world, BlockPos pos) {
        if (blockState.getHardness(world, pos) <= 0) {
            return false;
        }
        if (!isInTags(blockState, toolTypes)) {
            if (blockState.getMaterial().isReplaceable()) {
                return false;
            }
            return false; // assume a harvest tool
//            if(blockState.getHarvestTool() != null) { //TODO: Create a method that gets the harvest tool
//                return false;
//            }
        }
        return true;
    }

    public static float getBlockBreakingSpeed(PlayerInventory inventory, BlockState block) {
        return inventory.main.get(inventory.selectedSlot).getMiningSpeedMultiplier(block);
    }

    public static float getDigSpeed(PlayerEntity player, BlockState state) {
        float destroySpeed = getBlockBreakingSpeed(player.getInventory(), state);
        if (destroySpeed > 1.0F) {
            int efficiencyModifier = EnchantmentHelper.getEfficiency(player);
            ItemStack heldItem = player.getMainHandStack();
            if (efficiencyModifier > 0 && !heldItem.isEmpty()) {
                destroySpeed += (float) (efficiencyModifier * efficiencyModifier + 1);
            }
        }

        if (StatusEffectUtil.hasHaste(player)) {
            destroySpeed *= 1.0F + (float) (StatusEffectUtil.getHasteAmplifier(player) + 1) * 0.2F;
        }

        if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float multiplier;
            switch (Objects.requireNonNull(player.getStatusEffect(StatusEffects.MINING_FATIGUE)).getAmplifier())
            {
                case 0:
                    multiplier = 0.3F;
                    break;
                case 1:
                    multiplier = 0.09F;
                    break;
                case 2:
                    multiplier = 0.0027F;
                    break;
                case 3:
                default:
                    multiplier = 8.1E-4F;
            }

            destroySpeed *= multiplier;
        }

        if (player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player)) {
            destroySpeed /= 5.0F;
        }

        if (!player.isOnGround()) {
            destroySpeed /= 5.0F;
        }
        return destroySpeed;
    }
}
