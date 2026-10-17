package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.internal.rune.Reentry;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Vanguard's Vein Breaker, the path's <b>world keystone</b> (docs/technical/rune-system.md section
 * 4.1): mine an ore or a log <em>while sneaking</em> and the connected blocks of the same kind
 * break with it, up to a limit. Each one wears the tool and costs a little hunger, so it saves
 * clicks, not resources.
 *
 * <p>The extra blocks are broken the way the player's own mining breaks them
 * ({@code ServerPlayerGameMode.destroyBlock}): the same drops, the same tool wear, and the same
 * break event, so protection mods that cancel it still protect the blocks. {@link #BREAKING}
 * stops those extra breaks from starting another search.
 *
 * <p>It stops early rather than do harm: it never breaks the tool (it leaves one point of
 * durability), never mines an ore the tool can't harvest (the block would be lost), and stops when
 * the player's food runs out. Creative players are left alone: they already break everything at
 * once.
 *
 * <p>Its numbers are the rune's data: {@code max_blocks}, {@code exhaustion_per_block} (vanilla
 * charges 0.005 per block, so the default is five times that).
 */
public final class VeinBreakerEffects {

    public static final ResourceLocation ID = Runes.id("vein_breaker");

    /** Guards this rune's own extra breaks from starting another search. */
    private static final Reentry BREAKING = new Reentry();

    private VeinBreakerEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOW, VeinBreakerEffects::onBreak);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (BREAKING.active() || event.isCanceled()) {
            return;
        }

        if (!(event.getPlayer() instanceof ServerPlayer player) || !player.isShiftKeyDown() || player.isCreative()) {
            return;
        }

        BlockState origin = event.getState();
        if (!origin.is(Tags.Blocks.ORES) && !origin.is(BlockTags.LOGS)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        Block kind = origin.getBlock();
        List<BlockPos> vein = VeinSearch.connected(event.getPos(),
                pos -> level.isLoaded(pos) && level.getBlockState(pos).is(kind),
                rune.get().count("max_blocks", 32) - 1);

        float exhaustion = (float) rune.get().value("exhaustion_per_block", 0.025);
        BREAKING.run(() -> breakAll(player, level, vein, exhaustion));
    }

    /** Breaks the vein block by block, stopping as soon as the player can no longer afford the next. */
    private static void breakAll(ServerPlayer player, ServerLevel level, List<BlockPos> vein, float exhaustion) {
        for (BlockPos pos : vein) {
            if (!canAfford(player, level.getBlockState(pos))) {
                return;
            }

            if (player.gameMode.destroyBlock(pos)) {
                player.causeFoodExhaustion(exhaustion);
            }
        }
    }

    /** Whether the player can break {@code state} with what they hold and have, without harm. */
    private static boolean canAfford(ServerPlayer player, BlockState state) {
        ItemStack tool = player.getMainHandItem();
        if (tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= 1) {
            return false;
        }

        return player.getFoodData().getFoodLevel() > 0 && player.getMainHandItem().isCorrectToolForDrops(state);
    }
}
