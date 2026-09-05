package com.ascension.atmosphere.internal.supply;

import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereRegistry;
import com.ascension.atmosphere.internal.AtmosphereTuning;
import com.ascension.atmosphere.internal.OxygenTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Compresses the surrounding air into a tank.
 *
 * <p><strong>It only works where the air is breathable.</strong> That single rule is what makes
 * the expedition loop a loop: fill at base, go somewhere you cannot breathe, come back. It also
 * composes with the rest of the module for free &mdash; a station inside a pressurised room on
 * an airless world works, because a pressurised room <em>is</em> breathable air, and the same
 * station outside that room does not.
 *
 * <p>Breathability is checked at the player's head rather than at the block. The station is a
 * full solid block, so it is a wall as far as the sealed-volume fill is concerned and is never
 * inside its own room; asking about the block's own position would report vacuum in a working
 * base. The player's eye position is the air the machine would actually be drawing from.
 *
 * <p><strong>Placeholder, like the emitter.</strong> Filling is instant and free because there
 * is nothing yet for it to cost: oxygen production is the Create-powered generator chain
 * described in {@code atmosphere-api.md} section 7b, and that lands in a Tier 2 jar. Pricing
 * the refill before that exists would mean inventing a currency we intend to replace.
 */
public final class OxygenRefillStationBlock extends Block {

    public OxygenRefillStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (!OxygenTankItem.isTank(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            // Swing the arm on the client and let the server decide what actually happened.
            return ItemInteractionResult.SUCCESS;
        }

        if (!airHereIsBreathable(serverLevel, serverPlayer)) {
            say(serverPlayer, "block.ascension_atmosphere.oxygen_refill_station.no_air",
                    ChatFormatting.RED);
            return ItemInteractionResult.CONSUME;
        }

        TankOxygenSource tank = new TankOxygenSource(stack);
        int room = tank.capacity() - tank.available();
        if (room <= 0) {
            say(serverPlayer, "block.ascension_atmosphere.oxygen_refill_station.full",
                    ChatFormatting.YELLOW);
            return ItemInteractionResult.CONSUME;
        }

        tank.accept(room);
        serverLevel.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7f, 1.4f);
        serverPlayer.displayClientMessage(Component.translatable(
                        "block.ascension_atmosphere.oxygen_refill_station.filled",
                        AtmosphereTuning.formatDuration(
                                tank.available() / AtmosphereTuning.BASE_UNITS_PER_SECOND))
                .withStyle(ChatFormatting.GREEN), true);

        // The player's total supply just changed. Nudge the tracker so the bar is right the next
        // pass rather than at the next reconcile.
        OxygenTracker.invalidate(serverPlayer);
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * Empty-handed: say whether this station can currently do anything.
     *
     * <p>The same argument as {@code /ascension atmosphere why}. A machine that silently refuses
     * is a machine whose failure looks identical to a bug, and this one refuses for a reason the
     * player cannot see from outside.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        boolean ready = airHereIsBreathable(serverLevel, serverPlayer);
        say(serverPlayer,
                ready ? "block.ascension_atmosphere.oxygen_refill_station.ready"
                        : "block.ascension_atmosphere.oxygen_refill_station.no_air",
                ready ? ChatFormatting.GREEN : ChatFormatting.RED);
        return InteractionResult.CONSUME;
    }

    private static boolean airHereIsBreathable(ServerLevel level, ServerPlayer player) {
        Atmosphere atmosphere = AtmosphereRegistry.query(level, player.getEyePosition());
        return atmosphere.breathable();
    }

    private static void say(ServerPlayer player, String key, ChatFormatting colour) {
        player.displayClientMessage(Component.translatable(key).withStyle(colour), true);
    }
}
