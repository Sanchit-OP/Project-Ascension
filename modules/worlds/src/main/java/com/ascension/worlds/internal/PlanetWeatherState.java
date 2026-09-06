package com.ascension.worlds.internal;

import com.ascension.worlds.api.Planet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

/**
 * A single surface's weather clock: on or off, and how many ticks until that flips.
 *
 * <p>Deliberately not vanilla's own rain clock reused &mdash; that clock is shared with the
 * Overworld regardless of which dimension asks it (see {@code SpaceSpecialEffects}'s javadoc),
 * so a planet-specific hazard needs its own, independent timer. Held as a {@code Level}
 * attachment (see {@code SpaceAttachments.WEATHER_STATE}) rather than a static map, per ADR-0007
 * rule 1 &mdash; the attachment lives exactly as long as the level it is attached to.
 */
final class PlanetWeatherState {

    static final Codec<PlanetWeatherState> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.BOOL.fieldOf("active").forGetter(s -> s.active),
                    Codec.INT.fieldOf("ticks_until_change").forGetter(s -> s.ticksUntilChange))
            .apply(instance, PlanetWeatherState::new));

    private boolean active;

    /** {@code -1} means "no timer scheduled yet" &mdash; a fresh level, or one just loaded. */
    private int ticksUntilChange;

    PlanetWeatherState() {
        this(false, -1);
    }

    private PlanetWeatherState(boolean active, int ticksUntilChange) {
        this.active = active;
        this.ticksUntilChange = ticksUntilChange;
    }

    boolean active() {
        return active;
    }

    /** Ticks remaining until the next flip, or {@code -1} if no timer has been scheduled yet. */
    int ticksUntilChange() {
        return ticksUntilChange;
    }

    /** Forces the next {@link #tick} to flip {@link #active} immediately. Debug command only. */
    void forceChange() {
        ticksUntilChange = 0;
    }

    /**
     * Advances the clock by one tick against {@code config}. Returns whether {@link #active}
     * flipped this tick, which is exactly when {@link PlanetWeatherMechanics} needs to tell
     * anyone.
     */
    boolean tick(Planet.Weather config, RandomSource random) {
        if (ticksUntilChange < 0) {
            // First tick this level has ever run this clock (or the first since it loaded with
            // no saved state yet) -- start calm rather than mid-storm.
            ticksUntilChange = nextCalmPeriod(config, random);
            return false;
        }
        ticksUntilChange--;
        if (ticksUntilChange > 0) {
            return false;
        }
        active = !active;
        ticksUntilChange = active ? config.durationTicks() : nextCalmPeriod(config, random);
        return true;
    }

    /**
     * Randomised around {@code averageIntervalTicks} rather than fixed, the same reason vanilla's
     * own rain delay is a range and not a constant &mdash; a clock that flips on a fixed schedule
     * reads as a timer, not weather.
     *
     * <p>Deliberately not {@code UniformInt} &mdash; that class's static initialiser registers
     * itself into {@code BuiltInRegistries}, which asserts {@code Bootstrap.bootStrap()} has run
     * and throws otherwise. Nothing here needs a codec-backed, datapack-referenceable value
     * provider; a plain random range needs no bootstrap and keeps this testable the same way
     * every other pure-logic class in this module is (ADR-0008: unit tests need no running game).
     */
    private static int nextCalmPeriod(Planet.Weather config, RandomSource random) {
        int base = config.averageIntervalTicks();
        int min = base / 2;
        int max = base + base / 2;
        return min + random.nextInt(max - min + 1);
    }
}
