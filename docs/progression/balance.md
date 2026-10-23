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

> **Boss health above 1,024 needed a fix (2026-10-19, ADR-0027).** Vanilla clamps the Max Health attribute at 1,024,
> so until the ceiling was raised every boss total in this document came out at exactly 1,024. A second fault was found
> with it: a boss's total was worked out from its base health alone, so Cataclysm's Ender Golem and Ender Guardian
> (each carrying a +150% `animation_health` modifier) came out 225 and 499.5 too high. Both are fixed and seen on a
> dedicated server.
>
> **Every boss total was cut by 35% (Sanchit, 2026-10-19: "a little too op with the hp, not the armor").** Armor and
> Magic Resist are untouched. The totals below are the cut ones; the first draft was Warden 1,500, Ender Golem 2,500,
> Ender Guardian 3,200, Netherite Monstrosity 4,200, Ignis 5,200 and Wither 6,500 on Normal. Easy and Hard kept their
> ratios (85% and 125% of Normal).

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
slowest is 2.1 times (the check allows 2.2; it was 1.7 and 1.8 until Ragefang's speed limit was lifted, section 3a). Weapons that carry **utility for a team** are tuned lower: Darius's
Axe (its shred helps everyone's hits) and Mortal Reminder (anti-heal). Wraithblade is brought back up from the
bottom. Heartwood of Pallas was lifted through its ability (1.5% of missing health a stack, up from 1%, and a
no cap), not its stats.

> **Raised again on 2026-10-22 (Sanchit: "currently the player is weak against the bosses, we need them all some buffs").** The times in this
> table are the new ones; section 3b says what changed and why. The table of 2026-10-19 had them at 52 to 110 s alone.

| Tier | Weapon | Alone (s) | Kit (s) | Why it is where it is |
|---|---|---|---|---|
| Strong | Spear of Shojin | 46 | 36 | True damage on every third hit |
| Strong | Veigo's Blade | 48 | 35 | Bonus from the target's current health |
| Middle | Divine Sunderer | 54 | 40 | Heals you |
| Middle | Darken Blade | 55 | 43 | Shields you |
| Middle | Galeforce | 55 | 40 | Dash and homing shot |
| Middle | Heartwood of Pallas | 53 | 46 | Stacks, then a long-draw execute |
| Strong | Ragefang | 36 | 29 | Builds attack speed as it hits, up to 7.5 a second (the ramp takes about 40 s of unbroken fighting) |
| Utility | Mortal Reminder | 44 | 34 | Ignores 60% of Armor; **anti-heal for the team** |
| Utility | Darius's Axe | 47 | 37 | **Shred (up to -60% Armor) for the team** |
| Utility | Wraithblade | 42 | 32 | The blink and 40 Lethality |
| Middle | Endless Hunger | 51 | 35 | Omnivamp sustains a long fight; hits hard and slow (a base 2 a second), faster as your Attack Damage grows, up to 3 a second |
| *Pair* | Ragefang, one in each hand | 28 | 23 | Both copies count |
| *Pair* | Endless Hunger, one in each hand | 24 | 18 | Both copies count |
| | *netherite sword* | 230 | 109 | |
| | *mace (vanilla's 6 at 0.6 was 787 s alone, 319 kit)* | 295 | 167 | Rebalanced 2026-10-22 (Sanchit: basic and trash): **12 at 0.8**. First set to 26 at 0.9 and cut: the mace is meant to win by **jumping and smashing** (a fall adds flat damage, 4 a block for the first three, 2 for the next five, and Density adds more), not by plain swings, so this figure is its flat swing only and says little about its worth |
| | *trident (vanilla's 9 at 1.1 was 286 s alone, 143 kit)* | 126 | 78 | Rebalanced 2026-10-22: **15 at 1.5** (Sanchit: a rare item, let it be a little good); throwing, Riptide, Loyalty and Channeling untouched. A thrown trident also carries the thrower's Attack Damage, as arrows do |

**What it does against a late boss.** On the Wither (4225 HP after the 2026-10-19 cut, 180 Armor, 110 Magic
Resist), alone, kit in brackets, after the 2026-10-22 raise (what it was before, alone, after the semicolon): Ragefang 100 s (75; was 179),
Spear of Shojin 105 (90; was 179), Veigo's Blade 111 (84; was 171), Divine Sunderer 116 (96; was 181), Galeforce 144 (112; was 240),
Darken Blade 174 (140; was 278), Heartwood 175 (143; was 255), Mortal Reminder 194 (153; was 441), Darius's Axe 209 (161; was 497),
Wraithblade 219 (166; was 572), Endless Hunger 270 (182; was 494).
The Spear's true damage was the worst runaway (88 s) and was cut to 0.5% on 2026-10-19; it is now 0.8%, which keeps it with Veigo and
Sunderer. What is left is a split between weapons whose damage is a percent of the boss's health, which
grow stronger as bosses grow, and weapons that deal flat damage (Darius, Ragefang, Wraithblade, Mortal
Reminder, Endless Hunger), which take about twice as long (three times before the raise). Against the Warden the split is mild. Boss Armor and
Magic Resist do not close it, because they cut flat and percent damage alike.

Cataclysm's weapons, with their real speeds (read from its jar, 2026-10-19), take 160 to 320 seconds alone, so
every legendary is 1.3 to 5 times faster than the best of them at the standard boss.

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
- **No caps (Sanchit, 2026-10-19).** A percent-of-health proc, Galeforce's missing-health shot and Hunt are all
  uncapped: a bigger boss takes more from them, and the later bosses carry more Armor and Magic Resist to answer
  it. To keep them from doing the whole job at the standard boss, the base percents are lower than the originals:
  Veigo 3% of current health (was 8%), Spear 0.5% true (was 5%; it had to fall furthest, because true damage ignores Armor), Divine Sunderer 2% and 2% (was 4% and 4%), Darken
  Blade 2.5% (was 5%), each +0.05% per Attack Damage (was +0.1% to +0.2%).
- **Ragefang was the outlier.** Its on-hit magic and attack-speed ramp did most of its work from a 1-damage
  stub. The first balance pass cut the ramp to +3% attack speed a hit up to **2.5** attacks a second (it was +5% up
  to 5), 3 magic damage a hit (+0.1 per Attack Damage), and a base speed of 1.4.
- **Ragefang retuned, 2026-10-19 (Sanchit: "hard nerfed with the attack speed cap").** The limit is now **7.5 attacks a
  second** (the first pass had cut it to 2.5; the original code's was 5) and its hit is back at 5.3. Sanchit said not to
  hold the kill time: it kills the standard boss in **52 s** alone (was 95), 42 as a pair (was 68), the fastest legendary.
  The ramp is still +3% a hit, so reaching 7.5 takes about 145 hits, 40 seconds of fighting without a pause of more than
  a second; a break of a second starts the stacks fading, so in a real fight it spends much of its time below the limit.
  The one number to turn is `ATTACK_SPEED_SHADOW_LIMIT` in `RagefangEffects` (and `RAGE_LIMIT` in
  `tools/time-to-kill.py`, kept equal).
- **Endless Hunger** gained 1.2% attack speed per Attack Damage (it was 2%), so the kit stops doubling it.
- **Endless Hunger retuned, 2026-10-19 (Sanchit: "too much attack speed", then "flat out reduce the attack speed but
  increase the AD", kill time not held).** It swung at a base 4 a second, 4.3 to 4.7 with a kit and 8.8 at 100 Attack
  Damage. Now a base of **2.0**, **+0.6%** per Attack Damage (2.2 a second at the kit's 14, 2.5 at 40) and a **ceiling of
  3** (reached at about 170 Attack Damage), and its hit is **14.0** (was 5.7), the figure of Wraithblade and Darius's Axe:
  fewer, harder hits. It kills the standard boss in 95 s alone (was 119), 54 with the kit (was 43). Numbers:
  `ATTACK_SPEED_PERCENT_PER_AD_POINT` and `ATTACK_SPEED_LIMIT` in `EndlessHungerEffects`, `ENDLESS_AS_PER_AD` and
  `ENDLESS_LIMIT` in the simulator.
- **The spread limit went from 1.8 to 2.2** (slowest alone / fastest alone is now 2.13: Wraithblade and Darius's Axe at
  110 s against Ragefang at 52), on Sanchit's word that these two need not hold their old kill times.
- **Better Combat** has its own ceiling of one attack every 2 ticks (10 a second, `attack_interval_cap` in its server config) and lets fast
  attacks through the hit cooldown (`allow_fast_attacks`); both are as the pack ships them and neither is the limit here.
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

Cataclysm's armour is under 70% of its slot's legendary budget (its own `item_stats.json`, each entry with a `cataclysm` mod check; the
check holds the ceiling). **Revised 2026-10-23 (the item audit, `item-audit.md`):** Ignitium, Cursium and the Monstrous Helm are
smithing upgrades that eat a netherite piece, and the two-thirds cut of 2026-10-19 had put most of them under it. Each is now at least
the netherite piece in Armor, Magic Resist and Health (the check enforces it): a full Ignitium set is 84 / 60 / 40, Cursium 76 / 53 / 42
and netherite 69 / 49 / 40.

**Its boss weapons were raised the same day**, now that their attack speeds are measured and kept as the mod gave them. A base boss
drop takes about 130 seconds of plain hits to kill the standard boss (Infernal Forge, Astrape, Ceraunus, Soul Render, the Incinerator, the
Annihilator, the Meat Shredder, the Ancient Spear, Tidal Claws, the Gauntlet of Guard), a fusion of two bosses' weapons about 115 (Brontes, Void
Forge, the Immolator, the Gauntlets of Bulwark and Maelstrom). A netherite sword takes 221 and a trident 126; the slowest legendary
kills in 54 seconds with its ability, and the floor is more than twice that, because these weapons' abilities are Cataclysm's own numbers and
are not modelled. The Cursed Bow is left alone: it fires three arrows and each carries the shooter's Attack Damage, so a bonus would be
tripled. Crafted gear (Black Steel, Khopesh, Athame, Coral) is untouched. `check-balance.py` holds all of it.

**The vanilla bow and crossbow** carry Attack Damage (+5 and +4, arrows of 12 and 10 plus the draw), and every bow, the legendary three
included, is swung at 1 a second when hit with: a bow in hand used to swing at a bare hand's 4 a second with its whole Attack Damage in the
blow, which for a legendary bow was a way round every sword.

### 3b. The weapons raised (2026-10-22)

Sanchit: the player is far too weak against the bosses; buff every legendary weapon, base stats and scaling both, and tread carefully
with the two that are dual-wielded. Two levers moved: every base damage in `item_stats.json` (a bow's Attack Damage), and every scaling
coefficient in `AbilityScaling` and its effects class. Scaling went up by about half; each base damage was then solved with
`tools/time-to-kill.py` so the weapon lands on its written time (`check-balance.py` holds it, and now holds the Wither too).

| Weapon | Damage (speed) before | after | Ability, before | after |
|---|---|---|---|---|
| Veigo's Blade | 10.1 (1.6) | **16.5** (1.6) | 3% of current health, +0.05% per AD | **4%, +0.075%** |
| Darius's Axe | 14.2 (1.0) | **34** (1.0) | shred 5% a stack, 12 stacks | unchanged |
| Spear of Shojin | 21.7 (1.4) | **31** (1.4) | true 0.5% of max health, +0.0125% | **0.8%, +0.02%** |
| Divine Sunderer | 16 (1.2) | **25.5** (1.2) | 2% + 2% of max health, +0.05% each | **2.5% + 2.5%, +0.07%** |
| Darken Blade | 18.1 (1.4) | **29** (1.4) | 2.5% of max health, +0.05%; shield 6, +0.5 | **3.5%, +0.07%; shield 10, +0.8** |
| Ragefang | 5.3 (1.4) | **12** (1.4) | 3 magic, +0.1 per AD | **4.5, +0.15** (ramp unchanged: +3% a hit, 7.5 a second) |
| Endless Hunger | 14 (2.0) | **24** (2.0) | +0.6% speed per AD, up to 3; Omnivamp 7% | unchanged |
| Wraithblade | 14.1 (1.6) | **32** (1.6) | 20 Lethality; blink | **40 Lethality**; blink unchanged |
| Mortal Reminder | +6.5 Attack Damage | **+23.5** | ignores 60% of Armor | unchanged |
| Heartwood of Pallas | +11.4 Attack Damage | **+21.5** | 3 magic, +0.15; Hunt 1.5% of missing health a stack | **4.5, +0.22; Hunt 2%** |
| Galeforce | +9.3 Attack Damage | **+22** | 20% of missing health, +0.5% | **25%, +0.7%** |

**Result** (the 1000 HP, 150 Armor standard boss, then the Wither; alone, then in the kit): the weapons now take **36 to 56 s alone**
(was 52 to 110) and **29 to 46 s in the kit** (was 37 to 67); on the Wither **100 to 270 s alone** (was 171 to 572) and **75 to 182 in
the kit** (was 108 to 345). Fastest to slowest at the standard boss is 1.55 times (the check allows 1.7, it was 2.2); at the Wither it
is 2.7 (the new limit is 2.8).

**Why the flat weapons went up furthest.** A weapon that deals a share of the boss's health grows with the boss; a flat weapon does
not, and loses twice over on a late boss: it has about 4 times the health to chew through and the Wither lets 29% of a physical hit
through (the Warden 57%). Darius's Axe went from 14 to 34 damage (2.4 times), Wraithblade 2.3 times, Mortal Reminder 3.6 times, against
1.4 to 1.6 times for the share-of-health weapons. Wraithblade's Lethality was doubled, but it is a weak lever: against 180 Armor each
point of Lethality is worth little under the 0.5^(Armor/100) curve (20 more Lethality saved it about 13% on the Wither).

**The dual-wield pairs (Ragefang and Endless Hunger) were checked on their own.** A pair adds the second copy's damage (2 x damage - 1),
Endless Hunger's Omnivamp (14%) and its Attack-Damage-driven swing speed (2.56 a second for a pair), so a base damage rise hits a pair
twice: once as extra damage, once as extra speed. A pair of Endless Hunger is 2.1 to 2.2 times as fast as one copy, however it is
tuned. Endless Hunger is therefore 24, not the 25.5 its single-weapon time alone would give, and its pair kills the standard boss in 24 s
(Ragefang's pair 28 s), at the Wither 125 s (Ragefang 71 s). Not touched, on purpose: Ragefang's ramp (+3% a hit, 7.5 a second),
Endless Hunger's speed per Attack Damage (0.6%, ceiling 3), both Omnivamp numbers, Darius's shred, the blink, the cooldowns. The server
check (`damage-checks.txt`, 280 checks, 0 failures) proves both pairs on the stand-in player: Attack Damage 23 for two Ragefangs and 47
for two Endless Hungers, 14% Omnivamp, a swing speed of 2.56, and a real swing of a Ragefang pair that carries both copies' damage and the
on-hit magic.

**Not fixed, said plainly.** Endless Hunger on its own still takes 2.7 times as long as the fastest weapon on the Wither (a pair is
fine), and every flat weapon trails the share-of-health ones there. Closing it means giving the flat weapons a share-of-health part,
which is a new mechanic and was not asked for; the cheaper help is already on the list: the runes and the growth points add flat Attack
Damage, which helps a flat weapon most. **Divine Sunderer's heal grew with its percent:** it heals its whole bonus, so on the Wither one
use is about 270 health (it was about 175), every 5 seconds. **The mace (12 at 0.8) and the trident (15 at 1.5)**, set earlier on
2026-10-22 against the old legendary times, now sit well behind the legendaries (295 s and 126 s at the standard boss alone); the mace's
real attack is its smash, and the trident was meant to be only a little good, so they stand as Sanchit set them.

## 4. Scaling the abilities

An ability's base number stays in its effects class. Each gains an extra from one stat, read as a total
through `StatReader`, never from an attribute directly. Every coefficient is in `AbilityScaling`.

| Ability | Base | Gains |
|---|---|---|
| Veigo's Blade, Wither | 4% of current health | +0.075% per Attack Damage |
| Darken Blade, burst | 3.5% of max health, 10 shield | +0.07% and +0.8 shield per Attack Damage |
| Divine Sunderer | 2.5% physical + 2.5% magic of max health | +0.07% each per Attack Damage |
| Spear of Shojin | 0.8% of max health, true | +0.02% per Attack Damage |
| Ragefang, Rage | 4.5 magic per hit | +0.15 per Attack Damage |
| Heartwood of Pallas | 4.5 magic per hit (Hunt: 2% of missing health a stack) | +0.22 per Attack Damage |
| Galeforce | 25% of missing health | +0.7% per Attack Damage |
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

Each later boss has more of everything, written per boss. HP is the first draft cut by 35% (2026-10-19). These are the **Normal** numbers; Easy is 15% less
health and 15 points less damage, Hard is 25% more health and 20 points more. Armor and Magic Resist do not
change with difficulty. Attack Damage is a percent on the boss's own, so x1.2 is +20%.

| Boss | HP (Easy / Normal / Hard) | Armor | Magic Resist | Damage (Easy / Normal / Hard) | Kit time to kill, Normal (s) |
|---|---|---|---|---|---|
| The Warden | 829 / 975 / 1219 | 80 | 40 | -15% / 0 / +20% | 19 to 30 |
| The Ender Golem | 1381 / 1625 / 2031 | 100 | 50 | +5% / +20% / +40% | 32 to 52 |
| The Ender Guardian | 1768 / 2080 / 2600 | 120 | 60 | +25% / +40% / +60% | 38 to 68 |
| The Netherite Monstrosity | 2321 / 2730 / 3413 | 140 | 80 | +45% / +60% / +80% | 48 to 93 |
| Ignis | 2873 / 3380 / 4225 | 160 | 90 | +65% / +80% / +100% | 58 to 127 |
| The Wither | 3591 / 4225 / 5281 | 180 | 110 | +85% / +100% / +120% | 75 to 182 |
| The Ender Dragon (5a) | 3868 / 4550 / 5688 | 190 | 90 | none (no attribute) | 78 to 211, up to x3 on her body |

Written into `mob_stats` (Armor, Magic Resist) and `mob_difficulty` (`health_total` and the damage percent, per
difficulty); the four Cataclysm bosses live in `ascension-compat-cataclysm`'s own files. `health_total` sets
the boss's maximum health to that number whatever its own base is. The Wither's damage percent has no effect: it
has no Attack Damage attribute, its skulls do fixed damage. Times use the same weapons and kit as section 3,
with no growth points or runes. A later world's boss goes below the Wither.

### 5a. The Ender Dragon (applied 2026-10-20, Sanchit approved the proposal)

Read from the 1.21.1 source (`EnderDragon`), so the numbers are not guesses:

- **Part damage.** Only the head takes a hit in full. Any other part (body, wings, tail) takes `damage / 4 + min(damage, 1)`,
  so a hit on the body is worth about a quarter. The cut is applied to the raw hit before Armor, and `max health` is the
  Dragon's one pool, so a fight that lands mostly on the body takes up to about three times the table's kill time.
- **Her attacks are fixed.** Body and wing contact 5, head and neck 10, from `mobAttack`, with no Attack Damage attribute:
  like the Wither, a damage percent has **no effect**. Her breath (a lingering cloud) is already magic; the contact hits
  are physical. Crystal beams and the perch are the only things the health number touches.
- **Crystal healing is 1 health every 10 ticks (2 a second) from the nearest crystal.** Trivial against 4,000+ health, so
  the crystals matter only for ending the fight, not for undoing damage; the vanilla 10-health crystal hit on her head
  is likewise nothing (about 0.2%). Nothing to change.
- **The perch.** She sits when `sittingDamageReceived` passes 25% of max health, so her perches stretch with her health.

| | Easy / Normal / Hard | Armor | Magic Resist | Damage percent |
|---|---|---|---|---|
| The Ender Dragon | 3,868 / 4,550 / 5,688 | 190 | 90 | none (no attribute) |

Why: just above the Wither (4,225), as the ladder asks; Armor one step above it (180 to 190); Magic Resist lower than the
Wither's (110) because the Dragon is a physical fight and the head is the weak point, so a magic-heavy kit is not
punished twice. `tools/time-to-kill.py 4550 190 90`, no growth or runes, head hits, kit, after the 2026-10-22 weapon raise: Ragefang pair 62 s, Veigo 93 s,
Darken Blade 139 s, Heartwood 143 s, Endless Hunger pair 111 s, Wraithblade 192 s (the Wither's 59, 84, 140, 143, 96, 166; before the raise Wraithblade was 398 s here).
Add up to a factor of three if most hits land on her body.

Open: whether the Dragon's kill should drop a heart (decided: no, for now).

## 6. Still open

- Seen in a client and on a dedicated server: difficulty applying on spawn and `/reload`, a legendary's stats
  on its tooltip and the character sheet, and one fight per scaled ability.
- Measure Cataclysm's weapon attack speeds, then bring its weapons to two thirds.
- Write the boss list (section 5) into `mob_stats` and `mob_difficulty` once Sanchit has edited it.
- A rune and growth pass against these numbers: the runes and the 35 growth points add to every total.
- After the 2026-10-22 raise: flat weapons still trail on the Wither (section 3b), Divine Sunderer's heal is large on a late boss, and none of it
  has been fought in a client yet. The runes and the growth points are next, and they add Attack Damage on top of these times.
