package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.worlds.api.Planet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/**
 * The on/off clock behind a planet's weather &mdash; not what a storm looks like (that's
 * {@code DustStormEffect}, client-only and untestable headlessly), just whether the timer flips
 * at the right moments and for the right durations.
 */
class PlanetWeatherStateTest {

    private static final Planet.Weather CONFIG = new Planet.Weather(
            ResourceLocation.fromNamespaceAndPath("ascension_worlds", "dust_storm"),
            1000, 200, 12);

    @Test
    void startsInactive() {
        assertFalse(new PlanetWeatherState().active());
    }

    @Test
    void firstTickSchedulesAClockWithoutFlippingActive() {
        PlanetWeatherState state = new PlanetWeatherState();
        boolean changed = state.tick(CONFIG, RandomSource.create(1));
        assertFalse(changed);
        assertFalse(state.active());
    }

    @Test
    void eventuallyBecomesActive() {
        PlanetWeatherState state = new PlanetWeatherState();
        RandomSource random = RandomSource.create(1);
        boolean everFlipped = false;
        // The calm period is randomised up to 1.5x the average (1000), so 2000 ticks is enough
        // headroom for it to have fired at least once regardless of the roll.
        for (int i = 0; i < 2000 && !everFlipped; i++) {
            everFlipped = state.tick(CONFIG, random);
        }
        assertTrue(everFlipped, "storm never started within a generous margin of the average interval");
        assertTrue(state.active());
    }

    @Test
    void activeStormLastsExactlyItsConfiguredDuration() {
        PlanetWeatherState state = new PlanetWeatherState();
        RandomSource random = RandomSource.create(1);
        // Drive it to the moment it just became active.
        while (!state.tick(CONFIG, random)) {
            // spin
        }
        assertTrue(state.active());

        // It must stay active for exactly durationTicks (200) further ticks, flipping off on the
        // 200th, not before and not after.
        int ticksActive = 0;
        boolean flippedOff = false;
        while (!flippedOff) {
            flippedOff = state.tick(CONFIG, random);
            ticksActive++;
        }
        assertEquals(CONFIG.durationTicks(), ticksActive);
        assertFalse(state.active());
    }

    @Test
    void codecRoundTripsThroughJson() {
        PlanetWeatherState state = new PlanetWeatherState();
        state.tick(CONFIG, RandomSource.create(1));

        var encoded = PlanetWeatherState.CODEC
                .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, state)
                .getOrThrow();
        PlanetWeatherState decoded = PlanetWeatherState.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(state.active(), decoded.active());
    }
}
