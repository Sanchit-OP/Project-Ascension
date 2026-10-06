package com.ascension.worlds.internal.archive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Which inscription a plinth shows. One field, {@code entry}, set by the Archive's structure
 * template ({@code {entry: "ascension_worlds:moon/the_road"}}) and never changed in play.
 *
 * <p>Server-side only: reading goes through {@link ArchiveReading}, which sends the id to the
 * reader, so the client never needs this block entity's data and there is no update packet.
 */
public final class ArchivePlinthBlockEntity extends BlockEntity {

    static final String ENTRY_TAG = "entry";

    @Nullable
    private ResourceLocation entry;

    public ArchivePlinthBlockEntity(BlockPos pos, BlockState state) {
        super(ArchiveContent.PLINTH_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    public ResourceLocation entry() {
        return entry;
    }

    public void setEntry(@Nullable ResourceLocation entry) {
        this.entry = entry;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        entry = tag.contains(ENTRY_TAG) ? ResourceLocation.tryParse(tag.getString(ENTRY_TAG)) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (entry != null) {
            tag.putString(ENTRY_TAG, entry.toString());
        }
    }
}
