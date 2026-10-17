package com.ascension.combat.internal.rune;

import com.ascension.combat.AscensionCombat;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The rune system's own items, kept out of {@link com.ascension.combat.internal.CombatContent} so
 * the rune work and the item work don't share a file.
 *
 * <p><b>Rune Chalk</b> (docs/technical/rune-system.md section 7): used up by a save that removes or
 * replaces a rune. Crafted from an amethyst shard and calcite, both found in Earth's geodes, so it
 * asks a little exploring and never another world (ADR-0018).
 */
public final class RuneContent {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AscensionCombat.MOD_ID);

    public static final Supplier<Item> RUNE_CHALK = ITEMS.register("rune_chalk", () -> new Item(new Item.Properties()));

    private RuneContent() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        RuneAttachments.register(modBus);
        modBus.addListener(RuneContent::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(RUNE_CHALK.get());
        }
    }
}
