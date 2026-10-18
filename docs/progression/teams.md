# Teams: who shares what

> **Designed and decided 2026-10-18 ([ADR-0023](../decisions/0023-teams-are-ftb-teams-parties.md)).**
> Sanchit chose all three recommendations in §8. **Nothing is built yet**: until T1 lands, the game
> behaves as it does today (the whole server is one team). Facts about FTB Teams below were read from
> the installed jar (`2101.1.11`), not remembered. Every number is a placeholder.

## 1. In short

The vision says "co-op first, solo viable" and "team progression is shared at the team level"
(`vision.md`), but today **the team is the whole server** (`TeamGrowthState`). That is fine for four
friends, and wrong for a server with two groups on it: one group's Warden kill pays everyone.

**Proposal: progress has three owners, and each system names which one it belongs to.**

| Owner | What it is | Who shares it |
|---|---|---|
| **The world** | Things built in the world: gates lit, the mark's glyphs | Everyone on the server, any team, late joiners included |
| **Your team** | Things the group does: bosses, quests | Your party's members, now and later |
| **You** | Things you do alone: discoveries, your tree, your runes, your gear | You |

A team is an **FTB Teams party** (installed). A solo player is a team of one: FTB Teams gives every
player a personal team, and we use it the same way as a party. **We build no team screen, no invites and
no chat**: FTB Teams has them.

## 2. Every shared thing, and its owner

| Thing | Owner | Today | Change |
|---|---|---|---|
| Gate lit (3 gates, 2 points each) | **World** | Every player online, and on login (`GateAdvancements`) | None. It is a fact about the world, so a second team arriving at a lit gate has the points too |
| The mark's glyphs | **World** | Follows the gateway network | None |
| Boss first kill (6 bosses, 3 points each) | **Team** | Whole server | The killer's team, per §3 |
| Quest rewards | **You** (progress is the team's) | Not built | Each member claims their own, §6 |
| Discoveries (11, 1 point each) | **You** | Personal | None |
| Skill tree, rune page, gear | **You** | Personal | None |
| Rune slots opened by milestones | **You**, from the above | Count of the player's own boss and gate advancements | None, they follow whatever the player holds |
| Allies for runes (Challenge now, Guardian-style runes later) | **Team** | Any nearby player | Teammates, and your pets, §5 |
| Share-air rescue | **Anyone** | Any player | None: a rescue should not ask who you are |
| Return-to-Earth location | **You** | Per player | None |

## 3. Bosses: the killer's team earns it, once

- A boss counts when vanilla credits the kill to a player (the last hitter in the last few seconds),
  unchanged from `growth.md` §3.
- The **killer's team** earns the milestone. Every member gets its points: online now, or on next login.
  A teammate who was far away still gets it; a stranger standing next to the fight does not.
- A team earns each milestone **once**. Five players do not make it five times.
- A solo player's team is themselves, so solo play loses nothing.

## 4. Joining, leaving, and late joiners

**A party holds the union of its members' milestones.** That one rule settles every case:

| Event | What happens |
|---|---|
| **You join a party** | You get every milestone the party holds. The party gets every one you hold. Nothing you earned is lost |
| **You leave a party** | You keep everything you hold: points, nodes taken, runes. Later milestones go to your new team only |
| **A party disbands** | Each member keeps what they hold, as above |
| **A new player joins the server** | A team of one, with the gates the world has lit. They join a party to catch up on bosses |
| **A party has no members online** | Nothing is lost; the milestones sit in the team's saved state and in each member's advancements |

Why points are never taken away: nothing in growth takes points back (death costs nothing), and a
tree you built should not collapse because a friend left. The cost is that someone can join a
stranger's party to collect bosses they did not fight. The party's owner controls invites (FTB Teams
parties are invite-only unless set to free to join), and it is exactly the catch-up `vision.md` asks for:
"must avoid turning late-joining players into spectators".

**The gear half of the late-joiner gap** (`todo.md`) is not solved by points. §6 has a way, to be checked.

## 5. What else asks "who is on my team?"

- **Allies for runes.** Today only Challenge looks (`rune-system.md` §4.4). It should mean teammates
  and your own pets, found on the event that needs them and never by scanning every tick (ADR-0007).
  Players of other teams are not allies.
- **Friendly fire.** Our combat is built for big numbers and area effects. **Proposal: teammates cannot
  hurt each other** (a server setting, on by default), because Whirlwind-sized hits in a crowded fight
  would otherwise be a way to kill a friend by accident. PvP between teams stays whatever the server's
  `pvp` setting says.
- **The HUD** can show teammates' health in the corner, League-style, and an oxygen warning on a
  teammate you could rescue. That needs one small synced list of "who is on my team", sent when it
  changes, never per tick. It is the HUD session's to draw (`plans/session-prompts/6-hud-overhaul.md`);
  this design provides the list.
- **Not team-bound:** share-air, the mark, return locations, who can use a gate.

## 6. Quests

FTB Quests keeps progress **per team**: a task one member finishes counts for all, and every member
claims their own rewards. That fits the owners in §1 without code:

- A quest chapter can award skill points by running `/advancement grant` for an advancement that
  growth counts, so quests pay points exactly as a boss kill does, from data. Quest points are
  **personal** points (the claimant's own), and the first chapters should be priced so the v1 budget
  (35 points for a 56-node web) is not simply raised.
- **Gear for late joiners, to check:** if FTB Quests lets a new party member claim the rewards of quests
  the party finished before they joined, a chapter that rewards "a kit for this world" gives every late
  joiner the gear catch-up with no code. Not yet confirmed in a game; if it is false, the fallback is a
  small supply station per world, which is a separate design.
- Quests reward going somewhere; they never gate a path (ADR-0018).
- The quest book opens on **J** and the FTB team screen reaches the player through FTB's own
  inventory sidebar. Whether the team screen also gets a key is left until players ask.

## 7. How it is built

**A contract in `core`, one provider, no foreign dependency in Tier 1** (ADR-0003, ADR-0011):

1. **`ascension-core` `api.team`.** A provider registry in the same style as `AtmosphereRegistry`:
   who is on a player's team (a team id, the members of a team, whether two players share one) and
   an event when a player's team changes. **The default provider says everyone is one team**, so every
   Tier-1 module still loads and is testable with `core` alone (architecture rule 5), and a server
   without FTB Teams behaves exactly as today.
2. **`ascension-compat-ftbteams` (new Tier-2 jar).** Registers the provider over FTB Teams' own API:
   `TeamManager.getTeamForPlayerID` (works for an offline player), `Team.getMembers`, and the events
   `PLAYER_CHANGED`, `PLAYER_JOINED_PARTY`, `PLAYER_LEFT_PARTY` and `PLAYER_LOGGED_IN`. The team id is
   FTB's team id, so personal teams, parties and server teams are handled the same way.
3. **`ascension-combat`.** `TeamGrowthState` becomes a map from team id to earned milestones, kept on
   the Overworld as it is now. A shared milestone goes to the killer's team and its online members; a
   login, or the join event, hands over the milestones the player is missing (§4). The old whole-server
   set becomes the starting set of every team that exists when parties turn on, so an existing world
   loses nothing. Allies and friendly fire read the provider.

| Step | What | Checked by |
|---|---|---|
| **T1** | The `core` contract, the default provider, growth per team, unit tests for the union rule | Dedicated server, the dev client alone: nothing changes (one team) |
| **T2** | The FTB Teams jar, join and leave handling | Dedicated server, **two clients**: a solo player's kill pays only them; the second joins their party and gets the points; leaves and keeps them. Restart the server: same |
| **T3** | Allies and friendly fire | Two clients: a rune effect reaches a teammate and not a stranger; a teammate's hit does no damage |
| **T4** | The team list for the HUD | The HUD session reads it |
| **T5** | Refactor pass (ADR-0008) | No new features |

**Two-client testing is still owed** (it has been since M1). T2 is where it stops being optional.

**Performance:** nothing ticks. A team lookup is a map read; the team list is sent only on a change;
allies are found on the event that needs them, within a radius (ADR-0007).

## 8. Decisions

**Answered by Sanchit, 2026-10-18:**

1. **Bosses pay the killer's team, not the whole server.** Gates stay the world's.
2. **Joining gives you the party's milestones and gives it yours; leaving keeps everything you hold.**
3. **Teammates cannot hurt each other** (a setting, on by default).

**Defaults I will use unless he says otherwise:** no party size limit (`max_party_size` 0), limited
lives off (FTB's hardcore-style option), teams of other parties are not allies, and a one-time message
on first login pointing a new player at the team screen.

## 9. Not in this design

- **Claims and base protection** (FTB Chunks): what a co-op server wants next. It adds a map that
  overlaps Xaero's, so it is a separate decision.
- **The quest lines themselves.** This page says who owns what; the chapters come next.
- **Allied parties** (FTB Teams can ally two teams): their members are not allies in v1.
- **A per-world supply station** unless §6's check says it is needed.
- `architecture.md` lists an `ascension-progression` module for "team capability state, gating". ADR-0018
  removed the gating, and §7's provider replaces the team half, so that module stays unbuilt.

## 10. To verify in a game

1. FTB Quests: can a player who joins a finished party claim its past rewards?
2. FTB Quests: does a reward command run as the claimant (`@p`)?
3. FTB Teams' events fire on a dedicated server for a player who is offline at the time of the change.
4. Where the FTB team screen is reached from, with our controls (inventory sidebar, a command).
