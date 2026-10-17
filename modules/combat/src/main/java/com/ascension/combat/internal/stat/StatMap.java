package com.ascension.combat.internal.stat;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Stats by name, as a data map value: {@code { "armor": 100, "magic_resist": 40 }}. One mob type's
 * entry in {@code ascension_combat:mob_stats} (docs/technical/item-stats.md section 9).
 *
 * <p>Merged under the same rule as item stats ({@link SpecificityPicker}): an entry naming the mob
 * type beats its tier tag's, one stat at a time, whichever file loaded first.
 */
public final class StatMap {

    public static final Codec<StatMap> CODEC = Codec.unboundedMap(CombatStats.CODEC, Codec.DOUBLE)
            .xmap(stats -> new StatMap(stats, null), StatMap::stats);

    private final Map<CombatStat, Double> stats;

    /** The stats an entry naming the element itself decided, once this value is a merge's result. */
    @Nullable
    private final Set<CombatStat> specific;

    private StatMap(Map<CombatStat, Double> stats, @Nullable Set<CombatStat> specific) {
        this.stats = Collections.unmodifiableMap(new LinkedHashMap<>(stats));
        this.specific = specific;
    }

    /** Each stat as data writes it. */
    public Map<CombatStat, Double> stats() {
        return stats;
    }

    /** Merges two entries for one element; see the class javadoc. {@code first} loaded earlier. */
    public static StatMap merge(StatMap first, boolean firstNamesElement, StatMap second, boolean secondNamesElement) {
        SpecificityPicker<CombatStat> picker = new SpecificityPicker<>(
                first.specific, firstNamesElement, first.stats.keySet(),
                second.specific, secondNamesElement, second.stats.keySet());

        Map<CombatStat, Double> merged = new LinkedHashMap<>();
        Set<CombatStat> all = new LinkedHashSet<>(first.stats.keySet());
        all.addAll(second.stats.keySet());
        for (CombatStat stat : all) {
            picker.pick(stat, Optional.ofNullable(first.stats.get(stat)), Optional.ofNullable(second.stats.get(stat)))
                    .ifPresent(value -> merged.put(stat, value));
        }
        return new StatMap(merged, picker.recorded());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StatMap that && stats.equals(that.stats);
    }

    @Override
    public int hashCode() {
        return stats.hashCode();
    }

    @Override
    public String toString() {
        return "StatMap" + stats;
    }
}
