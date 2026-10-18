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

## Answers (Sanchit, 2026-10-19)

1. Picture right: yes.
2/4. No free basic versions; air depends on Create. Old blocks kept hidden, no recipes.
3. Air per second follows RPM; room cost follows size; short = unsealed.
5. Level 2 is its own item, 8 helium, 2,400 mB, no level 3.
6. 1 unit = 1 mB. 7. No hatch in v2. 8. Share-air and valve unchanged.
