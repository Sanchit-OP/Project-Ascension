package com.ascension.atmosphere.internal.sealed;

import com.ascension.atmosphere.internal.AtmosphereAttachments;
import com.ascension.atmosphere.internal.AtmosphereContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Holds a closed room breathable for as long as it has air (ADR-0026).
 *
 * <p>Pipes and tanks feed it through the standard fluid capability, so no other mod needs to be
 * known. The room itself is the same flood fill the Oxygen Emitter uses; this block only decides
 * whether the room is currently supplied.
 */
public final class AirDispenserBlock extends Block implements EntityBlock {

    public AirDispenserBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirDispenserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide || type != AtmosphereContent.AIR_DISPENSER_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((AirDispenserBlockEntity) be).serverTick();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                           LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof AirDispenserBlockEntity dispenser) {
            SealedVolume.Result result = dispenser.register(serverLevel);
            if (placer instanceof Player player) {
                OxygenEmitterBlock.report(player, result);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof AirDispenserBlockEntity dispenser) {
            int room = serverLevel.getData(AtmosphereAttachments.SEALED_VOLUMES).volumeSize(pos);
            player.displayClientMessage(Component.translatable(
                    "block.ascension_atmosphere.air_dispenser.status",
                    dispenser.airMb(), dispenser.capacityMb(), room, AirDemand.mbPerSecond(room)), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            serverLevel.getData(AtmosphereAttachments.SEALED_VOLUMES).removeEmitter(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
