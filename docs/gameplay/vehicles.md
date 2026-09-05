# Vehicles

> **Updated 2026-09-05.** The pillar "ships are controllable play spaces, not menu shortcuts"
> now has a concrete route: **[Sable](https://modrinth.com/mod/sable)** provides *sub-levels* —
> moving regions of blocks, block-entities and entities that stay interactive while assembled,
> driven by the Rapier physics engine. Create: Aeronautics builds in-game vehicle construction
> on top of it.
>
> We do **not** write a physics engine. But Sable is mixin-heavy and compat-risky, so support
> ships as the optional `ascension-compat-sable` jar and nothing in Tier 1 references it
> ([ADR-0006](../decisions/0006-sable-integration-posture.md)).
>
> **New open question this creates:** what is the fallback ship experience for players who do
> not install Sable? Tracked in `todo.md`.

## Purpose

Defines ships, land vehicles, utility craft, and their role in progression.

## Vehicle Rules

- Vehicles must unlock new mission profiles.
- Ships are controllable play spaces, not menu shortcuts.
- Vehicle upgrades should solve operational problems.

## Vehicle Classes

- Early engineering contraptions
- First orbital craft
- Cargo or logistics craft
- Planetary landing craft
- Specialized exploration craft
- Late-game suit-adjacent heavy platforms or mechanized modules

## Current Assumptions

- Late progression may include advanced suit modules with a power fantasy closer to mechanized combat platforms than simple armor upgrades.
- These systems should expand mission roles without replacing ships, logistics, or planet-specific traversal challenges.

## Open Questions

- How large do player ships need to be to support the fantasy?
- Are there non-space vehicles that matter mechanically?
- How much ship loss or crash recovery is acceptable?
- Are advanced suit modules wearable gear, deployable exosystems, or full titan-class machines?
