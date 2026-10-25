# ADR-0030: Rockets use Create Propulsion's thrusters, ships steer with Aeroworks, and power comes from Crafts & Additions

- **Status:** Accepted, 2026-10-25 (Sanchit, after testing alpha.2 with friends: nobody reached the Moon, "the controls were too hard")
- **Date:** 2026-10-25
- **Supersedes in part:** [ADR-0025](0025-rockets-are-thruster-ships.md): its choice of **Create Simulated Thrusters** as the
  thruster mod. Everything else in 0025 stands: a rocket is a Create Aeronautics ship with thrusters under it, carried to space by
  the rules of ADR-0015, and helium is its fuel.
- **See also:** [ADR-0031](0031-crude-oil-and-the-refinery-are-create-petrochem.md), the liquid fuel for the Thruster (crude oil, Create: Petrochem).
- **Builds on:** [ADR-0003](0003-modular-architecture-and-compatibility-policy.md) (foreign integration is data or a Tier-2 jar),
  [ADR-0018](0018-no-world-is-required.md)

## Context

Alpha.2's testers could not fly to the Moon. Sanchit's reading: the controls were too hard, and Simulated Thrusters was not what
he wanted. ADR-0025's own text named the alternative ("Create Propulsion: Simulated ... stays the fallback"), and said its fuel thrusters
are said not to lose push at altitude.

Read from the jars on 2026-10-25 (CurseForge files named in `pack/curseforge-extras.json`):

- **Create Propulsion: Simulated 1.1.5** (MIT) adds a **Thruster** (fluid fuel), a **Solid Fuel Thruster** (coal, charcoal, wood,
  blaze cake; fuels are data, `solid_thruster_fuels/*.json` and `thruster_fuels/*.json`), an **Ion Thruster** (Forge Energy), a vector
  thruster, wings, a Stirling engine, and **platinum** ore (new world generation). It requires Create 6.0.6+ and Sable 2.0.3+.
- **Create: Aeroworks 1.5.0** adds a **gyroscope** (keeps a ship level), a **joystick**, and a **control stand and desk** that take
  lever, joystick, wheel, yoke, throttle and button modules. It requires Create 6.0.0+ and Sable 1.1.0+.
- **Create Crafts & Additions 1.7.2** adds electricity (alternator, motors, accumulators, wires). Its wires do not work on a Sable
  ship alone: **Create Additions - Sable Compat 0.1.13** is the small addon that makes them.

## Decision

1. **Create Propulsion: Simulated replaces Create Simulated Thrusters** in the pack. The quest page that taught the old thrusters
   (`pack/quests/ship_fly.py`) is rewritten for the new ones.
2. **Raw helium is a fuel for the Solid Fuel Thruster**, one data file in `ascension-compat-create`
   (`data/createpropulsion/solid_thruster_fuels/ascension_worlds/raw_helium.json`, ignored when Propulsion is absent). It stays a
   superheated blaze-burner fuel too (ADR-0025 point 2).
3. **Aeroworks is the way a ship is steered and levelled**, and the quest book teaches its control stand and gyroscope before a rocket.
4. **Crafts & Additions with its Sable addon is the way a ship has stored power** (it runs the Ion Thruster and anything else electric).
5. **Brass can be crafted by hand** (3 copper and 3 zinc make 2; `ascension-compat-create`), because Propulsion's Thruster needs a
   brass casing and Create makes brass only in a mixer heated by a captured blaze.

## Consequences

- **Platinum is a new ore in the Overworld** (and the vector thruster needs it). The resource ledger and the journey map must say what
  it is for; the world is not required to have it (ADR-0018).
- **Helium is a solid item here, not a fluid.** The Solid Fuel Thruster burns it from its hopper. If a fluid helium (for the liquid
  Thruster) is wanted, that is a new fluid in `ascension-worlds`, not done.
- **The numbers are first guesses** (thrust 1.5, consumption 0.5 against coal's 1.0 and 0.4). Whether a thruster's push fades with
  altitude is still to be seen with a real ship.
- **Licences.** Aeroworks has its own licence, not read; the pack names every mod by CurseForge id and bundles none (the server zip is
  for our own server only).
- **Unseen:** all of it in a client. Server: the mods load together with Create 6.0.10, Aeronautics 1.3.2 and Sable 2.0.5 and
  `create-checks` passes with them (staging boot, 2026-10-25).

## Alternatives rejected

- **Keep Simulated Thrusters** and add only Aeroworks: Sanchit asked for better thrusters and controls.
- **A control scheme of our own.** More code to maintain for what Aeroworks already does on Sable ships.
