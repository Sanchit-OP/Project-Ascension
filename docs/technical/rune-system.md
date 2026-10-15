# The rune system

> **Draft for Sanchit, 2026-10-15**, by the rune session. Built on the research in
> [`docs/progression/rune-research.md`](../progression/rune-research.md). The structural choices
> are in [ADR-0020](../decisions/0020-rune-pages.md) (proposed). The questions waiting on Sanchit
> are in §13; until he answers, every "proposed" below is a recommendation, not a decision.
>
> Replaces `combat-api.md` §7 once accepted. Every number is a placeholder (`combat-api.md` §3):
> Sanchit balances by playing, and every number lives in a data file.

## 1. In short

- **Four paths:** Vanguard, Reaver, Aetherwright, Bulwark. Each has **three keystones** and **two
  rows of three minors**: 36 runes in all, 5 of them built today.
- **A page:** a primary path (one keystone, one minor from each row), a secondary path (one minor
  from each of its rows) and three **stat shards**.
- **Slots open as you grow**, through the growth session's milestones. Every rune can be tried from
  the first minute.
- **Change freely out of combat.** No altar.
- **Runes are data.** Each rune's numbers are a JSON file the client also receives, so hover cards
  show the real numbers and retuning never needs a rebuild.
- **A real screen:** a custom panel, emblems, an icon per rune, hover cards, motion and sound.
  Mockups first.

## 2. Where runes fit

| Layer | Its job | Owner |
|---|---|---|
| **Growth** | Permanent numbers: you get stronger over the game | Growth session (`growth.md`) |
| **Items** | What kind of fighter you are | Combat items, stats session (`item-stats.md`) |
| **Runes** | **How you fight**: behaviours you swap between | This doc |

**The boundary with growth** (agreed in both docs): growth is numbers and permanent; runes are
behaviours and swappable. The skill tree never holds procs; runes never give permanent stats.
Stat shards are the one place runes give plain stats, and they are small and swappable.

**Stats** are given and read only through the stats session's API (`item-stats.md` §6): runes add
attribute modifiers under a prefix registered as **bonus**, and read every stat through
`StatReader`.

## 3. The page

| Part | Picks | Rules |
|---|---|---|
| **Primary path** | 1 keystone + 2 minors | One of the path's three keystones. One minor from each of its two rows. |
| **Secondary path** | 2 minors | Any other path. One minor from each of its two rows. No keystone. |
| **Stat shards** | 3 | One from each row: offence, flex, defence. |

That is **eight picks**, against League's nine and today's five.

**Why two rows, not League's three** (research §7): Minecraft has fewer combat verbs than League,
and filler runes were League's own worst lesson. Each row asks one question, and every answer
should be worth taking.

**What the server checks** before saving a page (the client is never trusted):
- the primary path is set; the secondary, if set, is a different path;
- the keystone belongs to the primary path;
- each minor belongs to its side's path, at most one per row;
- each shard belongs to its row;
- every slot used is open to this player (§6);
- the player is out of combat (§7).

A page may be left part-empty. An empty slot is simply nothing.

## 4. The four paths

Each path has a colour, an emblem and a one-line promise. Colours avoid teal (the gateway
network's, `art-guide.md`). Emblems and icons are described in §10.

| Path | Promise | Colour | Plays like |
|---|---|---|---|
| **Vanguard** | *Keep hitting.* Damage that grows the longer you fight. | Forged gold | A duellist who wins long fights |
| **Reaver** | *Strike first, finish fast.* Burst, execute, ambush. | Crimson | An assassin who picks the moment |
| **Aetherwright** | *Make the magic do more.* Magic procs, burns, arcs. | Violet | A fighter whose weapon does the casting |
| **Bulwark** | *Stand in front. Bring everyone home.* Defence, the team, air. | Steel blue | The one the others stand behind |

Status: **Built** (exists today), **New**.

### 4.1 Vanguard: keep hitting

| Slot | Rune | What it does | Placeholder numbers | Status |
|---|---|---|---|---|
| Keystone | **Conqueror's Resolve** | Every second in combat you gain a stack. Each stack gives Attack Damage and omnivamp. Stacks clear when the fight ends. | 10 stacks; +0.5 AD and +1% omnivamp each | Built |
| Keystone | **Cadence** | Land three **fully charged** hits (melee at full swing, or fully drawn arrows) on one enemy within 4 s. The third deals bonus damage and **Exposes** it: it takes more damage from everyone. | +3 physical; Exposed: +10% damage taken for 5 s | New |
| Keystone | **Skirmisher** | Sprinting and hitting build charge. At full charge, your next hit (melee or arrow) heals you and gives a burst of speed. | Full at 100: 1 per block sprinted, 10 per hit; heals 3 HP; +25% speed for 2 s | New |
| Row 1: *how do you stay in the fight?* | **Second Wind** | Regenerate while in combat and below half HP. | 1 HP a second | Built |
| | **Momentum** | Chaining hits quickly builds stacks; each stack heals you for a share of the damage you deal. | 5 stacks, 2 s to chain; 1% per stack | Built |
| | **Spoils of War** | A kill heals you. | 2 HP + 5% of missing HP | New |
| Row 2: *how does your damage grow?* | **Relentless Strikes** | Hits build a stack that fades if you stop. Each stack adds damage to every hit. | 4 stacks, +0.75 physical each, 3 s to fade | Built |
| | **Killing Surge** | A kill gives a burst of attack speed. | +20% for 3 s | Built |
| | **Giant Slayer** | More damage to enemies with more max HP than you. Bosses. | +5% at 10% more HP, up to +15% at double | New |

### 4.2 Reaver: strike first, finish fast

| Slot | Rune | What it does | Placeholder numbers | Status |
|---|---|---|---|---|
| Keystone | **Three Cuts** | Hit one enemy three separate times within 3 s for a burst of bonus damage. | 4 + 50% of bonus AD, physical; 8 s cooldown | New |
| Keystone | **Ambush** | Hits on an enemy **that isn't targeting you** deal much more damage, more still if critical. Sneak up on things; weak against bosses, which always see you. | +40%; +20% more on a critical hit | New |
| Keystone | **Reaper's Tithe** | Hitting an enemy below half HP deals bonus damage. If it dies within 3 s, you take a **Soul**, and each Soul adds to the bonus. **You lose every Soul when you die.** | 3 + 0.5 per Soul; 20 Souls max; 6 s cooldown | New |
| Row 1: *how do you open?* | **Pounce** | Your critical (falling) hits hit harder. | Crit multiplier 1.5 → 1.75 | New |
| | **Drop In** | For a few seconds after a dash, blink, ender pearl or a fall of 4+ blocks, you cut through armour. | +10 Lethality, +10% penetration for 4 s | New |
| | **Unseen** | Out of combat and sneaking, mobs only notice you up close. | Within 6 blocks | New |
| Row 2: *how do you finish?* | **Finishing Blow** | More damage to enemies below 40% HP. | +12% | New |
| | **Overkill** | Damage beyond what a kill needed splashes onto the nearest enemy. | Within 4 blocks, up to 6 damage | New |
| | **Bloodrush** | A kill speeds you up and cuts your keystone's and items' cooldowns. | +20% speed for 3 s; −2 s cooldowns | New |

### 4.3 Aetherwright: make the magic do more

ADR-0012 rules out spells. Magic damage comes from procs: a few combat items' (Ragefang, Darken
Blade, Heartwood of Pallas, Unending Despair), and this path's own. **Every keystone here makes
its own magic damage**, so the path works with any weapon and grows with the magic items.

| Slot | Rune | What it does | Placeholder numbers | Status |
|---|---|---|---|---|
| Keystone | **Starfall** | A fully charged hit, or any magic damage you deal, calls a star down on the target. It lands a second later and hurts everything near it. Magic hits shorten its cooldown. | 4 + 20% of AD, magic, 2-block radius; 10 s cooldown, −1 s per magic hit | New |
| Keystone | **Kindle** | Your hits set an arcane burn that your next hits keep alive. After 3 s it burns hotter. It isn't fire: fire-immune Nether mobs burn too. | 1 magic a second for 4 s; +75% after 3 s | New |
| Keystone | **Arc** | Every fourth hit charges your weapon. The next hit releases an arc that jumps between enemies. Using an item's active fully charges it. | 3 + 15% of AD, magic, up to 3 jumps within 5 blocks | New |
| Row 1: *what makes your magic hit harder?* | **Attunement** | All your magic damage is stronger. | +12% | New |
| | **Unweave** | Your magic damage ignores part of Magic Resist. | 15% | New |
| | **Clarity** | While above 70% HP, more Attack Damage and magic damage. | +1 AD, +8% magic | New |
| Row 2: *what does your magic give back?* | **Siphon** | Heal for part of the magic damage you deal. | 10% | New |
| | **Quickening** | Items' active abilities recharge faster. | 20% faster | New |
| | **Gathering Charge** | Your magic grows the longer you stay alive. Lost when you die. | +2% every 2 minutes, up to +16% | New |

### 4.4 Bulwark: stand in front, bring everyone home

**Allies** are other players and your tamed pets near you, found when something happens (an event),
never by scanning every tick. So the team runes still do something for a solo player with a wolf.

| Slot | Rune | What it does | Placeholder numbers | Status |
|---|---|---|---|---|
| Keystone | **Iron Grip** | Every 4 s in combat, your next hit deals bonus damage from your max HP, heals you, and raises your max HP for the rest of the fight. | 4% of max HP as magic; heal 2% of max HP; +1 max HP, up to +6 | New |
| Keystone | **Brace** | Block a hit with a shield, or take a big hit, and you brace: huge Armor and Magic Resist for a moment, then a shockwave that hurts and throws back everything near you. | Big hit: 25% of max HP. +40 Armor and Magic Resist for 2.5 s; shockwave 3 + 10% of bonus Armor, magic, 4 blocks; 12 s cooldown | New |
| Keystone | **Lifeline** | You bond with the nearest ally. **In combat you breathe for both of you:** your air drains faster, theirs slower. When either of you takes a big hit, you both get a shield. Alone, the shield is yours. | 8 blocks; your drain +40%, theirs −40%; big hit: 20% of max HP; shield 3 + 10% of your max HP; 15 s cooldown | New |
| Row 1: *how do you take a hit?* | **Plating** | After a hard hit, the next few hits are softened. | After 4+ damage: next 3 hits within 3 s take 30% less; 12 s cooldown | New |
| | **Unbowed** | Harmful effects on you wear off sooner, and knockback moves you less. | 30% shorter; 40% less knockback | New |
| | **Hardened** | The longer the fight, the tougher you get. | After 10 s in one fight: +15 Armor and Magic Resist | New |
| Row 2: *what does your defence give back?* | **Shield Bash** | After you gain a shield, your next hit deals bonus damage. | 2 + 2% of max HP | New |
| | **Revitalize** | Heals and shields you give or receive are stronger, more so on someone low. | +10%; +20% below 40% HP | New |
| | **Challenge** | Mobs you hit keep attacking you instead of your allies, pets or villagers. | 4 s | New |

**Lifeline is the oxygen tie-in** confirmed in `combat-api.md` §7. It answers that doc's open
question with both readings at once: the Bulwark player **pays** air, and the ally's drain is
**blunted**. On Earth and the Nether (breathable air) the air part costs nothing; in vacuum it is a
real price. Swapping runes per world is part of the game. How the drain reaches `atmosphere`
without a Tier 1 dependency is in §9.

## 5. Stat shards

Small plain stats, out of the paths' way (research §2, lesson 4). All three together are worth
about **one boss's growth** (`growth.md` §8), so a full page never outweighs progress.

| Row | Choices |
|---|---|
| **Offence** | +1 Attack Damage · +8% Attack Speed · +3% Move Speed |
| **Flex** | +1 Attack Damage · +5 Armor · +5 Magic Resist |
| **Defence** | +2 HP · +5 Armor · +5 Magic Resist |

## 6. Unlocks: slots open, runes don't

**Proposed:** every rune can be chosen from the first minute: trying builds is the fun. What
opens with growth is the page's **slots**:

| When | What opens |
|---|---|
| **From the start** | The primary path: any keystone, both minors |
| **1st milestone** | The secondary path |
| **2nd milestone** | The offence shard |
| **3rd milestone** | The flex shard |
| **4th milestone** | The defence shard |

- **A milestone is whatever growth counts** (`growth.md` §3: a boss's first kill, a lit gate). The
  rune side reads a number through growth's public API, `com.ascension.combat.api.growth`. That
  hook is agreed through Sanchit.
- **Why these:** the keystone is a path's identity, and the first thing a new player should
  discover. The secondary path and shards add breadth and raw power, which is what progress should
  bring. Four milestones arrive in the first half of v1 (six bosses and three gates exist).
- **No world is ever needed** (ADR-0018): any milestone counts, in any order.
- **A locked slot is shown**, dark with a padlock, and its card says what opens it.
- **Until growth is merged**, a server config switch (`runes.unlocks`: `all` or `progressive`)
  defaults to `all`.

## 7. Changing runes

**Proposed: free, any time out of combat.** WoW dropped its talent tomes for the same reason
(research §3): the cheaper a change, the more players experiment.

- The screen opens any time. In combat, it shows "In combat: changes wait until the fight ends",
  and saving is refused by the server with that reason, not silently.
- Nothing is saved until **Save**. **Reset** returns to the saved page.
- A change takes effect at once. Stacks from the old page (Conqueror's, Souls) are cleared.
- Saved pages (named loadouts) are a later nicety, not v1.

## 8. Effects: what a player sees

Research §2 lesson 5: a rune you can't feel might as well not exist.

- **Every keystone shows when it fires**: particles in its path's colour, and a vanilla sound.
  Starfall's streak, Three Cuts' slash, Brace's shockwave ring, Lifeline's shield shimmer.
- **Stacks you build** (Conqueror's, Souls, Gathering Charge) show as a small counter beside the
  hotbar, using the screen's art. One HUD element for every rune, not one each.
- **Exposed, Kindled** are mob effects, so they show the effect's particles on the target.

## 9. How it's built

### Runes are data

Every rune is a JSON file in a datapack registry, `ascension_combat:rune`, synced to clients the
way planets are (`AscensionWorlds`' `DataPackRegistryEvent.NewRegistry`):

```json
// data/ascension_combat/ascension_combat/rune/cadence.json
{
  "path": "vanguard",
  "slot": "keystone",
  "position": 1,
  "icon": "ascension_combat:rune/cadence",
  "effect": "ascension_combat:cadence",
  "values": { "hits": 3, "window_seconds": 4, "bonus_damage": 3.0,
              "exposed_seconds": 5, "exposed_amplify": 0.10 }
}
```

- **`effect`** names the Java behaviour (one listener class per behaviour, as today). A datapack can
  make a variant with different numbers, or remove a rune, with no code.
- **`values`** are the numbers. The behaviour reads them from the rune; the hover card prints them.
  Retuning is a JSON edit.
- **Rows and shards** use `"slot": "minor"` with `"row": 0 | 1`, and `"slot": "shard"` with
  `"row": "offence" | "flex" | "defence"`.
- **Text** is in the lang file: the name, one plain sentence, and a detail line with the numbers
  (`%s`), coloured by stat on the card.

### The page

`RunePageState` (an attachment, saved with the player, as today) gains the keystone and the shards,
and the minors become a choice per row. Old saves load: a missing keystone becomes the path's first,
missing shards stay empty.

Payloads: `SetRunePagePayload` (client → server) carries the whole page. A new
`RunePageResultPayload` (server → client) says saved, or refused and why. `RunePageSyncPayload`
(server → client, on login and after a save) is unchanged in role.

### Oxygen without a Tier 1 dependency

`combat` can't register into `atmosphere`'s `DrainModifier` registry (ADR-0011). **Proposed:** a
small contract in `core`, `BreathDemand`: any module registers a source that answers "how much
faster or slower does this player use air right now?", and `atmosphere` registers one
`DrainModifier` that multiplies them all. Combat answers for Lifeline. The same seam later serves
`gear`'s thrusters. It gets its own ADR when Lifeline is built (§12, R5).

### Performance (ADR-0007)

- **No collection outside an attachment** holds a player or an entity. Fight-local state (stacks,
  cooldowns, the Lifeline bond as a UUID) lives in player attachments, cleared on logout and death.
- **No per-tick scans.** Listeners are per event. The few per-player tick checks (Conqueror's,
  Skirmisher's sprint charge) leave at once unless the player has that rune.
- **Allies** are found on the event that needs them, from the level's player list and the player's
  own pets, within a radius. Never by scanning entities every tick.
- **Starfall's delayed strike** is a game time stored on the player, resolved on their next tick.

### API (`com.ascension.combat.api.rune`)

- `RunePath` (today), plus a read-only `RunePages.get(Player)`: path, keystone, minors, shards.
- A `RunePageChangedEvent` on the NeoForge bus, for other mods.
- Rune behaviours stay internal in v1. Letting other mods add behaviours is a later API change.

## 10. The screen

**What it must do**, whatever look Sanchit chooses from the mockups:

- **Fit 420 × 240 GUI pixels**, so it works at GUI scales 2 to 4 on a 720p or 1080p screen. Pixel
  art at one texel per GUI pixel: sharp at every scale.
- **Show the whole page at once:** the primary path, its keystones and rows, the secondary path,
  the shards, Save and Reset.
- **The path is the identity:** its colour washes the panel, its emblem heads its column.
- **Four states** told apart by brightness, not only colour: chosen (full colour, lit ring), open
  (dim), not allowed by the rules (dimmer), locked by growth (dark, padlock).
- **Hover cards:** a nine-sliced panel beside the rune, never over it, kept on screen. Name, path,
  one plain sentence, the numbers coloured by stat, conditions ("in combat", "8 s cooldown"), and
  for a locked slot, what opens it.
- **Motion:** a rune brightens and lifts a pixel on hover; a ring draws round it when chosen
  (150 ms); the path colour crossfades (200 ms); the chosen keystone breathes slowly. Whole-pixel
  steps only.
- **Sound** (vanilla sound events, played as UI sounds): a quiet tick on hover, a chime on
  choosing pitched per path, a heavier one for a keystone, a dull note when refused, and a seal on
  Save.
- **Opened with R**, bound by default (growth's tree takes K).
- **The same game as growth's tree** (`growth.md` §10): the frame, cards, buttons, sockets and sounds
  are one kit, made by one script, that the growth screen reuses.

**The art**, all from one seeded script, `tools/generate-rune-art.py` (`art-guide.md`):
- 4 path emblems, 36 rune icons, 9 shard icons, socket frames per slot type and state, the panel,
  the hover card, buttons, the padlock.
- **Shape language per path:** Vanguard heavy and upright (blades, banners, chevrons); Reaver
  sharp and leaning (fangs, crescents, slashes); Aetherwright round and orbiting (stars, sparks,
  spirals); Bulwark broad and closed (shields, walls, rings).
- **No accidental symbols** (no crosses, letters or faces), and no teal or diamond eye: those are
  the network's.

**The look** is the mockups' job: three directions, rendered at GUI scales 2, 3 and 4, sent to
Sanchit. Nothing is built until he picks one.

## 11. Requested stats

For the stats session, through Sanchit (`combat-api.md` §3, "Requested stats"):

| Stat | For | Until it exists |
|---|---|---|
| **Tenacity** | Unbowed: shorter harmful effects, less knockback | The rune does it itself |
| **Heal and Shield Power** | Revitalize | The rune does it itself |
| **Item Haste** | Quickening, Bloodrush | The rune does it itself |

None blocks the runes. Each would also be worth an item later, which is why it belongs in the stat
model rather than inside a rune.

## 12. Increments

Each ends seen in Sanchit's client (ADR-0008); anything touching world state also on a dedicated
server; anything saved, across a world reload.

| # | What | Seen working when |
|---|---|---|
| **R1** | **The page:** the rune registry and its JSON, the new page shape (keystone choice, rows, shards), validation, the in-combat rule, the result payload. Vanguard moved to data. The old screen made to drive it. | A page saved on a dedicated server survives a reload; a page saved in combat is refused with a reason. |
| **R2** | **Vanguard's** two new keystones and two new minors. | Each fires, with its effect visible. |
| **R3** | **Refactor pass** (ADR-0008). | |
| **R4** | **Reaver** and **Aetherwright**. | Each rune fires. |
| **R5** | **Bulwark**, with the `BreathDemand` contract in `core` (own ADR). Stat shards. | Lifeline seen with two clients on the Moon. |
| **R6** | **Refactor pass.** | |
| **R7** | **The screen**, in the look Sanchit chose: panel, cards, motion, sound. The HUD stack counter. | At GUI scales 2, 3, 4. |
| **R8** | **The art:** every icon, emblem and frame from the generator. | In the screen, at all three scales. |
| **R9** | **Unlocks**, once growth's API is merged. | Slots opening as milestones land, on a dedicated server. |
| **R10** | **Refactor pass.** | |

The screen and art (R7–R8) can move before R4 if Sanchit would rather see the new screen first.

## 13. Decisions for Sanchit

1. **Page shape:** three keystones per path, two rows of three minors (recommended)? Or League's
   three rows? Or keep one keystone per path?
2. **Unlocks:** slots open with growth, every rune available from the start (recommended)? Or the
   keystone waits for the first boss (`player-power.md`'s idea)? Or no unlocks at all?
3. **Changing runes:** free out of combat (recommended)? Or only at a block, like an altar?
4. **Bulwark and oxygen:** Lifeline, where the Bulwark player pays air and the ally's drain is
   eased (recommended)? Or a combat drain everyone pays, which Bulwark blunts?
5. **Stat shards:** three rows, about one boss's worth in total (recommended)? Or none?
6. **The look:** which mockup? (Sent separately.)
7. **Lore:** leave runes lore-neutral for now, with no teal or eye (recommended)? Or are they the
   Observers' gift, or the Sealers' discipline?
8. **Names:** "Second Wind" is League's exact name; rename it (say, "Grit")? Every other name is
   ours to change too.
9. **Keybind:** R (recommended)?
