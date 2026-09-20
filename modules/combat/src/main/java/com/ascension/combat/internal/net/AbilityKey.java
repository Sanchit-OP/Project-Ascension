package com.ascension.combat.internal.net;

/**
 * Identifies which bow's own active a {@link AbilityCooldownSyncPayload} is for &mdash; a small
 * fixed enum rather than a registry round-trip, since there are exactly two consumers so far
 * (Heartwood of Pallas, Galeforce) and both are known at compile time.
 */
public enum AbilityKey {
    HEARTWOOD,
    GALEFORCE,
}
