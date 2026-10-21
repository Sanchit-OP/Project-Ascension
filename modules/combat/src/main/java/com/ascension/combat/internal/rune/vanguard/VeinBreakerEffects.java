package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.internal.rune.Reentry;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Vanguard's Vein Breaker, the path's <b>world keystone</b> (docs/technical/rune-system.md section
 * 4.1): mine an ore or a log <em>while sneaking</em> and the connected blocks of the same kind
 * break with it, up to a limit. Buffed 2026-10-22 (Sanchit: "a little underwhelming"): 64 blocks instead of 32, an ore vein
 * is followed across stone and deepslate, the tool keeps half the wear and the hunger cost is gone, and the drops fly to the player.
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
 * <p>Its numbers are the rune's data: {@code max_blocks}, {@code exhaustion_per_block} (0: free),
 * {@code tool_wear_share} (the share of the normal wear the tool keeps).
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
        Predicate<BlockState> sameVein = sameVein(origin);
        List<BlockPos> vein = VeinSearch.connected(event.getPos(),
                pos -> level.isLoaded(pos) && sameVein.test(level.getBlockState(pos)),
                rune.get().count("max_blocks", 64) - 1);
        float exhaustion = (float) rune.get().value("exhaustion_per_block", 0.0);
        double wearShare = rune.get().value("tool_wear_share", 0.5);
        BREAKING.run(() -> breakAll(player, level, vein, exhaustion, wearShare));
    }

    /**
     * What counts as the same vein. An ore is matched by its <em>kind of ore</em> ({@code c:ores/iron} and so on), so a vein that
     * crosses from stone into deepslate, or iron ore beside deepslate iron ore, is one vein; anything else (a log) by its block.
     */
    private static Predicate<BlockState> sameVein(BlockState origin) {
        List<TagKey<Block>> kinds = origin.getTags()
                .filter(tag -> tag.location().getNamespace().equals("c") && tag.location().getPath().startsWith("ores/"))
                .toList();
        if (kinds.isEmpty()) {
            Block block = origin.getBlock();
            return state -> state.is(block);
        }
        return state -> kinds.stream().anyMatch(state::is);
    }

    /** Breaks the vein block by block, stopping as soon as the player can no longer afford the next. */
    private static void breakAll(ServerPlayer player, ServerLevel level, List<BlockPos> vein, float exhaustion, double wearShare) {
        double refund = 0.0;
        for (BlockPos pos : vein) {
            if (!canAfford(player, level.getBlockState(pos), exhaustion > 0.0f)) {
                break;
            }
            ItemStack tool = player.getMainHandItem();
            int worn = tool.getDamageValue();
            if (player.gameMode.destroyBlock(pos)) {
                if (exhaustion > 0.0f) {
                    player.causeFoodExhaustion(exhaustion);
                }
                // Only part of the wear is kept: the rest is given back, a point at a time, as it builds up.
                refund += (1.0 - wearShare) * Math.max(0, tool.getDamageValue() - worn);
                if (refund >= 1.0 && tool.isDamageableItem() && tool.getDamageValue() > 0) {
                    tool.setDamageValue(tool.getDamageValue() - 1);
                    refund -= 1.0;
                }
            }
        }
        collectDrops(player, level, vein);
    }

    /** The drops of the blocks just broken fly to the player instead of lying scattered round a hole. */
    private static void collectDrops(ServerPlayer player, ServerLevel level, List<BlockPos> vein) {
        if (vein.isEmpty()) {
            return;
        }
        AABB box = new AABB(vein.get(0));
        for (BlockPos pos : vein) {
            box = box.minmax(new AABB(pos));
        }
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, box.inflate(1.5), item -> item.tickCount <= 1)) {
            drop.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            drop.setNoPickUpDelay();
        }
    }

    /** Whether the player can break {@code state} with what they hold and have, without harm. */
    private static boolean canAfford(ServerPlayer player, BlockState state, boolean needsFood) {
        ItemStack tool = player.getMainHandItem();
        if (tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= 1) {
            return false;
        }

        return (!needsFood || player.getFoodData().getFoodLevel() > 0) && player.getMainHandItem().isCorrectToolForDrops(state);
    }
}
