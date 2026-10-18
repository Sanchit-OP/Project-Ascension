package com.ascension.core.api.client;

/**
 * The icons a HUD host can draw for an element, so a module can show a stat without shipping its
 * own art or knowing which host is installed (ADR-0022). The host owns the look.
 *
 * <p>Additive: new icons may be appended, never reordered or removed (semver, ADR-0003).
 */
public enum HudIcon {
    ATTACK_DAMAGE,
    ATTACK_SPEED,
    ARMOR,
    MAGIC_RESIST,
    MOVE_SPEED,
    OMNIVAMP
}
