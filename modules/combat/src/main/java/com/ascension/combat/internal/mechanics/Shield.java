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

    /** Makes the shields an entity is granted stronger or weaker; Revitalize is the first. */
    @FunctionalInterface
    public interface Booster {
        float scale(LivingEntity entity, float amount);
    }

    private static final java.util.List<Booster> BOOSTERS = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addBooster(Booster booster) {
        BOOSTERS.add(booster);
    }

    /** Grants {@code amount} of shield (after boosters), never lowering whatever the entity already has. */
    public static void grant(LivingEntity entity, float amount) {
        ensureCeiling(entity);
        float scaled = amount;
        for (Booster booster : BOOSTERS) {
            scaled = booster.scale(entity, scaled);
        }
        entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(), scaled));
    }

    /**
     * Adds {@code amount} of shield (after boosters) on top of whatever the entity already has, up to {@code cap} and no further.
     * {@link #grant} sets a shield to a figure; this is for a source that tops one up a little at a time (Overheal).
     */
    public static void add(LivingEntity entity, float amount, float cap) {
        ensureCeiling(entity);
        float scaled = amount;
        for (Booster booster : BOOSTERS) {
            scaled = booster.scale(entity, scaled);
        }
        float room = Math.max(0.0f, cap - entity.getAbsorptionAmount());
        entity.setAbsorptionAmount(entity.getAbsorptionAmount() + Math.min(scaled, room));
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
