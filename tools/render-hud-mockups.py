#!/usr/bin/env python3
"""HUD mockups, v2 (docs/technical/hud.md): smooth, not pixel art.

    python tools/render-hud-mockups.py

Everything is drawn as vector shapes at 3x and scaled down, so edges are clean at any GUI scale.
Sizes are in GUI units (what the game would use); `k` is the GUI scale. The rune glyphs and colours
come from the rune art kit. The font here is a stand-in; the build ships an open-licence face.
Writes design/hud/*.png. Nothing here is the game; it is what we agree to build.
"""
import importlib.util
import math
import os
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('rk', os.path.join(HERE, 'generate-rune-art.py'))
rk = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rk)
OUT = os.path.join(rk.ROOT, 'design', 'hud')

SS = 3


def C(h, a=255):
    return (int(h[1] + h[2], 16), int(h[3] + h[4], 16), int(h[5] + h[6], 16), a)


GOLD = C('#c9a45a')
GOLD_HI = C('#f0d890')
GLASS = C('#0a0e18', 205)
GLASS_HI = C('#1c2436', 205)
WHITE = C('#f4f6fa')
DIM = C('#9aa3b4')
GREEN = (C('#2f8f4a'), C('#6fe08a'))
RED = (C('#9a1e2a'), C('#ff5a64'))
GOLD_SH = (C('#b08a1c'), C('#ffe070'))
AIR = C('#46c8ff')
TANK = C('#ffb23a')
TRAIL = C('#fff0d8', 230)

STATC = {'ad': C('#ff9a4a'), 'as': C('#ffe060'), 'omni': C('#ff6a6a'),
         'armor': C('#e8c07a'), 'mr': C('#a79cff'), 'ms': C('#e8eef6')}


def font(size_units, k, bold=True):
    px = max(6, int(size_units * k * SS))
    for name in ('bahnschrift.ttf', 'segoeuib.ttf', 'arialbd.ttf'):
        try:
            f = ImageFont.truetype('C:/Windows/Fonts/' + name, px)
            if name == 'bahnschrift.ttf':
                try:
                    f.set_variation_by_name('SemiBold' if bold else 'Regular')
                except Exception:
                    pass
            return f
        except Exception:
            continue
    return ImageFont.load_default()


class Layer:
    """A transparent canvas at SS x. All coordinates in GUI units."""

    def __init__(self, gw, gh, k):
        self.k, self.gw, self.gh = k, gw, gh
        self.u = k * SS
        self.img = Image.new('RGBA', (gw * self.u, gh * self.u), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    def p(self, v):
        return int(round(v * self.u))

    def box(self, x0, y0, x1, y1):
        return [self.p(x0), self.p(y0), self.p(x1), self.p(y1)]

    def paste_masked(self, fill_img, mask, x, y):
        self.img.alpha_composite(Image.composite(fill_img, Image.new('RGBA', fill_img.size, (0, 0, 0, 0)), mask),
                                 (self.p(x), self.p(y)))

    def rrect(self, x0, y0, x1, y1, r, fill=None, outline=None, width=0.0):
        self.d.rounded_rectangle(self.box(x0, y0, x1, y1), radius=self.p(r), fill=fill,
                                 outline=outline, width=max(1, self.p(width)) if outline else 0)

    def grad_rrect(self, x0, y0, x1, y1, r, top, bot):
        w, h = self.p(x1) - self.p(x0), self.p(y1) - self.p(y0)
        if w <= 0 or h <= 0:
            return
        g = Image.new('RGBA', (w, h))
        gd = ImageDraw.Draw(g)
        for yy in range(h):
            t = yy / max(1, h - 1)
            gd.line([(0, yy), (w, yy)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(4)))
        m = Image.new('L', (w, h), 0)
        ImageDraw.Draw(m).rounded_rectangle([0, 0, w - 1, h - 1], radius=self.p(r), fill=255)
        self.paste_masked(g, m, x0, y0)

    def glow(self, cx, cy, radius, colour, strength=1.0, blur=None):
        pad = radius * 2.2
        w = self.p(pad * 2)
        g = Image.new('RGBA', (w, w), (0, 0, 0, 0))
        ImageDraw.Draw(g).ellipse([w // 2 - self.p(radius), w // 2 - self.p(radius),
                                   w // 2 + self.p(radius), w // 2 + self.p(radius)],
                                  fill=colour[:3] + (int(255 * strength),))
        g = g.filter(ImageFilter.GaussianBlur(self.p(blur if blur else radius * 0.6)))
        self.img.alpha_composite(g, (self.p(cx) - w // 2, self.p(cy) - w // 2))

    def ring(self, cx, cy, r, width, colour, a0=-90, a1=270):
        b = [self.p(cx - r), self.p(cy - r), self.p(cx + r), self.p(cy + r)]
        self.d.arc(b, a0, a1, fill=colour, width=max(1, self.p(width)))

    def disc(self, cx, cy, r, fill):
        self.d.ellipse([self.p(cx - r), self.p(cy - r), self.p(cx + r), self.p(cy + r)], fill=fill)

    def text(self, x, y, s, size, colour=WHITE, anchor='la', bold=True, shadow=True):
        f = font(size, self.k, bold)
        if shadow:
            self.d.text((self.p(x) + self.p(0.5), self.p(y) + self.p(0.6)), s, font=f,
                        fill=(0, 0, 0, 150), anchor=anchor)
        self.d.text((self.p(x), self.p(y)), s, font=f, fill=colour, anchor=anchor)

    def text_w(self, s, size, bold=True):
        return font(size, self.k, bold).getlength(s) / self.u

    def poly(self, pts, fill, rot=0.0, cx=50, cy=50, scale=1.0, ox=0, oy=0):
        a = math.radians(rot)
        out = []
        for (x, y) in pts:
            dx, dy = (x - cx), (y - cy)
            rx = dx * math.cos(a) - dy * math.sin(a)
            ry = dx * math.sin(a) + dy * math.cos(a)
            out.append((self.p(ox + (rx + cx) * scale / 100.0), self.p(oy + (ry + cy) * scale / 100.0)))
        self.d.polygon(out, fill=fill)

    def finish(self):
        return self.img.resize((self.gw * self.k, self.gh * self.k), Image.LANCZOS)


# --------------------------------------------------------------------------------------------
# Icons: our own geometry, drawn in a 100 x 100 box
# --------------------------------------------------------------------------------------------

def icon(L, kind, x, y, size, colour):
    s = size
    if kind == 'ad':  # sword
        L.poly([(50, 0), (57, 12), (55, 62), (45, 62), (43, 12)], colour, rot=40, scale=s, ox=x, oy=y)
        L.poly([(28, 62), (72, 62), (72, 69), (28, 69)], colour, rot=40, scale=s, ox=x, oy=y)
        L.poly([(46, 69), (54, 69), (54, 88), (46, 88)], colour, rot=40, scale=s, ox=x, oy=y)
    elif kind == 'as':  # three chevrons
        for i in range(3):
            ox = i * 24
            L.poly([(8 + ox, 18), (24 + ox, 18), (44 + ox, 50), (24 + ox, 82), (8 + ox, 82), (28 + ox, 50)],
                   colour, scale=s * 0.82, ox=x + s * 0.08, oy=y + s * 0.09)
    elif kind == 'armor':  # shield
        L.poly([(50, 4), (88, 17), (83, 54), (50, 96), (17, 54), (12, 17)], colour, scale=s, ox=x, oy=y)
        L.poly([(50, 18), (50, 80), (30, 52), (28, 28)], (255, 255, 255, 70), scale=s, ox=x, oy=y)
    elif kind == 'mr':  # hexagon with centre gem
        L.poly([(50, 4), (90, 27), (90, 73), (50, 96), (10, 73), (10, 27)], colour, scale=s, ox=x, oy=y)
        L.poly([(50, 28), (68, 50), (50, 72), (32, 50)], (10, 14, 24, 255), scale=s, ox=x, oy=y)
    elif kind == 'ms':  # speed arrow
        L.poly([(30, 22), (52, 22), (82, 50), (52, 78), (30, 78), (60, 50)], colour, scale=s, ox=x, oy=y)
        L.poly([(6, 36), (30, 36), (30, 43), (6, 43)], colour, scale=s, ox=x, oy=y)
        L.poly([(6, 57), (30, 57), (30, 64), (6, 64)], colour, scale=s, ox=x, oy=y)
    elif kind == 'omni':  # droplet
        L.poly([(50, 4), (80, 52), (50, 52), (20, 52)], colour, scale=s, ox=x, oy=y)
        L.disc(x + s * 0.5, y + s * 0.63, s * 0.31, colour)


# --------------------------------------------------------------------------------------------
# Elements
# --------------------------------------------------------------------------------------------

def glass(L, x0, y0, x1, y1, r=3.0, trim=True):
    L.grad_rrect(x0, y0, x1, y1, r, GLASS_HI, GLASS)
    if trim:
        L.rrect(x0, y0, x1, y1, r, outline=GOLD[:3] + (150,), width=0.5)


def hotbar(L, cx, bottom, selected=2):
    w, h = 182, 22
    x0, y0 = cx - w / 2, bottom - h
    glass(L, x0, y0, x0 + w, y0 + h, 4, trim=True)
    for i in range(9):
        sx = x0 + 3 + i * 20
        L.rrect(sx, y0 + 3, sx + 16, y0 + 19, 2.5, fill=C('#ffffff', 14))
    sx = x0 + 3 + selected * 20
    L.rrect(sx - 1, y0 + 2, sx + 17, y0 + 20, 3.2, outline=GOLD_HI, width=0.9)
    return x0, y0, w, h


def health(L, x0, y, w, cur, mx, shield=0, trail=None):
    h = 8
    low = cur / mx < 0.3
    L.rrect(x0, y, x0 + w, y + h, 4, fill=C('#05070c', 230))
    if trail is not None and trail > cur:
        L.grad_rrect(x0, y, x0 + w * trail / mx, y + h, 4, TRAIL, C('#d8c8b0', 230))
    top, bot = (RED[1], RED[0]) if low else (GREEN[1], GREEN[0])
    L.grad_rrect(x0, y, x0 + max(8, w * cur / mx), y + h, 4, top, bot)
    L.rrect(x0 + 1.5, y + 0.9, x0 + max(8, w * cur / mx) - 1.5, y + 2.4, 1.2, fill=C('#ffffff', 70))
    if shield:
        sw = min(w, w * shield / mx)
        L.grad_rrect(x0, y, x0 + sw, y + 3.6, 1.8, GOLD_SH[1], GOLD_SH[0])
    for i in range(1, int(mx // 100) + 1):
        if i * 100 < mx:
            tx = x0 + w * i * 100 / mx
            L.d.line([L.p(tx), L.p(y + 1), L.p(tx), L.p(y + h - 1)], fill=(0, 0, 0, 90), width=max(1, L.p(0.25)))
    L.rrect(x0, y, x0 + w, y + h, 4, outline=GOLD[:3] + (170,), width=0.5)
    cur_s, mx_s = f'{cur}', f' / {mx}'
    total = L.text_w(cur_s, 6.4) + L.text_w(mx_s, 5.2, bold=False)
    tx = x0 + w / 2 - total / 2
    L.text(tx, y + 4.3, cur_s, 6.4, WHITE, anchor='lm')
    L.text(tx + L.text_w(cur_s, 6.4), y + 4.5, mx_s, 5.2, DIM, anchor='lm', bold=False)
    if shield:
        L.text(x0 + w - 2, y - 2.2, f'+{shield}', 5.4, GOLD_SH[1], anchor='rm')


def level_badge(L, cx, cy, lvl, frac):
    r = 8.4
    L.glow(cx, cy, r, C('#9cf04c'), 0.18, blur=3)
    L.disc(cx, cy, r, C('#0a0e18', 245))
    L.ring(cx, cy, r - 0.7, 1.4, C('#ffffff', 28))
    L.ring(cx, cy, r - 0.7, 1.4, C('#a6f058'), -90, -90 + 360 * frac)
    L.text(cx, cy + 0.2, str(lvl), 7.2, C('#d8ffb0'), anchor='mm')


def food(L, x, y, frac):
    w = 36
    L.rrect(x, y, x + w, y + 5, 2.5, fill=C('#05070c', 230))
    L.grad_rrect(x, y, x + max(5, w * frac), y + 5, 2.5, C('#ffcc88'), C('#b86e1c'))
    L.rrect(x, y, x + w, y + 5, 2.5, outline=GOLD[:3] + (140,), width=0.4)


def stat_block(L, x_right, bottom, vals, deltas=None):
    """League's block: two columns, icon and number, nothing else. Sits just left of the hotbar."""
    deltas = deltas or {}
    cols = [['ad', 'as', 'omni'], ['armor', 'mr', 'ms']]
    cw, rh = 30, 10
    w, h = cw * 2 + 6, rh * 3 + 6
    x0, y0 = x_right - w, bottom - h
    glass(L, x0, y0, x0 + w, y0 + h, 4)
    L.d.line([L.p(x0 + w / 2), L.p(y0 + 4), L.p(x0 + w / 2), L.p(y0 + h - 4)], fill=GOLD[:3] + (60,), width=max(1, L.p(0.25)))
    for ci, col in enumerate(cols):
        for ri, key in enumerate(col):
            ix = x0 + 4 + ci * cw
            iy = y0 + 4.5 + ri * rh
            icon(L, key, ix, iy, 7.4, STATC[key])
            d = deltas.get(key, 0)
            c = C('#7af08a') if d > 0 else (C('#ff7a70') if d < 0 else WHITE)
            L.text(ix + 22, iy + 3.8, vals[key], 6.2, c, anchor='rm')
    return x0, y0, w, h


def rune_disc(L, cx, cy, rid, path, state, stacks=None, charge=None, cooldown=None):
    ramp = rk.PATHS[path]['ramp']
    r = 9.2
    live = state == 'live'
    col = ramp[3]
    if live:
        L.glow(cx, cy, r, col, 0.35, blur=3.5)
    L.disc(cx, cy, r, C('#0a0e18', 235))
    L.disc(cx, cy, r - 0.9, C('#161c2c', 235))
    glyph = rk.glyph(rid, ramp, 'chosen').convert('RGBA')
    gs = L.p(r * 1.45)
    glyph = glyph.resize((gs, gs), Image.BICUBIC)
    if not live:
        px = glyph.load()
        for yy in range(glyph.height):
            for xx in range(glyph.width):
                a = px[xx, yy]
                px[xx, yy] = (a[0], a[1], a[2], a[3] * 5 // 10)
    L.img.alpha_composite(glyph, (L.p(cx) - gs // 2, L.p(cy) - gs // 2))
    L.ring(cx, cy, r - 0.3, 0.9, col[:3] + ((255 if live else 90),))
    if charge is not None:
        L.ring(cx, cy, r + 1.3, 1.1, C('#ffffff', 30))
        L.ring(cx, cy, r + 1.3, 1.1, ramp[4], -90, -90 + 360 * charge)
    if cooldown is not None:
        L.d.pieslice([L.p(cx - r), L.p(cy - r), L.p(cx + r), L.p(cy + r)], -90, -90 + 360 * cooldown,
                     fill=(0, 0, 0, 150))
    if stacks is not None:
        bx, by = cx + r * 0.72, cy + r * 0.72
        L.disc(bx, by, 3.6, C('#05070c', 250))
        L.ring(bx, by, 3.4, 0.5, col)
        L.text(bx, by + 0.1, str(stacks), 5.0, WHITE, anchor='mm', shadow=False)


def oxygen_big(L, cx, cy, lungs, tank, seconds, critical):
    """The warning: a large O2 emblem. The ring is the lungs, the inner amber ring is the tank.
    It grows and reddens as air runs out, then pulses."""
    r = 20
    col = C('#ff5a64') if critical else AIR
    L.glow(cx, cy, r + 3, col, 0.55 if critical else 0.28, blur=7)
    L.disc(cx, cy, r, C('#080c16', 240))
    L.ring(cx, cy, r - 1.2, 2.4, C('#ffffff', 26))
    if lungs > 0:
        L.ring(cx, cy, r - 1.2, 2.4, col, -90, -90 + 360 * lungs)
    L.ring(cx, cy, r - 5.5, 1.2, C('#ffffff', 22))
    if tank > 0:
        L.ring(cx, cy, r - 5.5, 1.2, TANK, -90, -90 + 360 * tank)
    L.text(cx - 1.4, cy + 0.6, 'O', 18, col, anchor='mm')
    L.text(cx + 7.4, cy + 5.4, '2', 10, col, anchor='mm')
    if seconds is not None:
        L.text(cx, cy + r + 6, f'{seconds}', 7.0, col, anchor='mm')


def oxygen_small(L, cx, cy, lungs, tank):
    """Fine air, space or not: a quiet badge. Nothing shouts until it has to."""
    r = 7
    L.disc(cx, cy, r, C('#080c16', 200))
    L.ring(cx, cy, r - 0.9, 1.3, AIR[:3] + (230,), -90, -90 + 360 * lungs)
    if tank > 0:
        L.ring(cx, cy, r - 3.0, 0.8, TANK[:3] + (230,), -90, -90 + 360 * tank)
    L.text(cx - 0.7, cy + 0.3, 'O', 6.4, AIR, anchor='mm', shadow=False)
    L.text(cx + 2.8, cy + 2.2, '2', 3.8, AIR, anchor='mm', shadow=False)


# --------------------------------------------------------------------------------------------
# World backdrops (our own) and scenes
# --------------------------------------------------------------------------------------------

def world(w, h, vac, seed):
    rng = random.Random(seed)
    img = Image.new('RGBA', (w, h))
    d = ImageDraw.Draw(img)
    top, bot = ((6, 8, 16), (18, 22, 36)) if vac else ((92, 148, 214), (206, 226, 244))
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)) + (255,))
    if vac:
        for _ in range(w * h // 2600):
            x, y = rng.randrange(w), rng.randrange(int(h * 0.6))
            a = rng.randrange(90, 255)
            d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(220, 228, 245, a))
    horizon = int(h * 0.62)
    g0, g1 = ((96, 98, 108), (138, 140, 152)) if vac else ((62, 112, 52), (110, 160, 70))
    for x in range(0, w, 2):
        hh = horizon + int(h * 0.012 * math.sin(x / (w * 0.012)) + h * 0.006 * math.sin(x / (w * 0.004)))
        for y in range(hh, h, 2):
            t = (y - hh) / h
            d.rectangle([x, y, x + 1, y + 1], fill=tuple(int(g0[i] + (g1[i] - g0[i]) * (t + rng.random() * 0.15)) for i in range(3)) + (255,))
    return img.filter(ImageFilter.GaussianBlur(1.2))


def vignette(img, colour, strength):
    w, h = img.size
    big = Image.new('L', (w, h), 255)
    inner = Image.new('L', (w, h), 0)
    ImageDraw.Draw(inner).ellipse([w * 0.04, h * 0.02, w * 0.96, h * 0.98], fill=255)
    inner = inner.filter(ImageFilter.GaussianBlur(w * 0.09))
    mask = ImageChops.subtract(big, inner).point(lambda v: int(v * strength))
    img.alpha_composite(Image.composite(Image.new('RGBA', (w, h), colour), Image.new('RGBA', (w, h), (0, 0, 0, 0)), mask))


def scene(name, k, w=1920, h=1080):
    gw, gh = w // k, h // k
    base = world(gw * k, gh * k, name.startswith('vacuum'), 11).convert('RGBA')
    if name == 'vacuum':
        vignette(base, (210, 20, 34, 255), 0.9)
    L = Layer(gw, gh, k)
    cx = gw // 2
    hx0, hy0, hw, hh = hotbar(L, cx, gh - 3)
    hy = hy0 - 13
    vals = {'ad': '54', 'as': '0.63', 'omni': '0%', 'armor': '74', 'mr': '41', 'ms': '+10%'}
    deltas = {}
    if name == 'calm':
        health(L, hx0 + 10, hy, hw - 10, 640, 640)
        level_badge(L, hx0 + 4.5, hy + 4, 27, 0.62)
        food(L, hx0 + hw + 7, hy + 1.5, 1.0)
    elif name == 'fight':
        health(L, hx0 + 10, hy, hw - 10, 148, 640, shield=60, trail=232)
        level_badge(L, hx0 + 4.5, hy + 4, 27, 0.62)
        food(L, hx0 + hw + 7, hy + 1.5, 0.7)
        vals.update({'ad': '58', 'as': '0.81', 'ms': '+25%'})
        deltas = {'ad': 1, 'as': 1, 'ms': 1}
        rx = hx0 + hw + 15
        rune_disc(L, rx, hy0 + 11, 'conquerors_resolve', 'vanguard', 'live', stacks=8)
        rune_disc(L, rx + 23, hy0 + 11, 'cadence', 'vanguard', 'live', charge=0.66)
        rune_disc(L, rx + 46, hy0 + 11, 'second_wind', 'bulwark', 'idle', cooldown=0.55)
    elif name == 'vacuum':
        health(L, hx0 + 10, hy, hw - 10, 520, 640)
        level_badge(L, hx0 + 4.5, hy + 4, 27, 0.62)
        food(L, hx0 + hw + 7, hy + 1.5, 0.9)
        oxygen_big(L, cx, hy0 - 62, 0.16, 0.0, 6, True)
    elif name == 'vacuum-ok':
        health(L, hx0 + 10, hy, hw - 10, 640, 640)
        level_badge(L, hx0 + 4.5, hy + 4, 27, 0.62)
        food(L, hx0 + hw + 7, hy + 1.5, 0.9)
        oxygen_small(L, cx, hy0 - 24, 1.0, 0.7)
    elif name == 'vacuum-mid':
        health(L, hx0 + 10, hy, hw - 10, 640, 640)
        level_badge(L, hx0 + 4.5, hy + 4, 27, 0.62)
        oxygen_big(L, cx, hy0 - 62, 0.42, 0.0, 21, False)
    stat_block(L, hx0 - 6, gh - 3, vals, deltas)
    base.alpha_composite(L.finish())
    return base.convert('RGB')


def main():
    os.makedirs(OUT, exist_ok=True)
    for f in os.listdir(OUT):
        if f.endswith('.png'):
            os.remove(os.path.join(OUT, f))
    for name in ['calm', 'fight', 'vacuum', 'vacuum-mid', 'vacuum-ok']:
        scene(name, 3).save(os.path.join(OUT, f'{name}-scale3.png'))
    for k in (2, 4):
        scene('fight', k).save(os.path.join(OUT, f'fight-scale{k}.png'))
    print('wrote', OUT)


if __name__ == '__main__':
    main()
