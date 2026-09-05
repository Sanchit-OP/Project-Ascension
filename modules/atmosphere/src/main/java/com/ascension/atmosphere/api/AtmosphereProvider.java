package com.ascension.atmosphere.api;

import java.util.Optional;

/**
 * Answers whether a position is breathable.
 *
 * <p>Implement this to contribute atmosphere from anywhere: a dimension baseline, a structure,
 * a sealed room, a vehicle. You never extend our classes, and we never need to know your mod
 * exists.
 *
 * <p>Providers are consulted in descending {@link #priority()} and the first that claims the
 * position wins. Return {@link Optional#empty()} to abstain; abstaining is the common case and
 * must be cheap.
 *
 * <p><strong>Called on the server thread, potentially often.</strong> Do no I/O, no allocation
 * you can avoid, and no world scanning. Answer from cached state.
 */
@FunctionalInterface
public interface AtmosphereProvider {

    /**
     * @return the atmosphere at this position, or empty to make no claim
     */
    Optional<Atmosphere> query(AtmosphereContext context);

    /**
     * Higher wins. Use a constant from {@link AtmospherePriority}.
     *
     * <p>Must be stable for the lifetime of the provider.
     */
    default int priority() {
        return AtmospherePriority.DIMENSION;
    }
}
