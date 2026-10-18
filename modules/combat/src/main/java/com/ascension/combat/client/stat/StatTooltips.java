package com.ascension.combat.client.stat;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.internal.stat.ItemStatModifiers;
import com.ascension.combat.internal.stat.StatDataMaps;
import com.ascension.combat.internal.stat.StatSystem;
import com.ascension.combat.internal.stat.StatText;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
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

    private record Line(Holder<Attribute> attribute, AttributeModifier modifier) {
    }

    private StatTooltips() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(StatTooltips::onGatherSkipped);
        bus.addListener(StatTooltips::onItemTooltip);
    }

    private static void onGatherSkipped(GatherSkippedAttributeTooltipsEvent event) {
        Map<EquipmentSlotGroup, List<Line>> lines = collect(event.getStack());
        if (hasCombatStats(lines)) {
            event.setSkipAll(true);
            return;
        }
        // Respiration's "Oxygen Bonus" is vanilla's air bubbles; here its effect is lung capacity, said in its description.
        lines.values().stream().flatMap(List::stream)
                .filter(line -> is(line.attribute(), Attributes.OXYGEN_BONUS))
                .forEach(line -> event.skipId(line.modifier().id()));
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

    /** One stat's share from enchantments in one slot group, folded into the item's own line for that stat. */
    private record Key(CombatStat stat, AttributeModifier.Operation operation) {
    }

    private static List<Component> block(Map<EquipmentSlotGroup, List<Line>> bySlot, @Nullable Player player, TooltipFlag flag) {
        List<Component> block = new ArrayList<>();
        bySlot.forEach((slot, lines) -> block.addAll(section(slot, sort(lines, player, flag))));
        return block;
    }

    /** One slot group's lines, sorted into what each is drawn as. */
    private record Sorted(List<CombatStat> weaponStats, List<Double> weaponValues, List<Line> stats,
                          List<Component> others, Map<Key, Double> enchanted) {
    }

    private static Sorted sort(List<Line> lines, @Nullable Player player, TooltipFlag flag) {
        Sorted sorted = new Sorted(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new LinkedHashMap<>());
        for (Line line : lines) {
            AttributeModifier modifier = line.modifier();
            if (modifier.amount() == 0.0 || isConverted(line.attribute())) {
                continue;
            }

            if (is(line.attribute(), Attributes.OXYGEN_BONUS)) {
                continue;
            }

            CombatStat stat = statFor(line.attribute());
            if (stat != null && isEnchantment(modifier)) {
                sorted.enchanted().merge(new Key(stat, modifier.operation()), modifier.amount(), Double::sum);
            } else if (modifier.id().equals(Item.BASE_ATTACK_DAMAGE_ID) && is(line.attribute(), Attributes.ATTACK_DAMAGE)) {
                sorted.weaponStats().add(CombatStats.ATTACK_DAMAGE);
                sorted.weaponValues().add(modifier.amount() + base(player, Attributes.ATTACK_DAMAGE, ItemStatModifiers.BARE_HAND_DAMAGE));
            } else if (modifier.id().equals(Item.BASE_ATTACK_SPEED_ID) && is(line.attribute(), Attributes.ATTACK_SPEED)) {
                sorted.weaponStats().add(CombatStats.ATTACK_SPEED);
                sorted.weaponValues().add(modifier.amount() + base(player, Attributes.ATTACK_SPEED, ItemStatModifiers.BARE_HAND_SPEED));
            } else if (stat != null) {
                sorted.stats().add(line);
            } else {
                sorted.others().add(line.attribute().value().toComponent(modifier, flag));
            }
        }
        return sorted;
    }

    /**
     * The lines for one slot group: where the stats count, the weapon's own attack, then each stat. An
     * enchantment's share is added to the item's own line for that stat when there is one ("7 +2 Attack
     * Damage"); a stat only an enchantment gives is a line of its own.
     */
    private static List<Component> section(EquipmentSlotGroup slot, Sorted sorted) {
        List<Component> weapon = new ArrayList<>();
        for (int i = 0; i < sorted.weaponStats().size(); i++) {
            CombatStat stat = sorted.weaponStats().get(i);
            weapon.add(weaponLine(stat, sorted.weaponValues().get(i), sorted.enchanted().remove(new Key(stat, AttributeModifier.Operation.ADD_VALUE))));
        }

        List<StatLine> statLines = new ArrayList<>();
        for (Line line : sorted.stats()) {
            CombatStat stat = statFor(line.attribute());
            Double extra = sorted.enchanted().remove(new Key(stat, line.modifier().operation()));
            statLines.add(new StatLine(stat, line.modifier().operation(), line.modifier().amount(), extra));
        }
        sorted.enchanted().forEach((key, amount) -> statLines.add(new StatLine(key.stat(), key.operation(), amount, null)));

        if (weapon.isEmpty() && statLines.isEmpty() && sorted.others().isEmpty()) {
            return List.of();
        }

        List<Component> section = new ArrayList<>();
        section.add(Component.translatable(slotKey(slot)).withStyle(ChatFormatting.GRAY));
        section.addAll(weapon);
        statLines.sort(Comparator.comparingInt(line -> CombatStats.all().indexOf(line.stat())));
        for (StatLine line : statLines) {
            section.add(Component.literal(StatText.modifier(line.stat(), line.operation(), line.amount()))
                    .append(extra(line.stat(), line.operation(), line.extra()))
                    .append(" ")
                    .append(line.stat().displayName())
                    .withStyle(style -> style.withColor(TextColor.fromRgb(line.stat().color()))));
        }
        section.addAll(sorted.others());
        return section;
    }

    private record StatLine(CombatStat stat, AttributeModifier.Operation operation, double amount, @Nullable Double extra) {
    }

    /** What an item's enchantments add to a stat, after the item's own number, in the enchantment colour; nothing when none. */
    private static Component extra(CombatStat stat, AttributeModifier.Operation operation, @Nullable Double amount) {
        if (amount == null || amount == 0.0) {
            return Component.empty();
        }
        return Component.literal(" " + StatText.modifier(stat, operation, amount)).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    /** A modifier given by an enchantment: its id is under {@code ascension_combat:enchantment/} (ADR-0022). */
    private static boolean isEnchantment(AttributeModifier modifier) {
        return modifier.id().getNamespace().equals(AscensionCombat.MOD_ID) && modifier.id().getPath().startsWith(StatSystem.ENCHANTMENT_PREFIX);
    }

    private static Component weaponLine(CombatStat stat, double value, @Nullable Double extra) {
        return Component.literal(StatText.value(stat, value))
                .append(extra(stat, AttributeModifier.Operation.ADD_VALUE, extra))
                .append(" ")
                .append(stat.displayName())
                .withStyle(ChatFormatting.WHITE);
    }

    /** The holder's own value, or a bare hand's when the holder is unknown, as at game start. */
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
