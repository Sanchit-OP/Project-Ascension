# ADR-0031: Crude oil and the refinery are Create: Petrochem, and its fuels feed the liquid Thruster

- **Status:** Accepted, 2026-10-25 (Sanchit: "go with Petrochem ... if it has any issues we go with the Diesel Generators")
- **Date:** 2026-10-25
- **Builds on:** [ADR-0030](0030-rocket-parts-are-create-propulsion-and-aeroworks.md) (the liquid Thruster is Create Propulsion's),
  [ADR-0025](0025-rockets-are-thruster-ships.md) (a rocket is a thruster ship; helium stays its Moon fuel),
  [ADR-0018](0018-no-world-is-required.md) (nothing here is required), [ADR-0003](0003-modular-architecture-and-compatibility-policy.md)
  (foreign integration is data or a Tier-2 jar)

## Context

Create Propulsion's liquid Thruster needs a liquid fuel, and Sanchit did not want it to be helium. He asked for a mod that puts crude oil on
Earth with a full refining chain, whose products fuel the rocket. Three candidates were read from their repositories and Modrinth/CurseForge
pages (comparison table in `plans/alpha-3.md`): **Create: Diesel Generators** (safe, one refining step, no kerosene), **Create: The Factory Must
Grow** (the biggest industry, a second game inside the pack) and **Create: Petrochem** (a real chain: desalting, three distillation modes, kerosene).

Create Propulsion ships fuel data for Diesel Generators and TFMG and none for Petrochem, so Petrochem needs a few small files from us.

## Decision

1. **Create: Petrochem 1.3.3** (CurseForge project 1494004, file 8926887, MIT; Create 6.0.10 or newer is its only dependency) is in the pack.
   **Create: Diesel Generators is the named fallback** if it misbehaves in a client or on the public server.
2. **Five Thruster fuels, as data** in `ascension-compat-create` (`data/createpropulsion/thruster_fuels/petrochem/`, each with `required_mod: petrochem`):
   kerosene thrust 1.15 using 0.8 (the efficient climb fuel), gasoline 1.2 / 0.9, diesel 1.1 / 0.85, LPG 1.0 / 0.95, fuel oil 0.9 / 1.0. For scale, Propulsion's
   own table has gasoline at 1.2 / 0.9 for every mod that makes it.
3. **Oil is on Earth only, with no tag or code of ours.** Petrochem's pumpjack recipes are keyed to biomes (desert and deep ocean give petroleum, 80 to 95 mB a
   cycle; swamp and jungle give oil that is already desalted); a check proves every one names a vanilla biome, so the Moon, Nether and End yield nothing.
4. **Helium is unchanged**: still a Solid Fuel Thruster fuel and a blaze burner fuel. The oil chain is a second, liquid road, not a replacement.
5. The flight quest page gets an **oil lane** (oil, steel, pumpjack, electrolyzer, distillation, kerosene), `pack/quests/ship_fly.py`.

## What was proved, and what was not

On a dedicated server with the whole alpha.3 set plus Petrochem (staging copy, 2026-10-25): it boots in 2.3 s, no mixin conflict names Petrochem, the fluids,
the crude-oil tag and all its recipe types load, and Create Propulsion's own fuel table reports the five fuels with the numbers above (`create-checks`
52 checks, 0 failures); the quest book loads with the new lane (`quest-checks` 10/0, 236 item ids real). **Not proved:** a pumpjack, an electrolyzer or a tower
working in a client or on a ship; kerosene running a Thruster; Petrochem's machines beside Sable.

Petrochem's jar carries about 15 stray test recipes of the author's other mod (`gearbox:*`); they log "Parsing error loading recipe" at start and do nothing.

## Consequences

- **The refinery is a Nether-tier road.** Steel (iron and coal), bronze (copper and tin) and the tower's heat are all *heated* Create recipes, and a Blaze Burner needs a
  captured blaze (Planet 3). Tin needs no ore: a zinc nugget filled with a thick potion by a spout. So the Moon is reached on coal, charcoal or helium and oil is the
  upgrade that follows the Nether. That is the shape ADR-0018 asks for, but it is a progression fact Sanchit should know before he reworks progression.
- The machines need power in two forms: the electrolyzer takes Forge Energy (Crafts & Additions' alternator, aboard or on the ground), the pumpjack takes rotation.
- Petrochem has an "Expert" mode (a separate mod, `petrochem_expert`, not in this pack) that roughly doubles the fluids; its recipes would replace the base ones.
- Steel, bronze and tin items are Petrochem's, tagged `c:plates/steel` and the like. Nothing of ours registers them, so the resource ledger is unaffected.
