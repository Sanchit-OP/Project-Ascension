# Mod List

> **Reframed 2026-09-05 by [ADR-0002](../decisions/0002-custom-mods-not-curated-modpack.md).**
> This was previously read as a dependency list. It is not. Project-Ascension builds its own
> mods; the entries below are a **landscape survey** — reference points and possible optional
> integration targets, evaluated per-feature on merit, never adopted wholesale.
>
> Nothing on this page is a commitment. Per
> [ADR-0003](../decisions/0003-modular-architecture-and-compatibility-policy.md), no Tier-1
> Ascension module may hard-depend on any of it.

## Purpose

Track the surrounding ecosystem: what already exists, what it does well, what it cannot do for
us, and where an optional integration jar would be worth building.

## Confirmed ecosystem facts (2026-09-05)

| Mod | Version | Notes |
|---|---|---|
| Create: Aeronautics | 1.3.2, MC 1.21.1 NeoForge | Actively maintained. Requires Create 6.0.10 + Sable. The version anchor (ADR-0001). |
| Sable | 1.21.1, NeoForge + Fabric | Physics sub-levels via Rapier. Moving block regions that stay interactive. Mixin-heavy, self-described as compat-risky. |
| Create | 6.0.10 | Required by Aeronautics. |

## Integration candidates

Mods worth an optional Tier-2 connector jar:

- **Sable** — the ships pillar. See [ADR-0006](../decisions/0006-sable-integration-posture.md).
  `ascension-compat-sable` makes sub-levels carry pressurised atmosphere zones.
- **Create / Create: Aeronautics** — in-game vehicle construction on top of Sable.
- **JEI / FTB Quests** — presentation and quest surfacing.

## Reference points (studied, not depended on)

- **Ad Astra** — reference for planet frameworks. Rejected as a chassis by
  [ADR-0004](../decisions/0004-fully-custom-dimensions.md): menu travel, flat-timer oxygen,
  orbit as a loading layer.
- **Mekanism / AE2** — reference for automation and processing pacing.
- **L_Ender's Cataclysm / Bosses of Mass Destruction** — reference for encounter construction.
  Their bosses have fixed AI; the adaptive-boss requirement in `docs/progression/combat.md`
  cannot be met by retuning them.
- **TacZ** — reference for firearm feel and ammo logistics.

## Per-mod evaluation fields

- What it does well
- What it cannot do for our design
- Whether an optional connector is worth building
- Profiling result (required before any dependency, per ADR-0007 rule 13)

## Open questions

- Which connectors are worth building for v1, beyond Sable?
- What is the fallback ship experience for players without Sable?
