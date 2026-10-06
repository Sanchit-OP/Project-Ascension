"""3D in-hand models for the combat weapons: Veigo's Blade, the Darkin Blade and the Black Cleaver.

Run from the repository root:
    python tools/generate-combat-models.py

Writes models/item/<id>_3d.json and textures/item/<id>_3d.png under the combat module's assets,
and the previews in design/combat-items/3d/. Hand edits to any of those are lost on the next run:
edit this file. The flat inventory icons and the <id>.json wrappers that pick between icon and 3D
model are tools/generate-combat-art.py's.

Each weapon is drawn the way the sprites are: a front view typed in as a pixel map, one character
per pixel, upright with the tip at the top, one model unit per pixel. Every symbol has a colour and
a thickness, and the model is that picture extruded: thin bevelled edges, a thicker ridge, thicker
guards, a bulging gem. Neighbouring cells of the same thickness merge into one cuboid. The pixel
map is also the texture: south faces show their rectangle of it, north faces the mirror image,
side faces stretch the texel on their edge. Glowing pixels are their own cuboids, drawn
full-bright through NeoForge's per-element neoforge_data.

League of Legends items, drawn to look like their League counterparts in our own pixels (Sanchit,
2026-10-06): nothing is traced, sampled or scaled from Riot's images. Palettes come from the
sprites in generate-combat-art.py, so icon and model can't drift apart. The plan is
plans/combat-art-3d.md; the batch is described in docs/technical/combat-art.md. Needs Pillow and,
for the previews, numpy.
"""
import importlib.util
import json
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))


def _load(name, file):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, file))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


art = _load('combat_art', 'generate-combat-art.py')
ASSETS = art.ASSETS
TEX = 32  # texture size; the pixel map sits in its top-left corner
GLOW = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}

MODELS = {}


def weapon(name, palette, depth, rows, glow='', grip=(8, 6), first_person=None, scale=(0.85, 0.68)):
    """palette: symbol -> colour; depth: symbol -> thickness in model units (pixels). grip: the
    model point the hand holds; the hand transforms are vanilla's handheld ones moved there
    (upright_handheld), at scale (third person, first person). first_person overrides that view."""
    width = max(len(r) for r in rows)
    rows = [r.ljust(width, '.') for r in rows]
    assert len(rows) <= TEX and width <= TEX, (name, width, len(rows))
    for y, r in enumerate(rows):
        for c in r:
            assert c == '.' or (c in palette and c in depth), (name, y, c)
    MODELS[name] = dict(palette=palette, depth=depth, rows=rows, glow=glow, grip=grip,
                        first_person=first_person, scale=scale)


def sprite_palette(name):
    return art.SPRITES[name][1]


# --- Veigo's Blade: Viego's sword ------------------------------------------------------------
# Bottom to top, as his model render reads hilt to tip: an anchor pommel whose horns point at the
# blade, a shaft with a glowing socket, the great guard (a diamond hub round a bulging jewel, two
# arms ending in crescents that open outward), a short wrapped grip, a small horned guard and the
# long narrow blade with a glowing ridge. Spectral jade throughout. An odd width, so the blade
# has a centre ridge. (League's guard arms are offset like an S; at this size that read as a
# tangle, so they are level.)
_V = sprite_palette('veigos_blade')
weapon('veigos_blade', {
    'w': _V['w'], 'l': _V['l'], 'g': _V['g'],                        # blade (all glowing): edges, ridge
    'W': _V['w'], 'L': _V['l'], 'M': _V['m'], 'D': _V['d'], 'K': _V['K'],  # guards and pommel
    'J': _V['g'], 'j': _V['G'],                                      # the jewel and socket
    'R': _V['m'], 'r': _V['d'],                                      # wrapped grip, shaft
}, {
    'w': 1, 'l': 1, 'g': 2,
    'W': 2.5, 'L': 3, 'M': 3, 'D': 3, 'K': 2,
    'J': 4, 'j': 4,
    'R': 2, 'r': 2,
}, [
    ".......w.......",
    "......wgl......",
    "......wgl......",
    "......wgl......",
    "......wgl......",
    "......wgl......",
    "......wgl......",
    "......wgl......",
    "......wgl......",
    "......wgl......",
    "......wgl......",
    "......wgl......",
    "......wgl......",
    "......wgl......",
    "....W.wgl.L....",
    "....LMwglMD....",
    "....WLLMMMD....",
    ".....DDDDD.....",
    "......RrR......",
    "......rRr......",
    "W.....RrR.....W",
    "WL.....L.....WL",
    ".WL...LWM...WL.",
    ".LMLLLMJMDLLLM.",
    ".MDMDMMjDDMDMD.",
    ".DM...MDD...DM.",
    "DM.....D.....MD",
    "K......r......K",
    "...W...J...L...",
    "...LM..j..MD...",
    "....LMMMMMD....",
    ".....DDDDK.....",
], glow='wlgJj', grip=(8, 12.5),
    # In first person it stands upright at the right of the view, face on, the great guard in
    # sight at the bottom; VeigosBladeClient swings it from there (the swing strip in its preview).
    first_person={'rotation': [0, -25, 13.8], 'translation': [2.83, 1.69, -4.26], 'scale': [0.75] * 3})

# The two big weapons stand like Veigo's Blade in first person, upright at the right of the view
# and face on; vanilla's swing then reads as a forward chop. (Vanilla's own diagonal pose, which
# upright_handheld gives, juts out of the view at 32 pixels tall.)
BIG_FIRST_PERSON = {'rotation': [0, -25, 16.3], 'translation': [2.58, 2.19, -2.7], 'scale': [0.7] * 3}

# --- Darken Blade: Aatrox's Darkin Blade -----------------------------------------------------
# A massive dark greatsword: gunmetal plates bevelled down to thin edges, barbs along both edges
# raking back toward the hilt, and a glowing crimson channel with a hot core sunk into the middle
# of the blade. A broad guard swept down into spikes, a long maroon-wrapped grip, and a spear-point
# pommel, as on his back in his render.
_D = sprite_palette('darken_blade')
weapon('darken_blade', {
    'e': _D['w'], 'l': _D['l'], 'm': _D['m'], 'd': _D['d'], 'K': _D['K'],  # blade plates, edges
    'R': _D['R'], 'Y': _D['Y'],                                      # the channel, its hot core
    'T': _D['l'], 'U': _D['w'], 't': _D['d'], 'u': _D['K'],          # barbs, lit and shadowed side
    'E': _D['w'], 'L': _D['l'], 'M': _D['m'], 'D': _D['d'], 'k': _D['K'], 'J': _D['R'],  # guard
    'h': _D['h'], 'H': _D['H'], 'b': _D['b'], 'B': _D['B'],          # wrapped grip
}, {
    'e': 1, 'l': 2, 'm': 3, 'd': 3, 'K': 1.5,
    'R': 2, 'Y': 2.5,
    'T': 2, 'U': 2, 't': 2, 'u': 2,
    'E': 2.5, 'L': 3.5, 'M': 3.5, 'D': 3.5, 'k': 3, 'J': 3,
    'h': 2.5, 'H': 2.5, 'b': 2.5, 'B': 2.5,
}, [
    ".......e.......",
    "......eRd......",
    "......eYd......",
    ".....eRYRd.....",
    ".....eRYRd.....",
    "....elRYRdK....",
    "....elRYRdK....",
    "..TelmRYRmdK...",
    ".TTelmRYRmdK...",
    ".U.elmRYRmdKt..",
    "...elmRYRmdKtt.",
    "..TelmRYRmdK.u.",
    ".TTelmRYRmdK...",
    ".U.elmRYRmdKt..",
    "...elmRYRmdKtt.",
    "..TelmRYRmdK.u.",
    ".TTelmRYRmdK...",
    ".U.elmRYRmdKt..",
    "...elmRYRmdKtt.",
    "...elmRYRmdK.u.",
    "..ELLMMJMMDDk..",
    ".ELLMMMJMMMDDk.",
    "EL..LMMMMMD..Dk",
    "E.....DMD.....k",
    "......HBb......",
    "......Bbh......",
    "......bhH......",
    "......hHB......",
    "......HBb......",
    "......lem......",
    "......lmd......",
    ".......m.......",
], glow='RYJ', grip=(8, 6), first_person=BIG_FIRST_PERSON)

# --- Darius's Axe: the Black Cleaver ---------------------------------------------------------
# A huge near-black axe: the main bit flares from the haft to a broad, convex polished edge, its
# plates split by sunk black seams fanning out from the haft, a cold grey sheen on each. Black
# thorns along its top and bottom, a smaller dull back bit, a spike above the haft, and a long
# brown haft with iron bands. Even width, so the haft is two pixels and centred.
_C = sprite_palette('darius_axe')
weapon('darius_axe', {
    'c': _C['c'], 'd': _C['d'], 'm': _C['m'], 's': _C['s'], 'K': _C['K'],  # plates, seams
    'l': _C['l'], 'w': _C['w'],                                      # the polished edge
    't': _C['K'],                                                    # thorns
    'b': _C['b'], 'B': _C['B'], 'h': _C['m'],                        # haft, iron bands
    'x': _C['m'], 'X': _C['d'],                                      # the head's core round the haft
}, {
    'c': 3, 'd': 3, 'm': 3, 's': 3, 'K': 2,
    'l': 2, 'w': 1,
    't': 2,
    'b': 2, 'B': 2, 'h': 2.5,
    'x': 3.5, 'X': 3.5,
}, [
    "........lX...t.w..",
    "......t.xX.t.sslw.",
    "......t.xX.tssmlw.",
    "......ldxX.ssmmdlw",
    ".....lmdxXsmmmKKlw",
    "....lmsdxXmdKKsslw",
    "...lmsdcxXKKssmmlw",
    "..lmmddcxXssmKmdlw",
    "..lmddccxXsmmdKdlw",
    "...lmdccxXKKmddclw",
    "....lmdcxXssKKdclw",
    ".....ldcxXsmmdKKlw",
    "......lcxX.mddcclw",
    "......t.xX.tddclw.",
    "......t.xX.t.cclw.",
    "........xX...t.w..",
    ".......hxXh.......",
    "........Bb........",
    "........Bb........",
    "........Bb........",
    "........hh........",
    "........Bb........",
    "........Bb........",
    "........Bb........",
    "........Bb........",
    "........hh........",
    "........Bb........",
    "........Bb........",
    "........Bb........",
    "........Bb........",
    ".......hhhh.......",
    "........XX........",
], grip=(8, 6), first_person=BIG_FIRST_PERSON)


# --- Building -------------------------------------------------------------------------------
def texture(name):
    m = MODELS[name]
    img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    for r, row in enumerate(m['rows']):
        for c, ch in enumerate(row):
            if ch != '.':
                img.putpixel((c, r), art.hexc(m['palette'][ch]))
    return img


def rectangles(rows, key):
    """Greedy merge: cover the cells whose key is not None with rectangles of one key each,
    widest first along each row, then as many rows down as stay identical."""
    h, w = len(rows), len(rows[0])
    done = [[False] * w for _ in range(h)]
    out = []
    for r in range(h):
        for c in range(w):
            k = key(r, c)
            if done[r][c] or k is None:
                continue
            c1 = c
            while c1 + 1 < w and not done[r][c1 + 1] and key(r, c1 + 1) == k:
                c1 += 1
            r1 = r
            while r1 + 1 < h and all(not done[r1 + 1][x] and key(r1 + 1, x) == k for x in range(c, c1 + 1)):
                r1 += 1
            for y in range(r, r1 + 1):
                for x in range(c, c1 + 1):
                    done[y][x] = True
            out.append((k, c, r, c1 + 1, r1 + 1))
    return out


def elements(name):
    m = MODELS[name]
    rows, depth, glow = m['rows'], m['depth'], m['glow']
    h, w = len(rows), len(rows[0])
    x0 = 8 - w / 2.0
    s = 16.0 / TEX

    def cell(r, c):
        return rows[r][c] if 0 <= r < h and 0 <= c < w else '.'

    def thick(r, c):
        ch = cell(r, c)
        return 0.0 if ch == '.' else depth[ch]

    def key(r, c):
        ch = cell(r, c)
        return None if ch == '.' else (depth[ch], ch in glow)

    def num(v):
        v = round(v, 4)
        return int(v) if v == int(v) else v

    out = []
    for (t, lit), c0, r0, c1, r1 in rectangles(rows, key):
        fr = [x0 + c0, h - r1, 8 - t / 2]
        to = [x0 + c1, h - r0, 8 + t / 2]
        faces = {
            'north': {'uv': [c1 * s, r0 * s, c0 * s, r1 * s], 'texture': '#0'},
            'south': {'uv': [c0 * s, r0 * s, c1 * s, r1 * s], 'texture': '#0'},
        }
        # A side shows if any cell along it has a thinner neighbour; the part hidden inside a
        # thicker neighbour is simply overdrawn. Its texture is that edge's texels, stretched.
        if any(thick(r, c1) < t for r in range(r0, r1)):
            faces['east'] = {'uv': [(c1 - 0.75) * s, r0 * s, (c1 - 0.25) * s, r1 * s], 'texture': '#0'}
        if any(thick(r, c0 - 1) < t for r in range(r0, r1)):
            faces['west'] = {'uv': [(c0 + 0.25) * s, r0 * s, (c0 + 0.75) * s, r1 * s], 'texture': '#0'}
        if any(thick(r0 - 1, c) < t for c in range(c0, c1)):
            faces['up'] = {'uv': [c0 * s, (r0 + 0.25) * s, c1 * s, (r0 + 0.75) * s], 'texture': '#0'}
        if any(thick(r1, c) < t for c in range(c0, c1)):
            faces['down'] = {'uv': [c0 * s, (r1 - 0.75) * s, c1 * s, (r1 - 0.25) * s], 'texture': '#0'}
        for f in faces.values():
            f['uv'] = [num(v) for v in f['uv']]
        el = {'from': [num(v) for v in fr], 'to': [num(v) for v in to], 'faces': faces}
        if lit:
            el['neoforge_data'] = dict(GLOW)
        out.append(el)
    return out


def model_json(name, icon_texture):
    m = MODELS[name]
    return {
        'credit': 'Project Ascension; generated by tools/generate-combat-models.py, edit that instead',
        'texture_size': [TEX, TEX],
        'gui_light': 'front',
        'textures': {'0': 'ascension_combat:item/%s_3d' % name, 'particle': 'ascension_combat:item/' + icon_texture},
        'elements': elements(name),
        'display': display(name),
    }


def display(name):
    m = MODELS[name]
    d = upright_handheld(m['grip'], *m['scale'])
    if m['first_person']:
        d['firstperson_righthand'] = d['firstperson_lefthand'] = m['first_person']
    return d


# --- Display transforms ---------------------------------------------------------------------
def upright_handheld(grip, scale_tp=0.85, scale_fp=0.68):
    """Vanilla's handheld transforms, turned for a model drawn upright instead of diagonal.

    ItemTransform is T . Rx . Ry . Rz . S about the model's centre, so an extra Rz(-45), which
    tips our upright model onto vanilla's diagonal, folds exactly into Z. Then translate so the
    grip lands where vanilla's handle is: t = t_vanilla + s . R . (handle - Rz(-45) . grip), with
    handle and grip measured from the centre (8, 8, 8)."""
    import math
    vanilla_handle = (3.5 - 8, 3.5 - 8, 0)  # a vanilla sword's handle, model pixels
    g = (grip[0] - 8, grip[1] - 8, 0)
    a = math.radians(-45)
    turned = (g[0] * math.cos(a) - g[1] * math.sin(a), g[0] * math.sin(a) + g[1] * math.cos(a), 0)
    shift = [vanilla_handle[i] - turned[i] for i in range(3)]

    def rot(rx, ry, rz, v):
        def rx_(v, d):
            c, s = math.cos(math.radians(d)), math.sin(math.radians(d))
            return (v[0], c * v[1] - s * v[2], s * v[1] + c * v[2])

        def ry_(v, d):
            c, s = math.cos(math.radians(d)), math.sin(math.radians(d))
            return (c * v[0] + s * v[2], v[1], -s * v[0] + c * v[2])

        def rz_(v, d):
            c, s = math.cos(math.radians(d)), math.sin(math.radians(d))
            return (c * v[0] - s * v[1], s * v[0] + c * v[1], v[2])
        return rx_(ry_(rz_(v, rz), ry), rx)

    out = {}
    for ctx, (r, t, sc) in {'thirdperson': ([0, -90, 55], [0, 4.0, 0.5], scale_tp),
                            'firstperson': ([0, -90, 25], [1.13, 3.2, 1.13], scale_fp)}.items():
        rz = r[2] - 45
        moved = rot(r[0], r[1], r[2], shift)
        tr = [round(max(-80, min(80, t[i] + sc * moved[i])), 2) for i in range(3)]
        # The left hand takes the same values: ItemTransform mirrors them for that hand.
        out[ctx + '_righthand'] = out[ctx + '_lefthand'] = {'rotation': [r[0], r[1], rz], 'translation': tr,
                                                            'scale': [sc] * 3}
    return out


# --- Output ---------------------------------------------------------------------------------
def write():
    tex_dir = os.path.join(ASSETS, 'textures', 'item')
    model_dir = os.path.join(ASSETS, 'models', 'item')
    for name in MODELS:
        texture(name).save(os.path.join(tex_dir, name + '_3d.png'))
        icon = art.HELD_3D[name]
        with open(os.path.join(model_dir, name + '_3d.json'), 'w', newline='\n') as f:
            f.write(json.dumps(model_json(name, icon), indent=2) + '\n')


def previews():
    """design/combat-items/3d/: each model's pixel map at 10x, and its in-hand render."""
    out_dir = os.path.join(ROOT, 'design', 'combat-items', '3d')
    os.makedirs(out_dir, exist_ok=True)
    k = 10
    maps = [texture(n) for n in MODELS]
    sheet = Image.new('RGBA', (len(maps) * (TEX * k + 20) + 20, TEX * k + 50), (60, 62, 70, 255))
    d = ImageDraw.Draw(sheet)
    for i, (n, im) in enumerate(zip(MODELS, maps)):
        x = 20 + i * (TEX * k + 20)
        sheet.alpha_composite(im.resize((TEX * k, TEX * k), Image.NEAREST), (x, 30))
        d.text((x, 10), n + ' (pixel map = texture; glow: %s)' % (MODELS[n]['glow'] or '-'), fill=(230, 230, 230, 255))
    sheet.save(os.path.join(out_dir, 'pixel-maps.png'))
    render = _load('render_item_model', 'render-item-model.py')
    for n in MODELS:
        model = render.load_model('ascension_combat:item/' + n)
        render.sheet(model, n, swing='greatsword' if n == 'veigos_blade' else 'vanilla').save(os.path.join(out_dir, n + '_hand.png'))


if __name__ == '__main__':
    import sys
    write()
    if '--no-previews' not in sys.argv:
        previews()
    print('wrote combat 3D models and design/combat-items/3d/ previews')
