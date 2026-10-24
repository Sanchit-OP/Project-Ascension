# Loot: what a chest gives on each planet

> **Built 2026-10-22, proved on a dedicated server (`loot-checks.txt`), not yet seen in a client.** Sanchit's complaint:
> loot was poor, and nothing improved on the Moon or beyond; the Ender Golem arena's chests gave wheat. This page is
> the answer, the numbers it gives and how to change them.

## What was wrong

The Ender Golem arena is Cataclysm's **Ruined Citadel** on the Moon. Its chests roll vanilla's `chests/simple_dungeon`
and `chests/end_city_treasure`, and `simple_dungeon` is mostly junk: 15 stacks a chest, of which about 7 are wheat,
seeds, bones, bread and rotten flesh (measured over 400 chests). Nothing about a planet changed what a chest held.

## What it does now

Every **chest** on the Moon, the Nether (Planet 3) and the End (Planet 4), from any mod and any structure, does two things:

1. **Adds a bonus** from two tables per planet. `ascension_worlds:planet/<planet>` is materials and treasure;
   `ascension_combat:planet/<planet>` is the build paths' components and our enchantment books.
2. **Takes the junk out.** The item tag `ascension_core:chest_junk` (wheat, seeds, beetroot, bread, bones, rotten flesh,
   poisonous potato, bowls) is removed from what the table rolled.

**Earth gets only a mild clean-up and no bonus** (Sanchit, 2026-10-22): seeds, rotten flesh and poisonous potatoes are taken out
(`ascension_core:chest_junk_mild`); wheat, bread and bones stay, as food and bone meal. Block drops, mob drops and fishing are
untouched; boss hearts are as they were. Nothing is replaced (resource-economy principle 5): the bonus is **added** and only the junk is taken away.

### The bonus, per chest

| | Materials and treasure (`ascension_worlds`) | Components and books (`ascension_combat`) |
|---|---|---|
| **The Moon** | 1 to 2 of: raw titanium, raw helium, gatestones, diamonds, emeralds, gold, iron, redstone, leather, amethyst, experience bottles, golden apples, ender pearls, echo shards. Three chests in ten also hold a **find**: an enchanted diamond tool or armour piece, an enchanted book, an enchanted golden apple, a Totem. | 45%: one build-path component (basic ones mostly; the eight Moon epics at weight 1 each, about 6% of chests). 35%: one of our five enchantment books. 5%: Soulbound. |
| **The Nether** | 1 to 2 of: netherite scrap, ancient debris, gold, diamonds, blaze rods, ghast tears, magma cream, nether wart, glowstone, quartz, crying obsidian, experience bottles, golden apples, raw titanium, raw helium. A **find** 30%: enchanted diamond gear, a netherite ingot, a wither skeleton skull, an enchanted book, an enchanted golden apple, a Totem. | 45%: a component (basics rarer; Moon epics and the nine Nether epics). 40% a book, 8% Soulbound. |
| **The End** | 1 to 2 of: diamonds, emeralds, gold, ender pearls, shulker shells, netherite scrap, dragon's breath, many experience bottles, golden apples, raw titanium, raw helium. A **find** 35%: enchanted netherite or diamond gear, a netherite ingot, a stronger enchanted book, enchanted golden apples, Totems. | 50%: an epic (no basics). 50% a book, 12% Soulbound. |

The enchant levels of the gear and books rise with the planet (Moon 20 to 30, Nether 24 to 32, End 28 to 39).

### What that comes to (400 chests each, the Citadel's tables)

Per 100 chests. Earth's `simple_dungeon` is the baseline: 15.1 stacks with 691 junk before the mild clean-up, 11.4 stacks and 320 junk after
(wheat, bread and bones stay), and no titanium, helium, diamonds or components either way.

| | stacks a chest | junk | raw titanium | raw helium | diamonds | experience bottles | netherite | enchanted books | components |
|---|---|---|---|---|---|---|---|---|---|
| Moon, `simple_dungeon` | 13.3 | 0 | 48 | 54 | 26 | 51 | 0 | 66 | 42 |
| Moon, `end_city_treasure` | 12.2 | 0 | 70 | 55 | 75 | 46 | 0 | 48 | 41 |
| Nether, `simple_dungeon` | 13.3 | 0 | 24 | 21 | 21 | 39 | 19 | 65 | 46 |
| End, `end_city_treasure` | 12.6 | 0 | 36 | 38 | 101 | 89 | 27 | 72 | 51 |

A Citadel chest therefore holds about 13 useful stacks where Earth's dungeon chest held about 8 once its junk is counted out,
and the planets rise: the End's chests hold more diamonds (101 against 75 a hundred chests) and nearly twice the experience
bottles of the Moon's.

## How it works (and why Lootr is untouched)

- **`ascension_core:chest_table`** is a loot condition: true when the table being rolled has a path under `chests/`, which
  is where vanilla, Cataclysm and our own keep theirs. It exists because the game cannot say "every chest table" in data.
  **`ascension_core:strip`** is a loot modifier that removes an item tag from what was rolled. Both live in `core`
  (ADR-0029) and do nothing until a loot file names them.
- Each planet's bonus is a plain `neoforge:add_table` modifier with those two conditions plus a `minecraft:location_check`
  on the dimension. **No chest table is named anywhere**, so a table another mod adds tomorrow is covered.
- **Lootr** fills its chests with `LootTable.fill`, the call the game's own chests use, and passes the chest's position
  and the opening player. Its bytecode was read and the check fills tables that way, so a Lootr chest gives each player
  their own rolled bonus. A chest that uses no loot table is not Lootr's and is not ours.
- The numbers live in one table in `tools/generate-planet-loot.py`; run it to rewrite the 13 JSON files, or with `--check`
  to see whether they match. The resource ledger lists these chests as the source of every component and material in them.

## Changing it

| To | Edit |
|---|---|
| Make a planet richer or poorer | the weights and the `chance` of its pools in `generate-planet-loot.py`, then run it |
| Add a planet | a block in `WORLDS` and `COMBAT`, its dimension in `PLANETS`; no Java |
| Add or drop a junk item | `data/ascension_core/tags/item/chest_junk.json` |
| Give Earth a bonus | add an `add_table` modifier with the Overworld's dimension (not done: Earth only loses its mild junk) |
| Change what Earth loses | `data/ascension_core/tags/item/chest_junk_mild.json` (the planets' tag includes it) |
| Make one structure special | a modifier with `neoforge:loot_table_id` for its table, as the Archive's enchantments do |

## The Heavy Core (the mace's head), 2026-10-25

Not a planet bonus. The alpha.2 test found the Heavy Core "very hard to get": vanilla gives it only from an ominous vault (the
Trial Chambers' second tier, behind an ominous key), as 1 of 10 shares in a pool rolled 75% of the time, **7.5% a vault**. The modifier
`ascension_combat:trial_heavy_core` adds a separate 5% for a Heavy Core when the table `chests/trial_chambers/reward_ominous` is
rolled, about **12.2% a vault** (measured 12.26% over 40,000 rolls by `loot-checks`). Normal vaults are unchanged. To change the rate,
edit `chance` in `data/ascension_combat/loot_table/chests/trial_heavy_core.json`.

## Open

- **Components in chests are a choice.** They let a lucky chest skip a crafting step (a Moon epic is the work of two
  basics and three raw helium). Set the component weights to zero in `COMBAT` (keeping the books) if Sanchit would rather
  the build paths stay craft-only. Legendary items are never in chests, and a boss's heart only from the boss.
- **Not touched, by choice:** mob and boss drops, fishing, block drops, Earth's bonus (it has none), and Cataclysm's own tables (the
  Citadel's rewards come from the planet bonus, not from changing Cataclysm). Boss kills could pay more (diamonds, a book);
  ask before doing it.
- **Not seen in a game.** Seen on a dedicated server with a stand-in player and a placed chest. A client check is owed: open
  a Citadel chest on the Moon and read the tooltips.
- **Lootr removal.** Removing Lootr later deletes every converted chest (`docs/technical/pack-extras.md`). This page does not
  depend on it: the bonus applies to plain chests too.
- **Just Enough Resources** does not show modifier-added loot; the ledger and this page are where it is written down.
