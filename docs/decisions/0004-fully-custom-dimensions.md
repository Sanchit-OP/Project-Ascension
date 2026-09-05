# ADR-0004: Fully custom dimensions

- **Status:** Accepted
- **Date:** 2026-09-05

## Context

Two viable routes to planets: adopt Ad Astra's dimension and planet framework and retune it
hard, or author our own dimensions and travel system.

Ad Astra is faster to a playable slice, but its model fights three locked pillars: travel is a
menu, oxygen is a flat dimension-wide timer, and orbit is a loading layer. Retuning all three
means fighting the mod at every step and inheriting its update cadence — which also violates
ADR-0003's rule against hard third-party dependencies in Tier-1 modules.

## Decision

**Author our own dimensions and travel system.** No Ad Astra dependency.

Planets are **data-driven** (JSON), not code-per-planet, so adding a world is an authoring
task rather than an engineering task.

## Consequences

- Total control over orbit, descent, landing, travel cost, and gateway state.
- More work before anything is playable. Mitigated by ADR-0005 (three worlds) and by
  sequencing atmosphere first — it is testable in the Overworld, before any dimension exists.
- `ascension-worlds` owns the planet registry, orbit layers, and the gateway network.

## Deferred, not settled

**Is orbit a separate dimension or a high-Y band of the surface dimension?**

Seven planets x two dimensions is real chunk-loading and memory cost, and
`docs/technical/dimensions.md` already worries that orbit risks being "just a loading layer."
A high-altitude band buys the same fiction far more cheaply, but complicates worldgen and
gravity.

This is decided during the `ascension-worlds` design pass, before Moon work begins. It does
not block M0 or M1.

## Alternatives rejected

- **Ad Astra as chassis** — fastest to playable, forfeits the ships, oxygen and orbit pillars.
- **Hybrid (Ad Astra for some worlds, custom for others)** — two travel systems to maintain,
  worst of both.
