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

## 3. Power by cost (redone 2026-10-19: two thirds of the first pass)

Sanchit: the first pass gave too much; the game should not feel like an overpowered simulator. Defensive
stats and the Cataclysm armour are now two thirds of what they were, and weapons are tuned by time to kill.

**Weapons are balanced by time to kill, not by points.** A weapon's worth is mostly its ability, which no
stat sum can say. `tools/time-to-kill.py` simulates every weapon against a standard boss (1000 HP, 150
Armor, 90 Magic Resist) with its abilities used correctly, reading the numbers from the game's own data.

- **Alone** is the weapon with nothing else worn: what it is worth by itself.
- **Kit** is the weapon worn with a full set of legendary armour, whose pieces together add about +8 Attack
  Damage and +8 Lethality. It is what a player late in the game sees.

| Weapon | Alone (s) | Kit (s) | Why it is where it is |
|---|---|---|---|
| Ragefang | 85 | 50 | Builds attack speed as it hits |
| Spear of Shojin | 85 | 51 | True damage on every third hit |
| Veigo's Blade | 85 | 47 | Bonus from the target's current health |
| Mortal Reminder | 85 | 55 | Ignores 60% of Armor; wounds cut healing |
| Divine Sunderer | 89 | 60 | Heals you |
| Darken Blade | 95 | 64 | Shields you |
| Heartwood of Pallas | 94 | 68 | Stacks, then a long-draw execute |
| Galeforce | 94 | 64 | Dash and homing shot |
| Darius's Axe | 105 | 66 | **Slower on purpose:** its shred (up to -60% Armor) makes everyone else's hits land |
| Endless Hunger | 110 | 46 | **Slower on purpose:** Omnivamp sustains a long fight; it speeds up as your Attack Damage grows |
| Wraithblade | 115 | 69 | **Slower on purpose:** the blink and 20 Lethality |
| *netherite sword* | 230 | 109 | |
| *a 14-damage boss weapon* | 131 | 79 | |

The fastest and slowest are 1.36 times apart alone (the check allows 1.5), so no one weapon is the only
choice, and every legendary is 1.2 to 2.7 times faster than a boss weapon. Each weapon's written time is in
`tools/check-balance.py`; the build fails if its data drifts more than 6% from it.

**What changed to get there**
- **Percent-of-health procs are capped** at once your Attack Damage a go (Veigo, Spear, Divine Sunderer, Darken
  Blade); Galeforce's shot at 3 times and Hunt at 5 times. Before this a boss with five times the health took
  five times the damage, and the weapons that took a share of it never slowed down: Spear took 24 seconds on
  1000 HP and on 5000. Now they scale like the rest.
- **Ragefang was the outlier.** Its on-hit magic and attack-speed ramp did most of its work from a 1-damage
  stub. Now +3% attack speed a hit up to double (it was +5% up to 5 attacks a second), 3 magic damage a hit
  (+0.1 per Attack Damage), and a base speed of 1.4.
- **Endless Hunger** gains 1.2% attack speed per Attack Damage (it was 2%), so the kit stops doubling it.

**Armour, boots and off-hand are balanced by points**, one budget per slot: helmet 37, chestplate 57,
leggings 47, boots 30, off-hand 37 (each two thirds of the first pass). A piece lands within 3 points of it.

| Stat | Points per unit |
|---|---|
| Health | 1 |
| Armor, Magic Resist | 0.5 |
| Attack Damage | 4 |
| Attack Speed % | 0.8 |
| Move Speed % | 1.5 |
| Lethality | 1.2 |

Omnivamp and Penetration are not scored: they are set by hand. Ninja Tabi and Mercury Treads sit at 24 on
purpose (Sanchit: too strong); Wizard's Agony and Tasted Blood are exactly their identity stat.

**Rules (Sanchit, 2026-10-19), enforced by the check:**
- A melee weapon's damage and speed are **one figure each**, in `weapon`; no separate +Attack Damage or
  +Attack Speed line.
- **Bows carry damage only** (and Galeforce a little Move Speed): no Health, no Attack Speed.
- **Armor and Magic Penetration only on Wizard's Agony** (12% each).
- **Omnivamp on five items only:** Veigo's Blade 4%, Endless Hunger 7%, Spear of Shojin 3%, Divine Sunderer 3%,
  Tasted Blood 8%.

### Other mods' gear

Cataclysm's armour is two thirds of what the mod gave it, with boss sets lifted in Health to sit near 55% of
their slots (its own `item_stats.json`, each entry with a `cataclysm` mod check; the check holds a ceiling of
70%). **Its weapons are not touched yet:** a weapon's damage can only be replaced together with its attack
speed, and Cataclysm's speeds have never been measured. Measuring them is the next step; then the boss
weapons go to the same two thirds.

## 4. Scaling the abilities

An ability's base number stays in its effects class. Each gains an extra from one stat, read as a total
through `StatReader`, never from an attribute directly. Every coefficient is in `AbilityScaling`.

| Ability | Base | Gains |
|---|---|---|
| Veigo's Blade, Wither | 8% of current health | +0.2% per Attack Damage, never more than your Attack Damage |
| Darken Blade, burst | 5% of max health, 6 shield | +0.1% and +0.5 shield per Attack Damage |
| Divine Sunderer | 4% physical + 4% magic of max health | +0.1% each per Attack Damage |
| Spear of Shojin | 5% of max health, true | +0.1% per Attack Damage |
| Ragefang, Rage | 3 magic per hit | +0.1 per Attack Damage |
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

## 5. Bosses by progression (a draft, for Sanchit to edit)

Bosses should grow as the game goes on: each later boss has more of everything, and the numbers are written
per boss, not per tier. These are proposals. Heavy and boss tiers share nothing below; each row is one
boss's own entry. Attack Damage is a multiple of what the boss does now.

| Boss | HP | Armor | Magic Resist | Damage | Kit time to kill (fastest to slowest weapon, s) | Alone |
|---|---|---|---|---|---|---|
| The Warden | 1500 | 80 | 40 | x1.0 | 43 to 80 | 77 to 128 |
| The Ender Golem | 2500 | 100 | 50 | x1.2 | 81 to 140 | 147 to 221 |
| The Ender Guardian | 3200 | 120 | 60 | x1.4 | 120 to 188 | 214 to 299 |
| The Netherite Monstrosity | 4200 | 140 | 80 | x1.6 | 179 to 270 | 320 to 451 |
| Ignis | 5200 | 160 | 90 | x1.8 | 255 to 385 | 438 to 642 |
| The Wither | 6500 | 180 | 110 | x2.0 | 365 to 553 | 564 and up |

The order is the order of the growth milestones. HP is the total, so it will be written as a flat addition
once each boss's base health is read from a running game. The times use the same weapons and kit as section 3,
with no growth points or runes, which only shorten them; a well-built player will beat these. A later world's
boss goes below the Wither in this table.

## 6. Still open

- Seen in a client and on a dedicated server: difficulty applying on spawn and `/reload`, a legendary's stats
  on its tooltip and the character sheet, and one fight per scaled ability.
- Measure Cataclysm's weapon attack speeds, then bring its weapons to two thirds.
- Write the boss list (section 5) into `mob_stats` and `mob_difficulty` once Sanchit has edited it.
- A rune and growth pass against these numbers: the runes and the 35 growth points add to every total.
