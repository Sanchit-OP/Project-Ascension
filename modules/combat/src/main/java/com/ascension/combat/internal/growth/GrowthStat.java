package com.ascension.combat.internal.growth;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * One stat a growth source gives (docs/progression/growth.md section 9): an attribute, named by id,
 * and an amount added to it.
 *
 * <p>The attribute is a plain id rather than a {@code Holder<Attribute>} on purpose. A holder codec
 * fails the whole file when the attribute isn't registered, so one node naming another mod's
 * attribute would take the node down with it. As an id, a missing attribute is skipped where stats
 * are applied ({@link PlayerGrowth}) and reported once by {@link GrowthDataCheck}, and growth can
 * name any mod's attribute without depending on that mod (ADR-0003, ADR-0011).
 *
 * @param operation how the amount combines, exactly as vanilla's own modifiers do: flat
 *     ({@code add_value}, the default) or a percentage of the base ({@code add_multiplied_base})
 */
public record GrowthStat(ResourceLocation attribute, double amount, AttributeModifier.Operation operation) {

    public static final Codec<GrowthStat> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ResourceLocation.CODEC.fieldOf("attribute").forGetter(GrowthStat::attribute),
                    Codec.DOUBLE.fieldOf("amount").forGetter(GrowthStat::amount),
                    AttributeModifier.Operation.CODEC
                            .optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
                            .forGetter(GrowthStat::operation))
            .apply(instance, GrowthStat::new));

    /** Which modifier this stat adds into: one per attribute and operation. */
    public StatKey key() {
        return new StatKey(attribute, operation);
    }

    /** An attribute and an operation: the unit growth's totals are summed and applied in. */
    public record StatKey(ResourceLocation attribute, AttributeModifier.Operation operation) {
    }
}
