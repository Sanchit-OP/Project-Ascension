# Planet 4 art: the contract between design and logic

**Status:** contract, 2026-10-20. Planet 4 is the vanilla End as an asteroid belt (ADR-0028): reached by airship, breathable,
the Ender Dragon its guardian, the Elytra its prize, its gate the Dragon's exit portal, and no floor under it (fall out of
the bottom and you are handed back to space). The story is [`docs/lore/the-end.md`](../lore/the-end.md) (an outline; ask for
prose before writing any); read it and [`what-the-warden-keeps.md`](../lore/what-the-warden-keeps.md).

Split the way the Nether's was ([`nether-art-format.md`](nether-art-format.md)):

| Owner | Scope |
|---|---|
| **Design** (its own worktree) | Every texture and look file below; its generator scripts in `tools/`; previews in `design/end/`; a design note. |
| **Logic** (the main checkout) | All behaviour: the gate (`EndGate`), the fall to space (`VoidFall`), the Dragon's numbers, worldgen, loot, text. |

Neither side edits the other's files. If the design needs something this cannot express, change this document first.
Read [`art-guide.md`](art-guide.md) first: **original art only**, every file from a seeded script in `tools/` and
reproducible byte for byte, light from the top-left, teal is the network's colour, the diamond eye its motif.

**Missing art is the placeholder.** Logic already runs without any of it: the End is a flat `color` disc from space, has
vanilla's End sky, and the quest page shows a generic banded sphere. The one placeholder that **becomes design's** is the
generic sprite that `tools/generate-quest-art.py` makes for the Planets page (section 4).

## What the player sees, in one paragraph

From space the End is not a plain sphere but a scatter of pale rock hung in the dark (section 1 says what the format can
do). A ship comes down out of an open sky onto a void with a bright central island and the Dragon's pillars below. The
Dragon dies and the exit portal wakes and offers the gates. Out in the belt, the End cities stand on their islands, with
the Elytra in a ship's item frame. Step off an island and you drift down slowly; under the belt's floor you are handed back
to the dark beside the planet.

## 1. The End from space: a belt (redone 2026-10-21)

Sanchit, 2026-10-20, of the first belt (about 160 identical lit spheres in a blob): *"it is not an asteroid belt at all."* Redone.
`ascension_space/end.json` has a `belt` (format: `worlds-api.md`, "The End is a belt, not a sphere"). What reads as a belt:

- **Wide, thin and flat:** 1,400 rocks in a layer 7% of the body's radius thick, thinnest where densest (`BeltRocks`).
- **A ring with structure:** an empty middle (`inner` 0.2), a broad inner band, a narrow bright outer band, a **gap** between them
  (`BeltRocks.density`, the one function the rocks and the dust texture share).
- **Many tiny rocks, a few big:** sizes from 1.5 blocks (fourth-power spread) with a handful of great rocks near 30, and **one
  great rock at the middle** (`central`) for the Dragon's island.
- **Irregular, tumbling:** each rock is a camera-facing sprite cut from an atlas of eight lumpy shapes
  (`end_rock.png`, 128 x 64, four to a row), turned slowly by its own spin; no rock is a sphere. Lit by the sun into a phase,
  violet on the night side.
- **Dust and a core:** a faint additive ring (`end_dust.png`) in three thin sheets at the plane, and a soft glow at the middle
  (`end_glow.png`).

Cost: three draw calls however many rocks, no allocation per frame (`BeltRenderer`), nothing static holding a level.
`tools/generate-end-belt-art.py` makes all three textures (seeded, byte for byte; `--preview` writes `design/end/belt-*.png`, a
mock of the renderer, not the game). **Not yet seen in a client**: the checklist below says what to look at.

## 2. The End's sky and haze

`assets/ascension_worlds/ascension_sky/the_end.json` (format: `worlds-api.md`, "Sky bodies" and "Haze"). The End draws
vanilla's own dark sky box; the format adds to it, it does not replace it. Design's choices:

- `haze.start` / `haze.end`: how far you see. The End has none today; a ship arriving at y 216 sees far. A light haze toward
  the horizon can sell the belt without hiding the islands.
- `bodies`: optional. Whether a distant sun or the Nether shows is design's call.
- If the look needs the vanilla sky box gone or tinted, propose it here and logic builds it.

Previews: the view down from y 216 on arrival and the view from the arena looking out.

## 3. The gate (no ruin) and the fall

The gate is the vanilla exit portal in the Dragon's arena, made by vanilla's podium feature, so design does **not** restyle it.
Optional decoration around it (a ring of the network's teal glyphs on the podium) would be a structure logic places after the
first kill; say in the handover if wanted. Not asked for in this round. The fall out of the bottom needs no art.

## 4. The quest-book planet art

`tools/generate-quest-art.py` makes `textures/quests/planets/<planet>.png` for every planet from the planet files' colours
(`planet_sprite`); the End used its `generic` style (banded sphere). **Done 2026-10-21 (`belt_sprite`):** the End's picture on
the page is its belt seen obliquely, as from space: the same eight rocks and the same bands and gap as the space view (imported
from `tools/generate-end-belt-art.py`, never copied), a violet core, rolled 16 degrees so it does not read as a line, 256 x 256
RGBA on a clear background, same `.mcmeta`. Only `end.png` changed (every other sprite regenerates byte for byte), and `chart.png`
did not change. Not yet seen in the book at its real size (about 60 to 90 px across); a 90 px preview read as a ringed belt.

## 5. The ruins, the carvings and the tablets (design round 2, 2026-10-21)

The lore is [`the-end.md`](../lore/the-end.md) (prose now, awaiting Sanchit's nod). `tools/generate-end-ruin.py` writes five
templates under `structure/end_ruin/`, `tools/generate-end-carvings.py` draws three new Sealer carvings (appended to the
`sealer_carving` block, 55 tiles now; `tools/generate-nether-art.py` cuts them), and `tools/archive-lore.py` has eight tablets.
A structure is placed with a random rotation, so every ruin is laid out to read from any side.

| Structure | Where | Holds | Says without words |
|---|---|---|---|
| `end_ruin`, the Ring of Ten | fixed, block (48, -32), outside the pillars | The Belt, The Last Keeper; carvings *the cutting*, *the keeper made* | ten lit stumps (the ten crystals) round a dais; the Sealers cut the world and the keeper |
| `end_cell`, the Quiet Cell | fixed, block (-40, 40) | The Cut Record (not the voice) | seven glyphs in its walls in Tacita's true order, 4 1 6 3 0 5 2 |
| `end_mooring`, the Mooring | random, highlands and midlands, spacing 20 | The Wings, The Towers; carving *the ships* | three empty ship cradles, gantries, seven open cages |
| `end_cloister`, the Cloister | random, midlands and highlands, spacing 24 | The Fruit, The Tall Ones | chorus planted in rows: it was not wild |
| `end_lamp`, the Last Lamp | random, highlands, midlands, barrens, spacing 14 | The Edge | a tower with a light on it, visible from the next island |

Previews: `design/end/ruin-<name>-plan.png` (floor plans, plinths numbered), `design/nether/murals.png` (all carvings, the End's last
three), `design/end/belt-*.png`.

## Handover checklist (round 2)

- [ ] Headless: `structure_starts.py` finds the Ring and the Cell; `find_blocks.py` finds the plinths, carvings and glyphs; `end-checks.txt` still passes.
- [ ] Client, from space: the belt at three distances, beside the Moon and the Nether; flat, ring-like, tumbling, dust, bright core.
- [ ] Client: each ruin, every tablet read, the three carvings, the glyph wall in the Cell, the Mooring and the Lamp on an outer island, a reload.

## Handover checklist for design

- [ ] `ascension_space/end.json` and `textures/environment/end.png` (and `end_glow.png` if used); previews of the sphere lit
      and from the approach shell.
- [ ] `ascension_sky/the_end.json` (haze and any bodies); previews from y 216 looking down and from the arena looking out.
- [x] The `end` style in `tools/generate-quest-art.py` (`belt_sprite`); the regenerated `end.png` (`chart.png` unchanged). Look at it in the book.
- [ ] Every generator reproduces the committed files byte for byte; `./gradlew build` passes.
- [ ] A design note `docs/technical/end-art-design.md` ending in a live checklist.
