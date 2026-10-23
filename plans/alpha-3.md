# Alpha 3

Started 2026-10-23, the day after alpha.2 went out (`main` at `f29f397` was alpha.2; everything after it is alpha.3). Sanchit: the next
plan is alpha.3, and **Iron's Spells needs a lot of care**. This page says what is in, what is proposed and how Iron's would be brought in
without breaking the game. Nothing here under "Proposed" is decided.

## In alpha.3 already (committed locally, not pushed)

| Commit | What |
|---|---|
| `137d257`, `d58c802` | The item audit (`docs/progression/item-audit.md`): Cataclysm's Ignitium, Cursium and Monstrous Helm at least the netherite they are smithed from; its 15 boss weapons raised and held under every legendary; the bow and crossbow add Attack Damage and every bow is swung slowly; three more Cataclysm abilities are magic; **four ability keys instead of one**. Server-checked (`damage-checks` 405/0, `check-balance` 0 problems), **not yet pressed in a client**. |

The unreleased changelog is the top section of `pack/launch/CHANGELOG.md`. The network version went from 6 to 7, so alpha.2 and alpha.3 cannot
share a server.

## Left over from the audit

- Curios items (Cataclysm's belts and rings are not worn any more; Icarus wings and the gliders) were not read; they are not item attributes.
- Shields add nothing. A design question, not a bug.
- A client look at the four keys, their tooltips and a bow's slow blow.

## Proposed: more places to explore (structures from other mods)

Low risk if it is one or two mods and each is brought in as the Cataclysm Ruined Citadel was: a biome tag in a compat jar says where it
may generate, and the planet loot system already pays out every chest on the Moon, the Nether and the End. Candidates found on Modrinth for
1.21.1 NeoForge on 2026-10-23: Dungeons and Taverns (a datapack, easiest to tune), YUNG's Better Dungeons, When Dungeons Arise (large, heavy),
Moog's Voyager Structures. Not Incendium (it replaces the Nether's biomes; Planet 3 is ours) and not an End structure mod (Planet 4 is ours).
Costs to check for each: world generation time and pre-generation, pack size, and a tier tag for every mob it adds (a mob with no tier is paper
next to our bosses).

## Proposed: Iron's Spells 'n Spellbooks (the careful one)

Why it is attractive: magic would stop being a few procs and a defence stat. Wizards that cast at the player make Magic Resist matter; the
rarity ladder of its spells can follow our bosses, early weapons, mid spells, late guns and mechs. Facts checked: 1.21.1 NeoForge build 3.16.3
(2026-08-18), over 100 spells in 9 schools, 9 casters, 2 bosses, 12 armour sets, 11 structures; all rights reserved, so it goes in by manifest like
our other such mods. **Iron's Spellbooks Tweaks** (MIT, server side) caps the rarity a player may cast and unlocks more by advancement or kill,
from datapack files, which is the gate Sanchit asked for and fits ADR-0018 (bosses unlock capability; no world is locked).

### Why care, specifically

1. **It is a one-way door for worlds.** Adding it to an existing world is harmless. A world played with it will not load cleanly without it. So
   it ships only when we mean to keep it.
2. **It is a third stat system** beside the legendary items and the runes and growth: its own mana, spell power, cooldown and spell resistance. Left
   alone it fights our numbers; wired badly it doubles a defence (Iron's resistance and our Magic Resist on one hit).
3. **Its numbers are tuned for 20-health mobs.** Our bosses carry 1,000 to 4,500 health and 100+ Armor and Magic Resist.
4. **Overlap with systems we built:** the HUD module (its mana bar), Curios (we cut the slots to seven on 2026-10-22; it wants its own), Lootr and the
   planet loot modifiers (it adds scrolls to chests), teams (spells that hurt or heal allies; ours cannot hurt a teammate), Better Combat, and the
   mixins `ascension-compat-cataclysm` and `-sable` already carry (ADR-0013, ADR-0016).
5. **Per-player unlocks against per-team progress.** Tweaks unlocks trigger on a player's own advancement or kill; our milestones belong to the team.
   Does a teammate who did not land the killing blow get the advancement? Unchecked.
6. **It updates often**, and its config has had multiplayer sync problems that Tweaks works around with its own file.

### How, in order (each step ends at a gate; no step is skipped)

0. **Decide, no code.** What do spells scale off: our Attack Damage, Magic Penetration and runes, or Iron's own spell power and mana? Is magic a
   *lane* (its own resource, its own armour) or a layer over the legendary kit? Sanchit's call; everything after depends on it.
1. **Sandbox.** A branch and its own worktree, and a copy of the test mods folder: **not** the Ascension Dev instance (another session installs
   jars there; compare before overwriting). Downloading the jars is Sanchit's go.
2. **Read before wiring.** The method that worked for Cataclysm: take the jar apart and list its damage types, projectile and summon entities,
   attributes, Curios slots, loot modifiers, structure biome tags, mixins and event hooks. Write what each does in `docs/technical/` before any
   compat code exists.
3. **The compat jar `ascension-compat-ironsspells`** (Tier 2, ADR-0003: Tier 1 never names it, and no mixin without an ADR): magic damage tags,
   tier tags and stats for its mobs, item data for its armour (**the item audit finds the Cataclysm fault in it; use the floor rule**), Curios slots,
   the rarity ladder as Tweaks unlock files tied to our growth milestones, and a plain rule for casting in vacuum.
4. **Balance by measurement**, as the weapons were: a server check casts with a stand-in player and the result is held by `check-balance` against the
   legendary band. A spell build must not kill the standard boss faster than the slowest legendary alone (54 s) at the same stage.
5. **Verify on the order ADR-0008 sets:** dedicated server, then two clients (teams), then a world unload and reload, then a performance-log entry
   (Iron's spawns many entities).
6. **Only then** the quest page, the recipes that gate it, the page text.

### Go or no-go

If step 0 or step 2 turns up something we cannot answer (a mixin that fights ours, damage that skips the damage events, no per-player gate we can
tie to a team), **Iron's does not ship in alpha.3**; it waits for alpha.4 and the sandbox branch stays. A cheaper alternative if magic is the
point: grow our own magic lane (Arcane Edge, Aether Tip and Spellpiercing exist) and use Iron's only for its structures and mobs.

## Proposed: guns and mechs (later than alpha.3)

Guns exist for 1.21.1 NeoForge: TaCZ (guns are data, so their damage can be set), Superb Warfare (guns, tanks, helicopters), Create: Gunsmithing.
No ready-made mech mod turned up in a Modrinth search; mechs may be ours, perhaps built on Sable. Open.

## Not decided (needs Sanchit)

1. Spell scaling and lane (step 0 above).
2. Structure mods: which one or two.
3. Whether alpha.3 is "Iron's in" or "audit, structures and a client pass", with Iron's on a branch for alpha.4.
4. The "few more things before we publish" and the "final rollout" list he mentioned, which still has not been told to me.
