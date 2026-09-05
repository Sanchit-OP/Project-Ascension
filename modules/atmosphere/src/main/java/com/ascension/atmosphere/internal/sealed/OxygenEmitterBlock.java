package com.ascension.atmosphere.internal.sealed;

import com.ascension.atmosphere.internal.AtmosphereAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pressurises the sealed room it is placed in.
 *
 * <p>Holds no state of its own. The computed volume lives in the level's
 * {@link SealedVolumeIndex}, so the block is only a marker and a trigger &mdash; there is
 * nothing here to fall out of sync with the index.
 */
public final class OxygenEmitterBlock extends Block {

    public OxygenEmitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                           LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel) {
            SealedVolume.Result result = serverLevel.getData(AtmosphereAttachments.SEALED_VOLUMES)
                    .update(serverLevel, pos);
            if (placer instanceof Player player) {
                report(player, result);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            serverLevel.getData(AtmosphereAttachments.SEALED_VOLUMES).remove(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Tell the player what happened.
     *
     * <p>Per the design note in {@code atmosphere-api.md}: an emitter that cannot find a sealed
     * space must say so, not sit there doing nothing while the player wonders why they are still
     * suffocating.
     */
    private static void report(Player player, SealedVolume.Result result) {
        if (result.sealed()) {
            // This emitter's own volume. The previous version reported the level-wide union,
            // which is a different and much less useful number the moment a second emitter
            // exists.
            player.displayClientMessage(
                    Component.literal("Sealed: pressurising " + result.size() + " blocks")
                            .withStyle(ChatFormatting.GREEN), true);
        } else {
            player.displayClientMessage(
                    Component.literal("Not sealed — this space is open or too large")
                            .withStyle(ChatFormatting.RED), true);
        }
    }
}
