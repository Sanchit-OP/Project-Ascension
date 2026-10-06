package com.ascension.worlds.internal.archive;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A pedestal holding one of the builders' inscriptions. Right-click reads it; it never gives the
 * tablet up (the Archive is a place you visit, not a chest you empty; portable copies come from
 * loot as {@link ArchiveTabletItem}s). Which inscription is its block entity's job
 * ({@link ArchivePlinthBlockEntity}), set by the structure template.
 *
 * <p>Unbreakable in survival, like the gateway field: a plinth carried off would leave a hole in
 * the story for every other player on the server.
 */
public final class ArchivePlinthBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<ArchivePlinthBlock> CODEC = simpleCodec(ArchivePlinthBlock::new);

    public ArchivePlinthBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
