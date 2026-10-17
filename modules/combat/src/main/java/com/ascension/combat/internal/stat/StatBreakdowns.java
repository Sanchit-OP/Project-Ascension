package com.ascension.combat.internal.stat;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.StatBreakdown;
import com.ascension.combat.api.stat.StatSources;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * Builds {@link StatBreakdown}s: each modifier on a stat's attribute, named by where it came from
 * and summed per source.
 *
 * <p>Naming, in order:
 * <ol>
 *   <li><b>Worn and held items, by name.</b> Each item is asked which modifiers it gives, so any
 *       mod's item is named correctly with no registration. The client can ask too: the local
 *       player's items and both data maps are synced.</li>
 *   <li><b>Registered sources</b> ({@link StatSources}): growth, runes, effects.</li>
 *   <li><b>"Other"</b> for the rest.</li>
 * </ol>
 *
 * <p>Built on demand, for a screen or a command. Nothing is cached.
 */
public final class StatBreakdowns {

    private static final Component BASE = Component.translatable("stat_source.ascension_combat.base");
    private static final Component OTHER = Component.translatable("stat_source.ascension_combat.other");

    private StatBreakdowns() {
    }

    public static StatBreakdown of(LivingEntity entity, CombatStat stat) {
        AttributeInstance instance = entity.getAttribute(stat.attribute());
        if (instance == null) {
            return new StatBreakdown(0.0, List.of());
        }

        List<AttributeModifier> modifiers = new ArrayList<>(instance.getModifiers());
        double[] shares = ModifierShares.of(instance.getBaseValue(), modifiers);
        Map<ResourceLocation, Component> itemNames = itemNames(entity, stat.attribute());

        Map<Component, Double> bySource = new LinkedHashMap<>();
        bySource.put(BASE, instance.getBaseValue());
        // Items first, in slot order, so the sheet reads weapon, armour, then everything else.
        itemNames.values().forEach(name -> bySource.putIfAbsent(name, 0.0));
        for (int i = 0; i < modifiers.size(); i++) {
            ResourceLocation id = modifiers.get(i).id();
            Component source = itemNames.containsKey(id)
                    ? itemNames.get(id)
                    : StatSources.labelFor(id).orElse(OTHER);
            bySource.merge(source, shares[i], Double::sum);
        }

        List<StatBreakdown.Part> parts = new ArrayList<>();
        bySource.forEach((source, amount) -> {
            if (source == BASE || amount != 0.0) {
                parts.add(new StatBreakdown.Part(source, amount));
            }
        });
        return new StatBreakdown(instance.getValue(), List.copyOf(parts));
    }

    /** Each modifier id a worn or held item gives on {@code attribute}, mapped to that item's name. */
    private static Map<ResourceLocation, Component> itemNames(LivingEntity entity, Holder<Attribute> attribute) {
        Map<ResourceLocation, Component> names = new LinkedHashMap<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Component name = stack.getHoverName();
            stack.forEachModifier(slot, (modifierAttribute, modifier) -> {
                if (modifierAttribute.value() == attribute.value()) {
                    names.putIfAbsent(modifier.id(), name);
                }
            });
        }
        return names;
    }
}
