# Player power: where a player's stats come from

> **Draft for discussion, 2026-10-15.** Sanchit's answer to decision 1, the same day: the work goes
> to three new sessions, using the prompts in `plans/session-prompts/`. Stats for every item, runes
> and growth. Build paths come after. The rest of this page is their starting point, not a
> decision.
>
> Sanchit paused the combat items' build paths (`resource-economy.md` §5) to settle what sits under
> them first: how a player gains HP and the other stats during play, and a rune system worth using.
> This page only proposes, and changes no combat code.

## 1. What exists today

The stats (`combat-api.md` §3): HP, Attack Damage, Attack Speed, Move Speed, Armor, Magic Resist,
Omnivamp, Lethality, and Armor and Magic Penetration.

| Source | What it gives a player today |
|---|---|
| **The player's base** | Vanilla's: 20 HP, 1 Attack Damage, vanilla attack and move speed. 0 in every combat stat. **Nothing ever raises it.** |
| **Vanilla armour** | **No protection against physical damage.** Combat replaces vanilla's armour maths with its own Armor stat (`PhysicalDamage`), and vanilla armour does not feed that stat. Leather to netherite all mitigate the same: nothing. Protection enchantments still work, because they apply separately. |
| **The 26 combat items** | Their effects, plus placeholder vanilla diamond stats. Only Ninja Tabi (15 Armor), Mercury Treads (15 Magic Resist), Gargoyle Stoneplate (2 and 2) and the Protean Mask (up to 8 and 8) give the combat defence stats. **None gives HP.** None can be obtained yet. |
| **Runes** | One path of four is built: Vanguard, with one keystone and four minors. Reaver, Aetherwright and Bulwark are empty. |
| **Mobs** | Armor and Magic Resist by tier (`MobStats`): bosses 100 and 40, heavy mobs 50 and 20. |

**How the rune page works now:**
- An unbound keybind opens a screen of plain vanilla buttons, with rune names only.
- Pick a primary path, which grants its one keystone, plus up to two of its minors. Then a
  secondary path's minors, up to two.
- Every choice is free from the first minute, and nothing is ever unlocked.

## 2. What is broken

1. **A player never grows.** HP is 20 for the whole game. Every other stat comes only from 26
   items that cannot be obtained yet. Nothing marks that you have beaten the Warden or reached the
   Nether.
2. **Armour does not protect.**
   - The defence curve halves damage at 100 Armor, and a boss has 100.
   - The best a player can reach today is about 25 Armor, from three of the 26 combat pieces.
   - A netherite set counts for nothing.
   - Every planet is balanced against players who have, in effect, no armour at all.
3. **The runes are not a system yet.**
   - Three paths are empty.
   - The screen gives no descriptions and no icons.
   - Nothing about runes connects to progress.
4. **Build paths would be balanced against nothing.** An item's stats only mean something next to
   what a player has without it.

## 3. A proposal: four layers, each with one job

| Layer | Its job | How it grows |
|---|---|---|
| **Growth** | Permanent base stats: the player getting stronger over the game | Milestones: bosses beaten, gates lit |
| **Armour** | Survival on each planet, gear you can make early | Vanilla's tiers, leather to netherite, now counting again |
| **Items** | Builds: what kind of fighter you are | The 26 League items, through build paths |
| **Runes** | Choices that shape a build, not raw power | Unlocked as you grow; free to change out of combat |

### 3.1 Growth: milestones, not grinding

**Proposed:**
- **The first time a player beats each boss, their base stats rise for good.**
- **Each gate they light raises them too.** It is the mark growing on the hand, which the lore
  already gives every lit gate (`the-mark.md`).

The rewards are data, one file per milestone, so a new planet's boss adds growth without code.

| Milestone | Example reward (placeholder) |
|---|---|
| The Warden (Earth) | +4 HP, +5 Armor |
| The Ender Golem, the Ender Guardian (Moon) | +4 HP each; +5 Armor and +5 Magic Resist |
| The Netherite Monstrosity, Ignis (the Nether) | +4 HP each; +5 Attack Damage |
| The Wither | +4 HP, +5 Magic Resist |
| Each gate lit | +2 HP |

**Why milestones:**
- **They can't be farmed.** A mob-XP level would be maxed in an afternoon at a mob grinder.
- **They make bosses and exploration pay.** That is the vision's "bosses unlock capability" and
  "exploration must return value".
- **They fit ADR-0018.** No world is required, but every world you conquer makes you stronger,
  in whatever order you go.

**Turned down:**
- **Vanilla XP levels.** You spend them on enchanting and anvils, so stats would drop every time
  you enchant.
- **Kill-XP combat levels.** Farmable, as above.

**Could sit alongside later:** rare stat shards in ruins, a small permanent bonus each, like
Terraria's life crystals, as a reward for exploring.

**Co-op:** the vision says progress is shared by the team. Two ways:
- **Every player who fought the boss gets its milestone.** That needs a rule for what counts as
  fighting it.
- **The whole team gets it.** That also closes `todo.md`'s late-joiner gap: a new teammate starts
  at the team's growth.

### 3.2 Armour counts again

**Proposed:** vanilla armour gives the combat Armor stat again, at a fixed rate kept in data.
- **Example:** each vanilla armour point becomes 5 Armor, so a full netherite set (20 points) is
  100 Armor, half of physical damage.
- **Toughness:** could become Magic Resist, or nothing.

Leather, iron, diamond and netherite are then the armour tiers again, and Earth's mining matters.
The combat items sit above netherite rather than being the only armour that works.

**The other way:** leave vanilla armour cosmetic, and make all defence come from growth and combat
items. That is cleaner for League-likeness, but a player has no armour at all until their first
combat item.

### 3.3 Items, then build paths

The build-path contract (`resource-economy.md` §5) resumes once growth and armour are settled.
Its stat budgets get set against them. For example:
- A legendary should be worth about what two bosses' growth is.
- No item should give more Armor than a full netherite set.

### 3.4 Runes: a real system

**Content: all four paths built.** For each path, three keystones to choose from (one, as now),
and two rows of three minors (one from each row).
- **Vanguard:** sustained damage. Built.
- **Reaver:** burst, execute, ambush.
- **Aetherwright:** amplifies the magic procs already on items.
- **Bulwark:** HP, defences, protecting the team. Its keystone ties into oxygen, as already decided
  (`combat-api.md` §7).

**Stat shards, as League has them.** Three small rows on the rune page:
- offence: Attack Damage or Attack Speed;
- flex: Attack Damage, Armor or Magic Resist;
- defence: HP, Armor or Magic Resist.

They give the rune page a little raw power, sized against growth.

**Unlocked as you grow.** For example:
1. One path and its minors from the start.
2. The keystone after the first boss.
3. The secondary path after the second.
4. The stat shards after the first lit gate.

So runes join the same progress as growth.

**Changing runes:** free out of combat, so players can try builds.
- `CombatStateRegistry` already knows when a player is in combat.
- The other way is a block to change them at, like an altar or the Archive's plinth. That makes
  changing a choice you travel for.

**A screen worth using:**
- An icon for each rune, drawn in League likeness by the art track.
- A tooltip saying what each rune does, with its numbers.
- The path's identity on show.
- A keybind bound by default, so players find it.
- The stylized mockup the current screen's javadoc mentions is its target.

### 3.5 The HP display

With growth and items, HP can pass 40, and vanilla's rows of hearts get hard to read. A health bar
with a number, like the oxygen HUD, may be needed. Decide once real numbers exist.

## 4. Order of work

1. **Sanchit's decisions** (§5).
2. **Armour counts again** (§3.2). It is the smallest change, and every planet's balance needs it.
3. **Growth** (§3.1): the milestone data, per-player or team storage, and the character sheet
   showing where each stat came from.
4. **Runes** (§3.4): the unlock rules first, then the three missing paths, then the stat shards,
   then the screen and its art.
5. **The build-path contract** (`resource-economy.md` §5), with stat budgets set against all of
   the above.
6. **A refactor pass** (ADR-0008) after every third increment, as always.

**Who builds it** is the first decision: the combat module has been the parallel session's, which
last committed on 2026-10-08.

## 5. Decisions for Sanchit

1. **Who builds the combat side:** this session, taking the combat module over, or the combat
   session, from this page?
2. **Growth:** milestones from bosses and lit gates (recommended)? Or another source of permanent
   stats?
3. **Co-op growth:** each player who fought, or the whole team (recommended, since it fixes the
   late-joiner gap)?
4. **Vanilla armour:** count it again at a fixed rate (recommended), or leave it cosmetic?
   *Answered 2026-10-15: it counts again. Each armour point gives 3.5 Armor and 2.5 Magic Resist,
   each toughness point 2.5 Health, and netherite +10 Health a piece (`item-stats.md` §5).*
5. **Runes:**
   - Three keystones per path, with two rows of three minors? Or keep one keystone?
   - Stat shards?
   - Unlocked by growth?
   - Changed freely out of combat, or at a block?
6. **The HP display:** keep vanilla hearts, or a bar with a number once HP grows?
