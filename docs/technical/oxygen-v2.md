# Oxygen v2: design (draft for Sanchit, 2026-10-19)

Nothing is built yet. This says what I would build, in plain rules, and asks seven questions.
ADR: `docs/decisions/0026-air-is-a-fluid.md` (Proposed).

## The picture (my reading of your words)

```
Create rotation -> AIR COMPRESSOR (machine) -> air (a fluid) -> Create fluid tank -> Create pump -> pipes
                        |                                                                  |
                        +- right-click fills a held tank                                   v
                                                                          AIR DISPENSER: holds a closed room
```

The compressor only makes air. The dispenser is the block that keeps a room breathable. A ship
cabin is just a closed room that happens to move.

## Rules, one sentence each

- **Unit:** 1 unit of oxygen = 1 mB of air. A portable tank is 1,200 mB (5 minutes at baseline drain).
- **Compressor:** makes 0.5 mB of air per second for every RPM it is turned at (256 RPM = 128 mB/s).
  It adds stress in proportion to speed, shown by Create's goggles. Stop turning it and it stops.
- **Dispenser:** holds 4,000 mB and spends 1 mB per second per 16 blocks of room it holds
  (a 2,000-block hall needs 125 mB/s, about one compressor at full speed).
- **Short of air:** when its buffer is empty the room is not held, all of it, with the existing
  "pressure lost" message. Never half-sealed. It recovers the moment air returns.
- **More energy, more area:** a faster or more compressors give more air per second, so a bigger
  room can be held. No gas physics.
- **Cost of checking:** the dispenser does one subtraction a second. The room shape is still
  worked out only when a block next to it changes (ADR-0007).

## Blocks and tiers

| Thing | Tier | Notes |
|---|---|---|
| Air fluid | 1 | no bucket, pale-gas texture, checked in a tank, a pipe, a pump |
| Air Dispenser | 1 | NeoForge fluid capability, accepts only air; shares the emitter's room code |
| Oxygen Emitter | 1, legacy | no recipe; stays registered (hidden) so existing worlds keep working |
| Oxygen Refill Station | 1, legacy | no recipe; same; its right-click job moves to the compressor |
| Air Compressor | 2 `compat-create` | the new machine; first Java and compile dependency in that jar |
| Level-2 tank | 1 | below |
| Ship air | 2 `compat-sable` | `AnchorSpace` |

**Decided by Sanchit 2026-10-19: air depends on Create.** There is no free standalone air. Tier 1 holds
the fluid, the dispenser and the tank (no Create code), but only the Create-powered compressor makes air.
The old emitter and refill station lose their recipes and stay as hidden legacy blocks.

## Level-2 tank

Its own item (`oxygen_tank_helium`), made by smithing a tank with helium (8, from a common item tag
that the Moon's raw helium joins, so an empty tag just makes it uncraftable). Holds 2,400 mB
(10 minutes). Charge and valve state carry over. It counts toward the two-tanks-carried limit and
reads through `TankOxygenSource`, so the HUD rings need no new field. No level 3.

## Ships: what the spike found (increment E, read from Sable 2.0.5's jar, not run)

- **Position to plot:** `Sable.HELPER.getAllIntersecting(level, box)` lists ships near a point and
  each ship's `logicalPose().transformPositionInverse(world)` is the point in the ship's own plot
  (assumed from the names; **unverified**: the first ship ride will say). A point already in the plot
  grid (`isInPlotGrid`) is used as it is, in case a rider's position is plot-space.
- **Assembly and loading:** blocks move in bulk with no block events. Sable calls a
  `SubLevelObserver.onSubLevelAdded` for every ship that appears in a level, so `ShipAir` asks the
  atmosphere api to recompute every room then, and again 40 ticks later for chunks that load late.
  (`BlockSubLevelAssemblyListener` exists, but it is a block interface and blocks are Tier 1.)
- **Block edits inside a plot:** I assume the place/break events carry plot positions like any
  other; **unverified**. A wall changed by anything that raises no event is the same known gap as
  on land (it fails safe).
- **Moving and turning:** rooms are stored in plot coordinates, which do not change when the ship
  moves; only the position lookup above uses the pose.
- **Carry to space (ADR-0015):** blocks are reloaded in the other level, so each dispenser's
  `onLoad` registers it there and the observer refreshes. Its air is in its saved data. **Unverified.**
- **Known limit:** where two ships overlap the lookup returns the first. Rooms are never shared.
- **Changed API (noted per the freeze rule):** `AnchorSpace` gained a default
  `toLocal(ServerLevel, Vec3)` because a ship space needs the level; and `api.AtmosphereAnchors`
  (register a space, ask for a refresh) is new. Old implementers are unaffected.

## Ships: what the server check found (2026-10-25, `ship-checks`, 11 checks, 0 failures)

`ShipChecks` (in `ascension-compat-sable`, dormant until `ship-checks.txt` is in the game directory) builds a sealed iron room with an
Air Dispenser and a shut oak door, then turns it into a real ship with Sable's own `/sable assemble area` command and lets it fall
for five seconds. Result, on a dedicated server: the room is sealed with 26 blocks on land and in the ship's plot; the old emitter on
land is gone; the dispenser in the plot holds the room pressurised; `ShipSpace.toLocal` maps a point a rider would stand on, in the
falling ship's world position, back to the right plot cell, and that cell reads pressurised. So the assumptions above held for this
room: plot positions, the bulk-assembly refresh and the position lookup. Not covered: a ship carried to space, two ships overlapping, a
ship turned over, a wall changed from outside the ship, and blocks that leak by design (stairs, iron bars, fences, walls, an open door).

**What holds air as a wall (2026-10-25, Sanchit):** a full cube, a shut door or trapdoor, and every block in the data tag
`ascension_atmosphere:airtight`, which holds slabs and glass panes and, from the Sable compat jar, Aeronautics' adjustable burner (the hot
air balloon's). Add a block to the tag in a datapack to make it seal. `ship-checks` places every block of Aeronautics, Simulated, Offroad and
Create in a wall and lists which seal; for Aeronautics only the propellers, propeller bearings, steam vent, mounted potato cannon and
levitite blend still leak, none of them part of a cabin wall; the envelope blocks and levitite seal.

The alpha.2 report ("the dispenser kept saying the block is open though the ship was sealed") is most likely the door: until
2026-10-25 a door never counted as a wall, and an airlock is the first thing a ship has.

## Increments

A look (art only, seeded script; amber-cream band for helium; shaft face on the compressor) ->
B fluid and dispenser -> C compressor -> D level-2 tank -> E ships -> F quest book, ledger, docs.
Server check class (`OxygenChecks`) covering: dispenser with air holds, empty does not, recovery,
compressor output follows speed, upgrade keeps charge, ship cabin after a move, reload. Heap and
tick cost go in `performance-log.md`. Few game launches: one combined client session.

## Answers (Sanchit, 2026-10-19)

1. Picture right: yes.
2/4. No free basic versions; air depends on Create. Old blocks kept hidden, no recipes.
3. Air per second follows RPM; room cost follows size; short = unsealed.
5. Level 2 is its own item, 8 helium, 2,400 mB, no level 3.
6. 1 unit = 1 mB. 7. No hatch in v2. 8. Share-air and valve unchanged.

## Live checklist (client, one session)

Jars for atmosphere, compat-create and compat-sable are in the Ascension Dev instance (old ones saved in
`mods-backup-before-oxygen-v2`). Use a fresh or creative world; `/give` the items.

1. **Look:** tank in hand and in the inventory, helium tank (amber band), refill station, emitter (all legacy: `/give` only), dispenser.
2. **Compressor:** place `ascension_compat_create:air_compressor`, power it with a shaft. Goggles show speed, stress and "Making N mB of air per second". Faster shaft, more air. Right-click with an empty tank fills it ("Tank filled").
3. **Pipes:** compressor -> mechanical pump -> fluid pipe -> Air Dispenser. Air shows in a Create tank and a pipe as a pale gas.
4. **Dispenser:** in a sealed room on the Moon (or `/atmosphere` debug vacuum), right-click it for its air and need; the room is breathable while it has air. Cut the pipe: "Pressure lost" when the buffer runs out; reconnect: "Pressure restored". Break a wall: pressure lost.
5. **Helium tank:** craft a tank + 8 raw helium (`#c:raw_materials/helium`; Moon's `raw_helium`). Charge and valve state carry over. Holds 10 minutes.
6. **Reload:** quit and rejoin; dispenser air and room still there.
7. **Ship (the unknown):** build an Aeronautics ship with a sealed cabin and a dispenser. Does standing in the cabin read breathable? After the ship moves and turns? After a carry to space? Report what happens.
