package com.ascension.combat.api;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * The stable, public handles for this module's two defensive stats (docs/technical/combat-api.md
 * section 3) &mdash; Armor and Magic Resist, both under {@code internal.mechanics.DefenseFormula}.
 * A third-party consumer (a Tier 2 compat jar reading a target's current stat, say) belongs here,
 * never importing {@code com.ascension.combat.internal.CombatAttributes} directly, per this
 * package's own contract (see {@code package-info.java}): the registration machinery is free to
 * change, these two references are not.
 *
 * <p>Not imported by simple name on purpose &mdash; {@code internal.CombatAttributes} shares this
 * class's own name, so every reference below is fully qualified instead of importable, which also
 * makes the wrapping obvious at the call site.
 *
 * <p>First real consumer: {@code ascension-compat-jade}, which reads both to add Armor/Magic
 * Resist lines to Jade's entity tooltip.
 */
public final class CombatAttributes {

    private CombatAttributes() {
    }

    public static Holder<Attribute> armor() {
        return com.ascension.combat.internal.CombatAttributes.ARMOR;
    }

    public static Holder<Attribute> magicResist() {
        return com.ascension.combat.internal.CombatAttributes.MAGIC_RESIST;
    }
}
