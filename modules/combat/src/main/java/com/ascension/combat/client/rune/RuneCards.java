package com.ascension.combat.client.rune;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneNumberLine;
import com.ascension.combat.internal.rune.RunePage;
import com.ascension.combat.internal.rune.RuneSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

/**
 * What a rune's hover card says (docs/technical/rune-system.md section 10): its name, which path
 * and kind it is, one plain sentence, its numbers coloured by stat, how far it has grown, and the
 * fine print about what choosing it costs.
 *
 * <p>Everything comes from the rune's data and the lang file, never typed here: the numbers are
 * read from the rune's own values ({@link RuneNumberLine}), so the card says what the rune does.
 */
public final class RuneCards {

    /** A rune's card, as text. {@code growth} and {@code fine} are empty when there is nothing to say. */
    public record Card(RunePath path, Component title, Component tag, Component body, List<Component> numbers,
            Optional<Component> growth, Optional<Component> fine) {
    }

    private RuneCards() {
    }

    /**
     * @param id        the rune
     * @param rune      its data
     * @param saved     the page as saved, for what choosing this would replace
     * @param primarySide whether the rune sits on the primary dial (a minor's side)
     * @param grown     kinds of enemy a growing rune has counted
     * @param chalk     Rune Chalk a change costs
     * @param levels    experience levels a change costs
     * @param lookup    the registry, to name what the saved page holds in the same place
     */
    public static Card of(ResourceLocation id, RuneDefinition rune, RunePage saved, boolean primarySide, int grown,
            int chalk, int levels, Function<ResourceLocation, RuneDefinition> lookup) {
        String key = nameKey(id);
        RunePath path = rune.path().orElse(null);
        Component pathName = path == null ? Component.empty()
                : Component.translatable("rune_path.ascension_combat." + path.getSerializedName());
        Component tag = switch (rune.slot()) {
            case KEYSTONE -> Component.translatable(
                    rune.world() ? "gui.ascension_combat.card.world_keystone" : "gui.ascension_combat.card.keystone", pathName);
            case MINOR -> Component.translatable("gui.ascension_combat.card.minor", pathName);
            case SHARD -> Component.translatable("gui.ascension_combat.card.shard");
        };
        List<Component> numbers = new ArrayList<>();
        for (RuneNumberLine line : rune.numbers()) {
            numbers.add(number(line, rune));
        }
        Optional<Component> growth = rune.growthCap().map(cap ->
                Component.translatable("gui.ascension_combat.card.grown", grown, cap));
        return new Card(path, Component.translatable(key), tag, Component.translatable(key + ".desc"), numbers, growth,
                finePrint(id, rune, saved, primarySide, chalk, levels, lookup));
    }

    /** One line of numbers: the words with each number written and coloured by its stat. */
    public static Component number(RuneNumberLine line, RuneDefinition rune) {
        Object[] args = new Object[line.parts().size()];
        for (int i = 0; i < args.length; i++) {
            RuneNumberLine.Part part = line.parts().get(i);
            int rgb = part.stat().flatMap(CombatStats::byName).map(stat -> stat.color()).orElse(0xFFFFFF);
            args[i] = Component.literal(part.format().apply(rune.value(part.value(), 0.0)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
        }
        return Component.translatable(line.text(), args);
    }

    /**
     * What choosing this rune does to the cost: free if its place is empty on the saved page, a
     * price if it would replace one, and a warning when what it replaces has grown.
     */
    private static Optional<Component> finePrint(ResourceLocation id, RuneDefinition rune, RunePage saved,
            boolean primarySide, int chalk, int levels, Function<ResourceLocation, RuneDefinition> lookup) {
        if (saved.runes().contains(id)) {
            return Optional.empty();
        }

        Optional<ResourceLocation> occupant = occupant(rune, saved, primarySide, lookup);
        if (occupant.isEmpty()) {
            return Optional.of(Component.translatable("gui.ascension_combat.card.free"));
        }

        Component name = Component.translatable(nameKey(occupant.get()));
        return Optional.of(Component.translatable("gui.ascension_combat.card.swap", name, chalk, levels));
    }

    /** What the saved page holds in the place {@code rune} would take. */
    private static Optional<ResourceLocation> occupant(RuneDefinition rune, RunePage saved, boolean primarySide,
            Function<ResourceLocation, RuneDefinition> lookup) {
        if (rune.slot() == RuneSlot.KEYSTONE) {
            return saved.keystone();
        }

        List<ResourceLocation> candidates = rune.slot() == RuneSlot.SHARD ? saved.shards()
                : primarySide ? saved.primaryMinors() : saved.secondaryMinors();
        for (ResourceLocation candidate : candidates) {
            RuneDefinition held = lookup.apply(candidate);
            if (held != null && held.row() == rune.row()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public static String nameKey(ResourceLocation id) {
        return "rune." + id.getNamespace() + "." + id.getPath();
    }
}
