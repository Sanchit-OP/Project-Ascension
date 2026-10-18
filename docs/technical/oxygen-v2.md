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
| Oxygen Emitter | 1, kept | free "basic" room holder (a dispenser with endless air) |
| Oxygen Refill Station | 1, kept | free "basic" tank filler, as today |
| Air Compressor | 2 `compat-create` | the new machine; first Java and compile dependency in that jar |
| Level-2 tank | 1 | below |
| Ship air | 2 `compat-sable` | `AnchorSpace` |

Create's own recipes for the basic emitter and station are switched off by `compat-create` in the
pack, so a Create player uses the Create chain and a player with only our jar still has air.

## Level-2 tank

Its own item (`oxygen_tank_helium`), made by smithing a tank with helium (8, from a common item tag
that the Moon's raw helium joins, so an empty tag just makes it uncraftable). Holds 2,400 mB
(10 minutes). Charge and valve state carry over. It counts toward the two-tanks-carried limit and
reads through `TankOxygenSource`, so the HUD rings need no new field. No level 3.

## Ships: what I do not know yet

Sable keeps a ship's blocks in a plot of the same level (ADR-0016), so a dispenser on a ship is
indexed like any block. Missing: translating a player's position into the plot (`AnchorSpace`,
nobody registers it). Not yet checked: how Sable reports edits inside a plot, and what happens
to the index when a ship is assembled (blocks move in bulk, no events), disassembled, carried to
space or reloaded. Increment E starts with a small spike that finds this out before any design
is trusted, and I will report what it finds.

## Increments

A look (art only, seeded script; amber-cream band for helium; shaft face on the compressor) ->
B fluid and dispenser -> C compressor -> D level-2 tank -> E ships -> F quest book, ledger, docs.
Server check class (`OxygenChecks`) covering: dispenser with air holds, empty does not, recovery,
compressor output follows speed, upgrade keeps charge, ship cabin after a move, reload. Heap and
tick cost go in `performance-log.md`. Few game launches: one combined client session.

## Questions (recommendation first)

1. **Is the picture right?** Compressor makes air; dispenser holds the room; the emitter's room job moves to the dispenser. *Yes.*
2. **No-Create story.** Keep the free emitter and station as the basic versions; the Create pack removes their recipes. *Yes.*
3. **More energy, more area** as the rules above (air per second follows RPM; room cost follows size; short means unsealed). *Yes.*
4. **Old emitter:** keep as the free basic version, not remove. *Keep.*
5. **Level 2:** its own item, 8 helium, 2,400 mB, no level 3. *Yes.*
6. **Fluid scale:** 1 unit = 1 mB, a filled-by-pipe tank and a filled-by-right-click tank are the same. *Yes.*
7. **Doors:** no sealing hatch in v2; ships use full blocks, revisit after we fly one. *No hatch.*
8. Share-air and the valve unchanged.

Also: **naming.** You called it the oxygen station. I propose "Air Compressor" for the new Create
machine so the old Refill Station keeps its name for the quest book. Fine?
