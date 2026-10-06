# Combat items — art reference (League of Legends)

**What this is:** research for giving `ascension-combat`'s items real art. Gathered 2026-10-06
from the League of Legends Wiki. This is the brief; the art made from it (first pass,
2026-10-07) is described in [`combat-art.md`](combat-art.md).

**Sanchit's direction (2026-10-06):**

- These items are ported from League of Legends, and **they should look like their League
  counterparts.** Original "inspired" designs are not what he wants. A first round of
  original-styled samples was rejected for exactly that reason.
- **Veigo's Blade (Blade of the Ruined King) must be redone.** The current model "is not
  looking great" (see its section below).

**How that squares with the art rules** ([`art-guide.md`](art-guide.md), Sanchit's
original-art-only call):

- **Names and identities.** ADR-0012 already accepts the IP risk of adapting League names and
  identities closely. Making the items *look* like League's is part of that accepted risk.
- **Drawing.** We draw our own pixel art that resembles each item: same silhouette, palette and
  signature details. We never trace, sample, scale down or paste Riot's pixels; copying the icon
  files themselves would add an art-copyright exposure the ADR never accepted.
- **Reference images stay local.** Riot's images live only in `design/reference/league/`, which
  is git-ignored. They are never committed and never shipped.

## Where the references are

**Local screenshots** in `design/reference/league/` (open them with the Read tool):

| File | Shows |
|---|---|
| `01-all-item-icons.jpg` | all 24 item icons in one grid, labelled with *our* item names |
| `02-icons-weapons-1.jpg` | Blade of the Ruined King, Bloodthirster, Black Cleaver, Endless Hunger, Guinsoo's Rageblade, Spear of Shojin |
| `03-icons-weapons-2-helmets.jpg` | Youmuu's Ghostblade, Divine Sunderer, Mortal Reminder, Galeforce, Jak'Sho, Edge of Night |
| `04-icons-chest-legs-boots.jpg` | Gargoyle Stoneplate, Guardian Angel, Hextech Rocketbelt, The Collector, Unending Despair, Mercury's Treads |
| `05-icons-boots-offhand.jpg` | Plated Steelcaps, Boots of Swiftness, Berserker's Greaves, Sorcerer's Shoes, Zhonya's Hourglass, Sterak's Gage |
| `06-viego-sword-full.jpg` | **Viego's sword, whole, from his model render**, plus his "Blade of the Ruined King" ability icon (the glow) |
| `07-champion-renders-aatrox-darius-varus.jpg` | full-body renders: Aatrox, Darius (axe), Varus (bow) |
| `08-aatrox-darkin-blade-icons.jpg` | Aatrox's ability icons: the Darkin Blade close up |
| `09-darius-axe-and-varus-icons.jpg` | Darius's axe close up (Decimate, Apprehend); Varus's arrows |
| `10-varus-bow-closeup.jpg` | Varus's bow, cropped from his render |
| `11-viego-render-and-splash.jpg` | Viego's render and splash, side by side |
| `12-ninja-tabi-old-vs-steelcaps.jpg` | the old Ninja Tabi icon next to today's Plated Steelcaps |
| `13-ours-current-veigos-blade.png` | **our current Veigo's Blade in first person**, the thing to replace |
| `14-cleaver-steelcaps-tier3-boots.jpg` | Black Cleaver, Plated Steelcaps, Armored Advance, Sorcerer's Shoes, **Spellslinger's Shoes**, **Gluttonous Greaves**, Immortal Path |
| `15-spellslingers-and-gluttonous-closeup.jpg` | Spellslinger's Shoes and Gluttonous Greaves, large |

The second icon in `02` is **Bloodthirster, not Darken Blade**. It was a wrong first guess.
Darken Blade's reference is Aatrox's sword, in `07`/`08`.

**Online,** to look again or go deeper (open them in the built-in browser):

| What | URL pattern |
|---|---|
| item page | `https://wiki.leagueoflegends.com/en-us/<Item_Name>` |
| item icon, 64 × 64 | `https://wiki.leagueoflegends.com/en-us/images/<Item_Name>_item.png`, e.g. `Zhonya's_Hourglass_item.png` |
| champion render (transparent, whole body) | `.../en-us/images/<Champion>_Render.png` |
| champion splash | `.../en-us/images/<Champion>_OriginalCentered.jpg` |
| champion ability icons | `.../en-us/images/<Champion>_<Ability_Name>.png`, e.g. `Aatrox_The_Darkin_Blade.png`, `Viego_Blade_of_the_Ruined_King.png` |

How the screenshots were taken: the browser pane's zoom doesn't work, so a scratch page was built
in the wiki's origin with the icons as `<img>` tags at 250 px (`image-rendering: pixelated`), then
screenshotted. Browser screenshots also land automatically under the session's `tool-results`
folder.

## The catalogue: what each one looks like

Our id → League source → Minecraft form. "Look" is what a sprite must keep; colours are
approximate, read off the icons.

### Melee weapons (all `SwordItem` today)

| Our item | League source | Look |
|---|---|---|
| `veigos_blade` | Blade of the Ruined King, Viego's sword | See its own section below. Spectral jade-mint, ornate crescent guards. |
| `darken_blade` | Aatrox's sword, the Darkin Blade | A massive, broad greatsword, almost cleaver-wide. Dark gunmetal body (≈ #2e2e3a → #55576a), **jagged barbs** along both edges, a **glowing crimson channel** down the blade's length (≈ #c0203a, hot core #ff5060), angular dark hilt. Shown in fire. |
| `darius_axe` | **Black Cleaver** (Sanchit, 2026-10-07) | The Black Cleaver icon: a huge near-black double-bit axe, the big blade on one side, spiked edges, cracked plates, a polished pale lower edge, a brown haft. (Not chosen: Darius's own axe, two crescent blades, pale steel edges, Noxian red.) |
| `endless_hunger` | Endless Hunger | Two black crescent sickle blades with gold trim, curling into an S, crimson-pink energy. As one held weapon: a long curved black blade, gold edge, crimson glow. |
| `ragefang` | Guinsoo's Rageblade | A wavy, flame-shaped blade: orange-gold with yellow flame tongues along the edge, red-orange core, dark brown and gold hilt. |
| `spear_of_shojin` | Spear of Shojin | A big curved **lime-jade blade** like a dragon's claw (≈ #7ee03a, shade #2a7a20, highlight #d8ffa0) on an ornate bronze-gold spiked collar, with a teal wind trail. The icon shows only the head; as an item it is that head on a long haft. |
| `wraithblade` | Youmuu's Ghostblade | A straight sword. Grey-white steel blade; ornate **gold-bronze crossguard with a glowing magenta-violet gem** at its centre; dark purple wrapped grip; gold pommel. Pink-violet ghost petals. |
| `divine_sunderer` | Divine Sunderer (removed from League) | Golden and ornate: a big **gold half-moon head with emerald-green inlaid curves**, dark navy shaft. Reads as a golden axe or war-hammer. |

### Ranged weapons (all `BowItem`)

| Our item | League source | Look |
|---|---|---|
| `mortal_reminder` | Mortal Reminder | A crossbow-like bow: grey steel limbs with **sawtooth spikes**, gold bands, a purple-grey frame, a steel bolt with a broad head pointing up-right. Olive-green backdrop. |
| `heartwood_of_pallas` | Varus's bow | Organic Darkin bow: a **glowing violet core** at the grip, two long horn-like limbs plus hooked claw protrusions, dark purple-black chitin (≈ #1c1024 → #4a2a6a) with violet sheen (#8a50e0), **magenta glowing spots** (#e050b0), lilac core glow (#d0b0ff). Crimson-magenta arrows. |
| `galeforce` | Galeforce (removed) | A sleek **silver-white bow**, angular blade-like limbs swept like wings, cyan wind streaks. Dark blue backdrop. |

### Helmets, chestplates, leggings, boots (all `ArmorItem`, vanilla diamond material)

| Our item | League source | Look |
|---|---|---|
| `protean_mask` | Jak'Sho, The Protean | A dark void-purple chitin mask, spiky, with **glowing magenta-red cracks and eyes**. |
| `edge_of_night` | Edge of Night | A dark indigo-purple **hooded veil**, a pale skeletal hand or blade below. As a helmet: the hood. |
| `gargoyle_stoneplate` | Gargoyle Stoneplate | A pale stone-grey **cuirass** with broad shoulders, **golden-orange crack streaks** across the chest. |
| `guardian_wing` | Guardian Angel | A **cyan crystal in a gold frame** with **white feathered wings** either side, a dark blade below. |
| `rocketbelt` | Hextech Rocketbelt | A brass-bronze belt with **two rocket thruster cones** and a blue hextech crystal at the centre, blue-white exhaust jets. |
| `collectors_pants` | The Collector | League's icon is a **gun** (a dark steel blunderbuss muzzle with gold filigree). For leggings: dark steel with gold filigree. |
| `unending_despair` | Unending Despair | Dark green-black **spiky carapace** around a **bright cyan glowing core**, magenta-purple backdrop. |
| `mercury_treads` | Mercury's Treads | A brown leather boot, steel toe cap and sole, a **gold-rimmed blue sapphire medallion** on the side. |
| `ninja_tabi` | **Plated Steelcaps** (Sanchit, 2026-10-07) | A boot of silver steel plates and spiky studs over olive cloth, toe to the left. (Not chosen: the old green split-toe tabi.) |
| `swifty` | Boots of Swiftness | A white-cream boot with a **fluffy white fur cuff** and white feathers at the side. |
| `berserkers_greaves` | Berserker's Greaves | A steel armoured greave over a brown leather foot, **yellow lightning** crackling round it. |
| `wizards_agony` | **Spellslinger's Shoes**, Sorcerer's Shoes' tier 3 (Sanchit, 2026-10-07) | A dark navy-steel boot with gold trim and silver plates, a **spiked black orb** at the toe, a magenta-violet aura. |
| `tasted_blood` | **Gluttonous Greaves** (Sanchit, 2026-10-07; added to League in 2026, Aatrox's quote) | A **bone-white boot shaped like a maw**: jagged spikes up the shaft, a crimson streak, **fanged teeth at the toe** round a dark red mouth. |

### Off-hand

| Our item | League source | Look |
|---|---|---|
| `zonias_hourglass` | Zhonya's Hourglass | An angular **ornate gold frame** around an hourglass, **amber sand** in the top bulb running down, pale blue glass. |
| `steraks_harness` | Sterak's Gage | A huge **armoured gauntlet**, a clawed fist of dark crimson-purple metal plates with spiked knuckles. |

`test_magic_blade` is a test item and needs no art.

## Veigo's Blade (Blade of the Ruined King): the redo

**The reference** (`06`, `11`): Viego's sword, read from the model render left to right:

1. An **open crescent pommel**, anchor-like, with hooked ends.
2. A long two-handed grip with a **diamond socket**.
3. A large secondary guard: **two crescent arms**, one curving up and forward, one down, each
   ending in a hook.
4. A short ricasso and a small guard at the base of the blade.
5. A **long, narrow, straight blade**.

The whole sword is **spectral jade-mint** (≈ #5fe0b0, shaded #2a9d7a, highlights near-white
#c8ffee), luminous rather than metallic. The ability icon shows it **glowing cyan-mint against
darkness**. The Blade of the Ruined King *item* icon instead shows the hilt in dark gunmetal,
wreathed in bright cyan Black Mist with a small cyan gem: that is the "possessed" reading.

**Ours** (`13`; files `design/weapons/viegos-blade/`; runtime model
`modules/combat/.../models/item/veigos_blade.json`, 373 cuboids on a 64 × 64 atlas `veigos_blade_3d.png`):

- **The structure is right.** Its README lists exactly the pieces above.
- **The look is wrong.** In first person it is a thin, dark, desaturated slate-teal sword with
  no glow. The guards read small; in the hotbar it is a thin diagonal line.
- **Not checked:** whether the proportions are also off in third person.

Directions for the next session to weigh with Sanchit:

- **Palette:** bright jade-mint plus an **emissive layer** (NeoForge `neoforge_data` full-bright
  faces work in item models with elements too; see `art-guide.md`, "Glow").
- **Weight:** chunkier, bolder crescent guards so the silhouette reads at a glance.
- **Hotbar:** a dedicated inventory sprite (a 2D GUI model) separate from the in-hand 3D model,
  so it doesn't read as a line.
- **Source of truth:** keep the Blockbench project (`tools/generate-viegos-blade.mjs` makes
  the starting geometry), or move to a generator in the style of the art guide.

## Turning a League icon into a Minecraft item

- League icons are **64 × 64 painted close-ups with a backdrop**. Minecraft items are **16 × 16
  transparent sprites of the whole object**. Keep the silhouette, the signature palette and one
  or two signature details (a gem, a glow, filigree). Drop the backdrop, motion blur and
  painterly gradients.
- Orientation, following vanilla: weapons diagonal, grip bottom-left to tip top-right; boots in
  side view; helmets and chestplates from the front.
- **Bows need four textures:** idle plus three pulling stages, wired through model overrides
  (`mortal_reminder.json` already has the predicate structure, currently pointing at vanilla).
  Swords use parent `minecraft:item/handheld`; armour uses `minecraft:item/generated`.
- **Glow suits many of these** (Viego's mist, Youmuu's gem, Guardian Angel's crystal, Varus's
  core, Galeforce's wind) via an emissive model layer.
- **Worn armour** has its own look per item since 2026-10-07: one armour material per item,
  diamond's stats, its own layer texture (`combat-art.md`).

## Sanchit's answers (2026-10-07)

1. **Darius's Axe:** the Black Cleaver.
2. **Ninja Tabi:** today's Plated Steelcaps.
3. **Wizard's Agony** is Spellslinger's Shoes (Sorcerer's Shoes' tier 3); **Tasted Blood** is
   Gluttonous Greaves.
4. **2D or 3D:** 16 × 16 sprites first; 3D in-hand models later.
5. **Worn armour:** changes too.
6. **Who draws:** this session. Models, animation and item art are made outside the combat
   session and merged back.
