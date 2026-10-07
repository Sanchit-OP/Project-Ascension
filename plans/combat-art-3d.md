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

## Round 2: Java entity models (2026-10-07), Veigo's Blade built, waiting on Sanchit

**The verdict on round 1** (extruded pixel maps): "looks straight from Lego, not at all
immersive; we went too deep in the pixelated thing." Veigo's blade was shorter than its hilt
("shy in front of a dagger"), the Darkin Blade about 1:3.5 wide, the Cleaver's bit faced sideways
to the swing. Icons, wiring, animation and the preview renderer stayed.

**The route, Sanchit's call** after a pros and cons list (fine JSON models, OBJ meshes, Java
entity models, GeckoLib): **Java entity models**, Cataclysm's route. Weapon looks and feel are
this session's department, so the client rendering code in `ascension-combat` is ours. GeckoLib
was ruled out not by its licence (MIT) but by ADR-0003/0012: combat is Tier 1, no third-party hard
dependency. Cataclysm was studied for technique only (its code is CC BY-NC-ND, its art all rights
reserved): one shared item renderer, a model per weapon in parts at any angle, an "eyes" glow
pass, a tick counter for animation.

**Built:** `WeaponRenderer`, `WeaponModels`, `WeaponModel`, `WeaponClient` in
`com.ascension.combat.client.weapon`; the data format; `tools/generate-combat-models.py`
rewritten as a part/box modeller with a painted 128 x 128 texture and glow layer; the preview
renderer reads the format and draws glow additively; `WeaponModelTest` pins the conventions both
sides share. Veigo's Blade is the first model (blade two thirds of the length). The Darkin Blade
and the Cleaver use their sprites in hand until they're remodelled. `combat-art.md` has the
details and the live checklist.

**Round 3, same day: how it is held.** Sanchit tested and found it felt like "a diamond sword
with a swapped model, and even the diamond sword feels better in hand". Two findings: the client
he tested had a **stale combat jar** (Oct 6) in the CurseForge instance, so he saw the old
Blockbench model, not round 2 (the preview renderer reproduced his screenshot of that old model
to 4 px, so the renderer is right); and the point stands that nothing conveyed holding it. His
calls: Viego **trails the sword in one hand while walking and fights two-handed** (slash left,
slash right, stab), using the hilt's two grips; **draw the player's hands** in first person; keep
the art **fully code-generated** and push its detail much further. Built: the third-person `hold`
(WeaponPose, an ArmPose enum extension) and the first-person `first_person` (WeaponFirstPerson,
the player's own arms on the hilt), both data in the model file, both previewed; a longer hilt.
`VeigosBladeClient` is gone. Epic Fight and Better Combat were weighed: both could drive the
animation as optional compat jars (data only), but both change how combat plays, so they are a
later, opt-in layer, not the base (Sanchit to decide if wanted).

**Next:**

1. **Sanchit tests round 3 in game,** with fresh jars in the CurseForge instance (build, copy,
   `runDevServer`, join). First checks: does each view match the preview, and does it feel held?
2. **The polish pass** (Sanchit: iterate in code, push detail further): richer texture painting
   (gradients, edge highlights, engraving on the fuller and guard), finer geometry (bevelled
   edges, a fuller groove), a closer look at the jade colour in daylight.
3. The Darkin Blade and the Black Cleaver in this route, each with its own hold: the Darkin
   Blade broad but about 1:7, very long; the Cleaver's edge facing the swing in both views, a
   long haft, held as an axe.
4. **Animation beyond the hold, to decide with Sanchit:** idle motion (a pulsing jewel, Viego's
   mist), sparks where the dragged tip meets the ground.
5. Then the other swords.

## Round 1 (rejected): extruded pixel maps, 2026-10-07; the lessons that still hold

Round 1 drew each weapon as an upright pixel map extruded one unit per pixel (`c247e8d`, git
history only). Rejected as toy-like (above). What it taught, still true for round 2:

- **First person needs its own pose.** Vanilla's sword pose turned 45 degrees (which third person
  still uses, `upright_handheld()`) juts out of the right of the view for anything this long.
  Fit one instead: choose where two model points (a guard gem, the tip) should sit on the screen,
  fix the X and Y rotation and the scale, and solve for the Z rotation and the translation
  (`python tools/render-item-model.py ... --fit` does it; see its docstring).
- **Veigo's old display values were never seen in game.** The 2026-09-09 screenshot shows a
  vanilla-like pose and predates them. Don't treat old numbers as tuned without a screenshot.
- **Check proportions against real weapons** before modelling: blade about two thirds to three
  quarters of a sword; even a broad blade about 1:7; an axe's edge faces the swing.
- **The preview copies the game's pose arithmetic** (`hold_pose()` for `WeaponPose` and
  `HoldAnimation`, `fp_pose()` and `fp_quads()` for `FirstPersonHold` and `WeaponFirstPerson`);
  if the Java changes, change them too.

## Still open after 3D

- Worn armour textures are simple (flat colour, trim and one detail each); they could get a
  richer pass.
- Sanchit's live test of everything, using the checklist in `combat-art.md`.
