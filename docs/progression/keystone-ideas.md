# Keystone ideas

> **A brainstorm for Sanchit, 2026-10-15.** He asked for keystones beyond the oxygen one: "different
> things we can add as keystones for minecraft type work. can be combat can be quality of life can
> be something else as well." Nothing here is decided. **Reply with the numbers you like** (and any
> you hate), and which shape in §8 you want.
>
> Every idea is buildable without spells (ADR-0012) and without mixins (ADR-0003). "Hook" says what
> in Minecraft it listens to. "Path" is a suggestion.

## 1. Melee

| # | Idea | What it does | Hook | Path |
|---|---|---|---|---|
| 1 | **Conqueror's Resolve** *(built)* | Every second in a fight: more Attack Damage and healing, up to a cap. | Combat state | Vanguard |
| 2 | **Cadence** | Three **fully charged** hits on one enemy: a big third hit, and it takes more damage from everyone. Rewards timing your swings. | Attack cooldown | Vanguard |
| 3 | **Riposte** | Raise your shield just as a hit lands (a parry): the attacker is stunned and your next hit is a critical. | Shield block timing | Vanguard |
| 4 | **Cavalry** | Hits from horseback deal bonus damage scaling with your mount's speed, and knock harder. | Riding | Vanguard |
| 5 | **Berserk** | The lower your HP, the faster you swing and the more you heal from hits. | HP | Vanguard |
| 6 | **Three Cuts** | Three hits on one enemy within 3 s add a burst of damage. | Hits | Reaver |
| 7 | **Ambush** | Much more damage against an enemy that isn't targeting you. Sneak up on things. | Mob targets | Reaver |
| 8 | **Reaper's Tithe** | Bonus damage to enemies below half HP; kills give Souls that raise it. | Kills | Reaver |
| 9 | **Skyfall** | A critical hit from a fall of 3+ blocks makes a shockwave, like the mace, and you take no fall damage from it. | Falls, crits | Reaver |

## 2. Ranged

| # | Idea | What it does | Hook | Path |
|---|---|---|---|---|
| 10 | **Skirmisher** | Sprinting and hitting charge you up; the next hit (or arrow) heals you and speeds you. Kiting with a bow. | Sprint, hits | Vanguard |
| 11 | **Deadeye** | An arrow that hits near the head is a headshot: big bonus damage, more the farther the shot. | Arrow impact point | Reaver |
| 12 | **Ricochet** | A fully drawn arrow bounces from its target to the next one. | Arrow impact | Aetherwright |

## 3. Magic procs (no spells)

| # | Idea | What it does | Hook | Path |
|---|---|---|---|---|
| 13 | **Starfall** | Charged hits and magic damage call a falling star on the target. | Hits | Aetherwright |
| 14 | **Kindle** | Hits set an arcane burn that grows hotter after 3 s. Burns fire-immune Nether mobs too. | Hits | Aetherwright |
| 15 | **Arc** | Every fourth hit releases lightning that jumps between enemies. | Hits | Aetherwright |

## 4. Defence

| # | Idea | What it does | Hook | Path |
|---|---|---|---|---|
| 16 | **Iron Grip** | Every 4 s in a fight, your next hit deals damage from your max HP and heals you. | Combat state | Bulwark |
| 17 | **Brace** | Block with a shield or take a big hit: huge armour for a moment, then a shockwave. | Shield block | Bulwark |
| 18 | **Retribution** | Blocking with a shield reflects part of the damage, and the attacker heals less. | Shield block | Bulwark |
| 19 | **Undying** | Once every 5 minutes, a killing blow leaves you at 1 HP and briefly untouchable. A free totem on a timer. | Death | Bulwark |

## 5. Companions: your pets fight with you

| # | Idea | What it does | Hook | Path |
|---|---|---|---|---|
| 20 | **Pack Leader** | Your tamed wolves (and cats, parrots) share your armour, hit harder, heal when you heal, and come back to your bed if they die. | Pets | Bulwark |
| 21 | **Golem Oath** | An iron golem you build follows you as a companion, one at a time. Villagers keep the one craft the ancients let them keep; this is you keeping it too. | Golems | Bulwark |

## 6. Team

| # | Idea | What it does | Hook | Path |
|---|---|---|---|---|
| 22 | **Lifeline** | Bond with the nearest ally; a big hit on either of you shields you both. (Can be with or without the air-sharing part.) | Allies | Bulwark |
| 23 | **Rally** | Your kills give nearby allies a burst of speed and healing. | Kills | Vanguard |
| 24 | **Banner** | Hold or plant a banner: allies near it hit harder and regenerate. | Banners | Bulwark |

## 7. Not combat: survival, exploring, gathering

These are "world" keystones: a whole way to play Minecraft. **Taking one gives up a combat
keystone**, which is the trade.

**Survival and planets**

| # | Idea | What it does | Hook | Path |
|---|---|---|---|---|
| 25 | **Deep Lungs** | Your lungs hold half as much air again, and switching tanks is instant. The simple oxygen keystone. | Oxygen | Bulwark |
| 26 | **Cat's Eye** | Creepers and phantoms keep away from you, as they do from cats. The lore says cats can see the Observers; this rune lends you that sight. | Mob AI | Reaver |
| 27 | **Well Rested** | Sleeping heals you fully and gives a long regeneration. The Observers want you awake (phantoms); this answers them. | Sleep | Bulwark |
| 28 | **Silent Step** | You make no vibrations: sculk sensors and shriekers never hear you, so the Warden doesn't come. For Ancient City runs. | Game events | Reaver |
| 29 | **Gilded** | Gold you wear wards off the rot twice as well, and gives Magic Resist. Piglins like you more. | The rot, gold | Aetherwright |
| 30 | **Forager** | Food heals you a little at once and fills you for longer. | Eating | Vanguard |
| 31 | **Keepsake** | When you die you keep your hotbar and armour. Everything else drops as normal. | Death | Bulwark |
| 32 | **Recall** | Out of combat, stand still for 5 s to return to your bed, once every 10 minutes. Same world only, so it never replaces a ship. | Teleport | Aetherwright |

**Exploring and moving**

| # | Idea | What it does | Hook | Path |
|---|---|---|---|---|
| 33 | **Hunter's Sense** | While you sneak, hostile mobs within 16 blocks glow through walls. Caves stop surprising you. | Glowing effect | Reaver |
| 34 | **Skyborne** | Elytra flights last longer, rockets push harder, and diving onto an enemy hits hard. | Elytra | Reaver |
| 35 | **Pathfinder** | Out of combat: faster, step up full blocks without jumping, and sprinting costs no hunger. | Movement | Reaver |

**Gathering and crafting**

| # | Idea | What it does | Hook | Path |
|---|---|---|---|---|
| 36 | **Vein Breaker** | Mine one ore or log and the connected ones of the same kind break with it, up to a cap. The most-loved Minecraft QoL mod, as a choice. | Block break | Vanguard |
| 37 | **Prospector** | Ores you mine drop as if your pick had one more Fortune, and every 30 s a sneak shows ores within 8 blocks. | Loot, sneak | Vanguard |
| 38 | **Forge Touch** | Ores you mine drop smelted, with the furnace's XP. | Loot | Aetherwright |
| 39 | **Magnet** | Items and XP within 8 blocks fly to you. | Items | Aetherwright |
| 40 | **Scholar** | More XP from everything, and enchanting costs a level less. The Observers are generous with those who enchant; this is that generosity. | XP, enchanting | Aetherwright |
| 41 | **Alchemist** | Potions you drink last half as long again; your splash potions are stronger on allies. | Potions | Aetherwright |
| 42 | **Silver Tongue** | Villagers trade with you as if you'd saved their village. | Trading | Bulwark |

## 8. Where world keystones go: three shapes

**A. Combat only.** Four combat paths, three combat keystones each, picked from §1–6. World ideas
go elsewhere (growth's tree, items) or nowhere.

**B. Each path keeps two combat keystones and gets one world keystone** (recommended). Vanguard's
world keystone might be Vein Breaker, Reaver's Cat's Eye, Aetherwright's Scholar, Bulwark's Pack
Leader. Picking it costs you that path's combat keystone, and with the change item and growing
runes, that's a lifestyle, not a toggle. Still 36 runes.

**C. A fifth path, all world** (League's Inspiration, which we dropped). Three world keystones, and
world minors (Magnet, Forager, Pathfinder) that any combat page can take as its secondary path. Most
players would then take it as their secondary every time, so combat secondaries would fade.

**One boundary:** growth's Wayfarer arm already gives plain numbers for exploring (speed, mining
speed, safe falls, reach, air). World keystones should be **behaviours** (vein mining, a magnet,
creepers fleeing), never the same numbers again.

## 9. Your other answer, as I read it

Changing runes: free out of combat, **but removing or swapping a rune costs an item**, so nobody
swaps on the spot. And **runes grow over the game, up to a cap; swapping one loses its growth.**
My proposal, for you to correct:

- **The item:** *Rune Chalk*, crafted from Earth materials (an amethyst shard and calcite, say, four
  per craft). Saving a page uses one if you removed or replaced any rune. Filling an empty slot is
  always free, so a new player's first page and every newly opened slot cost nothing.
- **Runes grow:** each rune on your page grows as you defeat **kinds of enemy you haven't
  defeated with it yet**, up to 12 kinds, and its numbers rise to their best. Bosses count. A kind
  you already counted counts no more, so a mob farm can't fill it; exploring new places does.
  Swap the rune out, and it starts again from nothing.
- **Some runes are pure growth,** like League's Overgrowth: a Bulwark minor that gives max HP as it
  grows, a Vanguard one that gives attack speed.
- **The screen shows growth** as a ring filling round each rune's socket. Swapping a grown rune
  asks you to confirm: "Lose 9/12 growth?"
