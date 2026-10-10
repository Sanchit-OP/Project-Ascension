"""Authoring helper for the lore art: the script, the mark, the pylon's glyphs, the light on the Moon.
No build or runtime dependency.

Run from the repository root:  python tools/generate-lore-art.py
Needs Pillow (and numpy, for the in-hand previews). Rewrites every file it owns under
modules/worlds/src/main/resources/assets/ascension_worlds/ and the previews under design/lore-art/,
so hand edits to those files are lost on the next run -- edit this file instead.

Owns, by the contract's sections (docs/technical/lore-art-format.md):
  1. the script: seven glyphs and the eye in eight stages, drawn here as pixel maps
  2. the script as a bitmap font (font/script.json, textures/font/script.png)
  3. the mark on the player's hand (textures/entity/mark/stage_<n>.png)
  4. the illagers' mark (textures/entity/mark/illager.png)
  5. the mark_glyph block's textures, models and blockstate
  9. the light on the Moon (textures/environment/moon_signal.png)
The murals (6, 7, 8) are tools/generate-archive-art.py's; the pylon band and the shrine's template
are tools/generate-lunar-archive.py's. Both read GLYPHS from here.

Everything is original: the glyphs are an invented script, placed by hand in the pixel maps below,
in the family of the gatestone's runes (RUNES in tools/generate-gateway-art.py). They copy no real
alphabet and not the enchanting table's. Palettes come from the gateway script (art guide: import,
never copy). Design record: docs/technical/lore-art-design.md.
"""
import importlib.util
import json
import math
import os
import random

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


art = _load('gateway_art', 'generate-gateway-art.py')

BASALT, ANORTH, TEAL, DULL = art.BASALT, art.ANORTH, art.TEAL, art.DULL
mix, shade, new = art.mix, art.shade, art.new
ROOT = art.ROOT
ASSETS = os.path.join(ROOT, 'modules/worlds/src/main/resources/assets/ascension_worlds')
TEX = os.path.join(ASSETS, 'textures')
DESIGN = os.path.join(ROOT, 'design/lore-art')
NS = 'ascension_worlds'


# --- 1. the script ---------------------------------------------------------------------------------
#
# Seven glyphs, one per gate in gate order: 0 Earth, 1 the Moon, 2 the burning world, 3-6 the later
# worlds. Together they are the seven signs of Tacita's name. Drawn like the gatestone's runes:
# one-pixel strokes, diagonals as steps, small closed counters, compact blocks rather than airy
# calligraphy. Each has its own silhouette (tall, wide, falling, rising, stepped, hooked, cupped), so
# no two are confused at 5 px. Names are only for this file; the game never names them.
#
#   0 key    a counter on a stepped stem -- the gatestone's first rune, grown up
#   1 gate   two posts, the lintel broken, a stone hanging through the gap
#   2 blade  a stepped fall that curls up at its end
#   3 chain  two counters linked on the rising diagonal
#   4 stair  a bar that steps down twice and turns back
#   5 hook   a bar bent down into a hanging counter
#   6 basin  a wall and a floor, and a stroke falling into the corner

GLYPHS = [                      # 5 x 5: the compact form (the mark, the pylon's carving)
    ["..#..",
     ".#.#.",
     "..#..",
     "..##.",
     "...#."],
    ["##.##",
     "#...#",
     "#.#.#",
     "..#..",
     ".##.."],
    ["#....",
     "##...",
     ".##..",
     "..#.#",
     "..##."],
    ["...##",
     "..#.#",
     ".###.",
     "#.#..",
     "##..."],
    ["###..",
     "..#..",
     "..###",
     "....#",
     "...##"],
    ["####.",
     "...#.",
     ".#.#.",
     "#.#..",
     ".#..."],
    [".....",
     "#.#..",
     "#..#.",
     "#...#",
     ".####"],
]

GLYPHS_8 = [                    # 7 x 7 in an 8 x 8 cell: the text form (the font)
    ["..#....",
     ".#.#...",
     "..#....",
     "..##...",
     "...#...",
     "...##..",
     "....#.."],
    ["###.###",
     "#.....#",
     "#.....#",
     "#..#..#",
     "...#...",
     "...#...",
     "..##..."],
    ["#......",
     "##.....",
     ".##....",
     "..##...",
     "...#..#",
     "...#.#.",
     "...##.."],
    [".....##",
     "....#.#",
     "....##.",
     "...#...",
     ".##....",
     "#.#....",
     "##....."],
    ["####...",
     "...#...",
     "...#...",
     "...####",
     "......#",
     "......#",
     "....###"],
    ["#####..",
     "....#..",
     "....#..",
     "...#...",
     ".#.#...",
     "#.#....",
     ".#....."],
    [".......",
     "#..#...",
     "#...#..",
     "#....#.",
     "#.....#",
     "#.....#",
     ".######"],
]

# The eye in eight stages, 0 closed to 7 wide open. It is the network's diamond eye (the gates', the
# keystone's, chiseled anorthosite's): it opens from a shut line through an almond to the full
# diamond, and its pupil goes from nothing, to a point, to a cat's slit, and at the last its whole
# iris lights round the slit. Pupils are a dot or a slit, never a centred cross; concentric outlines
# were tried for the last stage and read as a checkerboard at this size.
EYE_8 = [                       # 7 x 7, monochrome: the font's U+E010..E017
    [".......", ".......", ".......", "#######", ".......", ".......", "......."],
    [".......", ".......", "..###..", "##...##", "..###..", ".......", "......."],
    [".......", ".......", "..###..", "##.#.##", "..###..", ".......", "......."],
    [".......", "..###..", ".#...#.", "#..#..#", ".#...#.", "..###..", "......."],
    [".......", "..###..", ".#.#.#.", "#..#..#", ".#.#.#.", "..###..", "......."],
    ["...#...", "..#.#..", ".#...#.", "#..#..#", ".#...#.", "..#.#..", "...#..."],
    ["...#...", "..#.#..", ".#.#.#.", "#..#..#", ".#.#.#.", "..#.#..", "...#..."],
    ["...#...", "..###..", ".##.##.", "###.###", ".##.##.", "..###..", "...#..."],
]

# 9 x 9, in colour, for the mark: L lid (the rim), d the dark of the eye, i iris, p pupil.
EYE_MARK = [
    [".........", ".........", ".........", ".........", "LLLLLLLLL",
     ".........", ".........", ".........", "........."],
    [".........", ".........", ".........", "..LLLLL..", "LLdddddLL",
     "..LLLLL..", ".........", ".........", "........."],
    [".........", ".........", ".........", "..LLLLL..", "LLddpddLL",
     "..LLLLL..", ".........", ".........", "........."],
    [".........", ".........", "...LLL...", ".LLdddLL.", "LdddpdddL",
     ".LLdddLL.", "...LLL...", ".........", "........."],
    [".........", ".........", "...LLL...", ".LLdpdLL.", "LdddpdddL",
     ".LLdpdLL.", "...LLL...", ".........", "........."],
    [".........", "...LLL...", "..LdddL..", ".LddpddL.", "LdddpdddL",
     ".LddpddL.", "..LdddL..", "...LLL...", "........."],
    ["....L....", "...LdL...", "..LdpdL..", ".LddpddL.", "LdddpdddL",
     ".LddpddL.", "..LdpdL..", "...LdL...", "....L...."],
    ["....L....", "...LdL...", "..LdidL..", ".LdipidL.", "LdiipiidL",
     ".LdipidL.", "..LdidL..", "...LdL...", "....L...."],
]


def cells(rows):
    """The lit pixels of a pixel map, as (x, y, symbol)."""
    return [(x, y, ch) for y, row in enumerate(rows) for x, ch in enumerate(row) if ch != '.']


# --- previews ------------------------------------------------------------------------------------------

INK = (232, 230, 222)            # the sheet's 'paper' text colour
SHEET_BG = (30, 31, 36)


def stamp(img, rows, x0, y0, colour, scale=1):
    d = ImageDraw.Draw(img)
    for x, y, _ in cells(rows):
        d.rectangle([x0 + x * scale, y0 + y * scale, x0 + (x + 1) * scale - 1, y0 + (y + 1) * scale - 1],
                    fill=colour)


EYE_COLOURS = {'L': TEAL[3], 'd': TEAL[1], 'i': TEAL[4], 'p': TEAL[5]}


def stamp_eye(img, rows, x0, y0, scale=1):
    d = ImageDraw.Draw(img)
    for x, y, ch in cells(rows):
        d.rectangle([x0 + x * scale, y0 + y * scale, x0 + (x + 1) * scale - 1, y0 + (y + 1) * scale - 1],
                    fill=EYE_COLOURS[ch])


def script_sheet(path):
    """The canonical drawings: each glyph at 5 px and 8 px, zoomed and at 1x; the eye's stages in
    both forms; the set as a line of lit script and as a line carved in pale stone."""
    Z = 12
    W = 16 * 8 * Z // 2 + 80
    out = Image.new('RGB', (1180, 900), SHEET_BG)
    d = ImageDraw.Draw(out)
    y = 12
    d.text((16, y), 'The script: seven glyphs (gate order: 0 Earth, 1 the Moon, 2 the burning world, '
                    '3-6 the later worlds) and the eye in eight stages', fill=INK)
    y += 24
    d.text((16, y), '5 px (the mark, the pylon)', fill=INK)
    d.text((16, y + 8 * Z + 14), '8 px (the font: U+E000..E006)', fill=INK)
    for i in range(7):
        x = 16 + i * 9 * Z * 3 // 2
        stamp(out, GLYPHS[i], x + Z, y + 16, TEAL[4], Z)
        d.text((x, y + 16 + 6 * Z), str(i), fill=INK)
        stamp(out, GLYPHS_8[i], x + Z, y + 8 * Z + 30, TEAL[4], Z)
    y += 8 * Z + 30 + 8 * Z + 10
    d.text((16, y), 'the eye, stages 0-7: font (U+E010..E017) and mark (9 px, in colour)', fill=INK)
    y += 18
    for s in range(8):
        x = 16 + s * 9 * Z * 5 // 4
        stamp(out, EYE_8[s], x, y, INK, Z * 3 // 4)
        stamp_eye(out, EYE_MARK[s], x, y + 7 * Z, Z * 3 // 4)
        d.text((x, y + 7 * Z + 9 * Z * 3 // 4 + 4), str(s), fill=INK)
    y += 7 * Z + 9 * Z * 3 // 4 + 26
    # at 1x and 3x, lit on dark stone and carved in pale stone
    lit = Image.new('RGB', (7 * 7 + 2, 7), BASALT[1])
    for i in range(7):
        stamp(lit, GLYPHS[i], 2 + i * 7, 1, TEAL[4])
    carved = Image.new('RGB', (7 * 9 + 8 * 9 + 2, 9), ANORTH[3])
    for i in range(7):
        stamp(carved, GLYPHS_8[i], 2 + i * 9, 1, BASALT[2])
    for s in range(8):
        stamp(carved, EYE_8[s], 2 + (7 + s) * 9, 1, BASALT[2])
    d.text((16, y), 'at 1x, and at 4x: 5 px lit on basalt; 8 px glyphs and eyes cut in anorthosite', fill=INK)
    y += 18
    out.paste(lit, (16, y))
    out.paste(carved, (16 + lit.width + 16, y))
    y += 20
    out.paste(lit.resize((lit.width * 4, lit.height * 4), Image.NEAREST), (16, y))
    out.paste(carved.resize((carved.width * 4, carved.height * 4), Image.NEAREST), (16, y + 40))
    # the gatestone's runes, the family these grew from
    y += 40 + 9 * 4 + 20
    d.text((16, y), "the gatestone's runes (3 x 4), the family these belong to", fill=INK)
    runes = Image.new('RGB', (9 * 5 + 2, 6), BASALT[1])
    for i, r in enumerate(art.RUNES):
        stamp(runes, r, 2 + i * 5, 1, TEAL[3])
    out.paste(runes.resize((runes.width * 4, runes.height * 4), Image.NEAREST), (16, y + 16))
    out = out.crop((0, 0, out.width, y + 16 + 24 + 16))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path)
    return out


def main():
    script_sheet(os.path.join(DESIGN, 'script.png'))
    print('wrote design/lore-art/script.png')


if __name__ == '__main__':
    main()
