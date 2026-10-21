package com.ascension.combat.internal.enchanting.library;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The library's contents: each stored enchantment and its best level, saved with the block. Holds ids and numbers only, never an
 * entity or a level (ADR-0007), and nothing ticks.
 */
public final class EnchantmentLibraryBlockEntity extends BlockEntity {

    private static final String TAG = "Library";

    private final LibraryStore store = new LibraryStore();

    public EnchantmentLibraryBlockEntity(BlockPos pos, BlockState state) {
        super(LibraryContent.BLOCK_ENTITY.get(), pos, state);
    }

    public LibraryStore store() {
        return store;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag saved = new CompoundTag();
        store.view().forEach(saved::putInt);
        tag.put(TAG, saved);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag saved = tag.getCompound(TAG);
        for (String id : saved.getAllKeys()) {
            store.set(id, saved.getInt(id));
        }
    }
}
