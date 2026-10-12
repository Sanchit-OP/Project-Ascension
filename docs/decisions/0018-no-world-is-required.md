# ADR-0018: No world is required; players go where they choose

- **Status:** Accepted
- **Date:** 2026-10-12
- **Supersedes:** two of `docs/vision.md`'s non-negotiable rules, "Planets cannot be skipped in
  the main progression route" and "Unique planetary resources gate future progression", and the
  critical-path rules in `docs/progression/planets.md`.

## Context

The vision was written around a seven-step critical path: each world's resource gates the next,
and no world can be skipped. The resource-economy review (2026-10-12,
`docs/progression/resource-economy.md`) found that nothing actually enforces this. Planet 3 is
within one iron tank of Earth, so the Moon can be skipped. To restore the rule it proposed a hard
gate: a heat shield made from Moon helium that a ship must carry to land on Planet 3.

Sanchit, 2026-10-12, turned that down: "we never want any planet necessity. ppl can choose to go
anywhere they want."

## Decision

**No world is a prerequisite for another.** A player may fly to any world in any order, if they
can survive the trip and the world.

- **No hard gates.** There is no part a ship must carry to land, and no item from one world
  required to enter another. Each world keeps its own survival rule (the Moon's vacuum, Planet 3's
  rot), and that rule must be answerable with what a player could have before arriving: oxygen
  tanks from Earth iron, gold from Earth's mines.
- **Distance stays physics, not a lock.** Range is air carried times ship speed (`worlds-api.md`,
  "range is the gate"). Farther worlds are harder to reach and easier to die on the way to. A
  player can engineer past that by any route they like.
- **Materials reward rather than unlock.** A world's materials feed what makes a player stronger
  or their machines better: the combat items' build paths, ship upgrades, Create's tiers. The best
  gear asks for materials from many worlds, so visiting many is worth it without being required.
- **Mod tiers become upgrades, not walls.** Brass, which Create only makes with a Nether blaze, is
  the upgrade for Aeronautics propellers. A titanium propeller bearing, from Earth, gets a first
  ship into space.

## Consequences

- Every world must hold up for a player arriving early and under-equipped. Its danger is the
  balance, not an earlier world's key.
- "Bosses unlock capability" still stands. Boss drops become ingredients (the combat items' build
  paths), not keys to the next world. A gateway key still lights its own world's gate.
- The vision's "seven-step critical path" becomes seven worlds in any order. The lore can still
  suggest an order, as the voice does, without enforcing it.
- `tech-tree.md`'s lock types shrink to environment locks: a world's own rule, and distance.
  Production, knowledge and combat progress come from materials, not from locks.
- `todo.md`'s "late-joiner gap" gets easier: a joining player is held back only by gear, never by
  a missing unlock.
