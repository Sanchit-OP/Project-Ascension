#!/usr/bin/env python3
"""The quest book: from readable pages in pack/quests/ to the files FTB Quests and the game read.

    python tools/generate-quests.py            write the files
    python tools/generate-quests.py --check    fail if anything is wrong or out of date (the build runs this)

Reads  pack/quests/book.py (and the page modules it imports), pack/quests/guarded-words.txt, and the
       planet files under modules/worlds, so the Planets page is laid out from the worlds themselves.
Writes pack/config/ftbquests/quests/            what FTB Quests loads (copied into a game by
                                                tools/sync-pack-config.py)
       modules/compat/ftbquests/src/main/resources/assets/ascension_compat_ftbquests/quests/chart_layout.json
                                                where everything is on the Planets picture; the game's own
                                                projection is tested against the sample points in it

Standard library only. The pictures are made by tools/generate-quest-art.py (which needs Pillow); they are
committed, and this tool checks they exist.
See docs/progression/quests.md.
"""

import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(ROOT, 'pack', 'quests'))

from quests import emit, validate  # noqa: E402

QUEST_DIR = os.path.join(ROOT, 'pack', 'config', 'ftbquests', 'quests')
LAYOUT = os.path.join(ROOT, 'modules', 'compat', 'ftbquests', 'src', 'main', 'resources', 'assets',
                      'ascension_compat_ftbquests', 'quests', 'chart_layout.json')


def expected_files():
    from book import BOOK  # noqa: E402  (imported late so a syntax error in a page reports as one)
    files = {os.path.join(QUEST_DIR, *rel.split('/')): text for rel, text in emit.all_files(BOOK).items()}
    files[LAYOUT] = emit.chart_layout_text()
    return BOOK, files


def _read(path):
    with open(path, encoding='utf-8', newline='') as handle:
        return handle.read().replace('\r\n', '\n')


def _on_disk():
    found = set()
    for dirpath, _, names in os.walk(QUEST_DIR):
        for name in names:
            found.add(os.path.join(dirpath, name))
    return found


def main(argv):
    check = '--check' in argv
    book, files = expected_files()

    problems = validate.check(book)
    if problems:
        print('quest book: %d problem(s)' % len(problems))
        for problem in problems:
            print('  ' + problem)
        return 1

    if check:
        stale = []
        for path, text in files.items():
            if not os.path.exists(path) or _read(path) != text:
                stale.append(os.path.relpath(path, ROOT))
        for path in sorted(_on_disk() - set(files)):
            stale.append(os.path.relpath(path, ROOT) + ' (no longer generated)')
        if stale:
            print('quest book: generated files are out of date; run python tools/generate-quests.py')
            for name in stale:
                print('  ' + name)
            return 1
        quests = sum(len(c.quests) for c in book.chapters)
        print('quest book: %d page(s), %d quest(s), no problems' % (len(book.chapters), quests))
        return 0

    for path in sorted(_on_disk() - set(files)):
        os.remove(path)
    for path, text in files.items():
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w', encoding='utf-8', newline='\n') as handle:
            handle.write(text)
    print('quest book: wrote %d file(s) for %d page(s)' % (len(files), len(book.chapters)))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
