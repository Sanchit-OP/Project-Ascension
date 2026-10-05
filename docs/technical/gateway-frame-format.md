# Gateway frame format — the contract between design and logic

**Status:** contract, 2026-10-06. The gateway portal is being rebuilt as a walk-through multiblock
(Nether/End-portal style). The work is split in two, owned by different sessions:

| Owner | Scope |
|---|---|
| **Design** | The frame's shape and materials (the JSON below), every block/item *look* (models, blockstates, textures, lang names), any new purely decorative blocks, the keystone's crafting recipe, the Moon site's surrounding decoration. |
| **Logic** (`ascension-worlds` Java) | Reading this file, validating a player-built frame, activation, filling the opening with the walk-through field, walk-in travel, pre-generating the Moon's copy, disabling vanilla Nether/End portals. |

Neither side edits the other's files. If the design needs something the format cannot express,
change this document first.

## Gameplay it has to serve

- **Moon:** one copy is **pre-generated** on the Moon, dormant. Activated by right-clicking its
  keystone with the Moon's **gateway key** (dropped by the Moon boss, the Ender Guardian).
- **Earth:** **no pre-generated copy.** Players build the frame themselves in survival and
  activate it by right-clicking the keystone with a **Warden tendril** (new item, dropped by the
  Warden).
- **Active:** the opening fills with a walk-through field. Walking into it opens the destination
  screen; picking a planet sends you to that planet's active portal.
- **Breaking** any block of an active frame's opening border collapses the field (like a Nether
  portal).

## Block ids the design must use

| Id | What | Logic owns | Design owns |
|---|---|---|---|
| `ascension_worlds:gateway_keystone` | The one block players right-click to activate. Exactly one per frame. Blockstate property `active` = `true`/`false`. | behaviour | model, texture(s) for both states, blockstate JSON, recipe, lang name |
| `ascension_worlds:gateway_field` | The walk-through opening when active. No collision, translucent. Blockstate property `axis` = `x`/`z` (the plane it lies in, like `minecraft:nether_portal`). | behaviour | model, animated texture (`.png` + `.png.mcmeta`), blockstate JSON, lang name |
| `ascension_worlds:warden_tendril` | Activation item for Earth's portal. | item + Warden drop | texture, item model, lang name |

Any additional blocks the design invents are decorative only, registered by the design session in
their own class (not `WorldsContent`), and listed in its design doc.

## The file

One file: `modules/worlds/src/main/resources/data/ascension_worlds/ascension_worlds/gateway_frame/gateway.json`

```jsonc
{
  // Characters used in "layers" → what must be (or is placed) there.
  "key": {
    "#": { "block": "minecraft:crying_obsidian" },
    "S": { "tag": "ascension_worlds:gateway_frame_stone", "place": "ascension_worlds:anorthosite" },
    "K": { "block": "ascension_worlds:gateway_keystone" }
  },
  "keystone": "K",          // exactly one cell in the whole pattern uses this character
  "field": "O",             // the walk-through opening; NOT listed in "key"
  "layers": [               // bottom (y = 0) to top
    [                       // one layer = rows, front (z = 0) to back
      "  #####  "           // one row = cells, left (x = 0) to right
    ]
  ]
}
```

### Rules the logic enforces (a frame breaking them fails to load, with a log message)

1. **Cell characters:** a key character, the `field` character, or a space. **Space = don't care**
   — never checked when validating a player's build, never placed when pre-generating. Use spaces
   for anything that is not part of the frame proper (terrain shows through).
2. **Key entries** are either `{"block": id}` (exactly that block) or `{"tag": id, "place": id}`
   (any block in the block tag is accepted when a player builds it; `place` is what pre-generation
   puts there, and must itself be in the tag). Use tags to let Earth players build with
   Earth-obtainable materials while the Moon's pre-generated copy uses Moon stone. Block states
   are not checked (stairs/slabs face any way when validating; pre-generation places the default
   state) — keep orientation-sensitive blocks out of the validated frame, or accept that.
3. **Field cells** all lie in **one row index (one z)** — a flat, one-block-thick plane — form one
   **solid rectangle**, at least **2 wide × 3 tall**. Every field cell must be air when a player
   activates.
4. **The field rectangle must be enclosed** in its own plane: the cell directly beyond each edge of
   the rectangle (left, right, below, above) must be a key character, not a space — that border
   is what holds the field up.
5. **Exactly one keystone cell.** It must be reachable by a player standing in front of or beside
   the frame (not buried inside it).
6. **Size limit:** at most **15 wide (x) × 20 tall (y) × 9 deep (z)**.
7. **Orientation:** row index `z = 0` is the **front**. Players may build the frame facing any of
   the four horizontal directions; the logic tries all four rotations. The Moon's pre-generated
   copy is placed with its front facing **south**. Mirrored builds are not accepted.

### Optional: the Moon site

Pre-generation can wrap the Moon's frame in extra decoration (a plaza, ruined pillars, steps):
`data/<ns>/ascension_worlds/gateway_site/<id>.json` &mdash; its **own folder** (each datapack
registry parses every file in its folder with one codec, and a site is not a frame; amended
2026-10-06, it was first specified beside the frames). Same `key`/`layers` format (no
`keystone`/`field`), plus `"frame_offset": [x, y, z]` &mdash; where the frame's own cell `(0,0,0)`
sits inside the site grid. Decoration is placed on the Moon only and never validated. Same size
limit, &times;2. A pre-generated structure names its site: `"site": "<id>"` in
`worldgen/structure/gateway_frame_*.json`.

The site's **bottom layer is extended down** through anything replaceable to solid ground (at most
8 blocks), so the ruin never floats over a crater rim. With no site, the frame's own bottom layer
gets the same treatment.

## What logic does with it

- **Validate (Earth):** on keystone right-click with the activation item, check every non-space
  cell in each of the 4 rotations around the clicked keystone. All match → fill the field cells
  with `gateway_field` (correct `axis`), set the keystone `active=true`, register the portal with
  the network.
- **Pre-generate (Moon):** place the site (if any) and the frame at a fixed spot near the arrival
  point, front facing south, `place`/`block` per cell, field cells left as air, keystone
  `active=false`.
- **Arrive:** travellers land on the ground just in front of the destination frame's field: the
  spot one block out from the field's bottom-centre, searched a few blocks up and down (and a few
  out) for real standable ground, never inside a field.
