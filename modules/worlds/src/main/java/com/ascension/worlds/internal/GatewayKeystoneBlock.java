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
 * The one block in a gateway frame players interact with &mdash; see
 * {@code docs/technical/gateway-frame-format.md}. {@link #ACTIVE} means <em>attuned</em>: this
 * keystone has been activated once and lights its frame for free from then on.
 *
 * <ul>
 *   <li>Unattuned, holding what this planet's portal needs ({@link GatewayPortalActivation}):
 *       checks the frame and, if it is whole, attunes and lights it.</li>
 *   <li>Unattuned, anything else: says what it needs.</li>
 *   <li>Attuned: re-lights the frame if its field has collapsed (a border block was broken and
 *       put back), then opens the destination screen &mdash; the same thing walking into the
 *       field does.</li>
 * </ul>
 *
 * <p>Breakable and craftable (Earth's players build their own), unlike the old single-block
 * portal. Breaking a keystone loses its attunement: it drops as a plain keystone.
 */
public final class GatewayKeystoneBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public GatewayKeystoneBlock(Properties properties) {
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
        if (GatewayPortalActivation.satisfies(serverLevel, stack)) {
            GatewayPortalActivation.tryAttune(serverLevel, pos, serverPlayer, stack);
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
            GatewayPortalActivation.relightAndOpen(serverLevel, pos, serverPlayer);
        } else {
            GatewayPortalActivation.describeRequirement(serverLevel, serverPlayer);
        }
        return InteractionResult.CONSUME;
    }
}
