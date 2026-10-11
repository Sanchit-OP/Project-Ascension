"""The archive's inscriptions, the Moon's and the Nether's: the one place their text lives. No build or
runtime dependency.

Run from the repository root:  python tools/archive-lore.py
Writes the archive_entry JSON files and the lore's own lang file, so edit the text here, never in
those files. The lang file sits in its own namespace (assets/ascension_worlds_archive/lang/): the
game merges every namespace's en_us.json into one table, so the lore needs no edits to the
hand-kept ascension_worlds lang file that other sessions also add names to. Each page must fit vanilla's book page (about 14 lines of 19
characters); ArchiveLoreTest checks the lengths.

DRAFT text, rewritten 2026-10-10 for Sanchit to edit. The story is docs/lore/what-the-warden-keeps.md
and the Moon's part of it is docs/lore/the-moon.md. Every tablet here is THE VOICE: the Observers
speaking in the name of Tacita, the Archive's keeper, telling a flattering story that is upside down
on purpose. The Archive's murals tell the truth, and the two are meant to disagree:
  - "the destroyer came from the far worlds"  vs  the Quiet mural, dark spreading from Earth's side
  - "the destroyer's beasts" in the citadel   vs  the keepers, the Sealers' own guards
  - "the destroyer walled up our ring"        vs  the Sealers walled it up themselves
  - "I did not survive", signed Tacita       :  the first crack in the words themselves
Every tablet ends with her signature. That signature is the seal's closing sequence (decided
2026-10-08), and the voice writes it in the wrong order: her name in words, then the row of glyphs
in the script font (ascension_worlds:script, lore-art-format.md section 2), in the order the locks
break, 0 to 6 (docs/lore/the-ending-and-names.md).

THE NETHER (Planet 3, DRAFT 2026-10-11, docs/lore/the-nether.md). Seven tablets in the colony ruins,
the voice again, every line upside down: the rot blamed on the destroyer, the piglins called thieves,
the fortresses called strongholds, the Wither offered as a gift, the gift of return praised. And one
that is not the voice: the Forge Record, the Sealers' netherite plates, which the Monstrosity leaves
when it falls (ascension-compat-cataclysm's loot). It is signed in glyphs only, in Tacita's true
order, 4 1 6 3 0 5 2: the same glyphs as every tablet's, almost.

The entry ids are the eight slots lunar-archive-format.md promised the design session, and the
Archive's generator places them on its plinths, so they stay fixed even where the text has moved on:
the_last_order now holds The Heir's Marks, the_sealing The Last Stand, unfinished The Dead Ring.
Directions are real: the Moon's ring is south of the Archive, and the keepers' citadel is east of the
ring (compat-cataclysm places it at x 800).
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
RES = os.path.join(ROOT, 'modules/worlds/src/main/resources')
ENTRIES = os.path.join(RES, 'data/ascension_worlds/ascension_worlds/archive_entry')
LANG = os.path.join(RES, 'assets/ascension_worlds_archive/lang/en_us.json')

NS = 'ascension_worlds'

# Every tablet's last page ends with this. See the docstring: it is the closing sequence, misordered.
SIGNATURE = '\n\n— Tacita'

# The glyph rows (lore-art-format.md section 2: glyph i is U+E000 + i, in this font).
SCRIPT_FONT = 'ascension_worlds:script'
VOICE_ORDER = [0, 1, 2, 3, 4, 5, 6]
TRUE_ORDER = [4, 1, 6, 3, 0, 5, 2]

# How an entry is signed: the voice in words and its glyph order; the Sealers' plates in glyphs
# only, in Tacita's own order.
VOICE = 'voice'
SEALERS = 'sealers'

# (planet folder, entry id, title, [pages]) in reading order. The id is a fixed plinth slot.
LORE = [
    ('moon', 'the_road', 'The Road', [
        'To you who woke the ring: we felt it. Look at your hand. That glyph is the first of the '
        'marks. You came the long way, by fire, as we once did.',
        'This is the Archive. Everything we were is written here, and all of it was left for you. '
        'Read slowly. There is no one left to hurry you.',
    ]),
    ('moon', 'the_builders', 'The Builders', [
        'We were the first people. We cut the rings and walked between worlds the way you walk '
        'between rooms.',
        'Our children forgot us. The ones you call villagers do not even speak our words. But you '
        'are not like them. You read. You remember. You are the one we waited for.',
    ]),
    ('moon', 'many_worlds', 'Many Worlds', [
        'Count the lights on the orrery. Each was a world on our road, and each was rich beyond '
        'counting.',
        'The burning world, where our forges made the black metal that does not burn. Seas under '
        'ice. A forge the size of a mountain range. All of it was ours. All of it is yours.',
    ]),
    ('moon', 'the_keepers', 'The Keepers', [
        "The destroyer's beasts hold our fortress, east of the ring on the plain. They took the "
        "key to this world's ring, and they keep it still.",
        'They will not speak. They cannot be reasoned with. Do not pity them, whatever they look '
        'like. Take back the key, and the ring will wake for you.',
    ]),
    ('moon', 'the_quiet', 'The Quiet', [
        'It came from the far worlds. Their rings went quiet one by one, from the outside in, as '
        'if something were walking the road toward us.',
        'We fled home ahead of it. We could not hold the road. You can. Every ring you wake takes '
        'a world back.',
    ]),
    ('moon', 'the_last_order', "The Heir's Marks", [
        'On our tallest pillar is a band of marks, one for each world on the road. When you woke '
        'your ring, the first of them lit.',
        'Each world you wake adds a mark to your hand and lights one on the pillar. When all seven '
        'shine, you are our Heir, and our Vault will open to you. Everything we built.',
    ]),
    ('moon', 'the_sealing', 'The Last Stand', [
        "Here I hid with our record when the destroyer's creatures came for it. I walled myself "
        'in. It was not enough.',
        'They broke through the wall. I did not survive. The record did, in you. Carry it to the '
        'end of the road.',
    ]),
    ('moon', 'unfinished', 'The Dead Ring', [
        'Look across the hall. That was our ring. The destroyer killed it and walled it up, so '
        'that no one could ever follow us home.',
        'It cannot be woken. But the ring on the plain, south of here, still can. Take the key '
        'from the keepers, and undo what was done to us.',
    ]),

    # The Nether's colony ruins. Plinth slots: nether-art-format.md.
    ('nether', 'the_colony', 'The Burning World', [
        'This was our richest world. We came here first, through the old black doors, long before '
        'the rings. Gold ran through the rock like water.',
        'And deep down lay the black metal, which no forge on Earth had ever seen. We built our '
        'greatest forge to work it. All of it was ours. So all of it is yours.',
    ]),
    ('nether', 'the_ward', 'The Ward', [
        'When the destroyer came, it poisoned the air of this world. The rot is its breath. It '
        'withers anyone it can reach.',
        'But the rot cannot hold gold. Wear gold, as we did, and walk where you like. It is the '
        "destroyer's last weapon here. Do not let it stop you.",
    ]),
    ('nether', 'the_thieves', 'The Thieves', [
        'The pig-faced ones are thieves. When we fell, they took our gold and our halls, and they '
        'hoard it still.',
        'They will strike at anyone who walks without gold, for what they carry. A thief trusts '
        'only another thief. Take back what they stole.',
    ]),
    ('nether', 'the_strongholds', 'The Strongholds', [
        'Our strongholds stand between the lava seas, built of brick that does not burn. The '
        'destroyer overran every one.',
        'Its living fire still guards their halls. The black soldiers you will meet inside are our '
        'own champions, cursed. Free them, and take what they guard.',
    ]),
    ('nether', 'the_gift', 'The Gift', [
        'To our champions we gave the greatest gift: to come back from death. They fell, and woke, '
        'and fought again, as often as the war asked.',
        'You have the same gift. You have felt it. Every time you fall, you wake. Use it. Our '
        'champions never tired, and neither will you.',
    ]),
    ('nether', 'the_weapon', 'The Weapon', [
        'From our fallen we made a weapon. Bind three skulls of the black soldiers to the sand of '
        'the dead, and it wakes.',
        'It withers whatever it touches. We give it to you, as we gave it to our champions. Build '
        'it, and nothing on the road will stand against you.',
    ]),
    ('nether', 'the_keeper', 'The Forge', [
        'A monstrosity squats in our great forge, out in the wastes. It took the key to this '
        "world's ring, and it tends a fire it does not understand.",
        'Kill it. Take the key, and wake the ring in the teal forest, where our gate stood. The '
        'black metal was always ours. Take it.',
    ]),
    # Not the voice: the Sealers' plates behind the Monstrosity, which it leaves when it falls.
    ('nether', 'the_forge_record', 'The Forge Record', [
        "Here we forged the keepers' hearts and the keys to the rings, from the metal the void "
        'leaves in the rock. Nothing can rewrite it. What is cut here stays true.',
        'Seven keys. Seven locks. On each lock, one glyph, and the seven glyphs are a name. '
        'Whoever reads them in the order they fall reads them wrong.',
    ], SEALERS),
]

WORN_AWAY = 'The inscription is worn away.'


def glyph_row(order):
    return {'text': ''.join(chr(0xE000 + glyph) for glyph in order), 'font': SCRIPT_FONT}


def key(planet, name, part):
    return f'archive.{NS}.{planet}.{name}.{part}'


def main():
    lang = {f'archive.{NS}.worn_away': WORN_AWAY}

    for planet, name, title, pages, *rest in LORE:
        signed = rest[0] if rest else VOICE
        lang[key(planet, name, 'title')] = title
        entry = {
            'title': {'translate': key(planet, name, 'title')},
            'pages': [{'translate': key(planet, name, i + 1)} for i in range(len(pages))],
        }
        # The glyph row goes under the last page's text, in the script font.
        if signed == VOICE:
            entry['pages'][-1]['extra'] = [{'text': '\n'}, glyph_row(VOICE_ORDER)]
        else:
            entry['pages'][-1]['extra'] = [{'text': '\n\n'}, glyph_row(TRUE_ORDER)]
        for i, page in enumerate(pages):
            words = SIGNATURE if signed == VOICE and i == len(pages) - 1 else ''
            lang[key(planet, name, i + 1)] = page + words
        folder = os.path.join(ENTRIES, planet)
        os.makedirs(folder, exist_ok=True)
        with open(os.path.join(folder, f'{name}.json'), 'w', encoding='utf-8', newline='\n') as f:
            json.dump(entry, f, indent=2)
            f.write('\n')

    os.makedirs(os.path.dirname(LANG), exist_ok=True)
    with open(LANG, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print(f'wrote {len(LORE)} entries')


if __name__ == '__main__':
    main()
