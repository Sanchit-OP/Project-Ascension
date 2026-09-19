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
        });
    }
}
