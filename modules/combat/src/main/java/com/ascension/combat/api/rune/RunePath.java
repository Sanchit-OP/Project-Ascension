package com.ascension.combat.api.rune;

import com.mojang.serialization.Codec;

/**
 * The four rune paths (docs/technical/combat-api.md section 7). A public type: unlike a specific
 * rune's mechanic, which player has picked which path is exactly the kind of thing another mod
 * might reasonably want to read (a third-party balance tweak keyed on path, say), so this lives in
 * {@code api} rather than {@code internal} per section 8.
 *
 * <p>Only four, not League's five &mdash; Inspiration dropped entirely rather than kept as a
 * weaker fifth, Sanchit's call recorded in the design doc, not an oversight here.
 */
public enum RunePath {
    VANGUARD,
    REAVER,
    AETHERWRIGHT,
    BULWARK;

    public static final Codec<RunePath> CODEC = Codec.STRING.xmap(RunePath::valueOf, RunePath::name);
}
