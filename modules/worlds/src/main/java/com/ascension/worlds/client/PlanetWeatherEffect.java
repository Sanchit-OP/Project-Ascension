package com.ascension.worlds.client;

import com.ascension.worlds.api.Planet;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * How one {@code weather.type} id actually looks and feels, client-side. Registered against that
 * id in {@link PlanetWeatherEffects} &mdash; adding a second kind of planet weather later means
 * one new class implementing this and one registration line, never a new schema shape or a new
 * server-side concept (the on/off clock in {@code PlanetWeatherState} is shared by every type).
 *
 * <p>Every method takes {@code intensity} in {@code [0, 1]}, not a plain on/off &mdash; see
 * {@link ClientPlanetWeather}'s javadoc for why the server's boolean is smoothed into a ramp
 * client-side rather than passed straight through.
 */
interface PlanetWeatherEffect {

    /**
     * Cuts render distance, scaled by {@code intensity}. {@code event.setCanceled(true)} is what
     * makes the plane distances below actually apply &mdash; see {@link ViewportEvent.RenderFog}'s
     * own javadoc.
     *
     * <p><strong>Not load-bearing on its own.</strong> A rendering-optimisation mod that replaces
     * vanilla's terrain fog pass (Sodium, in this stack) is not guaranteed to fire this event at
     * all, which is exactly what made the first version of this effect read as "a bit hazy" rather
     * than "cannot see" &mdash; {@link #applyScreenOverlay} is the guarantee; this is a bonus on
     * top of it where the world renderer happens to still honour it.
     */
    void applyFog(ViewportEvent.RenderFog event, Planet.Weather config, float intensity);

    /** Tints the fog color, scaled by {@code intensity}. Not cancellable. Same caveat as above. */
    void applyFogColor(ViewportEvent.ComputeFogColor event, Planet.Weather config, float intensity);

    /**
     * A full-screen wash drawn directly onto the HUD layer, after everything else, at opacity
     * proportional to {@code intensity}. This is what actually guarantees the impairment:
     * {@code RenderGuiEvent} is part of the 2D overlay pipeline, not the 3D world-terrain pipeline
     * a renderer-replacement mod might reimplement, so it draws regardless of which mod is
     * responsible for the world underneath it.
     */
    void applyScreenOverlay(RenderGuiEvent.Post event, Planet.Weather config, float intensity);

    /**
     * Ambient particles and sound, called once per client tick while {@code intensity > 0}.
     * Density and volume should scale with {@code intensity} so the ramp is audible and visible,
     * not just a fog number changing underneath an unchanged soundscape.
     */
    void tick(Planet.Weather config, float intensity);
}
