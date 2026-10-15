# ADR-0019: Item stats are data, for every item from every mod

- **Status:** Accepted. Sanchit answered the design's four decisions on 2026-10-15
  (`item-stats.md` §11).
- **Date:** 2026-10-15
- **Amends:** [ADR-0012](0012-ascension-combat-module.md), which left how items carry stats open.

## Context

Sanchit, 2026-10-15: every item that should carry combat stats must have them, vanilla's, ours
and other mods' alike, through one unified system.

Today three things are wrong:

- **Only our 26 items can carry combat stats**, and each does it in its own Java.
  - Seven fixed boot stats and Gargoyle Stoneplate's are re-applied **every tick** by listeners.
  - The weapons borrow `Tiers.DIAMOND`, and the armour `ArmorMaterials.DIAMOND`.
- **Vanilla armour protects nothing.** `PhysicalDamage` replaces vanilla's armour maths with
  combat's own Armor stat, and nothing feeds that stat from vanilla armour. A netherite set counts
  for nothing (`player-power.md` §2).
- **There is no agreed way to read a stat.** League items scale off Attack Damage, and the rune and
  growth sessions, starting the same day, need one way to read stats that everyone shares.

ADR-0003 rules out mixins in Tier 1 and any Tier 1 dependency on a third-party mod. ADR-0007 rules
out per-tick work, and asks for data over code.

## Decision

**Item stats are a NeoForge data map, applied through `ItemAttributeModifierEvent`.** The design
is in `docs/technical/item-stats.md`.

- **The data map `ascension_combat:item_stats` keys stats by item or by item tag.** It is synced
  to clients, so tooltips can draw it.
  - An entry gives a weapon's own damage and speed.
  - It gives any stat by name.
  - It may name the slot the stats count in. A held item's stats count in the main hand.
- **Specific beats general, whatever the load order.** The data map uses our own merger: an
  entry naming the item beats a tag entry, one stat at a time. NeoForge's default merge is load
  order, which would let one mod's tag entry overwrite another mod's item entry.
- **A stat the data names replaces what the item would otherwise give for it.** A stat it
  doesn't name is left alone.
- **The data map `ascension_combat:stat_conversions`, keyed by attribute, turns vanilla
  attributes into our stats at a rate.** It covers every armour item from every mod with no
  per-item data. Sanchit's starting rates:
  - each vanilla armour point gives 3.5 Armor and 2.5 Magic Resist;
  - each toughness point gives 2.5 Health;
  - each item's result is rounded down;
  - netherite pieces name their own +10 Health. A diamond set then gives +20 and netherite +40.
- **One `ItemAttributeModifierEvent` listener applies both maps.** It adds modifiers with stable,
  slot-named ids. Vanilla adds and removes them when equipment changes, so nothing ticks.
- **Stats are read only through `com.ascension.combat.api.stat.StatReader`**, as one total each.
  It also gives a breakdown by source for the character sheet.
  - **There is no base/bonus split** (Sanchit, 2026-10-15). Items scale off total Attack Damage,
    and a bare hand is 1.
  - Other systems name their modifiers for the sheet through the `StatSources` registry.
- **Mob stats are a third data map, `ascension_combat:mob_stats`, keyed by entity type or tier
  tag.** It is applied as one unsaved modifier when a mob enters a world, and replaces `MobStats`'
  hard-coded tiers. The tiers become entity tags.
- **Vanilla's Attack Damage attribute is marked syncable at startup**, so clients see the real
  value. It is the one stat vanilla doesn't sync.
- **Other mods' data lives in their Tier 2 compat jars**, or in entries guarded by
  `neoforge:mod_loaded`. Tier 1 code never names another mod.

## Consequences

- **Vanilla armour counts again**, at a rate set in one file.
- **Other mods' armour and weapons count with no work.** Their armour converts; their weapons'
  damage already is vanilla's Attack Damage.
- **Eight per-tick listeners go** once our items move to data. Effects that really change every
  tick stay in code.
- **Every number in the stat model is a JSON edit**, and `/reload` applies it to worn gear and
  live mobs.
- **`StatReader` becomes a public, versioned contract** (ADR-0003 rule 3). The rune and growth
  sessions, and the build paths after them, depend on it.
- **First data maps in the project.** Data map files for a type all share one path,
  `data/ascension_combat/data_maps/<registry>/<name>.json`, and every pack's copy stacks.
- **Marking a vanilla attribute syncable is a global change.** It is harmless: a few bytes when an
  entity's Attack Damage changes, and only clients with this mod can connect anyway.

## Alternatives rejected

- **Per-tick modifiers for every item (today's `StatBoots` pattern).** It costs every player every
  tick, doesn't show in tooltips, and can't reach other mods' items without a rule per item.
- **Our own JSON reload listener instead of data maps.** We would have to rebuild what data maps
  give for free: tags, per-entry conditions, removal, and client sync. The only thing we would
  gain is merge order, and one merger class fixes that.
- **Stats stored on each item stack as a data component.** Every stack would need writing at
  craft, loot and trade time. It misses stacks that already exist, and can't be retuned.
- **Reading the target's vanilla Armor at damage time** instead of converting per item. It is
  simpler, but invisible in tooltips, can't say which item a stat came from, and silently converts
  mobs' natural armour too.
- **A mixin into `ArmorItem` or `ItemStack`.** Banned in Tier 1 (ADR-0003). No API path was
  missing.
- **A base/bonus split, as League has**, with a held weapon's own damage as base. Proposed, and
  turned down by Sanchit on 2026-10-15: two numbers per stat confuse more than they add. Items
  scale off the total.
- **5 Armor per armour and toughness point**, the proposed rate. Sanchit chose his own: Armor and
  Magic Resist from armour points, and Health from toughness.
- **Keeping mob stats in `EntityAttributeModificationEvent`.** That event runs once at startup,
  before datapacks load, so it can never read data.
