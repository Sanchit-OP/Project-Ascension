package com.ascension.worlds.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The one block every planet's gateway portal is, structurally &mdash; see
 * {@code plans/m3-vertical-slice.md}'s M3.2. {@link #ACTIVE} mirrors what
 * {@link GatewayNetworkState} says about this planet; it is a rendering/interaction convenience,
 * not the source of truth.
 *
 * <p>Right-clicking an inactive portal with the item this planet requires
 * ({@link GatewayPortalActivation}) consumes it and flips the block active. Right-clicking an
 * already-active portal opens the destination list instead ({@link GatewayDestinations}), and an
 * empty-handed click on either state reports what is needed or what is possible &mdash; the same
 * "a machine that silently refuses looks like a bug" reasoning
 * {@code OxygenRefillStationBlock} already uses.
 */
public final class GatewayPortalBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public GatewayPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (state.getValue(ACTIVE)
                || !(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (GatewayPortalActivation.tryActivate(serverLevel, serverPlayer, stack)) {
            level.setBlock(pos, state.setValue(ACTIVE, true), 3);
        } else {
            GatewayPortalActivation.describeRequirement(serverLevel, serverPlayer);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(ACTIVE)) {
            GatewayDestinations.sendPicker(serverPlayer, serverLevel);
        } else {
            GatewayPortalActivation.describeRequirement(serverLevel, serverPlayer);
        }
        return InteractionResult.CONSUME;
    }
}
