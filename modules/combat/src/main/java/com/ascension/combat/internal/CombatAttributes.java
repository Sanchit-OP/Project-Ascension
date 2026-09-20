package com.ascension.combat.internal;

import com.ascension.combat.AscensionCombat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Attributes owned by this module &mdash; the stat model itself (docs/technical/combat-api.md
 * section 3), as opposed to {@link CombatAttachments}, which holds fight-local bookkeeping no
 * other system needs to read.
 */
public final class CombatAttributes {

    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, AscensionCombat.MOD_ID);

    /**
     * Reduces magic damage (combat-api.md section 2) the same role Armor plays for physical.
     * Divine Sunderer's magic component (section 6) is the first thing in the catalog to read
     * it; Ragefang, Darken Blade and Heartwood of Pallas will later.
     *
     * <p>Added to every living entity type via {@link EntityAttributeModificationEvent} below,
     * not just the player &mdash; a target with no attribute at all is treated as 0 resist by
     * {@link com.ascension.combat.internal.mechanics.MagicDamage}, so this is really about making
     * the stat visible/attributable rather than a strict correctness requirement.
     *
     * <p>Mitigated by {@link com.ascension.combat.internal.mechanics.DefenseFormula}: every 100
     * points halves the damage that gets through (Sanchit's own spec, 2026-09-19), which is why
     * the range goes to 300 &mdash; that's the point 87.5% reduction sits at.
     */
    public static final DeferredHolder<Attribute, Attribute> MAGIC_RESIST = ATTRIBUTES.register(
            "magic_resist",
            () -> new RangedAttribute("attribute.ascension_combat.magic_resist", 0.0, 0.0, 300.0)
                    .setSyncable(true));

    /**
     * Reduces physical damage (combat-api.md section 2), under the same {@link
     * com.ascension.combat.internal.mechanics.DefenseFormula} curve as {@link #MAGIC_RESIST}
     * &mdash; a deliberate second attribute, not a reuse of vanilla's own {@code
     * net.minecraft.world.entity.ai.attributes.Attributes#ARMOR}, because that attribute is
     * hard-capped at 30 (baked into the vanilla {@code RangedAttribute} instance itself, not
     * something this module can widen without a mixin banned in Tier 1), which can't express a
     * value like 300 under this curve. Vanilla's own Armor attribute still exists, still drives
     * the vanilla armor-bar HUD from actually worn armor pieces, and vanilla's own toughness-based
     * mitigation formula still technically runs against it &mdash; but {@link
     * com.ascension.combat.internal.mechanics.PhysicalDamage} discards that result outright and
     * replaces it with this attribute's own curve, so vanilla Armor no longer affects how much
     * damage actually lands.
     */
    public static final DeferredHolder<Attribute, Attribute> ARMOR = ATTRIBUTES.register(
            "armor",
            () -> new RangedAttribute("attribute.ascension_combat.armor", 0.0, 0.0, 300.0)
                    .setSyncable(true));

    /**
     * Percentage healing on any damage dealt, of either type (docs/technical/combat-api.md
     * section 3) &mdash; Lifesteal and Omnivamp collapsed into one stat, Sanchit's own call
     * 2026-09-20: nothing in this catalog can tell a physical hit from a magic one at the generic
     * point healing happens, so a real physical-only Lifesteal would need per-hit damage-type
     * tagging this project doesn't have. Every "lifesteal-like" item already behaved this way
     * regardless (Endless Hunger, Tasted Blood, Sterak's Harness), so this just makes that real
     * instead of three separate hardcoded percentages.
     *
     * <p>Read by the one shared {@link com.ascension.combat.internal.mechanics.OmnivampHealing}
     * listener every consumer heals through now, attacker-side, same as {@link #LETHALITY} and
     * {@link #PENETRATION_PERCENT} below.
     */
    public static final DeferredHolder<Attribute, Attribute> OMNIVAMP = ATTRIBUTES.register(
            "omnivamp",
            () -> new RangedAttribute("attribute.ascension_combat.omnivamp", 0.0, 0.0, 3.0)
                    .setSyncable(true));

    /**
     * Flat armor penetration, attacker-side (combat-api.md section 3) &mdash; League's own
     * "Lethality," applied after {@link #PENETRATION_PERCENT} against whatever Armor is left, and
     * only ever against Armor (nothing in this catalog pierces Magic Resist with a flat number).
     * Read centrally by {@link com.ascension.combat.internal.mechanics.PhysicalDamage} rather than
     * per-item &mdash; Wraithblade and Edge of Night are its two consumers, each contributing to
     * it while their passive/burst window is active instead of registering their own reduction
     * modifier the way they used to.
     */
    public static final DeferredHolder<Attribute, Attribute> LETHALITY = ATTRIBUTES.register(
            "lethality",
            () -> new RangedAttribute("attribute.ascension_combat.lethality", 0.0, 0.0, 200.0)
                    .setSyncable(true));

    /**
     * Percentage Armor penetration, attacker-side (combat-api.md section 3). Briefly unified with
     * {@link #MAGIC_PENETRATION_PERCENT} into one unversal "Penetration" stat on 2026-09-20, split
     * back out the same day once Sanchit asked for the character sheet to show Armor Pen and Magic
     * Pen as separate lines, the same way League itself tracks them &mdash; a display request that
     * only makes sense if they're genuinely two different numbers underneath. Applied before {@link
     * #LETHALITY}, percent first then flat off whatever's left, matching League's own penetration
     * order. Read centrally by {@link com.ascension.combat.internal.mechanics.PhysicalDamage} rather
     * than per-item. Mortal Reminder's arrow-specific ignore is deliberately <em>not</em> folded in
     * here; see that class's own javadoc for why it stays its own reduction modifier, composing on
     * top of this one.
     */
    public static final DeferredHolder<Attribute, Attribute> ARMOR_PENETRATION_PERCENT = ATTRIBUTES.register(
            "armor_penetration_percent",
            () -> new RangedAttribute("attribute.ascension_combat.armor_penetration_percent", 0.0, 0.0, 1.0)
                    .setSyncable(true));

    /**
     * Percentage Magic Resist penetration, attacker-side (combat-api.md section 3) &mdash; the
     * magic-side counterpart to {@link #ARMOR_PENETRATION_PERCENT}, tracked as its own attribute
     * rather than sharing one universal Penetration stat; see that attribute's own javadoc for why.
     * Read centrally by {@link com.ascension.combat.internal.mechanics.MagicDamage}. Wizard's Agony
     * contributes to both this and {@link #ARMOR_PENETRATION_PERCENT} independently.
     */
    public static final DeferredHolder<Attribute, Attribute> MAGIC_PENETRATION_PERCENT = ATTRIBUTES.register(
            "magic_penetration_percent",
            () -> new RangedAttribute("attribute.ascension_combat.magic_penetration_percent", 0.0, 0.0, 1.0)
                    .setSyncable(true));

    private CombatAttributes() {
    }

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        modBus.addListener(CombatAttributes::onAttributeModification);
    }

    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(entityType -> {
            event.add(entityType, MAGIC_RESIST);
            event.add(entityType, ARMOR);
            event.add(entityType, OMNIVAMP);
            event.add(entityType, LETHALITY);
            event.add(entityType, ARMOR_PENETRATION_PERCENT);
            event.add(entityType, MAGIC_PENETRATION_PERCENT);
        });
    }
}
