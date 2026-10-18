#!/usr/bin/env python3
"""HUD mockups (docs/technical/hud.md). Seeded, original, built on the rune art kit's colours,
glyphs and stand-in font, so the HUD reads as the same family as the orrery.

    python tools/render-hud-mockups.py

Writes design/hud/*.png. Everything is drawn in GUI pixels, then scaled by the GUI scale with
nearest-neighbour, which is how Minecraft shows it. Nothing here is the game; it is what we agree
to build.
"""
import importlib.util
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('rk', os.path.join(HERE, 'generate-rune-art.py'))
rk = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rk)

OUT = os.path.join(rk.ROOT, 'design', 'hud')
hexc, mix, new, rect, put = rk.hexc, rk.mix, rk.new, rk.rect, rk.put
text, tw = rk.draw_text, rk.text_width

GOLD = rk.PATHS['vanguard']['ramp']
FRAME = hexc('#0b0f1a')
TRACK = hexc('#1a2030')
TRIM = hexc('#8a6a2c')
HP = [hexc('#2e7a3a'), hexc('#62c86a'), hexc('#b4f0a0')]
LOW = [hexc('#8a1c24'), hexc('#e2505a'), hexc('#ffb0a8')]
SHIELD = [hexc('#a8841c'), hexc('#f0d060'), hexc('#fff4b8')]
TRAIL = hexc('#f4e8d0')
AIR = [hexc('#1f6c94'), hexc('#43c5f0'), hexc('#b8f0ff')]
TANK = [hexc('#8a5a14'), hexc('#e0a43a'), hexc('#ffe2a0')]
FOOD = hexc('#d89040')
XPC = rk.STAT['xp']


# --------------------------------------------------------------------------------------------
# Pieces
# --------------------------------------------------------------------------------------------

def framed(img, x, y, w, h):
    """Dark inset with a one-pixel gold trim, the orrery's frame language in small."""
    rect(img, x - 1, y - 1, x + w + 1, y + h + 1, TRIM)
    rect(img, x, y, x + w, y + h, FRAME)
    rect(img, x + 1, y + 1, x + w - 1, y + h - 1, TRACK)


def fill_bar(img, x, y, w, h, frac, ramp):
    """A fill with a lit top row and a shaded bottom row, so it reads as a tube, not a flat strip."""
    n = int(round((w - 2) * max(0.0, min(1.0, frac))))
    if n <= 0:
        return
    rect(img, x + 1, y + 1, x + 1 + n, y + h - 1, ramp[1])
    rect(img, x + 1, y + 1, x + 1 + n, y + 2, ramp[2])
    rect(img, x + 1, y + h - 2, x + 1 + n, y + h - 1, ramp[0])


def ticks(img, x, y, w, h, every_frac_list):
    for f in every_frac_list:
        px = x + 1 + int((w - 2) * f)
        rk.vline(img, px, y + 1, y + h - 1, (0, 0, 0, 120))


def health(img, x, y, w, cur, mx, shield=0, trail=None, flash=False):
    """One bar for any maximum. Ticks every 100 HP (more of them as the pool grows) so length
    stays meaningful; the pale trail is health just lost; gold on top is absorption."""
    h = 9
    framed(img, x, y, w, h)
    low = cur / mx < 0.3
    if trail is not None:
        fill_bar(img, x, y, w, h, trail / mx, [TRAIL, TRAIL, TRAIL])
    fill_bar(img, x, y, w, h, cur / mx, LOW if low else HP)
    if shield:
        sw = min(1.0, shield / mx)
        fill_bar(img, x, y, w, 4, sw, SHIELD)
    ticks(img, x, y, w, h, [i * 100 / mx for i in range(1, int(mx // 100) + 1) if i * 100 < mx])
    if flash:
        rect(img, x + 1, y + 1, x + w - 1, y + 2, (255, 255, 255, 90))
    # The numbers sit beside the bar, not on it: a fill under text is unreadable at any scale.
    label = f'{cur}'
    nx = x + w + 4
    text(img, nx, y + 1, label, hexc('#ffffff'))
    text(img, nx + tw(label) + 2, y + 1, f'/ {mx}', hexc('#9aa0ae'))
    if shield:
        text(img, x + 1, y - 9, f'+{shield}', SHIELD[1])


def level_gem(img, cx, cy, lvl):
    s = str(lvl)
    wd = tw(s) + 6
    rect(img, cx - wd // 2 - 1, cy - 5, cx + wd // 2 + 2, cy + 6, TRIM)
    rect(img, cx - wd // 2, cy - 4, cx + wd // 2 + 1, cy + 5, FRAME)
    text(img, cx - tw(s) // 2, cy - 3, s, XPC)


def hotbar(img, cx, bottom):
    """Placeholder slots, our own drawing: the hotbar is not ours to restyle (see hud.md)."""
    w = 182
    x0 = cx - w // 2
    y0 = bottom - 22
    rect(img, x0, y0, x0 + w, y0 + 22, (10, 12, 18, 190))
    for i in range(9):
        sx = x0 + 3 + i * 20
        rect(img, sx, y0 + 3, sx + 16, y0 + 19, (60, 66, 82, 120))
    rect(img, x0 + 3 + 0 * 20 - 2, y0 + 1, x0 + 3 + 18, y0 + 21, (255, 255, 255, 0))
    sel = x0 + 3 + 2 * 20
    rk.rect(img, sel - 2, y0 + 1, sel + 18, y0 + 2, hexc('#ffffff'))
    rk.rect(img, sel - 2, y0 + 20, sel + 18, y0 + 21, hexc('#ffffff'))
    rk.rect(img, sel - 2, y0 + 1, sel - 1, y0 + 21, hexc('#ffffff'))
    rk.rect(img, sel + 17, y0 + 1, sel + 18, y0 + 21, hexc('#ffffff'))
    return x0, y0, w


def xp_line(img, x0, y, w, frac):
    rect(img, x0, y, x0 + w, y + 2, FRAME)
    rect(img, x0 + 1, y, x0 + 1 + int((w - 2) * frac), y + 1, XPC)


def food_pips(img, x, y, units, mx=20):
    """Food as a short bar, not ten icons: it still gates sprint and regeneration, but it is a
    slow resource and does not deserve the row's loudest place."""
    w = 36
    framed(img, x, y, w, 5)
    fill_bar(img, x, y, w, 5, units / mx, [hexc('#7a4a14'), FOOD, hexc('#ffd8a0')])


def oxygen(img, cx, y, lungs, tank, tank_cap, show_num, pulse):
    """One gauge, two sources: lungs (blue) then tank (amber), drained tank-first. Absent until
    air is short; the number appears only when it is about to matter."""
    w = 110
    x = cx - w // 2
    h = 6
    framed(img, x, y, w, h)
    lung_w = int(w * 0.5)
    fill_bar(img, x, y, lung_w + 1, h, lungs, AIR if lungs > 0.25 else LOW)
    rk.vline(img, x + lung_w, y, y + h, TRIM)
    fill_bar(img, x + lung_w, y, w - lung_w, h, tank, TANK)
    rk.rect(img, x + 1, y - 1, x + 3, y, TRIM)
    text(img, x - 16, y - 1, 'O2', AIR[1])
    if show_num is not None:
        s = f'{show_num}s'
        text(img, cx - tw(s) // 2, y - 10, s, LOW[1] if pulse else hexc('#ffffff'))


def stat_icon(img, x, y, stat):
    g = rk.shard_glyph(stat)
    img.alpha_composite(g, (x, y))


STAT_ROWS = [
    ('ad', 'ad', 'Attack Damage'), ('as', 'as', 'Attack Speed'),
    ('armor', 'armor', 'Armor'), ('mr', 'mr', 'Magic Resist'),
    ('ms', 'ms', 'Move Speed'),
]


def stat_panel(img, x, y, values, expanded=False, deltas=None):
    """League's column, slimmed: icon and number, coloured by what the stat is; a buff lifts the
    number to green, a debuff turns it red. Held key opens the names and the sources."""
    deltas = deltas or {}
    rows = list(STAT_ROWS)
    row_h = 13
    w = 112 if expanded else 46
    h = len(rows) * row_h + 4
    rect(img, x, y, x + w, y + h, (10, 12, 18, 170))
    rk.hline(img, x, x + w, y, TRIM)
    for i, (key, shard, name) in enumerate(rows):
        ry = y + 3 + i * row_h
        stat_icon(img, x + 3, ry, shard)
        col = rk.STAT[key]
        d = deltas.get(key, 0)
        if d > 0:
            col = hexc('#7af08a')
        elif d < 0:
            col = hexc('#ff7a70')
        v = values[key]
        text(img, x + 18, ry + 3, v, col)
        if expanded:
            text(img, x + 18 + tw(v) + 4, ry + 3, name if key not in ('armor', 'mr') else name, rk.TEXT_DIM)
    if expanded:
        extra = [('Armor 74', '-29% physical'), ('Resist 41', '-21% magic')]
        ey = y + h + 3
        rect(img, x, ey, x + 128, ey + 24, (10, 12, 18, 170))
        for i, (a, b) in enumerate(extra):
            text(img, x + 4, ey + 3 + i * 10, a, rk.STAT['armor'] if i == 0 else rk.STAT['mr'])
            text(img, x + 4 + tw(a) + 4, ey + 3 + i * 10, b, rk.TEXT_DIM)
    return h


def rune_chip(img, x, y, rid, path, state, stacks=None, frac=None, cooldown=None):
    """One equipped rune. Idle: dim and small. Live: bright with its number or ring. The glyph is
    the orrery's own, so a player already knows it."""
    ramp = rk.PATHS[path]['ramp']
    g = rk.glyph(rid, ramp, 'chosen')
    if state == 'idle':
        g = rk.shade_dim(g) if hasattr(rk, 'shade_dim') else dim(g)
    rect(img, x - 1, y - 1, x + 21, y + 21, ramp[0] if state != 'idle' else FRAME)
    rect(img, x, y, x + 20, y + 20, (12, 14, 22, 220))
    img.alpha_composite(g, (x, y))
    if frac is not None:  # a charge filling along the bottom edge
        rect(img, x, y + 18, x + 20, y + 20, FRAME)
        rect(img, x, y + 18, x + int(20 * frac), y + 20, ramp[3])
    if cooldown is not None:  # a sweep, as a dark cap from the top
        rect(img, x, y, x + 20, y + int(20 * cooldown), (0, 0, 0, 110))
    if stacks is not None:
        s = str(stacks)
        text(img, x + 21 - tw(s) - 1, y + 12, s, hexc('#ffffff'))
    if state == 'live':
        rk.hline(img, x - 1, x + 21, y - 2, ramp[4])


def dim(g):
    out = g.copy()
    px = out.load()
    for yy in range(out.height):
        for xx in range(out.width):
            r, gg, b, a = px[xx, yy]
            px[xx, yy] = (min(255, r * 9 // 10 + 20), min(255, gg * 9 // 10 + 20), min(255, b * 9 // 10 + 20), a * 8 // 10)
    return out


def vignette(img, colour, strength):
    """The low-air warning that is not only a bar: the screen edge reddens."""
    w, h = img.size
    px = img.load()
    for yy in range(h):
        for xx in range(w):
            dx = abs(xx - w / 2) / (w / 2)
            dy = abs(yy - h / 2) / (h / 2)
            e = max(0.0, max(dx, dy) - 0.6) / 0.4
            if e > 0:
                a = int(strength * 150 * e * e)
                r, g, b, aa = px[xx, yy]
                t = a / 255
                px[xx, yy] = (int(r * (1 - t) + colour[0] * t), int(g * (1 - t) + colour[1] * t),
                              int(b * (1 - t) + colour[2] * t), 255)


def world(w, h, vac, seed):
    rng = random.Random(seed)
    img = new(w, h)
    top, bot = (hexc('#05060c'), hexc('#10121c')) if vac else (hexc('#6fa0d8'), hexc('#c8dcf0'))
    for y in range(h):
        rk.hline(img, 0, w, y, mix(top, bot, y / h))
    if vac:
        for _ in range(w * h // 500):
            put(img, rng.randrange(w), rng.randrange(h // 2), hexc('#c8d0e0', rng.randrange(80, 255)))
    ground = (hexc('#6a6c74'), hexc('#8a8c94')) if vac else (hexc('#4c7a34'), hexc('#6a9a44'))
    horizon = int(h * 0.62)
    for x in range(w):
        hh = horizon + int(4 * __import__('math').sin(x / 23.0) + 3 * __import__('math').sin(x / 7.0))
        for y in range(hh, h):
            put(img, x, y, mix(ground[0], ground[1], rng.random() * 0.5 + (y - hh) / h))
    return img


# --------------------------------------------------------------------------------------------
# Scenes
# --------------------------------------------------------------------------------------------

def scene(name, gui_w, gui_h):
    img = world(gui_w, gui_h, name.startswith('vacuum'), 11)
    if name == 'vacuum':
        vignette(img, (190, 20, 30), 0.55)
    cx = gui_w // 2
    bottom = gui_h - 2
    hx0, hy0, hw = hotbar(img, cx, bottom)
    xp_line(img, hx0, hy0 - 4, hw, 0.62)
    level_gem(img, cx, hy0 - 3, 27)

    if name == 'calm':
        health(img, hx0, hy0 - 16, 100, 640, 640)
        food_pips(img, hx0 + hw + 6, hy0 - 8, 20)
        stat_panel(img, 4, gui_h // 2 - 20, {'ad': '42', 'as': '1.1', 'armor': '74', 'mr': '41', 'ms': '+10%'})
    elif name == 'fight':
        health(img, hx0, hy0 - 16, 100, 148, 640, shield=60, trail=232, flash=False)
        food_pips(img, hx0 + hw + 6, hy0 - 8, 14)
        stat_panel(img, 4, gui_h // 2 - 20,
                   {'ad': '58', 'as': '1.3', 'armor': '74', 'mr': '41', 'ms': '+25%'},
                   deltas={'ad': 1, 'as': 1, 'ms': 1})
        rx = hx0 + hw + 6
        rune_chip(img, rx, hy0 + 1, 'conquerors_resolve', 'vanguard', 'live', stacks=8)
        rune_chip(img, rx + 24, hy0 + 1, 'cadence', 'vanguard', 'live', frac=0.66)
        rune_chip(img, rx + 48, hy0 + 1, 'second_wind', 'bulwark', 'idle', cooldown=0.55)
    elif name == 'vacuum':
        health(img, hx0, hy0 - 16, 100, 520, 640)
        food_pips(img, hx0 + hw + 6, hy0 - 8, 18)
        oxygen(img, cx, hy0 - 32, lungs=0.15, tank=0.0, tank_cap=1.0, show_num=6, pulse=True)
        stat_panel(img, 4, gui_h // 2 - 20, {'ad': '42', 'as': '1.1', 'armor': '74', 'mr': '41', 'ms': '+10%'})
    elif name == 'vacuum-ok':
        health(img, hx0, hy0 - 16, 100, 640, 640)
        food_pips(img, hx0 + hw + 6, hy0 - 8, 18)
        oxygen(img, cx, hy0 - 32, lungs=1.0, tank=0.7, tank_cap=1.0, show_num=None, pulse=False)
    elif name == 'expanded':
        health(img, hx0, hy0 - 16, 100, 640, 640)
        stat_panel(img, 4, gui_h // 2 - 40, {'ad': '58', 'as': '1.3', 'armor': '74', 'mr': '41', 'ms': '+25%'},
                   expanded=True, deltas={'ad': 1, 'as': 1, 'ms': 1})
    return img


def shot(name, k, w=1920, h=1080):
    gw, gh = w // k, h // k
    s = scene(name, gw, gh)
    return rk.scaled(s, k).convert('RGB')


def main():
    os.makedirs(OUT, exist_ok=True)
    for name in ['calm', 'fight', 'vacuum', 'vacuum-ok', 'expanded']:
        shot(name, 3).save(os.path.join(OUT, f'{name}-scale3.png'))
    for k in (2, 4):
        shot('fight', k).save(os.path.join(OUT, f'fight-scale{k}.png'))
    print('wrote', OUT)


if __name__ == '__main__':
    main()
