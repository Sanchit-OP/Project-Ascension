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

## 1. The End from space (a belt, built by logic 2026-10-20)

Sanchit: the End is not a planet-shaped planet; it should look like an asteroid belt in space. Logic built it: `ascension_space/end.json` has a
`belt` (`worlds-api.md`, "The End is a belt, not a sphere"), 160 rocks wearing `end_rock.png`. **Design now owns the look of it**: restyle
`end_rock.png` (and `tools/generate-end-belt-art.py`), tune `end.json`'s numbers (count, sizes, thickness), and propose extras the format
cannot do yet (a faint dust band, rocks that glow violet on the night side, a bright central island rock). The older text below says a
sphere is the only option; ignore that.

### (older) The End from space

`assets/ascension_worlds/ascension_space/end.json` and `textures/environment/end.png` (64 x 64, the inscribed circle is the
disc), exactly the Nether's format (`worlds-api.md`, "Space bodies"). **What the format cannot do:** a space body is one lit
sphere sized by the planet's `body_radius` (320). It cannot draw a belt, a ring or a scatter of separate rocks. Choices, in
order of cheapness:

1. A lit sphere whose face reads as a cluster of islands (pale stone with dark gaps, an `emissive` layer for a violet glow in
   the gaps). Works today.
2. Propose a new space-body field (for example `ring`: a flat annulus texture around the sphere) here first; logic builds it.

`glow` may be a faint violet. Until the file exists it is a flat disc in the planet's `color` (9075637, a dusty violet).

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
(`planet_sprite`); the End uses its `generic` style (banded sphere). Design adds an `end` style: a pale, floating-stone look
matching section 1, 256 x 256 RGBA, same `.mcmeta`. Check `chart.png` still reads with the planet at `[-10945, 30070]`.

## Not in this round

- Archive tablets for the End and a plinth (the Nether has seven). The lore outline names the beats; text comes after the
  outline is approved.
- End city or ship structures (vanilla's; loot is vanilla's). Any new block.

## Handover checklist for design

- [ ] `ascension_space/end.json` and `textures/environment/end.png` (and `end_glow.png` if used); previews of the sphere lit
      and from the approach shell.
- [ ] `ascension_sky/the_end.json` (haze and any bodies); previews from y 216 looking down and from the arena looking out.
- [ ] The `end` style in `tools/generate-quest-art.py`; the regenerated `end.png` and `chart.png`.
- [ ] Every generator reproduces the committed files byte for byte; `./gradlew build` passes.
- [ ] A design note `docs/technical/end-art-design.md` ending in a live checklist.
