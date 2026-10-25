#!/usr/bin/env python3
"""Blueprint sheets for the quest book's project pages (docs/progression/dr-stone-roadmap.md). Original, seeded, drawn here:
nothing copied from another mod, a pack or the series that inspired the idea. Needs Pillow and numpy.

    python tools/generate-blueprint-art.py

Writes, under modules/compat/ftbquests/src/main/resources/assets/ascension_compat_ftbquests/textures/quests/:

  blueprint/<page>.png     one sheet per project page, as large as that page's layout (tools/quests/layout.py), so it is
                           drawn again whenever a page gains or loses a quest. Blue paper, a grid that lines up with the quests,
                           a frame with ruler marks, the step number of every row, a heading, a line drawing of the project's
                           motif and a title block that says which sheet it is, what the finale is and what it pays.
  icons/<page>.png         the page's round chapter icon: the motif in a brass rim, like the other chapter icons.

The sheet's picture is exactly the shape of its box, because the generator and tools/quests/validate.py agree on it: a page
whose sheet has the wrong shape fails the build.
Deterministic: every random choice comes from a fixed seed, so a rerun changes nothing.
"""

import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(ROOT, 'pack', 'quests'))

ASSETS = os.path.join(ROOT, 'modules', 'compat', 'ftbquests', 'src', 'main', 'resources', 'assets')
OUT = os.path.join(ASSETS, 'ascension_compat_ftbquests', 'textures', 'quests')
SHAPES = os.path.join(ASSETS, 'ftbquests', 'textures', 'shapes')
FONT = os.path.join(ROOT, 'modules', 'hud', 'src', 'main', 'resources', 'assets', 'ascension_hud', 'font', 'barlow_semibold.ttf')

PX = 56                      # pixels to one position unit on a sheet
K = 2                        # supersampling for the line work

PAPER_TOP = (17, 48, 90)
PAPER_BOTTOM = (9, 30, 62)
GRID = (134, 192, 238)
INK = (228, 240, 252)
INK_DIM = (150, 192, 226)
BRASS = (200, 176, 122)      # the theme's brass (ftb_quests_theme.txt)


def font(size_units):
    return ImageFont.truetype(FONT, max(8, int(round(size_units * PX * K))))


# ----------------------------------------------------------------------------------------------
# The project motifs: line drawings in a 100 x 100 box, centred on (50, 50). Each returns a list of
# ('line', [points]) / ('poly', [points]) / ('circle', (cx, cy, r)) / ('arc', (cx, cy, r, a0, a1)).
# ----------------------------------------------------------------------------------------------

def _gear(cx, cy, r_out, r_in, teeth, r_hole):
    pts = []
    for i in range(teeth):
        a = 2 * math.pi * i / teeth
        step = 2 * math.pi / teeth
        for da, r in ((-0.30, r_in), (-0.16, r_out), (0.16, r_out), (0.30, r_in)):
            pts.append((cx + r * math.cos(a + da * step * 1.6), cy + r * math.sin(a + da * step * 1.6)))
    return [('poly', pts), ('circle', (cx, cy, r_hole))]


def motif_spin():
    return _gear(50, 50, 40, 31, 10, 11) + [('circle', (50, 50, 4))]


def motif_gate():
    return [('line', [(18, 88), (18, 36), (26, 20), (50, 10), (74, 20), (82, 36), (82, 88)]),
            ('line', [(30, 88), (30, 40), (38, 29), (50, 24), (62, 29), (70, 40), (70, 88)]),
            ('poly', [(44, 18), (56, 18), (53, 30), (47, 30)]),
            ('line', [(10, 88), (90, 88)])]


def motif_brass():
    ingots = []
    for row, count in enumerate((3, 2, 1)):
        y = 78 - row * 24
        for i in range(count):
            x = 50 + (i - (count - 1) / 2) * 32
            ingots.append(('poly', [(x - 15, y), (x + 15, y), (x + 11, y - 16), (x - 11, y - 16)]))
    return ingots


def motif_current():
    return [('poly', [(58, 8), (30, 54), (46, 54), (38, 92), (72, 40), (54, 40)]),
            ('arc', (50, 50, 42, 200, 340)), ('arc', (50, 50, 42, 20, 160))]


def motif_crude():
    drop = [(50, 8), (66, 36), (72, 56), (66, 76), (50, 86), (34, 76), (28, 56), (34, 36)]
    return [('poly', drop), ('line', [(40, 62), (44, 72), (52, 76)]),
            ('line', [(8, 96), (92, 96)])]


def motif_wings():
    blades = []
    for k in range(3):
        a = 2 * math.pi * k / 3 - math.pi / 2
        tip = (50 + 42 * math.cos(a), 50 + 42 * math.sin(a))
        left = (50 + 14 * math.cos(a - 0.7), 50 + 14 * math.sin(a - 0.7))
        right = (50 + 16 * math.cos(a + 0.55), 50 + 16 * math.sin(a + 0.55))
        blades.append(('poly', [left, tip, right]))
    return blades + [('circle', (50, 50, 7)), ('circle', (50, 50, 44))]


def motif_breath():
    return [('line', [(36, 30), (36, 84), (40, 90), (60, 90), (64, 84), (64, 30)]),
            ('arc', (50, 30, 14, 180, 360)),
            ('line', [(46, 14), (46, 8), (54, 8), (54, 14)]),
            ('circle', (78, 30, 5)), ('circle', (86, 18, 3)), ('circle', (72, 14, 2.5))]


def motif_skyline():
    return [('arc', (50, 150, 100, 232, 308)),
            ('line', [(50, 52), (50, 6)]), ('line', [(44, 14), (50, 6), (56, 14)]),
            ('circle', (22, 26, 2)), ('circle', (80, 20, 2.5)), ('circle', (70, 40, 1.8)), ('circle', (30, 44, 1.8))]


def motif_moon():
    return [('arc', (50, 50, 40, 110, 430)), ('arc', (66, 44, 32, 100, 260)),
            ('circle', (34, 62, 5)), ('circle', (44, 38, 3.5)), ('circle', (30, 40, 2.5))]


# Pages that were pages before they were projects keep the icon tools/generate-quest-art.py draws for them.
OWN_ICON = {'first_days', 'the_moon'}

MOTIFS = {'spin': motif_spin, 'gate': motif_gate, 'brass': motif_brass, 'current': motif_current, 'crude': motif_crude,
          'wings': motif_wings, 'breath': motif_breath, 'skyline': motif_skyline, 'moon': motif_moon}


def draw_motif(draw, name, left, top, size, color, width):
    """The motif in a square of `size` pixels whose corner is (left, top)."""
    scale = size / 100.0

    def p(pt):
        return (left + pt[0] * scale, top + pt[1] * scale)

    for kind, data in MOTIFS[name]():
        if kind == 'line':
            draw.line([p(q) for q in data], fill=color, width=width, joint='curve')
        elif kind == 'poly':
            pts = [p(q) for q in data]
            draw.line(pts + [pts[0]], fill=color, width=width, joint='curve')
        elif kind == 'circle':
            cx, cy, r = data
            x, y = p((cx, cy))
            draw.ellipse((x - r * scale, y - r * scale, x + r * scale, y + r * scale), outline=color, width=width)
        elif kind == 'arc':
            cx, cy, r, a0, a1 = data
            x, y = p((cx, cy))
            draw.arc((x - r * scale, y - r * scale, x + r * scale, y + r * scale), a0, a1, fill=color, width=width)


# ----------------------------------------------------------------------------------------------
# A sheet
# ----------------------------------------------------------------------------------------------

def _text(draw, xy, text, size_units, fill, anchor='la'):
    draw.text(xy, text, font=font(size_units), fill=fill, anchor=anchor)


def _row_ys(chapter):
    ys = sorted({round(q.y, 1) for q in chapter.quests})
    # rows closer than a line gap are one step
    steps = []
    for y in ys:
        if not steps or y - steps[-1] > 1.0:
            steps.append(y)
    return steps


def _shape_mask(name, size):
    path = os.path.join(SHAPES, name, 'outline.png')
    image = Image.open(path).convert('RGBA').resize((size, size), Image.LANCZOS)
    return image.split()[3] if image.getextrema()[3][1] else image.convert('L')


def draw_sheet(chapter):
    bp = chapter.blueprint
    left, top, right, bottom = bp.box
    width_u, height_u = right - left, bottom - top
    w, h = int(round(width_u * PX)), int(round(height_u * PX))
    bw, bh = w * K, h * K

    def u(x, y):
        return ((x - left) * PX * K, (y - top) * PX * K)

    # Paper: a slow gradient and a vignette.
    ys = np.linspace(0.0, 1.0, bh)[:, None]
    xs = np.linspace(-1.0, 1.0, bw)[None, :]
    vig = 1.0 - 0.28 * np.clip((xs ** 2 + ((ys - 0.5) * 2) ** 2) / 2.0, 0.0, 1.0)
    paper = np.zeros((bh, bw, 3), dtype=np.float64)
    for i in range(3):
        paper[..., i] = (PAPER_TOP[i] + (PAPER_BOTTOM[i] - PAPER_TOP[i]) * ys) * vig
    base = Image.fromarray(np.clip(paper, 0, 255).astype(np.uint8), 'RGB').convert('RGBA')

    layer = Image.new('RGBA', (bw, bh), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)

    # Grid, in the quests' own coordinates so a quest sits on a line.
    def vline(x, a, wd):
        px = u(x, 0)[0]
        d.line((px, 0, px, bh), fill=GRID + (a,), width=wd)

    def hline(y, a, wd):
        py = u(0, y)[1]
        d.line((0, py, bw, py), fill=GRID + (a,), width=wd)

    x0, x1 = int(math.floor(left * 4)), int(math.ceil(right * 4))
    y0, y1 = int(math.floor(top * 4)), int(math.ceil(bottom * 4))
    for i in range(x0, x1 + 1):
        x = i / 4.0
        if i % 20 == 0:
            vline(x, 120, K * 2)
        elif i % 4 == 0:
            vline(x, 56, K)
        else:
            vline(x, 22, 1)
    for i in range(y0, y1 + 1):
        y = i / 4.0
        if i % 20 == 0:
            hline(y, 120, K * 2)
        elif i % 4 == 0:
            hline(y, 56, K)
        else:
            hline(y, 22, 1)

    # Step lines: one dashed rule across each row, with its number.
    steps = _row_ys(chapter)
    for n, y in enumerate(steps, start=1):
        py = u(0, y)[1]
        x = u(left + 1.5, 0)[0]
        end = u(right - 1.0, 0)[0]
        while x < end:
            d.line((x, py, min(x + 9 * K, end), py), fill=INK_DIM + (70,), width=K)
            x += 18 * K
        _text(d, u(left + 0.95, y), '%02d' % n, 0.34, INK_DIM + (210,), 'lm')

    # Frame: a heavy rule, a light one inside it, corner marks and ruler ticks.
    inset = 0.42
    fl, ft, fr, fb = left + inset, top + inset, right - inset, bottom - inset
    d.rectangle((*u(fl, ft), *u(fr, fb)), outline=INK + (210,), width=K * 3)
    d.rectangle((*u(fl + 0.16, ft + 0.16), *u(fr - 0.16, fb - 0.16)), outline=INK_DIM + (120,), width=K)
    for i in range(int(math.ceil(fl * 2)), int(math.floor(fr * 2)) + 1):
        x = i / 2.0
        major = i % 10 == 0
        tick = 0.22 if major else 0.1
        d.line((*u(x, ft), *u(x, ft + tick)), fill=INK + (190 if major else 110,), width=K)
        d.line((*u(x, fb - tick), *u(x, fb)), fill=INK + (190 if major else 110,), width=K)
        if major and abs(x - round(x)) < 1e-6:
            _text(d, u(x, ft + 0.3), '%d' % round(x), 0.24, INK_DIM + (190,), 'ma')
    for i in range(int(math.ceil(ft * 2)), int(math.floor(fb * 2)) + 1):
        y = i / 2.0
        major = i % 10 == 0
        tick = 0.22 if major else 0.1
        d.line((*u(fl, y), *u(fl + tick, y)), fill=INK + (190 if major else 110,), width=K)
        d.line((*u(fr - tick, y), *u(fr, y)), fill=INK + (190 if major else 110,), width=K)

    # Heading.
    _text(d, u(left + 1.05, top + 0.95), 'PROJECT %02d' % bp.number, 0.36, INK_DIM + (255,), 'ls')
    _text(d, u(left + 1.05, top + 1.95), chapter.title.upper(), 0.98, INK + (255,), 'ls')
    d.line((*u(left + 1.05, top + 2.15), *u(left + 1.05 + 5.2, top + 2.15)), fill=BRASS + (230,), width=K * 2)

    # The motif, faint, in the corner of the heading.
    size = 2.9 * PX * K
    mx, my = u(right - 1.3 - 2.9, top + 0.78)
    draw_motif(d, bp.motif, mx, my, size, INK_DIM + (150,), K * 3)

    # Title block: bottom right.
    bx1, by1 = right - 0.95, bottom - 0.95
    bx0, by0 = bx1 - 9.6, by1 - 2.4
    d.rectangle((*u(bx0, by0), *u(bx1, by1)), outline=INK + (230,), width=K * 2)
    rows = [('PROJECT', '%02d of %02d' % (bp.number, bp.of)),
            ('SHEET', chapter.title.upper()),
            ('FINALE', next(q.title for q in chapter.quests if q.slug == bp.finale)),
            ('PAYS', ('%d skill point%s to the whole team' % (bp.points, '' if bp.points == 1 else 's')) if bp.points else 'nothing: it only opens the way')]
    row_h = (by1 - by0) / len(rows)
    for i, (label, value) in enumerate(rows):
        y = by0 + i * row_h
        if i:
            d.line((*u(bx0, y), *u(bx1, y)), fill=INK_DIM + (150,), width=K)
        _text(d, u(bx0 + 0.18, y + row_h / 2), label, 0.2, INK_DIM + (230,), 'lm')
        _text(d, u(bx0 + 1.95, y + row_h / 2), value, 0.28, (BRASS if label == 'PAYS' and bp.points else INK) + (255,), 'lm')
    d.line((*u(bx0 + 1.8, by0), *u(bx0 + 1.8, by1)), fill=INK_DIM + (150,), width=K)

    # Legend: the three quest shapes, bottom left.
    lx, ly = left + 1.05, bottom - 1.95
    d.rectangle((*u(lx - 0.15, ly - 0.15), *u(lx + 5.7, ly + 0.95)), outline=INK_DIM + (150,), width=K)
    legend = (('ascension_star', 'LEARN'), ('ascension_nova', 'MAKE'), ('ascension_eye', 'PLACE'))
    for i, (shape, label) in enumerate(legend):
        sx = lx + 0.1 + i * 1.85
        size_px = int(0.72 * PX * K)
        mask = _shape_mask(shape, size_px)
        tint = Image.new('RGBA', (size_px, size_px), INK + (255,))
        tint.putalpha(mask)
        layer.alpha_composite(tint, (int(u(sx, ly + 0.08)[0]), int(u(sx, ly + 0.08)[1])))
        _text(d, u(sx + 0.8, ly + 0.44), label, 0.24, INK + (235,), 'lm')

    sheet = Image.alpha_composite(base, layer).convert('RGB')
    return sheet.resize((w, h), Image.LANCZOS)


# ----------------------------------------------------------------------------------------------
# The round icon
# ----------------------------------------------------------------------------------------------

def draw_icon(motif):
    size = 64
    k = 8
    big = size * k
    image = Image.new('RGBA', (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    d.ellipse((2 * k, 2 * k, big - 2 * k, big - 2 * k), fill=(12, 36, 70, 255))
    d.ellipse((2 * k, 2 * k, big - 2 * k, big - 2 * k), outline=BRASS + (255,), width=3 * k)
    inner = 38 * k
    draw_motif(d, motif, (big - inner) / 2, (big - inner) / 2, inner, INK + (255,), int(2.2 * k))
    return image.resize((size, size), Image.LANCZOS)


# ----------------------------------------------------------------------------------------------

def main():
    from book import BOOK  # noqa: E402
    done = []
    for chapter in BOOK.chapters:
        if not chapter.blueprint:
            continue
        sheet = draw_sheet(chapter)
        path = os.path.join(OUT, 'blueprint', chapter.slug + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        sheet.save(path, optimize=True)
        if chapter.slug not in OWN_ICON:
            icon_path = os.path.join(OUT, 'icons', chapter.slug + '.png')
            draw_icon(chapter.blueprint.motif).save(icon_path)
            with open(icon_path + '.mcmeta', 'w', encoding='utf-8', newline='\n') as handle:
                handle.write('{\n  "texture": {\n    "blur": true\n  }\n}\n')
        done.append('%s %dx%d (%d KB)' % (chapter.slug, sheet.size[0], sheet.size[1], os.path.getsize(path) // 1024))
    print('blueprint sheets: ' + ', '.join(done) if done else 'no blueprint pages in the book')


if __name__ == '__main__':
    main()
