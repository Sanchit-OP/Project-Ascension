# The Lunar Archive — the contract between design and logic

**Status:** contract, 2026-10-06. Sanchit picked the Archive (with Earthrise) as the Moon's next
content: the **lore anchor** for the premise in [`docs/lore/world.md`](../lore/world.md), a
civilisation that spanned every nearby planet until something ended it. Split the way the gateway
was ([`gateway-frame-format.md`](gateway-frame-format.md)):

| Owner | Scope |
|---|---|
| **Design** | The building: its shape, rooms, materials, murals, lighting, as structure templates (`.nbt`) made by a seeded generator script. The *look* of every new block and item below (models, blockstates, textures, lang names), and any purely decorative blocks it needs (murals, carved panels). Previews. |
| **Logic** (`ascension-worlds` Java) | The blocks' behaviour, the lore text and its registry, the tablet item and the reading screen, the orrery's hologram, loot tables, and the worldgen plumbing that places the building on the Moon. |

Neither side edits the other's files. If the design needs something this cannot express, change
this document first. Read [`art-guide.md`](art-guide.md) before drawing anything: original art
only, art is code, light from the top-left, teal is the network's colour, the diamond eye is its
motif.

## What it is, in the game

One **Archive** per Moon, pre-generated at a fixed spot, **north of the gateway ruin** (the
gateway is at about x −160, z 0; the Archive at about **x −160, z −256**, so a player who finds
one can find the other). It is the builders' record office: a vault cut into the ground, with
something on the surface that can be seen from a distance and says "something is down here".

What the player does there, in order:

1. **Spots it.** A surface marker visible from far off (a ruined portico, a fallen spire, a
   half-buried dome; design's call) and an obvious way down.
2. **Descends.** The vault is underground: dark, airless like the rest of the Moon, lit only by
   what the builders left (runes, glowing panels).
3. **Reads the walls.** Murals telling, without words, that the builders went from world to world
   through the gates. The first time a player sees the scale of what was lost.
4. **Reads the tablets.** **Archive plinths** each hold one inscription; right-clicking reads it.
   Eight on the Moon. Together they tell the premise and stop short of the answer.
5. **Finds the orrery.** The heart of the vault: a hologram of the solar system floating over the
   **Archive Orrery**, every planet's gate marked dark or lit. It lights up as the player wakes
   gates, so it is a progress board as well as a story beat.
6. **Takes something home.** A stores room with a couple of chests (supplies, a spare tablet).

Tone: awe and loss, not horror. Quiet, grand, abandoned in a hurry.

## Blocks and items the design must use

| Id | What | Logic owns | Design owns |
|---|---|---|---|
| `ascension_worlds:archive_plinth` | A pedestal holding one inscribed tablet. Right-click reads it. Not a container: it never gives the tablet up. Its block entity names the entry it shows (below). Horizontal `facing` property, so it can face the reader. | behaviour, block entity, hardness (unbreakable in survival, like the gateway field) | model (a tablet resting on a pedestal), blockstate for 4 facings, texture, lang name |
| `ascension_worlds:archive_orrery` | The star-map base. The hologram is drawn by logic **above** it, about 3.5 blocks across and 2 blocks up. No properties. | behaviour, the hologram renderer, light level | model and texture of the base itself, lang name |
| `ascension_worlds:archive_tablet` | Item: a portable copy of one inscription (from loot). Right-click reads it. | behaviour, its data component | item texture (one texture for all entries), lang name |

Plus any **purely decorative** blocks the design wants (mural panels, carved friezes, lit rune
strips), registered the way `GatewayDecorBlocks` does it: their own class, loot tables, mining
tag, creative tab. Design owns those entirely.

### Placing a plinth with its inscription

The plinth's block entity has one field, `entry`, the id of an archive entry. In the template:

```
block: ascension_worlds:archive_plinth[facing=south]
nbt:   {id: "ascension_worlds:archive_plinth", entry: "ascension_worlds:moon/the_road"}
```

A plinth with no `entry`, or an unknown one, shows "The inscription is worn away." The eight Moon
entry ids (text is logic's, in `en_us.json`):

| Id | Placement intent |
|---|---|
| `ascension_worlds:moon/the_road` | first plinth, near the entrance: "we came here through the gate" |
| `ascension_worlds:moon/the_builders` | murals hall |
| `ascension_worlds:moon/many_worlds` | murals hall |
| `ascension_worlds:moon/the_keepers` | near the orrery |
| `ascension_worlds:moon/the_quiet` | deeper in |
| `ascension_worlds:moon/the_last_order` | deeper in |
| `ascension_worlds:moon/the_sealing` | the innermost room |
| `ascension_worlds:moon/unfinished` | the innermost room, last: breaks off mid-sentence |

**2026-10-10:** the lore was rewritten (`docs/lore/the-moon.md`), and every tablet is now the voice
of the Observers speaking as Tacita. The ids stay fixed as plinth slots, so placement is
unchanged, but the last three slots now hold different text. `the_last_order` is *The Heir's
Marks*. `the_sealing` is *The Last Stand*, where the voice claims Tacita died in that room.
`unfinished` is *The Dead Ring*, which faces the dead gate across the hall. Text lives in
`tools/archive-lore.py`.

### Chests

Chests (or barrels) in the template name their loot table in block-entity NBT:
`{LootTable: "ascension_worlds:archive/stores"}`. Logic writes that table. One or two chests,
in a stores room, not on the main path.

## The template files

- **Format:** vanilla structure templates (`.nbt`, gzipped, the format the Structure Block saves),
  `DataVersion` **3955** (1.21.1). Templates carry block states and block-entity NBT, which the
  gateway's grid format cannot, so stairs can face the right way and plinths can name their
  entries.
- **Location:** `modules/worlds/src/main/resources/data/ascension_worlds/structure/lunar_archive/`.
- **Made by a script,** `tools/generate-lunar-archive.py`, seeded and reproducible like every other
  piece of art here (art-guide rule 2). Re-running it must reproduce the committed `.nbt`
  byte-for-byte, so write NBT deterministically (sorted palette, fixed block order, fixed gzip
  mtime). Hand-building in a creative world and saving with a Structure Block is a fallback, not
  the plan: it cannot be reviewed or regenerated.
- **Size:** each piece at most **48 × 48 × 48** (the Structure Block limit and what jigsaw handles
  comfortably). A bigger Archive is several pieces joined by jigsaw blocks; name them
  `lunar_archive/<piece>.nbt`, with the entrance piece called `entrance.nbt`, which is where
  placement starts.
- **The template's origin** (its 0,0,0 corner) is placed at the structure start. Put the entrance
  piece's **surface level at y = `surface_y`** inside the template and tell logic the number:
  logic sinks the start so that row sits on the ground.
- **Empty space is `structure_void`** where the template should leave the terrain alone, and
  `air` where it should carve. The vault is underground, so rooms are air and their surroundings
  are solid blocks the template places itself (don't rely on the terrain to be the wall).
- **No entities** in the template (no paintings, item frames, armour stands): entities are
  fragile across versions. Murals are blocks.
- **Light:** the Moon has no ambient light and monsters spawn at block light 0 (`dimension_type/
  moon.json`), so a dark vault fills with Endermen and worse. Light it enough to be a place, not a
  spawner: rune strips, glow layers on decor, or real light-emitting blocks (ask logic for a light
  level on a design block; it's a block property).

## What logic adds around the templates

- `worldgen/structure/lunar_archive.json`: type `minecraft:jigsaw`, start pool
  `ascension_worlds:lunar_archive/start` → `entrance.nbt`, biomes `#ascension_worlds:moon_biomes`,
  step `underground_structures`, `terrain_adaptation: "bury"`.
- `worldgen/template_pool/lunar_archive/*.json`, one per piece, from the names design hands over.
- `worldgen/structure_set/lunar_archive.json`: `ascension_worlds:fixed_position`, `locate_at`
  `[-160, -256]`.
- `loot_table/archive/stores.json`.
- Verification the same way as the gateway: headless structure starts, then a client walk-through.

## Handover checklist for design

- [ ] Templates and the generator script; `surface_y` stated.
- [ ] Every plinth names one of the eight ids above, each id used exactly once.
- [ ] Exactly one `archive_orrery`, with at least a 5 × 5 × 4 clear volume centred above it.
- [ ] Models, blockstates, textures, lang names for the three logic blocks/items and any decor.
- [ ] Previews in `design/lunar-archive/`: an isometric cut-away, a plan of each floor, the surface
  marker from 64 blocks away.
- [ ] A design note (`lunar-archive-design.md`) ending in a live checklist.
