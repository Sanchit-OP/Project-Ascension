package com.ascension.combat.internal.enchanting.library;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Enchantment Library (Sanchit, 2026-10-22): put enchanted books in, and take out any combination that fits on one new book.
 * Right-click opens {@link LibraryMenu}. Breaking the block returns its contents as one enchanted book per stored enchantment,
 * at the stored level, so nothing is lost by moving it.
 */
public final class EnchantmentLibraryBlock extends BaseEntityBlock {

    public static final MapCodec<EnchantmentLibraryBlock> CODEC = simpleCodec(EnchantmentLibraryBlock::new);

    public EnchantmentLibraryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantmentLibraryBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof EnchantmentLibraryBlockEntity library && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inventory, who) -> new LibraryMenu(id, inventory, library, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("container.ascension_combat.enchantment_library"));
            serverPlayer.openMenu(provider);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof EnchantmentLibraryBlockEntity library) {
            var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            library.store().view().forEach((id, stored) -> {
                Holder<Enchantment> holder = enchantments
                        .get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(id))).orElse(null);
                if (holder != null) {
                    ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                    book.enchant(holder, stored);
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, book));
                }
            });
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
