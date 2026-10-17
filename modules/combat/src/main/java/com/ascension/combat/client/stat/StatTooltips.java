package com.ascension.combat.client.stat;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.internal.stat.StatDataMaps;
import com.ascension.combat.internal.stat.StatText;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.GatherSkippedAttributeTooltipsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

/**
 * League-style stat lines at the top of every item's tooltip that carries combat stats
 * (docs/technical/item-stats.md section 7, Sanchit 2026-10-15: League's item cards as the
 * inspiration). Vanilla's, ours and other mods' items alike:
 *
 * <pre>
 * Diamond Sword                     Netherite Chestplate
 * In main hand                      When worn                    grey: where the stats count
 * 7 Attack Damage                   +28 Armor                    white: a weapon's own attack;
 * 1.6 Attack Speed                  +20 Magic Resist             colours: one per stat
 *                                   +10 Health                   (CombatStat#color)
 *                                   +0.1 Knockback Resistance    unknown to combat: vanilla's style
 * </pre>
 *
 * <p>For such an item every attribute line is drawn here and vanilla's "When in Main Hand" block is
 * skipped whole. Skipping only some lines is impossible: NeoForge skips by modifier id, and vanilla
 * armour gives its Armor, Toughness and Knockback Resistance one shared id. An item with no combat
 * stat keeps vanilla's lines untouched. An attribute with a conversion ({@code stat_conversions}:
 * vanilla armour and toughness) is left out: it has become Armor, Magic Resist and Health, which
 * show instead.
 *
 * <p>Modifiers come from the item and its enchantments, as vanilla's own lines do. Everything is
 * recomputed per tooltip: no cache.
 */
public final class StatTooltips {

    /** What a weapon's damage and speed add to when its holder is unknown, as at game start. */
    private static final double BARE_HAND_DAMAGE = 1.0;
    private static final double BARE_HAND_SPEED = 4.0;

    private record Line(Holder<Attribute> attribute, AttributeModifier modifier) {
    }

    private StatTooltips() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(StatTooltips::onGatherSkipped);
        bus.addListener(StatTooltips::onItemTooltip);
    }

    private static void onGatherSkipped(GatherSkippedAttributeTooltipsEvent event) {
        if (hasCombatStats(collect(event.getStack()))) {
            event.setSkipAll(true);
        }
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        Map<EquipmentSlotGroup, List<Line>> lines = collect(event.getItemStack());
        if (!hasCombatStats(lines)) {
            return;
        }

        List<Component> block = block(lines, event.getEntity(), event.getFlags());
        List<Component> tooltip = event.getToolTip();
        int at = Math.min(1, tooltip.size());
        tooltip.addAll(at, block);
        if (tooltip.size() > at + block.size()) {
            tooltip.add(at + block.size(), Component.empty());
        }
    }

    /** Every modifier the item shows, by slot group, unless the item hides its attributes. */
    private static Map<EquipmentSlotGroup, List<Line>> collect(ItemStack stack) {
        Map<EquipmentSlotGroup, List<Line>> lines = new EnumMap<>(EquipmentSlotGroup.class);
        if (stack.isEmpty()
                || !stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).showInTooltip()) {
            return lines;
        }

        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            lines.computeIfAbsent(entry.slot(), slot -> new ArrayList<>()).add(new Line(entry.attribute(), entry.modifier()));
        }
        for (EquipmentSlotGroup slot : EquipmentSlotGroup.values()) {
            EnchantmentHelper.forEachModifier(stack, slot, (attribute, modifier) ->
                    lines.computeIfAbsent(slot, s -> new ArrayList<>()).add(new Line(attribute, modifier)));
        }
        return lines;
    }

    private static boolean hasCombatStats(Map<EquipmentSlotGroup, List<Line>> lines) {
        return lines.values().stream().flatMap(List::stream)
                .anyMatch(line -> line.modifier().amount() != 0.0 && statFor(line.attribute()) != null);
    }

    private static List<Component> block(Map<EquipmentSlotGroup, List<Line>> bySlot, @Nullable Player player, TooltipFlag flag) {
        List<Component> block = new ArrayList<>();
        bySlot.forEach((slot, lines) -> {
            List<Component> weapon = new ArrayList<>();
            List<Line> stats = new ArrayList<>();
            List<Component> others = new ArrayList<>();

            for (Line line : lines) {
                AttributeModifier modifier = line.modifier();
                if (modifier.amount() == 0.0 || isConverted(line.attribute())) {
                    continue;
                }

                if (modifier.id().equals(Item.BASE_ATTACK_DAMAGE_ID) && is(line.attribute(), Attributes.ATTACK_DAMAGE)) {
                    weapon.add(weaponLine(CombatStats.ATTACK_DAMAGE, modifier.amount() + base(player, Attributes.ATTACK_DAMAGE, BARE_HAND_DAMAGE)));
                } else if (modifier.id().equals(Item.BASE_ATTACK_SPEED_ID) && is(line.attribute(), Attributes.ATTACK_SPEED)) {
                    weapon.add(weaponLine(CombatStats.ATTACK_SPEED, modifier.amount() + base(player, Attributes.ATTACK_SPEED, BARE_HAND_SPEED)));
                } else if (statFor(line.attribute()) != null) {
                    stats.add(line);
                } else {
                    others.add(line.attribute().value().toComponent(modifier, flag));
                }
            }

            if (weapon.isEmpty() && stats.isEmpty() && others.isEmpty()) {
                return;
            }

            block.add(Component.translatable(slotKey(slot)).withStyle(ChatFormatting.GRAY));
            block.addAll(weapon);
            stats.sort(Comparator.comparingInt(line -> CombatStats.all().indexOf(statFor(line.attribute()))));
            for (Line line : stats) {
                CombatStat stat = statFor(line.attribute());
                block.add(Component.literal(StatText.modifier(stat, line.modifier().operation(), line.modifier().amount()) + " ")
                        .append(stat.displayName())
                        .withStyle(style -> style.withColor(TextColor.fromRgb(stat.color()))));
            }
            block.addAll(others);
        });
        return block;
    }

    private static Component weaponLine(CombatStat stat, double value) {
        return Component.literal(StatText.value(stat, value) + " ").append(stat.displayName()).withStyle(ChatFormatting.WHITE);
    }

    private static double base(@Nullable Player player, Holder<Attribute> attribute, double fallback) {
        return player == null ? fallback : player.getAttributeBaseValue(attribute);
    }

    @Nullable
    private static CombatStat statFor(Holder<Attribute> attribute) {
        for (CombatStat stat : CombatStats.all()) {
            if (stat.attribute().value() == attribute.value()) {
                return stat;
            }
        }
        return null;
    }

    /** An attribute with a conversion (vanilla armour, toughness) shows as the stats it becomes. */
    private static boolean isConverted(Holder<Attribute> attribute) {
        return attribute.getData(StatDataMaps.STAT_CONVERSIONS) != null;
    }

    private static boolean is(Holder<Attribute> attribute, Holder<Attribute> vanilla) {
        return attribute.value() == vanilla.value();
    }

    private static String slotKey(EquipmentSlotGroup slot) {
        return "tooltip.ascension_combat.slot." + switch (slot) {
            case MAINHAND -> "mainhand";
            case OFFHAND -> "offhand";
            case HAND -> "hand";
            case ANY -> "any";
            default -> "worn";
        };
    }
}
