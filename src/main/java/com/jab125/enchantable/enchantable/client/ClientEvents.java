package com.jab125.enchantable.enchantable.client;

import com.google.common.collect.Sets;
import com.jab125.enchantable.enchantable.Enchantable;
import com.jab125.enchantable.enchantable.enchantment.ExcavatorEnchantment;
import com.jab125.enchantable.enchantable.enchantment.ModEnchantments;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

public class ClientEvents {
    private static boolean needsResetting = true;
    private static BlockPos lastHittingPos = null;
    private static final Long2ObjectMap<BlockBreakingInfo> DAMAGE_PROGRESS = new Long2ObjectOpenHashMap<>();

    public static void onStartClientTick(MinecraftClient client) {
        if (client.player != null) {
            boolean leftClicking = client.currentScreen == null && client.options.attackKey.isPressed() && client.mouse.isCursorLocked();
            if (leftClicking) {
                assert client.crosshairTarget != null;
                if (client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult result = (BlockHitResult) client.crosshairTarget;
                    BlockPos pos = result.getBlockPos();
                    if (!Enchantable.fireEditBlockEvent(client.player, client.world, pos)) {
                        needsResetting = true;
                        return;
                    }
                }
                if (needsResetting) {
                    ClientEvents.resetBreakParticles();
                    needsResetting = false;
                }
            }
        }
    }

    public static void onClientTick(MinecraftClient client) {
         if (client.interactionManager != null) {
            if (!client.interactionManager.isBreakingBlock()) {
                clearBreakProgress();
                return;
            }
            PlayerEntity player = client.player;
            if (player == null) {
                clearBreakProgress();
                return;
            }

            ItemStack heldItem = player.getMainHandStack();
            if (heldItem.isEmpty()) {
                clearBreakProgress();
                return;
            }

            World world = player.world;
            BlockPos pos = client.interactionManager.currentBreakingPos;
            Direction direction = Direction.getEntityFacingOrder(player)[0];
            double reach = 5.0D;
            HitResult result = player.raycast(reach, 0, false);

            if (result.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) result;
                direction = blockHitResult.getSide();
            }
            BlockState blockState = world.getBlockState(pos);

            if (!true) { // there was a forge method that i can't find the equivalent to
                clearBreakProgress();
                return;
            }

            clearBreakProgress();
            handleExcavatorBreakProgress(client, pos, player, world, heldItem, blockState, direction);
            handleOreEaterProgress(client, pos, player, world, heldItem, blockState, direction);
        }
    }

    private static void handleOreEaterProgress(MinecraftClient client, BlockPos pos, PlayerEntity player, World world, ItemStack heldItem, BlockState blockState, Direction direction) {

    }

    private static void handleExcavatorBreakProgress(MinecraftClient client, BlockPos pos, PlayerEntity player, World world, ItemStack heldItem, BlockState blockState, Direction direction) {
        if (!EnchantmentHelper.get(heldItem).containsKey(ModEnchantments.EXCAVATOR)) {
            return;
        }

        if (blockState.isIn(ConventionalBlockTags.ORES)) {
            return;
        }

        int level = EnchantmentHelper.get(heldItem).get(ModEnchantments.EXCAVATOR);
        int size = ExcavatorEnchantment.BASE_SIZE + Math.max(0, level - 1) * 2;
        Function<Pair<Integer, Integer>, BlockPos> function;
        if (direction.getAxis().isHorizontal()) {
            Direction finalDirection = direction.rotateYClockwise();
            function = pair -> pos.add(finalDirection.getAxis().choose(pair.left() - (size - 1) / 2, 0, 0), pair.right() - (size - 1) / 2, finalDirection.getAxis().choose(0, 0, pair.left() - (size - 1) / 2));
        }
        else {
            function = pair -> pos.add(pair.left() - (size - 1) / 2, 0, pair.right() - (size - 1) / 2);
        }

        BlockBreakingInfo progress = client.worldRenderer.blockBreakingInfos.get(player.getId());
        if (progress != null) {
            lastHittingPos = pos;
            Set<BlockPos> blocks = getExcavatorBlocks(world, size, pos, player, function);
            blocks.forEach(pos1 -> {
                BlockBreakingInfo subProgress = new BlockBreakingInfo(player.getId(), pos1);
                subProgress.setStage(progress.getStage());
                DAMAGE_PROGRESS.put(pos1.asLong(), subProgress);
                client.worldRenderer.blockBreakingProgressions.computeIfAbsent(pos1.asLong(), i -> Sets.newTreeSet()).add(subProgress);
            });
        }
        client.player.sendMessage(Text.literal(client.worldRenderer.blockBreakingProgressions.size() + ""));
    }

    private static void resetBreakParticles() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            assert client.interactionManager != null;
            BlockPos currentBlock = client.interactionManager.currentBreakingPos;
            assert client.world != null;
            BlockState blockState = client.world.getBlockState(currentBlock);
            client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, currentBlock, Direction.DOWN));
            client.interactionManager.currentBreakingProgress = 0.0F;
            client.interactionManager.breakingBlock = false;
            assert client.player != null;
            client.player.resetLastAttackedTicks();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void clearBreakProgress() {
        if (DAMAGE_PROGRESS.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        DAMAGE_PROGRESS.forEach((posLong, blockBreakingInfo) -> {
            Set<BlockBreakingInfo> set = client.worldRenderer.blockBreakingProgressions.get(posLong);
            if (set != null) {
                set.remove(blockBreakingInfo);
                if (set.isEmpty()) {
                    client.worldRenderer.blockBreakingProgressions.remove(posLong);
                }
            }
        });
        DAMAGE_PROGRESS.clear();
    }

    private static Set<BlockPos> getExcavatorBlocks(World world, int size, BlockPos source, PlayerEntity player, Function<Pair<Integer, Integer>, BlockPos> function)
    {
        ItemStack heldItem = player.getMainHandStack();
        if(heldItem.isEmpty()) {
            return Collections.emptySet();
        }

        Set<TagKey<Block>> toolTypes = new HashSet<>();
        if(heldItem.getItem() instanceof MiningToolItem toolItem)
        {
            toolTypes = Collections.singleton(toolItem.effectiveBlocks);
        }

        Set<BlockPos> blocks = new HashSet<>();
        for(int i = 0; i < size; i++)
        {
            for(int j = 0; j < size; j++)
            {
                BlockPos pos = function.apply(Pair.of(i, j));
                if(pos.equals(source))
                {
                    continue;
                }
                BlockState blockState = world.getBlockState(pos);

                if(blockState.isAir())
                {
                    continue;
                }
                if(ExcavatorEnchantment.isToolEffective(toolTypes, blockState, player, world, pos))
                {
                    if(blockState.isIn(ConventionalBlockTags.ORES))
                    {
                        continue;
                    }
                    blocks.add(pos);
                }
            }
        }
        return blocks;
    }
}
