package com.ascension.combat.api.stat;

import com.ascension.combat.internal.stat.StatBreakdowns;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/**
 * The one way to read a stat (docs/technical/item-stats.md section 6, ADR-0019). Everything that
 * scales off a stat reads it here: items, runes, growth, other mods.
 *
 * <p><b>There is no base/bonus split</b> (Sanchit, 2026-10-15). A stat is one total. An item that
 * "deals 50% of your Attack Damage" reads {@code value(entity, CombatStats.ATTACK_DAMAGE)}: a bare
 * hand's 1 plus everything else.
 *
 * <p>{@link #value} is in the attribute's own units:
 * <ul>
 *   <li>Health, Attack Damage, Armor, Magic Resist and Lethality in points;</li>
 *   <li>Attack Speed in attacks per second;</li>
 *   <li>Move Speed in vanilla's units: a player walks at 0.1;</li>
 *   <li>Omnivamp and both Penetrations as fractions: 0.08 is 8%.</li>
 * </ul>
 *
 * <p>Works on both sides. On a client, it reads the local player's stats as the server synced
 * them. Every stat is synced; vanilla's Attack Damage is marked syncable for this at startup.
 */
public final class StatReader {

    private StatReader() {
    }

    /** The stat's total, or 0 if the entity doesn't have it. */
    public static double value(LivingEntity entity, CombatStat stat) {
        AttributeInstance instance = entity.getAttribute(stat.attribute());
        return instance == null ? 0.0 : instance.getValue();
    }

    /** Where the stat's total comes from, source by source. For display, not for gameplay maths. */
    public static StatBreakdown breakdown(LivingEntity entity, CombatStat stat) {
        return StatBreakdowns.of(entity, stat);
    }
}
