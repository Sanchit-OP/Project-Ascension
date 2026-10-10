package com.ascension.worlds.internal.archive;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A pedestal holding one of the builders' inscriptions. Right-click reads it; it never gives the
 * tablet up (the Archive is a place you visit, not a chest you empty; portable copies come from
 * loot as {@link ArchiveTabletItem}s). Which inscription is its block entity's job
 * ({@link ArchivePlinthBlockEntity}), set by the structure template.
 *
 * <p>Unbreakable in survival, like the gateway field: a plinth carried off would leave a hole in
 * the story for every other player on the server.
 *
 * <p>Its shape is the design's model, box for box (base, step, column, desk; the
 * {@code lunar-archive-format.md} handover), and the same whichever way it faces. The block is
 * registered {@code noOcclusion} so a wall or floor touching it is never culled into a hole.
 */
public final class ArchivePlinthBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<ArchivePlinthBlock> CODEC = simpleCodec(ArchivePlinthBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(2, 2, 2, 14, 4, 14),
            Block.box(4, 4, 4, 12, 11, 12),
            Block.box(2, 11, 2, 14, 15, 14));

    public ArchivePlinthBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Faces the player who places it, so its inscription faces them (creative and tests only). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer reader
                && level.getBlockEntity(pos) instanceof ArchivePlinthBlockEntity plinth) {
            ArchiveReading.open(reader, plinth.entry());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArchivePlinthBlockEntity(pos, state);
    }
}
