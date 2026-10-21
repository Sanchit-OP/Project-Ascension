#!/usr/bin/env python3
"""Draws the two empty-slot icons for ascension-compat-curios: a tank and an hourglass, 16 x 16, brass on dark teal.

  python tools/generate-curios-slot-art.py

Original art (docs/technical/art-guide.md): plain shapes written out here, no copied pixels. Deterministic: running it
again writes the same bytes. Standard library only.
"""

import os
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
OUT = os.path.join(ROOT, 'modules', 'compat', 'curios', 'src', 'main', 'resources', 'assets',
                   'ascension_compat_curios', 'textures', 'slot')

BRASS = (196, 150, 70, 255)
BRASS_DARK = (122, 90, 40, 255)
TEAL = (62, 150, 150, 255)
CLEAR = (0, 0, 0, 0)

TANK = [
    '................',
    '.......##.......',
    '......#..#......',
    '.....######.....',
    '....#======#....',
    '...#=oooooo=#...',
    '...#=o....o=#...',
    '...#=o....o=#...',
    '...#=o....o=#...',
    '...#=o....o=#...',
    '...#=o....o=#...',
    '...#=oooooo=#...',
    '....#======#....',
    '.....######.....',
    '................',
    '................',
]

RELIC = [
    '................',
    '...##########...',
    '....#======#....',
    '.....#=oo=#.....',
    '......#oo#......',
    '.......##.......',
    '.......##.......',
    '......#oo#......',
    '.....#=oo=#.....',
    '....#=oooo=#....',
    '...#=oooooo=#...',
    '...##########...',
    '................',
    '................',
    '................',
    '................',
]

COLOURS = {'.': CLEAR, '#': BRASS_DARK, '=': BRASS, 'o': TEAL}


def png(rows):
    raw = b''
    for row in rows:
        raw += b'\x00' + b''.join(bytes(COLOURS[c]) for c in row)

    def chunk(kind, data):
        body = kind + data
        return struct.pack('>I', len(data)) + body + struct.pack('>I', zlib.crc32(body) & 0xffffffff)

    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, rows in (('empty_tank_slot', TANK), ('empty_relic_slot', RELIC)):
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
        with open(os.path.join(OUT, name + '.png'), 'wb') as handle:
            handle.write(png(rows))
        print('wrote', name)


if __name__ == '__main__':
    main()
