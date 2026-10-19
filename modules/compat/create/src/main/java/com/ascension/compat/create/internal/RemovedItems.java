package com.ascension.compat.create.internal;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The Create items this pack takes out of the game, by registry name (no Create class is touched).
 *
 * <p>Create's backtanks and diving gear are a wearable tank of pressurised air and a helmet that lets you breathe
 * under water: the same ideas as {@code ascension-atmosphere}'s oxygen tank and its water rule, under different
 * rules, so having both is the confusion ADR-0002 exists to avoid. The netherite diving gear also lets a player
 * swim in lava, which walks round the Nether. They are removed completely: no recipe makes them, no recipe uses
 * them, the recipe viewers and the creative menu do not show them, and the advancements for earning them are gone.
 * What the data does is in {@code data/create/} and {@code data/c/tags/item/hidden_from_recipe_viewers.json}.
 */
public final class RemovedItems {

    /** The six items, as Create registers them. {@code CreateChecks} proves each id still exists. */
    public static final List<ResourceLocation> IDS = List.of(
            id("copper_backtank"), id("netherite_backtank"),
            id("copper_diving_helmet"), id("netherite_diving_helmet"),
            id("copper_diving_boots"), id("netherite_diving_boots"));

    private RemovedItems() {
    }

    /** Whether this stack is one of the removed items. */
    public static boolean isRemoved(ItemStack stack) {
        return !stack.isEmpty() && IDS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("create", path);
    }
}
