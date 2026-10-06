# Combat items — art

**Status:** art, 2026-10-07, first pass. **Not yet seen in a running client** (ADR-0008). The
module builds and the generator reproduces every PNG byte for byte, but nobody has held these
items, drawn the bows or worn the armour in game.

| | |
|---|---|
| Generator | `tools/generate-combat-art.py` (hand-placed pixel maps; rerun it, never edit a PNG) |
| Previews | `design/combat-items/icons.png` (every sprite, plus the bows' draw frames), `worn.png` (each armour piece on a figure) |
| Brief | [`combat-art-reference.md`](combat-art-reference.md): what each League item looks like, and Sanchit's answers |
| Method | [`art-guide.md`](art-guide.md) |

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
- **Veigo's Blade** keeps its 3D model in hand, renamed `veigos_blade_3d.json`
  (`tools/generate-viegos-blade.mjs` now writes that name). `veigos_blade.json` is a
  `neoforge:separate_transforms` wrapper: 3D in hand, the flat `veigos_blade_icon` in the
  inventory, on the ground and in item frames, where the 3D model read as a thin line. The 3D
  redo is the next round.
- **Worn armour.** Each armour item has its own `ArmorMaterial` (`CombatContent.armor`): diamond's
  numbers, **copied rather than restated so no stat changes**, plus a layer texture
  `textures/models/armor/<id>_layer_1.png` (helmets, chestplates, boots) or `_layer_2.png`
  (leggings). One material per item, because each is its own League item, not one piece of a
  set. Each texture paints only the boxes its slot renders, so a Jak'Sho helmet and Mercury's
  boots worn together don't fight over the same pixels. That's the only Java this batch touches.

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
- [ ] Veigo's Blade: the flat icon in the hotbar, dropped and in an item frame; 3D in hand.
- [ ] Every armour piece worn, alone and in mixed sets, from the front, back and side.
- [ ] Armour stats unchanged: the character sheet and F3 armour readings match the old
      diamond values.
