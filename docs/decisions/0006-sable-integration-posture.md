# ADR-0006: Sable is an optional integration, never a hard dependency

- **Status:** Accepted
- **Date:** 2026-09-05

## Context

`docs/gameplay/vehicles.md` requires that "ships are controllable play spaces, not menu
shortcuts." That is expensive: it means block structures that move while players walk around
inside them and machines keep running.

Research on 2026-09-05 found **[Sable](https://modrinth.com/mod/sable)** — the physics library
underneath Create: Aeronautics. Sable adds *sub-levels*: moving regions of blocks,
block-entities and entities **that remain interactive while assembled**, driven by the Rapier
physics engine. It ships for 1.21.1 on both NeoForge and Fabric, has a public API and a
developer wiki.

This is the ships pillar, already solved. We do not need to write a physics engine.

The catch is stated by Sable's own authors:

> "Sable is an incredibly intrusive mod. It makes extensive use of mixins, and is prone to
> many compatibility issues with other mods."

It also adds no survival content and no in-game way to construct sub-levels — that is
Aeronautics' job.

That intrusiveness collides directly with Sanchit's first priority (compatibility) and second
(performance, no excess RAM, no leaks). Depending on it in Tier 1 would make every Ascension
module unusable to anyone unwilling to run a mixin-heavy Rust physics engine.

## Decision

**Sable support ships as `ascension-compat-sable`, a separate optional jar.**

- No Tier-1 module references Sable classes, directly or reflectively.
- Without Sable installed, every Ascension module loads and functions normally. Ships simply
  are not physics sub-levels.
- With Sable installed, `ascension-compat-sable` bridges the two: sub-levels carry
  atmosphere zones, so a sealed ship becomes a pressurised habitat.

The same posture applies to Create, Aeronautics, JEI, FTB Quests and everything else.

## Consequences

- The ships pillar is achievable without building a physics engine, at the cost of an optional
  dependency that some players will not install.
- We need a defined **fallback ship experience** for players without Sable. This is an open
  design question, tracked in `todo.md`; it does not block M0 or M1.
- `ascension-atmosphere`'s zone API must be general enough to describe a zone attached to a
  *moving* region, not just static world coordinates. **This constrains the M1 API design and
  must be accounted for there**, even though the Sable bridge itself is built much later.
- Sable's memory and tick profile must be measured before it is relied on for v1 ships, per
  ADR-0007.

## Alternatives rejected

- **Hard-depend on Sable in Tier 1** — easiest ships, breaks the primary requirement.
- **Write our own physics sub-levels** — enormous, and duplicates a maintained library.
- **Ignore Sable, ships are menus** — contradicts a locked pillar.

## Sources

- https://modrinth.com/mod/sable
- https://github.com/ryanhcode/sable
