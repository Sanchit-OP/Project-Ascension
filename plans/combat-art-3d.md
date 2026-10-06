# Combat art: where it stands, start here

**For:** the next session on the combat-item art track (Sanchit is moving it to a new Claude
account, 2026-10-07). Self-contained: you need nothing from the old session's memory.

**Read first, in order:**

1. `CLAUDE.md`: project rules (ADR-0003 no third-party deps, ADR-0008 nothing is done until seen
   in a client, commit each step, push only when asked).
2. [`docs/technical/art-guide.md`](../docs/technical/art-guide.md): how art is made here. Art is
   code; a PNG is the output of a script in `tools/`.
3. [`docs/technical/combat-art-reference.md`](../docs/technical/combat-art-reference.md): the
   brief. What each League item looks like, and Sanchit's answers.
4. [`docs/technical/combat-art.md`](../docs/technical/combat-art.md): what's been made and how it
   is wired.

## Sanchit's direction, all of it

- **Combat items are ported from League of Legends and must look like their League
  counterparts.** Original "inspired" designs were rejected. **Our own pixels, never Riot's:**
  hand-placed in the generator, never traced, sampled or scaled down from Riot's images. ADR-0012
  already accepts the IP risk of League names and identities.
- **Reference screenshots** are in `design/reference/league/` (git-ignored, never committed;
  files `01`–`15`, indexed in the brief). For more, use the League wiki in the browser:
  `https://wiki.leagueoflegends.com/en-us/images/<Item_Name>_item.png`. The browser pane can't zoom,
  so build a scratch page in the wiki's origin with the icons as `<img>` at 250–390 px,
  `image-rendering: pixelated`, then screenshot it.
- **His answers (2026-10-07):** Darius's Axe is the **Black Cleaver**; Ninja Tabi is **Plated
  Steelcaps**; Wizard's Agony is **Spellslinger's Shoes**; Tasted Blood is **Gluttonous Greaves**;
  **16 × 16 first, then 3D**; **worn armour changes too**; **art is drawn in this art session**,
  not by the combat session. Models, animation and item art are made here and merged back.
  Gameplay logic stays the combat session's.
- Everything else about art in `art-guide.md` still holds: original pixels, vanilla only as a
  local, uncommitted yardstick.

## Done (all on `main`, not pushed)

| Commit | What |
|---|---|
| `dd299ef`, `fa2123a` | the League research brief, art-guide pointer |
| `531aafe` | all 25 combat sprites, bow draw frames, Veigo's flat icon, worn armour textures, glow layers |
| `4658af7` | one armour material per armour item (`CombatContent.armor`): diamond's stats copied, own layer texture. The only Java. |
| `e592fe9` | generator docstring |
| `eb468a4` | redraws of the four weakest: Rocketbelt (now the belt itself), Sterak's, Mortal Reminder, Spellslinger's |

- **Generator:** `tools/generate-combat-art.py` (run from repo root). It writes item textures,
  armour layers, item models and the previews `design/combat-items/icons.png` and `worn.png`.
  Rerunning reproduces every PNG byte for byte.
- **Builds:** `./gradlew :modules:combat:build -PnoDevMods --offline` passes.
- **Not seen in a client yet.** The live checklist is at the end of `combat-art.md`. Sanchit
  tests; you can't drive the Minecraft window.

## Gotchas already paid for

- **Glow in item models:** NeoForge's `neoforge:item_layers` loader is silently ignored if the
  parent chain ends at `item/generated` (vanilla bakes those itself, `ModelBakery` checks
  `GENERATION_MARKER` first). Glowing 2D models must parent `neoforge:item/default` or
  `neoforge:item/default-tool`. The generator handles it (`NEOFORGE_PARENT`).
- **Emissive data** on an item layer: `"neoforge_data": {"layers": {"1": {"block_light": 15,
  "sky_light": 15, "ambient_occlusion": false}}}`. On a cuboid element it is per element:
  `"neoforge_data": {...}` on the element (as `lunar_helium_ore.json` does).
- `neoforge:separate_transforms` puts a different model per display context: Veigo's Blade
  already uses it (`base` = 3D, `gui`/`ground`/`fixed` = the flat icon).
- **Git line endings:** `core.autocrlf=true`. Python writes LF, so `git status` can show JSON
  files as modified with no real diff. `git add` normalises them. Read files with
  `newline=''` if you need to preserve CRLF.
- **Shell:** write multi-line Python to a file with the Write tool. Big heredocs containing
  `'''` got mangled once.
- **Other sessions work in the main checkout** (worlds/Archive files). Work in a git worktree
  (`git worktree add -b <branch> ../Project-Ascension-<name> main`), rebase onto `main`, then
  `git merge --ff-only` from the main checkout.

## Next: 3D in-hand models (was just starting; nothing written yet)

**Scope:** the three headline weapons first: **Veigo's Blade** (a full redo; Sanchit said the
current one "is not looking great"), **Darken Blade** (Aatrox's Darkin Blade), **Darius's Axe**
(the Black Cleaver). Show Sanchit, then the other swords.

**What exists for Veigo:** `models/item/veigos_blade_3d.json`, 373 cuboids on a 64 × 64 atlas,
from `tools/generate-viegos-blade.mjs` (Blockbench source in `design/weapons/viegos-blade/`). Its
look is wrong: thin, dark, slate-teal, no glow, small guards. The structure is right. Bottom to
top: crescent pommel (y 0–3), lower hilt, the **great guard** (two crescent arms, x 3–13,
y 5.4–8.8, around a jewel), wrapped grip (y 7.6–12), small blade guard (y 12–13.5), long narrow
blade (y 12.5–32). It stands **upright, 32 units tall, centred on x = 8, z = 8**.
`VeigosBladeClient` gives it a custom first-person swing, and its `display` transforms were tuned
live for that. **Keep the new Veigo model in that same frame and reuse those display values**,
or the swing breaks. Once replaced, stop the `.mjs` from clobbering the new file: make it throw
"superseded", and keep it for history.

**The plan:**

1. **Model = a pixel-art front view, extruded.** Draw each weapon as a char-grid pixel map like
   the sprites, upright, 1 model unit per pixel (up to 16 wide × 32 tall). Give each pixel
   class a thickness: thin bevelled edges, a thicker spine or ridge, thicker guards, a bulging
   gem. Greedy-merge same-thickness cells into rectangular cuboid elements, z centred on 8. The
   pixel map is also the texture (32 × 32, `texture_size [32, 32]`): south faces map their
   rectangle, north faces the mirror (`[c1, r0, c0, r1]`), side faces stretch the edge texel
   column or row. Skip faces fully hidden by a thicker neighbour. Glowing pixels go in their
   own elements with element-level `neoforge_data`. Put this in a new
   `tools/generate-combat-models.py` that imports palettes from `generate-combat-art.py` (art
   guide: import, don't copy).
2. **Wiring, per weapon:** `<id>_3d.json` (elements and hand display) and `<id>_icon.json` (the
   existing 2D sprite model). `<id>.json` is a `separate_transforms` wrapper like Veigo's: 3D in
   hand, flat icon in the inventory, on the ground and in frames. `generate-combat-art.py`
   currently writes `<id>.json` as the 2D model for darken_blade and darius_axe; change it to
   write `<id>_icon.json` for those.
3. **Display transforms for upright models without a custom animation** (Darken Blade, Black
   Cleaver): start from vanilla handheld and subtract 45° from the Z rotation. That's exact,
   because `ItemTransform` is `T · Rx · Ry · Rz · S`, so an extra `Rz(-45)` folds into Z. Then add
   a translation so the grip lands where vanilla's handle is:
   `t = t_handheld + s · R(handheld) · shift`, in model units (clamped to ±80). Vanilla
   handheld: third person `[0,-90,55] / [0,4,0.5] / 0.85`, first person
   `[0,-90,25] / [1.13,3.2,1.13] / 0.68`. A 32-tall model rotated 45° spans about 1.4× a
   vanilla sword at the same scale, which suits greatswords.
4. **Preview before Sanchit tests.** Write `tools/render-item-model.py`, a numpy/Pillow
   rasteriser. The old session drafted it but it was never saved. It should:
   - resolve the model (parents, textures, `separate_transforms` base);
   - build quads from elements, or extrude `item/generated` layers;
   - apply the game's chain: first person `T(0.56,-0.52,-0.72) · display · T(-0.5,-0.5,-0.5)`
     with a 70° perspective camera; third person `ItemInHandLayer`: arm,
     `Rx(-90) · Ry(180) · T(1/16, 0.125, -0.625)`, then display;
   - light faces like items in the world (two lights, (0.2, 1, -0.7) and (-0.2, 1, 0.7), plus
     0.4 ambient);
   - also draw a three-angle turntable.

   Check the old Veigo model renders sensibly first, as calibration. Then the new ones. Commit
   the previews in `design/combat-items/`.
5. **Docs:** add a 3D section and live checklist lines to `combat-art.md`, update
   `design/weapons/viegos-blade/README.md`, and note it in the `todo.md` "Real textures" entry.

**Palettes for the three:**
- **Veigo:** spectral jade-mint, edges `#c8ffee`, light `#7ff0c8`, mid `#3fc89a`, dark `#1f7a62`,
  deep `#0e3a30`. The blade core, jewel and grip socket glow.
- **Darkin Blade:** dark gunmetal `#262532` → `#a9aec6`, a crimson channel `#c0183a` with a hot
  core `#ff5a6e` that glows, saw teeth on both edges, a swept spiky guard.
- **Black Cleaver:** near-black plates `#15151b` / `#25262e` / `#3c414c`, a polished lower edge
  `#b8c4cc`, spikes on the blade edges, a brown haft `#4a2c1c`.

Pull these from the sprites in `generate-combat-art.py` so they match.

## Still open after 3D

- The other swords in 3D (Endless Hunger, Rageblade, Shojin, Youmuu's, Divine Sunderer).
- Worn armour textures are simple (flat colour, trim and one detail each); they could get a
  richer pass.
- Sanchit's live test of everything, using the checklist in `combat-art.md`.
