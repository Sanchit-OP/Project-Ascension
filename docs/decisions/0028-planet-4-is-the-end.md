# ADR-0028: Planet 4 is the End, an asteroid belt, and its gate is the Dragon's exit portal

- **Status:** Proposed (P0 of `plans/session-prompts/9-planet-4-the-end.md`; Sanchit has answered the design
  questions, he has not yet reviewed this text)
- **Date:** 2026-10-20

## Context

The alpha's stopping point is the End with the Ender Dragon and the Elytra (`plans/alpha-checklist.md`, A8).
`planets.md` and `lore-spread-proposal.md` still give Planet 4 to a "predator world where the keepers were
made". ADR-0014 left the vanilla End dormant ("meant to become planets"), and ADR-0017 turned the Nether into
Planet 3 as data. Sanchit, 2026-10-20: the End is Planet 4; breathable; the predator world slides to Planet 5;
the vanilla exit portal is this planet's gate instead of a pre-built frame ruin, joined to the other gates.

## Decision

1. **`minecraft:the_end` itself is Planet 4**, a planet entry like the Nether's (`planet/end.json`, order 40,
   position `[-10945, 30070]`). Its floating islands are the asteroid belt: the dark sky, the void between
   islands and the Elytra crossing are the look and the movement wrinkle.
2. **Air is breathable**, as the Nether is. No tank clock during the fight.
3. **The predator world is Planet 5**, and everything after it shifts one place: forge Planet 6, war machines
   Planet 7. `planets.md` and the lore proposal say so (this ADR supersedes their Planet 4 line).
4. **The gate is the exit portal.** The End has no gate ruin and no key item. When the Dragon dies, the exit
   portal in the arena becomes the End's gate: its position is recorded in `GatewayNetworkState` as the End's
   portal, the planet's gate is marked lit (the mark steps, discovery and gate milestones fire), and stepping
   into it opens the same destination screen as any gate. It does **not** end in the credits and does not send
   the player to spawn. Every other gate lists the End as a destination once it is lit, and arriving from a gate
   lands the player beside the exit portal.
   *Consequence:* there is no way home from the End before the Dragon dies except by ship. Nothing else in the
   network needs the End, so no world is blocked (ADR-0018). Falling into the void and respawning is
   unchanged (respawn rules are the player's own).
5. **The credits are kept** for the real ending (`the-ending-and-names.md`); the vanilla "first kill plays the
   credits" path is removed.
6. **The Dragon gives no heart** for build paths for now. The Elytra (End city ships) is the prize.
7. **The End's resource** is shulker shells and chorus; they pay the End half of ADR-0014's recipe debt.
8. **No Dragon key.** A fresh-world-only dimension override is needed only if the End's own dimension type blocks
   ships (checked in P1, not assumed).

9. **The void is not death** (Sanchit, 2026-10-20). The End has no survival rule: no hazard, breathable. Falling below the
   world's floor gives Slow Falling, and a few blocks under the floor the player is carried back into space beside the
   planet, as if they had passed the asteroid. It is a planet flag, `falls_to_space`, handled by `VoidFall` in
   `ascension-worlds`, so another belt world needs no class. Needs a line of lore (`the-end.md`).

## Per-planet design fields (`planets.md`)

| Field | Planet 4 |
|---|---|
| Fantasy | The belt: islands left hanging where something was taken apart. |
| Threat | The Ender Dragon; Endermen in numbers; shulkers in the End cities. |
| Constraint | The void: no floor. A fall hands you back to space. |
| Objective | Kill the Dragon; light the exit gate. |
| Prize | The Elytra (End city ships), shulker shells, the gate itself. |
| Mastery loop | Cross the gaps by Elytra, loot the cities, fly home or gate away. |
| Orbit purpose | A belt of rocks, not a sphere, from space. |
| Surface survival mechanic | None; the fall out of the belt (Slow Falling, then space). |
| Movement wrinkle | The Elytra over the void. |
| Unique resource | Shulker shells, chorus. |
| Core structure | The End cities. |
| Boss | The Ender Dragon. |
| Permanent travel improvement | The lit End gate. |

## Consequences

- The ship arrival over the End's centre would be the Dragon's arena: P1 must set a safe landing.
- The exit portal is created by vanilla's fight code, so the gate hooks the fight's end event (Tier 1,
  `ascension-worlds`; the Dragon is vanilla).
- Respawning the Dragon with crystals must not re-light or re-pay the gate (the gate is lit once; `activate`
  is idempotent).
- Lore: `docs/lore/the-end.md` outlines first (Endermen as the Observers showing through).
