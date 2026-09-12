/**
 * Stable public API of {@code ascension_combat}.
 *
 * <p>Everything in this package and its subpackages is a semantically versioned contract with
 * third-party consumers, per ADR-0003. Breaking changes here require a major version bump and
 * an ADR.
 *
 * <p>If a consumer ever needs to import from {@code com.ascension.combat.internal}, this API is
 * wrong and should be extended instead.
 */
package com.ascension.combat.api;
