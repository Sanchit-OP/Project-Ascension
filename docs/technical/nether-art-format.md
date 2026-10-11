# Planet 3 art: the contract between design and logic

**Status:** contract, 2026-10-11. Planet 3 is the real Nether opened to the sky (ADR-0017): reached
by airship, wear gold against the rot, vanilla's terrain and mobs. The story is
[`docs/lore/the-nether.md`](../lore/the-nether.md); read it, and
[`what-the-warden-keeps.md`](../lore/what-the-warden-keeps.md) for the whole.

Split the way the gateway, the Archive and the lore art were
([`gateway-frame-format.md`](gateway-frame-format.md), [`lunar-archive-format.md`](lunar-archive-format.md),
[`lore-art-format.md`](lore-art-format.md)):

| Owner | Scope |
|---|---|
| **Design** (its own worktree) | Every texture, model, blockstate, structure template and look file below; any decorative blocks it registers in its own class (`NetherDecorBlocks`, the way `GatewayDecorBlocks` does it); its generator scripts in `tools/`; previews in `design/nether/`; a design note. |
| **Logic** (the main checkout) | All behaviour: the rot, the gate ruin's search and placement, the key, the tablets' text and the reader, loot tables, worldgen wiring for the templates design hands over, the haze code. |

Neither side edits the other's files. If the design needs something this cannot express, change
this document first. Read [`art-guide.md`](art-guide.md) before drawing anything: **original art
only** (never copy, trace or sample vanilla, another mod or a pack), art is code (every file from a
seeded script in `tools/`, reproducible byte for byte), light from the top-left, teal is the
network's colour, the diamond eye is its motif.

**Missing art is the placeholder.** Logic already references every path below. Two files exist as
logic's placeholders so the world works before the art lands, and **they become design's** with
this contract: `gateway_site/gateway_nether_site.json` and `ascension_sky/the_nether.json`. Replace
them outright.

**Vanilla blocks are fine to build with.** Structures may use any vanilla block (blackstone, gold,
quartz, nether bricks): the rule is about textures, not about which blocks a ruin is made of. New
blocks need original textures.


## What the player sees, in one paragraph

From space, the burning world is a dark sphere veined with glowing lava seas and patches of teal.
Its ship comes down out of an open sky into haze over netherrack plateaus and lava seas. Without
gold on, the air withers them ("The rot is in the air here. Wear gold."). In the warped forest
nearest where they landed stands the colony's gate, a ruin in black stone and gold, dormant. Out in
the crimson forests and the wastes stand the colonists' ruins, the piglins' ancestors' homes, with
tablets on plinths in the voice: our richest colony, the piglins are thieves, take the black metal.
In a soul sand valley, a carving shows a soldier falling and rising, darker each time. The
Netherite Monstrosity holds the key in its forge; when it falls it leaves the key and the Forge
Record, the Sealers' plates, signed in glyphs in an order that is almost the one on every tablet.


## 1. The Nether from space

How it looks as a planet, the way Earth and the Moon already do (`worlds-api.md`, "Space bodies").

- `assets/ascension_worlds/ascension_space/nether.json`: `{"planet": "ascension_worlds:nether",
  "texture": "ascension_worlds:textures/environment/nether.png", "lit": ..., "glow": ...}`.
- `assets/ascension_worlds/textures/environment/nether.png`: the face, 64 × 64 like `earth.png`
  and `moon.png` (the inscribed circle is the disc). Drawn as a lit sphere, shaded by the sun.
- Seen from its approach shell it fills a quarter of the view (body radius 320), so it is looked at
  closely once per trip. Until it exists, it is a flat dark red disc (the planet's `color`).
- The art guide's own sky textures come from `tools/generate-sky-art.py`; extending that script is
  the natural place.
- **Proposed by design (2026-10-11): `emissive`**, an optional texture on a space body, added
  unshaded on top of the lit face and weighted to its night side (`1 - 0.85 x daylight`), so the
  lava seas glow where the sphere goes dark. A burning world whose night side is plain black reads
  as any rock. `textures/environment/nether_glow.png` is the layer (RGBA, transparent but for the
  lava); `design/sky/nether-from-space.png` shows it. Not in `nether.json` until the format has it.


## 2. The Nether's sky and haze

The Nether has no sky of its own: vanilla draws no sun, moon or stars there, and what is overhead
is the fog colour, which follows the biome (dark red over the wastes and crimson forest, a
purple-black over the warped forest, dark teal over the soul sand valleys, grey over the deltas:
the biomes' own `fog_color`). Its time is fixed at night. Vanilla's fog closes in at about
96 blocks, which made sense under a roof; now a ship comes down at y 216 over plateaus at y 100 to
125 and lava seas at y 31.

`assets/ascension_worlds/ascension_sky/the_nether.json` (format: `worlds-api.md`, "Haze"):

- `haze.start`, `haze.end`: how far the air lets you see, in blocks. The placeholder is 24 to 160.
- `haze.color`: optional, one colour for the whole sky and haze in place of the biomes' own. Leaving
  it out keeps the per-biome colours.
- `bodies`: optional sky bodies (`worlds-api.md`, "Sky bodies"), drawn after the sky, before
  terrain. Earth and the Moon are 18,000 and about 16,000 blocks away; whether either should show
  through the haze is design's call.

Show it in previews: the view down from y 216 on arrival, and the view up from a plateau. If the
look needs something this cannot express (a gradient, embers in the air, a glow on the horizon
from the lava seas), propose it here first and logic builds it.

**Delivered (2026-10-11):** haze 48 to 192 with the biomes' own colours, and one body, the sun
through the smoke (`nether_sun.png`, unlit, additive, 8 degrees, yaw 165, elevation 32). Earth and
the Moon are not drawn. Reasons and previews: `nether-art-design.md`, section 2. Nothing more is
asked of the format for the sky.


## 3. The gate ruin

The colony's gate, where the rot came through. Lore: the warped forest is teal because it grew
around the gate under the Observers' attention, and the Endermen gather there because it is as
close as they can get to the dead gate. Gold is the colonists' ward.

- **The file:** `data/ascension_worlds/ascension_worlds/gateway_site/gateway_nether_site.json`, in
  the site format of `gateway-frame-format.md` (`key`, `layers`, `frame_offset`), replacing logic's
  placeholder ring.
- **The frame stays `gateway.json`**, shared with the Moon and Earth: never change it here. Re-skin
  it with the site's `frame_palette` (`gateway-frame-format.md`): any of the tag-keyed symbols
  (`D d P I C _`) to any block in that symbol's frame tag. The placeholder uses polished blackstone
  bricks, polished blackstone, quartz bricks, quartz pillar, chiseled polished blackstone and
  blackstone brick slabs. To use a block not yet in a tag, add it to the tag: design owns the frame
  tags, and Earth's builders then gain it too, so only add what an Earth player can also get.
- **Where it stands:** the warped-forest chunk nearest the origin with solid ground on top,
  searched outward up to 1,536 blocks; front facing south; on the surface there, the top of a
  plateau, usually y 100 to 125. On the first test world it stood at about (24, 107, 328). Every
  world differs, so design for uneven ground: the bottom layer is extended down up to 8 blocks
  through anything replaceable. Lava falls and pools can sit right beside it.
- **Size:** the site up to 30 wide × 40 tall × 18 deep, frame included.
- **No new behaviour:** the keystone, field and gatestone ring work as on the Moon. A key from the
  Monstrosity attunes it.


## 4. The colonists' ruins and their tablets

The colony's homes and halls, gone to ruin, where the piglins' ancestors lived before the gate went
dark. Blackstone, gold and basalt. Tablets on plinths, in the voice.

**The plinth.** `ascension_worlds:colony_plinth`: an archive plinth in every way but its look
(same block entity, same `entry` field, same horizontal `facing`, unbreakable). Logic registered it;
design owns its model, blockstate (four facings), texture and lang name. Its collision box is the
archive plinth's for now; say if the model needs another.

```
block: ascension_worlds:colony_plinth[facing=south]
nbt:   {id: "ascension_worlds:archive_plinth", entry: "ascension_worlds:nether/the_colony"}
```

(The block entity type keeps the archive plinth's id.)

**The seven tablets** (text is logic's, in `tools/archive-lore.py`, draft):

| Id | Title | What it says (the voice) | Placement intent |
|---|---|---|---|
| `ascension_worlds:nether/the_colony` | The Burning World | our richest world; gold like water; the black metal is yours | the first plinth a player meets |
| `ascension_worlds:nether/the_ward` | The Ward | the rot is the destroyer's breath; wear gold | early: it is the survival rule |
| `ascension_worlds:nether/the_thieves` | The Thieves | the piglins stole our gold | a ruin near the crimson forest |
| `ascension_worlds:nether/the_strongholds` | The Strongholds | the fortresses were ours; free the black soldiers | any |
| `ascension_worlds:nether/the_gift` | The Gift | our champions came back from death, as you do | any |
| `ascension_worlds:nether/the_weapon` | The Weapon | bind three skulls to the sand of the dead | any |
| `ascension_worlds:nether/the_keeper` | The Forge | a monstrosity holds the key; the ring is in the teal forest | late, or in more than one ruin |

**The templates.** `data/ascension_worlds/structure/nether_colony/<piece>.nbt`, made by a seeded
`tools/generate-nether-colony.py`, in the template rules of `lunar-archive-format.md` ("The template
files"): DataVersion 3955, deterministic NBT, `structure_void` where the terrain should stay, no
entities, at most 48 × 48 × 48 a piece.

- **Several ruins, not one building.** At least three variants (`hall_a`, `hall_b`, ...), each a
  whole ruin in one template, each with two or three plinths. Spread the seven ids so that finding
  three different ruins finds all seven. A variant may repeat `the_colony` or `the_keeper`.
- **Where they go:** scattered, findable with `/locate`, in the crimson forest and the nether wastes
  (the colony's farmland and town). Logic sets the spacing; propose one if the ruins' size calls for
  it.
- **Ground:** like the gate, on the top of a plateau. State each variant's `surface_y` (the
  template row that sits on the ground) in the handover.
- **Chests:** one small stores chest in some variants, `{LootTable: "ascension_worlds:nether_colony/stores"}`
  (a spare tablet, gold, now and then a golden helmet or boots, crying obsidian).
- **Light:** lighting does not stop Nether spawns (its block-light limit is 15), so light for looks.
- **The cracks** (`the-nether.md`): a ruin can carry a small mural of colonists in gold turning away
  a figure without it, the truth under "the thieves".


## 5. The Sealers' carving

The truth about the gift, with no words, in a soul sand valley: a soldier falling, rising, falling
and rising again, darker each time, until only a black skeleton stands. The voice has nothing to say
about it.

- One or two small templates, `data/ascension_worlds/structure/sealer_carving/<piece>.nbt`, from the
  same generator script or its own. Mural blocks as on the Moon (design's decorative blocks, their
  own textures), in the Sealers' style (`lore-art-format.md`, "The Sealers' record").
- Placed by logic in soul sand valleys, rare and findable.


## Not in this round

- Sealer prison ruins with inward arrow slits beside the fortresses, and the bastion mural: both
  would mean reworking vanilla's own structures. Later, if the carving and the colony ruins land well.
- The Netherite Monstrosity's forge: it is Cataclysm's structure. The Forge Record reaches the player
  as a tablet the Monstrosity leaves, not as plates in the forge; it uses the existing tablet item.


## What logic builds against this, in the meantime

Built 2026-10-11, all working with placeholders:

- the rot and the gold ward (`worlds-api.md`, "Hazards")
- the gate ruin's search, its placement, and the site palette (`gateway-frame-format.md`)
- the Monstrosity's key and the Forge Record in its loot (`ascension-compat-cataclysm`)
- the seven tablets' text and the Forge Record's, with the glyph signatures (`tools/archive-lore.py`)
- `ascension_worlds:colony_plinth` and `ascension_worlds:nether_colony/stores`
- the haze (`SkyHaze`)

When the templates land, logic adds their worldgen: the structures, a pool per ruin, the structure
sets and the biome tags.


## Handover checklist for design

- [x] `ascension_space/nether.json` and `textures/environment/nether.png`; a preview of the sphere
      lit and from its approach shell.
- [x] `ascension_sky/the_nether.json`: the haze (and any bodies); previews from y 216 looking down
      and from a plateau looking up. Anything the format cannot do, proposed in section 2.
- [x] `gateway_site/gateway_nether_site.json` with its palette; previews of the ruin from the front,
      from above, and from 64 blocks.
- [x] `colony_plinth`: model, blockstate (four facings), texture, lang name.
- [x] At least three colony ruin templates with the seven tablets spread across them, each
      variant's `surface_y`, a preview of each.
- [x] The Sealers' carving template(s) and their blocks.
- [x] Every generator reproduces the committed files byte for byte; `./gradlew :modules:worlds:build`
      passes.
- [x] A design note, `docs/technical/nether-art-design.md`, ending in a live checklist and the list
      of piece names and `surface_y` values for logic.


## Design's handover (2026-10-11)

Design record: [`nether-art-design.md`](nether-art-design.md). Everything below was placed by real
worldgen on a dedicated server, in fresh worlds on the first test world's seed, through a throwaway
datapack shaped as proposed here, and compared with the world block for block
(`tools/worldgen-diagnostics/template_fit.py`).

**Pieces and `surface_y`.** Every template's floor is row **1** (one row of rubble under it).

| Piece | Size | Holds | Biomes |
|---|---|---|---|
| `nether_colony/hall_a` | 21 × 16 × 32 | `the_colony`, `the_ward`, `the_strongholds` | nether wastes, crimson forest |
| `nether_colony/hall_b` | 25 × 15 × 28 | `the_colony`, `the_gift`, `the_thieves`; one stores chest | nether wastes, crimson forest (the crimson forest for preference: `the_thieves` and the colonists' mural are here) |
| `nether_colony/hall_c` | 23 × 17 × 30 | `the_colony`, `the_weapon`, `the_keeper`; one stores chest | nether wastes, crimson forest |
| `sealer_carving/wall` | 13 × 11 × 7 | the Sealers' carving, no plinth | soul sand valley |

**Worldgen, as tested.** For each: `type minecraft:jigsaw`, a start pool of one
`single_pool_element` (`projection rigid`), `size 1`, **`start_height {"absolute": -1}`**,
`project_start_to_heightmap WORLD_SURFACE_WG`, **`terrain_adaptation beard_thin`**, step
`surface_structures`. A pool per ruin, or one pool for the three halls with equal weights (any three
different ruins find all seven tablets; every ruin begins with `the_colony` at its door).

- **`beard_thin`, not `none`.** Vanilla levels the ground to a piece's lowest row plus one, so with
  the floor at row 1 it builds the plateau up to the floor and cuts it away above. Measured on 23
  ruins and 19 carvings: every template cell present (the only differences were fungi, vines and
  mushrooms grown in afterwards); round ruins on plateaus the ground sat one below the floor (means
  −1.0 to −1.7). A first version with a six-deep footing stood six above the levelled ground under
  `beard_thin`, and hung over cliffs under `none`; that is why the footing went.
- **Keep them off the lava seas (request).** One ruin in 23 and two carvings in 19 were set on a lava
  sea at y 31, because `WORLD_SURFACE_WG` counts the lava. The gate's seek already has the check that
  would stop it (`WORLD_SURFACE_WG` equal to `OCEAN_FLOOR_WG` at the start). A structure type or a
  placement condition doing the same for these would do.
- **Keep the ruins out of the basalt deltas** (as above): in an all-biomes test the deltas' features
  grew 1,100 to 2,700 basalt blocks into one ruin and poured lava through its floor.
- **Spacing (proposal).** Halls `random_spread` spacing 24, separation 8 (about one per 380 blocks of
  wastes and crimson forest); the carving spacing 32, separation 12 (rare). Both with
  `exclusion_zone {"other_set": "minecraft:nether_complexes", "chunk_count": 4}`: in a test a hall
  overlapped a fortress.
- **Loot:** chests name `ascension_worlds:nether_colony/stores`, as the contract says.

**The gate: features overwrite its frame (bug, logic's).** On the test seed the ruin stands at
(17, 105, 331) on the warped forest's border with the basalt deltas. Generated for real, 4,484 of its
4,585 cells came out as written; the rest were overwritten afterwards by features, among them
**twelve of the frame's own back step slabs, turned to lava and magma by the deltas' lava pools**. A
frame missing those cells fails the check attunement makes, so on this seed the Monstrosity's key
would not light it. Suggested fix: move `gateway_frame_nether` to step
**`top_layer_modification`**, the last, so no feature runs after it (the giant fungi then grow before
it, and its own cleared cells cut through any cap that overhangs the terrace, which reads as cut-back
growth). Or write the frame's cells again after features. The Moon's copy has no such neighbours.

**Gold and the gate.** The frame is gold (`P` and `C`): 98 gold blocks where a newly arrived player
needs gold to survive the rot. Piglins guard them as vanilla guards gold blocks, but a player who
strips them leaves a frame that fails attunement until it is repaired with any block in its tags.
Sanchit's call whether that is a lesson or a trap; if a trap, a pre-generated frame could stay
unbreakable until it is attuned, as the plinths are.

**Decorative blocks.** `NetherDecorBlocks` (`colony_mural`, `sealer_carving`), registered by one
line in `AscensionWorlds` beside `ArchiveDecorBlocks`. `colony_plinth` keeps the archive plinth's
shape box for box; its look needs nothing more.
