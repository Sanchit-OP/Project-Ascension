package com.ascension.combat.internal.mechanics;

import com.ascension.combat.AscensionCombat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Temporary-HP pool that soaks damage before real HP (docs/technical/combat-api.md section 5),
 * shared by Darken Blade, Edge of Night, Gargoyle Stoneplate, and Sterak's Harness. Wraps vanilla's
 * own absorption (the golden hearts) rather than a hand-rolled pool &mdash; it already renders and
 * already soaks damage first.
 *
 * <p>Found necessary by Sanchit's own test 2026-09-20: Darken Blade's shield never appeared against
 * an Iron Golem hit, because every one of this catalog's four shield items called {@code
 * LivingEntity#setAbsorptionAmount} directly, and that setter <em>silently clamps to {@link
 * Attributes#MAX_ABSORPTION}</em> (confirmed in vanilla's own {@code Attributes.java}: a bare {@code
 * RangedAttribute(0.0, 0.0, 2048.0)}) &mdash; which defaults to zero for every entity unless
 * something raises it first, the same way vanilla's own Absorption potion effect raises it
 * internally before granting absorption. None of the four items ever did, so all four have been
 * granting a shield that gets clamped straight back to zero the instant it's set.
 */
public final class Shield {

    private static final ResourceLocation CEILING_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "shield_max_absorption_ceiling");

    /** Comfortably above the largest single shield this catalog grants (Gargoyle Stoneplate's 40). */
    private static final double MAX_ABSORPTION_CEILING = 100.0;

    private Shield() {
    }

    /** Grants {@code amount} of shield, never lowering whatever the entity already has. */
    public static void grant(LivingEntity entity, float amount) {
        ensureCeiling(entity);
        entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(), amount));
    }

    private static void ensureCeiling(LivingEntity entity) {
        AttributeInstance maxAbsorption = entity.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbsorption == null || maxAbsorption.hasModifier(CEILING_MODIFIER_ID)) {
            return;
        }

        maxAbsorption.addPermanentModifier(new AttributeModifier(
                CEILING_MODIFIER_ID, MAX_ABSORPTION_CEILING, AttributeModifier.Operation.ADD_VALUE));
    }
}
