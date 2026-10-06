"""Preview a block structure without starting the game. No build or runtime dependency.

Run from the repository root:
    python tools/render-structure-preview.py <frame.json> [--site <site.json>] [--out <dir>]

Reads anything in the gateway frame format (docs/technical/gateway-frame-format.md): a "key",
"layers" bottom-up, rows front (z=0) to back, cells left to right. An optional site file in the
same format with "frame_offset" is drawn around it. Writes three PNGs to --out (default: the
current directory): two isometric views and a straight front elevation.

Colours come from the blocks' own textures: each block id is resolved through its blockstate and
model to a texture in this repository, and drawn in that texture's average colour, so a preview
tracks the real art. Vanilla ids are not ours to read, so they fall back to a small table of
rough colours, then to a grey derived from the id. Needs Pillow.

This is a sketching tool. It checks nothing (the logic's GatewayFrame does that at load).
"""
import argparse
import glob
import hashlib
import json
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
VANILLA_GUESS = {
    'air': None, 'deepslate': (80, 80, 86), 'deepslate_bricks': (72, 72, 76),
    'deepslate_tiles': (54, 54, 58), 'polished_deepslate': (72, 72, 74), 'blackstone': (44, 38, 46),
    'polished_blackstone': (54, 50, 60), 'polished_blackstone_bricks': (50, 44, 54),
    'basalt': (80, 80, 86), 'polished_basalt': (98, 98, 100), 'smooth_basalt': (72, 72, 78),
    'netherrack': (110, 54, 54), 'nether_bricks': (46, 24, 28), 'quartz_block': (236, 230, 222),
    'quartz_pillar': (234, 228, 220), 'calcite': (222, 224, 220), 'polished_diorite': (192, 192, 194),
    'stone': (126, 126, 126), 'stone_bricks': (122, 122, 122), 'crying_obsidian': (40, 12, 70),
    'obsidian': (20, 18, 30), 'bone_block': (226, 220, 196), 'sculk': (14, 40, 48),
}
FIELD_COLOUR = (80, 220, 230)


def _assets():
    return glob.glob(os.path.join(ROOT, 'modules', '*', 'src', 'main', 'resources', 'assets', '*'))


def _find(ns, kind, path):
    for a in _assets():
        if os.path.basename(a) == ns:
            p = os.path.join(a, kind, *path.split('/'))
            if os.path.exists(p):
                return p
    return None


def _texture_of(block_id):
    """block id -> path of the texture its default model mostly shows, or None."""
    ns, path = block_id.split(':')
    bs = _find(ns, 'blockstates', path + '.json')
    if not bs:
        return None
    variants = json.load(open(bs)).get('variants', {})
    if not variants:
        return None
    v = next(iter(variants.values()))
    model_ref = (v[0] if isinstance(v, list) else v)['model']
    for _ in range(4):                                      # follow parents a few levels
        mns, mpath = model_ref.split(':') if ':' in model_ref else ('minecraft', model_ref)
        mp = _find(mns, 'models', mpath + '.json')
        if not mp:
            return None
        m = json.load(open(mp))
        tex = m.get('textures', {})
        for k in ('all', 'side', 'base', 'bottom', 'particle', 'end'):
            t = tex.get(k)
            while t and t.startswith('#'):
                t = tex.get(t[1:])
            if t:
                tns, tpath = t.split(':') if ':' in t else ('minecraft', t)
                return _find(tns, 'textures', tpath + '.png')
        if 'parent' not in m:
            return None
        model_ref = m['parent']
    return None


_cache = {}


def colour_of(block_id):
    if block_id in _cache:
        return _cache[block_id]
    c = None
    png = _texture_of(block_id)
    if png:
        im = Image.open(png).convert('RGBA').crop((0, 0, 16, 16))
        px = [p for p in im.getdata() if p[3] > 0]
        c = tuple(sum(p[i] for p in px) // len(px) for i in range(3)) if px else None
    if c is None:
        name = block_id.split(':')[1]
        c = VANILLA_GUESS.get(name)
        if c is None and name not in VANILLA_GUESS:
            h = hashlib.md5(block_id.encode()).digest()
            g = 90 + h[0] % 90
            c = (g, g, g + h[1] % 20)
    _cache[block_id] = c
    return c


def load(path, offset=(0, 0, 0)):
    """-> {(x, y, z): (block_id or '#field', half_height)}"""
    d = json.load(open(path))
    key, field = d['key'], d.get('field')
    out = {}
    ox, oy, oz = offset
    for y, layer in enumerate(d['layers']):
        for z, row in enumerate(layer):
            for x, ch in enumerate(row):
                if ch == ' ':
                    continue
                if ch == field:
                    out[(x + ox, y + oy, z + oz)] = ('#field', False)
                    continue
                e = key[ch]
                bid = e.get('block') or e.get('place')
                out[(x + ox, y + oy, z + oz)] = (bid, 'slab' in bid)
    return d, out


def render_iso(vox, d, path, scale=14, title=None):
    dx, dy, dz = d
    n = math.sqrt(dx * dx + dy * dy + dz * dz)
    d = (dx / n, dy / n, dz / n)
    r = (d[2], 0, -d[0])
    rn = math.hypot(r[0], r[2])
    r = (r[0] / rn, 0, r[2] / rn)
    u = (d[1] * r[2] - d[2] * r[1], d[2] * r[0] - d[0] * r[2], d[0] * r[1] - d[1] * r[0])
    if u[1] < 0:
        u = tuple(-c for c in u)

    def proj(p):
        return (sum(a * b for a, b in zip(p, r)) * scale, -sum(a * b for a, b in zip(p, u)) * scale)

    shades = {(1, 0, 0): 0.80, (-1, 0, 0): 0.80, (0, 1, 0): 1.0, (0, -1, 0): 0.5,
              (0, 0, 1): 0.65, (0, 0, -1): 0.65}

    def solid(k):
        v = vox.get(k)
        return v is not None and v[0] != '#field' and not v[1] and colour_of(v[0]) is not None

    faces = []
    for (x, y, z), (bid, half) in vox.items():
        field = bid == '#field'
        col = FIELD_COLOUR if field else colour_of(bid)
        if col is None:
            continue
        h = 0.5 if half else 1.0
        for nrm, sh in shades.items():
            if sum(a * b for a, b in zip(nrm, d)) >= 0:
                continue
            nb = (x + nrm[0], y + nrm[1], z + nrm[2])
            if not field and solid(nb) and not (nrm == (0, 1, 0) and half):
                continue
            if field and vox.get(nb, ('',))[0] == '#field':
                continue
            fixed = [i for i in range(3) if nrm[i] != 0][0]
            ax = [i for i in range(3) if nrm[i] == 0]
            corners = []
            for a0, a1 in ((0, 0), (1, 0), (1, 1), (0, 1)):
                p = [x, y, z]
                p[fixed] += 1 if nrm[fixed] > 0 else 0
                if fixed == 1 and nrm[1] > 0:
                    p[1] = y + h
                p[ax[0]] += a0
                p[ax[1]] += a1
                if ax[0] == 1:
                    p[1] = y + a0 * h
                if ax[1] == 1:
                    p[1] = y + a1 * h
                corners.append(tuple(p))
            cen = [sum(c[i] for c in corners) / 4 for i in range(3)]
            faces.append((sum(cen[i] * d[i] for i in range(3)), [proj(p) for p in corners],
                          tuple(int(v * sh) for v in col), field))
    faces.sort(key=lambda f: -f[0])
    xs = [p[0] for f in faces for p in f[1]]
    ys = [p[1] for f in faces for p in f[1]]
    pad, top = 16, 18 if title else 0
    img = Image.new('RGBA', (int(max(xs) - min(xs)) + 2 * pad, int(max(ys) - min(ys)) + 2 * pad + top),
                    (20, 22, 30, 255))
    ox, oy = -min(xs) + pad, -min(ys) + pad + top
    for _, poly, c, field in faces:
        pts = [(px + ox, py + oy) for px, py in poly]
        if field:
            ov = Image.new('RGBA', img.size, (0, 0, 0, 0))
            ImageDraw.Draw(ov).polygon(pts, fill=c + (150,))
            img = Image.alpha_composite(img, ov)
        else:
            ImageDraw.Draw(img).polygon(pts, fill=c + (255,), outline=tuple(max(0, v - 25) for v in c) + (255,))
    if title:
        ImageDraw.Draw(img).text((8, 4), title, fill=(230, 230, 230, 255))
    img.save(path)


def render_front(vox, path, scale=12):
    xs, ys, zs = zip(*vox)
    x0, y0 = min(xs), min(ys)
    W, H = max(xs) - x0 + 1, max(ys) - y0 + 1
    img = Image.new('RGB', (W * scale, H * scale), (20, 22, 30))
    dr = ImageDraw.Draw(img)
    for x in range(x0, x0 + W):
        for y in range(y0, y0 + H):
            for z in range(min(zs), max(zs) + 1):
                v = vox.get((x, y, z))
                if not v:
                    continue
                field = v[0] == '#field'
                col = FIELD_COLOUR if field else colour_of(v[0])
                if col is None:
                    continue
                k = 1.0 - min(z - min(zs), 8) * 0.06
                col = tuple(int(c * k) for c in col)
                top = (y0 + H - 1 - y) * scale + (scale // 2 if v[1] else 0)
                left = (x - x0) * scale
                dr.rectangle([left, top, left + scale - 1, (y0 + H - y) * scale - 1], fill=col,
                             outline=tuple(max(0, c - 30) for c in col))
                if not field:
                    break
    img.save(path)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('frame')
    ap.add_argument('--site')
    ap.add_argument('--out', default='.')
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    _, vox = load(a.frame)
    if a.site:
        site = json.load(open(a.site))
        offset = tuple(site['frame_offset'])
        _, svox = load(a.site)
        vox = {(x + offset[0], y + offset[1], z + offset[2]): v for (x, y, z), v in vox.items()} | {
            k: v for k, v in svox.items()}
        # the frame wins wherever both define a cell
        _, fvox = load(a.frame, offset)
        vox.update(fvox)
    name = os.path.splitext(os.path.basename(a.frame))[0]
    render_iso(vox, (1, -0.85, 1.15), os.path.join(a.out, f'{name}-front-left.png'), title=f'{name}, front-left')
    render_iso(vox, (-0.7, -0.8, -1), os.path.join(a.out, f'{name}-back-right.png'), title=f'{name}, back-right')
    render_front(vox, os.path.join(a.out, f'{name}-front.png'))
    print('wrote', os.path.join(a.out, f'{name}-*.png'))


if __name__ == '__main__':
    main()
