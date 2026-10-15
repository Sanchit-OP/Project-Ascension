# Player growth: research

> **Research, 2026-10-15.** The groundwork for `growth.md`, which is the design. Sanchit asked for
> "a lot of different ways to get stats", stats that items will scale off, and "maybe a skill tree
> or something so players go in different directions". This page looks at how other games and
> mods let players grow, what makes growth feel earned, and what fits Ascension. It ends with
> what we *can* add and what we *should* add (§5).

## 1. The question

Today a player never grows (`player-power.md` §2): 20 HP all game, and every other stat comes from
items. Growth has to fix that without breaking three things already decided:

- **ADR-0018:** no world is required. Growth must come from any world, in any order.
- **The vision:** co-op first, team progress shared, bosses unlock capability, exploration must
  return value.
- **ADR-0007:** nothing per tick, no leaks, data over code.

## 2. How other games do it

### League of Legends: levels inside one match

- **How you grow:** experience from minions and kills takes a champion from level 1 to 18 inside
  one match. Each level raises base stats (HP, Attack Damage, Armor...) by fixed amounts, and gives
  one point for an ability.
- **What works:** levels are earned by playing the objective, and the cap of 18 ends the climb.
- **What matters for us:** League splits stats into **base** (the champion plus its levels) and
  **bonus** (items and runes). Item ratios name which one they scale off: "60% *bonus* AD", or
  Sterak's Gage, which gives AD equal to a share of *base* AD. So whether our growth counts as base
  or bonus decides which items it feeds (`growth.md` §8).
- **What went wrong once:** until 2017, League had permanent rune and mastery pages bought with
  account currency. Riot made them free because power that carried between matches felt unfair.
  Permanent growth is fine in Ascension because there is no opponent to be unfair to, but the
  lesson stands: growth everyone gets anyway is a floor, not a choice.

### Dark Souls and Elden Ring: levels, and finite finds

- **How you grow:** souls (runes) from kills buy levels. Each level puts one point into one stat,
  and each level costs more than the last. Past a soft cap a stat gives less per point.
- **Finds:** Estus shards, Golden Seeds and Sacred Tears are placed in the world, a fixed number of
  each. Finding one makes your healing better, for good.
- **Bosses:** Elden Ring's Great Runes come from its demigod bosses: beat the boss, gain a power.
- **What works:** farming is allowed but limits itself, because each level costs more. Players who
  farm are choosing an easier game. The finds turn exploration into power you can feel.
- **What does not carry over to Minecraft:** a Create or vanilla mob farm kills while the player
  is away. A kill-funded level would be farmed by a machine, not chosen by a player.
- **Elden Ring's expansion** added Scadutree Fragments: finite finds that raise a separate power
  level which only works inside the expansion's own land. A world-scoped growth, worth knowing
  about (§5).

### Path of Exile: the big passive tree

- **How you grow:** about 1,300 nodes. Points come mostly from levels, and about two dozen from
  story quests. A further handful, the strongest, come from the Labyrinth, a set-piece trial.
- **Node kinds:** small nodes (+10% of something), notables (a bigger, named bonus), and keystones
  (a rule change, such as "you have no life, only energy shield").
- **Respec:** a few free refund points from quests, then a currency (Orbs of Regret).
- **What works:** two players with the same items can play completely differently. The strongest
  points come from set pieces, not grinding.
- **What goes wrong:** new players are lost without a planner, and most small nodes are too small
  to feel. The notables carry the excitement.

### Diablo: paragon

- **Diablo 3:** after the level cap, endless paragon levels, each a small stat point. It became a
  treadmill: whoever played longest was strongest, with no end.
- **Diablo 4:** paragon boards with sockets for glyphs and big legendary nodes. Much more choice,
  but heavy to read.
- **Lesson:** growth with no cap turns into a grind. A cap with a choice inside it is better.

### Terraria: life crystals, and world flags

- **How you grow:** a Life Crystal adds 20 HP, up to 400. Later, Life Fruit adds 5, up to 500. Since
  1.4.4, a handful of one-time consumables (Vital Crystal, Aegis Fruit, Arcane Crystal...) each add
  a small permanent bonus once.
- **What works:** each find is a visible step (one more heart on screen), and the cap means
  exploring fully is rewarded but can't be overdone.
- **Co-op:** boss defeats are **world flags**: once the team beats a boss, the world moves on for
  everyone, including people who join later. Each player who took part gets their own treasure bag.
  Character growth (crystals) stays with each character.

### Zelda and Hollow Knight: hearts from bosses, pieces from exploring

- **Zelda:** every dungeon boss drops a heart container. Pieces of heart, four to a container, are
  hidden around the world.
- **Hollow Knight:** mask shards, vessel fragments, charm notches and pale ore, all placed by hand,
  many behind a challenge.
- **What works:** the most readable growth rule in games: *beat a boss, get a heart*. Pieces let
  small finds feel like progress without each one being big.

### Co-op games: Valheim and Monster Hunter

- **Valheim:** each boss's trophy, hung at the starting altar, unlocks its power for every player,
  who each choose to take it. Team progress, personal choice.
- **Monster Hunter:** everyone in the hunt gets the full reward, even if they joined partway. Nobody
  is punished for being the healer or arriving late.

### Minecraft mods

| Mod | How players grow | What we learn |
|---|---|---|
| **Pufferfish's Skills** | Skill trees defined in datapacks. Points come from per-category experience: kills for combat, blocks for mining. Many community trees for 1.21.1 NeoForge. | Data-driven trees work well in Minecraft and modpack makers love them. The weak part is the point source: experience for doing things is farmable, so trees end up full of tiny +1% nodes. |
| **Project MMO** | A level per activity (mining, combat, agility...), raised by doing it. Levels can lock items. | Grinding by repetition, which macros and machines do better than people. Locking items behind levels goes against ADR-0018. |
| **Passive Skill Tree** (NeoForge 1.21.1 port of Daripher's mod) | A Path of Exile-style tree, points from vanilla XP levels. | XP is also what you enchant with, so stats and enchanting fight over one currency. That is why `player-power.md` turned vanilla XP down. |
| **Vault Hunters** | Vault levels from vault runs buy skill points for abilities and talents. | Works because the whole pack is built around the vault loop. A grind loop by design, not by accident. |
| **Scaling Health** | Heart crystals, from ore and mob drops, raise max HP up to a configured cap. | Terraria's life crystals in Minecraft. The ore is endless, so only the cap stops it. |
| **Vanilla advancements** | None. They give a toast and sometimes XP. | Minecraft already has a finite, per-player, saved, synced, data-driven list of milestones, including "found this structure". It just pays nothing. |

## 3. What makes growth feel earned, not grindy

1. **It comes from doing something, once.** A boss, a gate, a place found. Not hours spent.
2. **It is finite, and you can see what is left.** Zelda's heart pieces, Hollow Knight's map, a
   checklist. Seeing the next goal is half the pull.
3. **Each step is big enough to notice.** One more heart on screen beats +0.5% of anything. Few
   big steps feel better than many tiny ones.
4. **It has a cap.** An end to the climb means a player who explores fully is rewarded, and one who
   grinds is not ahead of them.
5. **Choice matters but is not a trap.** Path of Exile lets you refund; Souls lets you respec a few
   times. Free unlimited respec makes the choice meaningless; no respec punishes experiments.
6. **Co-op shares the floor, not the choices.** Terraria's world flags and Valheim's boss powers
   move the whole team on. Each player still chooses their own direction.
7. **The best rewards come from the hardest set pieces.** Path of Exile's Labyrinth, Elden Ring's
   Great Runes, Zelda's heart containers.

**Minecraft's own traps:**
- **Machines farm anything repeatable.** Mob grinders and Create turn any per-kill or per-block
  reward into an idle reward.
- **Vanilla XP is spent** on enchanting and anvils, so it can't also be a stat.
- **Worlds are endless.** Any reward found in loot or ore is endless too, unless it has a cap.
- **Death resets XP.** Growth must not be lost on death.

## 4. What fits Ascension

- **Bosses.** v1 has six: the Warden and the Wither (Earth), the Ender Golem and the Ender Guardian
  (the Moon), the Netherite Monstrosity and Ignis (the Nether). Each is a set piece and each can be
  beaten first in any order. Zelda's rule, *beat a boss, gain a heart*, fits them exactly.
- **Gates.** Three in v1, seven in the full game. The mark already grows one glyph per lit gate
  (`the-mark.md`). Growth from gates is the mark's power made real. That also suits the lore as
  it stands: the voice promises the Heir great power as the glyphs fill, and the power is real.
  That is what makes the trap work. The lore needs no change for this.
- **Places to find.** Our own handcrafted sites (the Lunar Archive, the Ruined Citadel, the gate
  ruin in the warped forest), vanilla's best (the Ancient City, Trial Chambers, Woodland Mansions,
  Ocean Monuments, fortresses, bastions), and landing on each world for the first time. A finite
  list, one per place type, matches "exploration must return value".
- **Oxygen.** Ascension's own survival stat. Atmosphere already grows lung capacity through gear
  rather than cutting drain. Growth growing the lungs follows the same rule.
- **Co-op.** The gates and the mark are already team-wide: everyone on the server shares them
  (`GateProgress`). Boss growth can be the same. Personal choice goes into the tree.
- **ADR-0018.** If every boss gives the same amount, the order a team takes the worlds in never
  matters, and no world becomes a must.
- **Items scale off stats** (Sanchit, 2026-10-15). Growth must give the stats items scale off
  (Attack Damage, HP, the defences) in amounts that change those numbers, and must say whether it
  counts as base or bonus.

## 5. What we can add, and what we should

| Idea | Can we? | Should we? | Why |
|---|---|---|---|
| **First kill of each boss: a heart, a little defence, and tree points** | Yes, data only | **Yes, v1** | Set pieces, finite, any order, the clearest rule in games. |
| **Each gate lit: a heart and a tree point** | Yes, small `worlds` change | **Yes, v1** | The mark already counts gates. Team-wide by nature. |
| **Discoveries: the first visit to each notable place, a tree point** | Yes, data only (vanilla's location trigger) | **Yes, v1** | Finite, personal, pays exploration. |
| **A compact skill tree with four directions** | Yes | **Yes, v1** | Sanchit's "different directions". Points from finite sources only. |
| **Milestones kept as advancements** | Yes | **Yes, v1** | Saved, synced and shown by vanilla, with toasts. Other mods and datapacks can add milestones as plain JSON. |
| **Respec at a cost** | Yes | **Yes, v1** | Choice without a trap (§3, point 5). |
| **Oxygen in the tree** (lung capacity) | Yes, small `atmosphere` change | **Yes, soon after v1's first build** | Ascension's own stat, and gear already grows lungs. |
| **Souls-style points straight into stats, no tree** | Yes | Only if the tree is too much | Simpler screen, less identity. |
| **Relics: one stat find per handcrafted site, claimed once per player** (like Trial Chamber vaults) | Yes, a new block | Later | A good Zelda-style reward, but each site needs placing by hand. |
| **Reading the Archive's tablets grants points** | Yes | Sanchit's lore call | Information becomes power; the voice rewarding you for listening fits the trap. |
| **World-scoped power** (Elden Ring's Scadutree) | Yes | Not now | Interesting, but it makes one world's growth worthless elsewhere, which pushes against ADR-0018. |
| **Repeat boss kills, harder each time** | Yes | Not in v1 | An endgame idea once there is an endgame. |
| **Kill-XP or activity levels** | Yes | **No** | Machines farm them. |
| **Vanilla XP levels as stats** | Yes | **No** | XP is spent on enchanting. |
| **Endless paragon levels** | Yes | **No** | A treadmill with no end. |

## Sources

- [Pufferfish's Skills community trees for 1.21.1](https://www.curseforge.com/minecraft/data-packs/puffish-themission)
  and [Project MMO's bridge to it](https://www.modpackindex.com/mod/109643/project-mmo-pufferfish-skills-compat)
- [Passive Skill Tree, NeoForge 1.21.1 port](https://www.modpackindex.com/mod/203854/passive-skill-tree-neoforge)
- [Stronger Skill Tree](https://moddex.gg/mod/stronger-skill-tree-for-pufferfishs-skills): a
  Pufferfish tree where choosing one path blocks the others
- The rest is from general knowledge of the games named; numbers are as each game ships them and
  may have changed in later patches.
