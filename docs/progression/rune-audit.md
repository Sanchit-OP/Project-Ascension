# Rune audit (2026-10-22)

> Sanchit: "next will be the rune page. We need to audit it one by one, and see what to be changed, reworded or replaced to make the
> game good to feel." Done the way the enchantments were (`enchantments.md` section 12): every one of the 45 runes read from its data
> file, its code and its tooltip, each given a verdict and a proposal. **Nothing is changed yet**: the proposals are numbers and wording
> waiting for his word, and four decisions are his (section 4). Design reference: [`rune-system.md`](../technical/rune-system.md).
>
> **Applied the same day (Sanchit's answers).** Ambush: primed strike, +100% and +2% a Lethality point (25 Lethality is his +150%), ignores 25% of Armor and +1% a
> point; Momentum from the damage before Armor; Revitalize kept (healing builds) and **Overheal** built as a keystone in **Cat's Eye's** place (he called creepers and
> phantoms a gimmick); Riposte's window 1 s; growing runes cap 20; Vein Breaker and Cadence buffed; **Predator** (Lethality is damage) replaced **Silent Step**; Arc
> redesigned (charged until the fight ends, four strikes a bolt); Scholar a small buff. What was built is in `rune-system.md`; the Curios branch is not merged, so the
> shield and off-hand conflict stands until it is.

## 1. The verdicts at a glance

| Verdict | Count | Runes |
|---|---|---|
| **Keep** | 15 | Vein Breaker, Killing Surge, Giant Slayer, Deadeye, Silent Step, Drop In, Vanish, Scholar, Unweave, Quickening, Pack Leader, Cat's Eye, Unbowed, Riposte, Challenge |
| **Tune** (the mechanism is right, the number is stale) | 18 | Conqueror's Resolve, Cadence, Second Wind, Blooded, Relentless Strikes, Pounce, Finishing Blow, Overkill, Trophy Hunter, Starfall, Arc, Attunement, Clarity, Siphon, Gathering Charge, Brace, Plating, Deep Roots |
| **Rework** (a different mechanism) | 3 | Ambush, Momentum, Revitalize |
| **Shards** | 9 | Keep; one number (Health +2 to +3) |

Nothing needs throwing away. The runes are sound ideas with the wrong sizes, and three that need a new mechanism to matter at all.

## 2. Why the numbers are stale

The runes were written on 2026-10-15 to 17, for a player with a 7-damage sword and 20 health. Since then the weapons are 12 to 34 damage
(2026-10-22), a kitted player has about 100 to 150 health, and the skill tree gives +13 Attack Damage. **Reference player used below: 40
Attack Damage, 1.8 hits a second, 120 health, against the Warden (80 Armor, 40 Magic Resist) and the Wither (180 Armor, 110 Magic Resist).**
Against it, measured with the same Armor and Magic Resist curve the game uses:

| Rune | Adds now (single boss, % of plain damage) | Proposed |
|---|---|---|
| Starfall | **+2.2% (Warden), +2.7% (Wither)** | +14% / +17% |
| Arc | +5.9% / +7.3% | +16.5% / +20% |
| Relentless Strikes (4 stacks) | +7.5% | +16% |
| Conqueror's Resolve (10 stacks) | +12.5% | +20% |
| Cadence (third hit) | +7.5% of that one hit, plus Exposed +10% | +30% on that hit, Exposed +15% |
| Momentum (heals) | 2.1 / 1.0 health a second | 5.4 a second |
| Second Wind | 1 health a second (0.8% of max) | 3 a second |

**The yardstick for the fix:** a combat **keystone** should add about 15 to 25% of its stat at full effect; a **minor** 5 to 10%; a
**growing rune** about 10 to 15% at its cap; a **shard** about one skill node (`growth.md` section 8). Where a number is flat (+3 damage,
+0.75 a stack, 1 health, up to 6 splash) it goes stale again the next time the weapons move, so the proposals turn them into **percentages
of Attack Damage or of maximum health**, which follow the gear.

## 3. Findings that cut across the runes

1. **Flat numbers.** Eight runes carry a flat figure that is now a rounding error (section 2). Convert to percent.
2. **Aetherwright's magic pool is too small to build on.** Only the weapons that do magic (Ragefang, Darken Blade, Heartwood, the
   Sunderer's half, Sweeping Edge) feed it, perhaps a quarter of a typical build's damage, so Attunement (+12%), Clarity (+8%), Gathering
   Charge (up to +12%) and Siphon are worth about 2 to 4% of total damage between them. The two keystones are meant to be the pool for
   "any weapon" and are the two smallest keystones in the game (+2% and +6%). **Rescale the keystones and the minors start to matter.**
3. **Starfall's cooldown can empty itself.** Every magic hit shortens its 10 second cooldown by a second, and Ragefang lands a magic hit on
   every swing at up to 7.5 a second: the cooldown is gone in 1.3 seconds, so rescaling Starfall without fixing this would hand Ragefang a
   star every second and a half. Shorten at most once a second, or give the cooldown a floor.
4. **Two keystones do nothing in the fights that matter.** Ambush needs an enemy that is not targeting you (every boss is) and its +40% only
   improves a hit that already kills a vanilla mob; Arc and Starfall are too small. A keystone should work against a boss.
5. **Heals that take a share of damage dealt are weak against armour.** Momentum and Siphon heal a share of the damage after Armor or Magic
   Resist, and a boss leaves 29 to 57% of a hit, so they heal a third as much as the tooltip suggests. Heal from the damage **before** the
   cut (Momentum) or raise the share (Siphon).
6. **Shields collide with the off-hand legendaries.** Brace's shield trigger and Riposte need a shield in the off hand, which is where Zonia's
   Hourglass and Sterak's Harness go. A Bulwark player chooses between them. Brace still triggers on a big hit without a shield, so only
   Riposte is really hurt (section 4, decision 3).
7. **The growing runes fill in the first hours.** Their cap is 12 kinds of enemy and Earth's nights alone hold more than 12, so the reward is
   used up early and then sits there. A cap of 20, with the same total at the cap, spreads it over the whole alpha (Moon, Nether, End mobs).
8. **Feedback is mostly good.** Fourteen runes with running state have a chip beside the hotbar, and the on-hit keystones have a sound and a
   particle (Ambush, Cadence, Deadeye's chime and "Headshot!", Starfall's falling streak, Arc's sparks, Brace's anvil and shockwave).
   The silent ones are the conditional damage bonuses (Finishing Blow, Giant Slayer, Pounce): they work and nobody can tell. One quiet cue
   the first time the condition holds in a fight would fix it. Not a priority.
9. **Wording.** Arc says "every fourth hit throws lightning" but four hits charge it and the fifth throws it. Plating's trigger is "a hit of
   4 or more", which is every hit there is. Giant Slayer's two lines are awkward. The rest reads fine.

## 4. Decisions that are Sanchit's

1. **Ambush.** Three options, in order of my preference. **(a) Rework it into a primed strike:** your next hit after 6 seconds of not
   hitting anything, or after a dash, blink or pearl, deals +150% and ignores half the target's Armor, and the "not targeting you"
   bonus stays on top. It works against a boss (you back off, come in, burst) and rewards the hit-and-run that Reaver is. **(b)** Keep it as a
   mob hunter and raise it to +100% (+50% on a crit): it still does nothing against a boss. **(c)** Replace it with a new keystone.
2. **Momentum and Revitalize.** Both are small today. Proposed: Momentum heals from the damage before Armor, 1.5% a stack up to 5 stacks
   (5.4 health a second at the reference player); Revitalize becomes **Overheal**: healing that would go past full health becomes a shield up to
   25% of maximum health (the Sunderer's 270 heal, omnivamp at full health), and shields you receive are 20% stronger. Say if you would
   rather keep Revitalize as a heal multiplier.
3. **Riposte and the shield.** Keep it as it is (a Bulwark player picks a shield or an off-hand legendary), or give it a shield-free
   trigger (a hit taken within a quarter second of a dash or a jump back). Keeping it is simpler.
4. **Growing runes.** Cap 12 or 20 (finding 7). I would take 20.

## 5. Vanguard: keep hitting

| Rune | Does now | Verdict | Change |
|---|---|---|---|
| **Conqueror's Resolve** (keystone) | A stack a second in a fight, up to 10: +0.5 Attack Damage and +1% omnivamp each. Gone when the fight ends. | **Tune** | **+2% Attack Damage a stack** (20% at 10) instead of +0.5 flat; omnivamp stays 1%. Blurb: "Every second in a fight: +2% damage and +1% healing, up to 10 stacks." |
| **Cadence** (keystone) | Three fully charged hits on one enemy within 4 s: the third deals +3 and Exposes it (+10% damage taken from everyone, 5 s). | **Tune** | Third hit **+30%** of the hit (was +3); Exposed **+15% for 6 s** (was 10%, 5 s). Blurb: "Land three fully charged swings in a row on one enemy: the third hits 30% harder and Exposes it, so everyone deals 15% more to it for 6 seconds." The Exposed amplifier is the real keystone and helps the whole team. |
| **Vein Breaker** (world) | Sneak and mine: the connected ore or logs break with it, up to 32. | **Keep** | Works and is a good "hard to give up". FallingTree is in the alpha.2 pack and fells trees for everyone, so the logs half is now redundant; harmless. Option: ore only (and glow lichen and amethyst), leaving trees to FallingTree. |
| **Second Wind** (minor, sustain) | In combat and below half health, +1 health a second. | **Tune** | **2.5% of maximum health a second** (3 a second at 120). Same wording with "2.5% of your maximum health". |
| **Momentum** (minor, sustain) | Chaining hits within 2 s builds up to 5 stacks; each heals 1% of the damage dealt. | **Rework** | Heal from the **damage before Armor**, **1.5% a stack** (7.5% at 5), so a boss's armour does not shrink it. About 5.4 health a second, was 1 to 2. |
| **Blooded** (minor, grows) | +0.5% omnivamp for each new kind of enemy, cap 12 (+6%). | **Tune** | Cap 20, **+0.75% a kind** (15% at the cap). |
| **Relentless Strikes** (minor, damage) | Hits build up to 4 stacks (fade after 3 s): +0.75 damage each. | **Tune** | **+4% damage a stack** (16% at 4) instead of +0.75 flat. |
| **Killing Surge** (minor, damage) | A kill: +20% attack speed for 3 s. | **Keep** | Visible (a chip), simple, scales. Optionally 25% for 4 s. |
| **Giant Slayer** (minor, damage) | +5% damage against an enemy with 10% more max health than you, rising to +15% at double. | **Keep** | Reads as "+15% against bosses" in practice (every boss has 8 to 40 times your health). Reword: "+5% damage to an enemy with more health than you, rising to +15% at double. Bosses count." Consider 20%. |

Row verdict: after the changes row 1 is a real choice (Relentless for steady fights, Killing Surge for crowds, Giant Slayer for bosses).
Row 0 is a choice between a heal that scales with health (Second Wind), one that scales with how fast you hit (Momentum) and growth (Blooded).

## 6. Reaver: strike first, finish fast

| Rune | Does now | Verdict | Change |
|---|---|---|---|
| **Ambush** (keystone) | +40% (+20% more on a crit) to an enemy that is not targeting you. | **Rework** | Decision 1. As it stands it does nothing against a boss and nothing useful against a mob a sword already kills. |
| **Deadeye** (keystone) | Ranged hits: +2% a block past 10 up to +50%, and +50% on a headshot, with a chime, a flash and "Headshot!". Works for any mod's projectile. | **Keep** | The best feel in the game: you know when it fires. It is a bow keystone, and with the bows at 22 to 24 Attack Damage that is real. Measured in a client already (x1.77 headshot at 23 blocks). |
| **Silent Step** (world) | You make no vibrations (sculk sensors and shriekers never hear you); sneaking, mobs notice you only within 6 blocks. | **Keep** | Cancels every game event you cause, which also silences other mods that listen to them: one thing to watch when a mod misbehaves. |
| **Pounce** (minor) | Critical hits (hitting while falling) deal x1.75 instead of x1.5. | **Tune** | **x2.0**. Reword: "Hits you land while falling deal double damage (instead of one and a half times)." |
| **Drop In** (minor) | 4 s of +10 Lethality and +10% Armor Penetration after a dash, a blink, a pearl or a fall of 4 blocks. | **Keep** | Worth about +14% damage against the Warden and +21% against the Wither for the window; it opens a lot for the Wraithblade and Galeforce. Optionally 5 s. |
| **Vanish** (minor) | A kill of an enemy: 2 s invisible, nearby mobs drop you, 10 s cooldown. | **Keep** | An escape tool, fixed after Sanchit's test on 2026-10-18. Niche but clear. First candidate to replace if a fresher row-0 option is wanted. |
| **Finishing Blow** (minor) | +12% damage to an enemy below 40% health. | **Tune** | **+20%** below 40%. The last 40% of a boss is a long phase, so this is real. |
| **Overkill** (minor) | The damage beyond a kill splashes to the nearest enemy within 4 blocks, up to 6. | **Tune** | Up to **25**, radius **5**. Up to 6 is nothing now. |
| **Trophy Hunter** (minor, grows) | +1 Lethality per new kind of enemy, cap 12. | **Tune** | Cap 20, **+1 a kind** (20 at the cap, about +14% against armour). |

## 7. Aetherwright: make the magic do more

| Rune | Does now | Verdict | Change |
|---|---|---|---|
| **Starfall** (keystone) | A fully charged hit or any magic damage calls a star: 4 + 20% of Attack Damage as magic in 2 blocks after 1 s; 10 s cooldown, each magic hit shortens it by 1 s. | **Tune** | Damage **150% of Attack Damage** (60 at 40; no flat part); radius **3**; cooldown **8 s**; and the shortening limited to **once a second** (finding 3). About +14 to +17% against a boss, from +2 to +3%. |
| **Arc** (keystone) | Four hits charge the weapon, the fifth throws lightning: 3 + 15% of Attack Damage as magic, jumping 3 times within 5 blocks. An item's active charges it. | **Tune** | **50% of Attack Damage** (no flat part); **three hits charge it, the fourth throws it**; 3 jumps. About +16 to +20% on one target, and far more on a crowd. Reword: "Every third hit charges your weapon; the next one throws lightning that jumps between enemies." |
| **Scholar** (world) | +50% experience; enchanting costs a level less (never below 1). | **Keep** | A good world keystone: respec costs 20 levels and every rune change 3, so +50% experience is felt. |
| **Attunement** (minor) | All your magic damage +12%. | **Tune** | **+20%**, which is worth having once the keystones feed the pool. |
| **Unweave** (minor) | +15% Magic Penetration. | **Keep** | Against the Wither's 110 Magic Resist it is about +12% magic damage. |
| **Clarity** (minor) | Above 70% health: +1 Attack Damage and +8% magic damage. | **Tune** | **+8% Attack Damage and +10% magic damage** while above 70%. |
| **Siphon** (minor) | Heal for 10% of the magic damage you deal. | **Tune** | **25%**. Today it heals a few health a second at best. |
| **Quickening** (minor) | Item abilities recharge 20% faster. | **Keep** | Quietly one of the best minors: it helps the Wraithblade, Galeforce, the Sunderer and Heartwood. Optionally 25%. |
| **Gathering Charge** (minor, grows) | +1% magic damage per new kind, cap 12 (+12%). | **Tune** | Cap 20, **+1.25% a kind** (25% at the cap). |

Row verdict: the path is a build around the magic weapons, which is right; what was missing was a keystone big enough to build around.

## 8. Bulwark: stand in front

| Rune | Does now | Verdict | Change |
|---|---|---|---|
| **Brace** (keystone) | A shield block, or a hit of 25% of max health: +40 Armor and Magic Resist for 2.5 s, then a shockwave of 3 + 10% of Armor as magic in 4 blocks; 12 s cooldown. | **Tune** | **+80 Armor and Magic Resist for 3 s**, shockwave **5 + 40% of the braced Armor** (about 85 at 200 Armor), cooldown **8 s**. Today it is a 24% cut for 2.5 s in every 12 (a 5% average) and a shockwave of about 17. |
| **Pack Leader** (keystone) | Tame wolves, cats and parrots share half your Armor and Magic Resist, deal +3, heal a quarter of what you heal; one pet saved at 1 HP every 3 minutes. | **Keep** | The totem-style save is lovely. Raise the +3 to **+6** damage. Niche by design. |
| **Cat's Eye** (world) | Creepers flee within 6 blocks; phantoms never swoop. | **Keep** | Removes the two most annoying mobs; the gunpowder cost is a real price. |
| **Plating** (minor) | After a hit of 4 or more: the next 3 hits within 3 s take 30% less; 12 s cooldown. | **Tune** | The trigger is a hit of **8% of max health** (4 is every hit), then **40% less for 4 hits within 4 s**, cooldown **8 s**. Needs a small code change (a fraction instead of a flat trigger). Reword: "After a hit of 8% of your health or more, the next 4 hits within 4 seconds take 40% less." |
| **Unbowed** (minor) | Harmful effects last 30% shorter; 40% less knockback. | **Keep** | Does its job and is easy to read. |
| **Deep Roots** (minor, grows) | +0.5 Health per new kind, cap 12 (+6). | **Tune** | Cap 20, **+1.5 Health a kind** (30 at the cap). +6 is two skill nodes. |
| **Riposte** (minor) | Raise the shield within 0.5 s of a hit: the attacker is stunned 1 s (not bosses) and your next hit is a critical. | **Keep** | Decision 3. Fine as a skill rune; the stun does nothing to a boss, the crit still does. |
| **Revitalize** (minor) | Heals and shields you receive +10% (+20% under 40% health). | **Rework** | Decision 2: **Overheal**, healing past full health becomes a shield up to 25% of max health; shields received +20%. |
| **Challenge** (minor) | Mobs you hit keep attacking you for 4 s instead of allies, pets or villagers. | **Keep** | A team rune; works on bosses. Solo it is quiet, which is honest. |

## 9. The shards

| Shard | Now | Verdict |
|---|---|---|
| Offence: Attack Damage / Attack Speed / Move Speed | +1 / +8% / +3% | Keep |
| Flex: Attack Damage / Armor / Magic Resist | +1 / +5 / +5 | Keep |
| Defence: Health / Armor / Magic Resist | **+2** / +5 / +5 | Keep; **Health to +3**, to match one skill node (+3 Health) |

The skill-tree raise (+1 Attack Damage, +5 Armor, +5 Magic Resist a small node) happened to land on the shards' figures, so each shard is
about one node, which is what the design asked (three shards = one boss's points).

## 10. How to apply this

1. **Numbers and wording** (the Tune verdicts, the Keep wording): every one is a value in a rune's JSON file or its tooltip, so it is data only,
   plus three small code changes: Plating's trigger as a fraction of health, Starfall's shortening limit, and percent-of-Attack-Damage
   for Conqueror's, Relentless Strikes and Clarity (they add a flat amount today; a percent is an `ADD_MULTIPLIED_TOTAL` modifier).
2. **Reworks** (Ambush, Momentum, Revitalize): each a short class, after the decisions in section 4.
3. **Proof:** the dev server checks (`damage-checks.txt`) can read each number off a stand-in player, as they did for the weapons and the skill
   tree; the effects themselves want one client run: Starfall and Arc with a Ragefang (finding 3), Cadence's Exposed on a boss, Brace on a
   Warden's hit, Ambush's new trigger. None of the Reaver, Aetherwright or Bulwark effects has been played on a real boss yet (only Vanguard and
   Deadeye and Vanish were), so the first client run after the changes is also their first real test.
4. Nothing reaches players until alpha.2 (the combat jar, the pack and the server).
