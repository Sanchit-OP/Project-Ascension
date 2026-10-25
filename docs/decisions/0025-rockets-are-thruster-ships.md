# ADR-0025: Rockets are ships with thrusters, and helium is their fuel

- **Status:** Accepted, 2026-10-18 (Sanchit: the thrusters are how the rockets for space travel are made, and helium
  is the fuel). **The choice of thruster mod is superseded by [ADR-0030](0030-rocket-parts-are-create-propulsion-and-aeroworks.md)**
  (2026-10-25); the rest stands.
- **Date:** 2026-10-18
- **Amends:** [ADR-0015](0015-the-road-to-space-is-by-airship.md), whose context said "no rocket engines". Only that
  sentence changes. The decision itself, that **a Create Aeronautics ship is the way to space** and the ascent and
  descent rules, stand as written.
- **Builds on:** [ADR-0003](0003-modular-architecture-and-compatibility-policy.md) (foreign integration lives in Tier-2
  jars), [ADR-0018](0018-no-world-is-required.md) (materials reward, they do not unlock)

## Context

ADR-0015 kept propulsion simple on purpose: no vacuum mechanics and no rocket engines, so a ship climbs on balloons
and propellers. Two things since:

- **Thin air is the open problem of the road up.** At y 320 the air is about a third of sea level's, and balloons and
  propellers both weaken as they climb (`journey.md` 5.1). No survival-built ship has yet been seen to leave Earth.
- **Helium had no job.** It has plenty of supply (Moon only, iron-like abundance) and, until now, uses only in the build
  paths and a lamp that does not exist (`journey.md` 5.4).

Sanchit's call: the rockets for space travel are made with a thruster mod, and helium is the fuel.

What the stack offers, read from the mods themselves:

- Aeronautics has propellers, a hot air burner and levitite, **and no thruster block**.
- **Create Simulated Thrusters 1.1.1** (CurseForge project 1546743; also on Modrinth, where only 1.0.1 is published) adds a
  Redstone Thruster (turned by a shaft), a **Blaze Thruster** (burns fuel, no stress) and a Brass Thruster (large, steerable,
  3 by 3 by 3). It requires Create 6, Sable 1.1.3+ and Simulated 1.1.0+.
- The Blaze Thruster **burns Create's blaze-burner fuels**: it checks `create:superheated_blaze_burner_fuels` first and gives the
  blue, strongest push (1500 by default); other burner fuels give 700 (coal, logs) or 80 (sticks and the like), all settings in
  its config. Those lists are NeoForge data maps, so a fuel is a data entry.

## Decision

1. **A rocket is a Create Aeronautics ship with thrusters under it** (Create Simulated Thrusters), carried to space by the
   same rules as any ship (ADR-0015). There is still no vacuum physics and no required engine: balloons and propellers
   still work, and a thruster is a stronger, more expensive way to push. Nothing here makes a world or a part required
   (ADR-0018).
2. **Helium is a superheated blaze-burner fuel**, so it is thruster fuel and the strongest grade. Raw helium crystal burns
   for 4,800 ticks (a blaze cake burns 3,200). The entry lives in **`ascension-compat-create`** as one data file,
   `data/create/data_maps/item/superheated_blaze_burner_fuels.json`, conditional on `ascension_worlds` being loaded, so no
   Tier-1 module names a foreign mod (ADR-0003). It adds no Java.
3. **The mod is a pack dependency, not a Tier-1 one.** The quest book names its items (`create_simulated_thrusters:*`), so
   the pack must carry it; Tier-1 modules still load and run without it.

## Consequences

- **The same entry superheats Create's own blaze burners**, so a player with helium and a lit burner reaches Create's
  superheated recipes without a blaze cake. They still need a captured blaze (the Nether). It is one file to change if that
  is unwanted: move helium to `regular_blaze_burner_fuels`, or drop the entry.
- **The thrusters' own recipes need Nether goods.** The Blaze Thruster needs a blaze burner; the Redstone Thruster needs
  electron tubes (polished rose quartz, made from quartz); the Brass Thruster needs brass. So the first rocket comes after a
  first visit to the Nether, not before. Helium (Moon) and a blaze (Nether) meet in one ship, which is the intended
  cross-world cost.
- **Whether a thruster's push fades with altitude is not known.** Create Propulsion's changelog says its fuel thrusters do
  not; Create Simulated Thrusters says nothing, and has `airflow` settings. To be seen in the alpha with a real ship.
- **Licence.** The mod is All Rights Reserved. It is installed in the dev instance and must be referenced by the pack (a
  CurseForge manifest entry), never bundled in a download we publish.
- **Unseen:** the fuel entry loads without error on a dedicated server, and the mod loads beside Sable and Aeronautics, but
  no thruster has been fuelled with helium in a client. The numbers (4,800 ticks, the config thrusts) are first guesses.
- The quest book teaches it on **Aeronautics: In Flight** (thrusters, fuel, a rocket), and Create and the ship parts on
  three pages before it.

## Alternatives rejected

- **Create Propulsion: Simulated** (MIT, about a million downloads): its solid-fuel thruster has data-driven fuels and a
  cheap Overworld recipe, and its fuel thrusters are said not to lose push at altitude. Sanchit chose Create Simulated
  Thrusters instead; this one stays the fallback if the licence or the fuel rules become a problem.
- **A rocket engine of our own.** Brings a vehicle model, fuel logistics and balance for no gain over a mod that already
  works on Sable ships.
- **Helium as regular-grade fuel.** No Create side effects, but no better than coal except in burn time, so it would not
  be rocket fuel. Kept as the one-line fallback above.
