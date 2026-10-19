package com.ascension.compat.create.internal.dev;

import com.ascension.compat.create.internal.RemovedItems;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
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
