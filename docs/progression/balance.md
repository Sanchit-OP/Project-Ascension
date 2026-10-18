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

The spread is narrower than the first try (Sanchit, 2026-10-19: lessen the split), but still real: fastest to
slowest is 1.7 times (the check allows 1.8). Weapons that carry **utility for a team** are tuned lower: Darius's
Axe (its shred helps everyone's hits) and Mortal Reminder (anti-heal). Wraithblade is brought back up from the
bottom. Heartwood of Pallas was lifted through its ability (1.5% of missing health a stack, up from 1%, and a
higher cap), not its stats.

| Tier | Weapon | Alone (s) | Kit (s) | Why it is where it is |
|---|---|---|---|---|
| Strong | Spear of Shojin | 71 | 45 | True damage on every third hit |
| Strong | Veigo's Blade | 75 | 43 | Bonus from the target's current health |
| Middle | Divine Sunderer | 86 | 57 | Heals you |
| Middle | Darken Blade | 89 | 61 | Shields you |
| Middle | Galeforce | 89 | 63 | Dash and homing shot |
| Middle | Heartwood of Pallas | 93 | 64 | Stacks, then a long-draw execute |
| Middle | Ragefang | 95 | 52 | Builds attack speed as it hits |
| Utility | Mortal Reminder | 99 | 62 | Ignores 60% of Armor; **anti-heal for the team** |
| Utility | Darius's Axe | 110 | 67 | **Shred (up to -60% Armor) for the team** |
| Utility | Wraithblade | 110 | 66 | The blink and 20 Lethality |
| Slow alone | Endless Hunger | 119 | 43 | Omnivamp sustains a long fight; speeds up as your Attack Damage grows |
| *Pair* | Ragefang, one in each hand | 71 | 44 | Both copies count |
| *Pair* | Endless Hunger, one in each hand | 62 | 30 | Both copies count |
| | *netherite sword* | 230 | 109 | |

Cataclysm's weapons, with their real speeds (read from its jar, 2026-10-19), take 160 to 320 seconds alone, so
every legendary is 1.3 to 5 times faster than the best of them.

Each weapon's written time is in `tools/check-balance.py`; the build fails if its data drifts more than 6% from
it (8% for a pair).

### 3a. Dual wield: Ragefang and Endless Hunger

Carry one in each hand and **both count**. The tag `ascension_combat:dual_wield` marks them; for such a weapon
the item data adds, in the off hand, every stat and its damage as extra Attack Damage, under ids of their own so
two copies add up instead of one replacing the other. A swing carries both copies' damage; Endless Hunger's
Omnivamp is 7% a hand, 14% as a pair; its attack-speed bonus follows the Attack Damage, so a pair speeds up.

- **Who swings.** Better Combat swings the hands in turn (both are one-handed weapons in its data). For an
  off-hand swing it swaps the hands, so each swing already uses the weapon in that hand and its ability.
- **Speed is not doubled:** a swing is one weapon's speed. A pair is stronger because each swing hits for both.
- **The cost is two weapons**, which means two builds. Alone each is middling (Ragefang) or slow (Endless Hunger).
- **Assumed, not seen:** Better Combat's own dual-wield damage and speed multipliers (its server config) are
  taken as 1.0. If they are not, a pair's time moves with them.

**What changed to get there**
- **Percent-of-health procs are capped** at once your Attack Damage a go (Veigo, Spear, Divine Sunderer, Darken
  Blade); Galeforce's shot at 3 times and Hunt at 5 times. Before this a boss with five times the health took
  five times the damage, and the weapons that took a share of it never slowed down: Spear took 24 seconds on
  1000 HP and on 5000. Now they scale like the rest.
- **Ragefang was the outlier.** Its on-hit magic and attack-speed ramp did most of its work from a 1-damage
  stub. Now +3% attack speed a hit up to double (it was +5% up to 5 attacks a second), 3 magic damage a hit
  (+0.1 per Attack Damage), and a base speed of 1.4.
- **Endless Hunger** gains 1.2% attack speed per Attack Damage (it was 2%), so the kit stops doubling it.
- **Ragefang is slower alone, stronger as a pair:** its ability does most of its work, so a single copy sits in the middle.

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

## 5. Bosses by progression (Sanchit approved these, 2026-10-19)

Each later boss has more of everything, written per boss. These are the **Normal** numbers; Easy is 15% less
health and 15 points less damage, Hard is 25% more health and 20 points more. Armor and Magic Resist do not
change with difficulty. Attack Damage is a percent on the boss's own, so x1.2 is +20%.

| Boss | HP (Easy / Normal / Hard) | Armor | Magic Resist | Damage (Easy / Normal / Hard) | Kit time to kill, Normal (s) |
|---|---|---|---|---|---|
| The Warden | 1275 / 1500 / 1875 | 80 | 40 | -15% / 0 / +20% | 43 to 80 |
| The Ender Golem | 2125 / 2500 / 3125 | 100 | 50 | +5% / +20% / +40% | 81 to 140 |
| The Ender Guardian | 2720 / 3200 / 4000 | 120 | 60 | +25% / +40% / +60% | 120 to 188 |
| The Netherite Monstrosity | 3570 / 4200 / 5250 | 140 | 80 | +45% / +60% / +80% | 179 to 270 |
| Ignis | 4420 / 5200 / 6500 | 160 | 90 | +65% / +80% / +100% | 255 to 385 |
| The Wither | 5525 / 6500 / 8125 | 180 | 110 | +85% / +100% / +120% | 365 to 553 |

Written into `mob_stats` (Armor, Magic Resist) and `mob_difficulty` (`health_total` and the damage percent, per
difficulty); the four Cataclysm bosses live in `ascension-compat-cataclysm`'s own files. `health_total` sets
the boss's maximum health to that number whatever its own base is. The Wither's damage percent has no effect: it
has no Attack Damage attribute, its skulls do fixed damage. Times use the same weapons and kit as section 3,
with no growth points or runes. A later world's boss goes below the Wither.

## 6. Still open

- Seen in a client and on a dedicated server: difficulty applying on spawn and `/reload`, a legendary's stats
  on its tooltip and the character sheet, and one fight per scaled ability.
- Measure Cataclysm's weapon attack speeds, then bring its weapons to two thirds.
- Write the boss list (section 5) into `mob_stats` and `mob_difficulty` once Sanchit has edited it.
- A rune and growth pass against these numbers: the runes and the 35 growth points add to every total.
