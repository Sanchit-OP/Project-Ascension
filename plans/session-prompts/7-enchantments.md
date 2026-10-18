# Session prompt 7: enchantments through the stat system

Paste everything below the line into a new Claude Code session opened on this repository.

---

You are joining Project-Ascension: a Minecraft 1.21.1 NeoForge campaign built from our own mods,
by Sanchit (player name Devil0701). Your job is **how enchantments work in this game**: which of
vanilla's stay, how each one meets our stat system, which new ones we add, and where a player gets
them. Sanchit's idea is to build them *on* the stat system, so an enchantment is one more source of
stats like gear, runes and growth, and everything composes.

## Sanchit's words (2026-10-18)

> other thing we need to work on is how the enchantments are gonna work. I feel like maybe we can
> implement the enchantment with our stat system and it will make it all pretty cool.

Earlier (2026-10-12) he asked that balancing and "how the enchantments interact with" the items
we added be thought through. `docs/progression/enchantments.md` is only a list of open questions;
you write the real design.

## Read first

- `CLAUDE.md`, then `docs/decisions/`. These bind you:
  - **0003:** no Tier-1 module hard-depends on a third-party mod; mixins never in Tier 1; other
    mods' enchantments are handled in a Tier-2 `ascension-compat-*` jar or by data that loads only
    when the mod is present.
  - **0007:** performance. Nothing per-tick that scans, nothing static holding a Level or Entity.
  - **0008:** seen in a running client; world state on a dedicated server; anything saved, across a
    reload; a refactor pass every third increment.
  - **0018:** no world is ever required, so a good enchantment must not be locked behind a planet
    only; planets and bosses may *reward* with enchantments.
  - **0019:** item stats are data. Read it, and `docs/technical/item-stats.md` end to end. It is
    the model you plug into: `com.ascension.combat.api.stat.StatReader` reads a stat's total,
    `StatSources` names where a bonus came from (the character sheet shows it), and gear, runes
    and growth each add attribute modifiers under their own id prefix. There is **no base/bonus
    split**: a stat is one total.
- `docs/technical/combat-api.md` §3 (the stats, the defence curve: Armor 0 to 300, every 100 halves
  damage) and §3a (mobs).
- `docs/progression/enchantments.md` (the open questions), `docs/progression/player-power.md`,
  `docs/progression/resource-economy.md` §5 ("Balance and enchantments"), and the rune and growth
  docs, because levels and enchanting touch both (see "XP is a currency" below).
- `docs/lore/what-the-warden-keeps.md` ("Why does enchanting cost experience, and what are the
  symbols floating into the table?") and `the-nether.md`: **enchanting is the Observers' script.**
  The glyphs are their language, experience is the price, and every enchantment is noticed. A
  design that ignores this wastes the lore; one that uses it gets reasons for rules (why an
  enchantment is rare, why a boss gives one).

## What is true today

- **Enchantments in 1.21.1 are data**, a datapack registry (`data/<ns>/enchantment/<name>.json`),
  not code. The 42 vanilla ones show the format; read them in the Minecraft jar
  (`~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar`, under
  `data/minecraft/enchantment/`). Each has `effects` made of **effect components**. The ones in use:
  `minecraft:attributes` (respiration, efficiency, depth strider, sweeping edge, swift sneak,
  blast and fire protection, aqua affinity), `minecraft:damage_protection`, `minecraft:damage`
  (sharpness, smite, bane, power, impaling), `minecraft:armor_effectiveness` (breach), `knockback`,
  `post_attack`, `location_changed`, `tick`, `item_damage`, `projectile_*`, `repair_with_xp`,
  `smash_damage_per_fallen_block` and more. **`minecraft:attributes` and the `minecraft:attribute`
  effect type add an attribute modifier by level**, which is exactly a stat source.
  Also tags: `exclusive_set/*`, `in_enchanting_table`, `tradeable`, `treasure`, `on_random_loot`,
  `curse`, and `supported_items` / `primary_items` item tags.
- **Our stats are attributes.** Health, Attack Damage, Attack Speed and Move Speed are vanilla
  attributes; Armor, Magic Resist, Omnivamp, Lethality and Penetration % are ours.
  `PhysicalDamage` (`modules/combat/.../internal/mechanics/PhysicalDamage.java`) **replaces vanilla's
  armour reduction** with our Armor curve. It does not touch the *enchantment* reduction
  (`damage_protection`), so Protection and friends still apply on top, as vanilla's own separate
  step. Whether that is right for us is one of your questions.
- **The 26 combat items** (`CombatContent.java`) have mechanics of their own (passives, actives).
  Which vanilla enchantments they accept is decided by item tags and `supported_items`, not yet
  thought through. Some of our weapons are Better Combat weapons (its own attack animations and
  sweeps).
- **Respiration:** the atmosphere module replaced vanilla's air bubbles. Its rule is that gear grows
  *lung capacity* rather than reducing drain, "so enchantments cannot silently extend a tank"
  (`docs/technical/atmosphere-api.md`). What Respiration and Aqua Affinity should mean on a planet
  is yours to settle with that rule.
- **XP is a currency.** Levels pay for enchanting and anvils, a rune change (3 levels) and a
  skill-tree respec (20 levels). The **Scholar** rune lowers enchanting cost by a level. Growth
  deliberately does not use XP (it is farmable). Any new enchanting cost has to make sense beside
  those.
- **Sources today:** the enchanting table, books from villagers and loot, the anvil. Nothing of ours.
  The economy ledger (`docs/progression/resource-ledger.md`) shows where every material comes from.

## The job

1. **Audit what is true now.** In a doc, for **each of the 42 vanilla enchantments**, say what it
   does today in this game with the combat module loaded, and whether that is good: Does Protection
   still matter against a defence curve where Armor 74 already cuts damage? Is Sharpness's flat
   +3 meaningful when Attack Damage is scaled to Minecraft's size, and what does it do to an item
   that scales off its wielder's Attack Damage? Do Efficiency and the mining enchantments need
   anything? Is Mending still wanted when an item is a long build path? Test the doubtful ones in
   a running game (`/enchant`, then a measured hit), do not reason from the code.
2. **Research,** in the same doc: how other games and mods treat enchantments and affixes (Path of
   Exile and Diablo affixes, Minecraft Dungeons, Terraria reforges and modifiers, Apotheosis,
   Enigmatic Legacy, Tetra, League's enchants and Ornn upgrades); what makes a modifier feel like a
   build choice instead of a number. End with what we **can** add and what we **should** add.
3. **Design** in `docs/progression/enchantments.md` (replace the open-question stub), with an ADR for
   the structural choice. Cover:
   - **Policy per vanilla enchantment:** keep, rework, or remove, each with its reason, for example
     "Protection becomes +Armor", "Sharpness scales with the wielder's Attack Damage". The existing
     design rules still hold: enchantments must support build identity, must not trivialise hazards
     (oxygen, the rot), and utility is preferred over flat numeric inflation.
   - **Which stats an enchantment may give, and how much,** sized against gear, runes and growth
     (`item-stats.md` §3 and `growth.md` §8 have the numbers), so that a full enchant set is a real
     part of power and not the whole of it. Ask the stats owner for a new stat through Sanchit
     rather than adding an attribute yourself.
   - **Stat sources.** Register an id prefix with `StatSources` ("Enchantments") so the sheet and
     the future HUD stat panel can name what an enchantment gave. Keep the modifier ids stable.
   - **New enchantments of ours,** as data: ideas that fit stats (a Lethality or Omnivamp piece,
     Magic Resist for casters), the oxygen world (lung capacity, tank efficiency), and the planets;
     and ones that fit the lore (the script, the Observers noticing).
   - **Where they come from.** The table, and also boss drops, structure loot, a crafted route on
     each planet, and the Nether tablets. Which are **planet- or boss-rewarded**
     (ADR-0018: rewards, never requirements). Enchanted books as loot go through
     `minecraft:enchant_with_levels` / `enchant_randomly` functions and the `on_random_loot` tag.
   - **Cost.** XP levels, lapis, or something of ours; and how it sits with the rune and respec
     costs, and Scholar.
   - **Our 26 items:** what each accepts and why (tags), and how enchantments meet an item's own
     passive (does a Sharpness sword still feel like its build?).
   - **Tooltips.** Show an enchantment's stat the way the stat tooltips do (League-style lines,
     `client/stat/StatTooltips.java`); vanilla's enchantment lines read badly next to ours.
   - **Other mods' enchantments** (Cataclysm adds some; check with the mod present) by compat data
     or a Tier-2 jar.
4. **Ask Sanchit the decisions,** each as a plain question with your recommendation first.
5. **Build in increments,** each seen in a client: the stat-source plumbing and the vanilla
   reworks first (data only if you can), then new enchantments, then the sources, then tooltips and
   JEI. A refactor pass every third increment.
6. **Prove it, ADR-0008 style.** `client/dev/IntegrationChecks` plays the merged combat systems
   together in a real client and prints PASS and FAIL lines
   (`docs/technical/dev-environment.md` explains how to run it). Add checks there: an enchanted
   piece shows its stat on the sheet, under the right source, and a measured hit through the
   defence curve matches; a reload keeps it; a `/reload` that retunes a level reaches worn gear
   (see `WornGear` in `internal/stat/`). On a dedicated server too, because enchantments are
   registry data the client must receive.

## Working beside other sessions

- **The HUD overhaul (prompt 6)** is a separate session that will show stats (including from
  enchantments) in a side panel. Keep the source names readable.
- **Build paths (prompt 4)** may start later and add items; leave room for items with no
  vanilla enchantability.
- **The logic session** (the one that wrote this) is adding quality-of-life and team/quest mods to
  the pack. If an added mod brings enchantments, tell Sanchit.
- **Work in your own git worktree and branch** (for example `combat-enchantments`). If the app did
  not make one, run `git worktree add`. Merge into `main` only when Sanchit says.
- **Keep edits to shared files small:** `AscensionCombat.java`, the lang file, `CombatContent.java`.
- **Only one dev server can run at a time** (ports 25565 and 25575). Check before
  `./gradlew runDevServer`, and never stop another session's.
- **A trap this project has already hit:** Minecraft ignores a data field it does not know, so a
  misspelt key makes a condition empty and always true, with nothing in the log.
  `tools/check-data.py` (part of every build) catches the mistakes we have found so far; when you
  meet a new kind in enchantment JSON, add a rule.

## How Sanchit works

- **Commit each step,** ending the message with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
  **Never push** unless he asks.
- **Original art only** (`docs/technical/art-guide.md`).
- **Plain language.** Keep docs handoff-ready, send docs to his phone (`SendUserFile`), and when he
  must choose, ask a clear question with your recommendation first.
- Every build runs `tools/resource-ledger.py --check` and `tools/check-data.py` (Python on PATH).
- This shell's guard may reject complex commands: write Python scripts to your scratchpad and run
  them, or use the file tools.

## Questions to put to Sanchit (do not decide these yourself)

- Which vanilla enchantments to keep, rework or remove.
- Whether enchantments become stat sources in general (the idea), and how large they may be.
- Whether any enchantments are locked to a boss or a planet as rewards.
- The cost: XP levels as now, or something of ours.
