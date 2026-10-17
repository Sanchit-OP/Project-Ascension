package com.ascension.combat.internal.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.combat.internal.rune.RuneNumberLine.Format;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** How a hover card writes a rune's numbers (docs/technical/rune-system.md section 10). Pure formatting (ADR-0008). */
class RuneNumberLineTest {

    @Test
    void plainNumbersDropNeedlessDecimals() {
        assertEquals("4", Format.PLAIN.apply(4.0));
        assertEquals("0.75", Format.PLAIN.apply(0.75));
        assertEquals("1.5", Format.PLAIN.apply(1.5));
        assertEquals("10", Format.PLAIN.apply(10));
    }

    @Test
    void plusNumbersCarryTheirSign() {
        assertEquals("+3", Format.PLUS.apply(3.0));
        assertEquals("+0.5", Format.PLUS.apply(0.5));
    }

    @Test
    void fractionsBecomePercents() {
        assertEquals("1%", Format.PERCENT.apply(0.01));
        assertEquals("50%", Format.PERCENT.apply(0.5));
        assertEquals("+10%", Format.PLUS_PERCENT.apply(0.10));
        assertEquals("+0.5%", Format.PLUS_PERCENT.apply(0.005));
        assertEquals("+15%", Format.PLUS_PERCENT.apply(0.15), "0.15 * 100 is 15.000000000000002 in floating point");
    }

    @Test
    void aRatioIsHowMuchOverOne() {
        assertEquals("10%", Format.OVER_PERCENT.apply(1.1));
        assertEquals("100%", Format.OVER_PERCENT.apply(2.0));
    }

    @Test
    void aLineReadsFromJson() {
        RuneNumberLine line = RuneNumberLine.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"text": "a.b", "parts": [{"value": "hits"}, {"value": "x", "format": "plus", "stat": "attack_damage"}]}"""))
                .getOrThrow();
        assertEquals("a.b", line.text());
        assertEquals(2, line.parts().size());
        assertEquals(Format.PLAIN, line.parts().get(0).format());
        assertEquals(Optional.of("attack_damage"), line.parts().get(1).stat());
        assertTrue(line.parts().get(0).stat().isEmpty());
    }
}
