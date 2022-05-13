package com.jab125.enchantable.enchantable;

import com.jab125.enchantable.enchantable.enchantment.ModEnchantment;
import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.entity.animation.WardenAnimations;
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

@EnvironmentInterface(value = EnvType.CLIENT, itf = ClientModInitializer.class)
public class Enchantable implements ClientModInitializer, ModInitializer {
    public static final ModEnchantment.Acceptable HOE = item -> item instanceof HoeItem;
    public static final ModEnchantment.Acceptable TILLABLE = item -> item instanceof HoeItem || item instanceof ShovelItem;
    public static final ModEnchantment.Acceptable PICKAXE = item -> item instanceof PickaxeItem;
    @Override
    public void onInitialize() {
        registerEnchantments();
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
