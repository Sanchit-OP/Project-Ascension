# Making art for Ascension

**What this is:** how the gateway portal and the Moon's materials were given their textures, and
what to keep in mind when making the next batch: Planet 3, boss drops, machines, anything. Written
2026-10-06 by the session that made that art, for any session making art next. The worked
examples are [`gateway-portal-design.md`](gateway-portal-design.md),
[`moon-materials-art.md`](moon-materials-art.md) and, for item sprites drawn in another game's
likeness, [`combat-art.md`](combat-art.md).

## The rules

1. **Original art only.** Every texture we ship is made here, procedurally or as hand-placed
   pixel maps in a generator script. Never copy, trace, sample or "adapt" art from vanilla,
   another mod or a texture pack, CC0 or not. Inspiration is fine; copying is not. This is
   Sanchit's call, for legal safety (2026-10-06), and it replaces the older plan to source CC0
   packs. Two things are *not* covered by this rule:
   - Worldgen placing vanilla's own blocks (e.g. netherrack on Planet 3) is the game drawing its
     own block, not us shipping art.
   - Vanilla textures may be opened **locally, in a scratch folder**, as a yardstick for tone (does
     this ore sit right in a deepslate wall?). Such a render is never committed.
   - **Combat items are ported from League of Legends and should look like them** (Sanchit,
     2026-10-06). That means our own pixels in their likeness, never Riot's pixels. The brief,
     with the reference rules, is [`combat-art-reference.md`](combat-art-reference.md).
2. **Art is code.** A PNG is the output of a seeded script in `tools/`. Change the script, never
   the PNG. Re-running a generator must reproduce every committed PNG byte for byte; `git status`
   after a run is the check.
3. **Looks, not logic.** Art owns textures, models, blockstates, lang names and purely decorative
   blocks. Logic owns behaviour: block properties (light emission, hardness, collision), worldgen,
   anything in `WorldsContent`. If the art needs logic to do something (a lit state, a light
   level), write it in the contract or handoff doc; don't implement it. The combat module itself
   belongs to the combat session; its art is drawn wherever Sanchit directs.
4. **Not done until seen in a client** (ADR-0008). Previews prove the art is *coherent*, not that
   it works in game. Every art doc ends with a live checklist.
5. **Write down where it came from.** Each batch gets a short doc: what each texture depicts, why,
   how it was made, what is still placeholder.

## The workflow

### 1. Find out what the thing is and where the player meets it

Read the block's javadoc, its planet JSON, and the worldgen that places it before drawing
anything. The questions that changed decisions last time:

- **What is it, really?** Lunar basalt is vesicular (frozen lava bubbles); highland anorthosite is
  impact breccia; regolith is ground rock plus glass beads; helium was discovered as a yellow
  line in the Sun's spectrum. Real geology and physics give each texture a reason to look the way
  it does, and stop it looking generic.
- **What surrounds it?** Check `noise_settings/<planet>.json` (`default_block`, surface rules) and
  the ore's `placed_feature` height range. The Moon turned out to be *deepslate* under thin
  anorthosite and basalt layers, and helium ore spawns at y −64..24. A basalt-hosted ore would have
  shown as a dark square in every cave wall. Titanium (Earth, y −64..0) had the same trap.
- **How much area does it cover?** One block in a wall, a whole hillside, or the entire planet's
  surface? The more area, the less structure it can carry (see Tiling).
- **Where is it seen?** In a dark cave (glow?), on the surface under sky, in the inventory at
  16 px, held in hand.

### 2. Choose a palette, with a reason

A palette is a ramp of 5–6 colours from shadow to highlight, with one hue family. Ground the hue
in what the thing is (anodised titanium really is blue-violet, helium is the Sun's element), then
check it against two lists:

| Taken by vanilla ores | Taken by us |
|---|---|
| coal black · iron beige-peach · copper orange + green · gold yellow · redstone red · lapis blue · emerald green · diamond cyan · quartz white · ancient debris brown | **teal** = the gateway network (field, runes, keystone, key) · **amber-cream** = helium · **silver + blue-violet** = titanium |

An ore that reads as another ore is a gameplay bug. The first helium pass was too orange and read
as copper. The fix was to go paler and more luminous, and give it a different *shape* (crystals,
not grains).

### 3. Write the generator

- One script per batch, `tools/generate-<topic>-art.py`, seeded, top-of-file docstring saying what
  it owns and how to run it.
- **Import shared palettes; never copy them.** `generate-moon-materials-art.py` loads the gateway
  script with `importlib` and takes `BASALT`, `ANORTH`, `TEAL` and the helpers from it, so raw
  anorthosite and the bricks cut from it can't drift apart. Copy that pattern:
  ```python
  _spec = importlib.util.spec_from_file_location('gateway_art', os.path.join(HERE, 'generate-gateway-art.py'))
  art = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(art)
  ```
- One function per texture, taking an `rng`; a fixed seed per texture in `main()`. Changing a seed
  changes the art, so review the result.
- Hand-drawn shapes are string pixel maps with a symbol key (see `HELIUM_CRYSTALS`, `RAW_HELIUM`,
  `TENDRIL`, `RUNES`). They are easy to read in a diff and easy to nudge one pixel at a time.
- `main()` also writes a contact sheet (and, where useful, an in-context image) to
  `design/<topic>/`, built only from our own art so it can be committed.

### 4. Preview, look, critique, repeat

**Look at every preview yourself** (open the PNG with the Read tool) and write down what's wrong
before changing anything. Expect three to five rounds. The previews that earned their keep:

| Preview | Catches |
|---|---|
| Contact sheet at 8× | bad shapes, noise, clashing colours |
| One texture tiled 8 × 8 | repeating motifs, stripes, visible grid; essential for terrain |
| Ore as a vein inside its real host rock | ore-as-dark-square, hue confusion with vanilla ores |
| Textured front elevation (structure, dormant and active) | does the art work *together* |
| Isometric voxel render: `tools/render-structure-preview.py` | silhouette, proportion, ruin layout, before any game run |
| Item in hand: `tools/render-item-model.py` | a 3D item's framing in first and third person, its glow by night, its swing |
| Big items at 12×, on a mid-grey | readability, accidental symbols |

### 5. Wire it up

Models and blockstates (patterns below); lang names; for new decorative blocks, a loot table,
`#minecraft:mineable/*` (via one tag of our own, so the shared file gets a single line), recipes
including stonecutter variants, and a creative-tab entry. New blocks go in their own registration
class (`GatewayDecorBlocks` is the example), never `WorldsContent`.

### 6. Verify

- Re-run the generator: `git status` must show no PNG changes you did not intend.
- Every `ascension_*` texture referenced from a model exists; no model still points at a vanilla
  texture (a 20-line Python check over `assets/*/models/**`, as run for the Moon materials).
- `./gradlew :modules:<module>:build`.
- Structures in the frame format: run the JSON through the logic's own `GatewayFrame.CODEC` and
  `match()` in all four facings in a throwaway JUnit test. That caught real behaviour (a
  symmetric frame is recognised facing the "wrong" way; arrivals land a block high).

### 7. Commit and hand off

Commit each logical step. If another session is working in the main checkout, work in your own
`git worktree` (see "Working beside other sessions"). Add the design doc with its live checklist.

## What to keep in mind

**Light comes from the top-left.** Top and left edges are lit, bottom and right shadowed. Bricks
get a lit top row and a dark mortar line under them. A *hole* is the reverse: its near (top-left)
wall is in shadow and its far rim catches light. Crystals have a bright left facet and a dark
right one.

**Value does the work, not saturation.** Most texture lives in 3–4 neighbouring steps of one
ramp. Pure white and the darkest step are accents: a specular pixel, a crack, an outline.

**Tiling: the more area, the less structure.** A texture repeats with no seam if its noise
wraps. Use `tile_noise()` (a wrapping lattice), toroidal distance for Voronoi clasts, and
`wrap()` for any feature near an edge. Then judge it tiled 8 × 8:

- Regolith covers the whole planet: fine grain, a handful of features, almost no contrast.
- Anorthosite and basalt cover cliffs: calm clasts, few bright edges, no lines.
- Bricks and masonry cover structures: can carry strong edges, because the repeat *is* the
  pattern.

What failed: high-contrast clasts formed a visible lattice, and two-pixel pits read as horizontal
stripes. Each fix was to lower the contrast or the count, never to add more detail.

**Readable at 16 px.** Items get a one-pixel dark outline (tinted toward the item's hue, not pure
black) and three or four tones per shape. Get the silhouette right first; `outline_and_shade()`
in the Moon script does a decent first pass of outline, light and shadow on any filled mask. A
smooth ellipse reads as a ball; a raw chunk needs lobes and facets.

**Shape language says what a thing is:**

| Material | Shape |
|---|---|
| crystal | elongated prisms, hard facets, a white-hot tip |
| metal | small grains with a specular pixel and a dark rim on the shadow side |
| igneous rock | fine grain plus pits (vesicles) |
| broken rock | Voronoi clasts in a thin matrix |
| sedimentary / deep rock | horizontal strata |
| masonry | courses, lit top edge, dark mortar |
| organic (the tendril) | one smooth curve, glow along the inner edge |

**No accidental symbols.** A plus sign or cross turned up four separate times: the first
floating crown, the first gatestone glyph, the keystone's rays, and plus-shaped metal grains. A
centred four-way cross reads as religious or medical; also watch for letters, faces and
hooked-cross shapes. Invented scripts (the 3 × 4 rune alphabet) are deliberately not Latin-shaped.

**One material, one palette.** Raw stone and its dressed forms (bricks, polished, pillar,
chiseled, cracked) share a ramp, so masonry looks cut from the ground around it.

**Ores sit in the rock they're found in.** Draw the host to match the ore's real surroundings
(both ores here share a deepslate-dark layered host). Distinguish ores by hue *and* shape.

**Animation:**
- Every wave must have a whole number of periods across 16 px *and* across the loop. Then a
  5 × 7 opening has no seams and the loop never jumps.
- A texture tiled many times must be low-frequency and low-contrast. The first portal field was
  busy noise that became a grid across 35 tiles.
- Values used: field 32 frames at frametime 2; glows 16–24 frames at frametime 3; both
  interpolated.
- Static for blocks that appear thousands of times (ores). Nothing there needs to shimmer.

**Glow is a second model layer.** A cutout model with the base cube, plus the same cube again
with the glowing pixels and NeoForge's per-element
`"neoforge_data": {"block_light": N, "sky_light": N}`. That draws full-bright without the block
emitting light. Use 15 for magic (keystone, runes), 12 for natural (ore crystals). Real light
emission is a block property, so it's logic's to add. Edge pixels stay out of the glow layer,
so the shape survives in a pitch-black cave.

**Translucency** is `"render_type": "minecraft:translucent"` in the model; no Java needed.

**Structures are placed in default block states.** Pre-generation places each block's default
state, which means bottom slabs and upright pillars. That's safe. Stairs face north, so avoid
them in anything pre-generated, or propose a state field in the contract first.

**Identity over novelty.** The project has a visual language now: reuse it.
- **"The ring is universal, the body is local."** Every gateway, on every planet, is ringed in
  Inscribed Gatestone; its body is that planet's stone.
- **The diamond eye** is the network's motif: the floating crown, chiseled anorthosite, the
  keystone glyph, the gateway key's bow.
- **Teal** is the network's colour, so don't spend it on anything else.

## Failures worth learning from

| What went wrong | How it was caught | Fix |
|---|---|---|
| Floating crown, gatestone glyph, keystone rays and metal grains each formed a cross | looking at 8× previews | diamond eye, runes, concentric rings, L-shaped grains |
| Portal field busy and grid-like | textured front elevation, 5 × 7 tiled | low-frequency waves, lower contrast |
| Helium read as copper ore | ore-in-host preview | paler, cream-amber, prism shapes |
| Helium ore a dark square in deepslate | scratch comparison against vanilla deepslate | shared deepslate-dark host |
| Anorthosite showed a lattice; basalt showed stripes | 8 × 8 tiling | fewer, calmer features |
| Raw titanium a smooth ball; a notch read as legs | 12× item preview | lobed, faceted mask; filled notch |
| Anorthosite flecks too bright | contact sheet | flecks mixed 50 %, rarer |
| Moon site grid shorter than the frame inside it | contract checker | pad the site to contain the frame |

## The palettes

Values live in the scripts; these are the families and what they're for.

| Name | Script | Family | Used for |
|---|---|---|---|
| `BASALT` | gateway | cool violet-grey, dark | lunar basalt and its masonry, gatestone and keystone bodies |
| `ANORTH` | gateway | pale, faintly warm | anorthosite and its masonry, the gateway key |
| `FLECK` | gateway | blue-white | feldspar glints |
| `TEAL` / `DULL` | gateway | network teal, lit / dormant | field, runes, keystone, key pupil, tendril glow |
| `REGOLITH` | moon | neutral mid-grey | lunar dust |
| `HELIUM` | moon | amber → cream → white | helium crystals |
| `DEEP` | moon | neutral grey at deepslate's brightness | host rock for deep-band ores |
| `TITAN` + `TINTS` | moon | silver; blue-violet anodised tints | titanium |

## Model and blockstate patterns

Plain cube: `{"parent": "minecraft:block/cube_all", "textures": {"all": "ascension_worlds:block/<name>"}}`.

**Slab:** three variants. `type=bottom` uses parent `block/slab`, `type=top` uses `block/slab_top`
(both with `bottom`/`top`/`side` textures), and `type=double` uses the full block's model. The
loot table drops 2 when `type=double`. Copy `moon_basalt_brick_slab`.

**Pillar:** `axis=y` uses `block/cube_column`; `axis=z` uses the `_horizontal` model with
`"x": 90`; `axis=x` uses it with `"x": 90, "y": 90`. Copy `anorthosite_pillar`.

**Glowing layer** (copy `lunar_helium_ore.json` or `gateway_keystone_active.json`):
```json
{
  "parent": "minecraft:block/block",
  "render_type": "minecraft:cutout",
  "textures": { "particle": "...", "base": "...", "glow": "..._glow" },
  "elements": [
    { "from": [0,0,0], "to": [16,16,16], "faces": { "<each face>": { "texture": "#base", "cullface": "<face>" } } },
    { "from": [0,0,0], "to": [16,16,16], "shade": false,
      "neoforge_data": { "block_light": 12, "sky_light": 12, "ambient_occlusion": false },
      "faces": { "<each face>": { "texture": "#glow", "cullface": "<face>" } } }
  ]
}
```

**Thin translucent pane** (the portal field): one element 4 px thick (`[0,0,6]`–`[16,16,10]`, two
faces), `render_type` translucent, full-bright, `"ambientocclusion": false`. Copy
`gateway_field_ns.json` / `_ew.json`.

**Item:** `{"parent": "minecraft:item/generated", "textures": {"layer0": "ascension_worlds:item/<name>"}}`.
A block item's model parents its block model.

**Animated texture:** a vertical strip, 16 wide, 16 × frames tall, with a `.png.mcmeta`
`{"animation": {"frametime": 2, "interpolate": true}}`. `write_mcmeta()` writes it.

## Tools

| Tool | Gives you |
|---|---|
| `tools/generate-gateway-art.py` | palettes above, `bricks`, `crack`, `polished`, `pillar_side`/`pillar_top`, `chiseled`, rune and eye pixel maps, glow strips, `sheet`, `write_mcmeta`, `save` |
| `tools/generate-moon-materials-art.py` | `ramp`, `tile_noise`, `wrap`, Voronoi breccia, `deep_stone`, `grains`, `outline_and_shade`, the importlib pattern |
| `tools/render-structure-preview.py` | `python tools/render-structure-preview.py <frame.json> [--site <site.json>] [--out <dir>]`: isometric front-left and back-right plus a front elevation, coloured from each block's own texture |
| `tools/generate-combat-models.py` | `weapon()`: a 3D item model from a pixel map plus a thickness per symbol (greedy-merged cuboids, glow cuboids, the map as texture); `upright_handheld()`: vanilla's hand transforms for a model drawn upright |
| `tools/render-item-model.py` | `python tools/render-item-model.py <ns:item/id> [--out <dir>] [--swing vanilla\|greatsword]`: an item model in hand, first person day and night, third person, turntable and swing, through the game's own transforms and item lighting |

The gateway files are `.../ascension_worlds/gateway_frame/gateway.json` and
`.../ascension_worlds/gateway_site/gateway_moon_site.json`.

## A new planet, step by step

Planet 3 is the example: the roofless Nether world, with netherite as its resource
(`plans/m3-vertical-slice.md`, M3.3). Design calls are Sanchit's; this is the order of questions.

1. **What is new, and what is vanilla?** If its terrain is vanilla Nether blocks, those keep
   vanilla's textures and need no art. Art is only for *our* new blocks and items: any
   planet-specific stone, ore or masonry, the boss drop, the planet's gateway site.
2. **Its material family:** a body stone, a dust or soil, an accent, each with a palette that
   doesn't collide with the reserved hues above. A Nether world sits naturally in reds, blacks and
   ash; netherite is vanilla's dark brown-black.
3. **Its gateway:** the ring stays Inscribed Gatestone. The body comes from the planet. Vanilla
   blackstone and basalt already satisfy `#gateway_frame_dark`; new dressed blocks need tag
   entries. A ruin site, if it gets a pre-generated copy, follows the Moon site's rules: never
   overlaps a frame cell, keeps the approach clear, placed in default states.
4. **Its drops and key:** the boss drop shares the gateway key's language (pale stone, diamond
   eye, teal pupil), so all keys look like one set.
5. **Previews before the game:** tiled terrain, ores in host, items at 12×, the site through
   `render-structure-preview.py`.

## Working beside other sessions

- **Check first.** Run `git status` in the main checkout. If another session has uncommitted
  work, make your own worktree (`git worktree add -b <branch> ../Project-Ascension-<topic> main`),
  and don't edit files they have open.
- **Dry-run merges** with `git merge-file` before handing back. The repo uses `core.autocrlf=true`:
  worktree files check out CRLF while some working files are LF, so normalise line endings before
  comparing, or every file looks like a whole-file conflict.
- **Before handing back,** rebase onto `main` if it moved, re-run the generator and the build, and
  confirm the branch fast-forwards.
- **Write scripts to files,** not Bash heredocs. Backslashes inside heredoc Python get mangled.
