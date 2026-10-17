#!/usr/bin/env python3
"""The rune screen's art, and its mockups (docs/technical/rune-system.md section 10).

Owns, for now, only the mockups under design/runes/: four layouts for the rune screen, each drawn
at one texel per GUI pixel and shown at GUI scale 3, the icon contact sheet, and how the panel fits
at GUI scales 2 to 4. Once Sanchit picks a layout, the same script writes the real textures under
assets/ascension_combat/textures/gui/rune/.

The four layouts, all showing the same page (Vanguard with Cadence, Bulwark second):
  pouch    Minecraft's inventory: every rune in a grid, your page a small altar of slots.
  loadout  your character in the middle, rune slots round the body, the choices in a list.
  orrery   round dials: keystones round a hub, minors on an outer ring; the secondary a small dial.
  skymap   all four paths as four constellations in one sky; your page in a sidebar.

Sanchit, 2026-10-16, on the first mockups: "its kinda exact copy of the layout for the runes" --
League's two columns. These layouts are the answer; the art kit (icons, sockets, panels) stays.

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
        if (x, y) in groove:
            c = ramp[1] if ((x - 1, y) in groove or (x, y - 1) in groove) else ramp[0]
        if (x, y) in shine:
            c = ramp[4]
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
    """A pick striking a block of ore, the block cracking: the vein comes with it."""
    s = Shape(20)
    s.stroke([(1.8, 18.2), (9.0, 11.0)], 1.8)
    s.poly(taper([(4.6, 7.4), (8.2, 8.6), (11.2, 11.6), (12.4, 15.4)], [0.3, 1.6, 1.6, 0.3]))
    s.poly([(12.6, 8.6), (19.0, 8.6), (19.0, 15.0), (12.6, 15.0)])
    s.groove([(13.8, 9.8), (15.4, 11.6), (14.6, 13.8)], 0.7)
    s.shine([(16.6, 10.4), (17.2, 10.4)], 1.1)
    s.shine([(16.4, 13.2), (16.8, 13.2)], 1.0)
    s.poly([(14.0, 2.0), (16.2, 1.2), (17.2, 3.4), (15.2, 4.8)])
    s.ellipse(18.6, 5.8, 0.9)
    s.shine([(6.0, 7.8), (8.6, 8.6)], 0.8)
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
    """An open book, a bright orb of experience rising from it."""
    s = Shape(20)
    s.poly([(0.8, 8.4), (5.0, 8.0), (9.4, 10.4), (9.4, 18.6), (5.0, 16.4), (0.8, 16.8)])
    s.poly([(10.6, 10.4), (15.0, 8.0), (19.2, 8.4), (19.2, 16.8), (15.0, 16.4), (10.6, 18.6)])
    s.groove([(2.4, 11.0), (7.8, 12.6)], 0.7)
    s.groove([(2.4, 13.6), (7.8, 15.2)], 0.7)
    s.groove([(12.2, 12.6), (17.6, 11.0)], 0.7)
    s.groove([(12.2, 15.2), (17.6, 13.6)], 0.7)
    s.ellipse(15.6, 3.6, 2.8)
    s.ellipse(11.0, 3.0, 1.1)
    s.ellipse(12.4, 6.4, 0.8)
    s.shine([(14.6, 2.8), (15.0, 2.4)], 1.1)
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
    """A blade swinging again and again: two arcs of its swing behind it."""
    s = Shape(14)
    s.poly([(0.4, 10.8), (3.2, 13.6), (11.6, 5.2), (13.4, 0.6), (8.8, 2.4)])
    s.stroke(arc_pts(1.6, 1.6, 6.8, 6.8, 12, 78, 10), 1.1)
    s.groove([(2.4, 11.6), (10.6, 3.4)], 0.7)
    s.shine([(1.6, 10.6), (9.6, 2.6)], 0.7)
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
    """A spike driven down through an armour plate."""
    s = Shape(14)
    s.poly([(0.8, 7.0), (13.2, 7.0), (13.2, 10.6), (0.8, 10.6)])
    s.cut_poly([(5.6, 5.0), (10.6, 5.0), (5.4, 12.6), (3.0, 12.6)])
    s.poly(taper([(12.6, 0.6), (8.6, 6.2), (4.4, 12.8)], [0.9, 1.3, 0.2]))
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
        return [ramp[0]] + [mix(mix(grey(c), c, 0.35), (20, 22, 28, 255), 0.35) for c in ramp[1:]]
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
# The runes, and the page every mockup shows
# ---------------------------------------------------------------------------------------------

RUNES = {
    'vanguard': {
        'keystones': [('conquerors_resolve', "Conqueror's Resolve"), ('cadence', 'Cadence'),
                      ('vein_breaker', 'Vein Breaker')],
        'rows': [[('second_wind', 'Second Wind'), ('momentum', 'Momentum'), ('blooded', 'Blooded')],
                 [('relentless_strikes', 'Relentless Strikes'), ('killing_surge', 'Killing Surge'),
                  ('giant_slayer', 'Giant Slayer')]],
        'row_names': ['Stay in the fight', 'Damage that grows'],
    },
    'reaver': {
        'keystones': [('ambush', 'Ambush'), ('deadeye', 'Deadeye'), ('silent_step', 'Silent Step')],
        'rows': [[('pounce', 'Pounce'), ('drop_in', 'Drop In'), ('vanish', 'Vanish')],
                 [('finishing_blow', 'Finishing Blow'), ('overkill', 'Overkill'),
                  ('trophy_hunter', 'Trophy Hunter')]],
        'row_names': ['How you open', 'How you finish'],
    },
    'aetherwright': {
        'keystones': [('starfall', 'Starfall'), ('arc', 'Arc'), ('scholar', 'Scholar')],
        'rows': [[('attunement', 'Attunement'), ('unweave', 'Unweave'), ('clarity', 'Clarity')],
                 [('siphon', 'Siphon'), ('quickening', 'Quickening'), ('gathering_charge', 'Gathering Charge')]],
        'row_names': ['Magic that hits harder', 'Magic that gives back'],
    },
    'bulwark': {
        'keystones': [('brace', 'Brace'), ('pack_leader', 'Pack Leader'), ('cats_eye', "Cat's Eye")],
        'rows': [[('plating', 'Plating'), ('unbowed', 'Unbowed'), ('deep_roots', 'Deep Roots')],
                 [('riposte', 'Riposte'), ('revitalize', 'Revitalize'), ('challenge', 'Challenge')]],
        'row_names': ['Take a hit', 'Give it back'],
    },
}

SHARD_ROWS = [('Offence', ['ad', 'as', 'ms']), ('Flex', ['ad', 'armor', 'mr']), ('Defence', ['hp', 'armor', 'mr'])]
SHARD_SHORT = {'ad': '+1 AD', 'as': '+8% AS', 'ms': '+3% MS', 'armor': '+5 Armor', 'mr': '+5 MR', 'hp': '+2 HP'}

PAGE = {
    'primary': 'vanguard', 'secondary': 'bulwark',
    'keystone': 1,                      # Cadence
    'primary_minors': [2, 2],           # Blooded (growing, 9 of 12), Giant Slayer
    'secondary_minors': [0, 0],         # Plating, Riposte
    'shards': [1, 1, None],             # Attack Speed, Armor; the defence row still locked
    'shards_locked': [False, False, True],
    'growth': {'blooded': (9, 12)},
}

BLURB = {
    'conquerors_resolve': 'Every second in a fight: more Attack Damage and healing, up to 10 stacks.',
    'cadence': 'Three charged hits on one enemy: the third hits hard and Exposes it.',
    'vein_breaker': 'Sneak and mine: the connected ore or logs break with it.',
}


def pr():
    return PATHS[PAGE['primary']]['ramp']


def sr():
    return PATHS[PAGE['secondary']]['ramp']


def chosen_keystone():
    return RUNES[PAGE['primary']]['keystones'][PAGE['keystone']]


def rune_state(path, kind, row, i):
    """What a rune looks like on this page: chosen, open (could be taken) or barred (not by the
    rules: a keystone of the secondary path)."""
    if path == PAGE['primary']:
        if kind == 'key':
            return 'chosen' if i == PAGE['keystone'] else 'open'
        return 'chosen' if i == PAGE['primary_minors'][row] else 'open'
    if path == PAGE['secondary']:
        if kind == 'key':
            return 'barred'
        return 'chosen' if i == PAGE['secondary_minors'][row] else 'open'
    return 'open'


# ---------------------------------------------------------------------------------------------
# The art kit: iron (Minecraft-like plates and slots) and sky (night, brass, starlight)
# ---------------------------------------------------------------------------------------------

PANEL_W, PANEL_H = 400, 224

IRON = [hexc('#0b0c0f'), hexc('#1a1c21'), hexc('#24272d'), hexc('#30343b'), hexc('#454a53'),
        hexc('#666c77'), hexc('#959ca8')]
STAR_BRASS = [hexc('#3a3020'), hexc('#7a6840'), hexc('#c8b07a'), hexc('#f0e0b0')]
SKY = [hexc('#04060c'), hexc('#080c18'), hexc('#0e1426')]


def bevel(img, x0, y0, x1, y1, light, dark):
    hline(img, x0, x1, y0, light)
    vline(img, x0, y0, y1, light)
    hline(img, x0, x1, y1 - 1, dark)
    vline(img, x1 - 1, y0, y1, dark)


def iron_panel(rng):
    img = new(PANEL_W, PANEL_H)
    rect(img, 0, 0, PANEL_W, PANEL_H, IRON[0])
    rect(img, 1, 1, PANEL_W - 1, PANEL_H - 1, IRON[2])
    for y in range(2, PANEL_H - 2):
        for x in range(2, PANEL_W - 2):
            n = rng.random()
            if n < 0.06:
                put(img, x, y, with_alpha(IRON[3], 120))
            elif n < 0.09:
                put(img, x, y, with_alpha(IRON[1], 120))
    bevel(img, 1, 1, PANEL_W - 1, PANEL_H - 1, IRON[5], IRON[1])
    bevel(img, 2, 2, PANEL_W - 2, PANEL_H - 2, IRON[4], IRON[1])
    rect(img, 4, 4, PANEL_W - 4, 18, IRON[1])
    bevel(img, 4, 4, PANEL_W - 4, 18, IRON[0], IRON[3])
    for (x, y) in ((6, 6), (PANEL_W - 8, 6), (6, PANEL_H - 8), (PANEL_W - 8, PANEL_H - 8)):
        put(img, x, y, IRON[6])
        put(img, x + 1, y, IRON[5])
        put(img, x, y + 1, IRON[4])
        put(img, x + 1, y + 1, IRON[1])
    return img


def iron_inset(img, x0, y0, x1, y1, tint=None, strength=0.0):
    """A recessed plate: shadow on its top-left edge, light on its bottom-right (Minecraft's
    inventory slots read the same way)."""
    rect(img, x0, y0, x1, y1, IRON[1])
    if tint:
        for yy in range(y0 + 1, y1 - 1):
            t = 1 - (yy - y0) / (y1 - y0)
            rect(img, x0 + 1, yy, x1 - 1, yy + 1, with_alpha(tint[1], 110 * strength * t * t + 8))
    bevel(img, x0, y0, x1, y1, IRON[0], IRON[4])


def iron_socket(size, ramp, state, shape='round'):
    """A metal ring, the rune's glyph inside. Chosen: a lit ring in the path's colour. World
    keystones get a cut-cornered socket instead of a round one, so they stand apart."""
    img = new(size, size)
    r = size / 2
    outer = disc(r, r, r - 0.2)
    inner = disc(r, r, r - 2.6)
    if shape == 'world':
        def inside(x, y, m):
            dx, dy = abs(x + 0.5 - r), abs(y + 0.5 - r)
            return dx <= r - m and dy <= r - m and dx + dy <= r * 1.32 - m
        outer = {(x, y) for x in range(size) for y in range(size) if inside(x, y, 0.2)}
        inner = {(x, y) for x in range(size) for y in range(size) if inside(x, y, 2.6)}
    ring_l, ring_d = (ramp[3], ramp[1]) if state == 'chosen' else (IRON[5], IRON[1])
    if state in ('locked', 'barred'):
        ring_l, ring_d = IRON[4], IRON[0]
    for (x, y) in outer - inner:
        up = (x + 0.5 - r) + (y + 0.5 - r) < 0
        put(img, x, y, ring_l if up else ring_d)
    for (x, y) in inner:
        d = math.hypot(x + 0.5 - r, y + 0.5 - r) / r
        base = mix(IRON[0], IRON[2], 0.3 + 0.5 * d)
        if state == 'chosen':
            base = mix(base, ramp[1], 0.45 * (1 - d))
        put(img, x, y, base)
    for y in range(size):
        for x in range(size):
            if (x, y) not in outer and any((x + dx, y + dy) in outer for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                put(img, x, y, IRON[0])
    return img


def iron_slot(size, ramp, state):
    """A square inventory slot. Chosen ("on your page"): a lit border and a corner pip."""
    img = new(size, size)
    rect(img, 0, 0, size, size, IRON[1] if state != 'barred' else mix(IRON[1], IRON[0], 0.5))
    bevel(img, 0, 0, size, size, IRON[0], IRON[5])
    if state == 'chosen':
        rect(img, 1, 1, size - 1, size - 1, with_alpha(ramp[1], 120))
        bevel(img, 1, 1, size - 1, size - 1, ramp[3], ramp[2])
        put(img, size - 3, 2, ramp[4])
        put(img, size - 4, 2, ramp[3])
        put(img, size - 3, 3, ramp[3])
    return img


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
            ox, oy = polar(r, r - 1, 1, i)
            ox, oy = r + (r + 1.5) * math.cos(math.radians(i)), r + (r * 0.34) * math.sin(math.radians(i)) - 1
            if math.sin(math.radians(i)) < 0 and abs(ox - r) < r - 2:
                continue
            put(img, int(ox), int(oy), ramp[3] if state == 'chosen' else (130, 140, 160, 170))
    return img


def place_rune(img, kit, rid, ramp, state, x, y, size, world=False, glyph_state=None):
    """A socket and its glyph, centred in a size x size box at (x, y)."""
    sock = (iron_socket if kit == 'iron' else star_socket)(size, ramp, state, 'world' if world else 'round')
    blit(img, sock, x, y)
    g = glyph(rid, ramp, glyph_state or state)
    blit(img, g, x + (size - g.width) // 2, y + (size - g.height) // 2)


def ring_arc(img, cx, cy, rad, frac, colour, back):
    """A growth ring: `frac` of a circle, clockwise from the top."""
    for (x, y) in disc(cx, cy, rad) - disc(cx, cy, rad - 1.2):
        a = (math.degrees(math.atan2(x + 0.5 - cx, -(y + 0.5 - cy))) + 360) % 360
        put(img, x, y, colour if a <= frac * 360 else back)


def draw_button(img, kit, x, y, w, h, label, primary):
    if kit == 'iron':
        top, mid, bot = (IRON[5], IRON[4], IRON[1]) if primary else (IRON[4], IRON[3], IRON[1])
        rect(img, x, y, x + w, y + h, mid)
        bevel(img, x, y, x + w, y + h, top, bot)
        rect(img, x - 1, y - 1, x + w + 1, y, IRON[0])
    else:
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


def title_bar(img, kit, text='Runes'):
    """The title on the left; Save and Reset on the right, the cost of this save beside them."""
    draw_text(img, 9, 7, text, STAR_BRASS[3] if kit == 'sky' else TEXT, bold=True)
    bx = PANEL_W - 8
    for label, primary in (('Save', True), ('Reset', False)):
        w = text_width(label) + 14
        bx -= w
        draw_button(img, kit, bx, 5, w, 12, label, primary)
        bx -= 4
    cx = bx - 64
    draw_chalk(img, cx, 7)
    draw_text(img, cx + 10, 7, '×1', TEXT_BODY)
    draw_xp_orb(img, cx + 26, 7)
    draw_text(img, cx + 36, 7, '3 lv', STAT['xp'])


def blurb_box(img, kit, x0, y0, x1, y1):
    """The chosen keystone, in words."""
    if kit == 'iron':
        rect(img, x0, y0, x1, y1, with_alpha(IRON[0], 150))
        bevel(img, x0, y0, x1, y1, IRON[0], IRON[4])
    else:
        hline(img, x0, x1, y0, with_alpha(STAR_BRASS[1], 160))
        rect(img, x0, y0 + 1, x1, y1, with_alpha((4, 6, 14, 255), 120))
    rid, name = chosen_keystone()
    draw_text(img, x0 + 5, y0 + 5, name, pr()[4], bold=True)
    draw_text(img, x0 + 9 + text_width(name, True), y0 + 5, '· keystone', TEXT_FAINT)
    for i, ln in enumerate(wrap(BLURB[rid], x1 - x0 - 10)):
        if y0 + 17 + i * 10 + 8 <= y1:
            draw_text(img, x0 + 5, y0 + 17 + i * 10, ln, TEXT_BODY)


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
# Layout 1: the rune pouch. Minecraft's inventory: all runes in a grid, your page a small altar.
# ---------------------------------------------------------------------------------------------

def render_pouch():
    rng = random.Random(1201)
    img = iron_panel(rng)
    title_bar(img, 'iron')
    p, s = PAGE['primary'], PAGE['secondary']

    # --- the altar: your page, as slots in a shape: one keystone, two, two, three shards
    ax0, ay0, ax1, ay1 = 8, 22, 152, 160
    iron_inset(img, ax0, ay0, ax1, ay1, pr(), 0.9)
    draw_text(img, ax0 + 5, ay0 + 4, 'YOUR PAGE', TEXT_DIM)
    cx = (ax0 + ax1) // 2
    key = 30
    kx, ky = cx - key // 2, ay0 + 16
    m1y = ky + key + 8
    m2y = m1y + 22 + 16
    shy = m2y + 22 + 7
    mx = [cx - 26 - 11, cx + 26 - 11]
    shx = [cx - 24 - 7, cx - 7, cx + 24 - 7]
    # carved lines joining the primary part: keystone to its two minors
    for x in mx:
        line(img, cx, ky + key // 2, x + 11, m1y + 11, IRON[0])
        line(img, cx + 1, ky + key // 2, x + 12, m1y + 11, with_alpha(pr()[2], 160))
    rid, name = chosen_keystone()
    place_rune(img, 'iron', rid, pr(), 'chosen', kx, ky, key)
    em = emblem(p, 'chosen', 14)
    blit(img, em, kx - 20, ky + 8)
    for r in range(2):
        rid, name = RUNES[p]['rows'][r][PAGE['primary_minors'][r]]
        place_rune(img, 'iron', rid, pr(), 'chosen', mx[r], m1y, 22)
        if rid in PAGE['growth']:
            n, cap = PAGE['growth'][rid]
            ring_arc(img, mx[r] + 11, m1y + 11, 13.4, n / cap, pr()[4], with_alpha(pr()[0], 200))
    # the secondary part, under a rule
    hline(img, ax0 + 10, ax1 - 10, m2y - 9, IRON[0])
    hline(img, ax0 + 10, ax1 - 10, m2y - 8, IRON[3])
    blit(img, emblem(s, 'chosen', 14), cx - 7, m2y - 15)
    for r in range(2):
        rid, name = RUNES[s]['rows'][r][PAGE['secondary_minors'][r]]
        place_rune(img, 'iron', rid, sr(), 'chosen', mx[r], m2y, 22)
    for r in range(3):
        st = SHARD_ROWS[r][1][PAGE['shards'][r]] if PAGE['shards'][r] is not None else None
        locked = PAGE['shards_locked'][r]
        sock = iron_socket(14, stat_ramp(st) if st else NEUTRAL, 'locked' if locked else 'chosen')
        blit(img, sock, shx[r], shy)
        if locked:
            blit(img, padlock(NEUTRAL), shx[r] + 4, shy + 3)
        else:
            blit(img, shard_glyph(st), shx[r] + 3, shy + 3)

    # the held rune's target: the slot it can drop into pulses
    circle(img, mx[0] + 11, m1y + 11, 15.6, with_alpha(TEXT, 220), 1.0, dash=30)

    blurb_box(img, 'iron', ax0, ay1 + 4, ax1, PANEL_H - 8)

    # --- the pouch: every rune, one row per path
    px0, py0, px1, py1 = 158, 22, PANEL_W - 8, PANEL_H - 8
    iron_inset(img, px0, py0, px1, py1)
    draw_text(img, px0 + 5, py0 + 4, 'RUNE POUCH', TEXT_DIM)
    draw_text(img, px0 + 70, py0 + 4, 'pick one up, set it on your page', TEXT_FAINT)
    col_k = [px0 + 28, px0 + 52, px0 + 82]
    col_a = [px0 + 110, px0 + 129, px0 + 148]
    col_b = [px0 + 174, px0 + 193, px0 + 212]
    hy = py0 + 16
    draw_centred(img, col_k[0] + 37, hy, 'keystones', TEXT_FAINT)
    draw_centred(img, col_a[1] + 9, hy, 'row 1', TEXT_FAINT)
    draw_centred(img, col_b[1] + 9, hy, 'row 2', TEXT_FAINT)
    held = ('vanguard', 0, 1)
    for i, path in enumerate(PATH_ORDER):
        ramp = PATHS[path]['ramp']
        y = py0 + 28 + i * 27
        blit(img, emblem(path, 'chosen' if path in (p, s) else 'open'), px0 + 4, y + 1)
        for k, (rid, name) in enumerate(RUNES[path]['keystones']):
            st = rune_state(path, 'key', 0, k)
            blit(img, iron_slot(22, ramp, st), col_k[k], y)
            g = glyph(rid, ramp, 'chosen' if st == 'chosen' else st if st == 'barred' else 'open')
            blit(img, g, col_k[k] + 1, y + 1)
        vline(img, col_k[2] - 4, y + 3, y + 19, IRON[0])
        for r, cols in ((0, col_a), (1, col_b)):
            for k, (rid, name) in enumerate(RUNES[path]['rows'][r]):
                st = rune_state(path, 'min', r, k)
                blit(img, iron_slot(18, ramp, st), cols[k], y + 2)
                if (path, r, k) == held:
                    continue
                blit(img, glyph(rid, ramp, 'chosen' if st == 'chosen' else 'open'), cols[k] + 2, y + 4)
    # shards
    sy = py0 + 28 + 4 * 27 + 4
    draw_text(img, px0 + 5, sy + 4, 'Shards', TEXT_DIM)
    x = px0 + 44
    for r, (label, stats) in enumerate(SHARD_ROWS):
        for k, st in enumerate(stats):
            locked = PAGE['shards_locked'][r]
            chosen = PAGE['shards'][r] == k
            state = 'chosen' if chosen else 'open'
            blit(img, iron_slot(16, stat_ramp(st), 'barred' if locked else state), x, sy)
            if locked:
                blit(img, padlock(NEUTRAL), x + 4, sy + 3)
            else:
                blit(img, shard_glyph(st, state), x + 4, sy + 4)
            x += 18
        draw_centred(img, x - 28, sy + 19, label, TEXT_FAINT)
        x += 8
    draw_text(img, px0 + 5, sy + 32, 'Defence opens at your 4th milestone.', TEXT_FAINT)

    # the rune in hand: Momentum, picked up and on its way to the lit slot
    hx, hy2 = 120, 76
    blit(img, glyph('momentum', pr()), hx, hy2)
    cursor(img, hx + 9, hy2 + 9)
    return img


# ---------------------------------------------------------------------------------------------
# Layout 2: the loadout. You in the middle, rune slots round you, the choices in a list.
# ---------------------------------------------------------------------------------------------

def figure(img, cx, top):
    """A plain standing figure in the panel's iron tones: a stand-in for the player's own model,
    which the game draws here in the player's skin and armour."""
    parts = [(cx - 12, top, 24, 24), (cx - 12, top + 24, 24, 36), (cx - 24, top + 24, 12, 36),
             (cx + 12, top + 24, 12, 36), (cx - 12, top + 60, 12, 36), (cx, top + 60, 12, 36)]
    for (x, y, w, h) in parts:
        rect(img, x, y, x + w, y + h, IRON[3])
        vline(img, x, y, y + h, IRON[4])
        hline(img, x, x + w, y, IRON[4])
        vline(img, x + w - 1, y, y + h, IRON[2])
        hline(img, x, x + w, y + h - 1, IRON[2])
    for (x, y, w, h) in parts:
        hline(img, x - 1, x + w + 1, y - 1, with_alpha(IRON[0], 200))
        hline(img, x - 1, x + w + 1, y + h, with_alpha(IRON[0], 200))
        vline(img, x - 1, y, y + h, with_alpha(IRON[0], 200))
        vline(img, x + w, y, y + h, with_alpha(IRON[0], 200))
    for (x, y) in disc(cx, top + 97, 22):
        if y >= top + 96 and abs(y - top - 97) <= 3:
            put(img, x, y, with_alpha(IRON[0], 120))


def render_loadout():
    rng = random.Random(1202)
    img = iron_panel(rng)
    title_bar(img, 'iron')
    p, s = PAGE['primary'], PAGE['secondary']

    # --- you, and your page round you
    lx0, ly0, lx1, ly1 = 8, 22, 206, PANEL_H - 8
    iron_inset(img, lx0, ly0, lx1, ly1)
    cx, top = (lx0 + lx1) // 2, 46
    figure(img, cx, top)
    # path badges over each side
    for side, path, x in (('PRIMARY', p, lx0 + 6), ('SECONDARY', s, lx1 - 66)):
        ramp = PATHS[path]['ramp']
        draw_text(img, x, ly0 + 4, side, TEXT_DIM)
        blit(img, emblem(path, 'chosen', 14), x, ly0 + 14)
        draw_text(img, x + 17, ly0 + 17, PATHS[path]['name'], ramp[3], bold=True)
    # keystone on the chest, the heart of the build
    kx, ky = cx - 15, top + 30
    glow(img, cx, ky + 15, 22, pr()[3], 70)
    rid, name = chosen_keystone()
    place_rune(img, 'iron', rid, pr(), 'chosen', kx, ky, 30)
    circle(img, cx, ky + 15, 17.6, with_alpha(TEXT, 220), 1.0, dash=30)
    # primary minors by the left hand, secondary by the right
    for side, path, ramp, picks, sx, hand_x in (
            ('l', p, pr(), PAGE['primary_minors'], lx0 + 18, cx - 24),
            ('r', s, sr(), PAGE['secondary_minors'], lx1 - 40, cx + 24)):
        for r in range(2):
            y = top + 22 + r * 40
            rid, name = RUNES[path]['rows'][r][picks[r]]
            ex = sx + 22 if side == 'l' else sx
            line(img, ex, y + 11, hand_x, top + 40 + r * 14, with_alpha(ramp[2], 200))
            place_rune(img, 'iron', rid, ramp, 'chosen', sx, y, 22)
            if rid in PAGE['growth']:
                n, cap = PAGE['growth'][rid]
                ring_arc(img, sx + 11, y + 11, 13.4, n / cap, ramp[4], with_alpha(ramp[0], 200))
            label = name + (' %d/%d' % PAGE['growth'][rid] if rid in PAGE['growth'] else '')
            draw_centred(img, sx + 11, y + 24, label, ramp[3])
    # shards at the feet
    base = top + 104
    for r in range(3):
        x = cx - 31 + r * 24
        st = SHARD_ROWS[r][1][PAGE['shards'][r]] if PAGE['shards'][r] is not None else None
        locked = PAGE['shards_locked'][r]
        blit(img, iron_socket(14, stat_ramp(st) if st else NEUTRAL, 'locked' if locked else 'chosen'), x, base)
        if locked:
            blit(img, padlock(NEUTRAL), x + 4, base + 3)
        else:
            blit(img, shard_glyph(st), x + 3, base + 3)
    draw_centred(img, cx, base + 17, 'shards', TEXT_FAINT)

    # --- the list: the choices for the slot you clicked (the keystone)
    rx0, ry0, rx1, ry1 = 212, 22, PANEL_W - 8, PANEL_H - 8
    iron_inset(img, rx0, ry0, rx1, ry1, pr(), 0.7)
    draw_text(img, rx0 + 6, ry0 + 4, 'KEYSTONE', TEXT_DIM)
    draw_text(img, rx0 + 6 + text_width('KEYSTONE') + 6, ry0 + 4, 'choose one', TEXT_FAINT)
    for i, path in enumerate(PATH_ORDER):
        x = rx0 + 6 + i * 24
        blit(img, iron_socket(22, PATHS[path]['ramp'], 'chosen' if path == p else 'open'), x, ry0 + 15)
        blit(img, emblem(path, 'chosen' if path == p else 'open', 14), x + 4, ry0 + 19)
    draw_text(img, rx0 + 104, ry0 + 22, PATHS[p]['promise'], TEXT_BODY)
    y = ry0 + 44
    for k, (rid, name) in enumerate(RUNES[p]['keystones']):
        chosen = k == PAGE['keystone']
        world = k == 2
        h = 48
        if world:
            hline(img, rx0 + 8, rx1 - 8, y - 3, IRON[0])
            hline(img, rx0 + 8, rx1 - 8, y - 2, IRON[3])
            y += 2
        if chosen:
            rect(img, rx0 + 3, y, rx1 - 3, y + h - 2, with_alpha(pr()[1], 90))
            bevel(img, rx0 + 3, y, rx1 - 3, y + h - 2, pr()[3], pr()[1])
        place_rune(img, 'iron', rid, pr(), 'chosen' if chosen else 'open', rx0 + 8, y + 7, 30, world=world)
        tx = rx0 + 44
        draw_text(img, tx, y + 5, name, pr()[4] if chosen else TEXT_BODY, bold=chosen)
        if world:
            draw_text(img, tx + text_width(name, chosen) + 5, y + 5, 'world', TEXT_FAINT)
        for j, ln in enumerate(wrap(BLURB[rid], rx1 - tx - 6)[:3]):
            draw_text(img, tx, y + 16 + j * 10, ln, TEXT_DIM if not chosen else TEXT_BODY)
        y += h
    return img


# ---------------------------------------------------------------------------------------------
# Layout 3: the orrery. Round dials: keystones round a hub, minors on an outer ring.
# ---------------------------------------------------------------------------------------------

def dial(img, cx, cy, path, hub_r, key_r, minor_r, rim_r, keystones=True, minor_angles=None, key_size=30):
    """One path as a dial. Returns nothing; draws rings, spokes, sockets."""
    ramp = PATHS[path]['ramp']
    primary = path == PAGE['primary']
    picks = PAGE['primary_minors'] if primary else PAGE['secondary_minors']
    # the rim, with ticks like an astrolabe's
    circle(img, cx, cy, rim_r, STAR_BRASS[1], 1.6)
    circle(img, cx, cy, rim_r - 3, with_alpha(STAR_BRASS[1], 120))
    for a in range(0, 360, 15):
        x0, y0 = polar(cx, cy, rim_r - 3, a)
        x1, y1 = polar(cx, cy, rim_r - (6 if a % 45 else 8), a)
        line(img, x0, y0, x1, y1, with_alpha(STAR_BRASS[2], 160))
    # the rows as two faint arcs on the minor ring
    angles = minor_angles or [[150, 180, 210], [330, 0, 30]]
    for row in angles:
        a0, a1 = row[0] - 12, row[-1] + 12
        if a0 > a1:
            a1 += 360
        circle(img, cx, cy, minor_r + 0.5, with_alpha(ramp[2], 110), 1.0, arc=(a0 % 360, a1 % 360))
    if keystones:
        circle(img, cx, cy, key_r + 0.5, with_alpha(STAR_BRASS[1], 90), 1.0, dash=10)
    # spokes from the hub to what is chosen
    targets = []
    if keystones:
        ka = [210, 330, 90][PAGE['keystone']]
        targets.append(polar(cx, cy, key_r, ka))
    for r in range(2):
        targets.append(polar(cx, cy, minor_r, angles[r][picks[r]]))
    for (tx, ty) in targets:
        line(img, cx, cy, tx, ty, with_alpha(ramp[3], 200))
        line(img, cx + 1, cy, tx + 1, ty, with_alpha(ramp[2], 90))
    # the hub
    for (x, y) in disc(cx, cy, hub_r):
        put(img, x, y, mix(SKY[1], ramp[1], 0.45))
    circle(img, cx, cy, hub_r, STAR_BRASS[2], 1.2)
    em = emblem(path, 'chosen', 20 if hub_r >= 14 else 14)
    blit(img, em, int(cx - em.width / 2), int(cy - em.height / 2))
    # keystones on the inner ring: two for combat above, the world one set below
    if keystones:
        for k, (rid, name) in enumerate(RUNES[path]['keystones']):
            x, y = polar(cx, cy, key_r, [210, 330, 90][k])
            st = 'chosen' if k == PAGE['keystone'] else 'open'
            place_rune(img, 'sky', rid, ramp, st, int(x - key_size / 2), int(y - key_size / 2), key_size, world=k == 2)
    for r in range(2):
        for k, (rid, name) in enumerate(RUNES[path]['rows'][r]):
            x, y = polar(cx, cy, minor_r, angles[r][k])
            st = 'chosen' if k == picks[r] else 'open'
            place_rune(img, 'sky', rid, ramp, st, int(x - 11), int(y - 11), 22)
            if st == 'chosen' and rid in PAGE['growth']:
                n, cap = PAGE['growth'][rid]
                ring_arc(img, int(x - 11) + 11, int(y - 11) + 11, 13.4, n / cap, ramp[4], with_alpha(ramp[0], 200))


def render_orrery():
    rng = random.Random(2203)
    img = star_panel(rng)
    title_bar(img, 'sky')
    p, s = PAGE['primary'], PAGE['secondary']
    pcx, pcy = 98, 122
    nebula(img, pcx - 90, pcy - 90, pcx + 90, pcy + 90, pr(), 0.8, random.Random(7))
    dial(img, pcx, pcy, p, 14, 37, 70, 88)
    # the four paths sit in the dial's corners; turning to one turns the dial
    for (path, (x, y)) in zip(PATH_ORDER, ((10, 24), (164, 24), (10, 198), (164, 198))):
        st = 'chosen' if path == p else 'barred' if path == s else 'open'
        blit(img, star_socket(22, PATHS[path]['ramp'], 'chosen' if path == p else 'open'), x, y)
        blit(img, emblem(path, 'chosen' if path == p else 'open', 14), x + 4, y + 4)
    draw_centred(img, pcx, 210, PATHS[p]['name'].upper(), pr()[3], bold=True)

    # the secondary path: a smaller dial, minors only
    scx, scy = 248, 76
    nebula(img, scx - 55, scy - 55, scx + 55, scy + 55, sr(), 0.6, random.Random(11))
    dial(img, scx, scy, s, 11, 0, 36, 52, keystones=False, minor_angles=[[140, 180, 220], [320, 0, 40]])
    tx = 310
    draw_text(img, tx, 26, 'SECONDARY', TEXT_DIM)
    draw_text(img, tx, 36, PATHS[s]['name'].upper(), sr()[3], bold=True)
    others = [q for q in PATH_ORDER if q != p]
    for i, path in enumerate(others):
        x, y = tx + i * 24, 50
        blit(img, star_socket(20, PATHS[path]['ramp'], 'chosen' if path == s else 'open'), x, y)
        blit(img, emblem(path, 'chosen' if path == s else 'open', 14), x + 3, y + 3)
    draw_text(img, tx, 78, PATHS[s]['promise'], TEXT_BODY)
    draw_text(img, tx, 92, 'one from each', TEXT_FAINT)
    draw_text(img, tx, 101, 'half', TEXT_FAINT)

    # shards: three little moons each
    sy = 134
    draw_text(img, 202, sy, 'SHARDS', TEXT_DIM)
    for r, (label, stats) in enumerate(SHARD_ROWS):
        x0 = 202 + r * 64
        for k, st in enumerate(stats):
            locked = PAGE['shards_locked'][r]
            chosen = PAGE['shards'][r] == k
            state = 'locked' if locked else ('chosen' if chosen else 'open')
            blit(img, star_socket(16, stat_ramp(st), state), x0 + k * 17, sy + 11)
            if locked:
                blit(img, padlock(NEUTRAL), x0 + k * 17 + 4, sy + 15)
            else:
                blit(img, shard_glyph(st, 'chosen' if chosen else 'open'), x0 + k * 17 + 4, sy + 15)
        draw_centred(img, x0 + 25, sy + 29, label, TEXT_FAINT)

    blurb_box(img, 'sky', 198, 176, PANEL_W - 8, PANEL_H - 6)
    return img


# ---------------------------------------------------------------------------------------------
# Layout 4: the sky map. Every path a constellation in one sky; your page in a sidebar.
# ---------------------------------------------------------------------------------------------

# Each path's stars, in its own quarter of the sky (144 x 100): three keystones (k0, k1 and the
# world one, k2) and two arms of three (a: row 1, b: row 2). The figures echo the emblems: a
# spear's head and its two tails, a crescent, a comet's tail, a ring round a shield.
CONSTELLATIONS = {
    'vanguard': {
        'origin': (6, 22), 'label': (72, 90, 'c'),
        'k': [(44, 22), (72, 14), (114, 20)],
        'a': [(54, 48), (40, 67), (26, 86)], 'b': [(90, 48), (104, 67), (118, 86)],
        'edges': [('k0', 'k1'), ('k0', 'a0'), ('a0', 'a1'), ('a1', 'a2'), ('k1', 'b0'), ('b0', 'b1'), ('b1', 'b2')],
        'world_edge': ('k1', 'k2'),
    },
    'reaver': {
        'origin': (150, 22), 'label': (36, 46, 'l'),
        'k': [(100, 22), (118, 48), (122, 82)],
        'a': [(74, 12), (48, 12), (24, 26)], 'b': [(76, 86), (50, 86), (24, 72)],
        'edges': [('k0', 'k1'), ('k0', 'a0'), ('a0', 'a1'), ('a1', 'a2'), ('a2', 'b2'), ('k1', 'b0'), ('b0', 'b1'), ('b1', 'b2')],
        'world_edge': ('k1', 'k2'),
    },
    'aetherwright': {
        'origin': (6, 122), 'label': (142, 90, 'r'),
        'k': [(112, 22), (86, 28), (128, 52)],
        'a': [(64, 36), (42, 46), (20, 52)], 'b': [(78, 60), (58, 76), (34, 84)],
        'edges': [('k0', 'k1'), ('k1', 'a0'), ('a0', 'a1'), ('a1', 'a2'), ('k1', 'b0'), ('b0', 'b1'), ('b1', 'b2')],
        'world_edge': ('k0', 'k2'),
    },
    'bulwark': {
        'origin': (150, 122), 'label': (72, 2, 'c'),
        'k': [(58, 42), (86, 42), (72, 70)],
        'a': [(36, 20), (18, 50), (36, 80)], 'b': [(108, 20), (126, 50), (108, 80)],
        'edges': [('k0', 'k1'), ('a0', 'a1'), ('a1', 'a2'), ('a0', 'b0'), ('a2', 'b2'), ('b0', 'b1'), ('b1', 'b2'),
                  ('k0', 'a1'), ('k1', 'b1')],
        'world_edge': ('k0', 'k2'),
    },
}


def render_skymap():
    rng = random.Random(2204)
    img = star_panel(rng)
    title_bar(img, 'sky')
    p, s = PAGE['primary'], PAGE['secondary']
    for path, seed in zip(PATH_ORDER, (7, 9, 13, 11)):
        c = CONSTELLATIONS[path]
        ox, oy = c['origin']
        strength = 1.0 if path == p else 0.7 if path == s else 0.35
        nebula(img, ox, oy, ox + 144, oy + 100, PATHS[path]['ramp'], strength, random.Random(seed), 5)
    vline(img, 297, 22, PANEL_H - 6, with_alpha(STAR_BRASS[1], 140))

    for path in PATH_ORDER:
        c = CONSTELLATIONS[path]
        ramp = PATHS[path]['ramp']
        ox, oy = c['origin']
        pos = {'k%d' % i: (ox + x, oy + y) for i, (x, y) in enumerate(c['k'])}
        pos.update({'a%d' % i: (ox + x, oy + y) for i, (x, y) in enumerate(c['a'])})
        pos.update({'b%d' % i: (ox + x, oy + y) for i, (x, y) in enumerate(c['b'])})
        faint = with_alpha(mix(ramp[2], (150, 160, 190, 255), 0.5), 90)
        for (u, v) in c['edges']:
            line(img, *pos[u], *pos[v], faint)
        line(img, *pos[c['world_edge'][0]], *pos[c['world_edge'][1]], faint, dash=2)
        # the page's own lines, lit
        if path == p:
            ka = pos['k%d' % PAGE['keystone']]
            for r, arm in ((0, 'a'), (1, 'b')):
                t = pos['%s%d' % (arm, PAGE['primary_minors'][r])]
                line(img, *ka, *t, with_alpha(ramp[4], 230))
                line(img, ka[0] + 1, ka[1], t[0] + 1, t[1], with_alpha(ramp[3], 110))
        if path == s:
            a = pos['a%d' % PAGE['secondary_minors'][0]]
            b = pos['b%d' % PAGE['secondary_minors'][1]]
            line(img, *a, *b, with_alpha(ramp[4], 230))
            line(img, a[0] + 1, a[1], b[0] + 1, b[1], with_alpha(ramp[3], 110))
        # the stars
        for k, (rid, name) in enumerate(RUNES[path]['keystones']):
            x, y = pos['k%d' % k]
            st = rune_state(path, 'key', 0, k)
            place_rune(img, 'sky', rid, ramp, st, x - 13, y - 13, 26, world=k == 2,
                       glyph_state='barred' if st == 'barred' else None)
        for r, arm in ((0, 'a'), (1, 'b')):
            for k, (rid, name) in enumerate(RUNES[path]['rows'][r]):
                x, y = pos['%s%d' % (arm, k)]
                st = rune_state(path, 'min', r, k)
                place_rune(img, 'sky', rid, ramp, st, x - 10, y - 10, 20)
                if st == 'chosen' and rid in PAGE['growth']:
                    n, cap = PAGE['growth'][rid]
                    ring_arc(img, x, y, 12.4, n / cap, ramp[4], with_alpha(ramp[0], 200))
        lx, ly, align = c['label']
        name = PATHS[path]['name'].upper()
        colour = ramp[3] if path in (p, s) else mix(ramp[3], (90, 96, 110, 255), 0.5)
        w = text_width(name, True)
        x = ox + lx - (w // 2 if align == 'c' else w if align == 'r' else 0)
        draw_text(img, x, oy + ly, name, colour, bold=True)

    # the sidebar: your page, in words
    x0 = 302
    y = 24
    draw_text(img, x0, y, 'YOUR PAGE', TEXT_DIM)
    y += 13
    blit(img, emblem(p, 'chosen', 14), x0, y)
    draw_text(img, x0 + 17, y + 3, PATHS[p]['name'], pr()[3], bold=True)
    y += 18
    rid, name = chosen_keystone()
    place_rune(img, 'sky', rid, pr(), 'chosen', x0, y, 26)
    draw_text(img, x0 + 29, y + 9, name, pr()[4])
    y += 29
    for r in range(2):
        rid, name = RUNES[p]['rows'][r][PAGE['primary_minors'][r]]
        place_rune(img, 'sky', rid, pr(), 'chosen', x0 + 3, y, 20)
        label = name + (' %d/%d' % PAGE['growth'][rid] if rid in PAGE['growth'] else '')
        draw_text(img, x0 + 26, y + 6, label, pr()[3])
        y += 22
    y += 4
    blit(img, emblem(s, 'chosen', 14), x0, y)
    draw_text(img, x0 + 17, y + 3, PATHS[s]['name'], sr()[3], bold=True)
    y += 18
    for r in range(2):
        rid, name = RUNES[s]['rows'][r][PAGE['secondary_minors'][r]]
        place_rune(img, 'sky', rid, sr(), 'chosen', x0 + 3, y, 20)
        draw_text(img, x0 + 26, y + 6, name, sr()[3])
        y += 22
    y += 2
    for r in range(3):
        st = SHARD_ROWS[r][1][PAGE['shards'][r]] if PAGE['shards'][r] is not None else None
        locked = PAGE['shards_locked'][r]
        xx = x0 + r * 30
        blit(img, star_socket(16, stat_ramp(st) if st else NEUTRAL, 'locked' if locked else 'chosen'), xx, y)
        if locked:
            blit(img, padlock(NEUTRAL), xx + 4, y + 3)
        else:
            blit(img, shard_glyph(st), xx + 4, y + 4)
    return img


# ---------------------------------------------------------------------------------------------
# Mockups
# ---------------------------------------------------------------------------------------------

LAYOUTS = [('1-pouch', render_pouch), ('2-loadout', render_loadout), ('3-orrery', render_orrery),
           ('4-skymap', render_skymap)]


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
    """Every glyph, at 6x on mid-grey: the readability check."""
    cell, k = 24, 6
    rows = [('emblems', [emblem(pth) for pth in PATH_ORDER] + [emblem(pth, 'open') for pth in PATH_ORDER])]
    for path in PATH_ORDER:
        ramp = PATHS[path]['ramp']
        ids = [rid for rid, _ in RUNES[path]['keystones']] + [rid for row in RUNES[path]['rows'] for rid, _ in row]
        rows.append((path, [glyph(rid, ramp) for rid in ids]))
    rows.append(('shards', [shard_glyph(st) for st in SHARD_MAPS]))
    width = max(len(g) for _, g in rows) * cell
    sheet = new(width + 8, len(rows) * (cell + 10) + 8, hexc('#6c6c70'))
    for i, (label, glyphs) in enumerate(rows):
        y = 4 + i * (cell + 10)
        draw_text(sheet, 4, y, label, TEXT)
        for j, g in enumerate(glyphs):
            blit(sheet, g, 4 + j * cell + (cell - g.width) // 2, y + 11 + (cell - 9 - g.height) // 2)
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


def main():
    os.makedirs(DESIGN, exist_ok=True)
    for name in os.listdir(DESIGN):
        if name.startswith('mockup-') and name.endswith('.png'):
            os.remove(os.path.join(DESIGN, name))
    for key, fn in LAYOUTS:
        panel = drop_shadow(fn())
        on_screen(panel, 426, 240, 3).save(os.path.join(DESIGN, 'layout-%s.png' % key))
        if key == '1-pouch':
            fit_sheet(panel).save(os.path.join(DESIGN, 'fit-gui-scales.png'))
    icon_sheet().save(os.path.join(DESIGN, 'icons.png'))
    print('wrote', DESIGN)


if __name__ == '__main__':
    main()
