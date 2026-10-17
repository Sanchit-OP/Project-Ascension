#!/usr/bin/env python3
"""The rune screen's art, and its mockups (docs/technical/rune-system.md section 10).

Owns the Rune Chalk item's texture (assets/ascension_combat/textures/item/rune_chalk.png) and the
mockups under design/runes/: the rune screen as an orrery, drawn at one texel per GUI pixel and
shown at GUI scale 3; a hover card; every path as primary and a new player's locked page; how the
panel fits at GUI scales 2 to 4; and every icon in its socket. It also writes the screen's own
textures, as GUI sprites under assets/ascension_combat/textures/gui/sprites/ (section "The screen's
textures" below), and a contact sheet of them, design/runes/kit.png.

The layout's history: League's two columns first, which Sanchit turned down on 2026-10-16 ("its
kinda exact copy of the layout for the runes"); then four others (a rune pouch, a loadout round
the player, an orrery, a sky map of constellations), in commit 3773f06. He chose the orrery on
2026-10-17: the primary path a brass dial, keystones round its hub and minors on its outer ring,
one row to each half; the secondary path a smaller dial of minors only.

Run:  python tools/generate-rune-art.py
Everything is seeded; re-running reproduces every PNG byte for byte.

Text in the mockups is drawn in a stand-in pixel font with Minecraft's metrics (5 px wide, 7 px
caps, one descender row, 9 px lines). In game the screen uses Minecraft's own font.

Art rules (docs/technical/art-guide.md): original art only, light from the top-left, no teal and
no diamond eye (the gateway network's), no accidental crosses, letters or faces.
"""

import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
DESIGN = os.path.join(ROOT, 'design', 'runes')


# ---------------------------------------------------------------------------------------------
# Colour
# ---------------------------------------------------------------------------------------------

def hexc(h, a=255):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), a)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (a[3] if len(a) > 3 else 255,)


def with_alpha(c, a):
    return c[:3] + (int(max(0, min(255, a))),)


def grey(c):
    v = int(0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2])
    return (v, v, v, c[3])


# A path's ramp: outline, dark, mid, light, highlight. Hues chosen to stay off teal (the network's)
# and apart from each other in value as well as hue.
PATHS = {
    'vanguard': {
        'name': 'Vanguard', 'promise': 'Keep hitting.',
        'ramp': [hexc('#3a2208'), hexc('#7c4e14'), hexc('#bb8423'), hexc('#ebb848'), hexc('#fff0b4')],
    },
    'reaver': {
        'name': 'Reaver', 'promise': 'Strike first, finish fast.',
        'ramp': [hexc('#2c080e'), hexc('#6c1422'), hexc('#a82636'), hexc('#e2505a'), hexc('#ffc4c0')],
    },
    'aetherwright': {
        'name': 'Aetherwright', 'promise': 'Make the magic do more.',
        'ramp': [hexc('#1c0c34'), hexc('#482280'), hexc('#7846c6'), hexc('#a87cf0'), hexc('#ead8ff')],
    },
    'bulwark': {
        'name': 'Bulwark', 'promise': 'Stand in front.',
        'ramp': [hexc('#0c182c'), hexc('#24406c'), hexc('#3c6ca8'), hexc('#6ea0dc'), hexc('#d4e8ff')],
    },
}
PATH_ORDER = ['vanguard', 'reaver', 'aetherwright', 'bulwark']

# Stat colours (League's habit: attack damage orange, armour yellow, magic resist blue, HP green).
STAT = {
    'ad': hexc('#f0903c'), 'as': hexc('#f0d850'), 'ms': hexc('#dce4ec'), 'armor': hexc('#d8b070'),
    'mr': hexc('#9c90f8'), 'hp': hexc('#62c86a'), 'omni': hexc('#e05858'), 'magic': hexc('#c08cff'),
    'xp': hexc('#9cf04c'),
}
NEUTRAL = [hexc('#2a2a2e'), hexc('#4a4a50'), hexc('#76767e'), hexc('#a4a4ac'), hexc('#dcdce2')]

TEXT = hexc('#ffffff')
TEXT_BODY = hexc('#d4d8de')
TEXT_DIM = hexc('#8c929c')
TEXT_FAINT = hexc('#5c616a')


# ---------------------------------------------------------------------------------------------
# Pixels
# ---------------------------------------------------------------------------------------------

def new(w, h, c=(0, 0, 0, 0)):
    return Image.new('RGBA', (w, h), c)


def put(img, x, y, c):
    """Composite one pixel over what is there (alpha-aware)."""
    if not (0 <= x < img.width and 0 <= y < img.height):
        return
    if c[3] >= 255:
        img.putpixel((x, y), c)
        return
    if c[3] <= 0:
        return
    b = img.getpixel((x, y))
    a = c[3] / 255.0
    out_a = a + b[3] / 255.0 * (1 - a)
    if out_a <= 0:
        return
    rgb = tuple(int(round((c[i] * a + b[i] * (b[3] / 255.0) * (1 - a)) / out_a)) for i in range(3))
    img.putpixel((x, y), rgb + (int(round(out_a * 255)),))


def rect(img, x0, y0, x1, y1, c):
    """Fill [x0, x1) x [y0, y1)."""
    for y in range(y0, y1):
        for x in range(x0, x1):
            put(img, x, y, c)


def hline(img, x0, x1, y, c):
    for x in range(x0, x1):
        put(img, x, y, c)


def vline(img, x, y0, y1, c):
    for y in range(y0, y1):
        put(img, x, y, c)


def line(img, x0, y0, x1, y1, c, dash=0):
    """A one-pixel Bresenham line, both ends included; `dash` > 0 draws every other run."""
    x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    i = 0
    while True:
        if not dash or (i // dash) % 2 == 0:
            put(img, x0, y0, c)
        i += 1
        if x0 == x1 and y0 == y1:
            return
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def blit(dst, src, x, y):
    for sy in range(src.height):
        for sx in range(src.width):
            c = src.getpixel((sx, sy))
            if c[3]:
                put(dst, x + sx, y + sy, c)


def disc(cx, cy, r):
    """Pixel centres inside a circle centred on (cx, cy), which may sit on a half pixel."""
    out = set()
    for y in range(int(cy - r) - 1, int(cy + r) + 2):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                out.add((x, y))
    return out


def circle(img, cx, cy, r, c, width=1.0, arc=None, dash=0):
    """A ring of the given width; `arc` = (start, end) in degrees, 0 = right, clockwise."""
    for (x, y) in disc(cx, cy, r) - disc(cx, cy, r - width):
        a = (math.degrees(math.atan2(y + 0.5 - cy, x + 0.5 - cx)) + 360) % 360
        if arc:
            a0, a1 = arc[0] % 360, arc[1] % 360
            inside = a0 <= a <= a1 if a0 <= a1 else (a >= a0 or a <= a1)
            if not inside:
                continue
        if dash and int(a / dash) % 2:
            continue
        put(img, x, y, c)


def glow(img, cx, cy, r, colour, a):
    for (x, y) in disc(cx, cy, r):
        d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / r
        put(img, x, y, with_alpha(colour, a * (1 - d) ** 2))


def scaled(img, k):
    return img.resize((img.width * k, img.height * k), Image.NEAREST)


def polar(cx, cy, r, deg):
    a = math.radians(deg)
    return cx + r * math.cos(a), cy + r * math.sin(a)


# ---------------------------------------------------------------------------------------------
# Shapes: geometry drawn at 8x and thresholded into a pixel mask, then shaded in pixel-art style
# ---------------------------------------------------------------------------------------------

SS = 8


def arc_pts(cx, cy, rx, ry, a0, a1, step=8):
    n = max(2, int(abs(a1 - a0) / step) + 1)
    return [(cx + rx * math.cos(math.radians(a0 + (a1 - a0) * i / (n - 1))),
             cy + ry * math.sin(math.radians(a0 + (a1 - a0) * i / (n - 1)))) for i in range(n)]


def taper(pts, widths):
    """A polygon along a centre line, `widths` its half-width at each point: a stroke that swells
    and thins, for claws, horns and tails."""
    left, right = [], []
    for i, (x, y) in enumerate(pts):
        ax, ay = pts[max(0, i - 1)]
        bx, by = pts[min(len(pts) - 1, i + 1)]
        dx, dy = bx - ax, by - ay
        n = math.hypot(dx, dy) or 1.0
        nx, ny = -dy / n, dx / n
        left.append((x + nx * widths[i], y + ny * widths[i]))
        right.append((x - nx * widths[i], y - ny * widths[i]))
    return left + right[::-1]


def star_pts(cx, cy, big, small, n=5, rot=-90):
    pts = []
    for i in range(n * 2):
        r = big if i % 2 == 0 else small
        pts.append(polar(cx, cy, r, rot + i * 180 / n))
    return pts


class Shape:
    """A glyph built from polygons, ellipses and strokes, in glyph pixel coordinates.

    `fill` adds to the silhouette and `cut` removes from it; `groove` and `shine` mark inner
    detail (a dark cut, a lit edge) inside the silhouette.
    """

    def __init__(self, size):
        self.size = size
        self.layers = {k: Image.new('L', (size * SS, size * SS), 0) for k in ('fill', 'groove', 'shine')}

    def _draw(self, layer, kind, value, args, width=1.0):
        d = ImageDraw.Draw(self.layers[layer])
        if kind == 'poly':
            d.polygon([(x * SS, y * SS) for x, y in args], fill=value)
        elif kind == 'ellipse':
            cx, cy, rx, ry = args
            d.ellipse([(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS], fill=value)
        elif kind == 'stroke':
            pts = [(x * SS, y * SS) for x, y in args]
            d.line(pts, fill=value, width=max(1, int(width * SS)), joint='curve')
            r = width * SS / 2
            for (x, y) in (pts[0], pts[-1]):
                d.ellipse([x - r, y - r, x + r, y + r], fill=value)
        return self

    def poly(self, pts):
        return self._draw('fill', 'poly', 255, pts)

    def cut_poly(self, pts):
        return self._draw('fill', 'poly', 0, pts)

    def ellipse(self, cx, cy, rx, ry=None):
        return self._draw('fill', 'ellipse', 255, (cx, cy, rx, rx if ry is None else ry))

    def cut_ellipse(self, cx, cy, rx, ry=None):
        return self._draw('fill', 'ellipse', 0, (cx, cy, rx, rx if ry is None else ry))

    def stroke(self, pts, width):
        return self._draw('fill', 'stroke', 255, pts, width)

    def cut_stroke(self, pts, width):
        return self._draw('fill', 'stroke', 0, pts, width)

    def groove(self, pts, width=1.0):
        return self._draw('groove', 'stroke', 255, pts, width)

    def groove_ring(self, cx, cy, r_out, r_in):
        self._draw('groove', 'ellipse', 255, (cx, cy, r_out, r_out))
        return self._draw('groove', 'ellipse', 0, (cx, cy, r_in, r_in))

    def shine(self, pts, width=1.0):
        return self._draw('shine', 'stroke', 255, pts, width)

    def masks(self):
        out = {}
        for k, layer in self.layers.items():
            small = layer.resize((self.size, self.size), Image.BOX)
            out[k] = {(x, y) for y in range(self.size) for x in range(self.size)
                      if small.getpixel((x, y)) >= 128}
        out['groove'] &= out['fill']
        out['shine'] &= out['fill']
        return out


class MaskShape:
    """A glyph hand-placed as a pixel map: '#' fill, 'd' groove, 'h' shine. For the smallest
    icons, where geometry thresholded at 8 px loses the shape."""

    def __init__(self, rows):
        self.size = len(rows)
        self.rows = rows

    def masks(self):
        m = {'fill': set(), 'groove': set(), 'shine': set()}
        for y, row in enumerate(self.rows):
            for x, ch in enumerate(row):
                if ch in '#dh':
                    m['fill'].add((x, y))
                if ch == 'd':
                    m['groove'].add((x, y))
                if ch == 'h':
                    m['shine'].add((x, y))
        return m


def shade(shape, ramp, outline=True):
    """Pixel-art finish: lit along up-left edges, shadowed along down-right ones, a one-pixel
    outline outside, grooves dark and shine lit. `ramp` is outline, dark, mid, light, highlight."""
    m = shape.masks()
    fill, groove, shine = m['fill'], m['groove'], m['shine']
    n = shape.size
    img = new(n, n)
    for (x, y) in fill:
        ul = (x - 1, y) not in fill or (x, y - 1) not in fill
        dr = (x + 1, y) not in fill or (x, y + 1) not in fill
        c = ramp[3] if ul and not dr else ramp[1] if dr and not ul else ramp[2]
        if not ul and not dr:
            c = mix(ramp[2], ramp[3], 0.45 - 0.9 * (y / n)) if y < n / 2 else mix(ramp[2], ramp[1], (y / n - 0.5) * 0.9)
        if (x, y) in shine:
            c = ramp[4]
        if (x, y) in groove:
            c = ramp[1] if ((x - 1, y) in groove or (x, y - 1) in groove) else ramp[0]
        img.putpixel((x, y), c)
    if outline:
        for y in range(n):
            for x in range(n):
                if (x, y) in fill:
                    continue
                if any((x + dx, y + dy) in fill for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    img.putpixel((x, y), ramp[0])
    return img


# ---------------------------------------------------------------------------------------------
# Emblems (20 x 20)
# ---------------------------------------------------------------------------------------------

def emblem_vanguard():
    """A pointed banner: a spear's tip over a swallowtail. Heavy and upright."""
    s = Shape(20)
    s.poly([(10, 0.6), (15.4, 6.5), (15.4, 19.2), (10, 14.8), (4.6, 19.2), (4.6, 6.5)])
    s.groove([(10, 3.2), (10, 13.2)], 1.0)
    s.shine([(6.8, 7), (6.8, 15.6)], 0.9)
    s.groove([(5.2, 8.6), (10, 4.4), (14.8, 8.6)], 0.9)
    return s


def emblem_reaver():
    """Three claw marks, curved and tapered to points. Sharp and leaning."""
    s = Shape(20)
    for pts in (((11.4, 1.0), (9.6, 5.6), (6.8, 10.2), (2.0, 15.8)),
                ((15.8, 2.4), (14.0, 7.4), (10.6, 12.8), (5.4, 18.6)),
                ((19.0, 6.2), (17.6, 10.4), (14.8, 15.0), (10.6, 19.2))):
        s.poly(taper(list(pts), [0.2, 1.4, 1.5, 0.2]))
    return s


def emblem_aetherwright():
    """A comet: a bright round head, its tail one tapering sweep with streaks in it."""
    s = Shape(20)
    s.ellipse(14.0, 6.0, 4.4)
    for pts, w in ((((11.4, 8.6), (6.6, 13.4), (1.2, 18.8)), 2.2),
                   (((10.2, 5.4), (6.6, 9.0), (2.8, 12.8)), 1.3),
                   (((14.6, 9.8), (11.0, 13.4), (7.2, 17.2)), 1.3)):
        s.poly(taper(list(pts), [w, w * 0.6, 0.2]))
    s.groove_ring(14.0, 6.0, 4.4, 3.6)
    s.shine([(12.6, 4.6), (13.4, 4.0)], 1.8)
    return s


def emblem_bulwark():
    """A round shield with a rim and a boss. Broad and closed."""
    s = Shape(20)
    s.ellipse(10, 10, 8.9)
    s.groove_ring(10, 10, 6.9, 5.9)
    s.groove_ring(10, 10, 2.9, 2.0)
    s.shine([(10, 9), (10, 9)], 1.6)
    s.shine([(4.4, 6.0), (6.2, 3.8)], 0.9)
    return s


EMBLEMS = {
    'vanguard': emblem_vanguard,
    'reaver': emblem_reaver,
    'aetherwright': emblem_aetherwright,
    'bulwark': emblem_bulwark,
}


# ---------------------------------------------------------------------------------------------
# Keystones (20 x 20)
# ---------------------------------------------------------------------------------------------

def k_conquerors_resolve():
    """A crown of three points over a band: the fight's ramp, worn."""
    s = Shape(20)
    s.poly([(2.6, 16.6), (2.6, 6.4), (6.4, 10.6), (10, 3.0), (13.6, 10.6), (17.4, 6.4), (17.4, 16.6)])
    s.groove([(3.2, 13.4), (16.8, 13.4)], 1.0)
    s.ellipse(10, 2.8, 1.5)
    s.ellipse(2.6, 5.8, 1.3)
    s.ellipse(17.4, 5.8, 1.3)
    s.shine([(4.0, 9.4), (4.0, 12.4)], 0.9)
    s.shine([(9.0, 6.2), (7.6, 9.6)], 0.9)
    return s


def k_cadence():
    """A metronome, its arm mid-swing: the rhythm of charged hits."""
    s = Shape(20)
    s.poly([(4.4, 18.6), (15.6, 18.6), (12.4, 3.0), (7.6, 3.0)])
    s.cut_poly([(8.2, 6.0), (11.8, 6.0), (12.8, 12.6), (7.2, 12.6)])
    s.stroke([(10.0, 15.6), (16.4, 2.0)], 1.2)
    s.poly([(13.0, 6.6), (15.6, 7.8), (14.6, 10.2), (12.0, 9.0)])
    s.groove([(5.4, 15.8), (14.6, 15.8)], 0.8)
    s.shine([(6.0, 17.0), (8.4, 4.4)], 0.8)
    return s


def k_vein_breaker():
    """A pick striking a block of ore, the block splitting: the vein comes with it."""
    s = Shape(20)
    s.stroke([(1.8, 18.2), (8.6, 11.4)], 2.0)
    s.poly(taper([(3.8, 6.0), (7.6, 7.6), (10.4, 10.4), (12.0, 14.2)], [0.3, 1.8, 1.8, 0.3]))
    s.poly([(12.0, 4.0), (19.2, 4.0), (19.2, 11.2), (12.0, 11.2)])
    s.cut_stroke([(15.6, 3.4), (14.6, 7.6), (16.2, 11.8)], 0.9)
    s.shine([(13.4, 5.6), (13.8, 5.6)], 1.1)
    s.shine([(17.6, 8.6), (18.0, 8.6)], 1.1)
    s.shine([(5.0, 6.2), (7.6, 7.4)], 0.8)
    return s


def k_ambush():
    """A dagger driven down from the upper left, two streaks of its strike behind it."""
    s = Shape(20)
    s.poly([(4.2, 8.2), (8.2, 4.2), (18.8, 18.8)])
    s.stroke([(2.0, 2.0), (5.6, 5.6)], 2.6)
    s.ellipse(1.8, 1.8, 1.6)
    s.stroke([(1.6, 10.4), (6.6, 15.4)], 1.2)
    s.stroke([(1.2, 15.0), (4.0, 17.8)], 1.1)
    s.groove([(6.4, 6.4), (14.6, 14.6)], 0.8)
    s.shine([(5.6, 7.2), (11.4, 13.0)], 0.8)
    return s


def k_deadeye():
    """An arrow in the bullseye of a target, its fletching behind."""
    s = Shape(20)
    s.ellipse(12.2, 7.8, 7.4)
    s.cut_ellipse(12.2, 7.8, 5.6)
    s.ellipse(12.2, 7.8, 3.6)
    s.cut_ellipse(12.2, 7.8, 2.2)
    s.ellipse(12.2, 7.8, 1.2)
    s.stroke([(2.4, 17.6), (11.4, 8.6)], 1.3)
    s.stroke([(2.4, 17.6), (1.0, 14.4)], 1.1)
    s.stroke([(2.4, 17.6), (5.6, 19.0)], 1.1)
    return s


def k_silent_step():
    """A feather: light enough that nothing hears it land."""
    s = Shape(20)
    spine = [(3.0, 17.6), (7.0, 12.0), (11.0, 7.0), (16.6, 2.0)]
    vane = [(2.8, 15.0), (5.0, 10.0), (9.0, 5.4), (14.0, 2.2), (17.6, 1.4), (17.0, 5.0), (14.0, 9.6),
            (10.0, 13.4), (5.6, 16.4)]
    s.poly(vane)
    s.stroke([(1.6, 19.0), (4.0, 16.2)], 1.2)
    s.cut_stroke([(14.8, 8.4), (16.8, 7.4)], 0.8)
    s.cut_stroke([(5.4, 9.4), (4.4, 8.6)], 0.8)
    s.groove(spine, 0.9)
    for (x, y) in ((7.0, 12.0), (9.4, 9.0), (11.8, 6.4), (14.2, 4.0)):
        s.groove([(x, y), (x + 2.4, y + 0.6)], 0.6)
    s.shine([(4.6, 11.2), (8.6, 6.4)], 0.8)
    return s


def k_starfall():
    """A five-pointed star falling, its streak behind it."""
    s = Shape(20)
    s.poly(star_pts(13.2, 6.8, 6.2, 2.7, 5, -90))
    s.stroke([(9.0, 10.6), (2.0, 17.8)], 1.8)
    s.stroke([(12.4, 12.8), (8.0, 18.4)], 1.1)
    s.shine([(11.6, 5.4), (12.6, 3.6)], 1.0)
    return s


def k_arc():
    """A lightning bolt that forks."""
    s = Shape(20)
    s.poly([(11.4, 0.8), (4.0, 10.6), (9.0, 10.6), (5.8, 19.2), (16.4, 7.0), (11.2, 7.0), (14.6, 0.8)])
    s.stroke([(7.4, 13.2), (3.0, 15.4), (1.6, 18.0)], 1.0)
    s.shine([(10.8, 2.4), (6.4, 9.2)], 0.8)
    return s


def k_scholar():
    """An open book, its pages lined, a bright orb of experience above it."""
    s = Shape(20)
    s.poly([(0.6, 7.8), (5.2, 7.4), (9.4, 9.6), (9.4, 18.8), (5.2, 16.6), (0.6, 17.0)])
    s.poly([(10.6, 9.6), (14.8, 7.4), (19.4, 7.8), (19.4, 17.0), (14.8, 16.6), (10.6, 18.8)])
    s.shine([(2.6, 12.4), (7.6, 13.6)], 4.4)
    s.shine([(12.4, 13.6), (17.4, 12.4)], 4.4)
    for dy in (0.0, 2.6, 5.2):
        s.groove([(2.2, 10.4 + dy), (8.0, 12.0 + dy)], 0.6)
        s.groove([(12.0, 12.0 + dy), (17.8, 10.4 + dy)], 0.6)
    s.ellipse(10.0, 3.4, 2.9)
    s.shine([(9.0, 2.6), (9.4, 2.2)], 1.1)
    return s


def k_brace():
    """A shield, braced, the shockwave breaking either side of it."""
    s = Shape(20)
    s.poly([(5.2, 2.4), (14.8, 2.4), (14.8, 9.4), (10.0, 17.6), (5.2, 9.4)])
    s.groove([(10.0, 4.4), (10.0, 14.0)], 0.9)
    s.stroke(arc_pts(10.0, 9.6, 8.4, 8.4, 130, 230, 10), 1.1)
    s.stroke(arc_pts(10.0, 9.6, 8.4, 8.4, -50, 50, 10), 1.1)
    s.shine([(6.4, 3.6), (6.4, 9.0)], 0.8)
    return s


def k_pack_leader():
    """A paw print: your wolves at your side."""
    s = Shape(20)
    s.ellipse(10.0, 13.6, 4.8, 4.0)
    s.ellipse(3.8, 8.4, 2.0, 2.4)
    s.ellipse(7.6, 4.4, 2.0, 2.5)
    s.ellipse(12.4, 4.4, 2.0, 2.5)
    s.ellipse(16.2, 8.4, 2.0, 2.4)
    s.shine([(7.4, 12.0), (8.6, 11.0)], 1.0)
    return s


def k_cats_eye():
    """A cat, sitting, seen from the side: the creatures that see what you can't."""
    s = Shape(20)
    s.ellipse(11.4, 13.2, 5.6, 5.8)
    s.ellipse(6.2, 6.6, 3.6, 3.4)
    s.poly([(3.0, 5.0), (3.2, 1.0), (5.8, 3.6)])
    s.poly([(6.8, 3.4), (8.8, 0.8), (9.4, 4.6)])
    s.poly([(4.6, 8.2), (9.4, 7.6), (8.4, 15.0), (6.6, 18.8), (5.4, 18.8)])
    s.poly([(5.4, 17.6), (16.6, 17.6), (16.6, 19.2), (5.4, 19.2)])
    s.stroke([(16.2, 17.6), (18.6, 13.2), (18.6, 9.0), (17.0, 6.6)], 1.4)
    s.shine([(8.6, 9.6), (12.0, 8.0)], 0.9)
    return s


# ---------------------------------------------------------------------------------------------
# Minors (14 x 14)
# ---------------------------------------------------------------------------------------------

def m_second_wind():
    """A heart, a curl of wind across it. Hand-placed: at 14 px geometry loses the lobes."""
    return MaskShape([
        "..............",
        "..............",
        "..###....###..",
        ".#hh##..#####.",
        ".#h##########.",
        ".########dd##.",
        ".#####ddd####.",
        "..##dd######..",
        "...########...",
        "....######....",
        ".....####.....",
        "......##......",
        "..............",
        "..............",
    ])


def m_momentum():
    """A ball, rolling, speed lines behind it."""
    s = Shape(14)
    s.ellipse(9.0, 7.4, 3.9)
    s.stroke([(1.0, 4.6), (4.0, 4.6)], 1.0)
    s.stroke([(0.6, 7.6), (3.8, 7.6)], 1.0)
    s.stroke([(1.6, 10.6), (4.4, 10.6)], 1.0)
    s.shine([(7.6, 5.4), (8.6, 4.8)], 1.0)
    return s


def m_blooded():
    """A drop of blood, its highlight on the upper left."""
    s = Shape(14)
    s.ellipse(7.0, 8.8, 4.0)
    s.poly([(3.4, 7.6), (7.0, 0.8), (10.6, 7.6)])
    s.shine([(5.0, 7.6), (5.4, 9.8)], 0.9)
    return s


def m_relentless_strikes():
    """A broad blade mid-swing, the arc of its last swing behind it."""
    s = Shape(14)
    s.poly([(0.4, 10.0), (4.0, 13.6), (12.0, 5.6), (13.6, 0.4), (8.4, 2.0)])
    s.stroke(arc_pts(1.0, 1.0, 7.4, 7.4, 8, 82, 8), 1.6)
    s.groove([(2.6, 11.4), (10.8, 3.2)], 0.7)
    s.shine([(1.4, 9.8), (9.0, 2.2)], 0.7)
    return s


def m_killing_surge():
    """A flame rising: the surge after a kill."""
    s = Shape(14)
    s.ellipse(7.0, 9.0, 4.0)
    s.poly([(3.2, 8.6), (5.0, 1.6), (7.4, 5.6), (9.0, 2.6), (10.8, 8.6)])
    s.groove([(7.0, 7.6), (7.0, 11.4)], 1.0)
    s.shine([(4.6, 7.0), (4.6, 9.6)], 0.8)
    return s


def m_giant_slayer():
    """A great mass split by a crack: the big thing, felled."""
    s = Shape(14)
    s.poly([(1.4, 12.6), (3.6, 4.0), (7.6, 1.2), (11.2, 3.6), (12.6, 12.6)])
    s.groove([(8.4, 1.8), (6.6, 5.4), (8.2, 7.6), (6.0, 12.4)], 0.9)
    s.shine([(3.2, 8.0), (4.4, 4.4)], 0.8)
    return s


def m_pounce():
    """A leap: an arc rising and striking down."""
    s = Shape(14)
    s.stroke([(1.4, 12.0), (2.4, 6.0), (5.6, 2.4), (9.0, 3.0), (11.0, 6.4)], 1.4)
    s.poly([(8.4, 6.4), (13.6, 6.0), (11.8, 12.4)])
    return s


def m_drop_in():
    """An armour plate with a hole punched clean through it, cracks running out."""
    s = Shape(14)
    s.poly([(1.0, 2.4), (13.0, 2.4), (13.0, 9.0), (7.0, 13.2), (1.0, 9.0)])
    s.cut_ellipse(7.0, 6.6, 2.2)
    s.groove([(4.8, 5.0), (2.8, 3.4)], 0.7)
    s.groove([(9.2, 8.4), (11.4, 9.0)], 0.7)
    s.groove([(7.4, 8.8), (7.0, 11.6)], 0.7)
    s.shine([(2.2, 3.6), (2.2, 8.0)], 0.7)
    return s


def m_vanish():
    """A cloud of smoke, flat beneath, its last wisps drifting off."""
    s = Shape(14)
    s.poly([(1.2, 9.0), (12.4, 9.0), (12.4, 11.8), (1.2, 11.8)])
    s.ellipse(3.6, 9.4, 2.4)
    s.ellipse(6.6, 7.6, 3.2)
    s.ellipse(9.8, 8.8, 2.6)
    s.ellipse(11.2, 3.8, 1.1)
    s.ellipse(12.8, 1.4, 0.7)
    s.shine([(4.6, 6.6), (6.0, 5.4)], 0.9)
    return s


def m_finishing_blow():
    """A round thing cut clean in two."""
    s = Shape(14)
    s.ellipse(7.0, 7.2, 5.6)
    s.cut_stroke([(1.0, 13.0), (13.0, 1.4)], 1.5)
    s.shine([(3.2, 5.4), (4.6, 3.4)], 0.9)
    return s


def m_overkill():
    """An impact bursting, a drop flung clear of it."""
    s = Shape(14)
    s.poly(star_pts(6.0, 8.0, 5.4, 2.6, 5, -72))
    s.ellipse(11.8, 3.2, 1.6)
    s.stroke([(9.6, 5.6), (11.0, 4.2)], 0.9)
    return s


def m_trophy_hunter():
    """Tally marks: four and a stroke across. Every new kind of enemy counted once."""
    s = Shape(14)
    for x in (2.6, 5.4, 8.2, 11.0):
        s.stroke([(x, 1.8), (x + 0.4, 12.2)], 1.3)
    s.stroke([(0.8, 10.4), (13.2, 3.4)], 1.3)
    return s


def m_attunement():
    """A crystal, faceted and lit from within."""
    s = Shape(14)
    s.poly([(7.0, 0.6), (10.8, 4.0), (10.8, 9.8), (7.0, 13.4), (3.2, 9.8), (3.2, 4.0)])
    s.groove([(7.0, 2.4), (7.0, 11.8)], 0.8)
    s.shine([(4.4, 4.6), (4.4, 9.2)], 0.9)
    return s


def m_unweave():
    """A ward ring, broken open, its pieces flying out: resistance cut through."""
    s = Shape(14)
    s.ellipse(6.4, 7.6, 5.4)
    s.cut_ellipse(6.4, 7.6, 3.4)
    s.cut_poly([(6.4, 7.6), (14.0, 0.0), (14.0, 6.0)])
    s.poly([(10.8, 2.0), (12.6, 1.0), (12.4, 3.2)])
    s.poly([(12.0, 5.0), (13.6, 5.4), (12.4, 6.8)])
    return s


def m_clarity():
    """A candle, its flame standing straight: calm while you're whole."""
    s = Shape(14)
    s.poly([(4.6, 6.6), (9.4, 6.6), (9.4, 12.2), (4.6, 12.2)])
    s.poly([(2.6, 12.0), (11.4, 12.0), (11.4, 13.4), (2.6, 13.4)])
    s.ellipse(7.0, 3.8, 1.7, 2.0)
    s.poly([(5.6, 3.4), (7.0, 0.4), (8.4, 3.4)])
    s.groove([(7.0, 5.0), (7.0, 6.6)], 0.6)
    s.shine([(6.6, 3.0), (6.6, 4.4)], 1.0)
    s.shine([(5.6, 7.6), (5.6, 11.4)], 0.8)
    return s


def m_siphon():
    """A goblet, drinking the magic back."""
    s = Shape(14)
    s.poly([(2.0, 1.8), (12.0, 1.8), (10.6, 6.4), (7.0, 8.0), (3.4, 6.4)])
    s.stroke([(7.0, 7.6), (7.0, 11.0)], 1.4)
    s.ellipse(7.0, 12.0, 3.8, 1.3)
    s.groove([(3.2, 3.4), (10.8, 3.4)], 0.8)
    return s


def m_quickening():
    """An hourglass running fast."""
    s = Shape(14)
    s.poly([(2.0, 0.8), (12.0, 0.8), (12.0, 2.4), (2.0, 2.4)])
    s.poly([(2.0, 11.6), (12.0, 11.6), (12.0, 13.2), (2.0, 13.2)])
    s.poly([(3.4, 2.4), (10.6, 2.4), (7.6, 7.0), (10.6, 11.6), (3.4, 11.6), (6.4, 7.0)])
    s.shine([(6.6, 9.0), (7.4, 10.6)], 1.2)
    return s


def m_gathering_charge():
    """A spiral, drawing inward to a bright core."""
    s = Shape(14)
    pts = []
    for i in range(46):
        t = i / 45
        a = math.radians(-20 + t * 560)
        r = 5.6 - 4.4 * t
        pts.append((7.0 + r * math.cos(a), 7.0 + r * math.sin(a)))
    s.stroke(pts, 1.3)
    s.ellipse(7.0, 7.0, 1.4)
    s.shine([(7.0, 7.0), (7.0, 7.0)], 1.0)
    return s


def m_plating():
    """A breastplate: shoulders, a ridge down its middle."""
    s = Shape(14)
    s.poly([(1.4, 2.6), (4.6, 1.2), (5.6, 2.8), (8.4, 2.8), (9.4, 1.2), (12.6, 2.6), (12.0, 6.2),
            (11.0, 12.4), (7.0, 13.4), (3.0, 12.4), (2.0, 6.2)])
    s.groove([(7.0, 4.0), (7.0, 12.4)], 0.8)
    s.groove([(2.6, 6.2), (4.4, 5.0)], 0.7)
    s.groove([(11.4, 6.2), (9.6, 5.0)], 0.7)
    s.shine([(3.0, 4.0), (3.4, 9.0)], 0.8)
    return s


def m_unbowed():
    """An anvil: struck, unmoved."""
    s = Shape(14)
    s.poly([(1.0, 3.2), (13.0, 3.2), (13.0, 5.6), (9.8, 6.8), (9.8, 9.0), (11.8, 10.6), (11.8, 12.6),
            (2.2, 12.6), (2.2, 10.6), (4.2, 9.0), (4.2, 6.8), (1.0, 5.2)])
    s.shine([(1.6, 3.6), (12.2, 3.6)], 0.8)
    return s


def m_deep_roots():
    """A stump, its roots spreading down."""
    s = Shape(14)
    s.poly([(4.0, 1.0), (10.0, 1.0), (9.6, 7.8), (4.4, 7.8)])
    s.poly(taper([(4.8, 6.6), (3.0, 9.4), (0.8, 13.0)], [1.4, 1.0, 0.3]))
    s.poly(taper([(7.0, 7.0), (6.6, 10.0), (7.4, 13.4)], [1.4, 1.0, 0.3]))
    s.poly(taper([(9.2, 6.6), (11.0, 9.4), (13.2, 13.0)], [1.4, 1.0, 0.3]))
    s.groove([(4.6, 2.2), (9.4, 2.2)], 0.8)
    s.groove([(6.0, 3.4), (6.0, 7.0)], 0.6)
    s.groove([(8.2, 3.4), (8.2, 6.4)], 0.6)
    return s


def m_riposte():
    """A small shield, a blow turned back off its edge."""
    s = Shape(14)
    s.poly([(1.2, 3.0), (8.0, 3.0), (8.0, 8.0), (4.6, 13.0), (1.2, 8.0)])
    s.stroke([(13.4, 11.0), (10.6, 9.4), (9.8, 6.2), (11.0, 3.4)], 1.1)
    s.poly([(9.2, 2.8), (13.6, 0.8), (12.6, 5.0)])
    s.shine([(2.4, 4.2), (2.4, 7.6)], 0.8)
    return s


def m_revitalize():
    """A five-petalled flower opening."""
    s = Shape(14)
    for i in range(5):
        a = -90 + i * 72
        x, y = polar(7.0, 7.4, 3.9, a)
        s.ellipse(x, y, 2.0)
    s.ellipse(7.0, 7.4, 1.5)
    s.shine([(7.0, 7.4), (7.0, 7.4)], 1.0)
    return s


def m_challenge():
    """A war horn: call them to you."""
    s = Shape(14)
    centre = [(1.4, 12.6), (2.6, 8.8), (5.0, 5.6), (8.4, 3.6), (11.6, 3.4)]
    s.poly(taper(centre, [0.7, 1.0, 1.5, 2.1, 2.8]))
    s.ellipse(11.8, 3.4, 1.4, 2.9)
    s.groove([(4.2, 7.4), (5.8, 8.6)], 0.7)
    s.shine([(1.8, 10.8), (4.2, 6.6)], 0.7)
    return s


GLYPHS = {
    'conquerors_resolve': k_conquerors_resolve, 'cadence': k_cadence, 'vein_breaker': k_vein_breaker,
    'ambush': k_ambush, 'deadeye': k_deadeye, 'silent_step': k_silent_step,
    'starfall': k_starfall, 'arc': k_arc, 'scholar': k_scholar,
    'brace': k_brace, 'pack_leader': k_pack_leader, 'cats_eye': k_cats_eye,
    'second_wind': m_second_wind, 'momentum': m_momentum, 'blooded': m_blooded,
    'relentless_strikes': m_relentless_strikes, 'killing_surge': m_killing_surge,
    'giant_slayer': m_giant_slayer,
    'pounce': m_pounce, 'drop_in': m_drop_in, 'vanish': m_vanish,
    'finishing_blow': m_finishing_blow, 'overkill': m_overkill, 'trophy_hunter': m_trophy_hunter,
    'attunement': m_attunement, 'unweave': m_unweave, 'clarity': m_clarity,
    'siphon': m_siphon, 'quickening': m_quickening, 'gathering_charge': m_gathering_charge,
    'plating': m_plating, 'unbowed': m_unbowed, 'deep_roots': m_deep_roots,
    'riposte': m_riposte, 'revitalize': m_revitalize, 'challenge': m_challenge,
}


# Stat shards: 8 x 8, hand-placed, coloured by stat.

SHARD_MAPS = {
    'ad': ["........", "......#.", ".....##.", "....##..", "...##...", "..##....", ".#d.....", "........"],
    'as': ["........", "...#..#.", "..##.##.", "..#..#..", ".##.##..", ".#..#...", "........", "........"],
    'ms': ["........", "....#...", ".##.##..", "....###.", ".##.##..", "....#...", "........", "........"],
    'armor': ["........", ".######.", ".#hh###.", ".#h####.", "..####..", "...##...", "........", "........"],
    'mr': ["........", "..####..", ".#hh###.", ".#h##d#.", ".###dd#.", "..####..", "........", "........"],
    'hp': ["........", ".##..##.", ".######.", ".#h####.", "..####..", "...##...", "........", "........"],
}


def stat_ramp(stat):
    c = STAT[stat]
    return [mix(c, (0, 0, 0, 255), 0.75), mix(c, (0, 0, 0, 255), 0.4), c, mix(c, (255, 255, 255, 255), 0.35),
            mix(c, (255, 255, 255, 255), 0.75)]


PADLOCK = [
    "..ooo..",
    ".o...o.",
    ".o...o.",
    "ooooooo",
    "o33333o",
    "o32223o",
    "o33233o",
    "o22222o",
    "ooooooo",
]


def padlock(ramp):
    img = new(7, 9)
    key = {'o': ramp[0], '2': ramp[2], '3': ramp[3]}
    for y, row in enumerate(PADLOCK):
        for x, ch in enumerate(row):
            if ch in key:
                img.putpixel((x, y), key[ch])
    return img


# ---------------------------------------------------------------------------------------------
# Glyph states
# ---------------------------------------------------------------------------------------------

def state_ramp(ramp, state):
    """chosen: the path's ramp; open: dimmed toward slate; barred: dimmer; locked: near black."""
    if state == 'chosen':
        return ramp
    if state == 'open':
        return [ramp[0]] + [mix(grey(c), c, 0.55) for c in ramp[1:]]
    if state == 'barred':
        return [ramp[0]] + [mix(grey(c), (20, 22, 28, 255), 0.6) for c in ramp[1:]]
    return [hexc('#0c0d10')] + [mix(grey(c), (12, 13, 16, 255), 0.82) for c in ramp[1:]]


def glyph(rid, ramp, state='chosen'):
    return shade(GLYPHS[rid](), state_ramp(ramp, state))


def emblem(path, state='chosen', size=20):
    s = EMBLEMS[path]()
    if size != 20:
        small = Shape(size)
        for k in small.layers:
            small.layers[k] = s.layers[k].resize((size * SS, size * SS), Image.LANCZOS)
        s = small
    return shade(s, state_ramp(PATHS[path]['ramp'], state))


def shard_glyph(stat, state='chosen'):
    return shade(MaskShape(SHARD_MAPS[stat]), state_ramp(stat_ramp(stat), state))


# ---------------------------------------------------------------------------------------------
# The stand-in font (Minecraft's metrics, our own glyphs)
# ---------------------------------------------------------------------------------------------

_F = {
    'A': [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    'B': ["####.", "#...#", "####.", "#...#", "#...#", "#...#", "####."],
    'C': [".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."],
    'D': ["####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."],
    'E': ["#####", "#....", "###..", "#....", "#....", "#....", "#####"],
    'F': ["#####", "#....", "###..", "#....", "#....", "#....", "#...."],
    'G': [".####", "#....", "#..##", "#...#", "#...#", "#...#", ".###."],
    'H': ["#...#", "#...#", "#####", "#...#", "#...#", "#...#", "#...#"],
    'I': ["###", ".#.", ".#.", ".#.", ".#.", ".#.", "###"],
    'J': ["....#", "....#", "....#", "....#", "....#", "#...#", ".###."],
    'K': ["#...#", "#..#.", "###..", "#..#.", "#...#", "#...#", "#...#"],
    'L': ["#....", "#....", "#....", "#....", "#....", "#....", "#####"],
    'M': ["#...#", "##.##", "#.#.#", "#...#", "#...#", "#...#", "#...#"],
    'N': ["#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"],
    'O': [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    'P': ["####.", "#...#", "####.", "#....", "#....", "#....", "#...."],
    'Q': [".###.", "#...#", "#...#", "#...#", "#...#", "#..#.", ".##.#"],
    'R': ["####.", "#...#", "####.", "#...#", "#...#", "#...#", "#...#"],
    'S': [".####", "#....", ".###.", "....#", "....#", "#...#", ".###."],
    'T': ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    'U': ["#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    'V': ["#...#", "#...#", "#...#", "#...#", ".#.#.", ".#.#.", "..#.."],
    'W': ["#...#", "#...#", "#...#", "#...#", "#.#.#", "##.##", "#...#"],
    'X': ["#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"],
    'Y': ["#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..", "..#.."],
    'Z': ["#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"],
    'a': [".....", ".....", ".###.", "....#", ".####", "#...#", ".####"],
    'b': ["#....", "#....", "#.##.", "##..#", "#...#", "#...#", "####."],
    'c': [".....", ".....", ".###.", "#...#", "#....", "#...#", ".###."],
    'd': ["....#", "....#", ".##.#", "#..##", "#...#", "#...#", ".####"],
    'e': [".....", ".....", ".###.", "#...#", "#####", "#....", ".####"],
    'f': ["..##", ".#..", "####", ".#..", ".#..", ".#..", ".#.."],
    'g': [".....", ".....", ".####", "#...#", "#...#", ".####", "....#", "####."],
    'h': ["#....", "#....", "#.##.", "##..#", "#...#", "#...#", "#...#"],
    'i': ["#", ".", "#", "#", "#", "#", "#"],
    'j': ["...#", "....", "...#", "...#", "...#", "...#", "#..#", ".##."],
    'k': ["#...", "#...", "#..#", "#.#.", "##..", "#.#.", "#..#"],
    'l': ["#.", "#.", "#.", "#.", "#.", "#.", ".#"],
    'm': [".....", ".....", "##.#.", "#.#.#", "#.#.#", "#...#", "#...#"],
    'n': [".....", ".....", "#.##.", "##..#", "#...#", "#...#", "#...#"],
    'o': [".....", ".....", ".###.", "#...#", "#...#", "#...#", ".###."],
    'p': [".....", ".....", "#.##.", "##..#", "#...#", "####.", "#....", "#...."],
    'q': [".....", ".....", ".##.#", "#..##", "#...#", ".####", "....#", "....#"],
    'r': [".....", ".....", "#.##.", "##..#", "#....", "#....", "#...."],
    's': [".....", ".....", ".####", "#....", ".###.", "....#", "####."],
    't': [".#.", ".#.", "###", ".#.", ".#.", ".#.", "..#"],
    'u': [".....", ".....", "#...#", "#...#", "#...#", "#...#", ".####"],
    'v': [".....", ".....", "#...#", "#...#", "#...#", ".#.#.", "..#.."],
    'w': [".....", ".....", "#...#", "#...#", "#.#.#", "#.#.#", ".####"],
    'x': [".....", ".....", "#...#", ".#.#.", "..#..", ".#.#.", "#...#"],
    'y': [".....", ".....", "#...#", "#...#", "#...#", ".####", "....#", "####."],
    'z': [".....", ".....", "#####", "...#.", "..#..", ".#...", "#####"],
    '0': [".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."],
    '1': ["..#..", ".##..", "..#..", "..#..", "..#..", "..#..", "#####"],
    '2': [".###.", "#...#", "....#", "..##.", ".#...", "#....", "#####"],
    '3': [".###.", "#...#", "....#", "..##.", "....#", "#...#", ".###."],
    '4': ["...##", "..#.#", ".#..#", "#...#", "#####", "....#", "....#"],
    '5': ["#####", "#....", "####.", "....#", "....#", "#...#", ".###."],
    '6': ["..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."],
    '7': ["#####", "#...#", "....#", "...#.", "..#..", "..#..", "..#.."],
    '8': [".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."],
    '9': [".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."],
    '.': [".", ".", ".", ".", ".", ".", "#"],
    ',': [".", ".", ".", ".", ".", ".", "#", "#"],
    ':': [".", ".", "#", ".", ".", ".", "#"],
    ';': [".", ".", "#", ".", ".", ".", "#", "#"],
    '!': ["#", "#", "#", "#", "#", ".", "#"],
    '?': [".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#.."],
    "'": ["#", "#", ".", ".", ".", ".", "."],
    '-': ["....", "....", "....", "####", "....", "....", "...."],
    '+': [".....", "..#..", "..#..", "#####", "..#..", "..#..", "....."],
    '%': ["#...#", "#..#.", "...#.", "..#..", ".#...", ".#..#", "#...#"],
    '/': ["....#", "...#.", "...#.", "..#..", ".#...", ".#...", "#...."],
    '(': ["..#", ".#.", "#..", "#..", "#..", ".#.", "..#"],
    ')': ["#..", ".#.", "..#", "..#", "..#", ".#.", "#.."],
    '·': [".", ".", ".", "#", ".", ".", "."],
    '>': ["#...", ".#..", "..#.", "...#", "..#.", ".#..", "#..."],
    '<': ["...#", "..#.", ".#..", "#...", ".#..", "..#.", "...#"],
    '=': [".....", ".....", "#####", ".....", "#####", ".....", "....."],
    '→': ["......", "....#.", ".....#", "######", ".....#", "....#.", "......"],
    '×': [".....", ".....", "#...#", ".#.#.", "..#..", ".#.#.", "#...#"],
}
SPACE_W = 3


def text_width(s, bold=False):
    w = 0
    for ch in s:
        g = _F.get(ch)
        w += (SPACE_W if g is None else len(g[0])) + 1 + (1 if bold else 0)
    return max(0, w - 1)


def draw_text(img, x, y, s, colour, shadow=True, bold=False):
    """Draws `s` with its top-left at (x, y). Minecraft draws a shadow one pixel down-right at a
    quarter brightness; so does this."""
    shadow_c = (colour[0] // 4, colour[1] // 4, colour[2] // 4, colour[3])
    passes = ([(1, 1, shadow_c)] if shadow else []) + [(0, 0, colour)]
    for ox, oy, c in passes:
        cx = x
        for ch in s:
            g = _F.get(ch)
            if g is None:
                cx += SPACE_W + 1 + (1 if bold else 0)
                continue
            for gy, row in enumerate(g):
                for gx, p in enumerate(row):
                    if p == '#':
                        put(img, cx + gx + ox, y + gy + oy, c)
                        if bold:
                            put(img, cx + gx + ox + 1, y + gy + oy, c)
            cx += len(g[0]) + 1 + (1 if bold else 0)
    return x + text_width(s, bold)


def draw_centred(img, cx, y, s, colour, bold=False):
    draw_text(img, cx - text_width(s, bold) // 2, y, s, colour, bold=bold)


def wrap(s, width):
    words, lines, cur = s.split(), [], ''
    for w in words:
        trial = (cur + ' ' + w).strip()
        if text_width(trial) <= width:
            cur = trial
        else:
            lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines


# ---------------------------------------------------------------------------------------------
# The runes, and the pages the mockups show
# ---------------------------------------------------------------------------------------------

RUNES = {
    'vanguard': {
        'keystones': [('conquerors_resolve', "Conqueror's Resolve"), ('cadence', 'Cadence'),
                      ('vein_breaker', 'Vein Breaker')],
        'rows': [[('second_wind', 'Second Wind'), ('momentum', 'Momentum'), ('blooded', 'Blooded')],
                 [('relentless_strikes', 'Relentless Strikes'), ('killing_surge', 'Killing Surge'),
                  ('giant_slayer', 'Giant Slayer')]],
    },
    'reaver': {
        'keystones': [('ambush', 'Ambush'), ('deadeye', 'Deadeye'), ('silent_step', 'Silent Step')],
        'rows': [[('pounce', 'Pounce'), ('drop_in', 'Drop In'), ('vanish', 'Vanish')],
                 [('finishing_blow', 'Finishing Blow'), ('overkill', 'Overkill'),
                  ('trophy_hunter', 'Trophy Hunter')]],
    },
    'aetherwright': {
        'keystones': [('starfall', 'Starfall'), ('arc', 'Arc'), ('scholar', 'Scholar')],
        'rows': [[('attunement', 'Attunement'), ('unweave', 'Unweave'), ('clarity', 'Clarity')],
                 [('siphon', 'Siphon'), ('quickening', 'Quickening'), ('gathering_charge', 'Gathering Charge')]],
    },
    'bulwark': {
        'keystones': [('brace', 'Brace'), ('pack_leader', 'Pack Leader'), ('cats_eye', "Cat's Eye")],
        'rows': [[('plating', 'Plating'), ('unbowed', 'Unbowed'), ('deep_roots', 'Deep Roots')],
                 [('riposte', 'Riposte'), ('revitalize', 'Revitalize'), ('challenge', 'Challenge')]],
    },
}

SHARD_ROWS = [('Offence', ['ad', 'as', 'ms']), ('Flex', ['ad', 'armor', 'mr']), ('Defence', ['hp', 'armor', 'mr'])]

# Which path each rune belongs to.
RUNE_PATH = {rid: path for path, d in RUNES.items()
             for rid, _ in d['keystones'] + [rune for row in d['rows'] for rune in row]}

# One plain sentence per keystone: what the box under the secondary dial says.
BLURB = {
    'conquerors_resolve': 'Every second in a fight: more Attack Damage and healing, up to 10 stacks.',
    'cadence': 'Three charged hits on one enemy: the third hits hard and Exposes it.',
    'vein_breaker': 'Sneak and mine: the connected ore or logs break with it.',
    'ambush': 'Hit an enemy that has not seen you for far more damage.',
    'deadeye': 'An arrow near the head is a headshot, stronger from afar.',
    'silent_step': 'You make no vibrations; sneaking, mobs notice you only up close.',
    'starfall': 'Charged hits and magic call a falling star down on the target.',
    'arc': 'Every fourth hit throws lightning that jumps between enemies.',
    'scholar': 'More experience from everything; enchanting costs a level less.',
    'brace': 'Block or take a big hit: huge defences, then a shockwave.',
    'pack_leader': 'Your pets share your defences, hit harder, and heal with you.',
    'cats_eye': 'Creepers and phantoms keep away from you, as they do from cats.',
}

# The pages the mockups show. `secondary` None: not open yet. `shards` None per row: not chosen;
# `shards_locked` per row: not open yet.
PAGES = {
    'vanguard': {
        'primary': 'vanguard', 'keystone': 1, 'primary_minors': [2, 2],
        'secondary': 'bulwark', 'secondary_minors': [0, 0],
        'shards': [1, 1, None], 'shards_locked': [False, False, True], 'growth': {'blooded': (9, 12)},
    },
    'reaver': {
        'primary': 'reaver', 'keystone': 1, 'primary_minors': [0, 1],
        'secondary': 'aetherwright', 'secondary_minors': [2, 0],
        'shards': [0, 0, 0], 'shards_locked': [False, False, False], 'growth': {},
    },
    'aetherwright': {
        'primary': 'aetherwright', 'keystone': 0, 'primary_minors': [0, 2],
        'secondary': 'vanguard', 'secondary_minors': [1, 2],
        'shards': [2, 2, 0], 'shards_locked': [False, False, False], 'growth': {'gathering_charge': (4, 12)},
    },
    'bulwark': {
        'primary': 'bulwark', 'keystone': 2, 'primary_minors': [2, 2],
        'secondary': 'reaver', 'secondary_minors': [0, 0],
        'shards': [1, 1, 1], 'shards_locked': [False, False, False], 'growth': {'deep_roots': (12, 12)},
    },
    'new_player': {
        'primary': 'reaver', 'keystone': 0, 'primary_minors': [1, 0],
        'secondary': None, 'secondary_minors': [None, None],
        'shards': [None, None, None], 'shards_locked': [True, True, True], 'growth': {},
    },
}
PAGE = PAGES['vanguard']

# What the hover card says for the rune the mockup hovers.
CARDS = {
    'conquerors_resolve': {
        'name': "Conqueror's Resolve", 'tag': 'Vanguard keystone',
        'text': 'Every second in a fight you gain a stack. They clear when the fight ends.',
        'numbers': [[('+0.5', STAT['ad']), ('Attack Damage a stack', TEXT_BODY)],
                    [('+1%', STAT['omni']), ('omnivamp a stack', TEXT_BODY)],
                    [('10', TEXT), ('stacks at most', TEXT_BODY)]],
        'note': 'Swapping Cadence for it costs 1 Rune Chalk and 3 levels.',
    },
}


def set_page(name):
    global PAGE
    PAGE = PAGES[name]


def pr():
    return PATHS[PAGE['primary']]['ramp']


def sr():
    return PATHS[PAGE['secondary']]['ramp']


def chosen_keystone():
    return RUNES[PAGE['primary']]['keystones'][PAGE['keystone']]


# ---------------------------------------------------------------------------------------------
# The art kit: a night sky in a brass frame; runes as stars
# ---------------------------------------------------------------------------------------------

PANEL_W, PANEL_H = 400, 224

STAR_BRASS = [hexc('#3a3020'), hexc('#7a6840'), hexc('#c8b07a'), hexc('#f0e0b0')]
SKY = [hexc('#04060c'), hexc('#080c18'), hexc('#0e1426')]


def star_panel(rng):
    img = new(PANEL_W, PANEL_H)
    for y in range(PANEL_H):
        hline(img, 0, PANEL_W, y, mix(SKY[2], SKY[0], y / PANEL_H))
    for _ in range(420):
        x, y = rng.randrange(PANEL_W), rng.randrange(PANEL_H)
        b = rng.random()
        c = (150, 160, 190, 90) if b < 0.7 else (200, 210, 235, 150) if b < 0.95 else (255, 255, 255, 220)
        put(img, x, y, c)
    for i, c in ((0, STAR_BRASS[0]), (1, STAR_BRASS[2]), (2, STAR_BRASS[0])):
        hline(img, i, PANEL_W - i, i, c)
        hline(img, i, PANEL_W - i, PANEL_H - 1 - i, c)
        vline(img, i, i, PANEL_H - i, c)
        vline(img, PANEL_W - 1 - i, i, PANEL_H - i, c)
    for (cx, cy) in ((0, 0), (PANEL_W, 0), (0, PANEL_H), (PANEL_W, PANEL_H)):
        circle(img, cx, cy, 13, STAR_BRASS[1])
        circle(img, cx, cy, 9, with_alpha(STAR_BRASS[1], 150))
    hline(img, 6, PANEL_W - 6, 18, with_alpha(STAR_BRASS[1], 180))
    return img


def nebula(img, x0, y0, x1, y1, ramp, strength, rng, count=7):
    """A faint cloud of a path's colour."""
    pts = [(rng.uniform(x0, x1), rng.uniform(y0, y1), rng.uniform(18, 46)) for _ in range(count)]
    for y in range(max(0, y0), min(PANEL_H, y1)):
        for x in range(max(0, x0), min(PANEL_W, x1)):
            v = 0.0
            for (px, py, r) in pts:
                d = math.hypot(x - px, (y - py) * 1.3) / r
                if d < 1:
                    v += (1 - d) ** 2
            if v > 0:
                put(img, x, y, with_alpha(ramp[2], min(1.0, v) * 46 * strength))


def star_socket(size, ramp, state, shape='round'):
    """Open: a thin ring of starlight. Chosen: a bright star in a halo. World: a planet with its
    orbit. Barred or locked: a ring of dust."""
    img = new(size, size)
    r = size / 2
    if state == 'chosen':
        glow(img, r, r, r, ramp[3], 150)
        for (x, y) in disc(r, r, r - 1.6) - disc(r, r, r - 2.8):
            put(img, x, y, ramp[4])
        for (x, y) in disc(r, r, r - 2.8):
            put(img, x, y, with_alpha(mix(SKY[0], ramp[1], 0.55), 235))
    elif state in ('locked', 'barred'):
        for (x, y) in disc(r, r, r - 1.6) - disc(r, r, r - 2.6):
            if (x + y) % 2 == 0:
                put(img, x, y, (90, 96, 110, 160))
        for (x, y) in disc(r, r, r - 2.6):
            put(img, x, y, (6, 8, 14, 220))
    else:
        for (x, y) in disc(r, r, r - 1.6) - disc(r, r, r - 2.6):
            put(img, x, y, with_alpha(mix(ramp[2], (150, 160, 190, 255), 0.5), 170))
        for (x, y) in disc(r, r, r - 2.6):
            put(img, x, y, (6, 9, 18, 220))
    if shape == 'world':
        for i in range(0, 360, 3):
            a = math.radians(i)
            ox, oy = r + (r + 1.5) * math.cos(a), r + (r * 0.34) * math.sin(a) - 1
            if math.sin(a) < 0 and abs(ox - r) < r - 2:
                continue
            put(img, int(ox), int(oy), ramp[3] if state == 'chosen' else (130, 140, 160, 170))
    return img


def place_rune(img, rid, ramp, state, x, y, size, world=False):
    """A socket and its glyph, centred in a size x size box at (x, y). A locked socket is empty."""
    blit(img, star_socket(size, ramp, state, 'world' if world else 'round'), x, y)
    if state == 'locked':
        return
    g = glyph(rid, ramp, state)
    blit(img, g, x + (size - g.width) // 2, y + (size - g.height) // 2)


def ring_arc(img, cx, cy, rad, frac, colour, back):
    """A growth ring: `frac` of a circle, clockwise from the top."""
    for (x, y) in disc(cx, cy, rad) - disc(cx, cy, rad - 1.2):
        a = (math.degrees(math.atan2(x + 0.5 - cx, -(y + 0.5 - cy))) + 360) % 360
        put(img, x, y, colour if a <= frac * 360 else back)


def draw_button(img, x, y, w, h, label, primary):
    rect(img, x, y, x + w, y + h, (10, 14, 28, 255))
    c = STAR_BRASS[2] if primary else STAR_BRASS[1]
    hline(img, x, x + w, y, c)
    hline(img, x, x + w, y + h - 1, c)
    vline(img, x, y, y + h, c)
    vline(img, x + w - 1, y, y + h, c)
    draw_text(img, x + (w - text_width(label)) // 2, y + (h - 8) // 2 + 1, label, TEXT if primary else TEXT_BODY)


def draw_chalk(img, x, y):
    """Rune Chalk: a short white stick, chipped at one end."""
    for i in range(6):
        put(img, x + i, y + 6 - i, hexc('#f4f2ea'))
        put(img, x + i + 1, y + 6 - i, hexc('#c8c4b8'))
        put(img, x + i, y + 7 - i, hexc('#8c887c'))
    put(img, x + 6, y, hexc('#ffffff'))


def draw_xp_orb(img, x, y):
    for (px, py) in disc(x + 3.5, y + 3.5, 3.4):
        put(img, px, py, hexc('#3c8c14'))
    for (px, py) in disc(x + 3.5, y + 3.5, 2.4):
        put(img, px, py, hexc('#8ce03c'))
    put(img, x + 2, y + 2, hexc('#e8ff9c'))


def title_bar(img, cost=True):
    """The title on the left; Save and Reset on the right, the cost of this save beside them."""
    draw_text(img, 9, 7, 'Runes', STAR_BRASS[3], bold=True)
    bx = PANEL_W - 8
    for label, primary in (('Save', True), ('Reset', False)):
        w = text_width(label) + 14
        bx -= w
        draw_button(img, bx, 5, w, 12, label, primary)
        bx -= 4
    if cost:
        cx = bx - 64
        draw_chalk(img, cx, 7)
        draw_text(img, cx + 10, 7, '×1', TEXT_BODY)
        draw_xp_orb(img, cx + 26, 7)
        draw_text(img, cx + 36, 7, '3 lv', STAT['xp'])
    else:
        draw_text(img, bx - text_width('free') - 4, 7, 'free', TEXT_DIM)


def blurb_box(img, x0, y0, x1, y1):
    """The chosen keystone, in words."""
    hline(img, x0, x1, y0, with_alpha(STAR_BRASS[1], 160))
    rect(img, x0, y0 + 1, x1, y1, with_alpha((4, 6, 14, 255), 120))
    rid, name = chosen_keystone()
    world = PAGE['keystone'] == 2
    draw_text(img, x0 + 5, y0 + 5, name, pr()[4], bold=True)
    draw_text(img, x0 + 9 + text_width(name, True), y0 + 5, '· world keystone' if world else '· keystone', TEXT_FAINT)
    for i, ln in enumerate(wrap(BLURB[rid], x1 - x0 - 10)):
        if y0 + 17 + i * 10 + 8 <= y1:
            draw_text(img, x0 + 5, y0 + 17 + i * 10, ln, TEXT_BODY)


def draw_card(img, rid, ramp, x, y, w=168):
    """The hover card: name, tag, a plain sentence, numbers by stat colour, the fine print. A
    nine-sliced panel in game; drawn whole here."""
    c = CARDS[rid]
    body = wrap(c['text'], w - 14)
    note = wrap(c['note'], w - 14)
    h = 6 + 10 + 12 + len(body) * 10 + 4 + len(c['numbers']) * 10 + 4 + len(note) * 10 + 4
    rect(img, x + 2, y + 2, x + w + 2, y + h + 2, (0, 0, 0, 110))
    rect(img, x, y, x + w, y + h, with_alpha((6, 9, 20, 255), 244))
    hline(img, x, x + w, y, ramp[3])
    hline(img, x, x + w, y + h - 1, ramp[1])
    vline(img, x, y, y + h, ramp[2])
    vline(img, x + w - 1, y, y + h, ramp[1])
    for (cx, cy) in ((x, y), (x + w - 1, y), (x, y + h - 1), (x + w - 1, y + h - 1)):
        put(img, cx, cy, STAR_BRASS[3])
    yy = y + 6
    draw_text(img, x + 7, yy, c['name'], TEXT, bold=True)
    yy += 10
    draw_text(img, x + 7, yy, c['tag'], ramp[3])
    yy += 12
    for ln in body:
        draw_text(img, x + 7, yy, ln, TEXT_BODY)
        yy += 10
    yy += 4
    for parts in c['numbers']:
        xx = x + 7
        for s, col in parts:
            xx = draw_text(img, xx, yy, s, col) + 1 + SPACE_W
        yy += 10
    yy += 4
    for ln in note:
        draw_text(img, x + 7, yy, ln, TEXT_DIM)
        yy += 10


def cursor(img, x, y):
    """A plain arrow pointer, drawn here (not Minecraft's)."""
    rows = ["o.......", "oo......", "owo.....", "owwo....", "owwwo...", "owwwwo..", "owwwwwo.",
            "owwwooo.", "owoowo..", "oo..owo.", "......o."]
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == 'o':
                put(img, x + i, y + j, hexc('#101010'))
            elif ch == 'w':
                put(img, x + i, y + j, hexc('#ffffff'))


# ---------------------------------------------------------------------------------------------
# The orrery (Sanchit's pick, 2026-10-17): the primary path a dial, keystones round its hub, minors
# on its outer ring, one row to each half; the secondary path a smaller dial of minors only.
# ---------------------------------------------------------------------------------------------

KEY_ANGLES = [210, 330, 90]                     # two combat keystones above the hub, the world one below
PRIMARY_ROWS = [[150, 180, 210], [330, 0, 30]]  # row 1 on the left half of the ring, row 2 the right
SECONDARY_ROWS = [[140, 180, 220], [320, 0, 40]]

P_CX, P_CY = 98, 122                            # the primary dial
P_HUB, P_KEY, P_MINOR, P_RIM = 14, 37, 70, 88
S_CX, S_CY = 248, 76                            # the secondary dial
S_HUB, S_MINOR, S_RIM = 11, 36, 52
CORNERS = [(10, 24), (164, 24), (10, 198), (164, 198)]   # the four path tabs, round the primary dial


def dial_frame(img, cx, cy, rim_r, rows, minor_r, ramp, key_r=0):
    """The brass of a dial: its rim with ticks, its rows as faint arcs, the keystone ring."""
    circle(img, cx, cy, rim_r, STAR_BRASS[1], 1.6)
    circle(img, cx, cy, rim_r - 3, with_alpha(STAR_BRASS[1], 120))
    for a in range(0, 360, 15):
        x0, y0 = polar(cx, cy, rim_r - 3, a)
        x1, y1 = polar(cx, cy, rim_r - (6 if a % 45 else 8), a)
        line(img, x0, y0, x1, y1, with_alpha(STAR_BRASS[2], 160))
    for row in rows:
        a0, a1 = row[0] - 12, row[-1] + 12
        circle(img, cx, cy, minor_r + 0.5, with_alpha(ramp[2], 110), 1.0, arc=(a0 % 360, a1 % 360))
    if key_r:
        circle(img, cx, cy, key_r + 0.5, with_alpha(STAR_BRASS[1], 90), 1.0, dash=10)


def dial_hub(img, cx, cy, hub_r, path, ramp, locked=False):
    for (x, y) in disc(cx, cy, hub_r):
        put(img, x, y, mix(SKY[1], ramp[1], 0.45) if not locked else SKY[1])
    circle(img, cx, cy, hub_r, STAR_BRASS[2] if not locked else STAR_BRASS[1], 1.2)
    if locked:
        blit(img, padlock(NEUTRAL), int(cx - 3), int(cy - 4))
        return
    em = emblem(path, 'chosen', 20 if hub_r >= 14 else 14)
    blit(img, em, int(cx - em.width / 2), int(cy - em.height / 2))


def spokes(img, cx, cy, targets, ramp):
    for (tx, ty) in targets:
        line(img, cx, cy, tx, ty, with_alpha(ramp[3], 200))
        line(img, cx + 1, cy, tx + 1, ty, with_alpha(ramp[2], 90))


def minor_ring(img, cx, cy, minor_r, rows, path, picks, ramp, locked=False):
    for r in range(2):
        for k in range(3):
            x, y = polar(cx, cy, minor_r, rows[r][k])
            if locked:
                place_rune(img, None, NEUTRAL, 'locked', int(x - 11), int(y - 11), 22)
                continue
            rid, name = RUNES[path]['rows'][r][k]
            st = 'chosen' if k == picks[r] else 'open'
            place_rune(img, rid, ramp, st, int(x - 11), int(y - 11), 22)
            if st == 'chosen' and rid in PAGE['growth']:
                n, cap = PAGE['growth'][rid]
                ring_arc(img, int(x - 11) + 11, int(y - 11) + 11, 13.4, n / cap, ramp[4], with_alpha(ramp[0], 200))


def render_orrery(hover=None):
    rng = random.Random(2203)
    img = star_panel(rng)
    p, s = PAGE['primary'], PAGE['secondary']
    title_bar(img, cost=PAGE is not PAGES['new_player'])

    # --- the primary dial
    nebula(img, P_CX - 90, P_CY - 90, P_CX + 90, P_CY + 90, pr(), 0.8, random.Random(7))
    dial_frame(img, P_CX, P_CY, P_RIM, PRIMARY_ROWS, P_MINOR, pr(), P_KEY)
    targets = [polar(P_CX, P_CY, P_KEY, KEY_ANGLES[PAGE['keystone']])]
    targets += [polar(P_CX, P_CY, P_MINOR, PRIMARY_ROWS[r][PAGE['primary_minors'][r]]) for r in range(2)]
    spokes(img, P_CX, P_CY, targets, pr())
    dial_hub(img, P_CX, P_CY, P_HUB, p, pr())
    for k, (rid, name) in enumerate(RUNES[p]['keystones']):
        x, y = polar(P_CX, P_CY, P_KEY, KEY_ANGLES[k])
        place_rune(img, rid, pr(), 'chosen' if k == PAGE['keystone'] else 'open', int(x - 15), int(y - 15), 30,
                   world=k == 2)
    minor_ring(img, P_CX, P_CY, P_MINOR, PRIMARY_ROWS, p, PAGE['primary_minors'], pr())
    # the four paths sit in the dial's corners; choosing one turns the dial to it
    for path, (x, y) in zip(PATH_ORDER, CORNERS):
        state = 'chosen' if path == p else 'open'
        blit(img, star_socket(22, PATHS[path]['ramp'], state), x, y)
        blit(img, emblem(path, state, 14), x + 4, y + 4)
    draw_centred(img, P_CX, 210, PATHS[p]['name'].upper(), pr()[3], bold=True)

    # --- the secondary dial: minors only
    tx = 310
    draw_text(img, tx, 26, 'SECONDARY', TEXT_DIM)
    others = [q for q in PATH_ORDER if q != p]
    if s:
        nebula(img, S_CX - 55, S_CY - 55, S_CX + 55, S_CY + 55, sr(), 0.6, random.Random(11))
        dial_frame(img, S_CX, S_CY, S_RIM, SECONDARY_ROWS, S_MINOR, sr())
        spokes(img, S_CX, S_CY, [polar(S_CX, S_CY, S_MINOR, SECONDARY_ROWS[r][PAGE['secondary_minors'][r]])
                                 for r in range(2)], sr())
        dial_hub(img, S_CX, S_CY, S_HUB, s, sr())
        minor_ring(img, S_CX, S_CY, S_MINOR, SECONDARY_ROWS, s, PAGE['secondary_minors'], sr())
        draw_text(img, tx, 36, PATHS[s]['name'].upper(), sr()[3], bold=True)
        lines = wrap(PATHS[s]['promise'], PANEL_W - 8 - tx)
        for i, ln in enumerate(lines):
            draw_text(img, tx, 76 + i * 10, ln, TEXT_BODY)
        y = 76 + len(lines) * 10 + 4
        draw_text(img, tx, y, 'one from each', TEXT_FAINT)
        draw_text(img, tx, y + 9, 'half', TEXT_FAINT)
    else:
        dial_frame(img, S_CX, S_CY, S_RIM, [], S_MINOR, NEUTRAL)
        dial_hub(img, S_CX, S_CY, S_HUB, None, NEUTRAL, locked=True)
        minor_ring(img, S_CX, S_CY, S_MINOR, SECONDARY_ROWS, None, None, NEUTRAL, locked=True)
        draw_text(img, tx, 36, 'LOCKED', TEXT_FAINT, bold=True)
        draw_text(img, tx, 78, 'Opens at your', TEXT_DIM)
        draw_text(img, tx, 88, '1st milestone.', TEXT_DIM)
    for i, path in enumerate(others):
        x, y = tx + i * 24, 50
        state = 'chosen' if path == s else 'barred' if not s else 'open'
        blit(img, star_socket(20, PATHS[path]['ramp'], state), x, y)
        blit(img, emblem(path, state, 14), x + 3, y + 3)

    # --- shards: three little moons in each row
    sy = 134
    draw_text(img, 202, sy, 'SHARDS', TEXT_DIM)
    for r, (label, stats) in enumerate(SHARD_ROWS):
        x0 = 202 + r * 64
        locked = PAGE['shards_locked'][r]
        for k, st in enumerate(stats):
            chosen = PAGE['shards'][r] == k
            state = 'locked' if locked else ('chosen' if chosen else 'open')
            blit(img, star_socket(16, stat_ramp(st), state), x0 + k * 17, sy + 11)
            if locked:
                blit(img, padlock(NEUTRAL), x0 + k * 17 + 4, sy + 15)
            else:
                blit(img, shard_glyph(st, 'chosen' if chosen else 'open'), x0 + k * 17 + 4, sy + 15)
        draw_centred(img, x0 + 25, sy + 29, label, TEXT_FAINT)

    blurb_box(img, 198, 176, PANEL_W - 8, PANEL_H - 6)

    if hover:
        rid, k = hover
        x, y = polar(P_CX, P_CY, P_KEY, KEY_ANGLES[k])
        circle(img, x, y, 17.4, with_alpha(TEXT, 230), 1.0)
        cursor(img, int(x + 3), int(y + 3))
        draw_card(img, rid, pr(), int(x + 18), int(y - 6))
    return img


# ---------------------------------------------------------------------------------------------
# Mockups
# ---------------------------------------------------------------------------------------------

def backdrop(w, h, seed):
    """What sits behind a Minecraft screen: the world, blurred and darkened. Ours is a dusk
    gradient with soft shapes, built here, so the mockup carries no vanilla art."""
    rng = random.Random(seed)
    img = new(w, h)
    d = ImageDraw.Draw(img)
    for y in range(h):
        d.line([(0, y), (w, y)], fill=mix(hexc('#3c5a78'), hexc('#2a3424'), y / h))
    for _ in range(14):
        x, y, r = rng.uniform(0, w), rng.uniform(h * 0.45, h), rng.uniform(w * 0.05, w * 0.18)
        d.ellipse([x - r, y - r * 0.5, x + r, y + r * 0.5], fill=mix(hexc('#2c3c24'), hexc('#4a5a38'), rng.random()))
    img = img.filter(ImageFilter.GaussianBlur(radius=max(4, w // 90)))
    return Image.alpha_composite(img, Image.new('RGBA', (w, h), (0, 0, 0, 110)))


def on_screen(panel, gui_w, gui_h, k, seed=5):
    """The panel centred on a screen of gui_w x gui_h GUI pixels, shown at GUI scale k."""
    screen = backdrop(gui_w * k, gui_h * k, seed)
    screen.alpha_composite(scaled(panel, k), ((gui_w - panel.width) // 2 * k, (gui_h - panel.height) // 2 * k))
    return screen


def drop_shadow(panel):
    out = new(panel.width + 2, panel.height + 2)
    rect(out, 2, 2, out.width, out.height, (0, 0, 0, 110))
    out.alpha_composite(panel, (0, 0))
    return out


def icon_sheet():
    """Every rune in its real socket on the night sky, chosen above and open below, at 4x: the
    readability check that matters, since this is where they are seen."""
    k, cell = 4, 34
    rows = len(PATH_ORDER) * 2 + 1
    sheet = new(10 * cell + 80, rows * cell + 10, SKY[1])
    rng = random.Random(41)
    for _ in range(300):
        put(sheet, rng.randrange(sheet.width), rng.randrange(sheet.height), (150, 160, 190, 90))
    y = 6
    for path in PATH_ORDER:
        ramp = PATHS[path]['ramp']
        ids = [(rid, i < 3, i == 2) for i, (rid, _) in enumerate(RUNES[path]['keystones'])]
        ids += [(rid, False, False) for row in RUNES[path]['rows'] for rid, _ in row]
        for state in ('chosen', 'open'):
            draw_text(sheet, 4, y + 12, path[:5] if state == 'chosen' else '', ramp[3])
            blit(sheet, emblem(path, state), 40, y + 7)
            x = 70
            for rid, key, world in ids:
                size = 30 if key else 22
                place_rune(sheet, rid, ramp, state, x + (30 - size) // 2, y + (30 - size) // 2, size, world=world)
                x += cell
            y += cell
    x = 70
    for st in SHARD_MAPS:
        blit(sheet, star_socket(16, stat_ramp(st), 'chosen'), x + 7, y + 7)
        blit(sheet, shard_glyph(st), x + 11, y + 11)
        x += cell
    return scaled(sheet, k)


def fit_sheet(panel):
    """The panel on a 720p screen at GUI scale 3 (426 x 240), and a 1080p screen at 4 (480 x 270)
    and at 2 (960 x 540): the three ends of 'readable at GUI scales 2 to 4'."""
    shots = [('720p, GUI scale 3', on_screen(panel, 426, 240, 3)),
             ('1080p, GUI scale 4', on_screen(panel, 480, 270, 4)),
             ('1080p, GUI scale 2', on_screen(panel, 960, 540, 2))]
    thumb_w = 960
    out = new(thumb_w + 20, sum(int(s.height * thumb_w / s.width) + 26 for _, s in shots) + 10, hexc('#1a1a1e'))
    y = 10
    for label, shot in shots:
        draw_text(out, 10, y, label, TEXT)
        y += 14
        t = shot.resize((thumb_w, int(shot.height * thumb_w / shot.width)), Image.LANCZOS)
        out.alpha_composite(t, (10, y))
        y += t.height + 12
    return out


def paths_sheet():
    """The orrery with each path as primary, and a new player's page: every colour, every icon in
    place, and the locked states. 2x."""
    names = ['vanguard', 'reaver', 'aetherwright', 'bulwark', 'new_player']
    panels = []
    for n in names:
        set_page(n)
        panels.append((n, scaled(drop_shadow(render_orrery()), 2)))
    set_page('vanguard')
    w, h = panels[0][1].width, panels[0][1].height
    sheet = new(w * 2 + 30, (h + 24) * 3 + 10, hexc('#1a1a1e'))
    for i, (n, pnl) in enumerate(panels):
        x, y = 10 + (i % 2) * (w + 10), 10 + (i // 2) * (h + 24)
        draw_text(sheet, x, y, n.replace('_', ' '), TEXT)
        sheet.alpha_composite(pnl, (x, y + 14))
    return sheet


TEXTURES = os.path.join(ROOT, 'modules', 'combat', 'src', 'main', 'resources', 'assets', 'ascension_combat', 'textures')

CHALK = [hexc('#4e4460'), hexc('#aca2bc'), hexc('#d8d2e2'), hexc('#f0ecf6'), hexc('#ffffff')]
AMETHYST = [hexc('#6a3aa8'), hexc('#a070e0')]


def rune_chalk():
    """Rune Chalk, the item: a stick of pale calcite with amethyst ground through it (its recipe),
    lying diagonally the way Minecraft's sticks and rods lie. 16 x 16."""
    s = MaskShape([
        "................",
        "................",
        "...........###..",
        "..........#hh##.",
        ".........#h####.",
        "........#h####..",
        ".......#h####...",
        "......#h####....",
        ".....#h####.....",
        "....#h####......",
        "...#h####.......",
        "..#h####........",
        "..#####.........",
        "...###..........",
        "................",
        "................",
    ])
    img = shade(s, CHALK)
    for (x, y, c) in ((11, 6, 0), (8, 9, 1), (6, 10, 0), (12, 4, 1), (5, 12, 0)):
        img.putpixel((x, y), AMETHYST[c])
    return img


# ---------------------------------------------------------------------------------------------
# The screen's textures (R8): GUI sprites, one PNG per piece, the same drawing the mockups use.
#
# Minecraft's own sprite system finds them: `ascension_combat:rune/cadence` is
# textures/gui/sprites/rune/cadence.png, and a `.png.mcmeta` beside a panel makes it a nine-slice.
# A rune's data file names its sprite in `icon`, so a datapack's new rune brings its own.
# One texel per GUI pixel; nothing is scaled in game.
# ---------------------------------------------------------------------------------------------

GUI = os.path.join(TEXTURES, 'gui', 'sprites')
GUI_OWNED = ['rune', 'path', 'shard', 'socket', 'dial', 'nebula', 'hub', 'card', 'button', 'icon', 'panel.png']

KEY_SOCKET, MINOR_SOCKET, SHARD_SOCKET = 30, 22, 16   # the sockets' sizes: keystone, minor and path tab, shard
KEY_GLYPH, MINOR_GLYPH, SHARD_GLYPH = 20, 14, 8       # and their glyphs'
HUB_PRIMARY, HUB_SECONDARY = 28, 22                   # the hubs' sizes
DIAL_PRIMARY, DIAL_SECONDARY = 180, 108               # the dial frames' sizes
NEBULA_PRIMARY, NEBULA_SECONDARY = 180, 110

WHITE = (255, 255, 255, 255)


def ring_sprite(socket, grow, alpha):
    """A white ring just outside a socket, for hover: tinted or left white in game."""
    size = socket + grow * 2
    img = new(size, size)
    c = size / 2
    circle(img, c, c, socket / 2 + 2.4, with_alpha(WHITE, alpha), 1.0)
    return img


def halo_sprite(socket):
    """A soft white glow round a chosen keystone: drawn tinted in the path's colour, its alpha
    breathing."""
    size = socket + 8
    img = new(size, size)
    glow(img, size / 2, size / 2, size / 2, WHITE, 190)
    return img


def card_sprite(ramp):
    """The hover card's frame, a nine-slice: lit along the top, shaded below, brass at the corners."""
    n = 10
    img = new(n, n)
    rect(img, 0, 0, n, n, with_alpha((6, 9, 20, 255), 244))
    hline(img, 0, n, 0, ramp[3])
    hline(img, 0, n, n - 1, ramp[1])
    vline(img, 0, 0, n, ramp[2])
    vline(img, n - 1, 0, n, ramp[1])
    for (x, y) in ((0, 0), (n - 1, 0), (0, n - 1), (n - 1, n - 1)):
        put(img, x, y, STAR_BRASS[3])
    return img


def button_sprite(fill, border):
    n = 8
    img = new(n, n)
    rect(img, 0, 0, n, n, fill)
    hline(img, 0, n, 0, border)
    hline(img, 0, n, n - 1, border)
    vline(img, 0, 0, n, border)
    vline(img, n - 1, 0, n, border)
    return img


def dial_sprite(size, rim, rows, minor_r, ramp, key_r=0):
    img = new(size, size)
    dial_frame(img, size / 2, size / 2, rim, rows, minor_r, ramp, key_r)
    return img


def nebula_sprite(size, centre, seed, ramp, strength):
    """A path's faint cloud, cropped from where the mockup paints it so it sits the same."""
    panel = new(PANEL_W, PANEL_H)
    half = size // 2
    nebula(panel, centre[0] - half, centre[1] - half, centre[0] + half, centre[1] + half, ramp, strength,
           random.Random(seed))
    return panel.crop((centre[0] - half, centre[1] - half, centre[0] + half, centre[1] + half))


def hub_sprite(size, path, ramp, locked=False):
    img = new(size, size)
    dial_hub(img, size / 2, size / 2, size / 2, path, ramp, locked)
    return img


def sprite_set():
    """Every sprite the screen draws, as (name, image, mcmeta or None), in a fixed order."""
    out = []

    def add(name, img, mcmeta=None):
        out.append((name, img, mcmeta))

    for path in PATH_ORDER:
        ramp = PATHS[path]['ramp']
        for state in ('open', 'chosen'):
            add('socket/%s_key_%s' % (path, state), star_socket(KEY_SOCKET, ramp, state))
            add('socket/%s_world_%s' % (path, state), star_socket(KEY_SOCKET, ramp, state, 'world'))
            add('socket/%s_minor_%s' % (path, state), star_socket(MINOR_SOCKET, ramp, state))
        add('path/%s_small' % path, emblem(path, 'chosen', 14))
        add('path/%s_small_open' % path, emblem(path, 'open', 14))
        add('path/%s_small_barred' % path, emblem(path, 'barred', 14))
        add('hub/%s' % path, hub_sprite(HUB_PRIMARY, path, ramp))
        add('hub/%s_small' % path, hub_sprite(HUB_SECONDARY, path, ramp))
        add('dial/primary_%s' % path, dial_sprite(DIAL_PRIMARY, P_RIM, PRIMARY_ROWS, P_MINOR, ramp, P_KEY))
        add('dial/secondary_%s' % path, dial_sprite(DIAL_SECONDARY, S_RIM, SECONDARY_ROWS, S_MINOR, ramp))
        i = PATH_ORDER.index(path)
        add('nebula/%s' % path, nebula_sprite(NEBULA_PRIMARY, (P_CX, P_CY), 7 + i, ramp, 0.8))
        add('nebula/%s_small' % path, nebula_sprite(NEBULA_SECONDARY, (S_CX, S_CY), 11 + i, ramp, 0.6))
        add('card/%s' % path, card_sprite(ramp),
            {'gui': {'scaling': {'type': 'nine_slice', 'width': 10, 'height': 10, 'border': 2}}})
        for rid, p in RUNE_PATH.items():
            if p == path:
                add('rune/%s' % rid, glyph(rid, ramp, 'chosen'))
                add('rune/%s_open' % rid, glyph(rid, ramp, 'open'))

    add('card/neutral', card_sprite(NEUTRAL),
        {'gui': {'scaling': {'type': 'nine_slice', 'width': 10, 'height': 10, 'border': 2}}})
    for stat in SHARD_MAPS:
        ramp = stat_ramp(stat)
        add('socket/shard_%s_open' % stat, star_socket(SHARD_SOCKET, ramp, 'open'))
        add('socket/shard_%s_chosen' % stat, star_socket(SHARD_SOCKET, ramp, 'chosen'))
        add('shard/%s' % stat, shard_glyph(stat, 'chosen'))
        add('shard/%s_open' % stat, shard_glyph(stat, 'open'))
    add('socket/minor_locked', star_socket(MINOR_SOCKET, NEUTRAL, 'locked'))
    add('socket/key_locked', star_socket(KEY_SOCKET, NEUTRAL, 'locked'))
    add('socket/shard_locked', star_socket(SHARD_SOCKET, NEUTRAL, 'locked'))
    add('socket/hover_key', ring_sprite(KEY_SOCKET, 3, 235))
    add('socket/hover_minor', ring_sprite(MINOR_SOCKET, 3, 235))
    add('socket/hover_shard', ring_sprite(SHARD_SOCKET, 3, 235))
    add('socket/halo_key', halo_sprite(KEY_SOCKET))
    add('dial/secondary_locked', dial_sprite(DIAL_SECONDARY, S_RIM, [], S_MINOR, NEUTRAL))
    # a player who has chosen no path yet: a plain dial and an empty hub
    add('dial/primary_none', dial_sprite(DIAL_PRIMARY, P_RIM, [], P_MINOR, NEUTRAL, P_KEY))
    empty_hub = new(HUB_PRIMARY, HUB_PRIMARY)
    for (x, y) in disc(HUB_PRIMARY / 2, HUB_PRIMARY / 2, HUB_PRIMARY / 2):
        put(empty_hub, x, y, SKY[1])
    circle(empty_hub, HUB_PRIMARY / 2, HUB_PRIMARY / 2, HUB_PRIMARY / 2, STAR_BRASS[1], 1.2)
    add('hub/none', empty_hub)
    empty_small = new(HUB_SECONDARY, HUB_SECONDARY)
    for (x, y) in disc(HUB_SECONDARY / 2, HUB_SECONDARY / 2, HUB_SECONDARY / 2):
        put(empty_small, x, y, SKY[1])
    circle(empty_small, HUB_SECONDARY / 2, HUB_SECONDARY / 2, HUB_SECONDARY / 2, STAR_BRASS[1], 1.2)
    add('hub/none_small', empty_small)
    add('hub/locked', hub_sprite(HUB_SECONDARY, None, NEUTRAL, locked=True))
    add('icon/padlock', padlock(NEUTRAL))
    xp = new(8, 8)
    draw_xp_orb(xp, 0, 0)
    add('icon/xp_orb', xp)
    nine = {'gui': {'scaling': {'type': 'nine_slice', 'width': 8, 'height': 8, 'border': 1}}}
    add('button/normal', button_sprite((10, 14, 28, 255), STAR_BRASS[1]), nine)
    add('button/hover', button_sprite((24, 30, 52, 255), STAR_BRASS[3]), nine)
    add('button/primary', button_sprite((10, 14, 28, 255), STAR_BRASS[2]), nine)
    add('button/primary_hover', button_sprite((28, 34, 56, 255), STAR_BRASS[3]), nine)
    add('button/disabled', button_sprite((8, 10, 18, 255), TEXT_FAINT), nine)
    add('panel', star_panel(random.Random(2203)))
    return out


def export_gui():
    """Writes every sprite under textures/gui/sprites/, replacing what this script wrote before, and
    returns the set for the contact sheet."""
    import json
    import shutil
    for owned in GUI_OWNED:
        target = os.path.join(GUI, owned)
        if os.path.isdir(target):
            shutil.rmtree(target)
        elif os.path.isfile(target):
            os.remove(target)
            if os.path.isfile(target + '.mcmeta'):
                os.remove(target + '.mcmeta')
    sprites = sprite_set()
    for name, img, mcmeta in sprites:
        path = os.path.join(GUI, name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        if mcmeta:
            with open(path + '.mcmeta', 'w', newline='\n') as f:
                json.dump(mcmeta, f, indent=2)
                f.write('\n')
    return sprites


def kit_sheet(sprites):
    """Every sprite, 3x, in groups, on a dark ground, named: the art kit at a glance."""
    groups = {}
    for name, img, _ in sprites:
        if name != 'panel':
            groups.setdefault(name.split('/')[0], []).append((name, img))
    k, pad, width = 3, 8, 1400
    rows, y = [], pad
    for group in ['rune', 'socket', 'path', 'shard', 'hub', 'icon', 'button', 'card', 'dial', 'nebula']:
        items = groups.get(group, [])
        x, line_h, placed = pad, 0, []
        y += 14
        for name, img in items:
            kk = 1 if img.width > 60 else k      # the big dials and clouds at 1x, the small pieces at 3x
            w, h = img.width * kk, img.height * kk
            if x + w + pad > width:
                x, y, line_h = pad, y + line_h + 6, 0
            placed.append((name, img, x, y, kk))
            x += w + pad
            line_h = max(line_h, h)
        rows.append((group, y - 14 if not placed else placed[0][3] - 14, placed))
        y += line_h + 10
    sheet = new(width, y + pad, hexc('#1a1a1e'))
    for group, gy, placed in rows:
        draw_text(sheet, pad, gy, group.upper(), TEXT_DIM)
        for name, img, x, py, kk in placed:
            sheet.alpha_composite(scaled(img, kk), (x, py))
    return sheet


def main():
    os.makedirs(DESIGN, exist_ok=True)
    item_dir = os.path.join(TEXTURES, 'item')
    os.makedirs(item_dir, exist_ok=True)
    rune_chalk().save(os.path.join(item_dir, 'rune_chalk.png'))
    sprites = export_gui()
    for name in os.listdir(DESIGN):
        if name.endswith('.png'):
            os.remove(os.path.join(DESIGN, name))
    set_page('vanguard')
    panel = drop_shadow(render_orrery())
    on_screen(panel, 426, 240, 3).save(os.path.join(DESIGN, 'orrery.png'))
    on_screen(drop_shadow(render_orrery(hover=('conquerors_resolve', 0))), 426, 240, 3).save(
        os.path.join(DESIGN, 'orrery-hover.png'))
    fit_sheet(panel).save(os.path.join(DESIGN, 'orrery-fit-gui-scales.png'))
    paths_sheet().save(os.path.join(DESIGN, 'orrery-every-path.png'))
    icon_sheet().save(os.path.join(DESIGN, 'icons.png'))
    kit_sheet(sprites).save(os.path.join(DESIGN, 'kit.png'))
    print('wrote', DESIGN, 'and', len(sprites), 'sprites under', GUI)


if __name__ == '__main__':
    main()
