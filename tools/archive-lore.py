"""The Lunar Archive's inscriptions: the one place their text lives. No build or runtime dependency.

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
2026-10-08), and the voice writes it in the wrong order. Today it is only her name in words; the row
of glyphs it will become needs the mark's glyph art first (docs/lore/the-mark.md).

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
]

WORN_AWAY = 'The inscription is worn away.'


def key(planet, name, part):
    return f'archive.{NS}.{planet}.{name}.{part}'


def main():
    lang = {f'archive.{NS}.worn_away': WORN_AWAY}

    for planet, name, title, pages in LORE:
        lang[key(planet, name, 'title')] = title
        entry = {
            'title': {'translate': key(planet, name, 'title')},
            'pages': [{'translate': key(planet, name, i + 1)} for i in range(len(pages))],
        }
        for i, page in enumerate(pages):
            lang[key(planet, name, i + 1)] = page + (SIGNATURE if i == len(pages) - 1 else '')
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
