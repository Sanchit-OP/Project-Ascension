package com.ascension.combat.internal.stat;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.api.stat.CombatStat;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/**
 * Gives every item its stats (docs/technical/item-stats.md sections 4 and 5): vanilla's, ours and
 * other mods' alike, through the one event every item stack's modifiers pass through.
 *
 * <p>Vanilla adds and removes these modifiers itself when a slot's item changes, for players and
 * mobs alike, so nothing here ticks. The same event draws tooltips on the client, which is why
 * both data maps are synced.
 *
 * <p>Two sources, in order:
 * <ol>
 *   <li><b>Conversions.</b> Each of the item's own flat modifiers whose attribute has a {@link
 *       StatDataMaps#STAT_CONVERSIONS} entry (vanilla armour and toughness) adds our stats at that
 *       rate, summed per slot and rounded down, so tooltips show whole numbers. A stat the item's
 *       own data names is skipped: the data replaces it.</li>
 *   <li><b>The item's data</b> ({@link StatDataMaps#ITEM_STATS}): the weapon's own damage and speed
 *       replace vanilla's base modifiers, under vanilla's own ids so every tooltip and mod still
 *       reads them as the weapon's; stats are added on top.</li>
 * </ol>
 *
 * <p>Modifier ids name the slot group and the stat, {@code ascension_combat:item/chest/armor}.
 * They must be unique per slot, or a helmet's Armor would replace a chestplate's, and stable, or
 * unequipping would fail to remove them.
 *
 * <p>Runs at {@link EventPriority#LOW} so armour another mod adds through this same event is
 * converted too.
 */
public final class ItemStatModifiers {

    /** The path every modifier this class adds starts with. */
    public static final String ID_PATH_PREFIX = "item/";

    /** A player's own Attack Damage: what a weapon's tooltip adds to its modifier. */
    private static final double BARE_HAND_DAMAGE = 1.0;

    /** A player's own Attack Speed: a weapon's speed modifier is its speed minus this. */
    private static final double BARE_HAND_SPEED = 4.0;

    private ItemStatModifiers() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOW, ItemStatModifiers::onItemAttributeModifiers);
    }

    /** Whether {@code id} is one this class adds. */
    public static boolean isOurs(ResourceLocation id) {
        return id.getNamespace().equals(AscensionCombat.MOD_ID) && id.getPath().startsWith(ID_PATH_PREFIX);
    }

    private static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        ItemStats data = stack.getItemHolder().getData(StatDataMaps.ITEM_STATS);
        addConversions(event, data);
        if (data != null) {
            addData(event, stack, data);
        }
    }

    private static void addConversions(ItemAttributeModifierEvent event, ItemStats data) {
        Map<EquipmentSlotGroup, Map<CombatStat, Double>> sums = null;
        for (ItemAttributeModifiers.Entry entry : event.getModifiers()) {
            if (entry.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) {
                continue;
            }

            Map<CombatStat, Double> rates = entry.attribute().getData(StatDataMaps.STAT_CONVERSIONS);
            if (rates == null) {
                continue;
            }

            for (Map.Entry<CombatStat, Double> rate : rates.entrySet()) {
                if (data != null && data.names(rate.getKey())) {
                    continue;
                }

                if (sums == null) {
                    sums = new EnumMap<>(EquipmentSlotGroup.class);
                }
                sums.computeIfAbsent(entry.slot(), slot -> new LinkedHashMap<>())
                        .merge(rate.getKey(), entry.modifier().amount() * rate.getValue(), Double::sum);
            }
        }

        if (sums == null) {
            return;
        }

        // Added after the loop: getModifiers() is a live view of the list being added to.
        sums.forEach((slot, stats) -> stats.forEach((stat, sum) -> {
            double written = Math.floor(sum);
            if (written != 0.0) {
                event.replaceModifier(stat.attribute(), stat.modifier(id(slot, stat), written), slot);
            }
        }));
    }

    private static void addData(ItemAttributeModifierEvent event, ItemStack stack, ItemStats data) {
        EquipmentSlotGroup slot = data.slot().orElseGet(() -> defaultSlot(stack));

        data.weaponDamage().ifPresent(damage -> event.replaceModifier(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage - BARE_HAND_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
                slot));
        data.weaponSpeed().ifPresent(speed -> event.replaceModifier(Attributes.ATTACK_SPEED,
                new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed - BARE_HAND_SPEED, AttributeModifier.Operation.ADD_VALUE),
                slot));

        data.stats().forEach((stat, written) -> {
            if (written != 0.0) {
                event.replaceModifier(stat.attribute(), stat.modifier(id(slot, stat), written), slot);
            }
        });
    }

    /** The item's armour slot if it has one, otherwise the main hand. */
    private static EquipmentSlotGroup defaultSlot(ItemStack stack) {
        EquipmentSlot slot = stack.getEquipmentSlot();
        if (slot == null) {
            Equipable equipable = Equipable.get(stack);
            slot = equipable == null ? null : equipable.getEquipmentSlot();
        }
        return slot == null ? EquipmentSlotGroup.MAINHAND : EquipmentSlotGroup.bySlot(slot);
    }

    private static ResourceLocation id(EquipmentSlotGroup slot, CombatStat stat) {
        return ResourceLocation.fromNamespaceAndPath(
                AscensionCombat.MOD_ID, ID_PATH_PREFIX + slot.getSerializedName() + "/" + stat.name());
    }
}
