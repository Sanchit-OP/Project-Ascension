# Damage kinds: physical or magic

**What this is.** Every hit in the game is either **physical** (cut by the target's Armor) or **magic** (cut by
its Magic Resist). Written 2026-10-19, after Sanchit pointed out that Magic Resist did nothing for a player:
until then it only cut the magic procs of our own weapons, and nothing hit a player with magic at all. Code:
`internal/mechanics/DamageKinds.java` (the question) and `PhysicalDamage.java` (the defence). Check:
`internal/dev/DamageChecks.java`. Not yet seen in a client.

## The rule

**The attack decides, not the mob.** A Warden's fist is physical and its sonic boom is magic; a Creeper's
blast is magic; a skeleton's arrow is physical. Sanchit's words: "if it is attacking with fist then its
physical, if it's blasting like a creeper then its magic." Minecraft already names every attack with a
*damage type* (`mob_attack`, `explosion`, `sonic_boom`, ...), so the answer is read from that, not from a
list of mobs.

A hit is **magic** when either is true:

1. its damage type is in the tag **`#ascension_combat:magic`**; or
2. the entity that *directly* hit is in the tag **`#ascension_combat:magic_attack`** and is not the attacker
   itself. This is for mods that give all their projectiles one generic type (Cataclysm's fireballs are
   `mob_projectile`, the same as a shulker bullet). A mob's own swing has no other entity, so melee is always
   judged by its damage type, which is `mob_attack`, which is physical.

Everything else is **physical**. Damage nobody dealt on purpose (falling, drowning, starving, suffocation, the
void, cacti) is in neither tag, and neither defence touches it, as before.

Both tags are plain data. Another mod's attacks, or a change of mind about fire, are a JSON edit.

## What each defence does now

| | Armor | Magic Resist |
|---|---|---|
| A physical hit | cut on the curve (every 100 halves it) | not used |
| A magic hit | not used | cut on the same curve, after the attacker's Magic Penetration |

Magic Resist 40 lets 76% through (24% less); 100 halves a magic hit; 200 leaves a quarter. Vanilla's own
armour value is ignored for both, as before. Magic types that vanilla already lets through armour
(`bypasses_armor`: magic, wither, the dragon's breath, the sonic boom, and Cataclysm's six) are scaled
directly; magic types vanilla runs through armour (blasts, fireballs) take Armor's place in the same slot.

## Vanilla attacks, sorted

| Magic | Physical |
|---|---|
| Creeper, TNT, ghast and any other **blast** (`#minecraft:is_explosion`) | Every mob's **melee** (`mob_attack`): zombie, spider, Warden's fist, Ravager, Vex, the dragon's wings |
| **Fire**: burning, lava, standing in fire, a campfire, blaze and ghast fireballs (`#minecraft:is_fire`) | **Arrows and tridents**, a pillager's crossbow, a bogged's arrow |
| **Lightning** (`#minecraft:is_lightning`) | A shulker's bullet, a llama's spit, a breeze's wind charge |
| A witch's potions, poison and instant damage (`magic`, `indirect_magic`) | Thorns, a player's attack |
| An evoker's fangs and a guardian's beam (`indirect_magic`) | |
| The Wither's decay and its skulls (`wither`, `wither_skull`) | **Neither:** falling, drowning, starving, suffocation, cactus, the void, powder snow |
| The dragon's breath (`dragon_breath`) | |
| The Warden's sonic boom (`sonic_boom`) | |

The *damage types* here were read from the game's own tags and checked on a server. Which mob uses which
type is from knowing vanilla; the server check tests the types, not each mob.

## Cataclysm (the four bosses the campaign uses)

Read from the mod's classes (which damage-source calls each entity makes). Cataclysm already gives most of
its elemental attacks their own damage types; its fireballs and orbs use the generic projectile type, so those
are in the entity tag.

| Boss | Physical | Magic |
|---|---|---|
| **Ender Golem** | its fists (`mob_attack`) | void runes (`indirect_magic`) |
| **Ender Guardian** | its sword and fist swings | void runes; its void bullet (entity tag) |
| **Ignis** | its sword swings | fireballs and the abyss fireball (entity tag); the flame strike (`cataclysm:flame_strike`) |
| **Netherite Monstrosity** | its slams and punches | the flame jet and flare bomb (entity tag) |

The other Cataclysm entries are a **first pass**, kept for the planets that will use those bosses: lasers,
EMP, lightning, storm, abyssal burn and the soul and magic curses (`maledictio_magicae`, `maledictio_anima`)
are magic by type; the Harbinger's and Prowler's missiles and howitzers and the Leviathan's abyss orb are in the
entity tag. Left physical on purpose: the melee-flavoured types (`penetrate`, `shredder`, `sword_dance`,
`maledictio` = a cursed strike, `maledictio_sagitta` = a cursed arrow, the draconic slashes). All of it is in
`modules/compat/cataclysm/.../tags/{damage_type,entity_type}/`, every entry `required: false`.

## Our own magic

Our weapon procs and runes (Ragefang, Darken Blade, Heartwood of Pallas, Unending Despair, Arc, Starfall,
Brace) already cut their damage by the *target's* Magic Resist before dealing it
(`MagicDamage`). They deal it as a `magic` or `indirect_magic` hit, which this rule would cut a second time,
so those hits are marked **settled** and skipped (`MagicDamage.hurtSettled`, by identity of the one damage
source being delivered). The Protean Mask's backfire is a price, not an attack, so it is settled too and
Magic Resist does not soften it.

## Adding another mod's attacks

Two optional tag files, for example for a mod `foo`:

```json
// data/ascension_combat/tags/damage_type/magic.json   (its elemental damage types)
{ "replace": false, "values": [ { "id": "foo:ice_shard", "required": false } ] }
// data/ascension_combat/tags/entity_type/magic_attack.json   (projectiles that use a generic type)
{ "replace": false, "values": [ { "id": "foo:frost_bolt", "required": false } ] }
```

Put them in the mod's own `ascension-compat-*` jar, as Cataclysm's are. Entries a mod does not have are
skipped (`required: false`), so the same file is safe with or without the mod.

## What this changes in play (for Sanchit to tune; every number is still a placeholder)

- **Armor no longer protects against blasts and fire.** A player in heavy armour and little Magic Resist
  takes full creeper damage. Vanilla armour is worth about 3.5 Armor and 2.5 Magic Resist per point (the
  conversion in `player-power.md`), so a full diamond set (20 points) is 70 Armor and 50 Magic Resist:
  a blast now meets 50 (29% less) where it used to meet 70 (38% less).
- **Bosses take more from fire and blasts.** Mobs carry Armor above Magic Resist (a boss is 100 and 40), so
  TNT and fire now cut a boss by 24% where Armor 100 cut it by 50%.
- **Fire Protection and Blast Protection** (`enchantments.md`) already give Magic Resist *and* their own
  percentage cut. Both now apply to the same hit. That may be more than intended; look at it when tuning.
- **The Nether's rot** is the Wither effect, so it is a magic hit and Magic Resist now softens it. Gold stays
  the answer (it stops the rot); Magic Resist makes a dose hurt less.
- **Poison, harming potions, witch and evoker attacks, guardian beams, the dragon's breath and the Warden's
  sonic boom** are all answered by Magic Resist now, which is what the gear that gives it (Null-Magic Mantle,
  Mercury Treads, Gargoyle Stoneplate, the Brace rune, the Warding node and enchantment) is for.

## The hurt tint

A player can tell a hit was magic: a **faint blue tint at the very edge of the screen**, in the Magic Resist stat's own
colour, for half a second. Deliberately small: a band about 5% of the screen's height, nothing in the middle, at most a
fifth opaque at the edge and fading out as it goes in and as the half second runs. A bigger hit shows a little more (a
quarter of your health is the strongest), never less than a third strength. Physical hits show nothing.

How it works: when a magic hit has really landed on a player (damage after shields and Magic Resist above zero), the server
sends that player's client one small packet (`MagicHitPayload`; nothing per tick, nothing to anyone else). The client keeps the time and strength of
the last one and draws nested frames of flat fills (`MagicHitLayer`, no texture), just above vanilla's own camera
overlays and under the HUD, with or without the HUD module. Hidden when the GUI is hidden.

To change it: `ascension_combat-client.toml`, `magicHitTint` from 0 (off) to 1 (normal). To change the look for everyone,
the constants at the top of `MagicHitPulse` (peak opacity, thickness, length, band count); the arithmetic has unit tests.
Not seen yet: it is drawn by the client, and the server check cannot see a screen.

## Checking it

`damage-checks.txt` (a dedicated server, about a minute, no window): put an empty `run/server/damage-checks.txt`,
run `./gradlew runDevServer`, read `grep "damage-checks\]" run/server/logs/latest.log`. A pig with a known Armor
and Magic Resist is hurt with the game's own damage sources, first Magic Resist 100 and Armor 0, then the other
way round, and the damage that lands says which defence each attack met. It also checks that our own magic
is cut once, that Magic Penetration works, and (when Cataclysm is loaded) that Ignis' fireball is magic by its
entity while an arrow with the same damage type is not. A stand-in player on a stand-in connection takes a blast, lava, a punch,
a fall and a hit of no damage: the first two send the tint packet, the rest do not. **Seen 2026-10-19: 59 checks, 0 failures.**

## Not done

- **Not seen in a client**, and the check uses stand-in mobs, not a real Creeper or Warden. In the client run, look at the
  tint first: is it noticeable in passing and out of the way? (`MagicHitPulse` and `magicHitTint` are the knobs.)
- A line in the death message, or a stat tooltip that says what percentage Magic Resist cuts, are later choices.
- **Physical-only lifesteal** (`combat-api.md` section 3 said it needed per-hit damage tagging this project did
  not have). It has that now; nothing uses it.
