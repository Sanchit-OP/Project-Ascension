# Item stats: one system for every item

> **Design, 2026-10-15.** Not built yet. The decisions waiting on Sanchit are in §11. The
> structural choice is [ADR-0019](../decisions/0019-item-stats-are-data.md), proposed until he
> answers.
>
> Sanchit, 2026-10-15: *"we cant really only have our items have stats … either it comes from diff
> mod or not doesnt matter i need a unified system."*

**In one paragraph:** every item's stats come from one data file, keyed by item or by tag, for
vanilla's items, ours and other mods' alike. Any armour from any mod counts as our Armor through a
conversion rate, with no file needed. Stats are read through one API, which splits base from
bonus. Nothing ticks, and nothing needs a mixin.

## 1. What NeoForge 1.21.1 gives us

Checked against the NeoForge 21.1.249 source this project builds with, not against docs.

### What works

- **Data maps** (`DataMapType`, `RegisterDataMapTypesEvent`). These attach a value to any registry
  entry from a datapack file.
  - Entries can name an item (`minecraft:iron_sword`) or a tag (`#minecraft:swords`).
  - A modpack can override or remove entries with its own datapack.
  - Each entry can carry `neoforge:conditions`, for example "only if Cataclysm is loaded".
  - They can be **synced to clients**. Tooltips need that, because the client draws them.
  - They work on any registry. Items, entity types and attributes all get one here.
  - Reading one is a single lookup on the item's holder.
- **`ItemAttributeModifierEvent`** fires whenever any item stack's modifiers are asked for, from
  any mod. Adding a modifier there gives that item the stat. It fires in three places:
  - on the server, **only when a slot's item changes**. Vanilla then adds and removes the
    modifiers itself. There is no per-tick cost, and it already covers mobs wearing gear.
  - on the client, when a tooltip is drawn.
  - when mobs compare weapons to pick one up.
- **Tooltips.** NeoForge already replaces vanilla's attribute tooltip code (`AttributeUtil`).
  - An attribute can choose how it prints its value. NeoForge's `PercentageAttribute` prints
    "+8%".
  - `GatherSkippedAttributeTooltipsEvent` hides chosen lines.
  - `ItemTooltipEvent` can add our own lines anywhere in the tooltip, including the top.
- **Modifier ids tell you where a stat came from.** An attribute keeps each modifier under its own
  id. NeoForge marks a weapon's own damage and speed as "base" modifiers, the green tooltip
  numbers: `minecraft:base_attack_damage` and `minecraft:base_attack_speed`. Base versus bonus
  (§6) builds on that.

### What doesn't, and the traps

1. **Data maps merge in load order.** A tag entry in one mod's file can overwrite another mod's
   entry for a single item. Which file loads first depends on mod order, so this is a real bug
   waiting to happen: our `#minecraft:swords` baseline would beat the Cataclysm jar's numbers for
   its own swords, which Cataclysm puts in `minecraft:swords`. **Fix:** our own merge rule. An
   entry naming the item beats a tag entry, one stat at a time, whatever the order.
2. **An entry for an item that doesn't exist logs an error.** So other mods' items need a
   `neoforge:conditions` mod check per entry, or a place in that mod's compat jar.
   - Conditions only work **per entry**. A condition at the top of the file is silently ignored.
   - A missing tag is quietly empty, so tags are always safe.
3. **Every pack writes to the same file path.** The data map's id decides it:
   `data/ascension_combat/data_maps/item/item_stats.json`. Our jar, each compat jar and a
   modpack's datapack all ship a file at that path, and they stack.
4. **Vanilla never syncs Attack Damage to the client.** Its attribute is not marked syncable, and
   equipment modifiers are only applied on the server.
   - The character sheet's Attack Damage line reads the client's own copy, so **it shows 1 today,
     whatever you hold**. This is an existing bug.
   - **Fix:** mark the attribute syncable once at startup, with its public `setSyncable`, on both
     sides. The fallback is a small payload of our own.
5. **Worn items keep their old numbers after `/reload`.** Vanilla only re-reads an item's
   modifiers when the slot changes. **Fix:** re-apply worn gear when the data reloads.
6. **Modifier ids must be unique and stable per slot.** Otherwise a helmet's and a chestplate's
   Armor overwrite each other, or unequipping fails to remove a stat. Our ids name the slot:
   `ascension_combat:item/chest/armor`.
7. **`EntityAttributeModificationEvent`, which `MobStats` uses today, runs once at startup.**
   That is before any datapack loads, so it can never read data. Mob stats from data have to be
   applied when the mob enters a world (§9).
8. **Vanilla's Armor attribute caps at 30.** This is why combat has its own Armor attribute. It
   is unchanged: vanilla armour feeds ours by conversion, it does not replace it.

## 2. The shape: four parts

| Part | What it is | Where |
|---|---|---|
| **Stat definitions** | The ten stats, their short names in data, how each is written | Code, `combat.api.stat` |
| **Item stats** | Per item or per tag: a weapon's own numbers and bonus stats | Data map on items |
| **Conversions** | "Each vanilla armour point is worth N of our Armor", for every mod's armour at once | Data map on attributes |
| **Mob stats** | Per entity type or per tier tag | Data map on entity types |

One code path applies item stats: an `ItemAttributeModifierEvent` listener that looks up the item
and adds modifiers. One API reads stats: `StatReader` (§6).

## 3. The stats

These are the stat definitions this session owns. Other sessions ask for new ones (see
`combat-api.md` §3, "Requested stats").

| Name in data | Stat | Attribute | Written as | Applied as |
|---|---|---|---|---|
| `health` | Health | vanilla max health | `4` = +4 health (2 hearts) | flat |
| `attack_damage` | Attack Damage | vanilla attack damage | `5` = +5 | flat |
| `attack_speed` | Attack Speed | vanilla attack speed | `20` = +20% | % of base, summed |
| `move_speed` | Move Speed | vanilla movement speed | `10` = +10% | % of base, summed |
| `armor` | Armor | combat's own | `40` = +40 | flat |
| `magic_resist` | Magic Resist | combat's own | `40` = +40 | flat |
| `omnivamp` | Omnivamp | combat's own | `8` = 8% | flat |
| `lethality` | Lethality | combat's own | `10` = 10 Armor ignored | flat |
| `armor_penetration` | Armor Penetration | combat's own | `15` = 15% | flat |
| `magic_penetration` | Magic Penetration | combat's own | `15` = 15% | flat |

- **Percent stats are written as you say them:** `8` means 8%, not 0.08.
- **Attack Speed and Move Speed bonuses add up first, then scale the base.** Two +20% bonuses make
  +40%, as in League. Today Berserker's Greaves multiplies instead. The move to data fixes that.

## 4. The item data

### The file

`data/ascension_combat/data_maps/item/item_stats.json`:

```json
{
  "values": {
    "#minecraft:axes": { "stats": { "lethality": 3 } },
    "minecraft:netherite_axe": { "weapon": { "damage": 11 } },
    "minecraft:shield": { "slot": "offhand", "stats": { "armor": 10 } },
    "ascension_combat:darius_axe": {
      "weapon": { "damage": 8, "speed": 1.0 },
      "stats": { "attack_damage": 4, "health": 4, "armor_penetration": 15 }
    }
  }
}
```

(The axe and shield lines show the format. They are not proposed numbers.)

**An entry has three optional parts:**

- **`weapon`**: the weapon's own numbers, written as the tooltip shows them.
  - `damage`: a diamond sword is 7.
  - `speed`: attacks per second; a sword is 1.6.
  - These replace the item's own numbers and count as **base** (§6).
- **`stats`**: any stat from §3, by name. These count as **bonus**.
- **`slot`**: where the stats count. One of `mainhand`, `offhand`, `hand`, `head`, `chest`, `legs`,
  `feet`, `armor` or `any`. Without it, armour counts in its armour slot and everything else in the
  main hand.

### The rules

1. **An item entry beats a tag entry, one stat at a time**, whichever file loaded first. Between
   two entries of the same kind, the later pack wins, as with any datapack.
2. **A stat the data names replaces what the item would otherwise give for it**: vanilla's own
   number, or the conversion (§5). A stat it doesn't name stays as it was.
3. `"replace": true` on an item's entry throws away what its tags gave it.
4. NeoForge's own `"remove": [...]` takes entries out.
5. An entry for another mod's item carries a mod check, or lives in that mod's compat jar (§8):

```json
"cataclysm:ignitium_chestplate": {
  "neoforge:conditions": [{ "type": "neoforge:mod_loaded", "modid": "cataclysm" }],
  "stats": { "magic_resist": 20 }
}
```

### What a player sees when they edit it

`/reload` re-reads the file. Worn gear takes the new numbers at once (trap 5 in §1).

## 5. Vanilla armour counts again

`data/ascension_combat/data_maps/attribute/stat_conversions.json`:

```json
{
  "values": {
    "minecraft:generic.armor": { "stat": "armor", "per_point": 5 },
    "minecraft:generic.armor_toughness": { "stat": "armor", "per_point": 5 }
  }
}
```

**Any item, from any mod, whose own modifiers give vanilla armour now gives our Armor at this rate.**
No file per item is needed: Cataclysm's armour, Create's diving gear and horse armour all count
automatically. An item whose data names `armor` skips the conversion (rule 2).

- **Vanilla's own Armor and Toughness lines are hidden from the tooltip**, so a chestplate never
  shows two different "Armor" numbers.
- Vanilla's armour bar above the hotbar still shows. It still rises with better armour.
- Protection enchantments keep working as they do today. They were never part of this.

**Proposed rate:** 5 Armor for every armour point and every toughness point. Every 100 Armor
halves physical damage (`combat-api.md` §3).

| Full set | Armour points | Toughness | Our Armor | Physical damage taken |
|---|---|---|---|---|
| Leather | 7 | 0 | 35 | 78% |
| Gold | 11 | 0 | 55 | 68% |
| Chainmail | 12 | 0 | 60 | 66% |
| Iron | 15 | 0 | 75 | 59% |
| Diamond | 20 | 8 | 140 | 38% |
| Netherite | 20 | 12 | 160 | 33% |

- **Toughness has to count for something.** Without it, netherite and diamond are equal, since
  both are 20 armour points.
- **Toughness goes to Armor, not Magic Resist.** Magic Resist only reduces combat's weapon procs.
  On a player it would only matter against another player.
- **For comparison:** vanilla's diamond set stops 60–80% of a hit; this stops 62%. A boss's
  100 Armor sits between iron and diamond. One number in one file changes it all.
- **Mobs wearing armour gain Armor the same way**, as they gain protection from it in vanilla. A
  zombie in an iron chestplate gets +30.

## 6. Base versus bonus, and the one way to read a stat

League items often scale off *bonus* stats: "deal 50% of your bonus Attack Damage". So every stat
splits in two.

**Base** is what the player is:
- the entity's own value: a player's 20 health and 1 Attack Damage, and 0 Armor;
- **the held weapon's own damage and speed**, the `weapon` part of its data. These are vanilla's
  green tooltip numbers: a weapon is your attack in Minecraft, as a champion's base attack is in
  League;
- growth, which the growth session adds;
- a mob's stats from mob data (§9).

**Bonus** is what the build adds:
- items' `stats`, and the converted vanilla armour;
- runes;
- potions, and anything else.

**Total = base + bonus.** With percentage modifiers, bonus is defined as total minus base, the same
as League's.

### The API, `com.ascension.combat.api.stat`

```java
// One stat: its name in data, its attribute, and whether it is a percentage.
public final class CombatStat { ... }

// The ten stats in section 3, and a lookup by name.
public final class CombatStats {
    public static final CombatStat HEALTH, ATTACK_DAMAGE, ATTACK_SPEED, MOVE_SPEED,
            ARMOR, MAGIC_RESIST, OMNIVAMP, LETHALITY, ARMOR_PENETRATION, MAGIC_PENETRATION;
    public static Optional<CombatStat> byName(String name);
    public static List<CombatStat> all();
}

// The one place stats are read, on either side.
public final class StatReader {
    public static double total(LivingEntity entity, CombatStat stat);
    public static double base(LivingEntity entity, CombatStat stat);
    public static double bonus(LivingEntity entity, CombatStat stat);
    public static StatBreakdown breakdown(LivingEntity entity, CombatStat stat); // per source
}

public enum StatLayer { BASE, BONUS }

// Lets another system say what its modifiers are. Unregistered modifiers count as bonus.
public final class StatSources {
    public static void register(ResourceLocation modifierIdPrefix, StatLayer layer, Component label);
}
```

**The contract for the rune and growth sessions:**
- **Give a stat** by adding an attribute modifier to the stat's attribute (`CombatStat#attribute`).
  Its id must start with a prefix you registered with `StatSources`. Growth registers as `BASE`,
  runes as `BONUS`.
- **Read stats only through `StatReader`.** Never read an attribute directly for gameplay.
- **Ask for a new stat** in `combat-api.md` §3, "Requested stats". Don't register attributes
  yourself.

`breakdown` names each source: every worn or held item by its name, each registered source by its
label, and "Other" for the rest. That is what the character sheet shows (§7).

## 7. Tooltips and the character sheet

### Tooltips: League's stat block

Every item with stats gets a block of stat lines **at the top of its tooltip, under the name**, as
League's item cards have them:

```
Darius's Axe
8 Attack Damage          <- the weapon's own, white
1.0 Attack Speed
+4 Attack Damage         <- bonus, coloured by stat
+4 Health
+15% Armor Penetration
When held
```

- **One colour per stat, the same on the character sheet:** Attack Damage orange, Attack Speed
  yellow, Health green, Armor gold, Magic Resist blue, Omnivamp red, Lethality and Armor
  Penetration dark orange, Magic Penetration violet, Move Speed white.
- A small grey line says where the stats count: "When held", "When worn" or "In off hand".
- **Vanilla's "When in Main Hand: 7 Attack Damage" block is replaced** by this for every item that
  has stats, vanilla's and other mods' included.
- Attributes we don't know, such as another mod's reach or knockback resistance, still print below
  in vanilla's way.

### The character sheet

Each row gains its sources.

- **Hover a stat to see where it comes from.** For example: "Armor 145: Netherite set 130 ·
  Growth 15".
- **Stats a League item can scale from show base + bonus**, as League's own stat panel does:
  "Attack Damage 14 (8 + 6)".
- This fixes the sheet's Attack Damage line, which today shows 1 whatever you hold (trap 4 in §1).

## 8. Other mods' items

**Most of it is automatic:**
- **Armour:** the conversion covers it.
- **Weapons:** a weapon's damage is vanilla's Attack Damage, which already is the stat.

Cataclysm's Ignitium set, its swords and Create's netherite diving gear all count on the first day,
with no file.

**Data only adds League-style extras.** For example, Ignitium armour could give Magic Resist, or the
Soul Render could give Omnivamp. Those entries live in the mod's Tier 2 compat jar, under the same
file path:
- **`ascension-compat-cataclysm`:** Cataclysm's weapons, armour and boss tiers (§9). The jar
  already exists.
- **`ascension-compat-create`:** only if Create's tools ever need anything.

A mod with no compat jar gets entries with a mod check in our own file, or in a modpack's datapack.
Tier 1 code never names another mod (ADR-0003).

## 9. Mobs in the same data

**Proposed:** `MobStats`' hard-coded tiers become data.

- **The tiers become entity tags:** `ascension_combat:tier/swarm`, `light`, `medium`, `heavy` and
  `boss`. NeoForge's `#c:bosses` is included in `boss`, so any mod's boss that tags itself gets boss
  stats.
- **The numbers become a data map**, `data/ascension_combat/data_maps/entity_type/mob_stats.json`:

```json
{
  "values": {
    "#ascension_combat:tier/light": { "armor": 10 },
    "#ascension_combat:tier/boss": { "armor": 100, "magic_resist": 40 },
    "minecraft:warden": { "armor": 120 }
  }
}
```

- **Applied when a mob enters a world**, as one temporary modifier. It is never saved with the mob,
  so the data is always the truth, and `/reload` reaches mobs already loaded.
- **Cataclysm's boss numbers**, today repeated in `CataclysmBossStats`, move into the compat jar's
  data, mostly as tag entries. That class goes.
- **Same stat names as items.** HP and Attack Damage could be tuned per mob later, but nothing
  changes them now.

## 10. Performance and the rules

- **No per-tick work.** The item listener runs when a slot changes, a tooltip is drawn, or data
  reloads. Mob stats apply once per mob as it enters a world.
  - Today eight per-tick listeners re-apply fixed stats: seven boot rules and Gargoyle Stoneplate's
    passive. They go when our items move to data.
  - Effects that really change every tick stay as they are: Sterak's HP-to-damage conversion,
    Endless Hunger, the Protean Mask's stacks.
- **No caches and no static collections of entities.** The data lives in NeoForge's registries,
  which clear and refill it on reload.
- **No mixins and no third-party names in Tier 1.** The mixins rule and the compat rule both hold
  (ADR-0003).
- **Data over code.** A new mod's item, a retuned rate or a new mob is a JSON edit (ADR-0007
  rule 10).

## 11. Decisions for Sanchit

Asked 2026-10-15. Each has a recommendation first.

1. **Vanilla armour's rate.** 5 Armor for every armour and toughness point, so a diamond set is
   140 Armor (recommended)? Or toughness becomes Magic Resist instead? Or vanilla armour stays
   cosmetic?
2. **Base versus bonus.** A held weapon's own damage counts as base, and everything else as bonus
   (recommended)? Or all of an item's Attack Damage is bonus, leaving only the player's own 1 as
   base?
3. **Tooltips.** League's stat block at the top, replacing vanilla's "When in Main Hand" lines
   (recommended)? Or keep vanilla's lines and only format ours?
4. **Mobs.** Move mob stats into the same data, with tiers as tags (recommended)? Or leave
   `MobStats` in code?

**Asked later, when the work reaches them:**

5. **Our 26 items' starting numbers.** Proposed: League's own numbers, scaled.
   - Armor, Magic Resist and every percentage stay as League has them. Our curve equals League's
     at 100: both halve damage there.
   - Attack Damage is divided by 10, and Health by 100.
   - Then you tune by playing, as always.
6. **Extras for vanilla weapons**, for example Lethality on axes. Optional, and one line of data
   each.

## 12. Increments

ADR-0008: each increment is done once it is seen in your client and checked on a dedicated server.
A refactor pass comes every third increment.

| # | Increment | Seen working when |
|---|---|---|
| 1 | **Vanilla armour counts.** The stat definitions, the item and conversion data maps (synced), the item listener, the item-beats-tag merge (unit-tested), and refreshing worn gear on `/reload` | In iron armour the sheet shows 75 Armor and a zombie's hit lands softer. On a dedicated server the numbers survive a reconnect. After `/reload` with a new rate, worn armour changes |
| 2 | **The reading API, and the sheet's sources.** `StatReader`, `StatSources`, Attack Damage synced | The sheet shows a diamond sword's 7 Attack Damage and lists each stat's sources |
| 3 | **Refactor pass** | |
| 4 | **League-style tooltips** | A diamond chestplate shows "+50 Armor" once. A modded sword shows its stat block |
| 5 | **Our 26 items into data.** The per-tick boot rules go, and our armour stops borrowing diamond's numbers | Each item's tooltip and the sheet agree with its data line. Ninja Tabi gives 15 Armor exactly once |
| 6 | **Refactor pass** | |
| 7 | **Mobs into data**, Cataclysm's bosses included | Jade shows the same Armor and Magic Resist as before, from data. A tag edit plus `/reload` changes a live mob |
| 8 | **Other mods' extras.** Cataclysm entries in its compat jar | Ignitium armour shows its extras with Cataclysm installed, and nothing logs an error without it |
| 9 | **Refactor pass** | |

**The rune and growth sessions can start reading stats after increment 2.** The API's shape (§6)
is fixed now, so they can write against it before then.
