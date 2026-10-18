package com.ascension.core.api.client;

/**
 * Where on the screen an element wants to sit, named for what it sits next to rather than for
 * pixel coordinates, so the host can adapt to any GUI scale, aspect ratio and safe area.
 *
 * <p>Several elements may share an anchor; the host stacks them away from the hotbar in
 * {@link HudElement#order()} order. Which anchors a host honours, and where it puts them, is the
 * host's decision (ADR-0022).
 */
public enum HudAnchor {
    /** Just left of the hotbar, bottom-aligned with it. Stacks further left. */
    HOTBAR_LEFT,
    /** Just right of the hotbar, bottom-aligned with it. Stacks further right. */
    HOTBAR_RIGHT,
    /** On the row above the hotbar, starting at its left edge. Stacks upward. */
    ABOVE_HOTBAR,
    /** Centred above the hotbar and clear of the row above it. Stacks upward. */
    CENTER_ABOVE_HOTBAR,
    /** Top-left corner. Stacks downward. */
    TOP_LEFT,
    /** Top-right corner. Stacks downward. */
    TOP_RIGHT
}
