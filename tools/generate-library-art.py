#!/usr/bin/env python3
"""Draws the Enchantment Library's two block textures (16 x 16): dark wood shelves of coloured book spines with a violet
rune band, and a lid. Original art (docs/technical/art-guide.md); seeded, so running it again writes the same bytes.

  python tools/generate-library-art.py
"""

import os
import random
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
OUT = os.path.join(ROOT, 'modules', 'combat', 'src', 'main', 'resources', 'assets', 'ascension_combat', 'textures', 'block')

WOOD = [(58, 38, 26), (70, 46, 31), (50, 32, 22)]
SPINES = [(120, 40, 52), (46, 84, 110), (70, 110, 70), (150, 118, 50), (96, 60, 130), (40, 40, 60)]
RUNE = (170, 120, 230)
LID = [(64, 42, 120), (78, 52, 140), (54, 34, 100)]


def png(pixels):
    raw = b''.join(b'\x00' + b''.join(bytes(p) + b'\xff' for p in row) for row in pixels)

    def chunk(kind, data):
        body = kind + data
        return struct.pack('>I', len(data)) + body + struct.pack('>I', zlib.crc32(body) & 0xffffffff)

    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def side(rng):
    px = [[WOOD[rng.randrange(3)] for _ in range(16)] for _ in range(16)]
    for shelf_top in (1, 9):
        x = 1
        while x < 15:
            width = rng.choice((1, 2, 2, 3))
            colour = SPINES[rng.randrange(len(SPINES))]
            for xx in range(x, min(x + width, 15)):
                for yy in range(shelf_top, shelf_top + 6):
                    shade = -12 if xx == x else 0
                    px[yy][xx] = tuple(max(0, c + shade) for c in colour)
            x += width
    for xx in range(16):
        px[0][xx] = px[7][xx] = px[8][xx] = px[15][xx] = WOOD[2]
    for xx in (3, 7, 11):
        px[7][xx] = RUNE
    return px


def top(rng):
    px = [[LID[rng.randrange(3)] for _ in range(16)] for _ in range(16)]
    for i in range(16):
        px[0][i] = px[15][i] = px[i][0] = px[i][15] = LID[2]
    for i in range(5, 11):
        px[5][i] = px[10][i] = RUNE
        px[i][5] = px[i][10] = RUNE
    return px


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, make, seed in (('enchantment_library_side', side, 11), ('enchantment_library_top', top, 12)):
        with open(os.path.join(OUT, name + '.png'), 'wb') as handle:
            handle.write(png(make(random.Random(seed))))
        print('wrote', name)


if __name__ == '__main__':
    main()
