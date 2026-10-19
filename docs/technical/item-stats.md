# Item stats: one system for every item

> **Design, 2026-10-15. Sanchit answered the four decisions the same day** (§11). **Increments 1 to 7 are built**
> (2026-10-16) and Sanchit saw them working in his client on 2026-10-17; increments 8 and 9 are open (§12).
> The structural choice is [ADR-0019](../decisions/0019-item-stats-are-data.md).
>
> Sanchit, 2026-10-15: *"we cant really only have our items have stats … either it comes from diff
> mod or not doesnt matter i need a unified system."*

**In one paragraph:** every item's stats come from one data file, keyed by item or by tag, for
vanilla's items, ours and other mods' alike. Any armour from any mod gives Armor, Magic Resist and
Health through conversion rates, with no file needed. Stats are read through one API, as totals:
items scale off your total Attack Damage, never a base/bonus split. Nothing ticks, and nothing
needs a mixin.

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
  numbers: `minecraft:base_attack_damage` and `minecraft:base_attack_speed`. The character sheet's
  sources (§7) build on that.

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
| **Item stats** | Per item or per tag: a weapon's own attack and any added stats | Data map on items |
| **Conversions** | "Each vanilla armour point is worth N Armor and M Magic Resist", for every mod's armour at once | Data map on attributes |
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
| `attack_speed` | Attack Speed | vanilla attack speed | `20` = +20% | % of the weapon's speed, summed |
| `move_speed` | Move Speed | vanilla movement speed | `10` = +10% | % of walking speed, summed |
| `armor` | Armor | combat's own | `40` = +40 | flat |
| `magic_resist` | Magic Resist | combat's own | `40` = +40 | flat |
| `omnivamp` | Omnivamp | combat's own | `8` = 8% | flat |
| `lethality` | Lethality | combat's own | `10` = 10 Armor ignored | flat |
| `armor_penetration` | Armor Penetration | combat's own | `15` = 15% | flat |
| `magic_penetration` | Magic Penetration | combat's own | `15` = 15% | flat |

- **Percent stats are written as you say them:** `8` means 8%, not 0.08.
- **Attack Speed and Move Speed bonuses add up first, then scale the speed.** Two +20% bonuses make
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

(These lines show the format. None of them is a proposed number.)

**An entry has three optional parts:**

- **`weapon`**: the weapon's own attack, written as the tooltip shows it.
  - `damage`: a diamond sword is 7, which includes the 1 a bare hand does.
  - `speed`: attacks per second; a sword is 1.6.
  - These replace the item's own numbers. Speed needs its own place: a weapon's speed is attacks
    per second, while `attack_speed` in `stats` is a percentage bonus.
- **`stats`**: any stat from §3, by name, added on top.
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

**Sanchit's rates, 2026-10-15:**
- each armour point gives **3.5 Armor and 2.5 Magic Resist**;
- each toughness point gives **2.5 Health**.

`data/ascension_combat/data_maps/attribute/stat_conversions.json`:

```json
{
  "values": {
    "minecraft:generic.armor": { "armor": 3.5, "magic_resist": 2.5 },
    "minecraft:generic.armor_toughness": { "health": 2.5 }
  }
}
```

**Any item, from any mod, whose own modifiers give vanilla armour or toughness now gives our stats
at these rates.** No file per item is needed: Cataclysm's armour, other mods' armour and horse
armour all count automatically.

- **Each item's result is rounded down**, so every tooltip shows whole numbers. A diamond helmet's 3
  armour points give 10 Armor, not 10.5.
- **An item whose data names a stat skips that stat's conversion** (rule 2). That is how netherite
  gets more Health than its toughness alone gives (below).
- **Vanilla's own Armor and Toughness lines are hidden from the tooltip**, so a chestplate never
  shows two different "Armor" numbers.
- Vanilla's armour bar above the hotbar still shows. It still rises with better armour.
- The Protection enchantments are stat sources now: they give Armor or Magic Resist plus a small damage reduction (`docs/progression/enchantments.md`, ADR-0022).

**Netherite's own line.** Sanchit wants a diamond set to give +20 Health and a netherite set +40.
- A diamond piece has 2 toughness, so the rate gives it +5 Health and the set +20.
- A netherite piece has 3 toughness, which the rate would turn into +7.
- So each netherite piece names its own `"health": 10` in the item data. That makes the set +40.

Every 100 Armor or Magic Resist halves the damage of its type (`combat-api.md` §3).

| Full set | Armour points | Toughness | Armor | Magic Resist | Health | Physical damage taken |
|---|---|---|---|---|---|---|
| Leather | 7 | 0 | 23 | 16 | — | 85% |
| Gold | 11 | 0 | 37 | 26 | — | 77% |
| Chainmail | 12 | 0 | 41 | 29 | — | 75% |
| Iron | 15 | 0 | 52 | 37 | — | 70% |
| Diamond | 20 | 8 | 69 | 49 | +20 | 62% |
| Netherite | 20 | 12 | 69 | 49 | +40 | 62% |

- **These numbers land close to vanilla's survival.** Count damage taken and health together:
  - in diamond, about 65 raw physical damage kills you (40 health, 62% of each hit taken);
  - in vanilla diamond, it takes 50 to 100, depending on how big each hit is;
  - in netherite, about 97. Big hits get through vanilla's armour more, and ours treats every hit
    the same.
- **Magic Resist counts later.** Today it only reduces combat's weapon procs. Sanchit plans magic
  damage for some bosses.
- **A boss's 100 Armor is above any vanilla set.** The planets' bosses stay a real threat.
- **Putting armour on raises your maximum health but does not heal you**, as vanilla health
  bonuses behave. You regenerate into it. Healing on equip would let a player swap armour mid-fight
  to heal.
- **Mobs wearing armour gain the same stats**, as they gain protection from it in vanilla. A
  zombie in an iron chestplate gets +21 Armor and +15 Magic Resist.

## 6. Reading stats: totals only

**Sanchit, 2026-10-15: no base/bonus split.** League items often scale off *bonus* Attack Damage.
Ours scale off **your total Attack Damage**, "Attack Damage scaling", so nobody has to know which
part is which.
- A bare hand is 1 Attack Damage.
- Everything else adds to that: the weapon, items' stats, growth, runes, potions.
- An item that "deals 50% of your Attack Damage" reads one number.

The same holds for every stat: a stat is one total.

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
    public static double value(LivingEntity entity, CombatStat stat);                 // the total
    public static StatBreakdown breakdown(LivingEntity entity, CombatStat stat);      // per source
}

// Where a total comes from: the base value, each worn or held item by name, then each source.
public record StatBreakdown(double total, List<Part> parts) {
    public record Part(Component source, double amount) { }
}

// Names another system's modifiers on the character sheet. Unregistered ones show as "Other".
public final class StatSources {
    public static void register(ResourceLocation modifierIdPrefix, Component label);
}
```

`value` is in the attribute's own units: points for most stats, attacks per second for Attack
Speed, vanilla's 0.1 walking speed for Move Speed, and fractions (0.08 = 8%) for Omnivamp and both
Penetrations. A modifier's written value goes through `CombatStat#modifier`, which knows the
conversion.

**`/ascension stats [target]`** (permission 2) prints every stat of a living entity with its
breakdown. It works from a console or RCON, so it checks stats on a dedicated server with no client.

**The contract for the rune and growth sessions:**
- **Give a stat** by adding an attribute modifier to the stat's attribute (`CombatStat#attribute`).
  Its id must start with a prefix you registered with `StatSources`, such as
  `ascension_combat:growth/` labelled "Growth".
- **Read stats only through `StatReader.value`.** Never read an attribute directly for gameplay.
  Items that scale do it off the total.
- **Ask for a new stat** in `combat-api.md` §3, "Requested stats". Don't register attributes
  yourself.

`breakdown` names each source: every worn or held item by its name, each registered source by its
label, and "Other" for the rest. That is what the character sheet shows (§7).

## 7. Tooltips and the character sheet

### Tooltips: League's stat block

**Sanchit, 2026-10-15:** take League's item cards as the inspiration. A held item's stats count
while it is in the main hand.

Every item with stats gets a block of stat lines **at the top of its tooltip, under the name**, as
League's item cards have them:

```
Darius's Axe
In main hand
8 Attack Damage          <- the weapon's own attack, white
1.0 Attack Speed
+4 Attack Damage         <- added stats, coloured by stat
+4 Health
+15% Armor Penetration
```

- **One colour per stat, the same on the character sheet:** Attack Damage orange, Attack Speed
  yellow, Health green, Armor gold, Magic Resist blue, Omnivamp red, Lethality and Armor
  Penetration dark orange, Magic Penetration violet, Move Speed white.
- **A small grey line says where the stats count:** "In main hand", "When worn" or "In off hand".
  A weapon's stats count in the main hand only.
- **Vanilla's "When in Main Hand: 7 Attack Damage" block is replaced** by this for every item that
  has stats, vanilla's and other mods' included.
- Attributes we don't know, such as another mod's reach or knockback resistance, still print below
  in vanilla's way.

### The character sheet

Each row gains its sources.

- **Hover a stat to see where it comes from.** For example: "Armor 84: Netherite set 69 · Growth
  15", or "Attack Damage 12: bare hand 1 · Netherite Sword 7 · Runes 4".
- **The main-hand item counts**, whatever it is, and shows as its own line.
- This fixes the sheet's Attack Damage line, which today shows 1 whatever you hold (trap 4 in §1).

## 8. Other mods' items

**Most of it is automatic:**
- **Armour:** the conversion covers it.
- **Weapons:** a weapon's damage is vanilla's Attack Damage, which already is the stat.

Cataclysm's Ignitium set, its swords and any other mod's armour all count on the first day,
with no file.

**Data only adds League-style extras.** For example, Ignitium armour could give Magic Resist, or the
Soul Render could give Omnivamp. Those entries live in the mod's Tier 2 compat jar, under the same
file path:
- **`ascension-compat-cataclysm`:** Cataclysm's weapons, armour and boss tiers (§9). The jar
  already exists.
- **`ascension-compat-create`:** only if Create's tools ever need anything.

A mod with no compat jar gets entries with a mod check in our own file, or in a modpack's datapack.
Tier 1 code never names another mod (ADR-0003).

### What Cataclysm's items give with no data of ours

Measured 2026-10-16 on a dedicated server: each item on a zombie, read with `/ascension stats`.
These are what each item adds. Weapon damage leaves out a bare hand's 1, so a tooltip shows one
more. The extras Sanchit picks go on top of these, in `ascension-compat-cataclysm`.

| Item | Slot | Attack Damage | Armor | Magic Resist | Health |
|---|---|---|---|---|---|
| ancient_spear | weapon | +8.5 |  |  |  |
| astrape | weapon | +9.5 |  |  |  |
| athame | weapon | +4 |  |  |  |
| black_steel_axe | weapon | +8 |  |  |  |
| black_steel_sword | weapon | +5 |  |  |  |
| bloom_stone_pauldrons | chest |  | +31 | +22 | +5 |
| bone_reptile_chestplate | chest |  | +38 | +27 | +6 |
| bone_reptile_helmet | head |  | +21 | +15 | +6 |
| brontes | weapon | +13 |  |  |  |
| ceraunus | weapon | +15 |  |  |  |
| coral_bardiche | weapon | +9 |  |  |  |
| coral_spear | weapon | +5.5 |  |  |  |
| cursed_bow | weapon |  |  |  |  |
| cursium_boots | feet |  | +17 | +12 | +10 |
| cursium_chestplate | chest |  | +35 | +25 | +10 |
| cursium_helmet | head |  | +17 | +12 | +10 |
| cursium_leggings | legs |  | +28 | +20 | +10 |
| gauntlet_of_bulwark | weapon | +10 | +10 | +7 | +7 |
| gauntlet_of_guard | weapon | +10 | +10 | +7 | +7 |
| gauntlet_of_maelstrom | weapon | +10 | +10 | +7 | +7 |
| ignitium_boots | feet |  | +21 | +15 | +10 |
| ignitium_chestplate | chest |  | +38 | +27 | +10 |
| ignitium_elytra_chestplate | chest |  | +38 | +27 | +10 |
| ignitium_helmet | head |  | +21 | +15 | +10 |
| ignitium_leggings | legs |  | +31 | +22 | +10 |
| infernal_forge | weapon | +13 |  |  |  |
| meat_shredder | weapon | +7.5 |  |  |  |
| monstrous_helm | head |  | +10 | +7 | +7 |
| soul_render | weapon | +14 |  |  |  |
| the_annihilator | weapon | +7.5 |  |  |  |
| the_immolator | weapon | +7.5 |  |  |  |
| the_incinerator | weapon | +13 |  |  |  |
| tidal_claws | weapon | +7 |  |  |  |
| void_forge | weapon | +13 |  |  |  |

- **Ignitium and Cursium carry 4 toughness a piece**, so each piece gives +10 Health, the same as
  netherite's own line. A full Ignitium set is 111 Armor, 79 Magic Resist and +40 Health, against
  netherite's 69 / 49 / +40, so the boss armour stays the better set.
- **The three gauntlets carry vanilla armour in the main hand**, so holding one gives Armor, Magic
  Resist and Health too.
- **The Cursed Bow gives nothing**, like every vanilla bow: arrows take the shooter's Attack Damage
  instead (`PhysicalDamage`).

## 9. Mobs in the same data

**Sanchit, 2026-10-15: yes.** `MobStats`' hard-coded tiers become data. (The Warden line below
shows the format, not a proposed number.)

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
  - Eight per-tick listeners used to re-apply fixed stats: seven boot rules and Gargoyle
    Stoneplate's passive. They went in increment 5, with Wraithblade's passive Lethality.
  - Effects that really change every tick stay as they are: Sterak's HP-to-damage conversion,
    Endless Hunger, the Protean Mask's stacks.
- **No caches and no static collections of entities.** The data lives in NeoForge's registries,
  which clear and refill it on reload.
- **No mixins and no third-party names in Tier 1.** The mixins rule and the compat rule both hold
  (ADR-0003).
- **Data over code.** A new mod's item, a retuned rate or a new mob is a JSON edit (ADR-0007
  rule 10).

## 11. Decisions

**Answered by Sanchit, 2026-10-15:**

1. **Vanilla armour.** He chose his own numbers over the proposed 5 Armor per armour and
   toughness point:
   - each armour point is worth 3.5 Armor and 2.5 Magic Resist, rounded down per item;
   - each toughness point is worth 2.5 Health;
   - netherite pieces carry their own +10 Health each, so a diamond set gives +20 and a
     netherite set +40.

   Magic Resist will matter once some bosses deal magic damage (§5).
2. **Base versus bonus: neither.** Items scale off total Attack Damage. A bare hand is 1 (§6).
3. **Tooltips:** League's item cards are the inspiration. A held item's stats count in the main
   hand (§7).
4. **Mobs:** into the same data, with tiers as tags (§9).

**Asked later, when the work reaches them:**

5. **Our 26 items' starting numbers.** These will be sized against the armour numbers above,
   which now set what a full set is worth. Proposed: League's own numbers, scaled.
   - Armor, Magic Resist and every percentage stay as League has them. Our curve equals League's
     at 100: both halve damage there.
   - Attack Damage and Health get scaled down to Minecraft's size.
   - Then you tune by playing, as always.
6. **Extras for vanilla weapons**, for example Lethality on axes. Optional, and one line of data
   each.

## 12. Increments

ADR-0008: each increment is done once it is seen in your client and checked on a dedicated server.
A refactor pass comes every third increment.

| # | Increment | Seen working when |
|---|---|---|
| 1 | **Vanilla armour counts.** The stat definitions, the item and conversion data maps (synced), the item listener, the item-beats-tag merge (unit-tested), netherite's Health lines, and refreshing worn gear on `/reload` | In iron the sheet shows 52 Armor and 37 Magic Resist, and a zombie's hit lands softer. In diamond max health is 40, and in netherite 60. On a dedicated server the numbers survive a reconnect. After `/reload` with a new rate, worn armour changes |
| 2 | **The reading API, and the sheet's sources.** `StatReader`, `StatSources`, Attack Damage synced | The sheet shows 7 Attack Damage holding a diamond sword (its 6 plus a bare hand's 1), as its tooltip says and lists each stat's sources |
| 3 | **Refactor pass** | |
| 4 | **League-style tooltips** | A diamond chestplate shows +28 Armor, +20 Magic Resist and +5 Health, once each. A modded sword shows its stat block |
| 5 | **Our 26 items into data.** The per-tick boot rules go, and our armour stops borrowing diamond's numbers | Each item's tooltip and the sheet agree with its data line. Ninja Tabi gives 15 Armor exactly once |
| 6 | **Refactor pass** | |
| 7 | **Mobs into data**, Cataclysm's bosses included | Jade shows the same Armor and Magic Resist as before, from data. A tag edit plus `/reload` changes a live mob |
| 8 | **Other mods' extras.** Cataclysm entries in its compat jar | Ignitium armour shows its extras with Cataclysm installed, and nothing logs an error without it |
| 9 | **Refactor pass** | |

**Progress:**
- **Client check, 2026-10-17.** Sanchit joined a dedicated server from his Ascension Dev instance,
  saw stats on the items and called it working. He did not report the sheet's hover, `/reload` or
  health across a relog one by one; each of those was checked over RCON on the server.
- **Increments 1 and 2 are built**, checked on a dedicated server over RCON and seen in the client.
- **Increment 3 is done.** One text helper (`StatText`) replaces two copies of number formatting.
  Stats got a kind (points, percent or speed), so no screen names stats one by one. Jade reads
  through `StatReader`. The old `api.CombatAttributes` is deprecated.
- **Increment 4 is built** and seen in the client: League-style stat lines on every item's
  tooltip, one colour per stat (`CombatStat#color`), shared with the character sheet.
- **Increment 5 is built** and checked on a dedicated server. Our items' numbers are data, unchanged
  in value; the per-tick code that applied them is gone. Their stats now reach mobs that wear them.
  The League-scaled numbers (§11 question 5) are still Sanchit's to choose, and are a data edit.
- **Increment 6 is done.** The bare-hand numbers are defined once, leftovers from the move are
  gone, and `StatSources` has tests.
- **Increment 7 is built** and checked on a dedicated server. Mob stats are data, with tiers as
  tags. Every vanilla tier, Cataclysm's four bosses, the Harbinger (through `#c:bosses` alone) and a
  pig all read right. A `/reload` that changes a tier reaches live mobs, and a mob unloaded and
  reloaded with its chunk gets its stats exactly once. `MobStats` shares the item-beats-tag merge
  (`SpecificityPicker`); `MobStatTier` and `CataclysmBossStats` are gone.

**The rune and growth sessions can read stats now**, through `StatReader` (§6).

**Seen together with them, 2026-10-18** (`client/dev/IntegrationChecks`, 64 checks): netherite, one
growth node and a Health shard add up to 63 Health, and the sheet names each part ("Netherite Boots
10, Growth 1, Runes 2"); a 20-point hit in that gear is cut by exactly the defence curve at the
player's whole Armor (74); max Health and held Health come back from disk. It found one fault here: a
respawn healed to full before worn gear's Health was back, so a player in netherite who kept their
inventory came back at 23 of 63. `HealthAcrossLogin` now heals last, after growth and the runes,
having put the gear's modifiers on at once.
