package com.ascension.combat.internal.armor;

import com.ascension.combat.AscensionCombat;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A single flat-or-percent stat bonus while one specific item is worn in one specific slot
 * (docs/technical/combat-api.md section 6). Mercury Treads, Ninja Tabi, Swifty, and Berserker's
 * Greaves are all this exact shape &mdash; a different attribute, value, and operation each, but
 * nothing else distinguishes them &mdash; so this is one reusable rule rather than four
 * near-identical effects classes. Four simultaneous real consumers is well past the point that
 * would justify writing this once.
 */
public final class StatBoots {

    private final Supplier<Item> item;
    private final EquipmentSlot slot;
    private final Supplier<Holder<Attribute>> attribute;
    private final double amount;
    private final AttributeModifier.Operation operation;
    private final ResourceLocation modifierId;

    public StatBoots(
            String name,
            Supplier<Item> item,
            EquipmentSlot slot,
            Supplier<Holder<Attribute>> attribute,
            double amount,
            AttributeModifier.Operation operation) {
        this.item = item;
        this.slot = slot;
        this.attribute = attribute;
        this.amount = amount;
        this.operation = operation;
        this.modifierId = ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, name);
    }

    public void register(IEventBus bus) {
        bus.addListener(this::onPlayerTick);
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AttributeInstance instance = player.getAttribute(attribute.get());
        if (instance == null) {
            return;
        }

        instance.removeModifier(modifierId);
        if (!player.getItemBySlot(slot).is(item.get())) {
            return;
        }

        instance.addTransientModifier(new AttributeModifier(modifierId, amount, operation));
    }
}
