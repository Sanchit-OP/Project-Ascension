# Runes: research

> **Research, 2026-10-15**, by the rune session (`plans/session-prompts/2-rune-system.md`). It
> gathers what the rune system could be before anything is designed. The design that comes out of
> it is [`docs/technical/rune-system.md`](../technical/rune-system.md). Nothing here is decided.
>
> Sources are at the end. League's rune effects below are summarised in our own words, as of
> League's patch 26.x.

## Contents

1. League's rune system, all of it
2. What makes League's runes fun, and where they went wrong
3. Other games' talent and rune systems
4. What makes a rune screen feel good
5. What Minecraft combat can support
6. What fits Ascension
7. What we can add, and what we should add

---

## 1. League's rune system, all of it

### The shape of a page

A League rune page has **nine picks**, made before a game and fixed for that game:

| Part | Picks | Rule |
|---|---|---|
| **Primary path** | 1 keystone + 3 minors | One keystone from 3 or 4. Then one minor from each of three rows of three. |
| **Secondary path** | 2 minors | Any other path. Two minors from two *different* rows. No keystone. |
| **Stat shards** | 3 | One from each of three small rows: offence, flex, defence. |

Pages are saved and named. The client also offers a recommended page for each champion.

### The five paths

Each path has a colour, an emblem and a one-line identity.

**Precision (gold): improved attacks and sustained damage**

| Slot | Rune | What it does |
|---|---|---|
| Keystone | Press the Attack | Three hits in a row on one enemy deal bonus damage and leave them taking more damage for a while. |
| Keystone | Lethal Tempo | Attacking stacks attack speed; at full stacks, attacks gain range and on-hit damage. |
| Keystone | Fleet Footwork | Moving and attacking charge an empowered hit that heals you and gives a burst of speed. |
| Keystone | Conqueror | Hits stack bonus damage over the fight; at full stacks, you heal from damage you deal. |
| Row 1 | Absorb Life · Triumph · Presence of Mind | Heal on a kill · heal and gold on a takedown · restore mana on a takedown. |
| Row 2 | Legend: Alacrity · Haste · Bloodline | Takedowns build stacks of attack speed · ability haste · lifesteal (and HP at full stacks). |
| Row 3 | Coup de Grace · Cut Down · Last Stand | More damage to enemies below 40% HP · to enemies with more max HP than you · when you are low. |

**Domination (red): burst damage and reaching your target**

| Slot | Rune | What it does |
|---|---|---|
| Keystone | Electrocute | Three separate hits on a champion within 3 s add a burst of damage. |
| Keystone | Dark Harvest | Hitting a low-HP champion deals bonus damage and harvests a soul; souls stack all game. |
| Keystone | Hail of Blades | Your first three attacks on a champion get a huge attack-speed boost. |
| Row 1 | Cheap Shot · Taste of Blood · Sudden Impact | True damage on slowed or stunned targets · a heal on damaging a champion · armour and magic pen after a dash or stealth. |
| Row 2 | Sixth Sense · Grisly Mementos · Deep Ward | Vision: find enemy wards · trinket cooldown from takedowns · longer-lasting wards in enemy territory. (Added 2025.) |
| Row 3 | Treasure Hunter · Relentless Hunter · Ultimate Hunter | Each *unique* takedown gives gold · out-of-combat speed · ultimate cooldown. |

**Sorcery (blue-violet): empowered abilities and resources**

| Slot | Rune | What it does |
|---|---|---|
| Keystone | Summon Aery | Damaging an enemy sends a little spirit to hurt them; shielding an ally sends it to shield them. |
| Keystone | Arcane Comet | Ability damage calls down a comet on the target. |
| Keystone | Stormraider's Surge | Dealing a quarter of a champion's HP within 3 s gives a burst of speed and slow resist. (Returned 2026, replacing Phase Rush.) |
| Keystone | Deathfire Touch | Abilities set a burn that grows stronger after 3 s. (Returned 2026.) |
| Row 1 | Axiom Arcanist · Manaflow Band · Nimbus Cloak | Stronger ultimate, shorter on takedowns · more mana · speed after a summoner spell. |
| Row 2 | Transcendence · Celerity · Absolute Focus | Ability haste at levels · more of every speed bonus · bonus damage while above 70% HP. |
| Row 3 | Scorch · Waterwalking · Gathering Storm | Your next ability burns · speed and power in the river · power that grows every 10 minutes. |

**Resolve (green): durability and crowd control**

| Slot | Rune | What it does |
|---|---|---|
| Keystone | Grasp of the Undying | Every 4 s in combat, your next hit deals bonus damage from your max HP, heals you, and permanently raises your HP. |
| Keystone | Aftershock | After you stun an enemy, you gain big resistances, then explode for damage. |
| Keystone | Guardian | Guard a nearby ally; when either of you takes damage, you both get a shield. |
| Row 1 | Demolish · Font of Life · Shield Bash | Charge a hit on a tower · enemies you stun heal allies who hit them · your next hit after gaining a shield deals more. |
| Row 2 | Conditioning · Second Wind · Bone Plating | Armour and magic resist after 12 minutes · regenerate after taking damage · the next three hits after a big one are reduced. |
| Row 3 | Overgrowth · Revitalize · Unflinching | Permanent HP from nearby minions dying · stronger heals and shields · tenacity and slow resist. |

**Inspiration (teal): creative tools and rule-bending**

| Slot | Rune | What it does |
|---|---|---|
| Keystone | Glacial Augment | Your attacks and immobilising effects slow and freeze. |
| Keystone | Unsealed Spellbook | Swap your summoner spells during the game. |
| Keystone | First Strike | Strike first in a fight for bonus damage and gold. |
| Rows | Flashtraption · Magical Footwear · Cash Back; Triple Tonic · Time Warp Tonic · Biscuit Delivery; Cosmic Insight · Approach Velocity · Jack of All Trades | Free items, potions and boots; shop tricks; haste; speed toward impaired enemies; stats for item variety. |

### Stat shards

| Row | Choices |
|---|---|
| Offence | Adaptive force (≈5 AD) · 10% attack speed · 8 ability haste |
| Flex | Adaptive force · 2.5% move speed · HP that grows with level |
| Defence | 65 HP · 15% tenacity and slow resist · HP that grows with level |

A champion has roughly 600–2,500 HP over a game, so the defence shard's 65 HP is about **3–10%** of a
player's health. **Shards are small by design.**

---

## 2. What makes League's runes fun, and where they went wrong

Riot rebuilt runes in 2017 ("Runes Reforged"). The old system had about sixty slots, most of them
small flat stats, which made picking a chore rather than a choice. Their stated goal was to put
the power into **fewer picks, each of which matters**, and push the plain stats into the
background. The lessons, as we read them:

1. **A keystone is a way to play, not a number.** Electrocute asks for three quick hits. Grasp
   asks you to trade in and out. Fleet Footwork rewards kiting. Aftershock rewards diving in. Each
   keystone changes what you *do*, and you can tell it apart in one sentence.
2. **Every row asks one question.** Precision's last row is three answers to "when do you deal
   extra damage?": against the low, against the tank, or when you are low yourself. You pick the
   situation you expect. A row of three *different kinds* of thing is harder to choose from.
3. **The secondary path is flavour, not power.** Two minors from another path let you mix
   identities (a Precision fighter splashing Resolve's toughness) without a second keystone.
4. **Plain stats live apart, in shards.** Riot's 2018 follow-up found that tying stats to a path
   pushed players away from the path they wanted. Moving stats into shards fixed it.
5. **You feel it fire.** Every keystone has a clear visual and sound when it procs: the comet
   falls, Electrocute's bolt cracks. A rune nobody notices might as well not exist.
6. **Growth inside one game.** Dark Harvest's souls, the Legend stacks and Gathering Storm make
   a page feel stronger as the game goes on.
7. **Cheap to try.** Pages are free to edit, saved, and come recommended.

**Where it went wrong:**
- **Filler runes.** Map-specific or economy runes (Waterwalking, Cash Back, the ward row) are
  picked by default or never. Every rework since 2018 has been about filler.
- **Hard to judge.** Runes whose value is hidden (small percentages, conditions you can't see)
  frustrate players. The 2017 complaint about the old system was exactly this.
- **Snowballing.** Permanent stacks (Dark Harvest, Grasp) can run away with a game.

---

## 3. Other games' talent and rune systems

| Game | How it works | What to take from it |
|---|---|---|
| **WoW: Dragonflight talents** | Two trees (class, spec). Arrows show what leads to what; rows open as points are spent. Octagon "choice nodes" hold two options, one at a time. Named, saved loadouts. Since 2022 you can change talents anywhere out of combat: no tomes, no rested areas. | **Change freely out of combat.** Saved loadouts. A choice node needs a clear mark, or players miss it (Blizzard's forums found it confusing). |
| **Path of Exile** | A passive tree of 1,300+ nodes, mostly small stats. Its **keystones** are rule-changing nodes with a real downside: Chaos Inoculation sets your life to 1 and makes you immune to chaos. Respecs cost currency. | **A keystone with a cost is memorable.** But a huge tree of small stats is the "illusion of choice" critics name. |
| **Grim Dawn: Devotion** | Constellations on a sky map. Buying a constellation's stars grants affinity, which opens other constellations. Bright stars grant "celestial powers" that proc in combat. | **A star map is a beautiful metaphor**, and Ascension is a space game. But players found it hard to plan: dim what you can't reach, and show what a goal costs. |
| **Last Epoch** | A passive tree per class and mastery. Players complained about respeccing one click at a time. | **One button to clear.** |
| **Diablo IV: Paragon** | Boards of nodes with glyph sockets; you place glyphs and the board lights up around them. | **The socket metaphor**: placing a glyph into a socket and watching it light is satisfying. |
| **Hades: Mirror of Night** | Every talent is one of a pair (A or B), swapped for free. Boons come on cards in their god's colour, with rarity frames and exact numbers. | **One colour per patron. Cards with exact numbers.** An A/B pair is the simplest real choice. |
| **Hollow Knight: charms** | Charms cost notches; you only change them at a bench. The charm screen is simple icons on dark cloth, and still beautiful. | **A place to change** gives the swap a ritual. **Simple icons on a dark ground** can look premium. |
| **Destiny 2: subclass** | Pick 2 aspects; each aspect opens fragment slots. | **Big picks open small picks.** |
| **Minecraft mods** | Pufferfish's Skills is the common skill-tree framework: data-driven trees, categories, opened with K, nodes in advancement-style frames. Passive Skill Tree brings PoE's tree to Minecraft. | Data-driven trees are normal in Minecraft. Their screens look like the **advancements screen**, which is exactly the vanilla look Sanchit wants away from. |

---

## 4. What makes a rune screen feel good

**League's own client screen**, from memory of playing it:
- The page is **two columns**: primary path on the left, secondary on the right, shards beside them.
- Each path has a full **background painting** and an animated emblem. Choosing a path crossfades
  the whole screen into that path's colour, and plays its sound.
- **Chosen runes are lit, the others greyed.** A thin line traces down the column, through the
  runes you chose.
- **Keystones are big**, minors medium, shards tiny dots.
- **Hovering** shows a card: the name, what it does in a sentence or two, the numbers in colour.
- Pages are **saved and named** along the top.

**What we take from it and the rest of §3:**

| Element | What makes it good |
|---|---|
| **Layout** | The whole page visible at once, no scrolling. Read in one direction: path, keystone, minors, then the second path, then shards. **The path's identity dominates:** its colour, its emblem, its name. |
| **States** | Four states, readable without colour: *chosen* (full colour, lit ring), *open* (dim colour), *not allowed by the rules* (dimmer), *locked by progress* (dark silhouette with a padlock). Value and contrast do the work, not hue alone. |
| **Icons** | One clear silhouette per rune, readable at its size. A shared **frame per slot type** (keystone large and ornate, minor medium, shard small), tinted by the path. Icons in the path's colour family. |
| **Hover card** | Appears at once, next to the rune, never covering it, and stays on screen. Name and path. **One plain sentence** of what it does. Then the numbers, each stat in its own colour (League's habit: attack damage orange, armour yellow, magic resist blue, HP green). Then its conditions: "in combat", "8 s cooldown". If it's locked, **say what unlocks it**: a locked rune is a goal. |
| **Sound** | A quiet tick on hover. A chime on choosing, pitched per path. A heavier sound for a keystone. A dull note for "not allowed". A final sound on saving. Quiet, short, never on every frame. |
| **Motion** | Short and eased (100–250 ms): a rune brightens and lifts on hover, a ring draws itself round a chosen rune, the path colour crossfades, the keystone breathes slowly. In pixel-art steps that land on whole pixels, so the art never blurs. |
| **Cheap to try** | Changes are local until saved; a clear "save" and "reset". Saved pages if wanted. |

### Minecraft's screens: what's possible

- **Everything is laid out in GUI pixels.** At GUI scale *n* each GUI pixel is *n×n* screen pixels.
  On a 1080p screen, scale 4 leaves only **480 × 270** GUI pixels; 720p at scale 3 leaves
  **426 × 240**. A screen meant for scales 2–4 must fit about **420 × 240**.
- **Textures** are drawn with `GuiGraphics.blit`/`blitSprite`. Since 1.20.2, GUI sprites can be
  **nine-sliced** by a `.mcmeta` file, so one small texture makes a panel or hover card of any
  size. Pixel art drawn one texel per GUI pixel stays crisp at every scale.
- **The font** is vanilla's, 8 px high (9 with spacing). A hover card holds ~30 characters a line.
- **Motion:** the screen renders every frame, so eased animation is just arithmetic on the time.
  Translating by whole GUI pixels keeps art sharp.
- **Sound:** `SimpleSoundInstance.forUI` plays any sound event with a pitch. Vanilla's own sounds
  (amethyst chimes, the enchanting table, the beacon) are the game playing its own sounds, like
  worldgen placing vanilla blocks; new sounds of our own would need recording or synthesis.

---

## 5. What Minecraft combat can support

ADR-0012 rules out spells. Here is what a rune *can* hook into, with no mixins (ADR-0003) and no
per-tick world scans (ADR-0007):

| Minecraft verb | How a rune hears it | Rune shapes it suits |
|---|---|---|
| **A charged melee hit** (the attack cooldown) | The attacker's attack-strength scale, read in `AttackEntityEvent` | "A fully charged hit…": Minecraft's own timing skill |
| **A critical hit** (falling) | `CriticalHitEvent`: can change the crit multiplier | Ambush from above, jump-crit builds |
| **Sprinting, sweeping, knockback** | Sprint state at the hit; `LivingKnockBackEvent` | Charge-in openers; knockback resistance |
| **A drawn bow, crossbow, trident** | `ArrowLooseEvent` (draw charge), `ProjectileImpactEvent` | Fully drawn shots, kiting builds |
| **Blocking with a shield** | `LivingShieldBlockEvent` | Bulwark: block to trigger |
| **Taking a hit** | `LivingIncomingDamageEvent`, `LivingDamageEvent.Post` | Plating, retaliation, shields |
| **Healing** | `LivingHealEvent` (scale any heal), our `OmnivampHealing` | Heal and shield power |
| **A kill** | `LivingDeathEvent` | On-kill bursts, souls, reset cooldowns |
| **Status effects** | `MobEffectEvent.Applicable`/`Added` | Shorter slows, poison and wither (tenacity) — duration change to verify |
| **Mob targeting** | `LivingChangeTargetEvent` | Taunt (protect allies), stealth (lose them) |
| **Fall damage** | `LivingFallEvent` | Ambush landings |
| **Item actives** (Galeforce, Wraithblade, Zonia's…) | Our own cooldowns (`PausableCooldown`) | "Item haste", empowered hit after an active |
| **Shields, stasis, dash, blink, true damage, grievous wounds** | Our shared mechanics (`combat-api.md` §5) | Every path |
| **In combat** | `CombatStateRegistry` (`core`) | Ramps, out-of-combat bonuses |
| **Oxygen** | `atmosphere`'s `DrainModifier`, through a `core` contract | Bulwark's keystone, share-air |
| **Allies** | Other players and tamed pets near you, found on an event, not every tick | Shields and heals for the team |

**What doesn't fit Minecraft:**
- **Spells and ability haste:** no cast system (ADR-0012). Item actives have cooldowns, though,
  so *item* haste works.
- **Vision and wards:** Minecraft has no fog of war. Domination's ward row has no analogue.
- **Gold and the shop:** no economy inside a fight. Loot and XP stand in.
- **Champion levels:** the growth session's subject, not runes'.
- **Towers and minions:** none.
- **Physical-only lifesteal:** healing can't tell a physical hit from a magic one (`combat-api.md`
  §3), so lifesteal is omnivamp here.
- **Permanent stacks** (Dark Harvest, Grasp's HP): a League game lasts 30 minutes; a Minecraft
  world lasts months, and a mob farm would max any per-kill stack overnight. Stacks must reset (on
  death, out of combat) or be capped by something you can't farm.

---

## 6. What fits Ascension

- **Planets and their rules.** The Moon's vacuum and dust storms, space, the Nether's rot (wear
  gold), Earth's Deep Dark. A rune can *reward* a place ("in vacuum…", "while wearing gold…") but
  must never be *required* for one (ADR-0018). A situational rune keyed to a world is the
  Waterwalking trap unless the situation comes up on every world.
- **Oxygen.** Bulwark's keystone is confirmed to tie into oxygen drain in combat (`combat-api.md`
  §7). The open question (§9 there): does it **cost** you air for power, or **blunt** a drain
  everyone pays? Today nobody pays a combat drain, so the "blunt" reading would first change
  oxygen for everyone.
- **Co-op first** (`vision.md`). Bulwark's team runes. Share-air rescue already exists. Allies in
  Minecraft include tamed wolves, which helps solo players.
- **Solo must still work.** A run alone can't depend on runes that only do something with a
  teammate.
- **Bosses.** The bosses (Warden, Wither, Cataclysm's) have huge HP and 100 Armor (`MobStats`):
  "more damage to things bigger than you" (Cut Down) and max-HP damage have a real home here.
- **The lore.** The script (`the-mark.md`, `what-the-warden-keeps.md`): enchanting borrows from
  the void and the Observers notice, and "they are generous with you, because they want you
  strong". Runes could be read two ways, and **the lore is Sanchit's to write**:
  1. *The Observers' gift*: power that comes too easily, like enchanting. Runes would then belong
     to the trap.
  2. *The Sealers' discipline*: fighting arts carved, not written, so the Observers can't rewrite
     them.

  Either way, the **teal script and the diamond eye are the network's** (`art-guide.md`), so the
  rune screen shouldn't use them until the lore says runes are the network's.
- **League likeness.** Sanchit wants the combat *items* to look like League's (`combat-art-reference.md`).
  Runes are different: §9 of `combat-api.md` asks that the two-tree page *not* read as a League
  clone. The answer the research suggests: League's **structure and polish** (paths, keystones,
  rows, shards, hover cards), with an **original look** (our own emblems, frames and layout).

---

## 7. What we can add, and what we should add

### What we *can* add

Everything below is buildable with the hooks in §5, no spells, no mixins.

- **Keystones:** a ramp over a fight (Conqueror, built) · a rhythm of charged hits (Press the
  Attack) · a kiting charge that heals and speeds you (Fleet Footwork) · a three-hit burst
  (Electrocute) · a big opener from out of combat (First Strike) · an execute that harvests souls
  lost on death (Dark Harvest) · a falling star on magic damage (Arcane Comet) · a ramping burn
  (Deathfire Touch) · an empowered hit after an item active (League's Sheen family) · a pulsing
  max-HP hit that heals (Grasp) · block-to-brace-then-shockwave (Aftershock) · a bond that shields
  you and an ally (Guardian) · oxygen for defence (ours) · a little companion spirit (Aery; needs
  an entity and its art).
- **Minors:** heal on kill · attack speed on kill · decaying hit stacks · regen when low · damage
  vs low HP, vs bigger targets, when you're low · true damage on slowed targets · heal on hit with
  a cooldown · pen after a dash or fall · crit damage · stealth (mobs lose you) · out-of-combat
  speed · item haste · a cooldown cut on kills · magic pen · magic damage up · power while healthy ·
  power that grows while you stay alive · damage reduction after a big hit · knockback and effect
  resistance · shields after a block · taunt · heal allies who hit what you hit · stronger heals and
  shields · resistances after a long fight · air-sharing that shields.
- **Stat shards:** small AD, attack speed, HP, Armor, Magic Resist, move speed.
- **The page:** unlocks with growth · free changes out of combat · saved pages · a "rune altar"
  block for changing.
- **The screen:** a custom panel, path emblems, rune icons, hover cards, motion, sound.

### What we *should* add

1. **All four paths, three keystones each** (12 keystones). Each a distinct way to play, told
   apart in one sentence, and each **felt when it fires** (particles and a sound).
2. **Two rows of three minors per path** (24 minors), each row asking one question. Fewer than
   League's three rows, because Minecraft has fewer combat verbs, and filler is League's own worst
   lesson. Primary: keystone + one minor per row. Secondary: one minor from each row of another
   path. The page keeps today's "two and two".
3. **Stat shards**, three rows, sized to about one boss's worth of growth in total.
4. **No per-kill permanent stacks.** Anything that stacks resets on death or leaving combat.
5. **Unlocks that open slots, not runes.** Every rune can be tried from the first minute; the
   page's *slots* open as the player grows (secondary path, then shards), through a hook the
   growth session provides. Locked slots show what opens them.
6. **Changes free out of combat**, as WoW learned. No altar.
7. **Bulwark's oxygen keystone costs air for defence** (the "cost" reading): simple, and it works
   today without changing oxygen for everyone.
8. **A screen with its own look**: a custom panel, four emblems and colours, an icon per rune,
   hover cards with numbers, motion and vanilla sounds, fitting 420 × 240 GUI pixels. League's
   polish, our own layout and art.
9. **Requested stats** (owned by the stats session): **Tenacity** (shorter harmful effects and
   less knockback), **Heal and Shield Power**, and **Item Haste** (shorter item-active cooldowns).
   Each is used by more than one rune and would be worth an item later. Until they exist, the runes
   that need them keep their effect local to the rune.

---

## Sources

- [League of Legends Wiki: Rune (League of Legends)](https://wiki.leagueoflegends.com/en-us/Rune_(League_of_Legends)): paths, rows, shards and their recent changes.
- [League Wiki: Deep Ward](https://wiki.leagueoflegends.com/en-us/Deep_Ward), [Template: Axiom Arcanist](https://wiki.leagueoflegends.com/en-us/Template:Rune_data_Axiom_Arcanist), [Template: Grisly Mementos](https://wiki.leagueoflegends.com/en-us/Template:Rune_data_Grisly_Mementos), [Template: Absorb Life](https://wiki.leagueoflegends.com/en-us/Template:Rune_data_Absorb_Life); [VPEsports: Sixth Sense](https://vpesports.com/lol/runes/sixth-sense/): the 2025 runes.
- [esports.gg: Deathfire Touch and Stormraider's Surge return](https://esports.gg/news/league-of-legends/deathfire-touch-stormraiders-surge-returns-in-lol-2026-season-2/); [League Patch 26.9 notes](https://leagueoflegends.com/en-us/news/game-updates/league-of-legends-patch-26-9-notes); [RiftPatchNotes: Deathfire Touch](https://www.riftpatchnotes.com/lol/rune/deathfire-touch): the 2026 keystones.
- [Surrender at 20: Runes Reforged red posts (2017)](https://surrenderat20.net/2017/06/red-post-collection-runes-reforged.html); [Riot: /dev on the purpose of pre-game](https://nexus.leagueoflegends.com/en-us/2017/05/dev-on-the-purpose-of-pre-game/); [Riot: /dev update on Runes Reforged (2018)](https://nexus.leagueoflegends.com/en-us/2018/01/dev-update-on-runes-reforged/): Riot's goals and lessons.
- [Warcraft Tavern: Dragonflight talent rework and UI](https://www.warcrafttavern.com/wow/news/dragonflight-pre-patch-is-live-talent-rework-ui-overhaul-accessibility-improvements-more/); [Warcraft Secrets: Dragonflight talent design philosophy](https://warcraft-secrets.com/news/world-of-warcraft-dragonflight-talent-system-design-philosophy); [Icy Veins: talents UI and loadouts](https://www.icy-veins.com/forums/topic/66885-talents-user-interface-and-loadouts-in-dragonflight/).
- [Grim Dawn: Devotion guide](https://www.grimdawn.com/guide/character/devotion); [Crate forums: Devotion Picker](https://forums.crateentertainment.com/t/tool-grim-dawn-devotion-picker/155421); [Icy Veins: Last Epoch passive points](https://icy-veins.com/last-epoch/passive-points).
- [RPGHQ forum thread on talent-tree design](https://rpghq.org/forums/viewtopic.php?p=339102): the "Sphere Grid" critique of big trees.
- [Mado Hosting: Keystone Expansion for Pufferfish's Skills](https://www.mado-hosting.com/minecraft-mods/keystone-expansion-pufferfishs-skills); [Survival Instinct Skills](https://www.mado-hosting.com/minecraft-mods/survival-instinct-skills): Minecraft skill-tree mods.
- Hades, Hollow Knight, Diablo IV, Destiny 2 and League's client screen: from play, not a source.
