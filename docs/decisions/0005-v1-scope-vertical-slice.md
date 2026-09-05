# ADR-0005: v1 scope is a three-world vertical slice

- **Status:** Accepted
- **Date:** 2026-09-05

## Context

The design docs specify seven mandatory off-world steps, each requiring an orbit layer, a
surface, a unique survival rule, a traversal wrinkle, a gating resource, a dungeon, a boss,
and a permanent post-clear shortcut — plus custom oxygen, gateway and gear systems beneath
them. That is a multi-year team-scale project.

`roadmap.md` named a vertical slice but never said how many worlds it contained.

`docs/vision.md` warns: *"A long campaign needs pacing variety, or planets will blur together
into repeated gear checks."* Building all seven before validating one end to end is the
fastest route to exactly that failure.

## Decision

**v1 ships Earth + Moon + Planet 3, finished.**

"Finished" means every system proven end to end at least once:

- constrained Earth primitive survival through first-flight readiness
- one full orbit-to-surface loop
- one authored boss unlock
- one restored gateway / repeat-travel shortcut
- oxygen mattering meaningfully across all three worlds

Planets 4 through 7 stay **designed but unbuilt**.

## Consequences

- Every system is validated once before it is duplicated six more times.
- Systems are built general and data-driven from the start (ADR-0004), so worlds 4-7 become
  content work rather than re-engineering.
- The seven-step critical path in `docs/progression/planets.md` remains the design target.
  Only the *build* order is cut.
- Planet 3 (the radiated power world) is included specifically because it is the first world
  that is not the Moon — it proves the framework generalises rather than being Moon-shaped.

## Alternatives rejected

- **All seven worlds in v1** — matches the docs, but multi-year with no playable validation
  and high risk of the blur failure `vision.md` warns about.
- **Earth only** — a real product, but leaves spaceflight, oxygen and gateways — the actual
  identity of the project — entirely unproven.
