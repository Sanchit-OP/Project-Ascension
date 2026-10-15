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
 * <p>Every stat is written the way a player says it. A flat stat's {@code 15} is fifteen points;
 * a percentage stat's {@code 8} is 8%, which {@link #modifier} turns into the attribute's own
 * {@code 0.08}. Nothing outside this class should know which stats store fractions.
 *
 * <p>The attribute is resolved lazily, so naming a stat (in a codec, say) never touches the
 * attribute registry.
 */
public final class CombatStat {

    /** How the stat is written and shown. */
    public enum Format {
        /** Points: {@code 15} is 15. */
        FLAT,
        /** A percentage: {@code 8} is 8%. */
        PERCENT
    }

    private final String name;
    private final Supplier<Holder<Attribute>> attribute;
    private final Format format;
    private final AttributeModifier.Operation operation;

    CombatStat(String name, Supplier<Holder<Attribute>> attribute, Format format, AttributeModifier.Operation operation) {
        this.name = name;
        this.attribute = attribute;
        this.format = format;
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

    public Format format() {
        return format;
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
        return format == Format.PERCENT ? written / 100.0 : written;
    }

    /** The inverse of {@link #toAmount}. */
    public double toWritten(double amount) {
        return format == Format.PERCENT ? amount * 100.0 : amount;
    }

    @Override
    public String toString() {
        return name;
    }
}
