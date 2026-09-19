package com.ascension.combat.internal.rune;

/**
 * Where in a path a rune sits (docs/technical/combat-api.md section 7): a path's one
 * {@code KEYSTONE} is granted automatically the instant that path is chosen as primary (see
 * {@link RunePageState}), while its {@code MINOR} runes are picked individually, up to two per
 * side, whether that path is primary or secondary.
 */
public enum RuneSlot {
    KEYSTONE,
    MINOR
}
