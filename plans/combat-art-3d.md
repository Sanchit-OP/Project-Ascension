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
| `e42a83d` | `tools/render-item-model.py`: an item model previewed in hand, through the game's own transforms |
| `c247e8d` | 3D in-hand models: Veigo's Blade (redone), the Darkin Blade, the Black Cleaver (`tools/generate-combat-models.py`) |

- **Generators:** `tools/generate-combat-art.py` (run from repo root) writes item textures,
  armour layers, item models and the previews `design/combat-items/icons.png` and `worn.png`.
  `tools/generate-combat-models.py` writes the `<id>_3d` models and textures and the previews in
  `design/combat-items/3d/`. Rerunning either reproduces every PNG byte for byte.
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

## Sanchit's verdict on the first 3D round (2026-10-07): rejected, models to be redone

> "Looks straight from Lego, not at all immersive. We went too deep in the pixelated thing."
> He wants actual polished 3D models that look like weapons, not toys. The icons, wiring,
> animation and preview renderer stay; the models themselves are what's wrong. Specifically:
>
> - **Veigo's Blade:** more handle than sword; the blade "looks shy in front of a dagger" (the
>   blade is 14 of 32 pixels; a longsword's is about three quarters).
> - **Black Cleaver:** held the wrong way; the face-on pose points the bit sideways while the
>   swing chops forward, so it would hit with the flat or the back.
> - **Darkin Blade:** far too wide (9 of 32, about 1:3.5).
>
> Next: research how other mods' weapons look and feel in hand, pick the inspiration with
> Sanchit, then choose a route: fine Blockbench-style JSON models (slim parts, 22.5/45-degree
> rotated elements, a 64-128 px painted texture, as L_Ender's Cataclysm's weapons read), NeoForge's
> built-in OBJ loader (`neoforge:obj`: real meshes, smooth normals, glow per material via `Ka`),
> or entity models drawn by an item renderer (Cataclysm's own route, Java). No GeckoLib: it is a
> third-party dependency (ADR-0003). The record below describes the rejected round.

## 3D in-hand models: the first three are built (2026-10-07), rejected (above)

Veigo's Blade, the Darkin Blade and the Black Cleaver are drawn, wired and previewed:
`docs/technical/combat-art.md`, "3D in-hand models", says how and shows the previews. **Not seen
in a client.** Sanchit asked to see the previews before the other swords are started.

**What the plan got right, and where it changed:**

- The method held: an upright pixel map, a thickness per symbol, greedy-merged cuboids, the map
  as the 32 × 32 texture, glow in its own cuboids, palettes imported from the sprites. 43-65
  cuboids a weapon. Grids may be odd-width (15) for a centre ridge, or even (18, the Cleaver) for a
  two-pixel haft; `x0 = 8 - width / 2` keeps either centred.
- **First person does not use vanilla-minus-45.** At 32 pixels tall that pose juts out of the
  right of the view. All three stand upright at the right, face on, guard in view (fitted with a
  small least-squares script: model points to screen spots). Third person does use the recipe
  (`upright_handheld()`, grip at the model point the hand holds).
- **Veigo's old display values were not "tuned live".** They put the great guard below the view;
  the 2026-09-09 screenshot shows a different, vanilla-like pose and predates them. The new pose
  replaces them, and the preview's swing strip (`--swing greatsword`, a copy of
  `VeigosBladeClient`'s arithmetic) shows the swing still reads. If the Java changes, update
  `greatsword_hand()` in the renderer to match.
- **Glow is not neon in hand.** The item shader shades emissive faces by direction like any other
  (0.4 ambient up to 1); emissive only means "ignores darkness". Bright glow colours survive best.
- Viego's S-offset guard arms read as a tangle at 15 pixels, so they're level.

## Next

1. **Sanchit's review of the three previews** (`design/combat-items/3d/*_hand.png`), then his live
   test with the checklist in `combat-art.md`. The first live check is whether the in-game
   first-person pose matches the preview: if not, the renderer is wrong somewhere; find out how
   before tuning numbers.
2. **The other swords in 3D:** Endless Hunger, Rageblade (Guinsoo's), Spear of Shojin, Youmuu's
   (Wraithblade), Divine Sunderer. Per weapon: a `weapon()` call in `generate-combat-models.py`
   (pixel map, depths, glow, grip), add it to `HELD_3D` in `generate-combat-art.py`, run both
   generators, read the preview, iterate. Use `BIG_FIRST_PERSON` for anything 32 tall, or fit a
   pose for smaller ones with the same approach. The Spear of Shojin is a polearm: its grip sits
   low on a long haft.

## Still open after 3D

- Worn armour textures are simple (flat colour, trim and one detail each); they could get a
  richer pass.
- Sanchit's live test of everything, using the checklist in `combat-art.md`.
