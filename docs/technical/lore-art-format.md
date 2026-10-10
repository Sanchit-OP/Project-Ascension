# Lore art, round 1: the contract between design and logic

**Status:** contract, 2026-10-10. The story is settled enough to draw: read
[`docs/lore/what-the-warden-keeps.md`](../lore/what-the-warden-keeps.md),
[`the-mark.md`](../lore/the-mark.md) and [`the-moon.md`](../lore/the-moon.md). This round is the art
the Moon and the mark need. The burning world (Planet 3) gets its own contract when its world is
built.

Split the way the gateway and the Archive were
([`gateway-frame-format.md`](gateway-frame-format.md), [`lunar-archive-format.md`](lunar-archive-format.md)):

| Owner | Scope |
|---|---|
| **Design** (its own worktree) | Every texture, model, blockstate, font and structure template below; the decorative blocks it registers in `ArchiveDecorBlocks`; the Archive's generator and art scripts; previews; a design note. |
| **Logic** (the main checkout) | All behaviour: rendering the mark on players and illagers, syncing gate progress to clients, lighting the pillar's glyphs, the signature line in the tablet reader, the light in the sky, Endermen carrying record tiles, placing the keeper shrine, worldgen wiring, loot. |

Neither side edits the other's files. If the design needs something this cannot express, change
this document first. Read [`art-guide.md`](art-guide.md) before drawing anything: **original art
only** (never copy, trace or sample vanilla, another mod or a pack), art is code (every file from a
seeded script in `tools/`, reproducible byte for byte), light from the top-left, teal is the
network's colour, the diamond eye is its motif.

**Missing art is the placeholder.** Logic references the paths below from day one. Until design
delivers a file, the game shows the missing-texture checkerboard there. Logic never creates files
at design's paths, so the two branches cannot collide.


## What the player sees, in one paragraph

When you light your first gate, a glyph appears on the back of your right hand, glowing teal, and a
point of teal light appears on the Moon in Earth's night sky. Every gate after that adds a glyph,
and the glyphs sit around an eye that opens wider each time. On the Moon, the tall pylon over the
Archive carries the same seven glyphs, lighting one by one in step with your hand. Illagers carry
the complete mark on their crossed arms. The voice calls it the Heir's Marks; it is really the
Observers counting the seal's broken locks. Every tablet ends with Tacita's signature, written in
the same glyphs.


## 1. The script: seven glyphs and the eye

The canonical drawings, and the source every other item below is drawn from.

- **Seven glyphs**, one per gate, indexed in gate order: **0** Earth, **1** the Moon, **2** the
  burning world, **3**–**6** the later worlds. Together they are the seven signs of Tacita's name
  in the ancients' script.
- **The eye**, in **eight stages**: **0** closed, through **7** wide open. It is the network's
  diamond eye, so it must read as the same motif as the one on the gates.
- They belong to the same family as the gatestone runes (`rune_pixels()` in
  `tools/generate-gateway-art.py`): the same stroke weight and the same feel. They are not a copy of
  any real alphabet, and not the enchanting table's alphabet, which is someone else's font.
- Each glyph must be distinct in silhouette at 5 × 5 pixels and at 8 × 8. Glyphs 0–6 must never be
  confused with each other on a 4-pixel-wide arm.
- No accidental symbols (art-guide rule): no letters, no crosses, no faces.


## 2. The script as a font

Logic writes the glyphs into text: Tacita's signature under every tablet, and tooltips.

- `assets/ascension_worlds/font/script.json`: one `bitmap` provider, texture
  `ascension_worlds:font/script.png`, **height 8, ascent 7**, so it sits on a line of normal text.
- Characters: **U+E000–U+E006** are glyphs 0–6. **U+E010–U+E017** are eye stages 0–7.
- Logic uses it as the font `ascension_worlds:script`.


## 3. The mark on the player

The mark lives on the **right arm, the back of the hand and forearm**, and has **stages 1 to 7**,
the number of gates lit. There is no mark at stage 0. Stage n shows glyphs 0 to n − 1 around the
eye at stage n.

**Format B, the decal (chosen by design, agreed with Sanchit, 2026-10-10).** Format A could not
carry it. Vanilla's first-person empty hand shows only the arm's **outer** face (the skin's
u 40–44) and its front face (u 44–48), and only about the eight rows nearest the hand. That is
roughly 8 × 8 skin pixels, seen at an angle, and seven glyphs and an eye cannot be told apart in
it. So the mark is one square texture per stage, drawn as its own quad:

- `assets/ascension_worlds/textures/entity/mark/stage_<n>.png`, n = 1..7: **24 × 24** RGBA. The
  ground is transparent, and every drawn pixel is fully opaque, so a cutout render type is enough.
  Glyphs 0 to n − 1 sit in a ring, glyph 0 at the top and the rest clockwise, round the eye at
  stage n in the middle.
- **Where:** on the right arm's **outer face** (the `−x` face of `rightArm`'s cube in model
  space), over the back of the hand and the wrist, covering **4 × 4 model pixels**: `z` from −2 to
  2 and `y` from 5 to 9. The cube runs `y` −2 (shoulder) to 10 (hand end). The quad stands
  **0.3 px** out from the cube face, above the sleeve layer, which is inflated 0.25. So it sits at
  `x = −3.3` on the wide arm (cube `x` −3..1) and `x = −2.3` on the slim arm (cube `x` −2..1).
  The outer face is 4 deep on both arms, so **one texture serves wide and slim**, and only the
  quad's `x` differs.
- **Which way up:** for the wearer, like a tattoo on the back of a hand. The texture's **top edge
  lies toward the hand end** (`y = 9`) and its **left edge toward the front** (`z = −2`). The uv
  corners are (0, 0) at (y 9, z −2), (1, 0) at (y 9, z 2), (1, 1) at (y 5, z 2), and (0, 1) at
  (y 5, z −2). In first person it then reads upright, sheared by the perspective. Others see it
  upside down while the arm hangs, which is how a hand tattoo looks to anyone but its owner.
- **Light:** full-bright, in both first and third person, in the arm's own pass (it follows every
  arm pose and swing). Where the mark goes dark (in a Moon dust storm and in the Deep Dark), draw
  the same texture with ordinary lighting. No second texture is needed for that.

It must read in **first person**, at the default field of view, with an empty hand and while
holding a tool. Vanilla draws no arm at all while an item is held. Showing the mark then is a
rendering choice for logic, for example drawing the arm in its empty-hand pose behind the item.
The design note previews that case.


## 4. The illagers' mark

The complete mark, stage 7, drawn on the outer faces of the illagers' crossed arms.

- `assets/ascension_worlds/textures/entity/mark/illager.png`: **24 × 24**, the same format as the
  player's (section 3, Format B), and the same picture as `stage_7`. It is drawn as a quad on the
  **front of the crossed arms' middle box** (in `IllagerModel`'s `arms` part, the box at
  `(−4, 2, −2)` sized 8 × 4 × 4), centred: `x` −2 to 2 and `y` 2 to 6. It sits 0.1 px in front of
  the box's `−z` face, so it turns with the arms. Vindicators, evokers and pillagers share the
  model, so one texture serves all three.
- **Which way up:** for whoever faces the illager. The texture's top edge lies toward `y = 2` (the
  top of the crossed arms), and its left edge toward the illager's right (`x = −2`). The player
  wears the mark for themself, and the illagers wear it for the world. Full-bright, like the
  player's.
- It must read as the same mark as the player's. Halfway through the game the player has to notice
  that their own mark is becoming this one.


## 5. The pylon's glyph band

**`ascension_worlds:mark_glyph`**, a new decorative block, registered by design in
`ArchiveDecorBlocks`:

- Properties: **`glyph`** (0–6) and **`lit`** (boolean, default false). Light level 7 when lit.
- **Unbreakable in survival**, like the plinths, because it is story-critical. It needs a
  creative-tab entry, but no loot.
- Look: the glyph carved into the pylon's anorthosite. Unlit, a dark incised carving. Lit, a teal
  glow, drawn as a full-bright second layer the way gatestone runes are.

In `tools/generate-lunar-archive.py`, the standing pylon's script band becomes the seven glyphs,
0 to 6, all `lit=false`, readable from the court. The layout is design's call: one face, or around
the shaft.

Logic sets `lit=true` on glyph k when gate k is lit. It finds the blocks inside the Archive's
structure pieces and acts only when a gate is attuned. It looks the block up by id, so neither side
depends on the other's classes.


## 6. The Archive's murals, revised

- **Cats.** Put a cat in a corner of *The Stonecutters* and of *The Step*, plus anywhere else it
  earns its place, but sparingly. Cats are the only creatures that can see the Observers, and the
  ancients kept them in every gate hall.
- **Earth's side.** Pick one side for the blue world and keep it there in every mural. *The Quiet*'s
  dark must spread from that side: the voice says the destroyer came from the far worlds, and the
  murals show it came from Earth.
- **The Road.** The line to the blue world is cut, and its inlay has been visibly prised out, with
  gouges, not just left uninlaid.
- *Optional:* in **The Keeper**, the staff lies across the ring's opening, like a guard in a
  doorway, so the keeper reads as keeping the ring shut rather than attacking it.
- Every mural still reads left to right in every start rotation. If the tile count changes,
  `ArchiveMuralBlock.TILES` must change with it, and the art script checks that.


## 7. The Sealers' record

A new mural, **The Sealers' Record**: the truth of the Moon, told only in pictures. The porters
(Endermen) carried it out of the sealed room tile by tile and scattered it across the Moon. The
player gets it back by killing Endermen that carry its tiles.

The panels, in order:

1. People breathing beside a lit ring. The Moon's air came through the gate.
2. The dark spreading out of the blue world, along the road.
3. The Sealers walling up their own ring and prising out its keystone.
4. Keepers being carved, and handed the key.
5. People lying still beside the dark ring.
6. One figure leaving on a ship, rising on fire, toward the blue world. This is Tacita.

The size is design's call, perhaps six panels of 3 × 3 tiles. Every tile is a new `archive_mural`
tile index, so `TILES` grows.

In the template, the **sealed room** gets the record's empty frame: bare plaster (tile 0) with
gouges where tiles were torn out, in the shape of the whole record. No record tiles are placed in
the template; they are in the Endermen's arms.

**Hand back:** the record's tile range, and the layout saying which tile goes where in the frame.
Logic gives tiles to Endermen from that range, and will later check when the frame is complete.


## 8. The keeper shrine

**`ascension_worlds:keeper_shrine`**, a small structure template, no bigger than 16 × 16 × 16
(about 9 × 7 × 9 is plenty). It holds one mural: the Sealers carving a keeper and handing it the
key.

Logic places it beside the Moon's citadel, where the Ender Guardian falls, so the player finds it
only after the fight. The citadel belongs to `ascension-compat-cataclysm`, so placement is logic's
problem. The Archive's rules apply: build from the same materials, light it so nothing spawns, use
no entities, and generate the `.nbt` deterministically.


## 9. The light on the Moon

`assets/ascension_worlds/textures/environment/moon_signal.png`, 16 × 16: a point of teal light.
Logic draws it over the vanilla moon in Earth's night sky once the player has lit a gate. It must
read at the moon's size on screen, and through a spyglass.


## Not in this round

- The burning world: its own contract, when Planet 3 is built.
- Earth's Sealer sherds, the trail-ruins tablet, carvings that only show in dust storms, the helium
  lamp, gold in the craters, signature fragments in the wild citadels, and Tacita's last mural in
  the Ancient City.


## What logic builds against this, in the meantime

For reference, so design knows what will be looking at its files:

- the Archive's worldgen
- gate progress synced to clients, once per attunement, never per tick
- the orrery hologram
- the mark's render layers on players and illagers
- the pylon lighting
- the signature line in the reader
- the moon signal
- Endermen carrying record tiles
- the shrine's placement


## Handover checklist for design

Done 2026-10-10. The design record, with the record's tile range and layout, the shrine's size and
the live checklist, is [`lore-art-design.md`](lore-art-design.md).

- [x] Script sheet: seven glyphs and eight eye stages, previewed at 1×, 5 px and 8 px.
- [x] `font/script.json` and its texture; a preview of a signature line set in normal text.
- [x] Mark textures, stages 1–7 (Format B, section 3). First-person and third-person previews of
      stages 1, 4 and 7.
- [x] The illager texture, with a preview beside a player at stage 7.
- [x] `mark_glyph`: registration, models, blockstate (all fourteen states), textures, lang name. The pylon band in the generator; a preview of the lit and unlit pylon from the
      court and from 64 blocks.
- [x] Revised murals (cats, Earth's side, the Road's gouge), and `TILES` updated.
- [x] The Sealers' Record: the tiles, the empty frame in the sealed room, the tile range and layout.
- [x] `keeper_shrine.nbt`, with its size and the position of the mural's wall stated.
- [x] `moon_signal.png`.
- [x] Both generators reproduce the committed files byte for byte; `./gradlew :modules:worlds:build`
      passes.
- [x] A design note, `docs/technical/lore-art-design.md`, ending in a live checklist.
