package com.ascension.compat.curios.internal;

import com.ascension.compat.curios.AscensionCompatCurios;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/**
 * A relic's stats are written for the off hand (the item data's slot is "offhand"). In the relic slot Curios applies
 * attributes through its own event, so this copies the off-hand modifiers across under ids of their own.
 *
 * <p>If the same item is also in the off hand its stats are already counted, so the curio copy is skipped: either place
 * counts, never both (prompt 11).
 */
public final class RelicStats {

    /** Path prefix of the copied modifier ids, registered with StatSources so the sheet names the slot. */
    public static final String ID_PREFIX = "relic/";

    private RelicStats() {
    }

    public static void onCurioModifiers(CurioAttributeModifierEvent event) {
        if (!RelicSlot.SLOT.equals(event.getSlotContext().identifier())) {
            return;
        }
        var stack = event.getItemStack();
        var wearer = event.getSlotContext().entity();
        if (wearer != null && wearer.getItemBySlot(EquipmentSlot.OFFHAND).is(stack.getItem())) {
            return;
        }
        stack.forEachModifier(EquipmentSlot.OFFHAND, (attribute, modifier) -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AscensionCompatCurios.MOD_ID,
                    ID_PREFIX + modifier.id().getNamespace() + "/" + modifier.id().getPath());
            event.addModifier(attribute, new AttributeModifier(id, modifier.amount(), modifier.operation()));
        });
    }
}
