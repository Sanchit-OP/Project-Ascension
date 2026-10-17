package com.ascension.combat.internal.rune;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.internal.CombatAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The stat shards (docs/technical/rune-system.md section 5): small plain stats in three rows,
 * out of the paths' way. A shard is a rune whose {@code effect} names the stat it gives
 * ({@code ascension_combat:shard/attack_damage}) and whose {@code amount} is that stat written the
 * way a player says it: 1 Attack Damage, 8 (percent) Attack Speed, 5 Armor. So a datapack adds a
 * shard for any stat in the stat system with a data file and no code.
 *
 * <p>Each row owns one modifier id ({@code ascension_combat:rune/shard_<row>}), so two shards of
 * one stat in different rows add up and swapping a row's shard takes the old one off. Event-driven:
 * {@link #refresh} runs from {@link RuneModifiers#refresh}, never per tick.
 */
public final class ShardEffects {

    private static final String EFFECT_PREFIX = "shard/";

    private ShardEffects() {
    }

    /** The stat a shard gives, from its effect: {@code shard/armor} is Armor. */
    public static Optional<CombatStat> statOf(RuneDefinition shard) {
        String path = shard.effect().getPath();
        return path.startsWith(EFFECT_PREFIX) ? CombatStats.byName(path.substring(EFFECT_PREFIX.length())) : Optional.empty();
    }

    /** Puts each row's shard on the player and takes off what is no longer chosen. */
    public static void refresh(ServerPlayer player) {
        Map<Integer, RuneDefinition> byRow = new HashMap<>();
        for (RuneDefinition rune : player.getData(CombatAttachments.RUNE_PAGE).resolve(Runes.registry(player))) {
            if (rune.slot() == RuneSlot.SHARD) {
                byRow.put(rune.row(), rune);
            }
        }

        for (int row = 0; row < RuneDefinition.SHARD_ROWS; row++) {
            ResourceLocation id = Runes.id("rune/shard_" + row);
            RuneDefinition shard = byRow.get(row);
            Optional<CombatStat> chosen = shard == null ? Optional.empty() : statOf(shard);
            for (CombatStat stat : CombatStats.all()) {
                RuneModifiers.setStat(player, stat, id, chosen.isPresent() && chosen.get() == stat ? shard.value("amount", 0.0) : 0.0);
            }
        }
    }
}
