package com.ascension.combat.internal;

import com.ascension.combat.AscensionCombat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Attributes owned by this module &mdash; the stat model itself (docs/technical/combat-api.md
 * section 3), as opposed to {@link CombatAttachments}, which holds fight-local bookkeeping no
 * other system needs to read.
 */
public final class CombatAttributes {

    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, AscensionCombat.MOD_ID);

    /**
     * Reduces magic damage (combat-api.md section 2) the same role armor plays for physical.
     * Divine Sunderer's magic component (section 6) is the first thing in the catalog to read
     * it; Ragefang, Darken Blade and Heartwood of Pallas will later. Nothing grants bonus magic
     * resist yet, so every target effectively starts at 0 until gear that raises it exists.
     *
     * <p>Added to every living entity type via {@link EntityAttributeModificationEvent} below,
     * not just the player &mdash; a target with no attribute at all is treated as 0 resist by
     * {@link com.ascension.combat.internal.mechanics.MagicDamage}, so this is really about making
     * the stat visible/attributable rather than a strict correctness requirement.
     */
    public static final DeferredHolder<Attribute, Attribute> MAGIC_RESIST = ATTRIBUTES.register(
            "magic_resist",
            () -> new RangedAttribute("attribute.ascension_combat.magic_resist", 0.0, 0.0, 300.0)
                    .setSyncable(true));

    private CombatAttributes() {
    }

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        modBus.addListener(CombatAttributes::onAttributeModification);
    }

    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(entityType -> event.add(entityType, MAGIC_RESIST));
    }
}
