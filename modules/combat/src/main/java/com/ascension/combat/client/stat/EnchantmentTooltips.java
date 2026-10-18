package com.ascension.combat.client.stat;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.internal.stat.StatText;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What goes under an enchantment's name in a tooltip (docs/progression/enchantments.md section 9):
 *
 * <pre>
 * Protection II
 * -4.5% damage taken                  its damage reduction, for any enchantment that has one
 * Keen Edge III
 * Your hits ignore some Armor.        a grey description, when the lang file has one
 * +6 Lethality                        on an enchanted book only: what it gives at its level
 * </pre>
 *
 * <p>Data-driven in both directions. A description is the lang key {@code enchantment.<namespace>.<name>.desc}
 * (another mod's enchantment gains a line by adding the key); what a reduction applies to is
 * {@code enchantment.<namespace>.<name>.reduces} ("fall damage"), "damage" when there is none. The numbers
 * come from the enchantment's own effects, so a retuned level reaches the tooltip with the data. Vanilla
 * writes the names; this finds each name's line and adds after it. Nothing is cached.
 */
public final class EnchantmentTooltips {

    private EnchantmentTooltips() {
    }

    public static void register(IEventBus bus) {
        // After StatTooltips has put its block in, so the names are where they will stay.
        bus.addListener(EventPriority.LOW, EnchantmentTooltips::onItemTooltip);
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        boolean book = stack.has(DataComponents.STORED_ENCHANTMENTS);
        add(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY), tooltip, false);
        add(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY), tooltip, book);
    }

    private static void add(ItemEnchantments enchantments, List<Component> tooltip, boolean preview) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int level = entry.getIntValue();
            List<Component> lines = new ArrayList<>();
            lines.addAll(reductionLines(holder, level));
            descriptionLine(holder).ifPresent(lines::add);
            if (preview) {
                lines.addAll(previewLines(holder, level));
            }
            if (lines.isEmpty()) {
                continue;
            }

            String name = Enchantment.getFullname(holder, level).getString();
            for (int i = 0; i < tooltip.size(); i++) {
                if (tooltip.get(i).getString().equals(name)) {
                    tooltip.addAll(i + 1, lines);
                    break;
                }
            }
        }
    }

    /** The grey description from the lang file, if the enchantment has one. */
    private static java.util.Optional<Component> descriptionLine(Holder<Enchantment> holder) {
        String key = key(holder, "desc");
        return key != null && I18n.exists(key)
                ? java.util.Optional.of(Component.translatable(key).withStyle(ChatFormatting.GRAY))
                : java.util.Optional.empty();
    }

    /**
     * The cut an enchantment gives besides its stats ("-12% fall damage taken"): not an attribute, so no stat
     * line carries it. Vanilla counts 4% a protection point.
     */
    private static List<Component> reductionLines(Holder<Enchantment> holder, int level) {
        float points = 0.0f;
        for (ConditionalEffect<EnchantmentValueEffect> effect : holder.value().getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION)) {
            points = effect.effect().process(level, RandomSource.create(), points);
        }
        if (points <= 0.0f) {
            return List.of();
        }

        String key = key(holder, "reduces");
        Component what = key != null && I18n.exists(key) ? Component.translatable(key) : Component.translatable("tooltip.ascension_combat.reduces.damage");
        return List.of(Component.translatable("tooltip.ascension_combat.damage_reduction", StatText.number(points * 4.0, 1), what)
                .withStyle(ChatFormatting.GRAY));
    }

    /** What an enchanted book's enchantment would give at its level, one coloured line per stat. */
    private static List<Component> previewLines(Holder<Enchantment> holder, int level) {
        List<Component> lines = new ArrayList<>();
        for (EnchantmentAttributeEffect effect : holder.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES)) {
            CombatStat stat = statFor(effect.attribute());
            if (stat == null) {
                continue;
            }
            double amount = effect.amount().calculate(level);
            lines.add(Component.literal(StatText.modifier(stat, effect.operation(), amount) + " ")
                    .append(stat.displayName())
                    .withStyle(style -> style.withColor(TextColor.fromRgb(stat.color()))));
        }
        return lines;
    }

    private static CombatStat statFor(Holder<Attribute> attribute) {
        for (CombatStat stat : CombatStats.all()) {
            if (stat.attribute().value() == attribute.value()) {
                return stat;
            }
        }
        return null;
    }

    /** {@code enchantment.<namespace>.<name>.<suffix>}, or null for an enchantment with no id. */
    private static String key(Holder<Enchantment> holder, String suffix) {
        return holder.unwrapKey()
                .map(key -> "enchantment." + key.location().getNamespace() + "." + key.location().getPath() + "." + suffix)
                .orElse(null);
    }
}
