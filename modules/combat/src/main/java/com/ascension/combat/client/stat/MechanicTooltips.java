package com.ascension.combat.client.stat;

import com.ascension.combat.AscensionCombat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What a legendary item does, said on its tooltip. The 26 catalog items each carry a mechanic
 * (on-hit procs, actives, passives) that no stat line can show, so this adds a "Legendary" line
 * under the name and, below the stat block, one paragraph per mechanic.
 *
 * <p>Data-driven in the way a tooltip can be: an item is legendary when it is in the item tag
 * {@code ascension_combat:legendary}, and its paragraphs are the lang keys
 * {@code tooltip.ascension_combat.<item>.1}, {@code .2} and so on, read until one is missing.
 * Each paragraph is {@code "Label: what it does"}: the label is drawn gold, the text grey. Adding
 * an item is a tag entry and some lang lines, no code. The numbers in the text are written by hand
 * from each effects class, so a balance change there means a lang edit here.
 *
 * <p>Runs after {@link StatTooltips} (lower priority), which has already put the stat block under
 * the name: the paragraphs go after that block's closing blank line. Nothing is cached.
 */
public final class MechanicTooltips {

    private static final TagKey<Item> LEGENDARY =
            TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "legendary"));

    private MechanicTooltips() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOW, MechanicTooltips::onItemTooltip);
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(LEGENDARY)) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        tooltip.add(Math.min(1, tooltip.size()), Component.translatable("tooltip.ascension_combat.legendary")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));

        List<Component> paragraphs = paragraphs(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        if (paragraphs.isEmpty()) {
            return;
        }
        // The stat block ends at its own blank line; with no such line the item has nothing after it.
        int blank = -1;
        for (int i = 2; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().isEmpty()) {
                blank = i;
                break;
            }
        }
        if (blank < 0) {
            tooltip.add(Component.empty());
            tooltip.addAll(paragraphs);
        } else {
            List<Component> inserted = new ArrayList<>(paragraphs);
            inserted.add(Component.empty());
            tooltip.addAll(blank + 1, inserted);
        }
    }

    /** One line per lang key {@code tooltip.ascension_combat.<item>.<n>}, until the numbering stops. */
    private static List<Component> paragraphs(String item) {
        List<Component> lines = new ArrayList<>();
        for (int n = 1; ; n++) {
            String key = "tooltip." + AscensionCombat.MOD_ID + "." + item + "." + n;
            if (!I18n.exists(key)) {
                return lines;
            }
            String text = I18n.get(key);
            int split = text.indexOf(": ");
            if (split < 0) {
                lines.add(Component.literal(text).withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.literal(text.substring(0, split + 2)).withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(text.substring(split + 2)).withStyle(ChatFormatting.GRAY)));
            }
        }
    }
}
