package com.ascension.worlds.client;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.WorldsRegistries;
import com.ascension.worlds.internal.net.PlanetWeatherSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;

/**
 * Whether the surface the client is currently on has an active weather event, per
 * {@link PlanetWeatherSyncPayload}'s last word on the subject, plus which
 * {@link PlanetWeatherEffect} renders it &mdash; resolved once, here, when that word arrives,
 * rather than by re-scanning the planet registry every frame from the render hooks that actually
 * need this.
 *
 * <p><strong>The server's boolean is a target, not a value to render directly.</strong> Sanchit's
 * call: a storm should build over about five seconds and wind down over about five, not snap on
 * and off on the tick the server's clock flips. {@link #tick()} steps {@link #intensity} toward
 * {@link #targetActive} by {@code 1/100} every client tick (100 ticks = 5s at 20 tps) regardless
 * of anything else, and every {@link PlanetWeatherEffect} method is handed that intensity instead
 * of a boolean so the ramp is visible and audible, not just a number changing underneath an
 * unchanged effect.
 *
 * <p>Never stale for longer than one packet: the server sends this the instant a player arrives
 * on any surface (dimension change or login), including an explicit {@code false} for a surface
 * with no weather of its own, so there is no window where a leftover {@code true} from a
 * previous world could linger into a new one.
 */
final class ClientPlanetWeather {

    /** 100 ticks = 5 seconds at 20 tps, both for the build-up and the wind-down. */
    private static final float RAMP_TICKS = 100.0f;

    private static volatile boolean targetActive;
    private static volatile Planet.Weather current;
    private static volatile float intensity;

    static boolean isActive() {
        return current != null && intensity > 0.0f;
    }

    static Planet.Weather current() {
        return current;
    }

    static float intensity() {
        return intensity;
    }

    static void accept(PlanetWeatherSyncPayload payload) {
        targetActive = payload.active();
        Planet.Weather resolved = resolveWeatherOfCurrentSurface();
        if (resolved != current) {
            // A different surface, or the same one gaining/losing its weather config entirely --
            // start the ramp fresh rather than carrying over an in-between value from wherever
            // this client was a moment ago. A dimension change is already a hard cut (a loading
            // screen breaks visual continuity anyway), so there is nothing to preserve here.
            intensity = 0.0f;
        }
        current = resolved;
    }

    /**
     * Advances the ramp by one client tick. Called unconditionally every tick, independent of
     * {@link #isActive()} &mdash; it has to keep running through a wind-down even after the
     * server's own {@link #targetActive} has already gone back to {@code false}.
     */
    static void tick() {
        float step = 1.0f / RAMP_TICKS;
        intensity = targetActive
                ? Math.min(1.0f, intensity + step)
                : Math.max(0.0f, intensity - step);
    }

    private static Planet.Weather resolveWeatherOfCurrentSurface() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        Registry<Planet> planets = level.registryAccess().registryOrThrow(WorldsRegistries.PLANET);
        for (Planet planet : planets) {
            if (planet.surface().equals(level.dimension()) && planet.weather().isPresent()) {
                return planet.weather().get();
            }
        }
        return null;
    }

    private ClientPlanetWeather() {
    }
}
