# Enchantments

> **Draft 1, 2026-10-18, branch `combat-enchantments`.** Nothing here is built. The audit (§2) is
> read from the 1.21.1 data and from our code; the rows marked **measure** still need a measured hit
> in a running game (ADR-0008) before anyone trusts them. Every number is a placeholder to tune by
> playing. All six decisions in §11 are answered (2026-10-18). The structural choice is
> [ADR-0022](../decisions/0022-enchantments-are-stat-sources.md) (Proposed).
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
- **Nothing of ours enchants anything yet.** Which of the 26 combat items accept which enchantments is
  decided by vanilla's item tags (`#minecraft:enchantable/*`) and by their enchantability value, and
  was never chosen.
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

## 5. Where they come from

Rewards, never requirements (ADR-0018). A player can always reach every vanilla-power enchantment at
the table; planets and bosses add *extra* ones.

| Source | What | Notes |
|---|---|---|
| Enchanting table | the vanilla pool and our reworks | the `in_enchanting_table` tag lists what is rollable |
| Villager books | the `tradeable` tag | villagers sell them because they cannot read them (lore) |
| Structure loot | our new ones through `on_random_loot` | uses the `enchant_randomly` loot function |

**Sanchit, 2026-10-18 (decision 5): no boss, planet or tablet rewards.** All enchantments come from the
table, villagers and structure loot. Boss books, tablet-taught enchantments and a crafted route were
considered and left out; they can return later without changing anything else here.

## 6. Respiration and Aqua Affinity on a planet

- **Respiration** becomes a **lung capacity** gain (a helmet grows the player's own lungs), as the
  atmosphere rule says: gear grows lungs, and nothing can silently extend a tank.
- Proposal: +4 seconds of lungs per level (lungs are 20 s), so III is +12 s. A planet's atmosphere
  still decides whether lungs are used at all, so this cannot trivialise a vacuum: a tank is the way to
  stay out for minutes.
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

## 9. Tooltips

Show an enchantment's stat as a League-style line in the same colour as its stat (`StatTooltips`),
and fold vanilla's enchantment names into one block. Vanilla's own "Protection IV" line still reads
fine on its own, so we only add the numbers to it. This is the last increment.

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
| 4 | **Respiration → lung capacity** | A Respiration III helmet gives 32 s of lungs on the Moon |
| 5 | **Our new enchantments** | each shows its stat |
| 6 | **Refactor pass** | |
| 7 | **Sources** (loot tables for our new enchantments) | each shows up in structure loot |
| 8 | **Tooltips and JEI** | |
| 9 | **Refactor pass** | |

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
