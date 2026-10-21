# Enchantments

> **Status, 2026-10-18, branch `combat-enchantments`: decisions answered, increments 1 to 8 built (1, 2, 4, 5
> seen in a client; 7 and 8 checked on a dedicated server and by build only), refactor passes 3, 6 and 9 done.** Not yet merged. The audit (§2) is read from
> the 1.21.1 data and our code; rows marked **measure** still need a measured hit in a running game
> (ADR-0008). Every number is a placeholder: a balancing and stats session follows. The decisions are in
> §11; the structural choice is [ADR-0022](../decisions/0022-enchantments-are-stat-sources.md).
>
> Sanchit, 2026-10-18: *"we need to work on how the enchantments are gonna work. I feel like maybe we
> can implement the enchantment with our stat system and it will make it all pretty cool."*

**In one paragraph:** an enchantment becomes one more source of stats, next to gear, runes and
growth. In 1.21.1 enchantments are data, and one of their effect types adds an attribute modifier by
level, which is exactly what our stat system reads. So most reworks are JSON, with no mixin and no
per-tick code. Protection stops being a second armour system on top of ours and becomes Armor;
Sharpness becomes Attack Damage that our items scale off; new enchantments of ours give Lethality,
Omnivamp, Magic Resist and lung capacity. Enchanting stays the Observers' script (the lore), costs
experience as it does now, and a few enchantments are rewards from bosses and planets, never
requirements (ADR-0018).

## 1. What is true now

- **Enchantments are a data registry** (`data/<ns>/enchantment/*.json`). Each has `effects`, a map of
  effect components. The relevant ones:
  - `minecraft:attributes` adds an attribute modifier **by level**. Respiration, Efficiency, Depth
    Strider, Sweeping Edge, Swift Sneak, Blast and Fire Protection and Aqua Affinity use it already.
    It can name **any attribute in the registry, including ours** (`ascension_combat:armor`).
  - `minecraft:damage_protection` (Protection and friends) and `minecraft:damage` (Sharpness, Smite,
    Bane, Power, Impaling) are vanilla's separate damage steps.
- **Our Armor replaces vanilla's armour reduction** (`PhysicalDamage`, `ARMOR` reduction). It does not
  touch the **enchantment** reduction. So today a hit is cut by our curve *and then* by Protection.
- **Protection is strong against our curve.** Each level is 1 "protection point" and each point is
  4% off, capped at 20 points (80%). Protection IV on four pieces is 16 points, **64% off, multiplied
  on top of Armor.** A diamond set (69 Armor, 62% taken) with Protection IV would take about 22%
  of a hit. One enchant would do more than the whole armour rework in `item-stats.md` §5.
- **Sharpness is flat and large against our weapons.** V adds +3 damage (the data is `linear`, base 1.0
  plus 0.5 per level above the first). On a diamond sword (7) that is +43%.
  It adds to the sword's hit only, so it does not feed anything that scales off Attack Damage.
- **Nothing of ours enchants anything yet** (written 2026-10-18; **fixed 2026-10-22**, see below). Which of the 26 combat items accept which
  enchantments is decided by vanilla's item tags (`#minecraft:enchantable/*`) and by their enchantability value, and
  was never chosen.
- **Fixed 2026-10-22 (Sanchit found it in play: the legendary items could not be enchanted).** The eight swords are in `#minecraft:swords`,
  the armour in `#minecraft:head_armor`, `chest_armor`, `leg_armor` and `foot_armor`, and the three bows in `enchantable/bow` and
  `enchantable/durability` (`data/minecraft/tags/item/` in `ascension-combat`), so vanilla's own enchantable tags reach them: a sword takes
  Sharpness, Keen Edge, Leech, Mending, Unbreaking and Soulbound, a bow Power, armour Protection and the rest, boots Feather Falling. **Armour had
  no durability at all**, and vanilla lets only a damageable item take an enchantment at the table, so it now has diamond's (multiplier 33:
  helmet 363, chestplate 528, leggings 495, boots 429). It can now wear out, which Mending and Unbreaking answer. The two off-hand relics
  (Zonia's Hourglass, Sterak's Harness) are plain, undamageable items and stay unenchantable. Proved on a dedicated server by `damage-checks`
  (now 245 checks, 0 failures; the new ones failed on every item before the tags, 13 on the armour before the durability). Not seen in a client.
- **Atmosphere already planned for it:** `atmosphere-api.md` lists "Respiration enchantment → a
  `gearEfficiency` contribution". Lung capacity grows from gear, and a tank is never extended by an
  enchantment (rule from the atmosphere module).
- **Costs:** the table charges levels (1 to 3) plus lapis (1 to 3). The Scholar rune takes a level
  off. A rune change costs 3 levels, a respec 20. Growth deliberately uses no XP (it is farmable).

## 2. Audit: the 42 vanilla enchantments

Effect type is what the data says. "Today" is what it does in this game with combat loaded. **Measure**
means a measured hit in a client is still owed.

### Armour

| Enchantment (max) | Effect type | Today, with combat loaded | Verdict |
|---|---|---|---|
| Protection (IV) | damage_protection | Stacks multiplicatively on top of the Armor curve; IV on a full set is 64% off. Dwarfs the armour tiers. **Measure** | **Rework** → Armor |
| Fire Protection (IV) | damage_protection + attributes | Same, for fire; also shortens burn time | Keep as a *hazard* enchant, smaller, fire only (Armor is physical, so it cannot be a stat) |
| Blast Protection (IV) | damage_protection + attributes | Same, for explosions | Keep, smaller (hazard enchant) |
| Projectile Protection (IV) | damage_protection | Same, for arrows | Keep, smaller (hazard enchant) |
| Feather Falling (IV) | damage_protection | 12% per level off fall damage, to 48% | Keep. Falls matter on low-gravity planets: check §6 |
| Thorns (III) | post_attack | Reflects damage; ignores Armor on the attacker's side? **Measure** | Keep, review once the mob data is final |
| Respiration (III) | attributes (oxygen bonus) | Vanilla's air bubbles are replaced by the atmosphere module | **Rework** → lung capacity (§6) |
| Aqua Affinity (I) | attributes | Underwater mining speed. Water is an unbreathable atmosphere here | Keep |
| Depth Strider (III) | attributes | Swim speed | Keep |
| Frost Walker (II) | location_changed | Utility | Keep |
| Soul Speed (III) | location_changed + tick | Nether, Planet 3 is the real Nether | Keep. A natural reward for Planet 3 (§7) |
| Swift Sneak (III) | attributes | Sneak speed | Keep |
| Binding Curse, Vanishing Curse | curses | Curses | Keep as they are |

### Melee, ranged, tools

| Enchantment (max) | Effect type | Today | Verdict |
|---|---|---|---|
| Sharpness (V) | damage | +3 flat to that weapon's hit only | **Rework** → Attack Damage (§3) |
| Smite (V), Bane of Arthropods (V) | damage + post_attack | Situational flat damage | Keep as situational; scale the same way as Sharpness |
| Breach (IV) | armor_effectiveness | Reduces the target's *vanilla* armour effectiveness, which our curve replaced. **Measure**: likely does nothing | **Rework** → Armor Penetration % |
| Density (V), Wind Burst (III) | mace effects | Mace only | Keep |
| Fire Aspect (II) | post_attack | Sets target alight | Keep |
| Knockback (II), Punch (II) | knockback | Utility | Keep |
| Sweeping Edge (III) | attributes (sweep damage) | Better Combat weapons have their own sweeps. **Measure** | Review |
| Looting (III), Fortune (III), Silk Touch | loot | Utility. Collector's Pants overlaps Looting | Keep; see §8 |
| Power (V) | damage | Flat on arrows; arrows already add the shooter's Attack Damage (`PhysicalDamage`) | **Rework** like Sharpness |
| Flame, Infinity, Multishot, Piercing, Quick Charge | utility | Utility | Keep |
| Impaling (V), Channeling, Loyalty, Riptide | trident | Trident only | Keep |
| Efficiency (V) | attributes (mining speed) | Utility; growth's Wayfarer sector also gives mining | Keep, check stacking with growth |
| Unbreaking (III) | item_damage | Utility | Keep |
| Mending (I) | repair_with_xp | Repairs with XP. Our items are long build paths and a lost item is expensive | **Decision** (§11) |
| Lure, Luck of the Sea | fishing | Utility | Keep |

**The two that must change:** Protection (it makes the whole armour system irrelevant) and Breach (it
does nothing against our curve). The rest is either utility, which the design rules prefer, or flat
damage that is better made stat-shaped.

## 3. The design: enchantments are stat sources

### 3.1 The mechanism (data only for the vanilla reworks)

A reworked enchantment replaces its damage effect with a `minecraft:attributes` effect:

```json
"effects": {
  "minecraft:attributes": [{
    "id": "ascension_combat:enchantment/protection",
    "attribute": "ascension_combat:armor",
    "amount": { "type": "minecraft:linear", "base": 3.0, "per_level_above_first": 3.0 },
    "operation": "add_value"
  }]
}
```

- The stat is applied **when the item enters or leaves a slot**, with no per-tick work, the same
  moment gear's own stats are applied.
- Because it is one more attribute modifier, `StatReader.value` totals it in, items that scale off
  Attack Damage see it, and the character sheet shows it.
- **One source name:** we register the prefix `ascension_combat:enchantment/` with `StatSources` as
  "Enchantments". Modifier ids must be unique per slot, so each id names the enchantment. (To verify
  in the jar: 1.21.1 derives the final modifier id from this `id` and the slot; the stat tests below
  prove it.)
- The enchantment datapack is shipped by `ascension-combat` and overrides vanilla's file of the
  same name. A modpack can still edit it. **Needs a data check rule** in `tools/check-data.py`: a
  misspelt `attribute` or `operation` is silently dropped.

### 3.2 How big may an enchantment be?

Growth's yardstick (`growth.md` §8): one unit = 1 HP, 4 Armor, 4 Magic Resist, 0.25 Attack Damage, 3%
Attack Speed, 1% Omnivamp, 2 Lethality, 2% Penetration or 2% Move Speed. A fully grown v1 player has
about 35 units of growth, a legendary item 8 to 10.

**Proposal (placeholders):**

| | Units |
|---|---|
| One enchantment at its top level | **3 to 5** |
| A fully enchanted set of gear (all slots, best enchantments) | **about 25** |
| So the three sources compare as | growth 35, items about 60 to 80, enchantments about 25 |

Enchantments should be a real, visible part of power but not the biggest part, and not so big that
the Protection problem comes back by another route.

### 3.3 The reworks in numbers (placeholders)

> **Buffed twice on 2026-10-22 (Sanchit: "0.5 a level ain't it"; the human side gets stronger, the mobs stay as they are; a player cannot fight a boss
> optimally, so kill times that assume it are too hopeful).** The tables below are the first pass; where they disagree, this box wins.
>
> | Enchantment | First pass (2026-10-18) | Now (per level, top level) |
> |---|---|---|
> | Sharpness | +0.5 Attack Damage (V +2.5) | **+1.5 (V +7.5)** |
> | Power | +0.5 Attack Damage (V +2.5) | **+1.5 (V +7.5)** |
> | Smite, Bane of Arthropods | vanilla flat damage only | **+1 Attack Damage a level against anything (V +5)** (a plain attribute, so items scale off it), **plus vanilla's flat +2.5 a level against what they name (V +12.5)**, left as vanilla |
> | Keen Edge | +2 Lethality (III 6) | **+8 (III 24)** |
> | Leech | +1% Omnivamp (III 3%) | **+4% (III 12%)** |
> | Vitality | +1 Health (III +3) | **+5 (III +15 a piece, +60 a set)** |
> | Protection | +3 Armor (IV 12) | **+5 (IV 20 a piece)**, the 2.25% cut unchanged |
> | Projectile Protection | +4 Armor (IV 16) | **+6 (IV 24)** |
> | Fire, Blast Protection, Warding | +3 Magic Resist (IV 12) | **+5 (IV 20)** |
> | Fleet, Breach | | unchanged |
>
> **Smite and Bane: a plain attribute plus vanilla's hidden bonus (Sanchit, 2026-10-22).** The first attempt added the Attack Damage by code only against
> the named mobs, which was too complex and was removed. Now each gives +1 Attack Damage a level on every hit (less than Sharpness's 1.5, which is the
> "reduced scaling"), so the items whose abilities scale off Attack Damage see it, and keeps vanilla's own flat damage against the undead (Smite) or
> arthropods (Bane) unchanged, **invisible to the sheet and to those abilities, on purpose: a hidden strong bonus the player finds**. Smite V against the
> undead (skeletons, zombies, the Wither, phantoms) is +5 and +12.5, 17.5 in all, against Sharpness V's 7.5. Proved on a dedicated
> server with real swings by `damage-checks` (251 checks, 0 failures): Sharpness V +7.5, Smite V +17.5 against a zombie (before its Armor), Bane V only its
> 5 against a zombie, Veigo's Blade hitting 250 harder with Smite V (it saw the 5 Attack Damage), Vitality III +15 a piece. **Not yet balanced against the
> bosses**: `tools/time-to-kill.py` does not count enchantments, and Smite V against the Wither needs a look when played.

| Enchantment | Becomes | Per level | Top level | Units |
|---|---|---|---|---|
| Protection | +Armor on each piece, **and** a small all-source damage reduction | +3 Armor, and 2.25% less damage | IV: +12 Armor a piece (+48 a set) and 9% a piece, **36% a full set** | 12 |
| Projectile Protection | **more** Armor than Protection (arrows are Attack Damage hits), plus 2.25% less projectile damage | +4 Armor, 2.25% vs projectiles | IV: +16 Armor a piece | 16 |
| Fire Protection | +Magic Resist, plus 2.25% less fire damage | +3 Magic Resist, 2.25% vs fire | IV: +12 Magic Resist a piece | 12 |
| Blast Protection | +Magic Resist, plus 2.25% less explosion damage | +3 Magic Resist, 2.25% vs blasts | IV: +12 Magic Resist a piece | 12 |
| Sharpness | +Attack Damage, held | +0.5 | V: +2.5 | 10 |
| Power | +Attack Damage (arrows add the shooter's AD already) | +0.5 | V: +2.5 | 10 |
| Breach | +Armor Penetration % | +4% | IV: 16% | 8 |

**Sanchit, 2026-10-18 (decision 1).** Protection gives Armor *and* a small reduction on top, so a
full Protection IV set takes 36% less from every source (vanilla's is 64%). Projectile Protection
gives more Armor, because projectile hits are mostly Attack Damage. Fire and Blast give Magic Resist
and the same small reduction against their own damage. Built as one enchantment each with two effects:
`minecraft:attributes` for the stat and `minecraft:damage_protection` for the reduction (vanilla counts
4% per point, so 2.25% is 0.5625 points a level). The armour exclusive set already stops two of them
sharing a piece. The 36% is his number; plain 2% a level would give 32%.

Notes on the choices:
- **Protection:** the diamond set rises from 69 to 117 Armor (54% taken instead of 62%), then the 36%
  takes it to about 35% of a hit. Still a step rather than vanilla's wall (about 22%).
- **Sharpness as Attack Damage** is deliberate: it now makes items that "deal 50% of your Attack
  Damage" hit harder too, and growth, runes and enchantments add up in one number.
- Smite and Bane stay situational (more against undead or arthropods) and keep a vanilla-style
  damage effect, scaled the same way, so a stat-source one cannot be stacked with them
  (`exclusive_set/damage` already prevents that).

### 3.4 What increment 2 built (2026-10-18)

Sharpness and Power give +0.5 Attack Damage a level in the main hand; Breach gives +4% Armor
Penetration a level (the attribute holds a fraction, so the data says 0.04). Seen on a dedicated
server (Sharpness IV on a diamond sword: `Base 3, Diamond Sword +6, Enchantments +2`; Power V on a
bow +2.5; Breach IV on a mace +16%) and in a client by the scripted checks.

Known limits, left for the balancing session:
- **Arrows:** a bow's Power counts while the bow is in the main hand. `PhysicalDamage` adds the
  shooter's Attack Damage at the moment an arrow lands, so a player who switches item in flight loses
  it. Vanilla's Power was fixed when the arrow was fired.
- **Melee with a Power bow:** the same Attack Damage is also there if you hit with the bow.
- Smite, Bane of Arthropods and Impaling are untouched (still vanilla's situational damage).

### 3.5 One unexplained thing (open)

In the scripted client run, a 20-point hit on the player taken straight after another 20-point hit
took about 36% more off than the curve plus Protection's 9% says. The same item on a zombie on a
dedicated server matched exactly (6.435 off a 10-point hit at 50 Armor). Vanilla counts 2.25 protection
points on the player, as designed. The extra cut shows up with no enchantment involved, so I believe a
rune or growth effect from the first hit is still active, but I have **not** confirmed that. The check
asserts the protection points and logs the hit instead. To settle it: take two hits with no enchantment
at all and compare.

## 4. New enchantments of ours (all data)

Names are working titles. Each is a `minecraft:attributes` effect on a stat from `item-stats.md` §3,
so no new attribute is needed. If any of them wants a stat that does not exist, that goes to Sanchit
(the stats owner) rather than being added here.

| Enchantment | Stat | Items | Idea |
|---|---|---|---|
| **Keen Edge** | Lethality +2 per level | weapons | the Armor-ignoring axis; fits axes |
| **Leech** | Omnivamp +1% per level | weapons | healing build |
| **Warding** | Magic Resist +3 per level | armour | caster and boss-magic defence |
| **Vitality** | Health +1 per level | armour | the health axis |
| **Fleet** | Move Speed +3% per level | boots | boots identity |
| **Deep Lung** | lung capacity +N seconds per level | helmets | oxygen world, replaces Respiration (§6) |
| **Pressure Seal** | slower drain for the tank on the back, capped | chestplates | needs the atmosphere module's rule; see §6 |
| **Glyphbound** *(lore)* | a small bonus per *different* enchantment on the item | any | the Observers notice a well-written line. Build choice rather than a number |

Each one is a build choice instead of a number only if two enchantments **compete for the same slot
or the same pool** (the exclusive sets), so we should add a few exclusive sets: Keen Edge / Sharpness,
Leech / Mending (the healing ones), Warding / Protection.

### 4.1 What increment 5 built (2026-10-18)

Five of the eight, all data (`data/ascension_combat/enchantment/`), each a `minecraft:attributes`
effect under `ascension_combat:enchantment/<name>`, so each shows under "Enchantments" on the sheet.

| Enchantment | Max | Gives, per level | Items | Notes |
|---|---|---|---|---|
| Keen Edge | III | +2 Lethality | weapons | exclusive with Sharpness, Smite, Bane (the damage set) |
| Leech | III | +1% Omnivamp | weapons | stacks with Keen Edge |
| Warding | IV | +3 Magic Resist | armour | exclusive with the Protection family (the armour set), a build choice |
| Vitality | III | +1 Health | armour | takes Protection |
| Fleet | III | +3% Move Speed | boots | |

They are in `minecraft:non_treasure`, so the enchanting table, villagers and loot all offer them (vanilla's
tags are built on that one). **Mending now repairs at half rate** (decision 3: `factor` 2.0 to 1.0).

**Left out for now:** Deep Lung (Respiration already does it), Pressure Seal (would need an atmosphere
change, and a tank must not be stretched by an enchantment), Glyphbound (needs code). Our 26 combat items
accept enchantments only through vanilla's tags so far; choosing per item is §8.

### 4.2 Soulbound and the tomes (built 2026-10-18; Sanchit dropped Apotheosis for now and asked for these)

Sanchit's answers: Soulbound from loot and trades only; Scrapping and Extraction first; how the tomes are
obtained was left to me (crafted, so no world is needed, ADR-0018).

| Thing | What it does | How you get it |
|---|---|---|
| **Soulbound** (enchantment, level 1) | The item stays with you through death and returns to the same inventory slot after the respawn. Does nothing under `keepInventory`. Code is `Soulbound.java` (a saved attachment, no mixin); the enchantment is data. | Loot and villager trades only (the `treasure`, `tradeable` and `on_random_loot` tags, not the table). The Archive's and Colony's chests give it 10% of the time. |
| **Tome of Scrapping** | At an anvil with an enchanted item: returns the item with every enchantment off (curses and Soulbound too), for 1 level; uses up the tome. | Shapeless: book, 2 lapis, flint. |
| **Tome of Extraction** | In a crafting grid with an enchanted item and a book: makes an enchanted book with all its enchantments, and the item comes back with none. A custom recipe type (`ascension_combat:extraction`), because an anvil can return only one thing. | Shapeless: book, 2 lapis, amethyst shard. |

Art: `tools/generate-tome-art.py` (ash cover with three ember scratches; violet cover with a gold clasp and
three rising motes). Not seen in a client yet. Soulbound, the recipe and the anvil have checks written in
`EnchantmentChecks.planTomes()` that have **not** been run.

**Damage reduction on tooltips (2026-10-18):** Protection's extra cut is not a stat, so no line showed it and
it looked missing. An enchanted piece's tooltip now says, for example, `-4.5% damage taken (Protection II)`.
The cut itself was measured working (see 3.5).

## 5. Where they come from

Rewards, never requirements (ADR-0018). A player can always reach every vanilla-power enchantment at
the table; planets and bosses add *extra* ones.

| Source | What | Notes |
|---|---|---|
| Enchanting table | the vanilla pool and our reworks | the `in_enchanting_table` tag lists what is rollable |
| Villager books | the `tradeable` tag | villagers sell them because they cannot read them (lore) |
| Structure loot | our new ones through `on_random_loot` | uses the `enchant_randomly` loot function |

**Built in increment 7:** the Lunar Archive's and the Nether colony's store chests each give, half the time,
one book of one of *our* five enchantments (`#ascension_combat:own`, a random level). It is a NeoForge
`add_table` loot modifier in `ascension-combat`'s own data, keyed on the chest table's id
(`ascension_worlds:archive/stores`, `ascension_worlds:nether_colony/stores`), so `worlds` knows nothing of
combat and Tier 1 modules stay independent: with combat absent the modifier simply never matches. Rolled 40
times on a dedicated server: 18 and 16 books. Vanilla chests already offer them through `on_random_loot`.

**Sanchit, 2026-10-18 (decision 5): no boss, planet or tablet rewards.** All enchantments come from the
table, villagers and structure loot. Boss books, tablet-taught enchantments and a crafted route were
considered and left out; they can return later without changing anything else here.

## 6. Respiration and Aqua Affinity on a planet

- **Respiration is already lung capacity** (found in increment 4): the atmosphere module's
  `VanillaBreathingGear` gives **10 seconds of lungs per level** (III = +30 s on 20 s), honouring the
  `waterIntegration` switch, and never touches a tank. Nothing needed building. My first proposal of
  4 s a level is dropped in favour of what is there; the balancing session may retune it.
  Seen in a client: lungs 80 units, 200 with Respiration III, 80 again once it is removed.
- **Aqua Affinity** stays as it is: water is a hazard that already has its own atmosphere.
- **Feather Falling** needs a check against the Moon's gravity before we decide anything.

## 7. Cost

- **Stay with XP levels and lapis** (the lore already says experience is the price). Nothing new is
  needed for v1.
- **Scholar** keeps taking a level off. The rune change (3) and respec (20) do not change.
- **Mending** is the interaction to settle: it turns XP into repairs, and XP is also the price of
  everything above (§11).
- **A new cost of ours** (a reagent from a planet) is possible later for the boss and tablet
  enchantments, and is not needed for the first version.

## 8. Our 26 items

- Every item gets an **enchantability tag** by role: weapons accept the weapon set, armour the armour
  set, boots the boots set. An item with no sensible enchantments (a trinket) is **simply
  unenchantable**, which leaves room for items with none.
- **Items that scale off Attack Damage** get Sharpness's value for free, since it is now Attack Damage.
- **Passives that overlap an enchantment** (Collector's Pants and Looting, Guardian's Wing and
  Mending) need one pass over the list. The usual fix: the enchantment is allowed, but they are in the
  same `exclusive_set`.

## 9. Tooltips and JEI

**Built in increment 8 (compiles and builds; not yet seen in a client, by Sanchit's rule of testing in
batches).** An enchantment's part of a stat is added after the item's own number in the enchantment colour
(light purple), on the same line, so the line reads like Sanchit's example:

```
Diamond Sword                       Diamond Chestplate (Protection IV)
In main hand                        When worn
7 +2 Attack Damage                  +28 +12 Armor          (item's own, then the enchantment's)
1.6 Attack Speed                    +20 Magic Resist
```

- The weapon's own attack line (white) takes the extra: `7 +2 Attack Damage` is a diamond sword at 7 with
  Sharpness IV's +2.
- A stat the item already shows takes the extra on that line. A stat only the enchantment gives (Warding on
  a helmet with no Magic Resist of its own) is its own line, in the stat's colour.
- The extra is found by the modifier id prefix `ascension_combat:enchantment/` (`StatSystem.ENCHANTMENT_PREFIX`),
  the same prefix the "Enchantments" source on the sheet uses. Vanilla's enchantment names ("Sharpness IV")
  still print below as vanilla draws them, so the line says how much and the name says which.
- Percent and speed stats print with their own units (`+9% Move Speed`) through the same `StatText`.

**JEI:** no code of ours. JEI builds its enchanted-book list from the enchantment registry, so our five
should appear with every level; I have **not** seen this. A description page for each would need a Tier 2
`ascension-compat-jei` jar and is not built.

### 9.1 Tooltip audit of every enchantment (2026-10-18, from the data, not seen in a client)

What an enchanted item's tooltip shows today: our stat lines (`7 +2 Attack Damage`), a `-X% damage taken
(name)` line for any enchantment with a damage-protection effect, and vanilla's own lines for attributes we
have no stat for. Everything else shows only the enchantment's name.

| Group | Enchantments | What shows | Verdict |
|---|---|---|---|
| Stat sources | Protection, Projectile, Fire, Blast Protection, Sharpness, Power, Breach, Keen Edge, Leech, Warding, Vitality, Fleet | the stat on the item's own line; Protection family also the reduction line | fine |
| **Misleading** | Respiration | vanilla's "Oxygen Bonus" attribute line, but the real effect is **+10 s of lungs a level** (atmosphere module) | **fix**: hide the vanilla line, say "+30 s lung capacity" |
| **Misleading** | Feather Falling | `-12% damage taken`, but it is falls only | **fix**: say which damage (falls) |
| **Nothing shown** | Soulbound | name only; nothing says it keeps the item on death | **add** a description |
| **Nothing shown** | Mending | name only; now repairs at half rate | **add** a description |
| **Nothing shown** | Smite, Bane of Arthropods, Impaling, Density | name only; the damage is vanilla's situational bonus | add a short description |
| Vanilla attribute lines | Efficiency, Depth Strider, Sweeping Edge, Swift Sneak, Aqua Affinity, Fire/Blast's burning time and knockback | vanilla draws them where it draws enchantment attributes | fine |
| Name only, self-explaining | Looting, Fortune, Silk Touch, Unbreaking, Knockback, Punch, Fire Aspect, Flame, Infinity, Multishot, Piercing, Quick Charge, Loyalty, Riptide, Channeling, Lure, Luck of the Sea, Thorns, Frost Walker, Soul Speed, Wind Burst, curses | players know these | leave |
| **Books** | every enchanted book | only the names; no stat preview | **add**: a book shows what each enchantment gives at its level |

**Proposed mechanism, one for all of the above:** a grey description line under an enchantment's name, read
from the lang file by key (`enchantment.<namespace>.<name>.desc`, with the level's number filled in where
there is one). Data-driven, so another mod's enchantment gains a line by adding a key, and we write keys
only for the ones in the table marked fix or add. On a book it also shows the stat preview.
**Built 2026-10-18 (compiles, builds, data check passes; not seen in a client):** `EnchantmentTooltips`.
Under an enchantment's name it adds (a) its damage reduction, worded by `enchantment.<ns>.<name>.reduces`
("-12% fall damage taken" for Feather Falling; "damage" otherwise), (b) its grey description from
`enchantment.<ns>.<name>.desc` when the lang file has one, and (c) on an enchanted book only, a coloured line
per stat it gives at that level. Descriptions exist for Soulbound, Mending, Smite, Bane of Arthropods,
Impaling, Density, Respiration and our five; Respiration's vanilla "Oxygen Bonus" line is hidden. A data
rule fails the build when one of our enchantments has no description.

## 10. Other mods' enchantments

Cataclysm and others add enchantments. Their files are not ours to edit; a Tier-2 compat jar (or an
entry with a `neoforge:conditions` mod check) can give them a stat the same way. **Not designed yet.**
I will check what ships with Cataclysm before saying any more.

## 11. Decisions for Sanchit

1. **Protection: ANSWERED.** +Armor plus a small reduction (36% on a full set); Projectile gives more
   Armor; Fire and Blast give Magic Resist plus the reduction. See §3.3.
2. **Sharpness and Power: ANSWERED.** +Attack Damage, 0.5 a level (V = +2.5), the larger option.
   That is 10 units, above the 3 to 5 proposed in §3.2; Sanchit accepts it, and a balancing and stats
   session at the end of this one will retune everything. Mark all numbers as placeholders until then.
3. **Mending: ANSWERED.** Keep, at half vanilla's repair rate (data only).
4. **Size: ANSWERED.** No cap now. Use the numbers above as placeholders; the balancing and stats
   session after this one sets the real ceiling (§3.2's 25 units is only a starting guess).
5. **Rewards: ANSWERED.** No. Table, villagers and loot only (§5).
6. **Cost: ANSWERED.** Stay with XP levels and lapis.

## 12. Build order (ADR-0008: each increment seen in a client; a refactor pass every third)

| # | Increment | Seen working when |
|---|---|---|
| 1 | **Plumbing.** The `Enchantments` stat source and one reworked enchantment (Protection) as data, plus a data-check rule | A Protection IV chestplate shows +12 Armor on the sheet under "Enchantments", and a measured hit matches the curve |
| 2 | **The other reworks** (Sharpness, Power, Breach) | Sharpness V shows +2.5 Attack Damage under "Enchantments" |
| 3 | **Refactor pass** | |
| 4 | **Respiration → lung capacity** (already built in atmosphere; proven here) | Respiration III: lungs 80 → 200 units, seen in a client |
| 5 | **Our new enchantments** (five built) | each shows its stat, and is offered by the table, trades and loot |
| 6 | **Refactor pass** | |
| 7 | **Sources** (our two structures' chests) | built; rolled on a dedicated server, not yet seen in a client |
| 8 | **Tooltips and JEI** | tooltips built; not yet seen in a client; JEI is the registry's own list |
| 9 | **Refactor pass** | done: the tooltip code split into sort and section, and a data rule for our enchantments' lang and tags |

Each increment is also checked on a dedicated server (the registry is synced), after a reload, and
with `IntegrationChecks` extended to cover it.

## 13. Research (from memory, not looked up this session)

These are what I remember of each, and what I take from them. None is a source for a number.

- **Path of Exile, Diablo:** random affixes on an item, with a pool and tiers. The build feeling
  comes from *choosing among a limited number of slots*, not from the numbers. We have that with
  exclusive sets and a cap on the number of enchantments per item.
- **Minecraft Dungeons:** each enchantment has a slot and a distinct behaviour, not a bigger number.
  A reason to prefer utility over inflation.
- **Terraria reforges:** a modifier changes several stats at once with a trade-off (damage up, speed
  down). Our stat names make the same trade easy: +Lethality, −Attack Speed.
- **Apotheosis, Tetra, Enigmatic Legacy:** Minecraft mods that grow the enchantment cap or make
  items from parts. Lesson: higher caps with no trade-off flatten the game quickly. We keep the cap low.
- **League:** items give flat stats and the build path is the build. Enchantments should look like a
  small *rune-shard* choice, not like a second item.

**What we can add:** anything that is a stat, a hazard answer, or a utility effect, as data.
**What we should add:** the reworks in §3, a few identity enchantments in §4, and the rewards in §5.

## 12. Audit of every enchantment, 2026-10-22 (Sanchit: "go through every single one")

All 42 vanilla enchantments and our six, against the systems as they now stand. **Done** = tuned by us; **Fine** = vanilla, nothing in our systems
disagrees; **Test** = needs a look in a real game; **Decide** = a design choice nobody has made.

| Enchantment | Under our systems | Verdict |
|---|---|---|
| Sharpness, Power | +1.5 Attack Damage a level (V 7.5); a bow's arrow carries the shooter's Attack Damage | Done |
| Smite, Bane of Arthropods | +1 Attack Damage a level (V 5) plus vanilla's flat +2.5 a level against the undead or arthropods (V 12.5), hidden on purpose | Done; **Test** Smite V against the Wither |
| Keen Edge (ours) | +8 Lethality a level (III 24), about +19% against any Armor | Done |
| Leech (ours) | +4% Omnivamp a level (III 12%) | Done |
| Breach (mace only) | +4% Armor Penetration a level (IV 16%), about +23% against the Dragon, +12% against 100 Armor | Fine; raise to +6% if the mace still lags in play |
| Density, Wind Burst (mace) | vanilla smash maths: flat damage added before Armor | Fine; **Test** the rebalanced mace's smash |
| Impaling (trident) | vanilla: only against aquatic mobs | Fine, situational |
| Riptide, Loyalty, Channeling | movement and utility | Fine |
| Knockback, Punch, Flame, Infinity, Multishot, Piercing, Quick Charge | utility; no crossbow of ours | Fine |
| Fire Aspect | burning is fire damage, which is magic here, so Magic Resist cuts it and fire-proof bosses ignore it | Weak against bosses, fine as utility |
| Looting | overlaps Collector's Pants | **Decide** (the usual answer: allowed, in the same exclusive set) |
| Sweeping Edge | Better Combat has its own sweep settings | **Test** that it does anything |
| Protection (+5 Armor a level), Projectile (+6), Fire and Blast (+5 Magic Resist), Warding (ours, +5 Magic Resist), Vitality (ours, +5 Health) | the defensive side, buffed | Done |
| Feather Falling | vanilla fall protection | Fine (more useful with a mace and ships) |
| Thorns | reflect damage is physical, so a mob's Armor cuts it | Weak against bosses; low priority |
| Respiration | already +10 s of lungs a level through the atmosphere module (seen in a client) | Fine |
| Aqua Affinity, Depth Strider, Frost Walker, Soul Speed, Swift Sneak | utility | Fine |
| Fleet (ours) | +3% Move Speed a level (III 9%) | Weak: candidate for +5% a level |
| Binding Curse | prevents unequipping | Fine |
| Vanishing Curse | now reaches the legendary items (they are damageable, so in `enchantable/vanishing`); its effect on a Soulbound item is untested | **Test**; make it exclusive with Soulbound if it destroys the item |
| Mending | our override halves the XP repair (factor 1, vanilla 2); the armour is damageable now | **Decide** (section 11) |
| Unbreaking, Efficiency, Fortune, Silk Touch, Luck of the Sea, Lure | utility | Fine |
| Soulbound (ours) | keeps the item through death | Fine |
| Deep Lung, Pressure Seal, Glyphbound | designed, never built (section 4) | Left out |
