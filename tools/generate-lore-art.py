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
import sys

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


def write_json(rel, obj):
    path = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


# --- 2. the script as a font ---------------------------------------------------------------------------
#
# One bitmap provider, 8 x 8 cells, height 8 and ascent 7: the same metrics as vanilla's own text, so a
# glyph's top row is a capital's top row and its seventh row sits on the baseline. Row 0 of the sheet
# is U+E000..E006 (the glyphs) and a padding cell; row 1 is U+E010..E017 (the eye, stage 0 to 7). The
# pixels are white, so the text colour logic gives a line is the colour it shows in.

FONT_GLYPHS = [chr(0xE000 + i) for i in range(7)] + ['\u0000']
FONT_EYES = [chr(0xE010 + s) for s in range(8)]


def font_texture():
    img = Image.new('RGBA', (64, 16), (0, 0, 0, 0))
    for i, rows in enumerate(GLYPHS_8):
        stamp(img, rows, i * 8, 0, (255, 255, 255, 255))
    for s, rows in enumerate(EYE_8):
        stamp(img, rows, s * 8, 8, (255, 255, 255, 255))
    return img


def font_json():
    return {'providers': [{
        'type': 'bitmap',
        'file': f'{NS}:font/script.png',
        'height': 8,
        'ascent': 7,
        'chars': [''.join(FONT_GLYPHS), ''.join(FONT_EYES)],
    }]}


def set_line(img, x, y, text_rows, colour, gap=1):
    """Lay pixel maps along a line the way the font does: each advances its own width plus one."""
    for rows in text_rows:
        stamp(img, rows, x, y, colour)
        x += max(cx for cx, _, _ in cells(rows)) + 1 + gap
    return x


def font_preview(path):
    """A tablet's last line and Tacita's signature under it, at 1x and at GUI scale 3, on the dark
    and the pale reader. Pillow's own bitmap font stands in for the game's text: the point is the
    glyphs' size and baseline beside ordinary letters, not the letters."""
    from PIL import ImageFont
    font = ImageFont.load_default_imagefont()
    cap = font.getbbox('H')                  # the stand-in's capital: top row and baseline
    panels = []
    for bg, words, sig in ((BASALT[1], (220, 220, 214), TEAL[4]), (ANORTH[4], (40, 40, 44), TEAL[2])):
        img = Image.new('RGB', (196, 34), bg)
        d = ImageDraw.Draw(img)
        d.text((4, 4 - cap[1]), 'the Vault will open to you.', font=font, fill=words)
        top = 4 + 12
        x = 4
        d.text((x, top - cap[1]), '- ', font=font, fill=words)
        x += font.getbbox('- ')[2]
        set_line(img, x, top + (cap[3] - cap[1]) - 7, GLYPHS_8, sig)
        panels.append(img)
    eyes = Image.new('RGB', (196, 14), BASALT[1])
    set_line(eyes, 4, 3, EYE_8, (220, 220, 214), gap=2)
    panels.append(eyes)
    big = [p.resize((p.width * 3, p.height * 3), Image.NEAREST) for p in panels]
    out = Image.new('RGB', (max(b.width for b in big) + max(p.width for p in panels) + 40, sum(b.height + 10 for b in big) + 26), SHEET_BG)
    d = ImageDraw.Draw(out)
    d.text((8, 6), "the signature in the voice's order (U+E000..E006), and the eye's stages (U+E010..E017); "
                   'GUI scale 3, and 1x at right', fill=INK)
    y = 26
    for p, b in zip(panels, big):
        out.paste(b, (8, y))
        out.paste(p, (b.width + 24, y))
        y += b.height + 10
    out.save(path)


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


# --- 3 and 4. the mark ---------------------------------------------------------------------------------
#
# Format B (contract section 3): one 24 x 24 decal per stage, drawn by logic as a quad over the back
# of the right hand and wrist (4 x 4 model pixels), and the stage-7 picture again on the illagers'
# crossed arms. Glyphs 0..n-1 ring the eye at stage n, glyph 0 at the top and the rest clockwise, the
# way a count fills. Slots not yet earned are bare skin: the mark grows, it does not fill in. Every
# drawn pixel is opaque, so the quad can be cutout.

MARK_SIZE = 24
MARK_CENTRE = 12                 # the eye's middle pixel; the ring is laid round the same point
MARK_RADIUS = 7.8


def glyph_slot(i):
    """Top-left corner of glyph i's 5 x 5 box in the decal."""
    a = 2 * math.pi * i / 7
    gx = MARK_CENTRE + MARK_RADIUS * math.sin(a)
    gy = MARK_CENTRE - MARK_RADIUS * math.cos(a)
    return int(round(gx - 2)), int(round(gy - 2))


def mark(stage):
    img = Image.new('RGBA', (MARK_SIZE, MARK_SIZE), (0, 0, 0, 0))
    p = img.load()
    for i in range(stage):
        x0, y0 = glyph_slot(i)
        for x, y, _ in cells(GLYPHS[i]):
            p[x0 + x, y0 + y] = TEAL[4] + (255,)
    for x, y, ch in cells(EYE_MARK[stage]):
        p[MARK_CENTRE - 4 + x, MARK_CENTRE - 4 + y] = EYE_COLOURS[ch] + (255,)
    return img


def check_mark():
    """No two glyph boxes touch, none touches the eye, all fit the decal."""
    eye = {(MARK_CENTRE - 4 + x, MARK_CENTRE - 4 + y) for x, y, _ in cells(EYE_MARK[7])}
    boxes = []
    for i in range(7):
        x0, y0 = glyph_slot(i)
        px = {(x0 + x, y0 + y) for x, y, _ in cells(GLYPHS[i])}
        if not all(0 <= x < MARK_SIZE and 0 <= y < MARK_SIZE for x, y in px):
            raise SystemExit(f'mark: glyph {i} leaves the decal')
        halo = {(x + dx, y + dy) for x, y in px for dx in (-1, 0, 1) for dy in (-1, 0, 1)}
        if halo & eye:
            raise SystemExit(f'mark: glyph {i} touches the eye')
        for j, other in boxes:
            if halo & other:
                raise SystemExit(f'mark: glyphs {j} and {i} touch')
        boxes.append((i, px))


# --- previews of the mark, through the game's own transforms ------------------------------------------
#
# tools/render-item-model.py supplies the rasteriser, the 70-degree hand camera, the entity pose and
# the entity shader's light; this adds vanilla's first-person empty hand (ItemInHandRenderer
# .renderPlayerArm, 1.21.1) and a ModelPart cube with the skin's uv layout, so the decal is seen on
# the face the game really shows.

def _rim():
    return _load('render_item_model', 'render-item-model.py')


def first_person_arm(rim, swing=0.0, equip=0.0):
    """Camera space of the right arm's ModelPart in vanilla's first-person empty hand (equip 0 is
    fully raised; vanilla lowers the arm 0.6 per unit of equip progress). The model part keeps its
    own offset (-5, 2, 0) and the idle bob's zRot."""
    T, R = rim.T, rim.Rdeg
    f1 = math.sqrt(swing)
    m = T(-0.3 * math.sin(f1 * math.pi) + 0.64000005, 0.4 * math.sin(f1 * math.pi * 2) - 0.6 - 0.6 * equip,
          -0.4 * math.sin(swing * math.pi) - 0.71999997)
    m = m @ R('y', 45) @ R('y', math.sin(f1 * math.pi) * 70) @ R('z', math.sin(swing * swing * math.pi) * -20)
    m = m @ T(-1, 3.6, 3.5) @ R('z', 120) @ R('x', 200) @ R('y', -135) @ T(5.6, 0, 0)
    return m @ T(-5 / 16, 2 / 16, 0) @ R('z', math.degrees(0.1))


def cube_quads(rim, m, tex, u, v, origin, dims, colour=None):
    """A ModelPart cube as vanilla builds it (ModelPart.Cube): six faces with the skin layout's uvs
    from texture offset (u, v), or one flat colour per face."""
    import numpy as np
    (x0, y0, z0), (w, h, d) = origin, dims
    lo, hi = (x0, y0, z0), (x0 + w, y0 + h, z0 + d)

    def V(a, b, c):
        return (hi[0] if a else lo[0], hi[1] if b else lo[1], hi[2] if c else lo[2])
    v0, v1, v2, v3 = V(0, 0, 0), V(1, 0, 0), V(1, 1, 0), V(0, 1, 0)
    v4, v5, v6, v7 = V(0, 0, 1), V(1, 0, 1), V(1, 1, 1), V(0, 1, 1)
    f4, f5, f6, f7, f8 = u + d, u + d + w, u + d + w + w, u + d + w + d, u + 2 * d + 2 * w
    f10, f11 = v + d, v + d + h
    faces = [([v5, v4, v0, v1], f4, v, f5, f10), ([v2, v3, v7, v6], f5, f10, f6, v),
             ([v0, v4, v7, v3], u, f10, f4, f11), ([v1, v0, v3, v2], f4, f10, f5, f11),
             ([v5, v1, v2, v6], f5, f10, f7, f11), ([v4, v5, v6, v7], f7, f10, f8, f11)]
    out = []
    for verts, ua, va, ub, vb in faces:
        pos = np.c_[np.array(verts, float) / 16.0, np.ones(4)] @ m.T
        if tex is None:
            out.append(rim.Quad(pos[:, :3], color=np.array(colour)))
        else:
            th, tw = tex.shape[:2]
            uvs = np.array([(ub, va), (ua, va), (ua, vb), (ub, vb)]) / np.array([tw, th])
            out.append(rim.Quad(pos[:, :3], uvs, tex))
    return out


def decal_quad(rim, m, img, corners):
    """The mark as its own full-bright quad. corners: model-space points for uv (0,0), (0,1), (1,1),
    (1,0) -- the texture's top-left, bottom-left, bottom-right, top-right."""
    import numpy as np
    tex = np.asarray(img, dtype=np.float32) / 255
    pos = np.c_[np.array(corners, float) / 16.0, np.ones(4)] @ m.T
    return rim.Quad(pos[:, :3], np.array([(0, 0), (0, 1), (1, 1), (1, 0)], float), tex, emissive=True)


def arm_decal(img, slim=False):
    """Contract section 3: outer face, z -2..2, y 5..9, 0.3 px out; top toward the hand end."""
    x = (-2 if slim else -3) - 0.3
    return img, [(x, 9, -2), (x, 5, -2), (x, 5, 2), (x, 9, 2)]


def illager_decal(img):
    """Contract section 4: the crossed arms' middle box, front face, x -2..2, y 2..6, 0.1 px out."""
    return img, [(-2, 2, -2.1), (-2, 6, -2.1), (2, 6, -2.1), (2, 2, -2.1)]


def stand_in_skin():
    """A plain skin of our own for the previews: tan arm, a short blue sleeve, like nobody's."""
    import numpy as np
    s = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    p = s.load()
    for y in range(16, 32):
        for x in range(40, 56):
            if y >= 20 or 44 <= x < 52:
                sleeve = y < 23 or (y < 20 and x < 48)
                p[x, y] = (54, 96, 150, 255) if sleeve else (184, 134, 102, 255)
    return np.asarray(s, dtype=np.float32) / 255


def held_item_quads(rim, model_ref='ascension_worlds:item/gateway_key'):
    model = rim.load_model(model_ref)
    return rim.transformed(rim.model_quads(model),
                           rim.FIRST_PERSON_HAND @ rim.item_matrix(model, 'firstperson_righthand'))


def first_person_views(path, stages=(1, 4, 7)):
    import numpy as np
    rim = _rim()
    size = (1920, 1080)
    proj = rim.perspective(70, size[0] / size[1])
    skin = stand_in_skin()
    crop = (1060, 560, 1780, 1080)
    w, h = crop[2] - crop[0], crop[3] - crop[1]
    rows = []
    # (label, night, held item, whole screen instead of the crop)
    for label, night, item, whole in (
            ('empty hand, day', False, None, False),
            ('empty hand, night (the mark is full-bright)', True, None, False),
            ('holding a sword, if logic draws the arm at rest behind it (a combat-module sword as a prop)',
             False, 'ascension_combat:item/divine_sunderer', False),
            ('the whole screen, empty hand', False, None, True)):
        row = []
        for st in stages:
            m = first_person_arm(rim)
            q = cube_quads(rim, m, skin, 40, 16, (-3, -2, -2), (4, 12, 4))
            img, corners = arm_decal(mark(st))
            q.append(decal_quad(rim, m, img, corners))
            if item:
                q += held_item_quads(rim, item)
            bg = rim.background(size, 'night' if night else 'day')
            shot = rim.to_image(rim.rasterise(q, proj, np.eye(4), size, bg, night))
            if whole:
                shot = shot.resize((w, w * size[1] // size[0]), Image.LANCZOS)
            else:
                shot = shot.crop(crop)
            row.append(shot)
        rows.append((label, row))
    heights = [max(im.height for im in row) for _, row in rows]
    out = Image.new('RGB', (len(stages) * (w + 8), sum(hh + 22 for hh in heights) + 24), SHEET_BG)
    d = ImageDraw.Draw(out)
    d.text((6, 6), 'the mark in first person, stages ' + ', '.join(map(str, stages)) +
           ': vanilla empty-hand pose, 70-degree hand camera, 1920 x 1080 (crop)', fill=INK)
    y = 24
    for (label, row), hh in zip(rows, heights):
        d.text((6, y + 2), label, fill=INK)
        for i, im in enumerate(row):
            out.paste(im, (i * (w + 8), y + 18))
        y += hh + 22
    out.save(path)


def stand_in_player(rim, pose, body, slim=False):
    """Flat-coloured humanoid in model space (our own stand-in, as render-item-model's)."""
    skin, shirt, pants, hair = (0.72, 0.52, 0.40, 1), (0.21, 0.38, 0.59, 1), (0.16, 0.22, 0.40, 1), (0.25, 0.17, 0.1, 1)
    q = []
    q += cube_quads(rim, pose, None, 0, 0, (-4, -8, -4), (8, 8, 8), skin)
    q += cube_quads(rim, pose @ rim.T(0, -8.2 / 16, 0), None, 0, 0, (-4.2, 0, -4.2), (8.4, 2, 8.4), hair)
    q += cube_quads(rim, pose, None, 0, 0, (-4, 0, -2), (8, 12, 4), shirt)
    arm = pose @ body.arm('right')
    q += cube_quads(rim, arm, None, 0, 0, (-2 if slim else -3, -2, -2), (3 if slim else 4, 12, 4), skin)
    q += cube_quads(rim, arm, None, 0, 0, (-3.25, -2.25, -2.25), (4.5, 3.5, 4.5), shirt)
    q += cube_quads(rim, pose @ body.arm('left'), None, 0, 0, (-1, -2, -2), (4, 12, 4), skin)
    q += cube_quads(rim, pose @ body.arm('left'), None, 0, 0, (-1.25, -2.25, -2.25), (4.5, 3.5, 4.5), shirt)
    for side in (-1, 1):
        q += cube_quads(rim, pose @ rim.T(side * 1.9 / 16, 12 / 16, 0), None, 0, 0, (-2, 0, -2), (4, 12, 4), pants)
    return q, arm


def stand_in_illager(rim, pose):
    """A flat-coloured illager in IllagerModel's proportions: crossed arms on the 'arms' part."""
    skin, robe, dark = (0.55, 0.57, 0.56, 1), (0.30, 0.31, 0.34, 1), (0.20, 0.20, 0.23, 1)
    q = cube_quads(rim, pose, None, 0, 0, (-4, -10, -4), (8, 10, 8), skin)
    q += cube_quads(rim, pose @ rim.T(0, -2 / 16, 0), None, 0, 0, (-1, -1, -6), (2, 4, 2), skin)
    q += cube_quads(rim, pose, None, 0, 0, (-4, 0, -3), (8, 12, 6), robe)
    q += cube_quads(rim, pose, None, 0, 0, (-4.5, 0, -3.5), (9, 20, 7), dark)
    arms = pose @ rim.T(0, 3 / 16, -1 / 16) @ rim.Rdeg('x', math.degrees(-0.75))
    q += cube_quads(rim, arms, None, 0, 0, (-8, -2, -2), (4, 8, 4), robe)
    q += cube_quads(rim, arms, None, 0, 0, (4, -2, -2), (4, 8, 4), robe)
    q += cube_quads(rim, arms, None, 0, 0, (-4, 2, -2), (8, 4, 4), skin)
    for side in (-1, 1):
        q += cube_quads(rim, pose @ rim.T(side * 2 / 16, 12 / 16, 0), None, 0, 0, (-2, 0, -2), (4, 12, 4), dark)
    return q, arms


def third_person_views(path):
    """Stages 1, 4 and 7 on a stand-in player seen from its right; then the player at stage 7 beside
    an illager, both facing the camera, at a few blocks and close up."""
    import numpy as np
    rim = _rim()
    relaxed = rim.Body(right=(0, 0, 5.7), left=(0, 0, -5.7))
    size = (420, 520)
    proj = rim.perspective(50, size[0] / size[1])
    shots = []
    for st in (1, 4, 7):
        q, arm = stand_in_player(rim, rim.entity_pose(), relaxed)
        img, corners = arm_decal(mark(st))
        q.append(decal_quad(rim, arm, img, corners))
        view = rim.look_at((-1.7, 1.25, 0.9), (-0.32, 0.72, 0.0))
        shots.append((f'stage {st}, from the right', rim.to_image(
            rim.rasterise(q + rim.ground_quads(), proj, view, size, rim.background(size, 'flat')))))
    # side by side: the player (stage 7) at x -0.6, an illager at x +0.6, both facing south (+z)
    pair = []
    q, arm = stand_in_player(rim, rim.T(-0.6, 0, 0) @ rim.entity_pose(), relaxed)
    img, corners = arm_decal(mark(7))
    q.append(decal_quad(rim, arm, img, corners))
    iq, arms = stand_in_illager(rim, rim.T(0.6, 0, 0) @ rim.entity_pose())
    img, corners = illager_decal(Image.open(os.path.join(TEX, 'entity/mark/illager.png')).convert('RGBA'))
    iq.append(decal_quad(rim, arms, img, corners))
    for label, eye, target in (('the player at stage 7 beside an illager, 3 blocks off', (-0.9, 1.5, 3.2), (0, 1.0, 0)),
                               ('closer, from the front-right', (-1.3, 1.25, 1.7), (0, 0.85, 0))):
        im = rim.to_image(rim.rasterise(q + iq + rim.ground_quads(), proj, rim.look_at(eye, target), size,
                                        rim.background(size, 'flat')))
        pair.append((label, im))
    allshots = shots + pair
    out = Image.new('RGB', (len(allshots) * (size[0] + 6), size[1] + 40), SHEET_BG)
    d = ImageDraw.Draw(out)
    for i, (label, im) in enumerate(allshots):
        out.paste(im, (i * (size[0] + 6), 36))
        d.text((i * (size[0] + 6) + 4, 6), label, fill=INK)
    d.text((4, 20), 'stand-in figures in flat colours; the decals are the real textures', fill=INK)
    out.save(path)


def mark_sheet(path, made):
    """Every stage flat at 8x on three skin tones, and the illager's copy."""
    tones = [(236, 200, 176), (184, 134, 102), (92, 60, 44)]
    Z = 8
    S = MARK_SIZE * Z
    out = Image.new('RGB', (8 * (S + 10) + 10, len(tones) * (S + 10) + 40), SHEET_BG)
    d = ImageDraw.Draw(out)
    d.text((10, 8), 'mark/stage_1..7 and mark/illager (24 x 24, at 8x) on pale, mid and dark skin', fill=INK)
    for r, tone in enumerate(tones):
        for i, img in enumerate(made):
            bg = Image.new('RGBA', img.size, tone + (255,))
            bg.alpha_composite(img)
            out.paste(bg.resize((S, S), Image.NEAREST), (10 + i * (S + 10), 30 + r * (S + 10)))
    out.save(path)


# --- 5. the pylon's glyphs -----------------------------------------------------------------------------
#
# ascension_worlds:mark_glyph, glyph 0..6, lit false/true. Each glyph is cut at double size (10 x 10,
# from its 5 x 5 form) into a dressed anorthosite panel bevelled like chiseled anorthosite, so a column
# of them reads as an inscription set into the pylon's pale shaft. Light from the top-left: inside the
# cut, the near (top and left) wall is in shadow and the far wall is lit, and the surface just past the
# far rim catches the light. Lit, a full-bright second layer fills the cut with teal, the way the
# gatestone's runes glow, breathing slowly; each glyph's breath is a seventh of a cycle behind the one
# above it, so a lit column reads as one inscription waking rather than seven lamps.

GLYPH_AT = 3                    # the doubled glyph sits 3 px in from the face's top-left
GLOW_FRAMES = 16


def glyph_cut(k):
    """The texels cut for glyph k on a 16 x 16 face -> (cut, near wall, far rim on the surface)."""
    cut = {(GLYPH_AT + 2 * x + dx, GLYPH_AT + 2 * y + dy)
           for x, y, _ in cells(GLYPHS[k]) for dx in (0, 1) for dy in (0, 1)}
    near = {(x, y) for x, y in cut if (x, y - 1) not in cut or (x - 1, y) not in cut}
    rim = {(x, y) for x in range(16) for y in range(16)
           if (x, y) not in cut and ((x - 1, y) in cut or (x, y - 1) in cut)}
    return cut, near, rim


def glyph_face(k):
    img = art.polished(ANORTH, random.Random(500 + k), base_idx=3)
    p = img.load()
    for i in range(2, 14):                                   # the panel's groove, as chiseled's
        p[i, 1] = ANORTH[2] + (255,)
        p[1, i] = ANORTH[2] + (255,)
    cut, near, rim = glyph_cut(k)
    for x, y in rim:
        p[x, y] = mix(p[x, y][:3], ANORTH[5], 0.7) + (255,)
    for x, y in cut:
        p[x, y] = (ANORTH[0] if (x, y) in near else ANORTH[1]) + (255,)
    return img


def glyph_glow(k):
    strip = new(16, 16 * GLOW_FRAMES)
    p = strip.load()
    cut, near, _ = glyph_cut(k)
    for f in range(GLOW_FRAMES):
        b = 0.5 - 0.5 * math.cos(2 * math.pi * (f / GLOW_FRAMES - k / 7))
        for x, y in cut:
            c = mix(TEAL[2], TEAL[3], 0.3 + 0.5 * b) if (x, y) in near else mix(TEAL[4], TEAL[5], 0.1 + 0.45 * b)
            p[x, y + 16 * f] = c + (255,)
    return strip


GLOW = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}
SIDES = ('north', 'south', 'west', 'east')


def glyph_models():
    variants = {}
    end = f'{NS}:block/anorthosite_bricks'
    for k in range(7):
        side = f'{NS}:block/mark_glyph/glyph_{k}'
        faces = {f: {'texture': '#side', 'cullface': f} for f in SIDES}
        faces.update({f: {'texture': '#end', 'cullface': f} for f in ('down', 'up')})
        write_json(f'models/block/mark_glyph/glyph_{k}.json', {
            'parent': 'minecraft:block/block',
            'textures': {'particle': side, 'side': side, 'end': end},
            'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces}],
        })
        write_json(f'models/block/mark_glyph/glyph_{k}_lit.json', {
            'parent': 'minecraft:block/block',
            'render_type': 'minecraft:cutout',
            'textures': {'particle': side, 'side': side, 'end': end, 'glow': side + '_glow'},
            'elements': [
                {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces},
                {'from': [0, 0, 0], 'to': [16, 16, 16], 'shade': False, 'neoforge_data': GLOW,
                 'faces': {f: {'texture': '#glow', 'cullface': f} for f in SIDES}},
            ],
        })
        for lit in (False, True):
            variants[f'glyph={k},lit={str(lit).lower()}'] = {
                'model': f'{NS}:block/mark_glyph/glyph_{k}' + ('_lit' if lit else '')}
    write_json('blockstates/mark_glyph.json', {'variants': variants})
    write_json('models/item/mark_glyph.json', {'parent': f'{NS}:block/mark_glyph/glyph_0'})


def glyph_block_sheet(path):
    """Every state of mark_glyph drawn from its own model through the hand-camera rasteriser (the
    structure tool's render_block_model draws a mirror image), unlit above and lit below, and the
    seven faces flat at 8x."""
    import numpy as np
    rim, rsp = _rim(), _load('render_structure_preview', 'render-structure-preview.py')
    size = (330, 330)
    proj = rim.perspective(40, 1.0)
    view = rim.look_at((2.45, 1.95, 2.75), (0.5, 0.48, 0.5))
    tiles = []
    for lit in ('false', 'true'):
        row = []
        for k in range(7):
            q = scene_quads(rim, rsp, {(0, 0, 0): f'{NS}:mark_glyph[glyph={k},lit={lit}]'})
            bg = np.zeros((size[1], size[0], 3), np.float32) + np.array([0.16, 0.17, 0.2], np.float32)
            row.append(rim.to_image(rim.rasterise(q, proj, view, size, bg)).resize((220, 220), Image.LANCZOS))
        tiles.append(row)
    w, h = tiles[0][0].size
    flat = [glyph_face(k).resize((128, 128), Image.NEAREST) for k in range(7)]
    out = Image.new('RGB', (7 * (w + 6) + 10, 2 * (h + 6) + 128 + 60), SHEET_BG)
    d = ImageDraw.Draw(out)
    d.text((8, 6), 'mark_glyph, glyph 0-6: unlit, lit (glow layer full-bright, first frame), and the faces at 8x',
           fill=INK)
    for r, row in enumerate(tiles):
        for k, im in enumerate(row):
            out.paste(im, (8 + k * (w + 6), 24 + r * (h + 6)))
    for k, im in enumerate(flat):
        out.paste(im, (8 + k * (w + 6) + (w - 128) // 2, 24 + 2 * (h + 6) + 10))
    out.save(path)


# --- the pylon in the world: a textured view of the Archive's template ---------------------------------
#
# The structure preview tool paints each block in its texture's average colour, which is right at 64
# blocks and useless for reading a carving from the court. This draws the template's real block
# models (their elements, uvs and full-bright layers, first animation frame) through the same
# rasteriser and hand camera as the mark's previews, at a player's eye height on a black sky.

CUBE_FACES = {
    'minecraft:block/cube_all': {f: '#all' for f in ('down', 'up', 'north', 'south', 'west', 'east')},
    'minecraft:block/cube_column': dict({f: '#side' for f in SIDES}, down='#end', up='#end'),
    'minecraft:block/cube_column_horizontal': dict({f: '#side' for f in ('down', 'up', 'west', 'east')},
                                                   north='#end', south='#end'),
    'minecraft:block/cube_bottom_top': dict({f: '#side' for f in SIDES}, down='#bottom', up='#top'),
}
SLAB_PARENTS = {'minecraft:block/slab': (0, 8), 'minecraft:block/slab_top': (8, 16)}


def block_model(rsp, ref):
    """A block model with its parents folded in, vanilla's cube and slab parents expanded."""
    textures, elements = {}, None
    while ref and ref not in CUBE_FACES and ref not in SLAB_PARENTS and ref != 'minecraft:block/block':
        ns, path = ref.split(':') if ':' in ref else ('minecraft', ref)
        found = rsp._find(ns, 'models', path + '.json')
        if not found:
            return None
        m = json.load(open(found))
        for k, v in m.get('textures', {}).items():
            textures.setdefault(k, v)
        if elements is None and 'elements' in m:
            elements = m['elements']
        ref = m.get('parent')
    if elements is None and ref in CUBE_FACES:
        elements = [{'from': [0, 0, 0], 'to': [16, 16, 16],
                     'faces': {f: {'texture': t, 'cullface': f} for f, t in CUBE_FACES[ref].items()}}]
    elif elements is None and ref in SLAB_PARENTS:
        y0, y1 = SLAB_PARENTS[ref]
        faces = {f: {'texture': '#side', 'uv': [0, 16 - y1, 16, 16 - y0], 'cullface': f} for f in SIDES}
        faces['down'] = {'texture': '#bottom', 'cullface': 'down'}
        faces['up'] = {'texture': '#top'} if y1 < 16 else {'texture': '#top', 'cullface': 'up'}
        elements = [{'from': [0, y0, 0], 'to': [16, y1, 16], 'faces': faces}]
    return None if elements is None else {'textures': textures, 'elements': elements}


def _variant(rsp, state):
    name, props = rsp.split_state(state)
    ns, path = name.split(':')
    bs = rsp._find(ns, 'blockstates', path + '.json')
    if not bs:
        return None
    for key, v in json.load(open(bs)).get('variants', {}).items():
        want = dict(kv.split('=', 1) for kv in key.split(',') if kv)
        if all(props.get(k) == val for k, val in want.items()):
            return v[0] if isinstance(v, list) else v
    return None


def scene_quads(rim, rsp, vox):
    """Every visible face of a template's blocks, in block units, from their own models. A state
    with no model we can read (stairs, vanilla blocks) falls back to its rough boxes in flat colour."""
    import numpy as np
    cache = {}

    def first_frame(model, ref):
        while ref.startswith('#'):
            ref = model['textures'].get(ref[1:], '')
        if ref not in cache:
            ns, path = ref.split(':')
            im = Image.open(rsp._find(ns, 'textures', path + '.png')).convert('RGBA')
            cache[ref] = np.asarray(im.crop((0, 0, im.width, im.width)), dtype=np.float32) / 255
        return cache[ref]
    rim.texture = first_frame                     # element_quads looks it up by name: the first frame
    full = {pos for pos, st in vox.items() if rsp.boxes_of(st) == rsp.FULL}
    normal = {'down': (0, -1, 0), 'up': (0, 1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1),
              'west': (-1, 0, 0), 'east': (1, 0, 0)}
    out = []
    for (x, y, z), st in vox.items():
        if rsp.boxes_of(st) == []:
            continue
        v = _variant(rsp, st)
        model = block_model(rsp, v['model']) if v else None
        place = rim.T(x, y, z)
        if model is None or rsp.split_state(st)[0].endswith('_stairs'):
            col = rsp.state_colour(st)
            for b in rsp.boxes_of(st):
                out += cube_quads(rim, place @ rim.S(16), None, 0, 0, b[:3], tuple(b[i + 3] - b[i] for i in range(3)),
                                  tuple(c / 255 for c in col) + (1,))
            continue
        turn = (rim.T(0.5, 0.5, 0.5) @ rim.Rdeg('y', -v.get('y', 0)) @ rim.Rdeg('x', -v.get('x', 0))
                @ rim.T(-0.5, -0.5, -0.5))
        for el in model['elements']:
            glow = el.get('neoforge_data', {}).get('block_light', 0) > 0
            keep = {}
            for f, fd in el['faces'].items():
                n = normal[fd.get('cullface', f)] if fd.get('cullface') else None
                if n and (x + n[0], y + n[1], z + n[2]) in full and not v.get('x') and not v.get('y'):
                    continue
                keep[f] = fd
            if not keep:
                continue
            m = place @ turn @ rim.S(1 / 16)
            if glow:                              # lift the full-bright layer off its base face
                m = m @ rim.T(8, 8, 8) @ rim.S(1.004) @ rim.T(-8, -8, -8)
            sub = {'textures': model['textures'], 'elements': [dict(el, faces=keep)]}
            out += rim.transformed(rim.element_quads(sub), m)
    return out


GROUND = (0.42, 0.42, 0.41)       # the plain beyond the template, a flat regolith grey


def ground_quads(rim, vox, y_top, margin=6):
    """The ground in and round the template's footprint wherever the template does not set it
    itself, at the paving's height, so it hides what lies below the paving. The plain beyond is
    painted into the background (see shot)."""
    import numpy as np
    xs = [p[0] for p in vox]
    zs = [p[2] for p in vox]
    c = np.array(GROUND + (1,))
    y = y_top - 0.002
    defined = {(x, z) for (x, yy, z), st in vox.items() if y_top - 9 <= yy < y_top and rsp_solid(st)}
    q = []
    x0, x1, z0, z1 = min(xs), max(xs), min(zs), max(zs)
    for x in range(x0 - margin, x1 + margin + 1):
        for z in range(z0 - margin, z1 + margin + 1):
            if (x, z) not in defined:
                q.append(rim.Quad(np.array([(x, y, z), (x, y, z + 1), (x + 1, y, z + 1), (x + 1, y, z)], float),
                                  color=c))
    return q


def rsp_solid(state):
    return not (state.startswith('minecraft:air') or state.startswith('minecraft:structure_void'))


def black_sky(size, seed=3):
    import numpy as np
    w, h = size
    img = np.zeros((h, w, 3), np.float32) + np.array([0.016, 0.02, 0.035], np.float32)
    rng = random.Random(seed)
    for _ in range(int(w * h / 3000)):
        img[rng.randrange(h), rng.randrange(w)] = rng.uniform(0.55, 0.92)
    return img


def pylon_views(court_path, far_path):
    import numpy as np
    from_archive = _load('lunar_archive', 'generate-lunar-archive.py')
    rim, rsp = _rim(), _load('render_structure_preview', 'render-structure-preview.py')
    _, vox, _ = rsp.load_nbt(os.path.join(from_archive.OUT, 'entrance.nbt'))
    S = from_archive.SURFACE_Y
    vox = {p: st for p, st in vox.items() if p[1] >= S - 9}
    glyph_at = {k: p for p, st in vox.items() if 'mark_glyph' in st
                for k in [int(rsp.split_state(st)[1]['glyph'])]}

    def lit(n):
        out = dict(vox)
        for k, p in glyph_at.items():
            if k < n:
                out[p] = f'{NS}:mark_glyph[glyph={k},lit=true]'
        return out
    feet = S + 1
    gx, gy, gz = glyph_at[3]
    column = (gx + 1, gy + 0.5, gz + 0.5)            # the middle of the glyph face (it faces +x)
    big = (1920, 1200)

    def shot(scene, eye, target, night, size=big, crop_at=None, surface_only=False):
        proj = rim.perspective(70, size[0] / size[1])
        view = rim.look_at(eye, target)
        if surface_only:                     # from afar, nothing below the paving shows
            scene = {p: st for p, st in scene.items() if p[1] >= S}
        q = scene_quads(rim, rsp, scene) + ground_quads(rim, scene, feet)
        bg = black_sky(size)
        ahead = np.array(target, float) - np.array(eye, float)
        ahead[1] = 0
        far = np.array(eye, float) + ahead / np.linalg.norm(ahead) * 1e5
        far[1] = feet
        h = proj @ view @ np.append(far, 1.0)
        horizon = int(max(0, min(size[1], (1 - h[1] / h[3]) / 2 * size[1])))
        bg[horizon:] = np.array(GROUND, np.float32) * (rim.NIGHT if night else 1.0)
        im = rim.to_image(rim.rasterise(q, proj, view, size, bg, night))
        if crop_at is None:
            return im.resize((size[0] // 2, size[1] // 2), Image.LANCZOS)
        c = proj @ view @ np.array(list(crop_at) + [1.0])
        sx, sy = (c[0] / c[3] + 1) / 2 * size[0], (1 - c[1] / c[3]) / 2 * size[1]
        return im.crop((int(sx) - 240, int(sy) - 300, int(sx) + 240, int(sy) + 300))
    court_eye = (gx + 9.5, feet + 1.62, gz + 5.5)
    _strip([('the pylon from the court, at eye height, 11 blocks off', shot(lit(0), court_eye, column, False)),
            ('the band, unlit (1920 x 1200, native crop)', shot(lit(0), court_eye, column, False, crop_at=column)),
            ('three gates lit (glyphs 0-2)', shot(lit(3), court_eye, column, False, crop_at=column)),
            ('all seven lit, at night', shot(lit(7), court_eye, column, True, crop_at=column))], court_path)
    far_eye = (gx + 46, feet + 1.62, gz + 45)
    target = (gx, feet + 11, gz)
    _strip([('from 64 blocks south-east: unlit', shot(lit(0), far_eye, target, False, surface_only=True)),
            ('all seven lit', shot(lit(7), far_eye, target, False, surface_only=True)),
            ('all seven lit, at night', shot(lit(7), far_eye, target, True, surface_only=True))], far_path)


def _strip(panels, path):
    h = max(im.height for _, im in panels)
    out = Image.new('RGB', (sum(im.width + 6 for _, im in panels), h + 22), SHEET_BG)
    d = ImageDraw.Draw(out)
    x = 0
    for label, im in panels:
        out.paste(im, (x, 22))
        d.text((x + 4, 5), label, fill=INK)
        x += im.width + 6
    out.save(path)


def room_shot(rim, rsp, vox, eye, target, size=(1440, 900), box=None, background=(0.05, 0.05, 0.07)):
    """A textured view inside a template (no sky, no plain), cropped to `box` cells first."""
    import numpy as np
    if box:
        (x0, x1), (y0, y1), (z0, z1) = box
        vox = {p: st for p, st in vox.items() if x0 <= p[0] <= x1 and y0 <= p[1] <= y1 and z0 <= p[2] <= z1}
    proj = rim.perspective(70, size[0] / size[1])
    bg = np.zeros((size[1], size[0], 3), np.float32) + np.array(background, np.float32)
    im = rim.to_image(rim.rasterise(scene_quads(rim, rsp, vox), proj, rim.look_at(eye, target), size, bg))
    return im.resize((size[0] * 2 // 3, size[1] * 2 // 3), Image.LANCZOS)


# --- 7 and 8 in the world: the record's frame and the keeper shrine ---------------------------------------

def record_layout(path):
    """The handover drawing: the record at 4x with every frame cell's (col, row) and tile number,
    as the reader standing in the sealed room sees the frame."""
    from_archive = _load('lunar_archive', 'generate-lunar-archive.py')
    aa = from_archive.art
    i = [n for n, _ in aa._ORDER].index('sealers_record')
    tw, th = aa._ORDER[i][1]
    c = aa.Canvas(tw * 16, th * 16, seed=100 + i)
    aa.MURAL_FUNCS['sealers_record'](c)
    base, glow = c.render()
    Z = 4
    img = Image.alpha_composite(base, glow).resize((tw * 16 * Z, th * 16 * Z), Image.NEAREST).convert('RGB')
    d = ImageDraw.Draw(img)
    for x, y, z, col, row, tile in from_archive.record_frame():
        px, py = col * 16 * Z, row * 16 * Z
        d.rectangle([px, py, px + 16 * Z - 1, py + 16 * Z - 1], outline=(250, 200, 80))
        d.rectangle([px + 2, py + 2, px + 58, py + 26], fill=(20, 20, 24))
        d.text((px + 5, py + 3), f'{col},{row}', fill=(250, 220, 140))
        d.text((px + 5, py + 14), f'tile {tile}', fill=(230, 230, 230))
    out = Image.new('RGB', (img.width + 20, img.height + 40), SHEET_BG)
    out.paste(img, (10, 30))
    ImageDraw.Draw(out).text((10, 8), "the Sealers' Record as the frame takes it: (col, row) from the reader's "
                                     'top-left, and the archive_mural tile that belongs there', fill=INK)
    out.save(path)


def sealed_room_views(path):
    """The sealed room's back wall from the breach: the frame as the template leaves it (every tile
    torn out), and with the record put back. Lit by the preview's sun, not by the room's own script."""
    from_archive = _load('lunar_archive', 'generate-lunar-archive.py')
    rim, rsp = _rim(), _load('render_structure_preview', 'render-structure-preview.py')
    shots = []
    for restored in (False, True):
        p = from_archive.vault()
        if restored:
            from_archive.sealed_room(p, restored=True)
        vox = {pos: st for pos, (_, st) in p.cells.items()}
        L = from_archive.LOW
        shots.append(('the frame as the template leaves it' if not restored else 'the record put back',
                      room_shot(rim, rsp, vox, (12.5, L + 2.9, 29.6), (12.5, L + 4.6, 23),
                                box=((6, 18), (0, L + 9), (21, 29)))))
    _strip(shots, path)


def shrine_views(path):
    from_archive = _load('lunar_archive', 'generate-lunar-archive.py')
    rim, rsp = _rim(), _load('render_structure_preview', 'render-structure-preview.py')
    _, vox, _ = rsp.load_nbt(os.path.join(from_archive.SHRINE_OUT, 'keeper_shrine.nbt'))
    F = from_archive.SHRINE_FLOOR
    shots = [('keeper_shrine from 8 blocks, at eye height', room_shot(
                 rim, rsp, vox, (2.0, F + 2.62, -7.5), (4.5, F + 3, 4), background=(0.016, 0.02, 0.035))),
             ('from its door', room_shot(rim, rsp, vox, (4.5, F + 2.62, 0.6), (4.5, F + 3.4, 8)))]
    _strip(shots, path)


# --- 9. the light on the Moon ---------------------------------------------------------------------------
#
# environment/moon_signal.png, 16 x 16: a point of teal light, for logic to draw over the vanilla moon
# in Earth's night sky once a gate is lit. A white-hot core and a halo that falls off as a diamond
# (Manhattan distance blended with round), so through a spyglass the light has the network's shape;
# it has no rays, which at this size would be a cross. Alpha carries the falloff, for additive
# blending the way the sun is drawn.

def moon_signal():
    img = new()
    p = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            d = 0.65 * (dx + dy - 1) + 0.35 * math.hypot(dx, dy)
            if d < 0.6:
                c, a = TEAL[5], 255
            elif d < 1.6:
                c, a = mix(TEAL[4], TEAL[5], 0.4), 255
            elif d < 2.7:
                c, a = TEAL[4], 215
            elif d < 4.0:
                c, a = TEAL[3], 130
            elif d < 5.4:
                c, a = TEAL[2], 60
            elif d < 6.6:
                c, a = TEAL[2], 22
            else:
                continue
            p[x, y] = c + (a,)
    return img


def stand_in_moon(size, rng):
    """A grey disc of our own with a few maria, standing in for vanilla's moon (never read here)."""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    p = img.load()
    maria = [(rng.uniform(0.25, 0.75) * size, rng.uniform(0.25, 0.75) * size, rng.uniform(0.08, 0.18) * size)
             for _ in range(5)]
    r = size / 2 - 0.5
    for y in range(size):
        for x in range(size):
            if math.hypot(x - r, y - r) > r:
                continue
            v = 0.82
            if any(math.hypot(x - mx, y - my) < mr for mx, my, mr in maria):
                v = 0.66
            g = int(225 * v)
            p[x, y] = (g, g, g - 6, 255)
    return img


def moon_signal_preview(path, signal):
    """The signal over a stand-in moon in a night sky: as the moon appears at the default field of
    view (its 32-pixel face about 175 px across at 1080p, the disc half that), and through a
    spyglass (ten times closer), drawn additively at a quarter of the moon quad's width."""
    rng = random.Random(9)
    panels = []
    for label, moon_px in (('the night sky at 1080p, default field of view', 88),
                           ('through a spyglass', 880)):
        W, H = 760, 460
        sky = Image.new('RGB', (W, H), (6, 8, 18))
        sp = sky.load()
        for _ in range(160):
            s = rng.randrange(120, 230)
            sp[rng.randrange(W), rng.randrange(H)] = (s, s, s)
        moon = stand_in_moon(16, random.Random(4)).resize((moon_px, moon_px), Image.NEAREST)
        cx, cy = W // 2, H // 2
        sky.paste(moon, (cx - moon_px // 2, cy - moon_px // 2), moon)
        sig = signal.resize((moon_px // 2, moon_px // 2), Image.NEAREST)
        ox, oy = cx - moon_px // 8 - sig.width // 2, cy - moon_px // 7 - sig.height // 2
        base = sky.load()
        q = sig.load()
        for y in range(sig.height):
            for x in range(sig.width):
                r, g, b, a = q[x, y]
                X, Y = ox + x, oy + y
                if a and 0 <= X < W and 0 <= Y < H:
                    k = a / 255
                    o = base[X, Y]
                    base[X, Y] = tuple(min(255, int(o[i] + (r, g, b)[i] * k)) for i in range(3))
        panels.append((label, sky))
    _strip(panels, path)


def main():
    os.makedirs(DESIGN, exist_ok=True)
    script_sheet(os.path.join(DESIGN, 'script.png'))

    art.save(font_texture(), 'font/script.png')
    write_json('font/script.json', font_json())
    font_preview(os.path.join(DESIGN, 'font-signature.png'))

    check_mark()
    marks = [mark(st) for st in range(1, 8)]
    for st, img in enumerate(marks, start=1):
        art.save(img, f'entity/mark/stage_{st}.png')
    art.save(mark(7), 'entity/mark/illager.png')        # the same picture: the player's becomes theirs
    mark_sheet(os.path.join(DESIGN, 'mark-stages.png'), marks + [mark(7)])

    for k in range(7):
        art.save(glyph_face(k), f'block/mark_glyph/glyph_{k}.png')
        art.save(glyph_glow(k), f'block/mark_glyph/glyph_{k}_glow.png')
        art.write_mcmeta(f'block/mark_glyph/glyph_{k}_glow.png', frametime=3, interpolate=True)
    glyph_models()
    glyph_block_sheet(os.path.join(DESIGN, 'mark-glyph-blocks.png'))

    signal = moon_signal()
    art.save(signal, 'environment/moon_signal.png')
    moon_signal_preview(os.path.join(DESIGN, 'moon-signal.png'), signal)

    if '--no-3d' not in sys.argv:
        first_person_views(os.path.join(DESIGN, 'mark-first-person.png'))
        third_person_views(os.path.join(DESIGN, 'mark-third-person.png'))
        pylon_views(os.path.join(DESIGN, 'pylon-court.png'), os.path.join(DESIGN, 'pylon-64.png'))
        sealed_room_views(os.path.join(DESIGN, 'record-frame.png'))
        shrine_views(os.path.join(DESIGN, 'keeper-shrine.png'))
    record_layout(os.path.join(DESIGN, 'record-layout.png'))
    print('wrote the script, its font, the mark, the pylon glyphs, the moon signal and their previews')


if __name__ == '__main__':
    main()
