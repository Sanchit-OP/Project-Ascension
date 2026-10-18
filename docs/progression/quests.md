# The quest book

> **Status (2026-10-18): Q1 and Q2 BUILT, loaded by the real FTB Quests on a dedicated server, and seen in a
> client once by the scripted run: it works, with a few things still unconfirmed** (sections 10 and 12). Written after Sanchit asked for quest pages that teach, reward and point onward
> without giving the lore away, a special **Planets** page that looks like a solar system and shows where you
> are (also while you are in space), and a book that looks like our pack. His four answers are in section 9.
> **Update, same evening: Q3 (the refactor pass) is done and Q4 (the five remaining pages) is built: 7 pages, 85 quests,
> loaded by the real FTB Quests on a dedicated server (10 checks, 0 failures). The new pages have not been seen in a client.**
> What the book taught us about the whole game is in [`journey.md`](journey.md); where this work stopped is section 13.
> Sections 2 to 4 are what FTB Quests already does (read from the installed jar, `ftb-quests-neoforge-2101.1.36`),
> so we only build what it cannot.

## 1. What the book is for

Three jobs, in this order:

1. **Teach.** Each page explains one thing the game does that a player would not guess: the valve, the
   gold ward, the orrery, the growth tree. One idea per quest.
2. **Reward going somewhere.** A quest pays for doing a thing. It never blocks a path (ADR-0018: no world
   is required), so a player who skips the book loses presents, not progress.
3. **Point onward.** Every quest ends by naming the next practical step, so a player never wonders "what
   now". The Planets page is the biggest signpost.

And one rule above all of them: **the book never explains why.** `lore-spread-proposal.md` rule 8 says "a
ledger, not a quest log": it records what the player has actually found, and shows worlds they have only heard
of as rumours. The quest book is that ledger.

### Keeping the lore out (the writing rules)

| Rule | Example |
|---|---|
| **Say what and where, never why.** | "The Archive on the Moon has a reader. Bring a lamp." Not who built it or why. |
| **Only use what the player has already been shown in the game.** A quest can name a tablet only after the player has read it. | Quest "Read the first tablet" is hidden until the Archive is entered (an advancement). |
| **Rumours stay unnamed.** A world the player has not reached is a faint ring on the chart and a line of text, never a name or a purpose. | "Something warm, far to the north-east." |
| **No answers to the questions in `what-the-warden-keeps.md`.** The book may *point at* the Warden, the mark or the rot. It never says what they are. | "The mark on your hand steps when a gate lights. Watch it." |
| **Hints are practical.** They tell the player a tool or a place, not a reason. | "Gold turns the rot away" is a rule of the world the player can see; *why* gold is not said. |
| **A word list is enforced.** `pack/quests/guarded-words.txt` lists terms that reveal the story; the build fails if quest text uses one. | Same idea as `check-data.py` failing on a wrongly spelled discovery. |

Voice: short, second person, a field notebook. Plain words. Humour is allowed, mystery is not explained.

## 2. What FTB Quests already gives us

Read from the jar, so nothing here is a guess about the mod:

| Need | Already there |
|---|---|
| Quests that follow what the player does | **Task types:** item, kill, advancement, dimension, biome, structure, location, stat, XP, observation, fluid, energy, checkmark, stage, custom. Our tablets, gates and milestones are advancements, so the advancement task covers them. |
| Hiding the future | Per quest: hide until dependencies are *visible* or *complete*, invisible until completed, hide text until complete, **hide details until startable**. This is how rumours work without code. |
| Rewards | Item, **choice of items**, random, loot table, XP, command (runs with `{p}` or `@p` for the claimant), advancement, toast, stage. |
| Team progress | Per FTB Teams party, which is our team model (ADR-0023). Each member claims their own rewards. |
| Layout | Free x/y placement, quest shapes, dependency lines, chapter groups, a per-quest guide page. |
| **Chapter images** | Any picture placed anywhere in a chapter with size, rotation, colour and alpha, **shown only once a chosen quest is done**, and optionally **clickable** (open a quest, run a command, a custom event). This is what makes the Planets page possible. |
| Looks | The whole screen is a **theme**: colours, backgrounds, icons, dependency lines and **quest shapes** are read from `ftb_quests_theme.txt` and `textures/shapes`, and the loader stacks every pack's copy (later wins). Our own mod can ship it. No mixin. |

What it does not give us, and so what we build, is in section 5.

## 3. The shape of the book

A cover page, then one page ("chapter") per world, then the pages that follow the player rather than a place.

| Page | Quests (first pass) | What it teaches |
|---|---|---|
| **Planets** | the star chart, section 5 | where you are, where the worlds are, what you have found |
| **First Days** (Earth) | 8 to 10 | gold matters, the Deep Dark, the Warden's tendril, the Earth gate's keystone |
| **The Road Up** (Earth) | 10 to 12 | titanium, the propeller bearing, an airship, tanks, the valve, a sealed room |
| **The Moon** | 12 to 14 | landing, the Archive and its reader, helium, the citadel and its key, lighting the gate |
| **The Rot** (Planet 3) | 12 to 14 | the gold ward, the Forge, netherite, the Monstrosity's key |
| **Gear and Growth** | 10 to 12 | the character sheet, a build path, runes and the orrery, the growth tree, enchantments |
| **Together** | 5 to 6 | making a party, the map, sharing air, a kit for a late joiner |

About 60 to 70 quests for the first release. Counts are a target, not a rule; a page stops when its
subject is taught.

Four kinds of quest, each with its own quest shape, so a player reads the page at a glance:

| Kind | Shape | Used for |
|---|---|---|
| **Lesson** | small round star | teaches one thing, one easy task |
| **Goal** | star with points | a real piece of play (build, craft, reach) |
| **Landmark** | the diamond eye | a place or boss; the network's motif, kept to gates, planets and bosses |
| **Secret** | dim ring until found | optional discoveries; invisible until the player is near it |

## 4. Rewards

- **No quest gates a path, and quests do not raise the growth budget.** Growth's v1 budget is 35 points
  from bosses, gates and discoveries (`growth.md` section 1). Quests would not add to it; see the decision
  in section 9. Their rewards are things that help a player do the next thing: a stack of the right food,
  a lamp, a repair kit, a map entry, a first tool, the right enchanted book (enchantments are now
  stat sources).
- **Kits for late joiners.** A "kit for this world" reward on each world's landmark quest is the gear
  half of the late-joiner gap (`teams.md` section 6). Whether a player who joins a finished party can claim
  past rewards is still **to check in a game** (listed in `teams.md`); if it is no, the fallback is a small
  supply station per world.
- **Chart knowledge is a reward.** Finishing a clue reveals a world on the star chart (section 5). The
  book is how the player *earns coordinates*, which `exploration.md` and ADR-0010 already ask for.
- **Choices over piles.** Where a reward is gear, it is a choice of one from three, so a build is a
  decision and a team of three gets three different things.

## 5. The Planets page

### What the player sees

A star chart in the same night-sky-in-a-brass-frame look as the orrery and growth screens. Earth sits at the
centre (space is Earth-centred: `worlds-api.md`), the other worlds sit at the true **bearing** they are at
in the space dimension, and the distance is drawn on a gentle curve so the near worlds are not crushed
(rings show the true distance every 5,000 blocks). Each world is a lit sphere, the same ones the skies and
space already draw, with its colour from the planet's data.

| State | Looks like | How it is reached |
|---|---|---|
| **Unknown** | nothing | the default |
| **Rumour** | a faint ring and one line, no name | a clue (a tablet, a conversation) |
| **Known** | the sphere, named, with the page for that world one click away | a quest or the first landing |
| **Visited** | brighter, with the gate lit or dark on its ring | landing there; the gate lighting |

All four are plain FTB Quests visibility (hide until a dependency is done), so adding a world is data, not code.
Clicking a planet opens its quest, which says in practical words what the world asks of you (terrain,
hazard, what to bring), and **nothing about why it exists**.

### "You are here"

Wherever the player is, the chart shows a marker:

- **On a world's surface:** the marker sits on that world.
- **In the space dimension:** the marker is at the player's real position on the plane, with a small
  heading arrow. This is the "see it between the planets" part, and it is exact because the space
  dimension's x and z *are* the chart's plane (`worlds-api.md`, "How space coordinates relate to world
  coordinates").

**How it is drawn without touching FTB Quests.** A chapter image is just a texture name. FTB's image icon
asks Minecraft's texture manager for that name, so a texture we register ourselves under the name is the one
that gets drawn. The chart is two chapter images, the static chart (made by our art script) and a clear
overlay of the same size on which our code paints the marker. The overlay is repainted only when the player
changes dimension, opens the book, or (in space) moves far enough to shift the marker a pixel, so there is no
per-tick work (ADR-0007). **Not yet seen working**, and the first thing to prove (increment Q2).

### Also in space, without opening the book

A small chart in the HUD's corner while the player is in the space dimension: the worlds they know, a marker
for themselves with its heading, and the distance to the nearest known world. It uses the same data and the
same drawing code as the book. Off in a world's surface; a key shows it larger. This is the HUD session's
element to build; the contract it reads is in section 6.

## 6. How it is built

**Three pieces, and none of them is foreign code in Tier 1** (ADR-0003, ADR-0011):

1. **`core`, a small chart contract** (`api.client`, next to the HUD's). Plain data: a body is an id, a
   lang key, a colour, a position on the plane and a state (unknown, rumour, known, visited); plus the
   player's own location. It also holds the **projection** (world position to chart position, the curve and
   the rings), which is pure arithmetic with unit tests. The default says no bodies, so every module still
   loads with `core` alone.
2. **`worlds`, the source.** It already knows every planet (`data/ascension_worlds/ascension_worlds/planet/*.json`).
   It tells the client the list when the player joins or the data reloads (one small packet, nothing per
   tick), and tracks which the player has found (advancements, so teams and persistence work like growth's).
3. **`ascension-compat-ftbquests`, a new Tier-2 jar.** It ships the **theme** (colours, frame, icons, shapes,
   Barlow for the numbers, as the HUD does), registers the overlay texture and repaints it, and reads nothing
   FTB-specific beyond what its API exposes. If FTB Quests is not installed the jar does not load and nothing
   else notices.

**The quest files are generated, not drawn by hand.** FTB Quests keeps a quest as an SNBT file with a random
hex id and its text in a separate language file. By hand that is brittle (rename a quest and a player's
progress is lost). So:

- The source is readable Python under `pack/quests/` (one file per page: id slug, title, text, tasks,
  rewards, dependencies, x/y), with a stable id derived from the slug.
- `tools/generate-quests.py` writes `pack/config/ftbquests/quests/**` (chapters, `lang/en_us.snbt`, reward
  tables). `tools/sync-pack-config.py` already copies pack config into the test instance.
- **The generator is also the check**, run by `./gradlew build` like `check-data.py`: every item,
  advancement, dimension and structure a quest names must exist; every dependency must resolve; no quest text
  may contain a guarded word; no two quests may share an id.
- The in-game editor is still fine for trying a layout, but the repo files are the truth.

**Art is ours, as ever** (`art-guide.md`): `tools/generate-quest-art.py`, seeded, writes the chart, each
page's backdrop (a large chapter image behind the quests: dusk and deepslate for Earth, grey and starlight for
the Moon, ember for Planet 3), the quest shapes, the frame and the icons. Palette from the rune art kit:
night sky `#04060c`, `#080c18`, `#0e1426`; brass `#3a3020`, `#7a6840`, `#c8b07a`, `#f0e0b0`. **Teal is kept for
the network** (gates and planets), as everywhere else.

## 7. Build order

Per ADR-0008 each step ends seen in a running client, and every third increment is a refactor pass. Following
Sanchit's rule, these are built as one batch and tested together when he says.

| | What | How it is checked |
|---|---|---|
| **Q1** | The compat jar, the theme and shapes, the art script, the generator and its checks, and **First Days** as the first page | generator tests; the quest file loads on a dedicated server with no log errors; the book opens in a client |
| **Q2** | The static chart, the **Planets** page and the **live marker** (self-contained in the compat jar; the core contract and packet moved to Q5, section 9) | projection unit tests; a client run that changes dimension and reads the overlay; a dedicated server |
| **Q3** | Refactor pass (ADR-0008 schedules one every third increment; due after Q2). **Done 2026-10-18.** | No behaviour changed, and it was proved: the generated files hash the same before and after, the 36 + 19 tests pass, and the dedicated-server check gives the same 6 passes. What moved: the three quest shapes are named once in `model.py` (every page needs them); the emitter's task and reward writers are two tables, so a new kind is one line; the validator is a small class with a method per concern instead of one 130-line function; the two dev checks share `CheckReport` (the trigger file and the PASS / FAIL / RESULT lines); the screen check's reach into Quests is `QuestBookDriver`; the overlay's pixel drawing is `ImageSurface`, and two tests that were in the layout test live with the code they test. The screen check's own plan was **not** changed (its three findings in section 12 are a fix, not a refactor); a client run is needed to see it still runs |
| **Q4** | The rest of the first pages (The Road Up, The Moon, The Rot, Gear and Growth, Together). **Built 2026-10-18** (section 13) | the generator's checks; a read-through for spoilers against the guarded words; the server check against the real game |
| **Q5** | The HUD chart in space (the HUD session) | a client run in the space dimension |

## 8. Risks

- **The marker trick is read from bytecode, not yet seen.** If FTB's image icon cached the texture before
  ours registered, the marker would not show. Fallbacks: register before the book can open (at resource
  reload), or ship the marker as one image per world with a reveal state instead of a moving dot in space.
- **A resource reload** can discard a registered texture; the overlay is registered again on every reload.
- **A new party member's past rewards** is unconfirmed (section 4).
- **Quest progress is per world.** Changing the book after a world is played can strand progress if a
  quest's slug changes, which is why ids come from slugs and slugs are never renamed.

## 9. Decisions (Sanchit, 2026-10-18)

All four taken as recommended:

1. **How the quests are written:** generated from readable files in the repo, with the checks above.
2. **Quest rewards and skill points:** items, kits and chart knowledge only. Quests add no skill points;
   growth keeps its 35.
3. **Worlds the player has not reached on the chart:** faint unnamed rings. (A world that is not built yet
   shows nothing; the rings are for Moon and Planet 3 before they are reached, and later worlds as they are
   laid out.)
4. **Build first:** the Planets page and its live marker, with the first page of quests.

Smaller defaults, taken unless he says otherwise: the book is called **The Ledger** in the cover text; the
in-space chart is shown automatically in the space dimension and larger on a key; quest points are personal
and kits are claimed by each member.

### What changed in the plan once building started

The first version of the chart is **self-contained in the compat jar**: the generator lays the page out from
the planet files (`data/ascension_worlds/ascension_worlds/planet/*.json`) and also writes a small layout file
(`chart.json`: the projection settings and where each planet is on the picture) that the client reads. The
marker needs nothing from the server, because the client already knows which dimension it is in and where it
stands. The `core` chart contract and the planet packet in section 6 are therefore **deferred to Q5**, when
the HUD chart is a second thing that needs the same data (`worlds-api.md`: a contract is added when two
things genuinely need it).

## 10. Status

| | |
|---|---|
| Design | this page; section 9 answered 2026-10-18 |
| **Built (Q1, Q2)** | `ascension-compat-ftbquests` (theme, three quest shapes `ascension_star` / `ascension_nova` / `ascension_eye`, night-sky background tile, the chart, three planet spheres, ring, halo, icons, the marker); the generator, validator, art tool and their tests; two pages: **First Days** (11 quests) and **Planets** (Earth, the Moon, the Nether, with rumour rings, halos and the marker) |
| Checked by machine | 19 Java tests (projection, painter, the layout file, and the generator's sample points replayed against the game's own projection), 36 Python tests (writer, ids, the real book, the chart, and a validator shown to catch each kind of mistake), `./gradlew build` runs the book check; **a dedicated server with the real FTB Quests loaded it first time: 1 chapter group, 2 chapters, 14 quests, 53 ids all known, 21 items all real, 0 failures** |
| **Seen (scripted client run, 2026-10-18)** | the theme wins the stack; the chart, rings, bearings and frame at book scale; a world's sphere, ring and dependency line; our pictures as quest and toast icons; the text at book width; the reveal order (Nether named after the Moon); and **the marker drawing in the real book** through the texture-by-name trick (section 12). Photographs in `design/quest-book/` |
| **Not yet confirmed** | the marker and the Moon's sphere in a clear, unobstructed shot (the quest panel covered them); the marker in space; the three quest shapes at normal size; how First Days looks as a whole page. Section 12 says why and what the next run should change |
| Open | the rest of the pages (Q4); the HUD chart in space with its core contract and packet (Q5); whether a late joiner can claim past rewards (`teams.md`); reward choices need reward tables, so the first pages give plain items |

## 11. Writing a page

A page is a Python module in `pack/quests/` with a `CHAPTER`, listed in `book.py`. The words and the
tools are in `tools/quests/model.py`; `first_days.py` is the pattern.

- **Quest:** a slug (never renamed once shipped), a place (x, y in book units; one unit is about one button),
  a title, a subtitle, text as a list of short lines, an icon (an item or one of our pictures), dependencies,
  tasks and rewards. `hide_until='complete'` hides it until what it depends on is done: use it for anything
  that would spoil the next step.
- **Tasks:** `checkmark()` (a lesson), `item(id, n)` (have it, not taken), `advancement(id)`,
  `dimension(id)`, `structure(id)`. Our advancements carry the discoveries, the gates and the bosses, so
  most real goals are an advancement. **Rewards:** `give(item, n)`; nothing that raises growth's 35 points.
- **Three kinds, three shapes:** lesson `ascension_star`, goal `ascension_nova`, landmark `ascension_eye`.
- **The voice:** section 1. A word in `guarded-words.txt` fails the build; add one the first time a draft
  comes near saying what it should only point at.
- **A world on the Planets page** is its planet file (`worlds`), a line in `planets.py` for what reveals it and
  what its page says, and a rerun of both tools; its place on the chart is computed.

## 12. The first client run (2026-10-18)

Recorded after Sanchit asked for the scripted run and then for nothing more than notes: **nothing below has been
fixed yet.** `QuestScreenChecks` opened `runes-test`, opened the real quest book in four states, photographed
it and read the marker's pixels back. Photographs and the check's own output are in `design/quest-book/`
(`run-2026-10-18.txt`). The run took about two minutes.

**Result: 14 checks, 4 failures, all four from one cause that is the check's, not the game's** (finding 1).
No error or warning from `ascension_*` or FTB Quests in the client log; the 96 exception-like lines are third-party
noise (mixin warnings for mods that are not installed, Cataclysm's missing subtitles, another mod's parse error).

| Passed | |
|---|---|
| The layout file was read | yes |
| Our texture is the one registered for the overlay image | yes |
| On Earth: a ring of marker around it, nothing in the middle | yes (alpha 255, 255, 0) |
| The oxygen-tank quest completes through `/ftbquests change_progress` | yes |
| In space at (1500, -2200): a point at the player's real place with its arrow ahead, and the Java projection agrees with the layout | yes (pixel 564, 435; arrow 546, 417; both alpha 255) |
| On the Moon: a ring around it, nothing in the middle; "The Moon" quest completes by standing there | yes |

**What the photographs show.** The theme is in effect: our dark starfield behind the page, brass borders, cream text,
the quest panel in our colours. The chart draws at book scale with its rings (the 20K and 40K labels read), N/E/S/W,
the bearing ticks and the brass frame. After the Moon is visited the Nether appears as a lava sphere inside a faint
ring with a dependency line running to it; before, only its ring. Our pictures work as quest icons (the Moon in
the "Quest completed" toast) and the chapter icons. Quests the player cannot start show a lock, ones with a reward to
claim show a red mark, and the sidebar button shows one too. The text wraps cleanly at the panel's width.
**The marker is drawn by the real book:** in the on-the-Moon shot a brass ring with a dark edge shows at the
panel's right edge, around the Moon's position and with the Moon's teal halo inside it, at the radius the code
draws it. So FTB Quests does ask the texture manager for our texture by name, which was the one idea in the design
that had only been read from bytecode.

**Findings** (all about the check, none yet about the book; none fixed):

1. **The "space at the Moon" stop is not in space.** The check teleports to the Moon's own position, which is inside
   the Moon's approach radius (320 blocks), so the game does what it is built to do and lands the player on the Moon
   (+1 skill point, "Landing on the Moon"). That caused the four failures, and the photograph named
   `planets-space-at-moon` was really taken on the Moon, with "The Moon" already done. A point more than 320 blocks
   from (4000, 0), such as (4600, 0), would be in space.
2. **The quest panel covers the subject.** `openGui(quest)` opens that quest's panel in the middle of the screen,
   which hides Earth, the Moon and the marker at the middle of the chart, and most of the First Days page. Every
   photograph is therefore a good picture of the theme and a poor one of the layout. The next run should close the
   panel (or open the chapter without a quest) before photographing, and could photograph twice, with and without it.
3. **The window did not resize.** The photographs are 1920 x 1080, not the 1600 x 900 the script asks for, so the
   window size call had no effect (the window may be maximised). Cosmetic.
4. **Not judged:** whether the three quest shapes read as star / nova / eye at normal size, whether the spheres are
   big enough on the chart (the Nether's is about 55 px across with a ring of about 95 px), and whether the First Days
   page reads as a whole. These need the clear shots from the next run, or Sanchit's own eyes.

**Left for when Sanchit says:** change the check for findings 1 to 3 and rerun for clean shots; his own look at the
book (J, then Planets); then the refactor pass (Q3, due), the remaining pages (Q4) and the HUD chart in space (Q5).

## 13. Where this work stopped (2026-10-18, late)

Sanchit asked for the refactor pass and then "complete the book, add all the things we want to add", and to learn from it
what the game still needs. Both are done to the point below; the context was full, so this is the checkpoint.

**Done and committed on `main` (not pushed):** the refactor pass (`99886ba`); the five pages and a revised First Days, the
chapter icons, the gate plan picture, the validator's new rules and the server check's new checks (`d7c1b55`); the
no-consume check and the journey document (the commit after it).

**What the book is now:** 7 pages, 85 quests (First Days 16, Planets 3, The Road Up 17, The Moon 15, The Rot 18, Gear and
Growth 10, Together 6). Step quests wait for the real mods' own advancements where they exist. The server check passes 10 of
10: 283 ids, 82 of 82 dependencies, 118 items real (including Create's, Simulated's and Aeronautics'), 31 advancements, 4
dimensions and 2 structures exist, and no item task consumes what a player holds.

**Still to do, in this order:**

1. ~~Small content fixes~~ **Done (2026-10-18):** an optional "The Wither" quest on The Rot (after the fortress) and a line on "The dark below" saying the city lies inside the Deep Dark. Server check 10 of 10 again.
2. **Update `QuestScreenChecks`** for the new pages and fix its three findings from section 12 (the space stop at (4000, 0)
   lands on the Moon, so use (4600, 0); close the quest panel before photographing; the window size call has no effect).
   **Sanchit decided (2026-10-18): no client run until every page and every path (the build paths) is done, then one run.**
   So do this update last, once, with the forge chain pages in; do not launch a client for it before.
3. **Reward tables** (choice of one from three): the generator does not write them yet (format in `journey.md` 5.8). Only
   worth it once there is gear to choose between (build paths).
4. ~~Update `CLAUDE.md` and `docs/technical/architecture.md`~~ **Done (2026-10-18).**
5. The decisions in `journey.md` section 5, which are Sanchit's to pick (a real ship test, range and air, helium's job, the
   build paths, the HUD chart, the ship cabin bridge).
