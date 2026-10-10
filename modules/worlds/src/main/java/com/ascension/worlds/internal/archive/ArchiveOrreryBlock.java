package com.ascension.worlds.internal.archive;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The base of the Archive's star map. For now an ordinary block, so the Archive's design session
 * can place it in its templates; the hologram of the solar system that floats above it, with every
 * planet's gate shown dark or lit, comes in a later increment
 * ({@code docs/technical/lunar-archive-format.md}).
 *
 * <p>Unbreakable in survival for the same reason as {@link ArchivePlinthBlock}. Its shape is the
 * design's drum on its base (the format's handover), registered {@code noOcclusion} like the plinth.
 */
public final class ArchiveOrreryBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(2, 3, 2, 14, 11, 14),
            Block.box(1, 11, 1, 15, 13, 15));

    public ArchiveOrreryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
