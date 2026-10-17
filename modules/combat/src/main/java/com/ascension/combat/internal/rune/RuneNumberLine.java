package com.ascension.combat.internal.rune;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.util.StringRepresentable;

/**
 * One line of numbers on a rune's hover card (docs/technical/rune-system.md section 10):
 * "+0.5 Attack Damage a stack". The words are a lang key with a {@code %s} for each number; the
 * numbers are read from the rune's own {@code values}, so the card can never say something the
 * rune doesn't do, and a datapack that retunes a rune retunes its card too.
 *
 * <pre>{@code
 * { "text": "rune.ascension_combat.cadence.n0", "parts": [ { "value": "hits" }, { "value": "window_seconds" } ] }
 * }</pre>
 *
 * @param text  the lang key, with one {@code %s} for each part, in order
 * @param parts the numbers
 */
public record RuneNumberLine(String text, List<Part> parts) {

    /** How a number is written. */
    public enum Format implements StringRepresentable {
        /** {@code 4}, {@code 0.75}. */
        PLAIN("plain"),
        /** {@code +3}, {@code +0.5}. */
        PLUS("plus"),
        /** A fraction as a percent: 0.01 is {@code 1%}. */
        PERCENT("percent"),
        /** {@code +10%}, {@code +1%}. */
        PLUS_PERCENT("plus_percent"),
        /** A ratio as how much over one it is: 1.1 is {@code 10%}, 2.0 is {@code 100%}. */
        OVER_PERCENT("over_percent");

        public static final Codec<Format> CODEC = StringRepresentable.fromEnum(Format::values);

        private final String name;

        Format(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public String apply(double value) {
            return switch (this) {
                case PLAIN -> trim(value);
                case PLUS -> "+" + trim(value);
                case PERCENT -> trim(value * 100.0) + "%";
                case PLUS_PERCENT -> "+" + trim(value * 100.0) + "%";
                case OVER_PERCENT -> trim((value - 1.0) * 100.0) + "%";
            };
        }

        /** Whole numbers without decimals; otherwise up to two, without trailing zeros. */
        private static String trim(double value) {
            if (Math.abs(value - Math.rint(value)) < 1e-9) {
                return Long.toString(Math.round(value));
            }

            String text = String.format(Locale.ROOT, "%.2f", value);
            return text.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
    }

    /**
     * One number: which of the rune's {@code values}, how to write it, and which stat's colour it
     * takes (a stat name from the stat system, such as {@code attack_damage}); none is white.
     */
    public record Part(String value, Format format, Optional<String> stat) {

        public static final Codec<Part> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        Codec.STRING.fieldOf("value").forGetter(Part::value),
                        Format.CODEC.optionalFieldOf("format", Format.PLAIN).forGetter(Part::format),
                        Codec.STRING.optionalFieldOf("stat").forGetter(Part::stat))
                .apply(instance, Part::new));
    }

    public static final Codec<RuneNumberLine> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.STRING.fieldOf("text").forGetter(RuneNumberLine::text),
                    Part.CODEC.listOf().fieldOf("parts").forGetter(RuneNumberLine::parts))
            .apply(instance, RuneNumberLine::new));
}
