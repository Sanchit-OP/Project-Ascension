package com.ascension.atmosphere.api;

/**
 * Priority bands for {@link AtmosphereProvider}.
 *
 * <p>Resolution order is fixed by these values rather than by registration order. Ambiguity
 * about which provider wins is the classic source of interop bugs that nobody can reproduce:
 * a ship interior must beat the planet's vacuum, and that must not depend on mod load order.
 *
 * <p>Values are spaced by 1000 so third parties can slot between bands without us reserving
 * every integer.
 */
public final class AtmospherePriority {

    /** A planet or dimension's baseline atmosphere. Lowest; everything overrides it. */
    public static final int DIMENSION = 0;

    /** Authored structures, such as a sealed bunker on an airless world. */
    public static final int STRUCTURE = 1000;

    /** Player-built sealed rooms pressurised by an emitter. */
    public static final int SEALED_VOLUME = 2000;

    /** Ship and vehicle interiors, including moving sub-levels. */
    public static final int VEHICLE = 3000;

    /** Admin, debug and creative overrides. Beats everything. */
    public static final int OVERRIDE = 10000;

    private AtmospherePriority() {
    }
}
