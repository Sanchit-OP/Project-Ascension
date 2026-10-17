package com.ascension.combat.internal.stat;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.entity.EquipmentSlotGroup;
import org.jetbrains.annotations.Nullable;

/**
 * One item's entry in the {@code ascension_combat:item_stats} data map
 * (docs/technical/item-stats.md section 4): the weapon's own attack, stats added on top, and
 * where they count.
 *
 * <pre>{@code
 * { "weapon": { "damage": 7, "speed": 1.6 }, "stats": { "armor": 15, "omnivamp": 8 }, "slot": "feet" }
 * }</pre>
 *
 * <p>Every part is optional. Values are written the way a player reads them: weapon damage as the
 * tooltip shows it (a bare hand's 1 included), speed in attacks per second, and stats as {@link
 * CombatStat} writes them.
 *
 * <p><b>Merging.</b> Two entries can reach one item: a tag's and the item's own, possibly from
 * different mods' files. NeoForge merges them in load order, so a tag in one jar could overwrite
 * another jar's numbers for its own item. {@link #merge} decides by specificity instead, one field
 * at a time ({@link SpecificityPicker}), and a merged value remembers which of its fields came from
 * an item entry ({@link #specificFields}).
 */
public final class ItemStats {

    private static final String WEAPON_DAMAGE = "weapon.damage";
    private static final String WEAPON_SPEED = "weapon.speed";
    private static final String SLOT = "slot";
    private static final String STAT_PREFIX = "stats.";

    private record Weapon(Optional<Double> damage, Optional<Double> speed) {
        static final Codec<Weapon> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("damage").forGetter(Weapon::damage),
                Codec.DOUBLE.optionalFieldOf("speed").forGetter(Weapon::speed)
        ).apply(i, Weapon::new));
    }

    public static final Codec<ItemStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Weapon.CODEC.optionalFieldOf("weapon").forGetter(ItemStats::weaponForCodec),
            Codec.unboundedMap(CombatStats.CODEC, Codec.DOUBLE).optionalFieldOf("stats", Map.of()).forGetter(ItemStats::stats),
            EquipmentSlotGroup.CODEC.optionalFieldOf("slot").forGetter(ItemStats::slot)
    ).apply(i, (weapon, stats, slot) -> new ItemStats(
            weapon.flatMap(Weapon::damage), weapon.flatMap(Weapon::speed), stats, slot, null)));

    private final Optional<Double> weaponDamage;
    private final Optional<Double> weaponSpeed;
    private final Map<CombatStat, Double> stats;
    private final Optional<EquipmentSlotGroup> slot;

    /** The fields an item's own entry decided, once this value is a merge's result; null before. */
    @Nullable
    private final Set<String> specificFields;

    private ItemStats(Optional<Double> weaponDamage, Optional<Double> weaponSpeed, Map<CombatStat, Double> stats,
            Optional<EquipmentSlotGroup> slot, @Nullable Set<String> specificFields) {
        this.weaponDamage = weaponDamage;
        this.weaponSpeed = weaponSpeed;
        this.stats = Collections.unmodifiableMap(new LinkedHashMap<>(stats));
        this.slot = slot;
        this.specificFields = specificFields;
    }

    /** The weapon's own damage, as its tooltip shows it. Replaces the item's own number. */
    public Optional<Double> weaponDamage() {
        return weaponDamage;
    }

    /** The weapon's own speed, in attacks per second. Replaces the item's own number. */
    public Optional<Double> weaponSpeed() {
        return weaponSpeed;
    }

    /** Stats added on top, written as {@link CombatStat} writes them. */
    public Map<CombatStat, Double> stats() {
        return stats;
    }

    /** Where the stats count. Empty means the item's own armour slot, or else the main hand. */
    public Optional<EquipmentSlotGroup> slot() {
        return slot;
    }

    /** Whether this entry names {@code stat}. A named stat replaces what conversion would give for it. */
    public boolean names(CombatStat stat) {
        return stats.containsKey(stat);
    }

    private Optional<Weapon> weaponForCodec() {
        return weaponDamage.isEmpty() && weaponSpeed.isEmpty()
                ? Optional.empty()
                : Optional.of(new Weapon(weaponDamage, weaponSpeed));
    }

    private Set<String> fieldKeys() {
        Set<String> keys = new LinkedHashSet<>();
        weaponDamage.ifPresent(v -> keys.add(WEAPON_DAMAGE));
        weaponSpeed.ifPresent(v -> keys.add(WEAPON_SPEED));
        slot.ifPresent(v -> keys.add(SLOT));
        stats.keySet().forEach(stat -> keys.add(STAT_PREFIX + stat.name()));
        return keys;
    }

    /**
     * Merges two entries for the same item, field by field. {@code first} loaded earlier;
     * {@code firstNamesItem} says whether its entry named the item itself rather than a tag (for a
     * value that is already a merge's result, its own record of that wins). See the class javadoc.
     */
    public static ItemStats merge(ItemStats first, boolean firstNamesItem, ItemStats second, boolean secondNamesItem) {
        SpecificityPicker<String> picker = new SpecificityPicker<>(
                first.specificFields, firstNamesItem, first.fieldKeys(),
                second.specificFields, secondNamesItem, second.fieldKeys());

        Map<CombatStat, Double> stats = new LinkedHashMap<>();
        Set<CombatStat> allStats = new LinkedHashSet<>(first.stats.keySet());
        allStats.addAll(second.stats.keySet());
        for (CombatStat stat : allStats) {
            picker.pick(STAT_PREFIX + stat.name(),
                    Optional.ofNullable(first.stats.get(stat)),
                    Optional.ofNullable(second.stats.get(stat)))
                    .ifPresent(value -> stats.put(stat, value));
        }

        return new ItemStats(
                picker.pick(WEAPON_DAMAGE, first.weaponDamage, second.weaponDamage),
                picker.pick(WEAPON_SPEED, first.weaponSpeed, second.weaponSpeed),
                stats,
                picker.pick(SLOT, first.slot, second.slot),
                picker.recorded());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ItemStats that
                && weaponDamage.equals(that.weaponDamage)
                && weaponSpeed.equals(that.weaponSpeed)
                && stats.equals(that.stats)
                && slot.equals(that.slot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weaponDamage, weaponSpeed, stats, slot);
    }

    @Override
    public String toString() {
        return "ItemStats[weaponDamage=" + weaponDamage + ", weaponSpeed=" + weaponSpeed
                + ", stats=" + stats + ", slot=" + slot + "]";
    }
}
