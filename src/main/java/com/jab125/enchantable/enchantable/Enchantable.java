package com.jab125.enchantable.enchantable;

import com.jab125.enchantable.enchantable.client.ClientEvents;
import com.jab125.enchantable.enchantable.enchantment.ExcavatorEnchantment;
import com.jab125.enchantable.enchantable.enchantment.ModEnchantment;
import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.mininglevel.FabricMiningLevelInit;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.animation.WardenAnimations;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.HoeItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import static com.jab125.enchantable.enchantable.enchantment.ModEnchantments.CULTIVATOR;
import static com.jab125.enchantable.enchantable.enchantment.ModEnchantments.registerEnchantments;
import static com.jab125.enchantable.enchantable.sound.ModSounds.registerSounds;

@EnvironmentInterface(value = EnvType.CLIENT, itf = ClientModInitializer.class)
public class Enchantable implements ClientModInitializer, ModInitializer {
    public static final ModEnchantment.Acceptable HOE = item -> item instanceof HoeItem;
    public static final ModEnchantment.Acceptable TILLABLE = item -> item instanceof HoeItem || item instanceof ShovelItem;
    public static final ModEnchantment.Acceptable PICKAXE = item -> item instanceof PickaxeItem;

    public static boolean fireEditBlockEvent(ClientPlayerEntity player, ClientWorld world, BlockPos pos) {
        return true; //TODO: Use Fabric Events API
        //throw new IllegalStateException("Not Implemented!");//return false;
    }

    @Override
    public void onInitialize() {
        registerEnchantments();
        registerSounds();

        PlayerBlockBreakEvents.BEFORE.register(((world1, player1, pos1, state, blockEntity) -> {ExcavatorEnchantment.onPlayerBreak(world1, player1, pos1, state, blockEntity); return true;}));

        ServerPlayNetworking.registerGlobalReceiver(BLOCK_TILL, ((server, player, handler, buf, responseSender) -> {
            var pos = buf.readBlockPos();
            var world = player.world;

            BlockState groundState = ShovelItem.PATH_STATES.get(world.getBlockState(pos).getBlock());

            boolean air = world.isAir(pos.up());
            boolean replaceable = world.getBlockState(pos.up()).getMaterial().isReplaceable();
            server.execute(() -> {
                if ((air || replaceable) && groundState != null && EnchantmentHelper.get(player.getMainHandStack()).containsKey(CULTIVATOR)) {
                    world.setBlockState(pos, groundState);

                    if(!air) {
                        world.setBlockState(pos.up(), Blocks.AIR.getDefaultState());
                    }
                }
            });
        }));

        ServerPlayNetworking.registerGlobalReceiver(DAMAGE_TILL, ((server, player, handler, buf, responseSender) -> {
            int damage = buf.readInt();
            Hand hand = buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
            server.execute(() -> {
                player.getMainHandStack().damage(damage, player, serverPlayerEntity -> serverPlayerEntity.sendToolBreakStatus(hand));
            });
        }));
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::onClientTick);
        ClientTickEvents.START_CLIENT_TICK.register(ClientEvents::onStartClientTick);
    }

    private static final Identifier BLOCK_TILL = new Identifier("enchantable", "block_till");
    private static final Identifier DAMAGE_TILL = new Identifier("enchantable", "damage_till");

    // tells the server to get rid of those nearby blocks
    public static void sendBlockTilledResult(BlockPos pos) {
        var buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ClientPlayNetworking.send(BLOCK_TILL, buf);
    }

    public static void sendTimeToDamageTill(int q, Hand d) {
        var buf = PacketByteBufs.create();
        buf.writeInt(q);
        buf.writeBoolean(d == Hand.MAIN_HAND);
        ClientPlayNetworking.send(DAMAGE_TILL, buf);
    }
}
