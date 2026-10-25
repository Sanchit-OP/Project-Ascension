package com.ascension.worlds.internal;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * A development aid, dormant unless a file named {@code explorer-map-checks.txt} sits in the game directory: on a dedicated server it checks the
 * cartographer's map to the nearest Ancient City ({@link ExplorerMaps}): the structure tag, the three levels it is offered at and no other
 * profession's, a real offer made from where a villager stands (a filled map with a marker, 14 emeralds and a compass), the same villager not
 * being offered it twice, and a crowd of real cartographers each leaving with it at most once. Prints {@code [explorer-map-checks]} lines and stops the server.
 */
public final class ExplorerMapChecks {

    private static final String TRIGGER = "explorer-map-checks.txt";
    private static final int CARTOGRAPHERS = 10;

    private static MinecraftServer server;
    private static boolean armed;
    private static int waitTicks;
    private static int checks;
    private static int failures;

    private ExplorerMapChecks() {
    }

    public static void register(IEventBus gameBus) {
        Path trigger = FMLPaths.GAMEDIR.get().resolve(TRIGGER);
        if (!Files.exists(trigger)) {
            return;
        }
        try {
            Files.deleteIfExists(trigger);
        } catch (IOException e) {
            return;
        }
        armed = true;
        gameBus.addListener((ServerStartedEvent event) -> server = event.getServer());
        gameBus.addListener(ExplorerMapChecks::onTick);
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (!armed || server == null || ++waitTicks < 100) {
            return;
        }
        armed = false;
        try {
            run(server.overworld());
        } catch (RuntimeException e) {
            failures++;
            checks++;
            log("FAIL an exception: " + e);
            e.printStackTrace();
        }
        log("RESULT: " + checks + " checks, " + failures + " failures");
        server.halt(false);
    }

    private static void run(ServerLevel level) {
        // The tag names exactly the Ancient City.
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<String> tagged = new ArrayList<>();
        structures.getTag(ExplorerMaps.ON_ANCIENT_CITY_MAPS).ifPresent(set -> set.forEach(holder -> tagged.add(holder.unwrapKey().orElseThrow().location().toString())));
        expect("the structure tag names the Ancient City and nothing else: " + tagged, tagged.equals(List.of("minecraft:ancient_city")));

        // The trade is added at levels 2, 3 and 4, once each, and the event hands it only to cartographers (other mods' listeners expect all five levels to exist).
        Int2ObjectMap<List<VillagerTrades.ItemListing>> cartographer = fiveLevels();
        NeoForge.EVENT_BUS.post(new VillagerTradesEvent(cartographer, VillagerProfession.CARTOGRAPHER));
        Int2ObjectMap<List<VillagerTrades.ItemListing>> farmer = fiveLevels();
        NeoForge.EVENT_BUS.post(new VillagerTradesEvent(farmer, VillagerProfession.FARMER));
        expect("a cartographer gets the map trade once at levels 2, 3 and 4 and not at 1 or 5",
                ours(cartographer, 1) == 0 && ours(cartographer, 2) == 1 && ours(cartographer, 3) == 1 && ours(cartographer, 4) == 1 && ours(cartographer, 5) == 0);
        expect("a farmer gets none", java.util.stream.IntStream.rangeClosed(1, 5).allMatch(l -> ours(farmer, l) == 0));

        // A real offer, made from where a villager stands.
        BlockPos spawn = level.getSharedSpawnPos();
        Villager villager = cartographer(level, spawn, 2);
        villager.overrideOffers(new MerchantOffers());
        VillagerTrades.ItemListing listing = cartographer.get(2).stream().filter(ExplorerMapChecks::isOurs).findFirst().orElseThrow();
        long begun = System.currentTimeMillis();
        MerchantOffer offer = listing.getOffer(villager, level.random);
        log("the nearest-city search took " + (System.currentTimeMillis() - begun) + " ms");
        expect("a cartographer is offered a map when a city lies within a hundred chunks", offer != null);
        if (offer != null) {
            var map = offer.getResult();
            expect("the offer is a filled map named for the Ancient City",
                    map.is(Items.FILLED_MAP) && Component.translatable(ExplorerMaps.NAME_KEY).equals(map.get(DataComponents.ITEM_NAME)));
            expect("it costs 14 emeralds and a compass",
                    offer.getItemCostA().item().value() == Items.EMERALD && offer.getItemCostA().count() == 14
                            && offer.getItemCostB().isPresent() && offer.getItemCostB().get().item().value() == Items.COMPASS);
            expect("it can be bought 6 times", offer.getMaxUses() == 6);
            // The marker is a component on the stack ("+"), which the map's saved data picks up when a player first holds it.
            MapDecorations marker = map.get(DataComponents.MAP_DECORATIONS);
            MapDecorations.Entry target = marker == null ? null : marker.decorations().get("+");
            expect("the map carries a marker on the city", target != null && target.type().is(MapDecorationTypes.RED_X.unwrapKey().orElseThrow()));
            if (target != null) {
                double distance = Math.hypot(target.x() - spawn.getX(), target.z() - spawn.getZ());
                log("the marker is at (" + Math.round(target.x()) + ", " + Math.round(target.z()) + "), " + Math.round(distance) + " blocks from the villager");
                expect("the city is within a hundred chunks (1,600 blocks) of the villager", distance <= 1700);
                BlockPos at = BlockPos.containing(target.x(), 0, target.z());
                expect("an Ancient City lies right there (the structure search, not skipping explored chunks, finds one within 8 chunks)",
                        level.findNearestMapStructure(ExplorerMaps.ON_ANCIENT_CITY_MAPS, at, 8, false) != null);
            }

            // The same villager is not offered it again at the next level.
            villager.getOffers().add(offer);
            expect("a villager who already sells the map is not offered it again", listing.getOffer(villager, level.random) == null);
        }

        // Real cartographers: each ends with the map at most once, and some have it.
        int withMap = 0;
        boolean twice = false;
        for (int i = 0; i < CARTOGRAPHERS; i++) {
            Villager trader = cartographer(level, spawn, 2);
            int count = 0;
            for (MerchantOffer o : trader.getOffers()) {
                if (o.getResult().is(Items.FILLED_MAP) && Component.translatable(ExplorerMaps.NAME_KEY).equals(o.getResult().get(DataComponents.ITEM_NAME))) {
                    count++;
                }
            }
            twice |= count > 1;
            withMap += count > 0 ? 1 : 0;
        }
        log(withMap + " of " + CARTOGRAPHERS + " new level-2 cartographers sell the map");
        expect("no cartographer sells the map twice", !twice);
        expect("some new cartographers sell it (the chance is two in three each)", withMap > 0);
    }

    private static Int2ObjectMap<List<VillagerTrades.ItemListing>> fiveLevels() {
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = new Int2ObjectOpenHashMap<>();
        for (int level = 1; level <= 5; level++) {
            trades.put(level, new ArrayList<>());
        }
        return trades;
    }

    private static boolean isOurs(VillagerTrades.ItemListing listing) {
        return listing.getClass().getSimpleName().equals("AncientCityMap");
    }

    private static long ours(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, int level) {
        return trades.get(level).stream().filter(ExplorerMapChecks::isOurs).count();
    }

    private static Villager cartographer(ServerLevel level, BlockPos pos, int tradeLevel) {
        Villager villager = EntityType.VILLAGER.create(level);
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        villager.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.CARTOGRAPHER, tradeLevel));
        return villager;
    }

    private static void expect(String what, boolean ok) {
        checks++;
        if (ok) {
            log("PASS " + what);
        } else {
            failures++;
            log("FAIL " + what);
        }
    }

    private static void log(String message) {
        System.out.println("[explorer-map-checks] " + message);
    }
}
