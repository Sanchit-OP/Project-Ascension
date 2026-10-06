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

It also reads vanilla structure templates (.nbt): `archive_previews()` is the Lunar Archive's
preview set (plans, cut-aways, a section, perspective views of the surface marker), called by
tools/generate-lunar-archive.py after it writes the templates.
"""
import argparse
import glob
import gzip
import hashlib
import json
import math
import os
import struct

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


# --- structure templates (.nbt) --------------------------------------------------------------------
#
# Everything below reads vanilla structure templates rather than the frame format, and draws block
# *states*: slabs and stairs get their real half-boxes, and a state's colour comes from the model
# its own variant points at (so a lit gatestone is drawn lit, and each mural tile in its own tones).

def _nbt(data, off, t):
    if t == 1:
        return struct.unpack_from('>b', data, off)[0], off + 1
    if t == 2:
        return struct.unpack_from('>h', data, off)[0], off + 2
    if t == 3:
        return struct.unpack_from('>i', data, off)[0], off + 4
    if t == 4:
        return struct.unpack_from('>q', data, off)[0], off + 8
    if t == 5:
        return struct.unpack_from('>f', data, off)[0], off + 4
    if t == 6:
        return struct.unpack_from('>d', data, off)[0], off + 8
    if t == 8:
        n = struct.unpack_from('>H', data, off)[0]
        return data[off + 2:off + 2 + n].decode('utf-8'), off + 2 + n
    if t == 9:
        et, n = struct.unpack_from('>bi', data, off)
        off += 5
        out = []
        for _ in range(n):
            v, off = _nbt(data, off, et)
            out.append(v)
        return out, off
    if t == 10:
        out = {}
        while True:
            st = data[off]
            off += 1
            if st == 0:
                return out, off
            name, off = _nbt(data, off, 8)
            out[name], off = _nbt(data, off, st)
    if t in (7, 11, 12):
        n = struct.unpack_from('>i', data, off)[0]
        size = {7: 1, 11: 4, 12: 8}[t]
        return data[off + 4:off + 4 + n * size], off + 4 + n * size
    raise ValueError(f'NBT tag type {t}')


def state_string(entry):
    props = entry.get('Properties', {})
    if not props:
        return entry['Name']
    return entry['Name'] + '[' + ','.join(f'{k}={v}' for k, v in sorted(props.items())) + ']'


def split_state(state):
    if '[' not in state:
        return state, {}
    name, rest = state[:-1].split('[', 1)
    return name, dict(kv.split('=', 1) for kv in rest.split(','))


def load_nbt(path):
    """A structure template -> (size, {pos: state}, {pos: block-entity tag}). Jigsaw blocks are
    swapped for the final_state they turn into, which is what a player will see."""
    data = gzip.decompress(open(path, 'rb').read())
    n = struct.unpack_from('>H', data, 1)[0]
    root, _ = _nbt(data, 3 + n, 10)
    palette = [state_string(e) for e in root['palette']]
    vox, tags = {}, {}
    for b in root['blocks']:
        pos = tuple(b['pos'])
        st = palette[b['state']]
        if 'nbt' in b:
            tags[pos] = b['nbt']
            if st.startswith('minecraft:jigsaw'):
                st = b['nbt']['final_state']
        vox[pos] = st
    return tuple(root['size']), vox, tags


FULL = [(0, 0, 0, 1, 1, 1)]


def boxes_of(state):
    """The rough shape of a state, as boxes in its unit cell."""
    name, props = split_state(state)
    if name in ('minecraft:air', 'minecraft:structure_void'):
        return []
    if name.endswith('_slab'):
        t = props.get('type', 'bottom')
        return [(0, 0, 0, 1, .5, 1)] if t == 'bottom' else [(0, .5, 0, 1, 1, 1)] if t == 'top' else FULL
    if name.endswith('_stairs'):
        top = props.get('half', 'bottom') == 'top'
        y0, y1 = (0, .5) if top else (.5, 1)
        base = (0, .5, 0, 1, 1, 1) if top else (0, 0, 0, 1, .5, 1)
        step = {'north': (0, y0, 0, 1, y1, .5), 'south': (0, y0, .5, 1, y1, 1),
                'west': (0, y0, 0, .5, y1, 1), 'east': (.5, y0, 0, 1, y1, 1)}[props.get('facing', 'north')]
        return [base, step]
    if name.endswith(':archive_plinth'):
        return [(0, 0, 0, 1, .125, 1), (.25, .125, .25, .75, .75, .75), (.125, .75, .125, .875, .9375, .875)]
    if name.endswith(':archive_orrery'):
        return [(0, 0, 0, 1, .25, 1), (.125, .25, .125, .875, .75, .875), (.0625, .75, .0625, .9375, .875, .9375)]
    if name == 'minecraft:chest':
        return [(.0625, 0, .0625, .9375, .875, .9375)]
    return FULL


def _variant_model(ns, path, props):
    bs = _find(ns, 'blockstates', path + '.json')
    if not bs:
        return None
    variants = json.load(open(bs)).get('variants', {})
    for key, v in variants.items():
        want = dict(kv.split('=', 1) for kv in key.split(',') if kv)
        if all(props.get(k) == val for k, val in want.items()):
            return (v[0] if isinstance(v, list) else v)['model']
    return None


def _model_textures(model_ref):
    """model -> (base texture path, glow texture path or None)."""
    glow = None
    for _ in range(5):
        mns, mpath = model_ref.split(':') if ':' in model_ref else ('minecraft', model_ref)
        mp = _find(mns, 'models', mpath + '.json')
        if not mp:
            return None, glow
        m = json.load(open(mp))
        tex = m.get('textures', {})

        def resolve(t):
            while t and t.startswith('#'):
                t = tex.get(t[1:])
            if not t:
                return None
            tns, tpath = t.split(':') if ':' in t else ('minecraft', t)
            return _find(tns, 'textures', tpath + '.png')
        if 'glow' in tex:
            glow = resolve(tex['glow'])
        for k in ('all', 'side', 'base', 'front', 'top', 'bottom', 'particle', 'end'):
            if tex.get(k):
                r = resolve(tex[k])
                if r:
                    return r, glow
        if 'parent' not in m:
            return None, glow
        model_ref = m['parent']
    return None, glow


def _average(png):
    im = Image.open(png).convert('RGBA').crop((0, 0, 16, 16))
    px = [p for p in im.getdata() if p[3] > 0]
    return tuple(sum(p[i] for p in px) // len(px) for i in range(3)) if px else None


STATE_GUESS = {'minecraft:chest': (128, 92, 52)}
_state_cache = {}


def state_colour(state):
    if state in _state_cache:
        return _state_cache[state]
    name, props = split_state(state)
    c = None
    if name in ('minecraft:air', 'minecraft:structure_void'):
        _state_cache[state] = None
        return None
    if ':' in name:
        ns, path = name.split(':')
        model = _variant_model(ns, path, props)
        if model:
            base, glow = _model_textures(model)
            if base:
                c = _average(base)
                if glow and c:
                    g = _average(glow)
                    if g:
                        c = tuple(int(c[i] * 0.4 + g[i] * 0.6) for i in range(3))
    if c is None:
        c = STATE_GUESS.get(name) or colour_of(name)
    _state_cache[state] = c
    return c


SHADE = {(1, 0, 0): 0.80, (-1, 0, 0): 0.80, (0, 1, 0): 1.0, (0, -1, 0): 0.5, (0, 0, 1): 0.65, (0, 0, -1): 0.65}


def _face_quads(vox):
    """Every box face that is not hidden against a full neighbour: (normal, 4 corners, colour)."""
    full = {pos for pos, st in vox.items() if boxes_of(st) == FULL}
    for (x, y, z), st in vox.items():
        col = state_colour(st)
        if col is None:
            continue
        for bx0, by0, bz0, bx1, by1, bz1 in boxes_of(st):
            lo, hi = (bx0, by0, bz0), (bx1, by1, bz1)
            for nrm in SHADE:
                axis = [i for i in range(3) if nrm[i]][0]
                on_edge = (hi[axis] == 1) if nrm[axis] > 0 else (lo[axis] == 0)
                nb = (x + nrm[0], y + nrm[1], z + nrm[2])
                if on_edge and nb in full:
                    continue
                plane = (hi if nrm[axis] > 0 else lo)[axis]
                a, b = [i for i in range(3) if i != axis]
                corners = []
                for ua, ub in ((0, 0), (1, 0), (1, 1), (0, 1)):
                    p = [0.0, 0.0, 0.0]
                    p[axis] = plane
                    p[a] = (lo[a], hi[a])[ua]
                    p[b] = (lo[b], hi[b])[ub]
                    corners.append((x + p[0], y + p[1], z + p[2]))
                yield nrm, corners, col


def render_iso_states(vox, d, path, scale=12, title=None, bg=(20, 22, 30)):
    dx, dy, dz = d
    n = math.sqrt(dx * dx + dy * dy + dz * dz)
    d = (dx / n, dy / n, dz / n)
    r = (d[2], 0, -d[0])
    rn = math.hypot(r[0], r[2])
    r = (r[0] / rn, 0, r[2] / rn)
    u = (d[1] * r[2] - d[2] * r[1], d[2] * r[0] - d[0] * r[2], d[0] * r[1] - d[1] * r[0])
    if u[1] < 0:
        u = tuple(-c for c in u)
    faces = []
    for nrm, corners, col in _face_quads(vox):
        if sum(a * b for a, b in zip(nrm, d)) >= 0:
            continue
        cen = [sum(c[i] for c in corners) / 4 for i in range(3)]
        pts = [(sum(a * b for a, b in zip(p, r)) * scale, -sum(a * b for a, b in zip(p, u)) * scale)
               for p in corners]
        faces.append((sum(cen[i] * d[i] for i in range(3)), pts, tuple(int(v * SHADE[nrm]) for v in col)))
    faces.sort(key=lambda f: -f[0])
    xs = [p[0] for f in faces for p in f[1]]
    ys = [p[1] for f in faces for p in f[1]]
    pad, top = 16, 20 if title else 0
    img = Image.new('RGB', (int(max(xs) - min(xs)) + 2 * pad, int(max(ys) - min(ys)) + 2 * pad + top), bg)
    dr = ImageDraw.Draw(img)
    ox, oy = -min(xs) + pad, -min(ys) + pad + top
    for _, poly, c in faces:
        dr.polygon([(px + ox, py + oy) for px, py in poly], fill=c, outline=tuple(max(0, v - 22) for v in c))
    if title:
        dr.text((8, 5), title, fill=(230, 230, 230))
    img.save(path)
    return img


def render_perspective(vox, eye, target, path, size=(960, 540), fov=60, ground_y=None, title=None,
                       stars_seed=3):
    """A pinhole view at eye height, with a flat regolith plain at ground_y (the top of the paving
    row) and a black sky: what a player walking up sees."""
    W, H = size
    f = [target[i] - eye[i] for i in range(3)]
    fn = math.sqrt(sum(v * v for v in f))
    f = [v / fn for v in f]
    r = [-f[2], 0, f[0]]
    rn = math.hypot(r[0], r[2])
    r = [r[0] / rn, 0, r[2] / rn]
    u = [r[1] * f[2] - r[2] * f[1], r[2] * f[0] - r[0] * f[2], r[0] * f[1] - r[1] * f[0]]
    if u[1] < 0:
        u = [-v for v in u]
    focal = (W / 2) / math.tan(math.radians(fov / 2))

    def proj(p):
        v = [p[i] - eye[i] for i in range(3)]
        zc = sum(v[i] * f[i] for i in range(3))
        if zc < 0.05:
            return None
        return (W / 2 + sum(v[i] * r[i] for i in range(3)) / zc * focal,
                H / 2 - sum(v[i] * u[i] for i in range(3)) / zc * focal), zc

    img = Image.new('RGB', (W, H), (4, 5, 9))
    dr = ImageDraw.Draw(img)
    rng = __import__('random').Random(stars_seed)
    for _ in range(140):
        s = rng.randrange(150, 235)
        dr.point((rng.randrange(W), rng.randrange(H)), fill=(s, s, min(255, s + 10)))
    if ground_y is not None:
        # the plain: project a point far along the ground to find the horizon, shade toward it
        far = proj([eye[0] + f[0] * 4000, ground_y, eye[2] + f[2] * 4000])
        hy = far[0][1] if far else H / 2
        for y in range(int(max(0, hy)), H):
            t = max(0.0, min(1.0, (y - hy) / (H - hy + 1)))
            g = int(96 + 52 * t ** 0.6)
            dr.line([(0, y), (W, y)], fill=(g, g - 1, g - 4))
    faces = []
    for nrm, corners, col in _face_quads(vox):
        cen = [sum(c[i] for c in corners) / 4 for i in range(3)]
        to = [cen[i] - eye[i] for i in range(3)]
        if sum(nrm[i] * to[i] for i in range(3)) >= 0:
            continue
        pts = [proj(c) for c in corners]
        if any(p is None for p in pts):
            continue
        dist = math.sqrt(sum(v * v for v in to))
        faces.append((dist, [p[0] for p in pts], tuple(int(v * SHADE[nrm]) for v in col)))
    faces.sort(key=lambda q: -q[0])
    for _, poly, c in faces:
        dr.polygon(poly, fill=c, outline=tuple(max(0, v - 18) for v in c))
    if title:
        dr.text((10, 8), title, fill=(220, 220, 220))
    img.save(path)
    return img


def render_plan(vox, tags, x_range, z_range, y_floor, y_cut, path, scale=14, title=None, marks=None,
                dark=None):
    """Top-down plan of one floor: walls (anything standing on the floor up to y_cut) drawn muted,
    the floor in its own colours, voids below the floor darkened by depth. Plinths are numbered in
    reading order with a tick toward their reader; C is a chest, the cyan ring the orrery's
    hologram. Cells in `dark` (floor a monster could spawn on, unlit) are struck through in red."""
    x0, x1 = x_range
    z0, z1 = z_range
    top = 22 if title else 0
    img = Image.new('RGB', ((x1 - x0 + 1) * scale + 2, (z1 - z0 + 1) * scale + 2 + top), (14, 15, 20))
    dr = ImageDraw.Draw(img)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            px, pz = (x - x0) * scale + 1, (z - z0) * scale + 1 + top
            cell = None
            for y in range(y_cut, y_floor - 7, -1):
                st = vox.get((x, y, z))
                if st and state_colour(st) is not None:
                    cell = (y, st)
                    break
            if not cell:
                continue
            y, st = cell
            c = state_colour(st)
            if y > y_floor:
                c = tuple(int(v * 0.55 + 18) for v in c)              # standing walls: muted
                dr.rectangle([px, pz, px + scale - 1, pz + scale - 1], fill=c, outline=(8, 8, 10))
            else:
                k = 1.0 if y == y_floor else max(0.35, 0.8 - 0.08 * (y_floor - y))
                c = tuple(int(v * k) for v in c)
                dr.rectangle([px, pz, px + scale - 1, pz + scale - 1], fill=c)
            if dark and any((x, yy, z) in dark for yy in range(y_floor - 6, y_cut + 1)):
                dr.line([px, pz, px + scale - 1, pz + scale - 1], fill=(230, 40, 40), width=2)
    for (x, y, z), tag in tags.items():
        if not (x0 <= x <= x1 and z0 <= z <= z1 and y_floor - 1 <= y <= y_cut):
            continue
        px, pz = (x - x0) * scale + 1, (z - z0) * scale + 1 + top
        st = vox.get((x, y, z), '')
        label = None
        if 'entry' in tag:
            label = str((marks or {}).get(tag['entry'].split('/')[-1], '?'))
            _, props = split_state(st)
            fx, fz = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}[props.get('facing', 'north')]
            cx, cz = px + scale / 2, pz + scale / 2
            dr.line([cx, cz, cx + fx * scale * 0.9, cz + fz * scale * 0.9], fill=(255, 220, 120), width=2)
            dr.rectangle([px + 1, pz + 1, px + scale - 2, pz + scale - 2], fill=(240, 200, 90))
        elif 'LootTable' in tag:
            label = 'C'
            dr.rectangle([px + 1, pz + 1, px + scale - 2, pz + scale - 2], fill=(150, 100, 50))
        if label:
            dr.text((px + 2, pz), label, fill=(10, 10, 10))
    for (x, y, z), st in vox.items():
        if st.endswith(':archive_orrery') and x0 <= x <= x1 and z0 <= z <= z1 and y_floor <= y <= y_cut:
            px, pz = (x - x0) * scale + 1, (z - z0) * scale + 1 + top
            dr.ellipse([px - scale * 2, pz - scale * 2, px + scale * 3, pz + scale * 3], outline=(110, 240, 230))
    if title:
        dr.text((6, 5), title, fill=(230, 230, 230))
    img.save(path)
    return img


def stack(images, path=None, gap=10, bg=(14, 15, 20), horizontal=False):
    if horizontal:
        W = sum(i.width for i in images) + gap * (len(images) - 1)
        H = max(i.height for i in images)
    else:
        W = max(i.width for i in images)
        H = sum(i.height for i in images) + gap * (len(images) - 1)
    out = Image.new('RGB', (W, H), bg)
    o = 0
    for i in images:
        out.paste(i, (o, 0) if horizontal else (0, o))
        o += (i.width if horizontal else i.height) + gap
    if path:
        out.save(path)
    return out


def archive_previews(ent, vlt, gen):
    """The Lunar Archive's previews, drawn from the .nbt files just written (so they prove the
    files, not the generator's memory of them). Called by tools/generate-lunar-archive.py."""
    out = gen.DESIGN
    os.makedirs(out, exist_ok=True)
    _, evox, etags = load_nbt(os.path.join(gen.OUT, 'entrance.nbt'))
    _, vvox, vtags = load_nbt(os.path.join(gen.OUT, 'vault.nbt'))
    ox, oy, oz = gen.VAULT_OFFSET
    whole = {(x + ox, y + oy, z + oz): st for (x, y, z), st in vvox.items()}
    whole.update(evox)
    marks = {e: i + 1 for i, e in enumerate(gen.ENTRIES)}
    vdark = set(gen.dark_spots(vlt)[1])
    edark = set(gen.dark_spots(ent)[1])

    def cut(vox, pred):
        return {k: v for k, v in vox.items() if pred(k)}

    M, L, S = gen.MAIN, gen.LOW, gen.SURFACE_Y
    plans = [
        render_plan(vvox, vtags, (0, 47), (0, 47), M, M + 3, os.path.join(out, 'plan-vault-main.png'),
                    title=f'vault, main floor (row {M})', marks=marks, dark=vdark),
        render_plan(vvox, vtags, (0, 47), (0, 47), L, L + 3, os.path.join(out, 'plan-vault-low.png'),
                    title=f'vault, gate hall and sealed room (row {L})', marks=marks, dark=vdark),
        render_plan(evox, etags, (0, 18), (0, 47), S, S + 3, os.path.join(out, 'plan-entrance-surface.png'),
                    title=f'entrance, surface (row {S})', marks=marks, dark=edark),
    ]
    stack(plans, os.path.join(out, 'plans.png'), horizontal=True)
    render_iso_states(cut(vvox, lambda k: k[1] <= M + 4), (-1, -1.25, -1),
                      os.path.join(out, 'cutaway-vault-main.png'),
                      title='vault, cut away above the main floor, from the south-east')
    render_iso_states(cut(vvox, lambda k: k[1] <= L + 15 and k[0] <= 18 and 31 <= k[2] <= 43),
                      (-0.6, -0.75, 1.2), os.path.join(out, 'cutaway-gate-hall.png'), scale=16,
                      title='the gate hall, north wall and ceiling cut away, looking at the dead gate')
    render_iso_states(cut(vvox, lambda k: k[1] <= L + 5 and 7 <= k[0] <= 16 and 22 <= k[2] <= 36),
                      (-0.45, -1.5, -1), os.path.join(out, 'cutaway-sealed-room.png'), scale=18,
                      title='the sealed room and its breach, ceiling cut away, from the south-east')
    render_iso_states(cut(whole, lambda k: k[0] <= gen.XC), (-1.0, -0.45, -0.35),
                      os.path.join(out, 'section-entrance.png'), scale=10,
                      title='section along the entrance axis, from the east')
    render_iso_states(cut(whole, lambda k: k[1] >= S - 8), (-1, -0.8, -1.3),
                      os.path.join(out, 'surface.png'), scale=12, title='the surface, from the south-east')
    surf = cut(evox, lambda k: k[1] >= S)
    ground = S + 1
    render_perspective(surf, (gen.XC + 0.5, ground + 1.62, 33 + 64), (gen.XC + 0.5, ground + 9, 33),
                       os.path.join(out, 'marker-64.png'), ground_y=ground,
                       title='the surface marker from 64 blocks south, at eye height')
    render_perspective(surf, (gen.XC + 45.5, ground + 1.62, 33 + 45), (gen.XC + 0.5, ground + 9, 33),
                       os.path.join(out, 'marker-64-diagonal.png'), ground_y=ground,
                       title='from 64 blocks south-east')
    render_perspective(surf, (gen.XC + 0.5, ground + 1.62, 33 + 18), (gen.XC + 0.5, ground + 4, 26),
                       os.path.join(out, 'approach.png'), ground_y=ground, fov=70,
                       title='walking up to the cutting')
    print('wrote previews to', os.path.relpath(out, ROOT))


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
