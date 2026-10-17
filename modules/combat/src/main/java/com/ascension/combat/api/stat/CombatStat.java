package com.ascension.combat.api.stat;

import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * One stat in the combat stat model (docs/technical/item-stats.md section 3): its name in data
 * files, the attribute it lives on, and how a number written in data becomes a modifier.
 *
 * <p>Every stat is written the way a player says it: {@code 15} Armor, {@code 8}% Omnivamp,
 * {@code 20}% Attack Speed. {@link #modifier} turns that into the attribute's own amount. Nothing
 * outside this class should know which stats store fractions.
 *
 * <p>The attribute is resolved lazily, so naming a stat (in a codec, say) never touches the
 * attribute registry.
 */
public final class CombatStat {

    /** How a stat is written, and how its total reads. */
    public enum Kind {
        /** Points: {@code 15} Armor is written 15 and reads 15. */
        POINTS,
        /** A share: {@code 8} is 8%, and the total reads as the fraction 0.08. Omnivamp, Penetration. */
        PERCENT,
        /**
         * A speed: a bonus is written as a percentage of the speed ({@code 20} is +20%), and the
         * total reads as the speed itself, in attacks per second or vanilla's walking units.
         */
        SPEED
    }

    private final String name;
    private final Supplier<Holder<Attribute>> attribute;
    private final Kind kind;
    private final AttributeModifier.Operation operation;

    CombatStat(String name, Supplier<Holder<Attribute>> attribute, Kind kind, AttributeModifier.Operation operation) {
        this.name = name;
        this.attribute = attribute;
        this.kind = kind;
        this.operation = operation;
    }

    /** The stat's name in data files: {@code armor}, {@code attack_speed}. */
    public String name() {
        return name;
    }

    /** The attribute this stat lives on. A stat is given by adding a modifier to it. */
    public Holder<Attribute> attribute() {
        return attribute.get();
    }

    public Kind kind() {
        return kind;
    }

    /**
     * A modifier giving {@code written} of this stat, written the way data files and tooltips
     * write it. Use this rather than building an {@link AttributeModifier} by hand, so the
     * operation and the percentage scale always match the rest of the stat model.
     */
    public AttributeModifier modifier(ResourceLocation id, double written) {
        return new AttributeModifier(id, toAmount(written), operation);
    }

    /** A written value as the attribute's own modifier amount: {@code 8}% becomes {@code 0.08}. */
    public double toAmount(double written) {
        return kind == Kind.POINTS ? written : written / 100.0;
    }

    /** The inverse of {@link #toAmount}. */
    public double toWritten(double amount) {
        return kind == Kind.POINTS ? amount : amount * 100.0;
    }

    @Override
    public String toString() {
        return name;
    }
}
