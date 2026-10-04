package com.ascension.worlds.internal;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * What opens a gateway portal, as pure rules over dimension ids &mdash; no {@code ItemStack}, no
 * {@code Level}, so the decision is unit-testable and lives in exactly one place.
 * {@link GatewayPortalActivation} applies it to a real click; {@link #requirementFor} is also what
 * the "what does this portal need?" hint reads, so the hint can never disagree with the check.
 *
 * <p>Earth's portal takes a crafted Relay Coil (Sanchit's call, 2026-09-26: not free, not a boss
 * &mdash; Earth has none). Every other planet's portal takes that planet's own gateway key, the
 * reward its boss drops. Data-driven by the key's {@code gateway_key_for_planet} component rather
 * than one branch per planet, so a new planet's portal works the moment its boss drops a key
 * naming it &mdash; ADR-0007 rule 10.
 */
final class GatewayRules {

    /** Earth's surface: vanilla's Overworld, per {@code planet/earth.json}. */
    static final ResourceLocation EARTH = ResourceLocation.withDefaultNamespace("overworld");

    enum Requirement { RELAY_COIL, PLANET_KEY }

    private GatewayRules() {
    }

    static Requirement requirementFor(ResourceLocation portalDimension) {
        return EARTH.equals(portalDimension) ? Requirement.RELAY_COIL : Requirement.PLANET_KEY;
    }

    /** A key opens exactly the portal on the planet it names; an untagged key opens nothing. */
    static boolean keyOpens(@Nullable ResourceLocation keyPlanet, ResourceLocation portalDimension) {
        return keyPlanet != null && keyPlanet.equals(portalDimension);
    }
}
