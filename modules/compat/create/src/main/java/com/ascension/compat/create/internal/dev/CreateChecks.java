package com.ascension.compat.create.internal.dev;

import com.ascension.compat.create.internal.RemovedItems;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

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
        checkMachinesBreak(server);
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
