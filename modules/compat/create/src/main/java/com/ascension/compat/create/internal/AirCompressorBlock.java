package com.ascension.compat.create.internal;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * A shaft runs through it along its axis; turning it makes air. Right-click with a tank (anything
 * that takes air through the item fluid capability) to fill it from the buffer.
 */
public final class AirCompressorBlock extends RotatedPillarKineticBlock
        implements IBE<AirCompressorBlockEntity> {

    public AirCompressorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public Class<AirCompressorBlockEntity> getBlockEntityClass() {
        return AirCompressorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirCompressorBlockEntity> getBlockEntityType() {
        return CreateContent.AIR_COMPRESSOR_ENTITY.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, net.minecraft.world.InteractionHand hand,
                                              BlockHitResult hit) {
        IFluidHandlerItem held = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (held == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, compressor -> {
                int moved = FluidUtil.tryFluidTransfer(held, compressor.output(), Integer.MAX_VALUE, true).getAmount();
                player.displayClientMessage(moved > 0
                        ? Component.translatable("block.ascension_compat_create.air_compressor.filled", moved)
                        : Component.translatable("block.ascension_compat_create.air_compressor.nothing"), true);
            });
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
