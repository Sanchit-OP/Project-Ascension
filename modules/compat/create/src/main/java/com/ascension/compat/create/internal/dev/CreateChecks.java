package com.ascension.compat.create.internal.dev;

import com.ascension.compat.create.internal.RemovedItems;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * A development aid, dormant unless a file named {@code create-checks.txt} sits in the game directory: on a
 * dedicated server it proves that Create's backtanks and diving gear are really gone from the game
 * ({@link RemovedItems}), prints {@code PASS} or {@code FAIL} lines tagged {@code [create-checks]}, and stops the
 * server. It reads the loaded recipes, advancements and tags, which is where the data overrides either took
 * effect or did not. (The creative menu is built by a client and is not covered.)
 */
public final class CreateChecks {

    private static final String TRIGGER = "create-checks.txt";

    private static int checks;
    private static int failures;

    private CreateChecks() {
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
        gameBus.addListener((ServerStartedEvent event) -> run(event.getServer()));
    }

    private static void run(MinecraftServer server) {
        try {
            check(server);
        } catch (RuntimeException e) {
            fail("the checks threw " + e);
            e.printStackTrace();
        }
        log("RESULT: " + checks + " checks, " + failures + " failures");
        server.halt(false);
    }

    private static void check(MinecraftServer server) {
        Set<Item> gone = new HashSet<>();
        for (ResourceLocation id : RemovedItems.IDS) {
            boolean exists = BuiltInRegistries.ITEM.containsKey(id);
            expect(id + " is a real item (the list is not stale)", exists);
            if (exists) {
                gone.add(BuiltInRegistries.ITEM.get(id));
            }
        }

        // Recipes: none makes one, none needs one.
        int makers = 0;
        int users = 0;
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (gone.contains(holder.value().getResultItem(server.registryAccess()).getItem())) {
                makers++;
                log("   still made by " + holder.id());
            }
            for (Ingredient ingredient : holder.value().getIngredients()) {
                for (ItemStack stack : ingredient.getItems()) {
                    if (gone.contains(stack.getItem())) {
                        users++;
                        log("   still used by " + holder.id());
                        break;
                    }
                }
            }
        }
        expect("no recipe makes a removed item", makers == 0);
        expect("no recipe uses a removed item", users == 0);

        // The recipe viewers are told to hide all six.
        var hidden = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("c", "hidden_from_recipe_viewers")));
        for (ResourceLocation id : RemovedItems.IDS) {
            expect(id + " is hidden from recipe viewers",
                    hidden.isPresent() && hidden.get().stream().anyMatch(h -> h.is(id)));
        }

        // Advancements: the three for earning them are gone, and nothing hangs off a missing parent.
        for (String name : new String[] {"backtank", "diving_suit", "diving_suit_lava"}) {
            expect("advancement create:" + name + " is gone",
                    server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("create", name)) == null);
        }
        Set<ResourceLocation> known = new HashSet<>();
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            known.add(holder.id());
        }
        int orphans = 0;
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            if (holder.id().getNamespace().equals("create")
                    && holder.value().parent().map(parent -> !known.contains(parent)).orElse(false)) {
                orphans++;
                log("   orphan: " + holder.id() + " wants " + holder.value().parent().get());
            }
        }
        expect("no Create advancement is left without its parent", orphans == 0);
        for (String name : new String[] {"chained_drain", "cross_streams", "pipe_organ", "brass"}) {
            AdvancementHolder holder = server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("create", name));
            expect("advancement create:" + name + " still exists, under the steam whistle",
                    holder != null && holder.value().parent()
                            .map(parent -> parent.equals(ResourceLocation.fromNamespaceAndPath("create", "steam_whistle")))
                            .orElse(false));
        }
        checkBrassByHand(server);
        checkBurnerByHand(server);
        checkEarthRecipes(server);
        checkMachinesBreak(server);
        if (ModList.get().isLoaded("petrochem")) {
            checkPetrochem(server);
        }
        if (ModList.get().isLoaded("curios")) {
            CuriosProbe.run(server.overworld(), ModList.get().isLoaded("aeronautics"));
        }
    }

    /** Reads Curios' own API, so it is a class of its own and only loaded when Curios is. */
    private static final class CuriosProbe {

        /** The goggles go in the Curios head slot (alpha.2 test, 2026-10-25). Whether the aviator's goggles then count as goggles needs a player. */
        static void run(ServerLevel level, boolean aeronautics) {
            expect("players have a Curios head slot", CuriosApi.getPlayerSlots(level).containsKey("head"));
            expect("Create's goggles go in the head slot", slotsOf(level, "create", "goggles").contains("head"));
            if (aeronautics) {
                expect("Aeronautics' aviator's goggles go in the head slot", slotsOf(level, "aeronautics", "aviators_goggles").contains("head"));
            }
        }

        private static Set<String> slotsOf(ServerLevel level, String namespace, String path) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
            return CuriosApi.getItemStackSlots(new ItemStack(item), level).keySet();
        }
    }

    /**
     * Crude oil and the refinery are Create: Petrochem's (ADR-0031), and its products fuel Create Propulsion's liquid Thruster through the
     * {@code thruster_fuels} files in this jar. Proves the fluids, the chain's recipes and the tag are loaded, that oil is only pumped from
     * biomes of Earth, and (Propulsion being read by reflection, since nothing here compiles against it) that its fuel table knows the five fluids.
     */
    private static void checkPetrochem(MinecraftServer server) {
        for (String name : new String[] {"petroleum", "desalted_oil", "oil", "kerosene", "diesel", "gasoline", "lpg", "fuel_oil"}) {
            expect("petrochem:" + name + " is a registered fluid", BuiltInRegistries.FLUID.containsKey(ResourceLocation.fromNamespaceAndPath("petrochem", name)));
        }
        Fluid petroleum = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("petrochem", "petroleum"));
        var crude = BuiltInRegistries.FLUID.getTag(TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "crude_oil")));
        expect("petroleum is in the c:crude_oil tag", crude.isPresent() && crude.get().stream().anyMatch(h -> h.value() == petroleum));

        Map<String, Integer> byType = new TreeMap<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
            if (type != null && type.getNamespace().equals("petrochem")) {
                byType.merge(type.getPath(), 1, Integer::sum);
            }
        }
        log("   petrochem recipe types loaded: " + byType);
        expect("the pumpjack has its four biome recipes", byType.getOrDefault("pumpjack", 0) == 4);
        expect("the distillation tower has its three modes", byType.getOrDefault("distilling", 0) == 3);
        expect("the electrolyzer has recipes", byType.getOrDefault("electrolyzing", 0) >= 1);

        // Oil comes out of the ground only where a recipe names the biome. None may name a biome of ours.
        List<String> biomes = new ArrayList<>();
        for (var entry : server.getResourceManager().listResources("recipe/pumpjack", id -> id.getNamespace().equals("petrochem")
                && id.getPath().endsWith(".json")).entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                biomes.add(JsonParser.parseReader(reader).getAsJsonObject().get("biome").getAsString());
            } catch (IOException | RuntimeException e) {
                fail("could not read " + entry.getKey() + ": " + e);
            }
        }
        log("   pumpjack biomes: " + biomes);
        expect("every pumpjack recipe names a vanilla biome or biome tag", !biomes.isEmpty() && biomes.stream().allMatch(b -> b.startsWith("minecraft:") || b.startsWith("#minecraft:")));

        if (!ModList.get().isLoaded("createpropulsion")) {
            log("   Create Propulsion is not loaded: the fuel table is not checked");
            return;
        }
        Map<ResourceLocation, float[]> known = thrusterFuels();
        log("   Create Propulsion knows " + known.size() + " liquid fuels");
        float[][] wanted = {{1.15f, 0.8f}, {1.1f, 0.85f}, {1.2f, 0.9f}, {1.0f, 0.95f}, {0.9f, 1.0f}};
        String[] names = {"kerosene", "diesel", "gasoline", "lpg", "fuel_oil"};
        for (int i = 0; i < names.length; i++) {
            float[] got = known.get(ResourceLocation.fromNamespaceAndPath("petrochem", names[i]));
            expect("Create Propulsion burns petrochem:" + names[i] + " in the liquid Thruster, thrust " + wanted[i][0] + " using " + wanted[i][1],
                    got != null && got[0] == wanted[i][0] && got[1] == wanted[i][1]);
        }
    }

    /** The fluids Create Propulsion's loaded {@code thruster_fuels} data accepts, with thrust and consumption, by reflection (this jar does not compile against it). */
    private static Map<ResourceLocation, float[]> thrusterFuels() {
        Map<ResourceLocation, float[]> fuels = new HashMap<>();
        try {
            Class<?> manager = Class.forName("dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterFuelManager");
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) manager.getMethod("getFuelPropertiesMap").invoke(null)).entrySet()) {
                Object properties = entry.getValue();
                float thrust = (Float) properties.getClass().getMethod("thrustMultiplier").invoke(properties);
                float consumption = (Float) properties.getClass().getMethod("consumptionMultiplier").invoke(properties);
                fuels.put(BuiltInRegistries.FLUID.getKey((Fluid) entry.getKey()), new float[] {thrust, consumption});
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail("could not read Create Propulsion's fuel table: " + e);
        }
        return fuels;
    }

    /**
     * A Blaze Burner made on Earth (Sanchit, 2026-10-25: oil is needed for the first ship, so heat cannot wait for the Nether). Steel, bronze, steam and
     * Petrochem's tower are all heated recipes, and only a burner heats. The recipe makes Create's own burner item, the one a captured blaze gives.
     */
    private static void checkBurnerByHand(MinecraftServer server) {
        var byHand = server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("ascension_compat_create", "blaze_burner_by_hand"));
        expect("a blaze burner by hand is a loaded recipe", byHand.isPresent());
        if (byHand.isEmpty()) {
            return;
        }
        Item burner = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "blaze_burner"));
        expect("it makes Create's blaze burner, the one a captured blaze gives", byHand.get().value().getResultItem(server.registryAccess()).is(burner));
        // A shaped recipe lists every cell of its grid, the empty ones too.
        var ingredients = byHand.get().value().getIngredients().stream().filter(i -> !i.isEmpty()).toList();
        expect("it takes a lava bucket, a magma block and three iron sheets, nothing from the Nether",
                ingredients.size() == 5
                        && ingredients.stream().anyMatch(i -> i.test(new ItemStack(Items.LAVA_BUCKET)))
                        && ingredients.stream().anyMatch(i -> i.test(new ItemStack(Items.MAGMA_BLOCK)))
                        && ingredients.stream().noneMatch(i -> i.test(new ItemStack(Items.NETHERRACK)) || i.test(new ItemStack(Items.BLAZE_ROD))));
        expect("the empty burner's own recipe is untouched",
                server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("create", "crafting/kinetics/empty_blaze_burner")).isPresent());
    }

    /**
     * The rest of the oil road without the Nether (Sanchit, 2026-10-25). Petrochem's tin needs a thick potion, which needs glowstone and a brewing
     * stand, so two zinc nuggets and an iron one make it by hand. The electron tube needs no recipe of ours: Create's own recipe wants polished rose
     * quartz, and Create itself makes quartz on Earth: a crushing wheel on diorite, a fan's water over crushed raw gold or over soul sand, and the fan page
     * of the book gives the first soul sand, which a fan's soul fire makes more of from sand.
     */
    private static void checkEarthRecipes(MinecraftServer server) {
        var recipes = server.getRecipeManager();
        for (String id : new String[] {"crushing/diorite", "splashing/crushed_raw_gold", "splashing/soul_sand", "haunting/soul_sand"}) {
            expect("Create makes quartz on Earth: its recipe " + id + " is loaded", recipes.byKey(ResourceLocation.fromNamespaceAndPath("create", id)).isPresent());
        }
        expect("the electron tube is Create's own recipe, with no hand recipe of ours",
                recipes.byKey(ResourceLocation.fromNamespaceAndPath("ascension_compat_create", "electron_tube_by_hand")).isEmpty()
                        && recipes.byKey(ResourceLocation.fromNamespaceAndPath("create", "crafting/materials/electron_tube")).isPresent());
        if (ModList.get().isLoaded("petrochem")) {
            var tin = recipes.byKey(ResourceLocation.fromNamespaceAndPath("ascension_compat_create", "tin_nugget_by_hand"));
            expect("a tin nugget by hand is a loaded recipe (Petrochem is here)", tin.isPresent());
            if (tin.isPresent()) {
                Item tinNugget = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("petrochem", "tin_nugget"));
                expect("it makes Petrochem's tin nugget from three nuggets", tin.get().value().getResultItem(server.registryAccess()).is(tinNugget)
                        && tin.get().value().getIngredients().size() == 3);
            }
        }
    }

    /** The compressor breaks with a pickaxe and drops itself; Create's wrench lifts the dispenser (alpha.2 test, 2026-10-25). */
    private static void checkMachinesBreak(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos pos = new BlockPos(0, 200, 0);
        level.getChunk(pos);
        ItemStack wooden = new ItemStack(Items.WOODEN_PICKAXE);
        Block compressor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ascension_compat_create", "air_compressor"));
        BlockState state = compressor.defaultBlockState();
        expect("the air compressor is mineable with a pickaxe", state.is(BlockTags.MINEABLE_WITH_PICKAXE));
        expect("a wooden pickaxe harvests the air compressor", wooden.isCorrectToolForDrops(state));
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, wooden);
        expect("the air compressor drops itself", drops.size() == 1 && drops.get(0).is(compressor.asItem()));
        Block dispenser = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ascension_atmosphere", "air_dispenser"));
        expect("the air dispenser is in create:wrench_pickup",
                dispenser.defaultBlockState().is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("create", "wrench_pickup"))));
    }

    /** Brass at the crafting table (Sanchit, 2026-10-25): no Nether needed, and dearer per ingot than Create's heated mixing. */
    private static void checkBrassByHand(MinecraftServer server) {
        var recipes = server.getRecipeManager();
        var byHand = recipes.byKey(ResourceLocation.fromNamespaceAndPath("ascension_compat_create", "brass_ingot_by_hand"));
        expect("brass by hand is a loaded recipe", byHand.isPresent());
        if (byHand.isEmpty()) {
            return;
        }
        ItemStack result = byHand.get().value().getResultItem(server.registryAccess());
        Item brass = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "brass_ingot"));
        Item zinc = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "zinc_ingot"));
        expect("brass by hand makes brass ingots", result.is(brass));
        var ingredients = byHand.get().value().getIngredients();
        int copper = 0;
        int zincCount = 0;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.test(new ItemStack(Items.COPPER_INGOT))) {
                copper++;
            }
            if (ingredient.test(new ItemStack(zinc))) {
                zincCount++;
            }
        }
        expect("brass by hand takes " + copper + " copper and " + zincCount + " zinc ingots", copper == 3 && zincCount == 3
                && ingredients.size() == 6);
        var mixing = recipes.byKey(ResourceLocation.fromNamespaceAndPath("create", "mixing/brass_ingot"));
        expect("Create's own brass mixing recipe is still there", mixing.isPresent());
        if (mixing.isPresent()) {
            ItemStack mixed = mixing.get().value().getResultItem(server.registryAccess());
            double handCost = ingredients.size() / (double) Math.max(1, result.getCount());
            double mixingCost = mixing.get().value().getIngredients().size() / (double) Math.max(1, mixed.getCount());
            expect("by hand costs " + handCost + " ingots per brass, the mixer " + mixingCost + ": dearer", handCost > mixingCost);
        }
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

    private static void fail(String message) {
        checks++;
        failures++;
        log("FAIL " + message);
    }

    private static void log(String message) {
        System.out.println("[create-checks] " + message);
    }
}
