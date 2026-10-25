package com.ascension.worlds.internal;

import com.ascension.worlds.AscensionWorlds;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A cartographer sells a map to the nearest Ancient City (Sanchit, 2026-10-25: the city is something the game needs and it is very hard to find).
 *
 * <p>It is vanilla's explorer-map trade (the one that sells the ocean and woodland maps) pointed at a structure tag of ours: the map is made when the offer is
 * generated, from where the villager stands, and its marker is the nearest city. Vanilla's own class skips any structure whose chunks already exist, which
 * suits a monument seen from the surface but not a city under the deep dark (a generated chunk is not a found city, and each search would use up the
 * nearest one), so the offer is built here with the search that skips nothing. A villager picks two trades at random from each level's list, so this one
 * is offered at three levels (apprentice, journeyman, expert) and never twice to the same villager: a cartographer who reaches expert has almost
 * certainly had it. No other mod is involved, so it lives here.
 */
public final class ExplorerMaps {

    public static final TagKey<Structure> ON_ANCIENT_CITY_MAPS = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "on_ancient_city_maps"));
    /** The map's name, in the worlds lang file. */
    public static final String NAME_KEY = "filled_map.ascension_worlds.ancient_city";

    /** Vanilla's ocean map costs 13 emeralds and a compass; the city is further and rarer. */
    private static final int EMERALDS = 14;
    private static final int MAX_USES = 6;
    private static final int VILLAGER_XP = 8;
    /** Vanilla's explorer maps look a hundred chunks out and are drawn at a scale of 2 (one pixel is four blocks). */
    private static final int SEARCH_CHUNKS = 100;
    private static final byte MAP_SCALE = 2;
    private static final float PRICE_MULTIPLIER = 0.2F;
    private static final int FIRST_LEVEL = 2;
    private static final int LAST_LEVEL = 4;

    private ExplorerMaps() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ExplorerMaps::onVillagerTrades);
    }

    private static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.CARTOGRAPHER) {
            return;
        }
        addTo(event.getTrades());
    }

    /** Adds the trade to a cartographer's lists; split from the event so a server check can run it on a map of its own. */
    static void addTo(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades) {
        for (int level = FIRST_LEVEL; level <= LAST_LEVEL; level++) {
            trades.computeIfAbsent(level, l -> new ArrayList<>()).add(new AncientCityMap());
        }
    }

    /** True when the villager already sells the map: it is not offered a second time at the next level. */
    static boolean sells(AbstractVillager villager) {
        Component name = Component.translatable(NAME_KEY);
        for (MerchantOffer offer : villager.getOffers()) {
            if (offer.getResult().is(Items.FILLED_MAP) && name.equals(offer.getResult().get(DataComponents.ITEM_NAME))) {
                return true;
            }
        }
        return false;
    }

    private static final class AncientCityMap implements VillagerTrades.ItemListing {

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            if (trader instanceof AbstractVillager villager && sells(villager)) {
                return null;
            }
            if (!(trader.level() instanceof ServerLevel level)) {
                return null;
            }
            // Null when no city lies within a hundred chunks: the villager then has no map to sell this time.
            BlockPos city = level.findNearestMapStructure(ON_ANCIENT_CITY_MAPS, trader.blockPosition(), SEARCH_CHUNKS, false);
            if (city == null) {
                return null;
            }
            ItemStack map = MapItem.create(level, city.getX(), city.getZ(), MAP_SCALE, true, true);
            MapItem.renderBiomePreviewMap(level, map);
            MapItemSavedData.addTargetDecoration(map, city, "+", MapDecorationTypes.RED_X);
            map.set(DataComponents.ITEM_NAME, Component.translatable(NAME_KEY));
            return new MerchantOffer(new ItemCost(Items.EMERALD, EMERALDS), Optional.of(new ItemCost(Items.COMPASS)), map, MAX_USES, VILLAGER_XP, PRICE_MULTIPLIER);
        }
    }
}
