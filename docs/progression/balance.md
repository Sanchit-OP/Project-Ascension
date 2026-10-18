# Balance: mobs, legendaries and scaling

> **Built 2026-10-19 on branch `combat-balance` (on top of `combat-tooltips`), not merged, not yet seen in a
> client or on a dedicated server.** Sanchit asked for three things: what difficulty does to mobs, power for
> the expensive gear set by what it costs, and scaling for the abilities. Every number here is a placeholder
> he tunes by playing. All of it is data or one coefficient file, so a retune is an edit.

## 1. The three parts

| Part | Where it lives | Checked by |
|---|---|---|
| Mobs by difficulty | `data/ascension_combat/mob_difficulty/{easy,normal,hard}.json`, applied in `MobStats` | A server run (not done yet) |
| Power by cost | `item_stats.json` (the 26 legendaries), the budget in `tools/check-balance.py` | `checkBalance`, every build |
| Ability scaling | `internal/mechanics/AbilityScaling.java` (every coefficient) | Tooltips state them; a client run |

## 2. Mobs by difficulty

The difficulty is the world's own (Easy, Normal, Hard), read from the level, so the setting players already
know does the work. A mob's tier tag (`#ascension_combat:tier/swarm` to `boss`) says how much it moves.
Percent of the mob's own base value, so a Ravager keeps being a Ravager.

| Tier | Easy | Normal | Hard |
|---|---|---|---|
| swarm | health -20% | none | health +20% |
| light | health -20% | none | health +30%, damage +10% |
| medium | health -25%, damage -10% | none | health +40%, damage +15% |
| heavy | health -30%, damage -15% | none | health +50%, damage +20% |
| boss | health -30%, damage -15% | none | health +75%, damage +25% |

- **On top of vanilla.** Vanilla's Hard already raises mob damage; these stack on it. Armor and Magic Resist
  do not change with difficulty: they are the tier's, so a boss's 100 Armor means the same everywhere.
- **Data-driven.** A `types` entry in the same file beats the tier for one mob. Another pack's file with the
  same name stacks, one entry at a time.
- **When it applies.** As a mob enters the world and on `/reload`, as one modifier each, never saved. A mob
  new to the world starts at its scaled health. Changing the difficulty reaches a mob already loaded on the
  next reload or rejoin.
- **Cataclysm's bosses** are in the `boss` tier already, so they scale with no extra line.

## 3. Power by cost

**One score, one budget per slot.** Every stat line is worth points, and a weapon's own damage counts as
damage per second. A legendary must land within 3 points of its slot's budget, so no legendary is stronger
than another of its slot. Abilities are not scored: they are each item's flavour; the budget is what its
stats are worth.

| Stat | Points per unit |
|---|---|
| Health | 1 |
| Armor, Magic Resist | 0.5 |
| Attack Damage | 4 |
| Weapon damage per second (damage x speed) | 3 |
| Attack Speed % | 0.8 |
| Move Speed % | 1.5 |
| Omnivamp % | 6 |
| Lethality | 1.2 |
| Armor or Magic Penetration % | 2 |

| Slot | Budget | Items |
|---|---|---|
| Melee weapon | 120 | Veigo's Blade, Darius's Axe, Spear of Shojin, Divine Sunderer, Darken Blade, Ragefang, Endless Hunger, Wraithblade |
| Bow | 120 | Mortal Reminder, Heartwood of Pallas, Galeforce |
| Helmet | 55 | Protean Mask, Edge of Night |
| Chestplate | 85 | Gargoyle Stoneplate, Guardian's Wing |
| Leggings | 70 | Rocketbelt, Collector's Pants, Unending Despair |
| Boots | 45 | the six boots |
| Off-hand | 55 | Zonia's Hourglass, Sterak's Harness |

- **Every melee weapon is 20 damage per second** before its stats and ability (damage times speed), whatever
  its speed: a slow axe hits hard, a fast blade hits often. Endless Hunger's speed is 4.0 at 5 damage, and
  its ability then adds attack speed from its Attack Damage.
- **Each item leans on its own stats.** Veigo's Blade is Attack Speed and Armor Penetration, Darius's Axe is
  Health and Attack Damage, Wizard's Agony is both penetrations, Tasted Blood is Omnivamp. Equal power,
  different shape.
- **A full set is about 255 points** (helmet, chest, legs, boots, off-hand 55 or so on top), against 99 for
  netherite and 135 for Ignitium. If that is too tanky in play, the slot budgets are the one place to turn it
  down, and the check keeps everything in line.

### Other mods' gear

Their power is what the mod gave them (measured in `item-stats.md` section 8), and the check holds a ceiling:
**no Cataclysm item may reach 65% of its slot's budget**, so every legendary stays well clear. Today:

| Band | Share of its slot | Examples |
|---|---|---|
| Boss gear, epic | 45% to 62% | Ignitium and Cursium sets, Ceraunus (60%), Soul Render (56%), the Forges, Gauntlet of Guard |
| Mid gear, rare | 28% to 38% | Astrape, Ancient Spear, Monstrous Helm |
| Vanilla | 32% to 40% | Netherite chestplate 40%, netherite sword 32% |

A legendary is therefore at least 1.6 times the best boss item of its slot, and 2.5 to 3 times netherite.
Cataclysm items do not need extras to fit the bands. If one is ever added above the ceiling, the check names it.

## 4. Scaling the abilities

An ability's base number stays in its effects class. Each gains an extra from one stat, read as a total
through `StatReader`, never from an attribute directly. Every coefficient is in `AbilityScaling`.

| Ability | Base | Gains |
|---|---|---|
| Veigo's Blade, Wither | 8% of current health | +0.2% per Attack Damage |
| Darken Blade, burst | 5% of max health, 6 shield | +0.1% and +0.5 shield per Attack Damage |
| Divine Sunderer | 4% physical + 4% magic of max health | +0.1% each per Attack Damage |
| Spear of Shojin | 5% of max health, true | +0.1% per Attack Damage |
| Ragefang, Rage | 5 magic per hit | +0.2 per Attack Damage |
| Heartwood of Pallas | 3 magic per hit | +0.15 per Attack Damage |
| Galeforce | 20% of missing health | +0.5% per Attack Damage |
| Rocketbelt | 6 burst damage | +0.5 per Attack Damage |
| Unending Despair | 12 magic per pulse | +0.1 per point of Health |
| Gargoyle Stoneplate | 4 per enemy, cap 40 | +0.05 per enemy and +0.4 to the cap, per Armor |

Not scaled: Darius's Axe (its shred is a percent and its Armor Penetration is already in its stats),
Mortal Reminder, Wraithblade, Edge of Night, the two stasis items and Collector's Pants (utility, not
damage), Sterak's Harness and Endless Hunger (they already convert a stat), the Protean Mask (it is
stacks). Attack Damage is the stat to build for: with growth and a legendary it reaches 20 to 30, so a
scaled proc grows by half again or doubles.

Tooltips state each base number and what it scales with.

## 5. Still open

- Seen in a client and on a dedicated server: difficulty applying on spawn and `/reload`, a legendary's stats
  on its tooltip and the character sheet, and one fight per scaled ability.
- Whether a full legendary set is too strong (section 3), and whether Hard's +75% boss health is enough.
- A rune and growth pass against these numbers: the runes and the 35 growth points add to every total.
- The Cataclysm gear's own Armor and Magic Resist scale off vanilla's armour rates; if Sanchit wants them
  higher, that is a line each in `ascension-compat-cataclysm`.
