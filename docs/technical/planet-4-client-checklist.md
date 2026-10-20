# Planet 4 (the End): the one batched client run

**For Sanchit, 2026-10-20.** One session, not a launch per tweak (ADR-0008). Everything below passed on a dedicated server with
stand-in players (`docs/technical/worlds-api.md`, "P8"); **none of it has been seen in a client**. Start a **fresh world** (the End's
Dragon fight is saved per world) with every module and the instance's mods. You need a ship that can reach the End: until a survival ship
flies, use a command-built one (`dev-environment.md`, "Checking ship travel without a client") or `/ascension worlds gateway travel`
as an operator.

## Getting there

- [ ] As an operator, `/execute in minecraft:the_end
      run tp @s 0 90 0` puts you over the arena, or fly a ship from space onto the End (it sits at `[-10945, 30070]` in space, south).
- [ ] **From space:** the End is a **belt**: a flat ring of many small irregular tumbling rocks with a gap in it, a faint dust band and a bright core with one great rock at the middle (not a blob of moons). View it from far, from 2,000 blocks and from near, beside the Moon. Flying into it carries the ship down.
- [ ] **Ship arrival:** the ship comes down over **empty air about 224 blocks east of the middle**, not over the arena. Note what you see.
- [ ] The planet name on the destination screen reads "The End" (after the gate is lit).

## The sky, the void, the fall

- [ ] The sky is vanilla's End sky (placeholder). Note anything that looks wrong.
- [ ] **Step off an island** (survival, no Elytra): you drift down slowly (Slow Falling) and take no damage.
- [ ] About twelve blocks below the floor you are **carried into space beside the End**, with the action-bar message "There is no floor under the
      belt. It hands you back to the dark." and Slow Falling still on.
- [ ] Fly back into the End's disc: you come down at the arrival column again.

## The Dragon

- [ ] Approach the arena: the boss bar appears; she has **4,550 health on Normal** (check the bar or `/data get entity @e[type=ender_dragon,limit=1] Health`).
- [ ] The ten pillars' crystals heal her (about 2 a second); breaking them stops that.
- [ ] A hit on her **head** does full damage; a hit on her body about a quarter. Try a sword on each.
- [ ] Her breath cloud hurts as magic (Magic Resist helps); her body and head hits are physical (Armor helps).
- [ ] Kill her. **No credits play.** The first kill gives every player on the team skill points (3), the portal wakes, and the End's gate-lit
      advancement and points arrive (2). The hand's mark gains a glyph.

## The gate

- [ ] The exit portal in the arena wakes; stepping into it opens the **destination screen** (not your bed, not the credits).
- [ ] Pick Earth (or the Moon/Nether if lit). You arrive at that world's gate.
- [ ] From another world's gate, pick The End: you land **beside the exit portal, 6 blocks east**, on the island.
- [ ] Kill her a second time by respawning (four end crystals on the portal): the gate does not change and nothing is paid twice.

## The outer islands and the Elytra

- [ ] Throw an ender pearl into a small gateway (the ring of bedrock pillars around the arena): it carries you far out.
- [ ] Find an **End city** (`/locate structure minecraft:end_city` finds the nearest, about 1,100 blocks from the middle on the first test seed).
      Chests in the cities are per-player (Lootr).
- [ ] Find a **ship** moored by a city with the **Elytra in its item frame**. Take it, put it on.
- [ ] Fly across a gap. Fireworks work. **Fly up to the top of the world: "The sky ends here" appears** and you do not leave on foot.
- [ ] Note the **frame time and memory** while flying fast over new ground (F3, or spark): the cost of generating the outer islands.
- [ ] Shulker shells drop from shulkers (two make a shulker box).

## The book and the pack

- [ ] The **Planets** page shows the End as a fourth world (its belt, a tilted ring of rock round a violet core, since 2026-10-21), each world just inside its distance ring, and the "you are here" ring centred on the world you stand on.
- [ ] The new **The Belt** page (nine quests) opens; the quests tick as you do the above (landing, Dragon, gate, small gateway, End city, Wings).
- [ ] Ships' air (Oxygen v2) is not involved: the End is breathable. Confirm no tank clock appears.

## Then

- [ ] **Quit, reload the same world**: the gate is still lit, the portal is still there, the Dragon has not come back.
- [ ] Note anything in the log (`latest.log`) that mentions `EndGate`, `VoidFall`, or errors in the End.
- [ ] Record: frame time in the arena, in the cities, and flying (`docs/technical/performance-log.md`).

## The ruins, carvings and tablets (design round 2, 2026-10-21)

- [ ] **The Ring of Ten**, just outside the pillars at about (60, -19): ten stumps with a light each round a dais, two carved stones. Read The Belt and The Last Keeper. Each carving should contradict the tablet beside it.
- [ ] **The Quiet Cell**, at about (-40, 40): a closed room, the door three wide, seven glyphs in its walls (4 1 6 3 0 5 2 from the door's left), The Cut Record on its plinth (not the voice: signed in glyphs only).
- [ ] An outer island: **the Mooring** (three cradles, gantries, open cages, the ships carving, The Wings and The Towers), **the Cloister** (chorus in rows, The Fruit and The Tall Ones), **the Last Lamp** (a tower with a light, The Edge). Say how far you flew before finding each.
- [ ] Every tablet fits its book page; the voice's end with Tacita's signature.
- [ ] A reload with the ruins' chunks unloaded and loaded again.
