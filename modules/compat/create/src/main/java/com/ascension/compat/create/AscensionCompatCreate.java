package com.ascension.compat.create;

import com.ascension.compat.create.internal.RemovedItems;
import com.ascension.compat.create.internal.dev.CreateChecks;
import java.util.List;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-create}.
 *
 * <p>Tier 2 in ADR-0003, and it calls no line of Create's Java: it works by registry name and by data. The data
 * (in {@code src/main/resources/data}) overrides Create's recipes for its backtanks and diving gear to
 * always-false conditions, removes the advancements for earning them, hides them from recipe viewers, and makes
 * raw helium a fuel. The one piece of code is {@link #hideFromCreativeMenu}, because a creative tab is built at
 * runtime and no datapack can take an item out of it. Which items, and why: {@link RemovedItems}.
 *
 * <p>Create's "wearable tank of pressurised air that drains while worn" is the exact idea
 * {@code ascension-atmosphere}'s oxygen tank already implements under different rules; shipping both is the
 * confusion this jar exists to remove. (Until 2026-10-19 the diving boots were left alone, as a movement item with
 * no air mechanic; Sanchit asked for all the diving gear to go, and the netherite pair swims in lava.)
 *
 * <p>{@code required} on Create in this jar's own {@code neoforge.mods.toml} is what keeps it
 * from loading into a no-op when Create is absent, the same mechanism
 * {@code ascension-compat-chunky} uses for Chunky.
 */
@Mod(AscensionCompatCreate.MOD_ID)
public final class AscensionCompatCreate {

    public static final String MOD_ID = "ascension_compat_create";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatCreate.class);

    public AscensionCompatCreate(IEventBus modBus) {
        modBus.addListener(AscensionCompatCreate::hideFromCreativeMenu);
        // Dormant unless its trigger file is in the game directory.
        CreateChecks.register(NeoForge.EVENT_BUS);
        LOGGER.info("Ascension Compat: Create loaded -- backtanks and diving gear removed");
    }

    /**
     * Takes the removed items out of every creative tab and out of the search tab. Runs for each tab as it is built,
     * after Create has filled its own, and touches nothing else.
     */
    private static void hideFromCreativeMenu(BuildCreativeModeTabContentsEvent event) {
        // Copied first: remove() changes the sets being walked.
        for (ItemStack stack : List.copyOf(event.getParentEntries())) {
            if (RemovedItems.isRemoved(stack)) {
                event.remove(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            }
        }
        for (ItemStack stack : List.copyOf(event.getSearchEntries())) {
            if (RemovedItems.isRemoved(stack)) {
                event.remove(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
            }
        }
    }
}
