# ADR-0016: One mixin in `ascension-compat-sable`: ship slots unique across dimensions

- **Status:** Accepted
- **Date:** 2026-10-11

## Context

Sable keeps each ship's blocks in a numbered storage slot (a "plot") of the level the ship is in,
and gives a new ship the first slot free **in that level**. Its saved data keeps the slot number,
and block entities inside it are saved at the slot's absolute coordinates, so a ship loaded
anywhere must go back into the same slot. Sable never moves a ship between levels, so for Sable
alone this is never a problem.

ADR-0015 carries ships between planets and space. A carried ship keeps its slot number, and the
slot must be free on arrival. The plain case breaks it: ship A is built on Earth in slot 0 and
flown to the Moon. Slot 0 is now free on Earth, so the next ship built there also gets slot 0, and
it can never reach the Moon while A is parked there. Two ships, built a day apart, and the second
is stuck forever.

`ShipTransfer` refuses such a crossing before anything moves, so nothing is lost, but refusing is
not a fix.

## Decision

**One `@Inject` into Sable's `SubLevelContainer.getFirstEmptyPlot`** (`SubLevelContainerMixin`):
on the server, a new ship gets the first slot that is free in **every** level, read from each
level's own occupancy record (which counts parked, unloaded ships too). Same search order as
Sable's own, so on a server where only one level ever holds ships the choice is exactly Sable's.

Per ADR-0003 rule 2: Tier 2 only, and this ADR is the justification. No Tier 1 module is touched.

## Why not without a mixin

- **Move a ship to a free slot when it crosses.** Its block entities are saved at absolute
  coordinates, and Create's block entities keep more absolute positions inside their own data.
  Rewriting every mod's block-entity data is unbounded and fragile.
- **Reserve a ship's slot in every other level from outside** (set occupancy bits through Sable's
  observer API). Sable saves those bits as if they were real ships, so the reservations need their
  own saved bookkeeping, kept in step with every way a ship can be built, split, merged or
  destroyed. Far more code than one injection, and easier to get wrong.
- **Refuse the crossing and tell the crew.** Kept as the fallback, but on its own it strands ships.

## Consequences

- Ships built **before** this jar was installed can still share a slot number, and those crossings
  are refused with a message to the crew. New worlds never see it.
- The slot space is shared across levels instead of per level: 128 × 128 = 16,384 slots for the
  whole server, against a few dozen ships expected.
- A Sable update that renames or reshapes `getFirstEmptyPlot` fails this jar at load, not silently.
  `sable_version_range` already pins the versions read (ADR-0006, `gradle.properties`).
