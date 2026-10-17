package com.ascension.combat.client;

import com.ascension.combat.internal.growth.GrowthNode;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * How the tree screen shows a stat and a theme: which icon and colour a stat has, how a number reads
 * (a percent for the stats that are fractions or multipliers), and each theme's colours. Pure apart
 * from looking an attribute's name up. The colours are the rune screen's palette
 * ({@code tools/generate-growth-art.py}), so the two screens read as one game.
 */
final class GrowthDisplay {

    private GrowthDisplay() {
    }

    /** No icon: a stat from an attribute this screen has no picture for. */
    static final int NO_GLYPH = -1;

    /** Icon columns in the sprite sheet, in order, and the colour each stat is drawn in (ARGB). */
    private static final Map<String, Integer> GLYPH_OF = Map.ofEntries(
            Map.entry("minecraft:generic.max_health", 0),
            Map.entry("ascension_combat:armor", 1),
            Map.entry("ascension_combat:magic_resist", 2),
            Map.entry("minecraft:generic.attack_damage", 3),
            Map.entry("ascension_combat:lethality", 4),
            Map.entry("ascension_combat:armor_penetration_percent", 5),
            Map.entry("ascension_combat:magic_penetration_percent", 5),
            Map.entry("minecraft:generic.attack_speed", 6),
            Map.entry("ascension_combat:omnivamp", 7),
            Map.entry("minecraft:generic.movement_speed", 8),
            Map.entry("minecraft:player.block_break_speed", 9),
            Map.entry("minecraft:player.block_interaction_range", 10),
            Map.entry("minecraft:generic.safe_fall_distance", 11),
            Map.entry("minecraft:generic.step_height", 12));

    private static final int[] GLYPH_COLOUR = {
            rgb(0x62c86a), rgb(0xd8b070), rgb(0x9c90f8), rgb(0xf0903c), rgb(0xf0b060), rgb(0xf0805c),
            rgb(0xf0d850), rgb(0xe05858), rgb(0xdce4ec), rgb(0xc8a878), rgb(0xa0d8e8), rgb(0xe8ecf4), rgb(0xc8d0a0)};

    /** Stats stored as a fraction (0.01 is 1%) rather than with a percent operation. */
    private static final Set<String> FRACTIONS = Set.of(
            "ascension_combat:omnivamp",
            "ascension_combat:armor_penetration_percent",
            "ascension_combat:magic_penetration_percent");

    static final int TEXT = rgb(0xffffff);
    static final int TEXT_BODY = rgb(0xd4d8de);
    static final int TEXT_DIM = rgb(0x8c929c);
    static final int TEXT_FAINT = rgb(0x5c616a);
    static final int XP_GREEN = rgb(0x9cf04c);
    static final int REFUSED = rgb(0xe05858);

    /** The brass of the frame: outline, dark, mid, light. */
    static final int[] BRASS = {rgb(0x3a3020), rgb(0x7a6840), rgb(0xc8b07a), rgb(0xf0e0b0)};

    /** Each theme's ramp: outline, dark, mid, light, highlight; in the order of {@link GrowthNode.Arm}. */
    private static final int[][] RAMP = {
            {rgb(0x2c080e), rgb(0x6c1422), rgb(0xa82636), rgb(0xe2505a), rgb(0xffc4c0)},   // power: red
            {rgb(0x3a2208), rgb(0x7c4e14), rgb(0xbb8423), rgb(0xebb848), rgb(0xfff0b4)},   // fury: gold
            {rgb(0x0c182c), rgb(0x24406c), rgb(0x3c6ca8), rgb(0x6ea0dc), rgb(0xd4e8ff)},   // guard: blue
            {rgb(0x0e2a12), rgb(0x2c6a26), rgb(0x4ea23c), rgb(0x8ad868), rgb(0xdcffc8)}};  // wayfarer: green

    private static int rgb(int rgb) {
        return 0xFF000000 | rgb;
    }

    static int[] ramp(GrowthNode.Arm arm) {
        return RAMP[arm.ordinal()];
    }

    /** The icon column for a stat, or {@link #NO_GLYPH}. */
    static int glyph(ResourceLocation attribute) {
        return GLYPH_OF.getOrDefault(attribute.toString(), NO_GLYPH);
    }

    static int colour(ResourceLocation attribute) {
        int glyph = glyph(attribute);
        return glyph == NO_GLYPH ? TEXT_BODY : GLYPH_COLOUR[glyph];
    }

    /** Blends two ARGB colours; {@code t} 0 is {@code a}, 1 is {@code b}. */
    static int mix(int a, int b, float t) {
        int out = 0xFF000000;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int from = (a >> shift) & 0xFF;
            int to = (b >> shift) & 0xFF;
            out |= Math.round(from + (to - from) * t) << shift;
        }
        return out;
    }

    /** {@code argb} with its alpha replaced. */
    static int alpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    static boolean isPercent(ResourceLocation attribute, AttributeModifier.Operation operation) {
        return operation != AttributeModifier.Operation.ADD_VALUE || FRACTIONS.contains(attribute.toString());
    }

    /** A stat's number as the player reads it: {@code +4}, {@code +0.25}, {@code +3%}. */
    static String number(ResourceLocation attribute, AttributeModifier.Operation operation, double amount) {
        boolean percent = isPercent(attribute, operation);
        double shown = percent ? amount * 100.0 : amount;
        String text = shown == Math.rint(shown)
                ? Long.toString((long) shown)
                : String.format(Locale.ROOT, "%.2f", shown).replaceAll("0+$", "").replaceAll("\\.$", "");
        return (shown >= 0 ? "+" : "") + text + (percent ? "%" : "");
    }

    /** A stat's name: the attribute's own, or its id if the client has no such attribute. */
    static Component name(ResourceLocation attribute) {
        Attribute known = BuiltInRegistries.ATTRIBUTE.get(attribute);
        return known == null ? Component.literal(attribute.toString()) : Component.translatable(known.getDescriptionId());
    }
}
