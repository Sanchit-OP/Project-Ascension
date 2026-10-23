# Item audit: every weapon and armour piece, as the game really gives it

> **Measured 2026-10-23 on a dedicated server with the whole pack loaded** (Sanchit: bows and crossbows give nothing, and
> some armour from other mods is worse than the netherite piece it is made from). The numbers are not read from our data
> files: `internal/dev/ItemAudit` asks every registered item for the attribute modifiers the game itself reads
> (`ItemStack.getAttributeModifiers()`), so our data, the vanilla-armour conversions and every other mod's own numbers are
> all in it. **Sections 1 to 4 are the measurement; section 5 is what was changed because of it** (the same day, on Sanchit's word: apply the armour
> and bow numbers, all Cataclysm boss weapons count, none better than a legendary, the abilities magic where they were not, and one ability
> key for each place instead of one for all).

Every item is in [`item-audit.csv`](item-audit.csv), **as it stands after the changes of section 5** (sections 1 to 3 give the numbers from before; the
recipes that turned a stronger piece into a weaker one went from 6 to 0): 151 weapons, bows, tools, armour and off-hand pieces (the 26
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

## 5. What was changed (2026-10-23)

All of it is data except the keys, held by `tools/check-balance.py` (the build runs it) and by `damage-checks` on a dedicated server (405 checks, 0
failures, which now include the lines below).

**Armour.** Every Cataclysm piece that eats a netherite piece is at least that piece in Armor, Magic Resist and Health, Ignitium the best,
Cursium next and the Monstrous Helm a little over netherite. All stay under the 70% ceiling, and the check refuses a piece under netherite:

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

A full set is now Ignitium 84 / 60 / 40 and Cursium 76 / 53 / 42 against netherite's 69 / 49 / 40.

**Bows.** The bow carries +5 Attack Damage (an arrow of 12, 235 s of plain arrows against the standard boss) and the crossbow +4 (an arrow of 10; with
Multishot, three arrows each carrying the Attack Damage, it is about as slow to kill as a legendary bow, not faster). **The Cursed Bow is left as the mod
made it**, because it fires three Phantom Arrows and each is an arrow that carries the shooter's Attack Damage: the +12 first proposed would have
tripled into a bow that out-damaged the best legendary one. At three arrows a draw it already beats a vanilla bow three to one before any bonus (its own
arrow damage is a Cataclysm config number, not ours).

**A bow in hand is swung slowly.** Found while checking that: a bow has no attack speed of its own, so hitting with one used a bare hand's 4 a
second with the bow's whole Attack Damage in the blow. For a legendary bow that is 24.5 four times a second, about 35 a second against the standard
boss, which kills it in 29 s, faster than any legendary. All five bows (the two vanilla ones and the three legendary) now carry `weapon` speed 1.0, so
a blow with a bow is 1 a second. Their tooltips gain a grey "1 Attack Speed" line. The server check swings each one.

**Cataclysm's boss weapons** keep the attack speed the mod gave them and take a damage that sets a plain-hit kill of the standard boss: about 130 s
for a base boss drop and 115 s for a fusion of two bosses' weapons. A netherite sword takes 221, a trident 126, and the slowest legendary 54 *with* its
ability. The abilities of these weapons are Cataclysm's own numbers and are not modelled, so the floor the check holds is more than twice the slowest
legendary: nothing here can pass a legendary.

| Base boss drop (130 s) | Damage x swings a second | Fusion (115 s) | Damage x swings a second |
|---|---|---|---|
| Infernal Forge (was 14) | 22 x 1.0 | Brontes (was 14) | 24.5 x 1.0 |
| Astrape (was 10.5) | 15.5 x 1.4 | Void Forge (was 14) | 24.5 x 1.0 |
| Ceraunus (was 16) | 31 x 0.7 | The Immolator (was 8.5) | 15.5 x 1.6 |
| Soul Render (was 15) | 20.5 x 1.05 | Gauntlet of Bulwark (was 11) | 15.5 x 1.6 |
| The Incinerator (was 14) | 15.5 x 1.4 | Gauntlet of Maelstrom (was 11) | 15.5 x 1.6 |
| The Annihilator (was 8.5) | 13.5 x 1.6 | | |
| Meat Shredder (was 8.5) | 15.5 x 1.4 | | |
| Ancient Spear (was 9.5) | 15.5 x 1.4 | | |
| Tidal Claws (was 8) | 13.5 x 1.6 | | |
| Gauntlet of Guard (was 11) | 13.5 x 1.6 | | |

Left as they were: the crafted Black Steel gear, Khopesh, Athame and the Coral gear, and the Cursed Bow (above).

**Magic abilities.** Most of these weapons' abilities already dealt magic damage (Soul Render's halberds, Astrape's lightning, Ceraunus' anchor, Void Forge's
runes, the Incinerator's and the Immolator's flame strikes). Three dealt a physical hit through the generic projectile type and are now magic by their
entity: Ceraunus' waves, Tidal Claws' tentacle and the Ancient Spear's sandstorm shot (`damage-kinds.md`). The swings themselves stay physical.

**One key for each ability (not one for all).** The "activate armor ability" key used to fire every worn active at once: Gargoyle Stoneplate,
Rocketbelt, Zonia's Hourglass and Galeforce together. There are four keys now, one for each place an item with an active sits, and the packet says which:

| Key (Controls name) | Fires | Default |
|---|---|---|
| Legs Ability | Rocketbelt | Z |
| Chest Ability | Gargoyle Stoneplate | X |
| Off-Hand and Relic Ability | Zonia's Hourglass, in the off hand or the relic slot | mouse button 4 |
| Held Item Ability | Galeforce | mouse button 5 |

The item's tooltip names its key, as the player has it bound ("[Z]", or "(unbound)"). Head and feet have no key until an item for them has an active.
The Cataclysm's own helmet, weapon and boots keys (N, V) are the mod's and unchanged. `AbilitySlot`, `ActivateAbilityPayload` (one byte; the
network version went from 6 to 7), `AbilityActivation`, `CombatKeybinds`. A client audit (`keybind-audit.txt`) showed the four keys registered with the
pack's defaults and no clash; the keys have not been pressed in a client, only on the server.

**Left alone.** Shields until there is a design for them, and the Curios items until someone reads them.
