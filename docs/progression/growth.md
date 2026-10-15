# Player growth

> **Proposed, 2026-10-15.** Waiting on Sanchit's decisions (§13). Research behind it:
> `growth-research.md`. Structural choices: [ADR-0021](../decisions/0021-player-growth.md).
> Every number here is a placeholder, as in `combat-api.md` §3: Sanchit balances by playing, and
> every number lives in a data file.

## 1. In short

A player grows in three ways, all permanent and all finite:

1. **Milestones.** The first time the team beats each boss, or lights each gate, everyone gains a
   heart (+2 max HP), a little defence, and skill points.
2. **Discoveries.** The first time *you* find each notable place, you gain a skill point.
3. **The skill tree.** You spend your points in four directions: Power, Fury, Guard and Wayfarer.
   This is where two players on the same team become different.

Nothing grows from kill counts, time played or vanilla XP, so nothing can be farmed. Growth gives
**numbers**; runes give **behaviours** (§11).

| Source | Everyone on the team, or you? | What it gives | v1 total |
|---|---|---|---|
| Boss, first kill | The team | A heart, +4 Armor or Magic Resist, 2 points | 6 bosses |
| Gate lit | The team | A heart, 1 point | 3 gates |
| Discovery | You | 1 point | 11 places |
| Skill tree | You | Whatever you choose | 26 points to spend |

## 2. Where it fits

| Layer | Its job | Owner |
|---|---|---|
| **Growth** | The player getting stronger over the game, for good | This page |
| **Armour and items** | Gear: what you carry | The stats session (`item-stats.md`) |
| **Runes** | A swappable build: how you fight | The runes session (`rune-system.md`) |

## 3. Milestones

**The first time the team beats each boss, or lights each gate, every player gains its reward.**
A heart per boss and a heart per gate is Zelda's rule, the clearest growth rule in games.
Milestones give survival (HP and the defences), never damage: damage is a choice, made in the tree
and with items.

| Milestone | Where | Everyone gains | Skill points |
|---|---|---|---|
| The Warden | Earth | +2 HP, +4 Armor | 2 |
| The Wither | Anywhere | +2 HP, +4 Magic Resist | 2 |
| The Ender Golem | The Moon | +2 HP, +4 Armor | 2 |
| The Ender Guardian | The Moon | +2 HP, +4 Magic Resist | 2 |
| The Netherite Monstrosity | The Nether | +2 HP, +4 Armor | 2 |
| Ignis | The Nether | +2 HP, +4 Magic Resist | 2 |
| Earth's gate lit | Earth | +2 HP | 1 |
| The Moon's gate lit | The Moon | +2 HP | 1 |
| The Nether's gate lit | The Nether | +2 HP | 1 |
| **v1 total** | | **+18 HP (20 → 38), +12 Armor, +12 Magic Resist** | **15** |

- **Every boss is worth the same**, so the order a team takes the worlds in never matters
  (ADR-0018).
- **Gates are the mark.** The mark gains a glyph with every lit gate (`the-mark.md`); now the
  player gains a heart with it. The voice promises the Heir power as the glyphs fill, and the
  power is real. That is what makes the trap work, so the lore needs no change.
- **What counts as beating a boss:** a kill vanilla credits to a player. That is the last player
  to hurt it in the last few seconds, including by arrow or a tamed wolf. A boss that dies to lava
  or `/kill` with no player behind it gives nothing.
- **A new boss or planet adds a milestone as two data files** (§9), no code.

## 4. Discoveries

**The first time you reach each notable place, you gain one skill point.** Personal: each player
finds them for themselves. One per *kind* of place, not per copy, so the list is finite.

| World | Places (v1) |
|---|---|
| Earth | An Ancient City, Trial Chambers, a Woodland Mansion, an Ocean Monument |
| The Moon | Landing on the Moon, the Lunar Archive, the Ruined Citadel |
| The Nether | Landing on the Nether, the gate ruin in the warped forest, a Nether Fortress, a Bastion |
| **v1 total** | **11 points** |

These use vanilla's own "player is inside this structure" check, which vanilla's structure
advancements already run. The Ruined Citadel is Cataclysm's, so its discovery loads only when
Cataclysm does (`ascension-compat-cataclysm`).

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
- **Two kinds of node:** small (worth 1 unit, §8) and notable (worth about 4, named, at the
  5th and 10th step of each arm). The notables are what make going deep feel worth it.
- **v1: 10 nodes per arm, 40 in all.** With 26 points a fully grown v1 player fills about two and
  a half arms. Each new world adds a few nodes to the arms' ends, as data, so there is always more
  tree than points.
- **Taken nodes stay taken** until a respec (§13, question 3).
- **The tree never holds procs or on-hit effects.** That is the runes' job (§11).

### v1 content (placeholders)

| Arm | Small nodes (8) | 5th: notable | 10th: notable |
|---|---|---|---|
| **Power** | 6 × +0.25 Attack Damage, 2 × +2 Lethality | **Heavy Hand:** +1 Attack Damage | **Sunder:** +8% Penetration |
| **Fury** | 5 × +3% Attack Speed, 3 × +1% Omnivamp | **Frenzy:** +12% Attack Speed | **Bloodthirst:** +4% Omnivamp |
| **Guard** | 4 × +1 HP, 2 × +4 Armor, 2 × +4 Magic Resist | **Stout Heart:** +4 HP | **Bastion:** +8 Armor, +8 Magic Resist |
| **Wayfarer** | 3 × +2% Move Speed, 2 × +15% mining speed, 2 × +1 safe fall distance, 1 × +0.5 block reach | **Deep Lungs:** +5 s of breath | **Sure Foot:** walk up full blocks |

- **Deep Lungs** needs `ascension-atmosphere` to read a lung-capacity attribute (§11). Until then
  the node is left out of the data.
- **Sure Foot** is vanilla's step-height attribute raised to one block.

## 6. Why none of it can be farmed

| Source | Why it can't be farmed |
|---|---|
| Bosses | First kill only. Killing the Wither ten times gives one heart. |
| Gates | Once each, and a lit gate stays lit. |
| Discoveries | Once per kind of place, not per copy. A hundred Ancient Cities give one point. |
| The tree | Points come only from the three above. A respec moves points, never makes them. |
| Death | Nothing is lost, and nothing is gained. |
| Co-op | A shared milestone is earned once per team. Five players do not make it five times. |

Turned down, from `player-power.md` and the research: kill-XP and activity levels (machines farm
them), vanilla XP levels (spent on enchanting), endless paragon levels (a treadmill).

## 7. Co-op and late joiners

- **Shared: bosses and gates.** When one happens, every player on the server gains it: those
  online now, and anyone who joins later, on their first login. "The team" means the whole server
  until Ascension has teams, the same as the gates and the mark (`GateProgress`).
- **Personal: discoveries and the tree.** The team shares the floor; each player chooses a
  direction. In a team of three, one can be the tank, one the striker, one the explorer.
- **A late joiner arrives with the team's hearts and points, unspent.** Their first act is choosing
  a direction. That closes the growth half of `todo.md`'s late-joiner gap. The gear half stays,
  as ADR-0018 already says.
- **The other way** (only players who fought gain it) needs a rule for what fighting means, and a
  late joiner would have to re-fight every boss. Cataclysm's Moon bosses may not come back.

## 8. The numbers, sized against items

### One yardstick

So the stats session can price items against growth, everything is measured in **units**. One small
node is one unit; a notable is about four.

| 1 unit is about | 1 HP | 4 Armor | 4 Magic Resist | 0.25 Attack Damage | 3% Attack Speed | 1% Omnivamp | 2 Lethality | 2% Penetration | 2% Move Speed |
|---|---|---|---|---|---|---|---|---|---|

Rough reasoning: at 38 HP, +1 HP is about 2.6% more survival, and +0.25 Attack Damage on a
diamond sword (7) is about 3.6% more damage.

### A fully grown v1 player

- **The floor (everyone):** 38 HP, +12 Armor, +12 Magic Resist. About 24 units.
- **The tree:** 26 points, about 26 units in small nodes plus up to 3 extra for each notable
  reached.
- **Example, Guard and Power filled:** 46 HP, 28 Armor, 28 Magic Resist, +2.5 Attack Damage,
  4 Lethality, 8% Penetration, and 6 points spare.

### For the stats session (through Sanchit)

- **A legendary item's raw stats ≈ 10 units**, about two bosses of growth (`player-power.md` §3.3).
  For example +2.5 Attack Damage, or +10 HP, or +40 Armor, before its effect.
- **Growth and items end up about even**, as in League: growth carries most of a player's HP and
  defence floor, items carry most of their damage.
- **Items that scale off a stat reward growth:** Sterak's Harness turns a share of max HP into
  Attack Damage, so every heart gained also feeds it.

### Base or bonus

**Proposed: growth counts as base**, like League's levels. Then an item's "bonus Attack Damage"
ratio rewards building items, and a "base" or "total" ratio rewards growth. Item designers get both
levers. The stats session owns this split, so this is a request to it (§11, §13 question 4).

### The hearts on screen

38 to 50 HP is two to two and a half rows of hearts, which vanilla draws fine. A health bar with a
number (`player-power.md` §3.5) can wait until real numbers pass that.

## 9. Storage and sync

### Milestones are advancements

**Every milestone is a vanilla advancement.** Vanilla already keeps advancements per player, saves
them with the world, sends them to the client, shows a toast and lists them on the advancements
screen. Other mods and datapacks can add advancements with plain JSON, and any mod can check one.

- **Bosses:** an advancement using vanilla's `player_killed_entity` trigger. No code.
- **Discoveries:** vanilla's `location` trigger with a structure or dimension. No code.
- **Gates:** `ascension-worlds` awards `ascension_worlds:gates/<planet>` when a gate lights,
  to every player and to anyone who joins later. A small change in `GateProgress`, which already
  tells every client when the network changes. This is how a gate reaches growth without either
  module knowing the other (ADR-0011): the advancement is the shared language.

A growth file then says what an advancement is worth, in
`data/ascension_combat/ascension_combat/growth_milestone/warden.json`:

```json
{
  "advancement": "ascension_combat:growth/warden",
  "shared": true,
  "points": 2,
  "stats": [
    { "attribute": "minecraft:generic.max_health", "amount": 2 },
    { "attribute": "ascension_combat:armor", "amount": 4 }
  ]
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
advancements and nodes whenever something changes. A balance change in a data file takes effect
on the next `/reload` or login, and a total can never drift from what earned it.

### Applying stats

- **One attribute modifier per stat, all with the id `ascension_combat:growth`.** Worked out again
  and replaced, never stacked.
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

- **Nothing ticks.** Working out growth is about fifty small sums, and only on the events above.
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
- **A side panel:** points to spend, and **where your growth came from**: each milestone and
  discovery earned, with what it gave, and totals by stat. Milestones not yet earned show as goals,
  unless they are secret.
- **A toast** when you gain a milestone names what you gained: "The Warden: +1 heart, +4 Armor,
  2 points".
- **Motion and sound:** a line fills in from the node you came from; a soft chime on taking a node,
  a deeper one on confirm.
- **Readable at GUI scales 2 to 4.**
- **The same game as the rune screen:** the same frame, palette, card style and sounds. Mockups
  first, from a seeded script as the art guide says, sent to Sanchit before any screen is built.

Could the centre emblem be the mark: seven glyph sockets around the eye, filling as gates light?
That is a lore choice, so it is a question (§13, question 9).

## 11. Working with the other sessions

**Stats (prompt 1).** Requests, through Sanchit:
1. **Count `ascension_combat:growth` modifiers as base**, in the stat-reading API (§8).
2. **No new combat stats needed.** Growth uses HP, Attack Damage, Attack Speed, Move Speed, Armor,
   Magic Resist, Omnivamp, Lethality and Penetration as they exist. Wayfarer uses vanilla
   attributes (movement speed, block break speed, safe fall distance, step height, block reach).
3. **The character sheet** could show each stat's growth share. If the API reports base and bonus
   separately, that comes free.

**Runes (prompt 2).**
1. **The boundary:** growth is numbers and permanent; runes are behaviours and swappable. The tree
   never gets procs; runes never give permanent stats.
2. **If runes unlock with growth,** a rune's unlock can name an advancement (any mod can check one)
   or a number of milestones, read through `com.ascension.combat.api.growth`. To be agreed through
   Sanchit.
3. **Visual language:** I'll build the tree screen from the rune screen's frame, palette, cards and
   sounds once its mockups are approved, rather than invent a second look.

**Atmosphere (not a parallel session).** For Deep Lungs: atmosphere registers a lung-capacity
attribute in its own api and counts it alongside its `LungCapacityModifier` registry. Small, and
only if Sanchit wants oxygen in the tree (§13, question 7).

## 12. Build order

Each step is checked in Sanchit's client, and on a dedicated server, as ADR-0008 asks.

| Step | What | How it is checked |
|---|---|---|
| **G1** | The data, storage and stats: both registries, the player and team attachments, the stat modifiers, an Ascension advancement tab, the Warden and Wither milestones, and a debug command (`/ascension growth show`, `respec`) | Dedicated server: kill the Warden, see the toast and 22 HP. A second client joins and has 22 too. Restart the server: still 22. Die: respawn at 22, full. |
| **G2** | The other sources: the gates (`worlds` awards their advancements), Cataclysm's four bosses (`compat-cataclysm` data), the discoveries | Each one earned in a client, and the totals on the debug command. |
| **G3** | Refactor pass (ADR-0008) | No new features. |
| **G4** | The tree: node data, the server's rules, the payloads, and a working screen | Take nodes, confirm, see the stats. Reload the world: still taken. A dedicated server refuses a node with no point to pay for it. |
| **G5** | The screen's art, once Sanchit has approved a mockup | Seen at GUI scales 2, 3 and 4. |
| **G6** | Refactor pass, and a reading in `performance-log.md` | Heap and tick time before and after. |

Growth adds no items in v1, so the resource ledger is unchanged.

## 13. Decisions for Sanchit

Each with the recommendation first.

1. **Co-op:** when a boss dies or a gate lights, does **the whole team** gain it, including people
   who join later? Or only the players who fought?
2. **Directions:** a **compact skill tree with four arms**? Or Souls-style points straight into
   stats with no tree? Or no choice at all, milestones only?
3. **Respec:** **anywhere out of combat, for 20 XP levels**? Free out of combat? Only at a lit gate?
   Never?
4. **Base or bonus:** does growth count as **base stats, like League's levels**? (Agreed with the
   stats session.)
5. **Advancements:** show milestones as **an Ascension tab on the advancements screen, with
   toasts**? Or keep them hidden?
6. **A heart per boss and per gate?** Every number is a placeholder, but is the shape right?
7. **Oxygen:** should the Wayfarer arm grow your lungs (a small `atmosphere` change), or leave
   oxygen to gear?
8. **Where the code lives:** **in the combat module, `internal/growth`**, as the session prompt
   said? Or a module of its own (`ascension-progression`, already on the architecture map)?
9. **The tree's centre:** the mark (the glyphs and the eye), or a neutral emblem?
10. **Later ideas:** relics in handcrafted sites, claimed once per player, and the Archive's tablets
    granting points. **Later**, or now?
