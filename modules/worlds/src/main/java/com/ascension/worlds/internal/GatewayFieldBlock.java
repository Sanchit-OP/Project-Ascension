package com.ascension.worlds.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The walk-through opening of a lit gateway frame &mdash; this project's {@code nether_portal}.
 *
 * <p><strong>Walking in opens the destination screen</strong> (Sanchit's call, 2026-10-05) rather
 * than carrying you somewhere fixed: the network grows planet by planet, so where to go is a
 * choice. Once per entry, not every tick &mdash; {@link GatewayPickerSession#stepIntoField}.
 *
 * <p><strong>Collapses like a Nether portal.</strong> A field block survives only while every
 * neighbour in its own plane (above, below, and along its {@link #AXIS}) is either more field or
 * something solid. Break a block of the frame's border and the field next to it becomes air,
 * which in turn leaves its neighbours unsupported, so the whole opening clears in one cascade
 * &mdash; purely local checks, no knowledge of the frame's pattern needed and nothing scanned. The
 * keystone stays attuned, so an intact frame re-lights for free from its keystone.
 */
public final class GatewayFieldBlock extends Block {

    /** The plane the field lies in, like {@code nether_portal}'s. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final VoxelShape X_AXIS_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape Z_AXIS_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

    public GatewayFieldBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_AXIS_SHAPE : Z_AXIS_SHAPE;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        Direction.Axis along = direction.getAxis();
        boolean inPlane = along == Direction.Axis.Y || along == state.getValue(AXIS);
        if (inPlane && !neighbour.is(this) && neighbour.canBeReplaced()) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player) {
            GatewayDestinations.onStepIn(player, serverLevel);
        }
    }
}
