package com.ascension.combat.internal;

import com.ascension.combat.AscensionCombat;
import java.util.List;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The ingredients of the combat items' build paths (docs/progression/build-paths.md): six basic
 * components, seventeen epic components and six boss hearts. They are plain items with no stats
 * and no behaviour; what they are made of and what they make is all data, written by
 * {@code tools/generate-build-paths.py}, which keeps this list and the recipes in step
 * ({@code tools/check-data.py} fails when they drift).
 */
public final class BuildPathContent {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AscensionCombat.MOD_ID);

    private static final List<String> BASICS = List.of(
            "long_sword", "dagger", "cloth_armor", "null_magic_mantle", "ruby_crystal", "basic_boots");

    private static final List<String> EPICS = List.of(
            "pickaxe", "caulfields_warhammer", "giants_belt", "recurve_bow", "chain_vest", "seekers_armguard",
            "last_whisper", "noonquiver", "bf_sword", "phage", "kindlegem", "vampiric_scepter", "serrated_dirk",
            "negatron_cloak", "spectres_cowl", "hearthbound_axe", "executioners_calling");

    /** One per boss; each finishes one combat item. */
    private static final List<String> HEARTS = List.of(
            "warden_heart", "ender_guardian_heart", "ender_golem_heart", "netherite_monstrosity_heart",
            "ignis_heart", "wither_heart");

    private static final List<DeferredItem<Item>> ALL = new java.util.ArrayList<>();

    static {
        for (String name : BASICS) {
            ALL.add(ITEMS.registerSimpleItem(name, new Item.Properties()));
        }
        for (String name : EPICS) {
            ALL.add(ITEMS.registerSimpleItem(name, new Item.Properties().rarity(Rarity.UNCOMMON)));
        }
        for (String name : HEARTS) {
            ALL.add(ITEMS.registerSimpleItem(name, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
        }
    }

    private BuildPathContent() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(BuildPathContent::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            ALL.forEach(item -> event.accept(item.get()));
        }
    }
}
