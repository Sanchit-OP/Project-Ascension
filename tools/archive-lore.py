"""The Lunar Archive's inscriptions: the one place their text lives. No build or runtime dependency.

Run from the repository root:  python tools/archive-lore.py
Writes the archive_entry JSON files and the lore's own lang file, so edit the text here, never in
those files. The lang file sits in its own namespace (assets/ascension_worlds_archive/lang/): the
game merges every namespace's en_us.json into one table, so the lore needs no edits to the
hand-kept ascension_worlds lang file that other sessions also add names to. Each page must fit vanilla's book page (about 14 lines of 19
characters); ArchiveLoreTest checks the lengths.

DRAFT text, written 2026-10-06 for Sanchit to edit. It follows docs/lore/world.md: a civilisation
that crossed every nearby world through the gates, then something ended it. It tells the premise
and stops short of the answer. Hooks it leaves on purpose:
  - the keepers  = the Ruined Citadel's Ender Golem and Guardian, guarding the gate key
  - "ash plains" = Planet 3, the Nether world; "a forge the size of a mountain range" = Planet 5
  - the blue world's gate was never opened: Earth matters, for reasons the endgame decides
  - the quiet moved from the outside in: something walked the road toward the Moon
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
RES = os.path.join(ROOT, 'modules/worlds/src/main/resources')
ENTRIES = os.path.join(RES, 'data/ascension_worlds/ascension_worlds/archive_entry')
LANG = os.path.join(RES, 'assets/ascension_worlds_archive/lang/en_us.json')

NS = 'ascension_worlds'

# (planet folder, entry name, title, [pages]) in reading order.
LORE = [
    ('moon', 'the_road', 'The Road', [
        'To whoever wakes the gate after us. You came the long way, by fire and by falling. '
        'We came through the ring, as we went everywhere: one step, and a new sky.',
        'This is the Archive. Everything we learned is written in its halls. Read slowly. '
        'There is no one left to hurry you.',
    ]),
    ('moon', 'the_builders', 'The Builders', [
        'We were not born on this rock. Nothing is. We cut our halls here because it was close '
        'and quiet, and because the blue world fills half the sky.',
        'We were stonecutters first, and then cutters of distance. Every gate is the same ring: '
        'the stone of the world it stands on, bound in our script.',
    ]),
    ('moon', 'many_worlds', 'Many Worlds', [
        'Count the lights on the orrery. Each was a world we walked. Ash plains that burned '
        'underfoot. Seas under ice. A forge the size of a mountain range.',
        'None of them was home, and all of them were. A child could wake on one world and sleep '
        'on another, and think nothing of it.',
    ]),
    ('moon', 'the_keepers', 'The Keepers', [
        'A gate left alone is a door left open. So we made keepers: stone that walks, and '
        'remembers one order. Let no one through who cannot open the way.',
        'If you are reading this, you have met them. We are sorry. They were never meant to '
        'outlast us.',
    ]),
    ('moon', 'the_quiet', 'The Quiet', [
        'It began with the farthest gate. It did not break. It stopped answering. Those we sent '
        'through did not come back, and the ring stayed dark.',
        'Then the next one. Then the next. Always from the outside in, as if something were '
        'walking the road toward us.',
    ]),
    ('moon', 'the_last_order', 'The Last Order', [
        'The order came from the forge world, the last voice to come through: put out the gates. '
        'All of them. Do not wait for us.',
        'We argued for a day we did not have. Then we did as we were told. The rings went dark '
        'one by one, and the sky felt very large.',
    ]),
    ('moon', 'the_sealing', 'The Sealing', [
        'We have sealed the Archive and set the keepers. The gate on the blue world we never '
        'opened, and now we never will.',
        'Whatever walks the road must not find the way there. If it already has, this record is '
        'for no one. We write it anyway.',
    ]),
    ('moon', 'unfinished', 'Unfinished', [
        'I am the last one here. The air is going. I can hear the gate across the hall, though it '
        'is dark, though we put it out ourselves.',
        'Something is on the other side of the ring. It is not knocking. It is reading the script, '
        'slowly, the way you are reading this, and it has almost',
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
            lang[key(planet, name, i + 1)] = page
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
