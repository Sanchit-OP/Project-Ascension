#!/usr/bin/env python3
"""The growth tree's mockups (docs/progression/growth.md section 10).

Draws the skill tree as a web of rings, like the rune screen's orrery: the real node files
(modules/combat/.../growth_node), in four rings round a centre emblem, so the whole tree fits one
screen. The four themes sit at the corners and a blend of each pair on the sides, so the web reads
Power, Fury, Guard, Wayfarer all the way round, and any node touches its neighbours in every
direction. Taken nodes are bright stars in a halo; nodes you can take are rings of starlight; a node
you have marked but not confirmed has a dashed white ring; the rest are dim rings. Notables are
larger and framed. A side panel lists points, where they came from, and your stats.

Everything below "The tree" is the rune screen's art kit, copied from tools/generate-rune-art.py in
the runes session's worktree (the night sky in a brass frame, sockets, buttons, the stand-in font)
so the two screens are one game. When the runes work merges, this copy should become an import of
a shared module: the helpers are identical and are meant to stay so.

Run:  python tools/generate-growth-art.py
Everything is seeded; re-running reproduces every PNG byte for byte. The panel is 400 x 224 GUI
pixels, one texel per GUI pixel, shown at GUI scale 3. Text is in the stand-in pixel font with
Minecraft's metrics; in game the screen uses Minecraft's own font.

Art rules (docs/technical/art-guide.md): original art only, light from the top-left, no teal and
no diamond eye (the gateway network's), no accidental crosses, letters or faces.
"""

import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
DESIGN = os.path.join(ROOT, 'design', 'growth')
PANEL_W, PANEL_H = 400, 224

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
# The rune screen's kit: the night sky in a brass frame, sockets, buttons, the pointer
# ---------------------------------------------------------------------------------------------

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


def draw_button(img, x, y, w, h, label, primary):
    rect(img, x, y, x + w, y + h, (10, 14, 28, 255))
    c = STAR_BRASS[2] if primary else STAR_BRASS[1]
    hline(img, x, x + w, y, c)
    hline(img, x, x + w, y + h - 1, c)
    vline(img, x, y, y + h, c)
    vline(img, x + w - 1, y, y + h, c)
    draw_text(img, x + (w - text_width(label)) // 2, y + (h - 8) // 2 + 1, label, TEXT if primary else TEXT_BODY)


def draw_xp_orb(img, x, y):
    for (px, py) in disc(x + 3.5, y + 3.5, 3.4):
        put(img, px, py, hexc('#3c8c14'))
    for (px, py) in disc(x + 3.5, y + 3.5, 2.4):
        put(img, px, py, hexc('#8ce03c'))
    put(img, x + 2, y + 2, hexc('#e8ff9c'))


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


# ---------------------------------------------------------------------------------------------
# The tree: four arms of the real node files, drawn as a constellation
# ---------------------------------------------------------------------------------------------

NODE_DIR = os.path.join(ROOT, 'modules', 'combat', 'src', 'main', 'resources', 'data', 'ascension_combat',
                        'ascension_combat', 'growth_node')

BRASS_RAMP = [STAR_BRASS[0], STAR_BRASS[1], STAR_BRASS[2], STAR_BRASS[3], STAR_BRASS[3]]
GREEN = [hexc('#0e2a12'), hexc('#2c6a26'), hexc('#4ea23c'), hexc('#8ad868'), hexc('#dcffc8')]

# Four themes, in the colours of the rune paths they pair with. A node takes the colour of its first stat.
ARMS = {
    'power': {'name': 'Power', 'ramp': PATHS['reaver']['ramp']},
    'fury': {'name': 'Fury', 'ramp': PATHS['vanguard']['ramp']},
    'guard': {'name': 'Guard', 'ramp': PATHS['bulwark']['ramp']},
    'wayfarer': {'name': 'Wayfarer', 'ramp': GREEN},
}

# The web: node files give a place in ring steps from the centre (x right, y down); this scales it to
# pixels. Ring 4 sits 86 px out, so its capstones just fit the 200 px the panel gives the tree.
UNIT = 21.5
CENTRE = (135, 121)
SMALL, NOTABLE = 13, 21

# What a node's first stat looks like: a glyph and a colour (the colours of the rune screen's cards).
STAT_OF = {
    'minecraft:generic.max_health': 'hp', 'ascension_combat:armor': 'armor',
    'ascension_combat:magic_resist': 'mr', 'minecraft:generic.attack_damage': 'ad',
    'ascension_combat:lethality': 'leth', 'ascension_combat:armor_penetration_percent': 'pen',
    'ascension_combat:magic_penetration_percent': 'pen', 'minecraft:generic.attack_speed': 'as',
    'ascension_combat:omnivamp': 'omni', 'minecraft:generic.movement_speed': 'ms',
    'minecraft:player.block_break_speed': 'dig', 'minecraft:player.block_interaction_range': 'reach',
    'minecraft:generic.safe_fall_distance': 'fall', 'minecraft:generic.step_height': 'step',
}
STAT.update({'leth': hexc('#f0b060'), 'pen': hexc('#f0805c'), 'dig': hexc('#c8a878'),
             'reach': hexc('#a0d8e8'), 'fall': hexc('#e8ecf4'), 'step': hexc('#c8d0a0')})
STAT_NAME = {
    'hp': 'HP', 'armor': 'Armor', 'mr': 'Magic Resist', 'ad': 'Attack Damage', 'leth': 'Lethality',
    'pen': 'Penetration', 'as': 'Attack Speed', 'omni': 'Omnivamp', 'ms': 'Move Speed',
    'dig': 'Mining Speed', 'reach': 'Reach', 'fall': 'Safe Fall', 'step': 'Step Height',
}

# Seven by seven, one bit each. Placeholders for the icons G5 will polish, drawn to read at a glance:
# no crosses, no letters, no faces (art-guide.md).
GLYPHS = {
    'hp': [".##.##.", "#######", "#######", "#######", ".#####.", "..###..", "...#..."],
    'armor': ["#######", "#.....#", "#.....#", "#.....#", ".#...#.", "..#.#..", "...#..."],
    'mr': ["..###..", ".#####.", "#######", "#######", "#######", ".#####.", "..###.."],
    'ad': [".....##", "....###", "...###.", "#.###..", ".###...", ".#.#...", "#...#.."],
    'leth': ["...#.#.", "..#.#.#", ".#.#.#.", "#.#.#..", ".#.#...", "#.#....", ".#....."],
    'pen': ["...#...", "..###..", ".#####.", "...#...", "...#...", "...#...", "...#..."],
    'as': ["....##.", "...##..", "..##...", ".######", "...##..", "..##...", ".##...."],
    'omni': ["...#...", "..###..", ".#####.", "#######", "#######", ".#####.", "..###.."],
    'ms': ["#..#...", ".#..#..", "..#..#.", "...#..#", "..#..#.", ".#..#..", "#..#..."],
    'dig': [".####..", "#...##.", "....#.#", "...#..#", "..#....", ".#.....", "#......"],
    'reach': [".......", "..#....", ".##....", "#######", ".##....", "..#....", "......."],
    'fall': ["....###", "..#####", ".#####.", "#####..", ".##....", "#......", "......."],
    'step': [".....##", ".....##", "...####", "...####", ".######", ".######", "......."],
}


def glyph_img(key, colour, light):
    """The glyph in the stat's colour, with a darker pixel under and to the right of each lit one."""
    rows = GLYPHS[key]
    img = new(8, 8)
    dark = mix(colour, (0, 0, 0, 255), 0.55)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == '#':
                put(img, x + 1, y + 1, dark)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == '#':
                put(img, x, y, light if (x + y) % 4 == 0 and y < 3 else colour)
    return img


def load_nodes():
    folder = os.path.join(NODE_DIR, 'web')
    nodes = {}
    for file in sorted(os.listdir(folder)):
        data = json.load(open(os.path.join(folder, file), encoding='utf-8'))
        nid = 'web/' + file[:-5]
        stats = data['stats']
        nodes[nid] = {
            'id': nid, 'arm': data['arm'], 'notable': data.get('kind') == 'notable',
            'start': data.get('start', False),
            'pos': data['position'], 'links': [l.split(':')[1] for l in data.get('links', [])],
            'stats': stats, 'key': STAT_OF[stats[0]['attribute']],
            'name': data['name']['translate'].split('.')[-1].replace('_', ' ').title(),
        }
    # a link joins two nodes whichever of them names it
    for node in list(nodes.values()):
        for link in node['links']:
            if node['id'] not in nodes[link]['links']:
                nodes[link].setdefault('also', []).append(node['id'])
    for node in nodes.values():
        node['near'] = set(node['links']) | set(node.get('also', []))
    return nodes


def node_xy(node):
    return CENTRE[0] + node['pos'][0] * UNIT, CENTRE[1] + node['pos'][1] * UNIT


def node_state(node, taken, marked):
    if node['id'] in taken:
        return 'chosen'
    if node['id'] in marked:
        return 'marked'
    if node['start'] or node['near'] & (taken | marked):
        return 'open'
    return 'locked'


# What the mockup's player has done: the Warden, the Wither, Earth's gate, an Ancient City and the
# Moon, so ten points. Six are spent, starting in Guard and walking round the ring into Fury, and two
# more are marked but not confirmed: Stout Heart, and a step toward Wayfarer.
SPENT = ['web/r1_se', 'web/r2_se', 'web/r1_e', 'web/r2_e', 'web/r1_ne', 'web/r2_ne']
MARKED = ['web/r3_se', 'web/r2_se_s']
EARNED = [("The Warden Falls", 3), ("The Wither Falls", 3), ("Earth's Gate Lit", 2),
          ("An Ancient City", 1), ("Landing on the Moon", 1)]
TOTAL_V1 = 35


def arm_cloud(img, cx, cy, ramp, radius=70, strength=34):
    for (x, y) in disc(cx, cy, radius):
        d = math.hypot(x + 0.5 - cx, (y + 0.5 - cy) * 1.25) / radius
        if d < 1:
            put(img, x, y, with_alpha(ramp[2], strength * (1 - d) ** 2))


def draw_link(img, a, b, ra, rb, state):
    ramp = [mix(ra[i], rb[i], 0.5) for i in range(5)]
    if state == 'chosen':
        line(img, a[0], a[1], b[0], b[1], with_alpha(ramp[3], 230))
    elif state == 'open':
        line(img, a[0], a[1], b[0], b[1], with_alpha(ramp[1], 190))
    else:
        line(img, a[0], a[1], b[0], b[1], (70, 78, 96, 90), dash=2)


def draw_node(img, node, state):
    ramp = ARMS[node['arm']]['ramp']
    x, y = node_xy(node)
    size = NOTABLE if node['notable'] else SMALL
    # a node you cannot reach yet is a dim ring, not the rune screen's dust, which means "barred"
    sock = star_socket(size, NEUTRAL if state == 'locked' else ramp,
                       'chosen' if state == 'marked' else 'open' if state == 'locked' else state,
                       'world' if node['notable'] else 'round')
    ox, oy = int(round(x - size / 2)), int(round(y - size / 2))
    blit(img, sock, ox, oy)
    if node['notable']:                      # framed: a second ring just inside the first
        circle(img, x, y, size / 2 - 3.2, with_alpha(ramp[3] if state != 'locked' else (90, 96, 110, 255), 150))
    if state == 'marked':                    # picked, not yet confirmed: a dashed white ring
        circle(img, x, y, size / 2 + 1.2, with_alpha(TEXT, 235), 1.0, dash=22)
    stat_c = STAT[node['key']]
    if state == 'locked':
        g = glyph_img(node['key'], (112, 120, 140, 230), (112, 120, 140, 230))
    elif state == 'open':
        g = glyph_img(node['key'], mix(stat_c, (150, 160, 190, 255), 0.45), mix(stat_c, TEXT, 0.4))
    else:
        g = glyph_img(node['key'], stat_c, mix(stat_c, TEXT, 0.55))
    blit(img, g, int(round(x - 3.5)), int(round(y - 3.5)))


def centre_emblem(img):
    """A neutral four-pointed star in a brass ring: the placeholder for growth.md question 9."""
    cx, cy = CENTRE
    glow(img, cx, cy, 15, STAR_BRASS[3], 90)
    circle(img, cx, cy, 8.5, STAR_BRASS[2])
    circle(img, cx, cy, 6.5, with_alpha(STAR_BRASS[1], 200))
    for k in range(-5, 6):                   # the four long points, thinning as they go out
        w = (5 - abs(k)) // 2
        for j in range(-w, w + 1):
            c = STAR_BRASS[3] if j == 0 else STAR_BRASS[2]
            put(img, cx + k, cy + j, c)
            put(img, cx + j, cy + k, c)


def sums(nodes, ids):
    """The numbers the taken nodes add up to, by stat key and whether it is a percent."""
    out = {}
    for nid in ids:
        for s in nodes[nid]['stats']:
            key = STAT_OF[s['attribute']]
            percent = s.get('operation') == 'add_multiplied_base' or key in ('omni', 'pen')
            entry = out.setdefault(key, [0.0, percent])
            entry[0] += s['amount'] * (100 if percent else 1)
    return out


def fmt(v):
    return str(int(v)) if abs(v - round(v)) < 1e-9 else ('%.2f' % v).rstrip('0').rstrip('.')


def title_bar(img, marked):
    draw_text(img, 9, 7, 'Skill tree', STAR_BRASS[3], bold=True)
    bx = PANEL_W - 8
    for label, primary in (('Confirm', marked > 0), ('Respec', False)):
        w = text_width(label) + 14
        bx -= w
        draw_button(img, bx, 5, w, 12, label, primary)
        bx -= 4
    cx = bx - 38
    draw_xp_orb(img, cx, 7)
    draw_text(img, cx + 10, 7, '20 lv', STAT['xp'])
    draw_text(img, cx - 68, 7, '%d marked' % marked, TEXT_BODY if marked else TEXT_FAINT)


def side_panel(img, nodes, available, marked_n):
    x0 = 272
    vline(img, x0 - 4, 21, PANEL_H - 4, with_alpha(STAR_BRASS[1], 150))
    draw_text(img, x0, 24, 'POINTS', TEXT_DIM)
    big = str(available)
    draw_text(img, x0, 34, big, TEXT, bold=True)
    draw_text(img, x0 + text_width(big, True) + 4, 34, 'to spend', TEXT_BODY)
    draw_text(img, x0, 46, '%d marked: %d left' % (marked_n, available - marked_n), TEXT_BODY if marked_n else TEXT_FAINT)

    draw_text(img, x0, 62, 'WHERE THEY CAME FROM', TEXT_DIM)
    y = 73
    for name, pts in EARNED:
        draw_text(img, x0, y, name, TEXT_BODY)
        draw_text(img, x0 + 122 - text_width('+%d' % pts), y, '+%d' % pts, STAR_BRASS[3])
        y += 10
    left = TOTAL_V1 - sum(p for _, p in EARNED)
    draw_text(img, x0, y + 1, '%d points still to find' % left, TEXT_FAINT)

    y += 17
    draw_text(img, x0, y, 'YOUR STATS', TEXT_DIM)
    y += 11
    for key, (amount, percent) in sums(nodes, SPENT).items():
        s = ('+' + fmt(amount) + ('%' if percent else '') + ' ')
        xx = draw_text(img, x0, y, s, STAT[key])
        draw_text(img, xx + 1, y, STAT_NAME[key], TEXT_BODY)
        y += 10
    draw_text(img, x0, PANEL_H - 24, 'Click a node to mark it.', TEXT_FAINT)
    draw_text(img, x0, PANEL_H - 14, 'Confirm spends them.', TEXT_FAINT)


def card(img, x, y, w, name, tag, ramp, body, numbers, note, ax=0, ay=0):
    """The hover card: name, arm and kind, a plain sentence, the numbers in their stat's colour, and
    what taking it needs. A nine-sliced panel in game; drawn whole here."""
    body_l, note_l = wrap(body, w - 14), wrap(note, w - 14)
    h = 6 + 10 + 12 + len(body_l) * 10 + 4 + len(numbers) * 10 + 4 + len(note_l) * 10 + 4
    if x + w > PANEL_W - 6:                  # beside the node it describes, kept on the panel
        x = int(ax - 12 - w)
    y = max(22, min(y, PANEL_H - 6 - h))
    rect(img, x + 2, y + 2, x + w + 2, y + h + 2, (0, 0, 0, 110))
    rect(img, x, y, x + w, y + h, with_alpha((6, 9, 20, 255), 252))
    hline(img, x, x + w, y, ramp[3])
    hline(img, x, x + w, y + h - 1, ramp[1])
    vline(img, x, y, y + h, ramp[2])
    vline(img, x + w - 1, y, y + h, ramp[1])
    for (cx, cy) in ((x, y), (x + w - 1, y), (x, y + h - 1), (x + w - 1, y + h - 1)):
        put(img, cx, cy, STAR_BRASS[3])
    yy = y + 6
    draw_text(img, x + 7, yy, name, TEXT, bold=True)
    draw_text(img, x + 7, yy + 10, tag, ramp[3])
    yy += 22
    for ln in body_l:
        draw_text(img, x + 7, yy, ln, TEXT_BODY)
        yy += 10
    yy += 4
    for s, col in numbers:
        draw_text(img, x + 7, yy, s, col)
        yy += 10
    yy += 4
    for ln in note_l:
        draw_text(img, x + 7, yy, ln, TEXT_DIM)
        yy += 10


def render_tree(hover=None):
    nodes = load_nodes()
    taken, marked = set(SPENT), set(MARKED)
    img = star_panel(random.Random(3107))
    title_bar(img, len(marked))
    for dx, dy, arm in ((-1, -1, 'power'), (1, -1, 'fury'), (1, 1, 'guard'), (-1, 1, 'wayfarer')):
        arm_cloud(img, CENTRE[0] + dx * 52, CENTRE[1] + dy * 46, ARMS[arm]['ramp'])
    # the rings' faint guides, under everything
    for r in (1, 2, 3, 4):
        circle(img, CENTRE[0], CENTRE[1], r * UNIT, with_alpha(STAR_BRASS[1], 40))
    # links under the sockets, each drawn once
    drawn = set()
    for node in nodes.values():
        a = node_xy(node)
        for other_id in node['near']:
            key = frozenset((node['id'], other_id))
            if key in drawn:
                continue
            drawn.add(key)
            other = nodes[other_id]
            states = {node_state(node, taken, marked), node_state(other, taken, marked)}
            on = {node['id'], other_id} & (taken | marked)
            state = ('chosen' if {node['id'], other_id} <= taken else
                     'open' if on else 'locked')
            draw_link(img, a, node_xy(other), ARMS[node['arm']]['ramp'], ARMS[other['arm']]['ramp'], state)
        if node['start']:
            draw_link(img, a, CENTRE, ARMS[node['arm']]['ramp'], BRASS_RAMP,
                      'chosen' if node['id'] in taken else 'open')
    centre_emblem(img)
    for node in nodes.values():
        draw_node(img, node, node_state(node, taken, marked))
    # the themes' names, in the corners their regions fill
    draw_text(img, 8, 23, ARMS['power']['name'].upper(), ARMS['power']['ramp'][3], bold=True)
    fury = ARMS['fury']['name'].upper()
    draw_text(img, 264 - text_width(fury, True), 23, fury, ARMS['fury']['ramp'][3], bold=True)
    guard = ARMS['guard']['name'].upper()
    draw_text(img, 264 - text_width(guard, True), PANEL_H - 14, guard, ARMS['guard']['ramp'][3], bold=True)
    draw_text(img, 8, PANEL_H - 14, ARMS['wayfarer']['name'].upper(), ARMS['wayfarer']['ramp'][3], bold=True)
    side_panel(img, nodes, sum(p for _, p in EARNED) - len(SPENT), len(marked))

    if hover:
        node = nodes[hover]
        x, y = node_xy(node)
        circle(img, x, y, (NOTABLE if node['notable'] else SMALL) / 2 + 2.4, with_alpha(TEXT, 230), 1.0)
        cursor(img, int(x + 3), int(y + 3))
        ramp = ARMS[node['arm']]['ramp']
        numbers = []
        for st in node['stats']:
            key = STAT_OF[st['attribute']]
            percent = st.get('operation') == 'add_multiplied_base' or key in ('omni', 'pen')
            numbers.append(('+' + fmt(st['amount'] * (100 if percent else 1)) + ('%' if percent else '') + ' '
                            + STAT_NAME[key], STAT[key]))
        state = node_state(node, taken, marked)
        note = 'Costs 1 point. You marked it: confirm to spend.' if state == 'marked' else 'Costs 1 point.'
        kind = 'notable' if node['notable'] else 'node'
        card(img, int(x + 12), int(y - 14), 142, node['name'], '%s - %s' % (ARMS[node['arm']]['name'], kind),
             ramp, 'More life.', numbers, note, x, y)
    return img


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
        if name.endswith('.png'):
            os.remove(os.path.join(DESIGN, name))
    panel = drop_shadow(render_tree())
    on_screen(panel, 426, 240, 3).save(os.path.join(DESIGN, 'tree.png'))
    on_screen(drop_shadow(render_tree(hover='web/r3_se')), 426, 240, 3).save(
        os.path.join(DESIGN, 'tree-hover.png'))
    fit_sheet(panel).save(os.path.join(DESIGN, 'tree-fit-gui-scales.png'))
    print('wrote', DESIGN)


if __name__ == '__main__':
    main()
