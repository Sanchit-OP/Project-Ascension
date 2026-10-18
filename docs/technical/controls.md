# Controls: default keys, the keybind tools, and what we unbind

Minecraft's Controls screen is a long flat list, and a pack like ours adds to it fast: 145 bindings
at the last count, from vanilla and eleven mods. This page says which keys the pack's defaults use,
which collide, what we unbind so players have room for the keys they actually want, and which
tools give players a better screen for changing them.

**Status, 2026-10-18: the tools are installed and the defaults are applied** (Sanchit's go-ahead).
The key map itself is provisional: he will check it with player testing, and it is one text file to
change (`pack/config/defaultoptions/keybindings.txt`).

**Seen:** in a dev client with no saved keys, Default Options reports "applied 10 defaults (10 keys
were reconfigured)" and the audit shows all ten as the pack's defaults, **both as the current key and
as the Reset key**. Keybind Atlas, Controlling, Searchables and Balm load beside the rest of the pack.
**Not seen by eye:** the Atlas screen and Controlling's search bar.

**What is left on a default key, on purpose:** Create's three Left Alt keys (modifier, tool belt,
tool menu), Cataclysm's own weapon-ability and boots-ability pair on V, Create's Ponder key on W (it
only acts over an item in a screen), and Tab, which is both the player list and Create Simulated's
rotate mode (not yet tried in play; unbind Simulated's if it fights).

## The audit

`client/dev/KeybindAudit` writes every binding the running pack has (name, category, default key,
current key, conflict context) to `run/client/keybind-audit-out.tsv`. Put an empty file named
`keybind-audit.txt` in `run/client/`, then `./gradlew runDevClient`; it writes the file and quits.
Rerun it whenever a mod is added. A conflict context matters: two bindings on one key only collide if
their contexts overlap (a `GUI` key and an `IN_GAME` key do not).

**145 bindings, 25 unbound by default.** The collisions that matter, all in-game (`UNIVERSAL` or
`IN_GAME` on both sides):

| Key | Taken by |
|---|---|
| **K** | our skill tree, and **Iris: toggle shaders** |
| **R** | our rune page, and **Iris: reload shaders** |
| **V** | our character sheet, and **two Cataclysm abilities** (weapon ability, boots ability) |
| **C** | **Ok Zoomer**'s zoom, **Cataclysm**'s helmet ability, and vanilla's **save hotbar** (creative) |
| **Tab** | vanilla's player list, and **Create Simulated: rotate mode** |
| Left Alt | Create's three keys, **on purpose** (modifier, tool belt, tool menu) |
| W | Create's Ponder hover key, which only acts over an item in a screen |

And two things missing: **our own ability key has no default** (it is the one key for every worn
item with an active, and for Galeforce), and **FTB Quests' quest book has no default**.

Everything else that "collides" in the dump is a screen-only key (JEI, the FTB Quests editor) that
cannot fire in play.

## The defaults we ship

Principles: every in-game action on its own key; the whole left hand stays free for movement; keys
the average player never touches are unbound, and a player can bind them back in one click.

**Ours and the tools' keys**

| Action | Default | Why |
|---|---|---|
| Rune page | **R** | unchanged |
| Skill tree | **K** | unchanged |
| Character sheet | **H** (was V) | V is Cataclysm's; H for "hero". The HUD overhaul may retire the sheet. |
| Activate item ability | **Z** (was unbound) | the one key every worn or held active uses |
| Quest book (FTB Quests) | **J** (was unbound) | the journal |
| Keybind Atlas | **B** (its own default is K) | K is the skill tree |

**Unbound by default**

| Binding | Was | Why |
|---|---|---|
| Save hotbar, Load hotbar (creative) | C, X | creative-only; frees C for zoom and X for later |
| Social interactions | P | the player list on Tab covers co-op |
| Iris: reload shaders | R | clashes with runes; a developer's key |
| Iris: toggle shaders | K | clashes with the skill tree; shaders are chosen in Video Settings |
| Cataclysm helmet ability | C | clashes with zoom; it moves to **N** rather than being left unbound, because the armour needs it |

Already unbound and left that way: smooth camera, spectator outlines, Bettercombat's feint and
mine-with-weapons, Create's rotate menu, Simulated's scrolls, ModernFix's config, entity culling's
toggle, JEI's cheat and edit modes, the FTB team screen (settled with the team design).

**Left as they are,** because they are the keys a player expects: movement, jump, sneak, sprint,
inventory (E), drop (Q), swap hands (F), hotbar 1 to 9, chat (T), command (/), player list (Tab),
advancements (L), screenshot (F2), perspective (F5), fullscreen (F11), Curios (G), Jade's keypad keys
(a laptop has none).

## The tools

All checked for 1.21.1 on NeoForge on 2026-10-18.

| Mod | Build | Size | What it gives the player |
|---|---|---|---|
| **Keybind Atlas** | `1.4.0` | 416 KB | A map of the keyboard and mouse: every key colour-coded as bound, free or hidden; hover for the action and the mod it comes from; an edit mode to rebind from the map. |
| **Controlling** + **Searchables** | `19.0.5` + `1.0.2` | 78 KB + 77 KB | A search bar and a "show conflicts only" filter on the normal Controls list, and a view of which keys are still free. |
| **Default Options** + **Balm** | `21.1.8` + `21.0.66` | 106 KB + 784 KB | Ships our default keys and options: a **fresh install starts with them**, a player's own changes persist through updates. This is what makes "unbound by default, bind it back if you want" work. |

**How the defaults reach a player.** Default Options applies `keybindings.txt` to a **fresh install**
and to any binding **not yet in the player's `options.txt`** (a newly added mod's keys). A key already
saved there keeps the player's value, which is what makes it safe to update. So a test game that
already has an `options.txt` needs its saved keys moved too:
`python tools/sync-pack-config.py --options <options.txt>` does it, moving a key only if it still
holds the old default (a key the player changed is reported and kept). On 2026-10-18 it moved four keys
in the Ascension Dev instance (creative hotbar save and load, social interactions, Cataclysm's helmet
ability), left Sanchit's own choices alone (ability on C, runes on `[`, sheet on `,`), and kept a
backup beside `options.txt`. His ability key on C will meet Ok Zoomer's zoom, also C by default; the
shipped default is Z, so only his own binding clashes.

Default Options keeps the pack's defaults in `config/defaultoptions/` (`keybindings.txt`,
`options.txt`). It is written by running `/defaultoptions saveAll` in a game set up the way we want, or
by hand: one `key_<name>:<key>` line per binding. Its licence is All Rights Reserved, so a distributed
pack references it by its Modrinth or CurseForge manifest, like the shader packs.
