# Player growth

> **Designed 2026-10-15; Sanchit's answers the same day are built in** (§13). Research behind it:
> `growth-research.md`. Structural choices: [ADR-0021](../decisions/0021-player-growth.md).
> Every number here is a placeholder, as in `combat-api.md` §3: Sanchit balances by playing, and
> every number lives in a data file.

## 1. In short

**Every bit of growth is a skill point, and every skill point is spent in a tree.** Points come
from three places, all permanent and all finite:

1. **Bosses.** The first time the team beats each boss, every player gains 3 points.
2. **Gates.** Each gate lit gives every player 2 points.
3. **Discoveries.** The first time *you* find each notable place, you gain 1 point.

You spend points in a tree with four arms: Power, Fury, Guard and Wayfarer. Even HP is a choice,
taken in the Guard arm. This is where two players on the same team become different.

Nothing grows from kill counts, time played or vanilla XP, so nothing can be farmed. Growth gives
**numbers**; runes give **behaviours** (§11).

| Source | Who gains it | Points | v1 total |
|---|---|---|---|
| Boss, first kill | The whole team | 3 | 6 bosses: 18 |
| Gate lit | The whole team | 2 | 3 gates: 6 |
| Discovery | You | 1 | 11 places: 11 |
| **Total by the end of v1** | | | **35 points, for a 55-node tree** |

## 2. Where it fits

| Layer | Its job | Owner |
|---|---|---|
| **Growth** | The player getting stronger over the game, for good | This page |
| **Armour and items** | Gear: what you carry | The stats session (`item-stats.md`) |
| **Runes** | A swappable build: how you fight | The runes session (`rune-system.md`) |

## 3. Milestones: bosses and gates

**The first time the team beats each boss, or lights each gate, every player gains its points.**
Points only: no stat is given automatically (Sanchit, 2026-10-15).

| Milestone | Where | Points, for every player |
|---|---|---|
| The Warden | Earth | 3 |
| The Wither | Anywhere | 3 |
| The Ender Golem | The Moon | 3 |
| The Ender Guardian | The Moon | 3 |
| The Netherite Monstrosity | The Nether | 3 |
| Ignis | The Nether | 3 |
| Earth's gate lit | Earth | 2 |
| The Moon's gate lit | The Moon | 2 |
| The Nether's gate lit | The Nether | 2 |
| **v1 total** | | **24** |

- **Every boss is worth the same**, so the order a team takes the worlds in never matters
  (ADR-0018).
- **Gates are the mark.** The mark gains a glyph with every lit gate (`the-mark.md`), and now the
  player gains points with it. The voice promises the Heir power as the glyphs fill, and the
  power is real. That is what makes the trap work, so the lore needs no change.
- **What counts as beating a boss:** a kill vanilla credits to a player. That is the last player
  to hurt it in the last few seconds, including by arrow or a tamed wolf. A boss that dies to lava
  or `/kill` with no player behind it gives nothing.
- **A new boss or planet adds a milestone as two data files** (§9), no code.
- **To watch in testing:** with no automatic HP, a player who never takes Guard nodes stays at
  20 HP all game. Armour and items carry their defence. That is the choice Sanchit made; the first
  Warden fight will show whether 20 HP and armour is enough there.

## 4. Discoveries

**The first time you reach each notable place, you gain one point.** Personal: each player finds
them for themselves. One per *kind* of place, not per copy, so the list is finite.

| World | Places (v1) |
|---|---|
| Earth | An Ancient City, Trial Chambers, a Woodland Mansion, an Ocean Monument |
| The Moon | Landing on the Moon, the Lunar Archive, the Ruined Citadel |
| The Nether | Landing on the Nether, the gate ruin in the warped forest, a Nether Fortress, a Bastion |
| **v1 total** | **11 points** |

These use vanilla's own "player is inside this structure" check, which vanilla's structure
advancements already run. **Each place's advancement lives with whatever owns the place**, so none of
it needs another mod loaded:

| Where the file lives | Places | Advancements screen |
|---|---|---|
| `ascension-combat` | The six vanilla places: Ancient City, Trial Chambers, Woodland Mansion, Ocean Monument, Nether Fortress, Bastion | The **Ascension** tab |
| `ascension-worlds` | Landing on the Moon, the Lunar Archive, landing on the Nether, the gate ruin | The **Gateways** tab |
| `ascension-compat-cataclysm` | The Ruined Citadel, and the four bosses | The **Ascension** tab, only with Cataclysm |

Landing on the Moon or the Nether is a "you are
in this dimension" check, so it also counts a player who logs in or respawns there.

## 5. The skill tree

### Shape

A centre, and four arms going out from it. Each arm is a short trunk of nodes with one fork.

| Arm | Direction | Stats |
|---|---|---|
| **Power** | Hit harder | Attack Damage, Lethality, Penetration |
| **Fury** | Hit faster, heal from it | Attack Speed, Omnivamp, Move Speed |
| **Guard** | Survive | HP, Armor, Magic Resist |
| **Wayfarer** | Explore other worlds | Move speed, mining speed, safe falls, reach, oxygen |

Power pairs naturally with Reaver runes, Fury with Vanguard, Guard with Bulwark, but nothing ties
them: any arm works with any rune page. Wayfarer is Ascension's own: the planets are the game, and
surviving them is a direction worth choosing.

### Rules

- **Every node costs 1 point.** You can take a node if it touches the centre or a node you have.
- **Two kinds of node:** small (worth 1 unit, §8) and notable (named, worth about 4 units). Each
  arm has three notables, at its 5th, 10th and last (14th) step; the last is the biggest. The
  notables are what make going deep feel worth it.
- **v1: 14 nodes per arm, 56 in all.** With 35 points a fully grown v1 player fills two arms and
  half of a third, about 60% of the tree. Each new world adds nodes to the arms' ends, as data, so
  there is always more tree than points.
- **Respec: anywhere out of combat, for 20 XP levels** (Sanchit, 2026-10-15). All points come back
  to spend again. A respec moves points; it never makes them. Free in creative mode.
- **The tree never holds procs or on-hit effects.** That is the runes' job (§11).

### v1 content (placeholders)

| Arm | Small nodes (11) | 5th: notable | 10th: notable | 14th: notable |
|---|---|---|---|---|
| **Power** | 7 × +0.25 Attack Damage, 2 × +2 Lethality, 2 × +2% Penetration | **Heavy Hand:** +1 Attack Damage | **Sunder:** +8% Penetration | **Executioner:** +1.5 Attack Damage |
| **Fury** | 6 × +3% Attack Speed, 3 × +1% Omnivamp, 2 × +2% Move Speed | **Frenzy:** +12% Attack Speed | **Bloodthirst:** +4% Omnivamp | **Whirlwind:** +18% Attack Speed |
| **Guard** | 5 × +1 HP, 3 × +4 Armor, 3 × +4 Magic Resist | **Stout Heart:** +4 HP | **Bastion:** +8 Armor, +8 Magic Resist | **Unbroken:** +6 HP |
| **Wayfarer** | 4 × +2% Move Speed, 3 × +15% mining speed, 2 × +1 safe fall distance, 2 × +0.5 block reach | **Deep Lungs:** +5 s of breath | **Sure Foot:** walk up full blocks | **Pioneer:** +6% Move Speed, +2 safe fall distance |

**A full arm gives:**

| Arm | Total |
|---|---|
| Power | +4.25 Attack Damage, 4 Lethality, 12% Penetration |
| Fury | +48% Attack Speed, 7% Omnivamp, 4% Move Speed |
| Guard | +15 HP, +20 Armor, +20 Magic Resist |
| Wayfarer | +14% Move Speed, +45% mining speed, +4 safe fall, +1 block reach, +5 s breath, full-block step |

- **Deep Lungs** needs `ascension-atmosphere` to read a lung-capacity attribute (§11, §13 question
  7). Until that is decided, the node is left out of the data. **So Wayfarer has 13 nodes, not 14,
  and the whole tree 55**; its fork opens a step earlier (§5 "As built").

### As built (G2)

The three other arms are data, written the same way as Guard: a trunk of four small nodes, a
notable at the fifth, a fork of two small nodes each side, a second notable where the fork rejoins,
three more smalls, and the last notable. Positions are in the arm's own frame (the trunk runs down
the `x = 0` line away from the centre); the screen turns each arm to its side of the tree.

| Arm | Nodes | Notable differences from the table above |
|---|---|---|
| **Power** | `might_1..7`, `edge_1..2`, `piercing_1..2`, `heavy_hand`, `sunder`, `executioner` | The small Penetration nodes give Armor Pen; **Sunder gives 8% Armor Pen and 8% Magic Pen** |
| **Fury** | `tempo_1..6`, `hunger_1..3`, `swiftness_1..2`, `frenzy`, `bloodthirst`, `whirlwind` | None. Attack Speed is a percent of the base; Omnivamp is a fraction (0.01 is 1%) |
| **Wayfarer** | `pace_1..4`, `delving_1..3`, `long_arm_1..2`, `soft_landing_1..2`, `sure_foot`, `pioneer` | No Deep Lungs. Mining speed is `block_break_speed`, reach is `block_interaction_range`, Sure Foot is +0.4 `step_height` (0.6 to 1.0) |
- **Sure Foot** is vanilla's step-height attribute raised to one block.

## 6. Why none of it can be farmed

| Source | Why it can't be farmed |
|---|---|
| Bosses | First kill only. Killing the Wither ten times gives 3 points, once. |
| Gates | Once each, and a lit gate stays lit. |
| Discoveries | Once per kind of place, not per copy. A hundred Ancient Cities give one point. |
| The tree | Points come only from the three above. A respec moves points, never makes them. |
| Death | Nothing is lost, and nothing is gained. |
| Co-op | A shared milestone is earned once per team. Five players do not make it five times. |

Turned down, from `player-power.md` and the research: kill-XP and activity levels (machines farm
them), vanilla XP levels (spent on enchanting), endless paragon levels (a treadmill).

## 7. Co-op and late joiners

**Decided (Sanchit, 2026-10-15): bosses and gates go to the whole team.**

- **Shared: bosses and gates.** When one happens, every player on the server gains its points:
  those online now, and anyone who joins later, on their first login. "The team" means the whole
  server until Ascension has teams, the same as the gates and the mark (`GateProgress`).
- **Personal: discoveries and the tree.** The team shares the points; each player spends theirs
  in their own direction. In a team of three, one can be the tank, one the striker, one the
  explorer.
- **A late joiner arrives with the team's points, unspent.** Their first act is choosing a
  direction. That closes the growth half of `todo.md`'s late-joiner gap. The gear half stays, as
  ADR-0018 already says.

## 8. The numbers, sized against items

### One yardstick

So the stats session can price items against growth, everything is measured in **units**. One
small node, one point, is one unit.

| 1 unit is about | 1 HP | 4 Armor | 4 Magic Resist | 0.25 Attack Damage | 3% Attack Speed | 1% Omnivamp | 2 Lethality | 2% Penetration | 2% Move Speed |
|---|---|---|---|---|---|---|---|---|---|

Rough reasoning: at 30 HP, +1 HP is about 3% more survival, and +0.25 Attack Damage on a diamond
sword (7) is about 3.6% more damage.

### A fully grown v1 player

35 points: about 35 units, plus a little more for each notable reached.

- **Guard and Power filled, 7 points spare:** 35 HP, 20 Armor, 20 Magic Resist, +4.25 Attack
  Damage, 4 Lethality, 12% Penetration.
- **Fury and Wayfarer filled:** 20 HP, +48% Attack Speed, 7% Omnivamp, +18% Move Speed, and
  the explorer's kit.

### Base or bonus: neither

**There is no base/bonus split** (the stats session, `item-stats.md` §6, Sanchit 2026-10-15). A
stat is one total, and items scale off the total. Growth adds to that total, so every point a player
puts into Power also makes their items hit harder. That is what Sanchit's answer "bonus" asked for,
and his "these stats will be used in the items' scaling as well".

### For the stats session (through Sanchit)

- **Growth's modifiers carry the prefix `ascension_combat:growth/`**, which growth registers with
  `StatSources` as "Growth" once that API lands (their increment 2).
- **A legendary item's raw stats ≈ 8 to 10 units**, about the growth from two or three bosses
  (`player-power.md` §3.3). For example +2.5 Attack Damage, or +10 HP, or +40 Armor, before its
  effect.
- **Growth and items end up about even.** Items should carry most of a player's damage; growth
  is how a player chooses what kind of fighter to be.

### The hearts on screen

A Guard player reaches about 35 HP, under two rows of hearts, which vanilla draws fine. A health
bar with a number (`player-power.md` §3.5) can wait until real numbers pass that.

## 9. Storage and sync

### Milestones are advancements

**Every milestone is a vanilla advancement.** Vanilla already keeps advancements per player, saves
them with the world, sends them to the client, shows a toast and lists them on the advancements
screen. Other mods and datapacks can add advancements with plain JSON, and any mod can check one.

- **Bosses:** an advancement using vanilla's `player_killed_entity` trigger. No code.
- **Discoveries:** vanilla's `location` trigger with a structure or dimension. No code.
- **Gates:** `ascension-worlds` awards `ascension_worlds:gates/<planet>` when a gate lights,
  to every player online and to anyone who joins later (`GateAdvancements`, called from
  `GateProgress`, which already tells every client when the network changes). The path is the
  planet's dimension path (`overworld`, `moon`, `the_nether`), with its namespace added for a planet
  from another pack. This is how a gate reaches growth without either module knowing the other
  (ADR-0011): the advancement is the shared language.

**Where a milestone's files live.** A growth file is a datapack registry entry, and a namespace
may add to any registry, so **the milestone file lives in the module that owns the boss, gate or
place**, next to its advancement: `data/<namespace>/ascension_combat/growth_milestone/...`. Without
`ascension-combat` that folder is never read, so `worlds` and `compat-cataclysm` carry growth
files that do nothing on their own and need no condition. Only an advancement whose parent is
Ascension's root needs `neoforge:conditions` (mod loaded: `ascension_combat`), which the Cataclysm
ones have. A new planet's pack therefore adds its gate, its places and what they are worth, with no
change to combat.

A growth file then says what an advancement is worth, in
`data/ascension_combat/ascension_combat/growth_milestone/warden.json`:

```json
{
  "advancement": "ascension_combat:growth/warden",
  "shared": true,
  "points": 3
}
```

And a tree node, in `data/ascension_combat/ascension_combat/growth_node/guard/stout_heart.json`:

```json
{
  "arm": "guard",
  "kind": "notable",
  "position": [0, 5],
  "links": ["ascension_combat:guard/4"],
  "stats": [
    { "attribute": "minecraft:generic.max_health", "amount": 4 }
  ]
}
```

- **Stats name any attribute by id**, vanilla's, ours, or another mod's. An attribute that isn't
  installed is skipped, with one log line. So growth can raise oxygen (an `atmosphere` attribute)
  without depending on `atmosphere`.
- **Percent stats** take `"operation": "add_multiplied_base"`, as vanilla's own modifiers do.
- **Both are datapack registries, sent to clients**, the same way `archive_entry` is. The tree
  screen draws from them.

### What is stored

| What | Where | Saved? |
|---|---|---|
| A player's milestones | Vanilla advancements | Yes, by vanilla |
| A player's taken nodes | A player attachment, `growth` | Yes, and kept through death |
| The team's shared milestones | An attachment on the Overworld, like the gateway network | Yes |
| Points and stats | Nowhere: worked out from the three above | No |

**Nothing is added up and stored.** Points and stats are worked out again from the player's
advancements and nodes whenever something changes, so a total can never drift from what earned it,
and a node removed from the data gives its point back. Growth's own files are datapack registries,
which load at server start, as planets and archive entries do: a change to one needs a restart.
Advancements reload with `/reload`. The server log checks the data at every start and reload: a
milestone naming a missing advancement, a link to a missing node, or an attribute that isn't
installed is reported, not silently skipped.

### Applying stats

- **One attribute modifier per attribute and operation**, with ids `ascension_combat:growth/add_value`
  and `ascension_combat:growth/add_multiplied_base` (and `add_multiplied_total`, unused so far).
  Worked out again and replaced, never stacked.
- **Permanent modifiers**, saved with the player: their saved attributes load before their health, so
  max HP from growth is in place before a reloaded player's health is read.
- **When:** login, respawn, an advancement earned or revoked, nodes taken, a respec, `/reload`.
- **Gaining max HP heals the difference**, so a new heart arrives full.
- **On respawn**, the new body gets its modifiers first, then full health.

### Sync

- **Stats** sync themselves: max HP is vanilla's, and every combat attribute is already syncable.
- **The screen's state:** one payload with the taken nodes and points earned and spent, sent at
  login and on any change. Never per tick.
- **From the client:** a payload with the nodes taken in one go (on confirm), and one for a respec.
  The server checks every rule and ignores anything that breaks one.

### Performance (ADR-0007)

- **Nothing ticks.** Working out growth is about a hundred small sums, and only on the events above.
- **Discoveries** ride vanilla's own location check, about once a second per player and only for
  criteria not yet earned, the same check vanilla's structure advancements already run.
- **No static collection holds a player.** The team state lives on the Overworld; the client's copy
  is cleared on logout.

## 10. The screen

**Opened with K** (bound by default, so players find it).

- **The tree in the middle:** the centre emblem, and the four arms going out, each in its own
  colour. Small nodes are round sockets; notables are larger and framed. Taken nodes glow, nodes you
  can take pulse softly, the rest are dim. Drag to pan if the tree is bigger than the screen.
- **Hovering a node** shows a card: its name, what it gives in numbers, and whether you can take it.
- **Taking nodes is two steps:** click to mark, then confirm. Nothing is spent until you confirm,
  and you can unmark before then.
- **A side panel:** points to spend, and **where your points came from**: each milestone and
  discovery earned, with how many points it gave. Milestones not yet earned show as goals, unless
  they are secret. Below that, your totals by stat.
- **A respec button**, greyed out in combat or without 20 XP levels, and asking you to confirm.
- **A toast** when you gain a milestone: "The Warden: 3 skill points".
- **Motion and sound:** a line fills in from the node you came from; a soft chime on taking a node,
  a deeper one on confirm.
- **Readable at GUI scales 2 to 4.**
- **The same game as the rune screen:** the same frame, palette, card style and sounds. Mockups
  first, from a seeded script as the art guide says, sent to Sanchit before any screen is built.

## 11. Working with the other sessions

**Stats (prompt 1).** Requests, through Sanchit:
1. **Growth registers `ascension_combat:growth/` with `StatSources`** as "Growth" once that API
   lands. Nothing else is needed: there is no base/bonus split (§8).
2. **No new combat stats needed.** Growth uses HP, Attack Damage, Attack Speed, Move Speed, Armor,
   Magic Resist, Omnivamp, Lethality and Penetration as they exist. Wayfarer uses vanilla
   attributes (movement speed, block break speed, safe fall distance, step height, block reach).
3. **The character sheet** could show each stat's growth share, if the API reports it.

**Runes (prompt 2).**
1. **The boundary:** growth is numbers and permanent; runes are behaviours and swappable. The tree
   never gets procs; runes never give permanent stats.
2. **If runes unlock with growth,** a rune's unlock can name an advancement (any mod can check one)
   or a number of milestones, read through `com.ascension.combat.api.growth`. To be agreed through
   Sanchit.
3. **Visual language:** the tree screen will use the rune screen's frame, palette, cards and sounds
   once its mockups are approved, rather than invent a second look.

**Atmosphere (not a parallel session).** For Deep Lungs: atmosphere registers a lung-capacity
attribute in its own api and counts it alongside its `LungCapacityModifier` registry. Small, and
only if Sanchit wants oxygen in the tree (§13, question 7).

## 12. Build order

Each step is checked in Sanchit's client, and on a dedicated server, as ADR-0008 asks. The screen
comes last, so the rules underneath are proven first with a debug command.

| Step | What | How it is checked |
|---|---|---|
| **G1** | The data and the rules: both registries, the player and team attachments, the stat modifiers, an Ascension advancement tab, the Warden and Wither milestones, the Guard arm's nodes, the respec rule, and a command (`/ascension growth`, `take <node>`, `respec`) | Dedicated server: kill the Warden, see the toast and 3 points. Take two Guard HP nodes: 22 HP. A second client joins and has 3 points too. Restart the server: still 22. Die: respawn at 22, full. |
| **G2** | Every source and every node: the gates (`worlds` awards their advancements), Cataclysm's four bosses (`compat-cataclysm` data), the discoveries, and the other three arms. **Built 2026-10-17**, not yet seen in a client | Each source earned in a client, totals on the command. |
| **G3** | Refactor pass (ADR-0008) | No new features. |
| **G4** | The screen, working: the tree, cards, taking and confirming, the side panel, the respec button, the toast | Take nodes from the screen, confirm, see the stats. Reload the world: still taken. |
| **G5** | The screen's art, once Sanchit has approved a mockup | Seen at GUI scales 2, 3 and 4. |
| **G6** | Refactor pass, and a reading in `performance-log.md` | Heap and tick time before and after. |

Growth adds no items in v1, so the resource ledger is unchanged.

## 13. Decisions

**Answered by Sanchit, 2026-10-15:**

1. **Co-op:** bosses and gates go to **the whole team**, including people who join later.
2. **What a milestone gives:** **skill points only**, about 3 per boss and 2 per gate. No stat is
   given automatically.
3. **Directions:** **a four-arm skill tree.**
4. **Respec:** **anywhere out of combat, for 20 XP levels.**
5. **Base or bonus:** growth counts as **bonus**. The stats session then settled that there is no
   split at all: growth adds to the one total items scale off, which gives the same result.

**Still open.** Until Sanchit says otherwise, the build follows the recommendation, which is listed
first:

6. **Advancements:** show milestones as **an Ascension tab on the advancements screen, with
   toasts**? Or keep them hidden?
7. **Oxygen:** should the Wayfarer arm grow your lungs (a small `atmosphere` change), or leave
   oxygen to gear? Deep Lungs stays out of the data until this is answered.
8. **Where the code lives:** **in the combat module, `internal/growth`**, as the session prompt
   said? Or a module of its own (`ascension-progression`, already on the architecture map)?
9. **The tree's centre:** the mark (the glyphs and the eye), or a neutral emblem? Needed by G5.
10. **Later ideas:** relics in handcrafted sites, claimed once per player, and the Archive's tablets
    granting points. **Later**, or now?
