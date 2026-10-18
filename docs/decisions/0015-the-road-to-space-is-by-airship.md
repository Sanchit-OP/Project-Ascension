# ADR-0015: The road to space is by airship

- **Status:** Accepted
- **Date:** 2026-10-11
- **Amended by:** [ADR-0025](0025-rockets-are-thruster-ships.md): the context's "no rocket engines" no longer holds (rockets are
  ships with thrusters, helium is their fuel). The decision below is unchanged.

## Context

Since M2.5, anything that rose past a planet's build height went to space: in practice a player
gliding up on elytra. ADR-0006 treated ships as optional, and the M2 plan expected Create
Aeronautics to arrive "later" without saying how the two would meet.

Two things changed that:

- **Survival could not reach space at all.** Elytra come from the End, which ADR-0014 made dormant.
  The only on-foot route left was a loophole: build a tower to y 319 and stand on it.
- **Sanchit's call, 2026-10-11:** the only way to space is a Create Aeronautics airship. Players
  rarely pick up Aeronautics unless the game gives them a reason and a path, so this campaign
  should *teach* it: the Moon is the obvious next step after Earth's gate, and an airship is how
  you get there. Propulsion stays simple ("we don't need to go that deep in the air pressure"):
  no vacuum mechanics, no rocket engines.

## Decision

**A Create Aeronautics ship is the way to space.** Built in two halves along the ADR-0003 line:

- **`ascension-worlds` (Tier 1) owns the line and says where it leads, for anything.** A public
  `api.SpaceTravel` says which planet a world belongs to, where something rising out of a planet's
  sky comes out in space, which planet a point in space is over, and where a vehicle coming down
  arrives. A provider registry, `api.AscentCarrier`, lets a vehicle mod claim the players riding
  it: worlds leaves a claimed player for their vehicle to carry instead of moving them alone a
  moment before their ship.
- **The on-foot route closes only when a vehicle exists.** Server setting `ascent.vehicleOnly`
  (default on): once any carrier is registered, a survival player rising past the build height
  without a ship goes nowhere and is told "The sky ends here. Only a ship can carry you further."
  With no vehicle mod installed the setting does nothing, so worlds alone never makes space
  unreachable. Creative and spectator players are never stopped.
- **`ascension-compat-sable` (Tier 2) moves the ships.** Once a second it checks the loaded ships.
  One whose centre has climbed past its planet's build height is carried into space, crew and all,
  along the line it was flying, through Sable's own ship save and load. One that reaches a
  planet's body in space is carried down to 40 blocks below that planet's build height, over the
  spot its pilot last left from.
- **A planet's air thins toward the line but does not end there.** Sable's default sky fades air
  pressure to exactly zero at a world's build height, and Aeronautics balloons and propellers both
  work from air pressure, so as shipped no balloon or propeller ship could ever reach y 320. The
  compat jar's `dimension_physics` data gives Earth and the Moon Sable's own default curve without
  that final fade: identical below y 280, then still about a third of sea-level pressure at the
  line. The Moon gets Earth's curve exactly, so a ship that can leave Earth can always leave the
  Moon again. A new planet a ship should leave needs the same file.
- **Space has no gravity for ships, and normal air.** The same data sets space's gravity to zero
  and holds air pressure at sea level. Aeronautics balloons lift against gravity, so in space they
  do nothing and nothing pulls the ship down; propellers work as they do at sea level. A ship
  coasts to a stop under Sable's own drag.

## Consequences

- A survival player needs an airship that can climb past y 320 to leave Earth. **None has been
  designed yet**, and the teaching path (an ancients' sky-dock ruin with a starter-ship schematic,
  advancements as signposts) depends on one. Air at y 320 is about a third of sea level's, so the
  ship must be designed for thin air.
- Elytra stay worthless for space travel, even once the End is a planet.
- A player who steps off a ship in space falls with ordinary gravity and has no way to steer (no
  elytra). Open problem, not solved here.
- Ships carry their storage-slot number between worlds, which needs one mixin (ADR-0016).
- The ascent check is a few ships once a second, not a world search (ADR-0007); crossing the line
  takes a ship several seconds anyway.

## Alternatives rejected

- **Keep the on-foot route alongside ships.** It undercuts the teaching goal, and the tower
  loophole is the route most players would actually find.
- **Close the on-foot route even with no vehicle mod.** Leaves worlds unusable on its own, against
  ADR-0003's first rule and Sanchit's compatibility-first priority.
- **Realistic vacuum (no pressure in space, rockets needed).** Sanchit's call: not that deep.
- **Detect ships by polling players.** A player is not a ship: an empty ship would never cross, and
  a player jumping on deck would be mistaken for the ship's height.
