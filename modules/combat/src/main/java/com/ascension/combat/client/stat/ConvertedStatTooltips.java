package com.ascension.combat.client.stat;

import com.ascension.combat.internal.stat.StatDataMaps;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.GatherSkippedAttributeTooltipsEvent;

/**
 * Hides the vanilla lines an item's stats were converted from (docs/technical/item-stats.md
 * section 5), so a diamond chestplate shows "+28 Armor" once rather than vanilla's "+8 Armor" above
 * it under the same name.
 *
 * <p>NeoForge can only skip a line by modifier id, and vanilla armour gives its Armor, Toughness and
 * Knockback Resistance one shared id per piece. So netherite's knockback line hides too, until the
 * League-style tooltip (increment 4) draws every line itself.
 */
public final class ConvertedStatTooltips {

    private ConvertedStatTooltips() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ConvertedStatTooltips::onGatherSkipped);
    }

    private static void onGatherSkipped(GatherSkippedAttributeTooltipsEvent event) {
        for (ItemAttributeModifiers.Entry entry : event.getStack().getAttributeModifiers().modifiers()) {
            if (entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE
                    && entry.attribute().getData(StatDataMaps.STAT_CONVERSIONS) != null) {
                event.skipId(entry.modifier().id());
            }
        }
    }
}
