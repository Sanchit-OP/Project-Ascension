package com.ascension.compat.create.internal;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Create Aeronautics' Aviator's Goggles counting as goggles when worn in Curios' head slot (alpha.2 test, 2026-10-25: "they won't go in the
 * head slot"). Two halves. The data half is {@code data/curios/tags/item/head.json}, which lets the item into the slot. This is the code half:
 * Create decides whether a player sees the goggle overlays by asking a list of predicates, and Aeronautics' own predicate looks at the helmet
 * slot only, so without this one the goggles would sit in the Curios slot and do nothing. Create adds the same kind of predicate for its own
 * goggles and its own Curios slot.
 *
 * <p>Only loaded when both Curios and Aeronautics are, so a pack without either never touches these classes. Holds nothing: the predicate
 * reads the player it is asked about (ADR-0007).
 */
public final class AviatorGogglesCurios {

    private static final Logger LOGGER = LoggerFactory.getLogger(AviatorGogglesCurios.class);
    private static final ResourceLocation AVIATORS = ResourceLocation.fromNamespaceAndPath("aeronautics", "aviators_goggles");

    private AviatorGogglesCurios() {
    }

    public static void register() {
        Item aviators = BuiltInRegistries.ITEM.get(AVIATORS);
        if (aviators == Items.AIR) {
            LOGGER.warn("Aeronautics is loaded but has no {}; its goggles will not count in the Curios head slot", AVIATORS);
            return;
        }
        GogglesItem.addIsWearingPredicate(player -> CuriosApi.getCuriosInventory(player)
                .flatMap(inventory -> inventory.findFirstCurio(aviators))
                .isPresent());
    }
}
