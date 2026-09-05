# Compatibility

> **Updated 2026-09-05.** The target version question is settled:
> **Minecraft 1.21.1 / NeoForge / Java 21** — see
> [ADR-0001](../decisions/0001-target-minecraft-version-and-loader.md).
>
> The compatibility *strategy* is settled by
> [ADR-0003](../decisions/0003-modular-architecture-and-compatibility-policy.md): no Tier-1
> module hard-depends on any third-party mod, all foreign integration lives in optional Tier-2
> jars, and mixins never appear in Tier-1. This turns most historical "mod pairing risk" into
> a question about a single optional jar rather than about the whole project.

## Known risk: Sable

Sable is self-described as *"incredibly intrusive... extensive use of mixins, and prone to many
compatibility issues with other mods."* It is the physics engine behind Create: Aeronautics and
the route to the ships pillar. It is quarantined behind `ascension-compat-sable` per
[ADR-0006](../decisions/0006-sable-integration-posture.md), and must be profiled in M0.5 before
anything depends on it.

## Purpose

Tracks integration risks, version constraints, and likely conflict areas.

## Risk Areas

- Dimension travel
- Gravity or movement systems
- Custom damage and armor logic
- Boss arena generation
- Automation balance
- Multiplayer synchronization

## Open Questions

- Which mod pairings are highest technical risk?
- Which systems are too fragile to build core progression around?
- What is the target Minecraft and loader version?
