# ADR-0002: Build custom mods, not a curated modpack

- **Status:** Accepted
- **Date:** 2026-09-05

## Context

The design documentation described a game. `docs/technical/mod-list.md` described a modpack.
These contradicted each other in three specific places, and all three resolved the same way.

| Design doc requires | Existing mods provide | Gap |
|---|---|---|
| "Ships are controllable play spaces, not menu shortcuts" | Ad Astra rockets: a launch cutscene and a dimension menu | Physics sub-levels or custom code |
| Custom oxygen with zones, suits, vehicles, behavioural drain | Ad Astra: a flat dimension-wide timer | Custom code |
| Bosses that "resist repetitive dominant strategies" | Cataclysm / BMD: fixed AI, retunable stats only | Custom code |

KubeJS cannot close any of these. It gates recipes and rewrites loot; it cannot author a boss
behaviour tree or run a per-tick zone-pressure model. `docs/technical/kubejs.md` already
half-acknowledged this, but the repo treated it as an open question when it was the root
question.

Direction from Sanchit: the mod list was **structural illustration, not a commitment**. Where
existing mods cannot deliver the design, we build our own.

## Decision

Project-Ascension is a **custom mod project**. Claude authors the Java.

Existing mods are reference points and optional integration targets. None is a required
foundation. Named mods are evaluated per-feature on merit, not adopted wholesale.

## Consequences

- The repository needs an implementation half, not just design docs.
- `docs/technical/mod-list.md` is reframed as a *landscape survey*, not a dependency list.
- `docs/technical/kubejs.md` is reframed: scripting is for pack-level tuning, not core systems.
- Scope is significantly larger than a pack, which is why ADR-0005 cuts v1 to three worlds.
- The design pillars in `docs/vision.md` stay intact rather than being trimmed down to fit
  whatever existing mods happen to do.

## Alternatives rejected

- **Pack-only (KubeJS + datapacks)** — would have required rewriting four non-negotiables in
  `vision.md` down to what mods already do.
- **Ad Astra as chassis** — see ADR-0004.
