# Item audit: every weapon and armour piece, as the game really gives it

> **Measured 2026-10-23 on a dedicated server with the whole pack loaded** (Sanchit: bows and crossbows give nothing, and
> some armour from other mods is worse than the netherite piece it is made from). The numbers are not read from our data
> files: `internal/dev/ItemAudit` asks every registered item for the attribute modifiers the game itself reads
> (`ItemStack.getAttributeModifiers()`), so our data, the vanilla-armour conversions and every other mod's own numbers are
> all in it. **Nothing here is changed yet.** Section 5 is the proposal, for Sanchit's nod.

Every item is in [`item-audit.csv`](item-audit.csv): 151 weapons, bows, tools, armour and off-hand pieces (the 26
legendaries and the vanilla, Cataclysm, Create, Enderman Overhaul and Aeronautics ones), with the stats a player ends up with, the
vanilla armour behind them, and a flag where something looks wrong. To measure again:

```bash
touch run/server/item-audit.txt && ./gradlew runDevServer
```

It writes `item-audit.csv`, `item-audit-recipes.csv` (every recipe that makes an item out of a stronger one) and
`item-audit-others.csv` (every other mod item, by name) into `run/server/` and stops the server. It measures; it does not pass or fail.

## 1. Armour made out of netherite that is worse than netherite

Three of Cataclysm's sets are not crafted: they are **smithing upgrades that use up a netherite piece**
(`cataclysm:smithing/*`, base `minecraft:netherite_*`). The balance pass of 2026-10-19 cut Cataclysm's armour to two thirds
and held it under 70% of the *legendary* budget, but netherite already sits at 50% to 65% of that budget, so the cut put
most of the boss sets **under the piece they eat**. Armor / Magic Resist / Health, against the same slot in netherite:

| Piece | Now | Netherite | Points now | Points netherite |
|---|---|---|---|---|
| Monstrous Helm | 7 / 5 / 5 | 10 / 7 / 10 | 11 | 18.5 |
| Ignitium helmet | 14 / 10 / 8 | 10 / 7 / 10 | 20 | 18.5 |
| Ignitium chestplate (and the elytra one) | 25 / 18 / 10 | 28 / 20 / 10 | 31.5 | 34 |
| Ignitium leggings | 21 / 15 / 8 | 21 / 15 / 10 | 26 | 28 |
| Ignitium boots | 14 / 10 / 7 | 10 / 7 / 10 | 19 | 18.5 |
| Cursium helmet | 11 / 8 / 11 | 10 / 7 / 10 | 20.5 | 18.5 |
| Cursium chestplate | 23 / 17 / 11 | 28 / 20 / 10 | 31 | 34 |
| Cursium leggings | 19 / 13 / 10 | 21 / 15 / 10 | 26 | 28 |
| Cursium boots | 11 / 8 / 7 | 10 / 7 / 10 | 16.5 | 18.5 |

(Points: Health 1, Armor 0.5, Magic Resist 0.5, `tools/check-balance.py`.) Six of the nine recipes make a piece with fewer points than the netherite they
use (both chestplates and leggings, the Monstrous Helm, the Cursium boots), and **no piece except the Cursium helmet is better in every
stat.** A full set: Ignitium 74 / 53 / 33, Cursium 64 / 46 / 39, netherite 69 / 49 / 40. `item-stats.md` section 8 promised that
"the boss armour stays the better set" (111 / 79 / 40 before the cut); after it, Cursium is the worse set outright and Ignitium
is 7 Health short.

Two sets are not made from netherite and sit where they should: **Bone Reptile** (helmet 14 / 10 / 4, a little over diamond; chestplate 25 / 18 / 4,
between iron and diamond), and **Bloom Stone Pauldrons** (21 / 15 / 3), iron plus a little, with Nature Heal.

## 2. Bows and crossbows give nothing

An arrow carries the shooter's whole Attack Damage on top of its own 6 (`PhysicalDamage.applyArrowAttackDamage`), and the only
things that give Attack Damage are a weapon's data and its enchantments. These have no data and no modifier, so holding one adds
nothing to the shot (an arrow is 6 + a bare hand's 1):

| Item | Attack Damage added | Plain hits to the standard boss (150 Armor), seconds |
|---|---|---|
| Bow, Crossbow, Cataclysm's Cursed Bow | 0 | 404 |
| Mortal Reminder, Heartwood of Pallas, Galeforce | +23.5, +21.5, +22 | 93 to 99 |

A netherite sword is 221 and the cheapest legendary bow 99: the vanilla bow is four times slower than the cheapest legendary
one and **slower than an iron sword (295)**. The Create potato cannon and Cataclysm's Laser Gatling fire their own projectiles, not
arrows, so shooter Attack Damage would not reach them either way; they are left out.

## 3. Cataclysm's weapons are about a netherite sword

None of Cataclysm's weapons has data of ours (the speeds were unmeasured; now they are, below). Plain-hit seconds to the standard
boss, with no abilities used (the legendaries reach 36 to 55 with theirs):

| Seconds | Weapon (damage x swings a second) |
|---|---|
| 144 | The Incinerator (14 x 1.4) |
| 161 | the three gauntlets (11 x 1.6) |
| 180 | Soul Render (15 x 1.05) |
| 192 | Astrape (10.5 x 1.4) |
| 202 | Brontes, Void Forge, Infernal Forge (14 x 1.0, tools) |
| 208 | The Annihilator, The Immolator (8.5 x 1.6) |
| 213 to 253 | Ancient Spear, Tidal Claws, Meat Shredder, Ceraunus (16 x 0.7) |
| **221** | **netherite sword (8 x 1.6)** |
| 126 | vanilla trident (15 x 1.5, rebalanced 2026-10-22) |

A boss drop lands between a trident (126) and a netherite sword you can craft (221): Tidal Claws is a netherite sword, the Annihilator and the Immolator are within
a dozen seconds of one, and **Ceraunus (253) is no faster than a diamond sword**. The two-thirds cut was never applied to weapons, so this is how the mod made them.

## 4. Small things

- **`ascension_combat:test_magic_blade`** does 1 physical damage by itself (it deals a flat 10 magic damage through its effect). It is a debug item,
  left out of every creative tab and given by `/give` only; nothing to fix.
- **Shields** (vanilla, Black Steel Targe, Azure Sea Shield, Enderman Overhaul's corrupted shield) give 0 Armor, 0 Magic Resist and 0
  Health in the off hand, where Zonia's Hourglass gives 27 / 27 / 10. Blocking works as in vanilla; whether a shield should add stats is a design question.
- **Create's cardboard sword** does 1 damage; its cardboard armour is leather-weight. A joke tier; nothing to fix.
- **Create's diving gear and backtanks** still show in the audit with stats; no recipe makes them and the recipe viewers hide them (`create-checks.txt`).
- **Elytra** gives no Armor, as vanilla; Ignitium's elytra chestplate does.
- **Hoods of Enderman Overhaul** (badlands, savanna, snowy) are chest items worth a chainmail chestplate. Nothing to fix.
- **Not covered:** Curios items (Cataclysm's belts, rings, amulets, Ankh), Icarus wings and the paragliders. They are worn in Curios slots or used,
  not read through item attribute modifiers, so the audit cannot see their numbers.

## 5. Proposal (not applied)

**Armour.** Every Cataclysm piece that eats a netherite piece is at least that piece in Armor, Magic Resist and Health, with
Ignitium the best, Cursium next and the Monstrous Helm a little over netherite. All stay under the 70% ceiling:

| Piece | Armor / Magic Resist / Health | Share of the slot budget |
|---|---|---|
| Ignitium helmet | 14 / 10 / 10 | 59% |
| Ignitium chestplate and elytra chestplate | 33 / 24 / 10 | 68% |
| Ignitium leggings | 25 / 18 / 10 | 67% |
| Ignitium boots | 12 / 8 / 10 | 67% |
| Cursium helmet | 12 / 8 / 11 | 57% |
| Cursium chestplate | 30 / 21 / 11 | 64% |
| Cursium leggings | 23 / 16 / 10 | 63% |
| Cursium boots | 11 / 8 / 10 | 65% |
| Monstrous Helm | 12 / 8 / 10 | 54% |

A full set becomes Ignitium 84 / 60 / 40 and Cursium 76 / 53 / 42 against netherite's 69 / 49 / 40. `tools/check-balance.py` gains a
floor so this cannot drift again: no armour that is smithed from netherite may be under that netherite piece in any of the three
stats.

**Bows.** An Attack Damage line, so the arrow is the size of the sword of the same standing: Bow +5 (an arrow of 12, 235 s, like a netherite sword), Crossbow +7
(slower to load, 202 s), Cursed Bow +12 (arrow 19, 149 s, with the boss weapons of section 3). The check's bow rule (damage and Move Speed only) already allows it.

**Cataclysm's boss weapons.** Lift the boss and structure drops to the band of 100 to 150 seconds plain (about 1.7 times their damage, speed
unchanged), still well under the legendaries once their abilities count. Not the crafted Black Steel gear, Khopesh, Athame or Coral gear. This one needs a
list from Sanchit of which weapons count, since each has an ability nobody has fought with yet.

**Left alone.** Shields until there is a design for them, and the Curios items until someone reads them.
