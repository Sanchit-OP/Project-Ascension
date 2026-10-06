# Combat items — art

**Status:** art, 2026-10-07: sprites and worn armour (first pass), then a held 3D model for
Veigo's Blade drawn by the combat module's own weapon renderer (see
[3D in-hand models](#3d-in-hand-models)). **Not yet seen in a running client** (ADR-0008). The module builds and the generators reproduce every PNG byte for byte, but nobody
has held these items, drawn the bows or worn the armour in game.

| | |
|---|---|
| Generators | `tools/generate-combat-art.py` (sprites, worn armour, item models), `tools/generate-combat-models.py` (held 3D weapon models); rerun them, never edit a PNG |
| Previews | `design/combat-items/icons.png` (every sprite, plus the bows' draw frames), `worn.png` (each armour piece on a figure), `3d/` (each 3D model in hand, from `tools/render-item-model.py`) |
| Brief | [`combat-art-reference.md`](combat-art-reference.md): what each League item looks like, and Sanchit's answers |
| Method | [`art-guide.md`](art-guide.md) |
| Next | [`plans/combat-art-3d.md`](../../plans/combat-art-3d.md): where the track stands, and the 3D model plan |

![icons](../../design/combat-items/icons.png)

Every combat item now has its own art in place of borrowed vanilla gear: 25 sprites at 16 × 16,
three draw frames per bow, a flat inventory icon for Veigo's Blade, and **its own worn look for
every armour piece**.

**Likeness, our pixels.** These are ported League items and Sanchit wants them to look like
their League counterparts (2026-10-06; a first, "original-styled" round was rejected). Each
sprite keeps its source's silhouette, palette and one or two signature details. Every pixel is
placed by hand in the generator's pixel maps: nothing is traced, sampled or scaled down from
Riot's images. The reference screenshots live in `design/reference/league/`, which git ignores.

## Sanchit's calls (2026-10-07)

| Question | Answer |
|---|---|
| Darius's Axe | the **Black Cleaver** |
| Ninja Tabi | today's **Plated Steelcaps** |
| Wizard's Agony | **Spellslinger's Shoes** (Sorcerer's Shoes' tier 3) |
| Tasted Blood | **Gluttonous Greaves** (a 2026 boots item, Aatrox's quote) |
| 2D or 3D | **16 × 16 first**, 3D in-hand models later |
| Worn armour | **changes too**: each piece looks like itself on the body |
| Who draws | **this session**: models, animation and item art are made outside the combat session and merged back |

## What each one is

| Item | League source | What the sprite keeps |
|---|---|---|
| `veigos_blade` | Viego's sword | long narrow jade blade, the two crescent guard arms, crescent pommel, diamond socket; glows |
| `darken_blade` | Aatrox's Darkin Blade | broad dark blade, saw teeth on both edges, a glowing crimson channel, swept guard |
| `darius_axe` | Black Cleaver | a huge near-black double-bit head, spiked edges, cracked plates, polished lower edge, wooden haft |
| `endless_hunger` | Endless Hunger | one of the icon's black crescent sickles: gold outer edge, glowing crimson inner edge |
| `ragefang` | Guinsoo's Rageblade | flame-tongued orange-gold blade with a red core; the tongue tips glow |
| `spear_of_shojin` | Spear of Shojin | the lime-jade dragon-claw blade on a gold collar, long haft |
| `wraithblade` | Youmuu's Ghostblade | silver blade with a dark fuller, broad gold crossguard round a glowing violet gem |
| `divine_sunderer` | Divine Sunderer | gold half-moon head with an emerald inlay arc, navy shaft |
| `mortal_reminder` | Mortal Reminder | steel bow with saw spikes, gold bands, a purple grip |
| `heartwood_of_pallas` | Varus's bow | dark violet chitin, glowing magenta spots and a lilac core at the grip, a claw at the grip |
| `galeforce` | Galeforce | silver-white bow, swept blade fins at the tips, cyan at the grip |
| `protean_mask` | Jak'Sho | a spiky void-purple mask, glowing red eyes and cracks |
| `edge_of_night` | Edge of Night | an indigo hood, a dark face, two faint glowing eyes |
| `gargoyle_stoneplate` | Gargoyle Stoneplate | a pale stone cuirass with broad pauldrons, glowing gold cracks |
| `guardian_wing` | Guardian Angel | a gold breastplate, white wings at the shoulders, a glowing cyan crystal |
| `rocketbelt` | Hextech Rocketbelt | the belt itself: two gold rocket cones, a blue crystal buckle, glowing blue exhaust jets |
| `collectors_pants` | The Collector | dark gunmetal with gold filigree (League's icon is a gun) |
| `unending_despair` | Unending Despair | green-black spiked carapace with glowing cyan veins and core |
| `mercury_treads` | Mercury's Treads | brown leather, steel cuff, toe cap and sole, a gold-set sapphire |
| `ninja_tabi` | Plated Steelcaps | layered steel plates and studs over olive cloth |
| `swifty` | Boots of Swiftness | a cream boot, white fur cuff, feathers at the heel |
| `berserkers_greaves` | Berserker's Greaves | a steel greave over a brown boot, crackling yellow lightning |
| `wizards_agony` | Spellslinger's Shoes | a navy boot with gold bands and a silver plate, a spiked orb at the toe, a violet aura |
| `tasted_blood` | Gluttonous Greaves | a bone-white boot with a jagged crest, a crimson streak, a fanged maw at the toe |
| `zonias_hourglass` | Zhonya's Hourglass | a gold frame round an hourglass, glowing amber sand mid-fall |
| `steraks_harness` | Sterak's Gage | a steel gauntlet with lilac sheen, long pale claws, a crimson band at the wrist |

`test_magic_blade` is a `/give`-only test item and keeps the vanilla look.

## How it is wired

- **Swords** use `minecraft:item/handheld`; armour and off-hand items use `item/generated`.
- **Glow.** A sprite's glowing pixels (gems, crystals, glowing channels, auras) are cut out of
  `<id>.png` into `<id>_glow.png`, drawn as layer 1 by NeoForge's `neoforge:item_layers` loader
  with `neoforge_data` full-bright (block and sky light 15, unshaded). So they stay lit in hand
  at night. In the inventory everything is full-bright anyway.
- **Bows** have four textures: idle and `_pulling_0..2`. The body is one pixel map, and the
  string and arrow are drawn per frame in vanilla's layout (arrowhead toward the top left). The
  models keep vanilla's bow transforms and `pull` thresholds.
- **3D in hand:** Veigo's Blade, so far (`HELD_3D` in `generate-combat-art.py`; the Darkin
  Blade and the Black Cleaver are next and use their sprites in hand until then). `<id>.json` is
  a `neoforge:separate_transforms` wrapper: `<id>_3d.json` in hand, the flat `<id>_icon.json`
  sprite in the inventory, on the ground and in item frames. See
  [3D in-hand models](#3d-in-hand-models).
- **Worn armour.** Each armour item has its own `ArmorMaterial` (`CombatContent.armor`): diamond's
  numbers, **copied rather than restated so no stat changes**, plus a layer texture
  `textures/models/armor/<id>_layer_1.png` (helmets, chestplates, boots) or `_layer_2.png`
  (leggings). One material per item, because each is its own League item, not one piece of a
  set. Each texture paints only the boxes its slot renders, so a Jak'Sho helmet and Mercury's
  boots worn together don't fight over the same pixels. That's the only Java this batch touches.

## 3D in-hand models

**Veigo's Blade, 2026-10-07.** Drawn by the combat module's own weapon renderer, the route
Sanchit chose after the first round was rejected (below).

![veigo](../../design/combat-items/3d/veigos_blade_hand.png)

**How it is drawn in game** (`com.ascension.combat.client.weapon`, art's code: models and how
they look and move; item behaviour stays the combat session's):

- `<id>_3d.json` parents `builtin/entity` and carries the hand transforms, so the game hands the
  item to `WeaponRenderer`, a `BlockEntityWithoutLevelRenderer`, through the item's client
  extension (`WeaponClient`; Veigo's is `VeigosBladeClient`, which also carries its swing).
- `WeaponRenderer` draws `assets/<ns>/weapon_models/<item>.json` twice: with its texture
  (`entityCutoutNoCull`, lit like any entity) and with its glow texture through the `eyes`
  render type (additive, full-bright: real glow, which the old JSON route could not do).
- `WeaponModels` loads those files on resource reload, so a resource pack can replace one, and
  `WeaponModel` flattens each part tree to a float array once. Data-driven: a new weapon is a
  generator entry and three files, not a class. No new dependency (this is how L_Ender's
  Cataclysm draws its weapons, studied for its technique only; its code and art are not ours to
  adapt).

**The format** (written by `tools/generate-combat-models.py`, read by `WeaponModel` and by
`tools/render-item-model.py`): parts with a `pivot` and a `rotation` in degrees (Z . Y . X, X
applied first, as vanilla's `ModelPart`; any angle), nesting `children`, and `cubes` with `from`,
`to` and per-face texel rectangles, corners in vanilla's block-face order. Model pixels in the
item's frame, as JSON item models. `WeaponModelTest` pins these conventions, and the preview
renderer was checked against the same cases.

**How the model is made.** Like a Blockbench model: slim boxes in parts turned to any angle, so
crescents are arcs of short bars and the blade tapers to a point in fine steps. Each box face gets
its own rectangle of a 128 x 128 texture at two texels per model pixel, painted from its
material: a palette ramp (from the sprite, so icon and model match), bevel light and shade on
its edges, a little grain, bands for a wrapped grip, and a matching glow texel where it glows.

| Part | Veigo's Blade |
|---|---|
| Blade | long, narrow and straight: y 13.5 to 38 of 38 (about two thirds), 2.4 wide, a raised ridge, tapering to a point; glows faintly, its ridge strongly |
| Small guard | a short bar with horns curving up beside the blade |
| Grip | wrapped, turned 45 degrees about its length so it reads round |
| Great guard | a diamond hub round a glowing jewel, arms out to crescents that open outward |
| Shaft, pommel | a glowing diamond socket; an anchor crescent whose horns point at the blade |

**Hand transforms.** Third person is vanilla's handheld transform turned 45 degrees for a model
drawn upright (an exact fold into the Z rotation), moved so the grip sits in the fist, at 0.75
scale. First person stands it upright at the right of the view, face on, the great guard in
sight at the bottom and the tip near the top, fitted by placing the jewel and the tip on chosen
spots of the screen. `VeigosBladeClient`'s swing runs from there (the strip in the preview).

**The rejected first round** (same day): each weapon was a pixel map extruded one model unit per
pixel. Sanchit: "looks straight from Lego, not at all immersive." Veigo's blade was shorter than
its hilt, the Darkin Blade about 1:3.5 wide, and the Black Cleaver's bit faced sideways to the
swing. Kept in git history only (`c247e8d`); its lessons are in `plans/combat-art-3d.md`.

## Known rough edges

- **Second pass on the weakest four** (2026-10-07): the Rocketbelt is now the belt itself (twin
  rockets, crystal buckle, glowing exhaust) rather than leggings with thrusters; Sterak's Gage is
  a steel-lilac gauntlet with long claws; Mortal Reminder's limbs carry solid saw teeth;
  Spellslinger's Shoes has a gold-trimmed cuff, a red lining, the spiked orb at the toe and an
  aura down its back.
- **Worn textures are simple.** Flat colour, trim bands and the signature detail (Jak'Sho's eyes,
  Guardian Angel's crystal and back wings, Mercury's sapphire, the Gluttonous maw at the toe).
  Armour layers can't glow: the layer renderer has no emissive pass.
- The **orphan** `textures/item/veigos_blade.png` (64 × 64, a red sword, origin not recorded)
  is referenced by nothing. It is left alone.

## Live checklist (ADR-0008)

- [ ] Every item in the creative tab: each reads at a glance, none shows the missing texture.
- [ ] Swords in hand, first and third person, by day and by night (glow layers lit, no
      z-fighting between layers).
- [ ] Each bow: idle, then drawing through all three frames, arrow lined up with the string.
- [ ] Veigo's Blade: the flat icon in the hotbar, dropped and in an item frame; the 3D model
      in hand (not invisible: if it is, the log says why, `bad weapon model`).
- [ ] Its first-person rest pose matches `design/combat-items/3d/veigos_blade_hand.png` (if it
      doesn't, `render-item-model.py` is wrong somewhere: note how, before tuning any numbers).
- [ ] Its swing: wind-up, cut and recovery read well and never swing the blade through the camera.
- [ ] Third person, held by a player and a mob, from the front and behind: the grip in the fist,
      the size right, nothing clipping the body badly. Left hand looks mirrored, not broken.
- [ ] By night: the blade, its ridge, the jewel and the socket glow; the hilt stays dark. By day
      the glow doesn't wash the blade out to white.
- [ ] F3+T (resource reload) keeps the model; with Sodium (in the test stack) it still draws and
      glows.
- [ ] Every armour piece worn, alone and in mixed sets, from the front, back and side.
- [ ] Armour stats unchanged: the character sheet and F3 armour readings match the old
      diamond values.
