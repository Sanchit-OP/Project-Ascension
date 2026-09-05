package com.ascension.atmosphere.internal.sealed;

import com.ascension.atmosphere.internal.AtmosphereAttachments;
import com.ascension.atmosphere.internal.AtmosphereContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Registers its emitter with the level index for as long as its chunk is loaded.
 *
 * <p>It stores nothing. Its entire job is lifecycle: the volume index is derived data that is
 * deliberately never serialised, so something has to put emitters back into it when a chunk
 * loads. Without this, an emitter placed before a restart is simply unknown afterwards and its
 * room silently stops working until the block is broken and replaced.
 *
 * <p>Does not tick. Volumes are recomputed from block changes, never on a schedule
 * (ADR-0007 rule 5).
 */
public final class OxygenEmitterBlockEntity extends BlockEntity {

    public OxygenEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(AtmosphereContent.OXYGEN_EMITTER_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getData(AtmosphereAttachments.SEALED_VOLUMES)
                    .addEmitter(serverLevel, getBlockPos());
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getData(AtmosphereAttachments.SEALED_VOLUMES)
                    .removeEmitter(getBlockPos());
        }
        super.setRemoved();
    }
}
