package com.ascension.combat.internal.stat;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.AdvancedDataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapValueMerger;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * The data maps behind item stats (docs/technical/item-stats.md, ADR-0019). The project's first
 * data maps. Every pack's file for one of them sits at the same path,
 * {@code data/ascension_combat/data_maps/<registry>/<name>.json}, and they stack in pack order.
 *
 * <p>Both are synced to clients: tooltips are drawn there, and they ask the same
 * {@code ItemAttributeModifierEvent} the server does.
 */
public final class StatDataMaps {

    /**
     * Stats per item or item tag, {@code data_maps/item/item_stats.json}. Merged by {@link
     * ItemStats#merge}, so an entry naming the item beats a tag's whatever order the files load in.
     */
    public static final AdvancedDataMapType<Item, ItemStats, ?> ITEM_STATS = AdvancedDataMapType
            .builder(id("item_stats"), Registries.ITEM, ItemStats.CODEC)
            .synced(ItemStats.CODEC, false)
            .merger((DataMapValueMerger<Item, ItemStats>) (registry, first, firstValue, second, secondValue) ->
                    ItemStats.merge(firstValue, first.right().isPresent(), secondValue, second.right().isPresent()))
            .build();

    /**
     * What one point of a vanilla attribute is worth in our stats,
     * {@code data_maps/attribute/stat_conversions.json}: {@code "minecraft:generic.armor":
     * {"armor": 3.5, "magic_resist": 2.5}}. Applied to every item from every mod that carries that
     * attribute, so no mod's armour needs a file of its own. A later pack's entry adds or replaces
     * stats one at a time rather than replacing the whole entry.
     */
    public static final AdvancedDataMapType<Attribute, Map<CombatStat, Double>, ?> STAT_CONVERSIONS = AdvancedDataMapType
            .builder(id("stat_conversions"), Registries.ATTRIBUTE, conversionCodec())
            .synced(conversionCodec(), false)
            .merger(DataMapValueMerger.mapMerger())
            .build();

    private StatDataMaps() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(StatDataMaps::onRegisterDataMapTypes);
    }

    private static void onRegisterDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(ITEM_STATS);
        event.register(STAT_CONVERSIONS);
    }

    private static Codec<Map<CombatStat, Double>> conversionCodec() {
        return Codec.unboundedMap(CombatStats.CODEC, Codec.DOUBLE);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, path);
    }
}
