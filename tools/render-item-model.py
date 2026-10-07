"""Preview an item model the way Minecraft draws it in hand, before anyone starts the game.

Run from the repository root:
    python tools/render-item-model.py ascension_combat:item/veigos_blade [--out DIR] [--name NAME]
        [--swing vanilla|greatsword]
    python tools/render-item-model.py <model> --fit RX,RY,SCALE --point X,Y,Z:U,V --point ...
        (prints a first-person display entry that puts those model points on those screen spots)

Writes <name>_hand.png to DIR (default: design/combat-items/3d/): first person by day and by
night, two third-person views of a stand-in player, a three-angle turntable of the bare model and
a strip of the first-person swing. Our own art only; no vanilla texture is read.

It resolves the model the way the game does: parents, texture variables, NeoForge's
separate_transforms (the base model is what the hands hold) and item_layers, then builds quads
from cuboid elements, or extrudes item/generated layers one pixel deep. A model parenting
builtin/entity is drawn the way the combat module's WeaponRenderer draws it: from
weapon_models/<item>.json (parts turned by any angle; WeaponModelTest pins the conventions),
once with its texture and once with its glow texture, added full-bright like the "eyes" pass. Each view applies the
game's own transform chain (checked against the 1.21.1 sources, 2026-10-07):

- First person, right hand: camera space, a fixed 70 degree vertical field of view,
  T(0.56, -0.52, -0.72) . display . T(-0.5, -0.5, -0.5) on the model in block units.
  (ItemInHandRenderer.applyItemArmTransform; VeigosBladeClient keeps the same rest pose.)
- Third person: the entity's pose (Ry(180 - yaw) . S(-1, -1, 1) . S(0.9375) . T(0, -1.501, 0)),
  the right arm (pivot (-5, 2, 0), xRot -pi/10 holding an item), then ItemInHandLayer's
  Rx(-90) . Ry(180) . T(1/16, 0.125, -0.625), then display.
- display is ItemTransform: T(translation / 16) . Rx . Ry . Rz . S, about the model's centre.

Light is the entity shader's: two directions, (0.2, 1, -0.7) and (-0.2, 1, 0.7), each
contributing 0.6 x max(0, n . l), plus 0.4 ambient, capped at 1. Items in hand take their
normals in world space, so in game the shading shifts as you turn; the first-person view
assumes you face north, level. Element `shade` does not matter for items (nothing in the item
path reads it). Night darkens everything to the lightmap's level except faces marked emissive
(neoforge_data block light), which keep full light, as they do in game.

Previews prove framing and proportion, not that the model works in game (ADR-0008). Needs
numpy and Pillow.
"""
import argparse
import glob
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))

# Built-in parents the game supplies. Only their display transforms matter here.
GENERATED_DISPLAY = {
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
    'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
    'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 3, 1], 'scale': [0.55, 0.55, 0.55]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'fixed': {'rotation': [0, 180, 0], 'scale': [1, 1, 1]},
}
HANDHELD_DISPLAY = dict(GENERATED_DISPLAY, **{
    'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4.0, 0.5], 'scale': [0.85, 0.85, 0.85]},
    'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4.0, 0.5], 'scale': [0.85, 0.85, 0.85]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
})
BUILTIN = {
    'minecraft:item/generated': {'generated': True, 'display': GENERATED_DISPLAY},
    'minecraft:item/handheld': {'generated': True, 'display': HANDHELD_DISPLAY},
    'neoforge:item/default': {'generated': True, 'display': GENERATED_DISPLAY},
    'neoforge:item/default-tool': {'generated': True, 'display': HANDHELD_DISPLAY},
    'minecraft:builtin/generated': {'generated': True},
    'minecraft:builtin/entity': {'entity': True},
}

LIGHT0 = np.array([0.2, 1.0, -0.7]) / np.linalg.norm([0.2, 1.0, -0.7])
LIGHT1 = np.array([-0.2, 1.0, 0.7]) / np.linalg.norm([-0.2, 1.0, 0.7])
NIGHT = 0.22  # lightmap brightness outdoors at midnight, roughly


# --- Resolving ------------------------------------------------------------------------------
def split_id(rid, kind):
    ns, _, path = rid.partition(':') if ':' in rid else ('minecraft', '', rid)
    return ns, path


def find_asset(ns, rel):
    hits = glob.glob(os.path.join(ROOT, 'modules', '**', 'src', 'main', 'resources', 'assets', ns, rel),
                     recursive=True)
    return hits[0] if hits else None


def load_model(rid):
    """The model JSON with its parent chain flattened: elements, textures, display, and whether
    it ends at the generated-item parent. A model ending at builtin/entity is drawn in game by
    the combat module's WeaponRenderer from weapon_models/<item>.json, which is loaded too."""
    model = _load_model(rid)
    if model.get('entity') and 'weapon' not in model:
        ns, path = split_id(rid if ':' in rid else 'minecraft:' + rid, 'models')
        item = path.split('/')[-1]
        item = item[:-3] if item.endswith('_3d') else item
        p = find_asset(ns, os.path.join('weapon_models', item + '.json'))
        if not p:
            raise FileNotFoundError('weapon model for ' + rid)
        with open(p) as f:
            model['weapon'] = json.load(f)
    return model


def _load_model(rid):
    rid = rid if ':' in rid else 'minecraft:' + rid
    if rid in BUILTIN:
        return dict(BUILTIN[rid], textures={})
    ns, path = split_id(rid, 'models')
    p = find_asset(ns, os.path.join('models', path + '.json'))
    if not p:
        raise FileNotFoundError(rid)
    with open(p) as f:
        m = json.load(f)
    return resolve(m)


def resolve(m):
    if m.get('loader') == 'neoforge:separate_transforms':
        # The hands use the base model; the game asks the wrapper for nothing else in hand.
        return resolve(m['base'])
    parent = _load_model(m['parent']) if 'parent' in m else {'textures': {}}
    out = dict(parent)
    out['textures'] = dict(parent.get('textures', {}), **m.get('textures', {}))
    if 'elements' in m:
        out['elements'] = m['elements']
        out['generated'] = False
    if 'display' in m:
        out['display'] = dict(parent.get('display', {}), **m['display'])
    if m.get('loader') == 'neoforge:item_layers':
        out['layer_data'] = m.get('neoforge_data', {}).get('layers', {})
    return out


def texture(model, ref, cache={}):
    seen = 0
    while ref.startswith('#'):
        ref = model['textures'].get(ref[1:], '')
        seen += 1
        if seen > 10:
            break
    if ref not in cache:
        ns, path = split_id(ref, 'textures')
        p = find_asset(ns, os.path.join('textures', path + '.png'))
        if not p:
            raise FileNotFoundError('texture ' + ref)
        cache[ref] = np.asarray(Image.open(p).convert('RGBA'), dtype=np.float32) / 255.0
    return cache[ref]


# --- Geometry -------------------------------------------------------------------------------
# Corner order per face, as vanilla's FaceInfo; UV corner i is (u0,v0),(u0,v1),(u1,v1),(u1,v0).
FACE_CORNERS = {
    'down': [(0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)],
    'up': [(0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)],
    'north': [(1, 1, 0), (1, 0, 0), (0, 0, 0), (0, 1, 0)],
    'south': [(0, 1, 1), (0, 0, 1), (1, 0, 1), (1, 1, 1)],
    'west': [(0, 1, 0), (0, 0, 0), (0, 0, 1), (0, 1, 1)],
    'east': [(1, 1, 1), (1, 0, 1), (1, 0, 0), (1, 1, 0)],
}


def default_uv(face, fr, to):
    return {
        'down': [fr[0], 16 - to[2], to[0], 16 - fr[2]],
        'up': [fr[0], fr[2], to[0], to[2]],
        'north': [16 - to[0], 16 - to[1], 16 - fr[0], 16 - fr[1]],
        'south': [fr[0], 16 - to[1], to[0], 16 - fr[1]],
        'west': [fr[2], 16 - to[1], to[2], 16 - fr[1]],
        'east': [16 - to[2], 16 - to[1], 16 - fr[2], 16 - fr[1]],
    }[face]


def rot_axis(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    if axis == 'x':
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == 'y':
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


class Quad:
    __slots__ = ('pos', 'uv', 'tex', 'color', 'emissive', 'additive')

    def __init__(self, pos, uv=None, tex=None, color=None, emissive=False, additive=False):
        self.pos, self.uv, self.tex, self.color, self.emissive = pos, uv, tex, color, emissive
        self.additive = additive


def element_quads(model):
    quads = []
    for el in model['elements']:
        fr, to = np.array(el['from'], float), np.array(el['to'], float)
        el_glow = el.get('neoforge_data', {}).get('block_light', 0) > 0
        rot = el.get('rotation')
        if rot:
            R = rot_axis(rot['axis'], rot['angle'])
            if rot.get('rescale'):
                k = 1 / math.cos(math.radians(abs(rot['angle'])))
                sc = np.array([k, k, k])
                sc['xyz'.index(rot['axis'])] = 1
                R = R @ np.diag(sc)
            origin = np.array(rot['origin'], float)
        for face, fd in el.get('faces', {}).items():
            corners = np.array([[(to if c else fr)[i] for i, c in enumerate(cc)] for cc in FACE_CORNERS[face]])
            if rot:
                corners = (corners - origin) @ R.T + origin
            uv = fd.get('uv') or default_uv(face, el['from'], el['to'])
            r = fd.get('rotation', 0) // 90
            uvs = []
            for i in range(4):
                j = (i + r) % 4
                uvs.append((uv[0] if j in (0, 1) else uv[2], uv[1] if j in (0, 3) else uv[3]))
            glow = el_glow or fd.get('neoforge_data', {}).get('block_light', 0) > 0
            quads.append(Quad(corners, np.array(uvs) / 16.0, texture(model, fd['texture']), emissive=glow))
    return quads


def generated_quads(model):
    """item/generated: each layer extruded one pixel deep, z 7.5..8.5, per opaque pixel."""
    quads = []
    layer_data = model.get('layer_data', {})
    i = 0
    while 'layer%d' % i in model['textures']:
        img = texture(model, '#layer%d' % i)
        glow = layer_data.get(str(i), {}).get('block_light', 0) > 0
        h, w = img.shape[:2]
        px = 16.0 / w
        solid = img[:, :, 3] > 0.1
        for y in range(h):
            for x in range(w):
                if not solid[y, x]:
                    continue
                c = img[y, x]
                x0, x1, y1, y0 = x * px, (x + 1) * px, 16 - y * px, 16 - (y + 1) * px
                z0, z1 = 7.5, 8.5
                faces = [[(x0, y1, z1), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1)],
                         [(x1, y1, z0), (x1, y0, z0), (x0, y0, z0), (x0, y1, z0)]]
                if x == 0 or not solid[y, x - 1]:
                    faces.append([(x0, y1, z0), (x0, y0, z0), (x0, y0, z1), (x0, y1, z1)])
                if x == w - 1 or not solid[y, x + 1]:
                    faces.append([(x1, y1, z1), (x1, y0, z1), (x1, y0, z0), (x1, y1, z0)])
                if y == 0 or not solid[y - 1, x]:
                    faces.append([(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)])
                if y == h - 1 or not solid[y + 1, x]:
                    faces.append([(x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1)])
                for f in faces:
                    quads.append(Quad(np.array(f, float), color=c, emissive=glow))
        i += 1
    return quads


def weapon_quads(weapon):
    """A weapon_models file as WeaponModel reads it: parts turned about their pivots (Z . Y . X,
    X first), cubes with texel UVs, drawn once with the texture and once more with the glow
    texture through the additive, full-bright "eyes" pass."""
    def tex(rid):
        ns, path = split_id(rid, 'textures')
        p = find_asset(ns, path)
        if not p:
            raise FileNotFoundError('texture ' + rid)
        return np.asarray(Image.open(p).convert('RGBA'), dtype=np.float32) / 255.0
    base = tex(weapon['texture'])
    glow = tex(weapon['glow_texture']) if 'glow_texture' in weapon else None
    tw, th = weapon['texture_size']
    out = []

    def part(pj, parent):
        pv = np.array(pj.get('pivot', [0, 0, 0]), float)
        rx, ry, rz = pj.get('rotation', [0, 0, 0])
        m = parent @ T(*pv) @ Rdeg('z', rz) @ Rdeg('y', ry) @ Rdeg('x', rx) @ T(*-pv)
        for cube in pj.get('cubes', []):
            fr, to = cube['from'], cube['to']
            for face, uv in cube['faces'].items():
                corners = np.array([[(to if c else fr)[i] for i, c in enumerate(cc)] for cc in FACE_CORNERS[face]],
                                   float)
                pos = (np.c_[corners, np.ones(4)] @ m.T)[:, :3]
                u0, v0, u1, v1 = uv[0] / tw, uv[1] / th, uv[2] / tw, uv[3] / th
                uvs = np.array([(u0, v0), (u0, v1), (u1, v1), (u1, v0)])
                out.append(Quad(pos, uvs, base))
                if glow is not None:
                    out.append(Quad(pos, uvs, glow, emissive=True, additive=True))
        for child in pj.get('children', []):
            part(child, m)
    for pj in weapon['parts']:
        part(pj, np.eye(4))
    return out


def model_quads(model):
    if 'weapon' in model:
        return weapon_quads(model['weapon'])
    return element_quads(model) if not model.get('generated', False) else generated_quads(model)


# --- Transforms (4x4, column vectors) -------------------------------------------------------
def T(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def S(x, y=None, z=None):
    return np.diag([x, x if y is None else y, x if z is None else z, 1.0])


def Rdeg(axis, deg):
    m = np.eye(4)
    m[:3, :3] = rot_axis(axis, deg)
    return m


def display(model, ctx, left=False):
    d = model.get('display', {}).get(ctx)
    if d is None:
        return np.eye(4)
    rx, ry, rz = d.get('rotation', [0, 0, 0])
    tx, ty, tz = [max(-5.0, min(5.0, v / 16.0)) for v in d.get('translation', [0, 0, 0])]
    s = d.get('scale', [1, 1, 1])
    if left:
        ry, rz, tx = -ry, -rz, -tx
    return T(tx, ty, tz) @ Rdeg('x', rx) @ Rdeg('y', ry) @ Rdeg('z', rz) @ S(*s)


def item_matrix(model, ctx):
    """Model pixels -> the space the display context hands the item to."""
    return display(model, ctx) @ T(-0.5, -0.5, -0.5) @ S(1 / 16.0)


FIRST_PERSON_HAND = T(0.56, -0.52, -0.72)


def _ease(v):
    t = min(1.0, max(0.0, v))
    return t * t * (3 - 2 * t)


def vanilla_hand(p):
    """Vanilla's first-person swing at progress p (right hand): ItemInHandRenderer's sway, then
    applyItemArmTransform and applyItemArmAttackTransform."""
    f5 = -0.4 * math.sin(math.sqrt(p) * math.pi)
    f6 = 0.2 * math.sin(math.sqrt(p) * math.pi * 2)
    f10 = -0.2 * math.sin(p * math.pi)
    f = math.sin(p * p * math.pi)
    f1 = math.sin(math.sqrt(p) * math.pi)
    return (T(f5, f6, f10) @ FIRST_PERSON_HAND @ Rdeg('y', 45 - 20 * f) @ Rdeg('z', -20 * f1)
            @ Rdeg('x', -80 * f1) @ Rdeg('y', -45))


def greatsword_hand(p):
    """VeigosBladeClient's first-person hand pose at swing progress p (right hand): wind-up, cut
    and recovery in place of vanilla's swing. A copy of its arithmetic; keep the two in step."""
    wind = _ease(p / 0.18) * (1 - _ease((p - 0.18) / 0.18))
    cut = _ease((p - 0.18) / 0.28) * (1 - _ease((p - 0.55) / 0.45))
    return (T(0.56 + 0.08 * wind - 0.32 * cut, -0.52 + 0.06 * wind - 0.10 * cut, -0.72 + 0.08 * wind - 0.16 * cut)
            @ Rdeg('y', 10 * wind - 28 * cut) @ Rdeg('z', -14 * wind + 90 * cut) @ Rdeg('x', -25 * cut))


def entity_pose(yaw=0.0):
    """Model space of a player standing at the origin facing yaw (0 = south, +z)."""
    return Rdeg('y', 180 - yaw) @ S(-1, -1, 1) @ S(0.9375) @ T(0, -1.501, 0)


ARM_X_ROT = -math.pi / 10


class Body:
    """A humanoid's pose for the preview, as HumanoidModel holds it: arm rotations (x, y, z) in
    degrees, the body's twist, the legs' swing, and the held weapon's turn about its grip."""

    def __init__(self, right=(math.degrees(ARM_X_ROT), 0, 0), left=(0, 0, 0), twist=0.0, legs=0.0,
                 item=(0, 0, 0), grip=(8, 8, 8)):
        self.right, self.left, self.twist, self.legs = right, left, twist, legs
        self.item, self.grip = item, grip

    def arm(self, side):
        """ModelPart.translateAndRotate: the pivot (following the body's twist) then Z . Y . X."""
        tw = math.radians(self.twist)
        x, z = (-math.cos(tw) * 5, math.sin(tw) * 5) if side == 'right' else (math.cos(tw) * 5, -math.sin(tw) * 5)
        rx, ry, rz = self.right if side == 'right' else self.left
        return T(x / 16, 2 / 16, z / 16) @ Rdeg('z', rz) @ Rdeg('y', ry) @ Rdeg('x', rx)


VANILLA_HOLD = Body()


def hold_pose(hold, state='idle', t=0.0, attack=0, walk=0.0):
    """WeaponPose's arithmetic for a right-handed holder: the hold's idle pose (walk: the vanilla
    arm swing it keeps, 0 standing) or attack `attack` at swing progress t, eased between keyframes
    as HoldAnimation does. A copy, like greatsword_hand(); keep the two in step."""
    def key_pose(k):
        return (tuple(k.get('right_arm', (0, 0, 0))), tuple(k['left_arm']) if 'left_arm' in k else None,
                k.get('body_twist', 0.0), tuple(k.get('item', (0, 0, 0))))
    if state == 'idle':
        r, l, tw, it = key_pose(hold['idle'])
        swing = math.degrees(walk) * hold.get('walk_swing', 1.0)
        r = (r[0] + swing, r[1], r[2])
        l = l or (-math.degrees(walk), 0, 0)  # the free arm swings as vanilla's
        return Body(r, l, tw, walk, it, hold.get('grip', (8, 8, 8)))
    keys = hold['attacks'][attack]
    k0 = keys[0]
    pose = key_pose(k0)
    for a, b in zip(keys, keys[1:]):
        if t <= a['t']:
            break
        if t <= b['t']:
            f = (t - a['t']) / max(1e-6, b['t'] - a['t'])
            f = f * f * (3 - 2 * f)
            pa, pb = key_pose(a), key_pose(b)
            lerp = lambda u, v: tuple(x + (y - x) * f for x, y in zip(u, v))
            left = lerp(pa[1], pb[1]) if pa[1] and pb[1] else (pa[1] if f < 0.5 else pb[1])
            pose = (lerp(pa[0], pb[0]), left, pa[2] + (pb[2] - pa[2]) * f, lerp(pa[3], pb[3]))
            break
        pose = key_pose(b)
    r, l, tw, it = pose
    return Body(r, l or (0, 0, 0), tw, 0.0, it, hold.get('grip', (8, 8, 8)))


def third_person_item(body=VANILLA_HOLD):
    return entity_pose() @ body.arm('right') @ Rdeg('x', -90) @ Rdeg('y', 180) @ T(1 / 16, 0.125, -0.625)


def item_turn(body):
    """WeaponRenderer's extra turn about the grip, between the display transform and the model."""
    g = np.array(body.grip, float) / 16
    rx, ry, rz = body.item
    return T(*g) @ Rdeg('z', rz) @ Rdeg('y', ry) @ Rdeg('x', rx) @ T(*-g)


def player_quads(body=VANILLA_HOLD):
    """A plain stand-in player (Steve's proportions, flat colours), in its model space."""
    skin, shirt, pants, hair, shoe = (0.78, 0.6, 0.47, 1), (0.23, 0.43, 0.66, 1), (0.16, 0.23, 0.42, 1), \
        (0.25, 0.17, 0.1, 1), (0.3, 0.3, 0.32, 1)
    out = []

    def box(m, lo, hi, color_fn):
        lo, hi = np.array(lo, float), np.array(hi, float)
        for face, cc in FACE_CORNERS.items():
            corners = np.array([[(hi if c else lo)[i] for i, c in enumerate(k)] for k in cc]) / 16.0
            ph = np.c_[corners, np.ones(4)] @ m.T
            out.append(Quad(ph[:, :3], color=np.array(color_fn(face))))
    pose = entity_pose()
    box(pose, (-4, -8, -4), (4, 0, 4), lambda f: hair if f in ('up', 'south') else skin)
    box(pose @ Rdeg('y', body.twist), (-4, 0, -2), (4, 12, 2), lambda f: shirt)
    box(pose @ body.arm('right'), (-3, -2, -2), (1, 10, 2), lambda f: skin)
    box(pose @ body.arm('left'), (-1, -2, -2), (3, 10, 2), lambda f: skin)
    leg = math.degrees(body.legs) * 1.4
    box(pose @ T(-1.9 / 16, 12 / 16, 0) @ Rdeg('x', -leg), (-2, 0, -2), (2, 12, 2),
        lambda f: shoe if f == 'down' else pants)
    box(pose @ T(1.9 / 16, 12 / 16, 0) @ Rdeg('x', leg), (-2, 0, -2), (2, 12, 2),
        lambda f: shoe if f == 'down' else pants)
    return out


def ground_quads(half=2.5, colour=(0.42, 0.45, 0.36, 1)):
    """The ground the stand-in player stands on (y = 0), so a trailing blade shows where it meets it."""
    c = np.array(colour)
    return [Quad(np.array([(-half, 0, -half), (-half, 0, half), (half, 0, half), (half, 0, -half)], float),
                 color=c)]


def transformed(quads, m):
    out = []
    for q in quads:
        p = np.c_[q.pos, np.ones(len(q.pos))] @ m.T
        out.append(Quad(p[:, :3], q.uv, q.tex, q.color, q.emissive, q.additive))
    return out


# --- Cameras and rasterising ----------------------------------------------------------------
def perspective(fov_y, aspect, near=0.05, far=100.0):
    f = 1 / math.tan(math.radians(fov_y) / 2)
    m = np.zeros((4, 4))
    m[0, 0], m[1, 1] = f / aspect, f
    m[2, 2], m[2, 3] = (far + near) / (near - far), 2 * far * near / (near - far)
    m[3, 2] = -1
    return m


def look_at(eye, target, up=(0, 1, 0)):
    eye, target, up = (np.array(v, float) for v in (eye, target, up))
    f = target - eye
    f /= np.linalg.norm(f)
    r = np.cross(f, up)
    r /= np.linalg.norm(r)
    u = np.cross(r, f)
    m = np.eye(4)
    m[0, :3], m[1, :3], m[2, :3] = r, u, -f
    m[:3, 3] = -m[:3, :3] @ eye
    return m


def ortho(half_w, half_h):
    m = np.eye(4)
    m[0, 0], m[1, 1], m[2, 2] = 1 / half_w, 1 / half_h, -0.01
    return m


def shade(normal, emissive, night):
    l0, l1 = max(0.0, normal @ LIGHT0), max(0.0, normal @ LIGHT1)
    light = min(1.0, (l0 + l1) * 0.6 + 0.4)
    return light * (1.0 if emissive or not night else NIGHT)


def rasterise(quads, proj, view, size, background, night=False, light_view=None):
    """quads in world space; light_view rotates normals into the lights' frame (None: world)."""
    w, h = size
    color = np.array(background, dtype=np.float32)[:h, :w].copy()
    depth = np.full((h, w), np.inf, dtype=np.float32)
    pv = proj @ view
    for q in sorted(quads, key=lambda q: q.additive):
        p = q.pos
        n = np.cross(p[1] - p[0], p[2] - p[0])
        if np.linalg.norm(n) < 1e-12:
            n = np.cross(p[2] - p[0], p[3] - p[0])
        nl = np.linalg.norm(n)
        if nl < 1e-12:
            continue
        n /= nl
        ln = n if light_view is None else light_view[:3, :3] @ n
        k = 1.0 if q.additive else shade(ln, q.emissive, night)
        clip = np.c_[p, np.ones(4)] @ pv.T
        if np.any(clip[:, 3] <= 1e-4):
            continue
        ndc = clip[:, :3] / clip[:, 3:4]
        sx = (ndc[:, 0] + 1) * 0.5 * w
        sy = (1 - ndc[:, 1]) * 0.5 * h
        inv_w = 1 / clip[:, 3]
        for tri in ((0, 1, 2), (0, 2, 3)):
            _tri(color, depth, q, tri, sx, sy, ndc[:, 2], inv_w, k, w, h)
    return color


def _tri(color, depth, q, tri, sx, sy, sz, inv_w, k, w, h):
    a, b, c = tri
    x0, y0, x1, y1, x2, y2 = sx[a], sy[a], sx[b], sy[b], sx[c], sy[c]
    area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
    if abs(area) < 1e-9:
        return
    minx, maxx = max(int(math.floor(min(x0, x1, x2))), 0), min(int(math.ceil(max(x0, x1, x2))), w - 1)
    miny, maxy = max(int(math.floor(min(y0, y1, y2))), 0), min(int(math.ceil(max(y0, y1, y2))), h - 1)
    if minx > maxx or miny > maxy:
        return
    xs, ys = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
    w0 = ((x1 - xs) * (y2 - ys) - (x2 - xs) * (y1 - ys)) / area
    w1 = ((x2 - xs) * (y0 - ys) - (x0 - xs) * (y2 - ys)) / area
    w2 = 1 - w0 - w1
    eps = -1e-6
    inside = (w0 >= eps) & (w1 >= eps) & (w2 >= eps)
    if not inside.any():
        return
    z = w0 * sz[a] + w1 * sz[b] + w2 * sz[c]
    region = depth[miny:maxy + 1, minx:maxx + 1]
    test = inside & ((z <= region + 1e-4) if q.additive else (z < region))
    if not test.any():
        return
    if q.tex is not None:
        iw = w0 * inv_w[a] + w1 * inv_w[b] + w2 * inv_w[c]
        u = (w0 * q.uv[a, 0] * inv_w[a] + w1 * q.uv[b, 0] * inv_w[b] + w2 * q.uv[c, 0] * inv_w[c]) / iw
        v = (w0 * q.uv[a, 1] * inv_w[a] + w1 * q.uv[b, 1] * inv_w[b] + w2 * q.uv[c, 1] * inv_w[c]) / iw
        th, tw = q.tex.shape[:2]
        tx = np.clip((u * tw).astype(int), 0, tw - 1)
        ty = np.clip((v * th).astype(int), 0, th - 1)
        texel = q.tex[ty, tx]
        test &= texel[:, :, 3] > 0.1
        rgb = texel[:, :, :3]
    else:
        rgb = np.broadcast_to(q.color[:3], xs.shape + (3,))
    if q.additive:
        color[miny:maxy + 1, minx:maxx + 1][test] += rgb[test] * texel[:, :, 3:4][test] * k
        return
    region[test] = z[test]
    color[miny:maxy + 1, minx:maxx + 1][test] = rgb[test] * k


def background(size, kind):
    w, h = size
    img = np.zeros((h, w, 3), np.float32)
    if kind == 'day':
        top, horizon, ground = (0.47, 0.65, 1.0), (0.72, 0.82, 1.0), (0.36, 0.42, 0.2)
    elif kind == 'night':
        top, horizon, ground = (0.01, 0.01, 0.04), (0.04, 0.05, 0.1), (0.03, 0.04, 0.02)
    else:
        img[:] = (0.24, 0.25, 0.28)
        return img
    hz = int(h * 0.42)
    for y in range(hz):
        t = y / max(1, hz - 1)
        img[y] = np.array(top) * (1 - t) + np.array(horizon) * t
    img[hz:] = ground
    return img


def to_image(arr):
    return Image.fromarray((np.clip(arr, 0, 1) * 255 + 0.5).astype(np.uint8), 'RGB')


# --- Views ----------------------------------------------------------------------------------
def first_person(model, size=(960, 540), night=False, hand=FIRST_PERSON_HAND):
    quads = transformed(model_quads(model), hand @ item_matrix(model, 'firstperson_righthand'))
    proj = perspective(70, size[0] / size[1])
    return to_image(rasterise(quads, proj, np.eye(4), size, background(size, 'night' if night else 'day'), night))


def third_person(model, eye, target, size=(480, 540), body=None):
    body = body or VANILLA_HOLD
    if body is VANILLA_HOLD and model.get('weapon', {}).get('hold'):
        body = hold_pose(model['weapon']['hold'])
    m = third_person_item(body) @ display(model, 'thirdperson_righthand') @ T(-0.5, -0.5, -0.5) @ item_turn(body) \
        @ S(1 / 16.0)
    quads = ground_quads() + player_quads(body) + transformed(model_quads(model), m)
    proj = perspective(50, size[0] / size[1])
    return to_image(rasterise(quads, proj, look_at(eye, target), size, background(size, 'flat')))


def turntable(model, yaw, size=(300, 540)):
    """The bare model in its own pixels, centred, spun about the vertical through (8, y, 8)."""
    quads = model_quads(model)
    pts = np.concatenate([q.pos for q in quads])
    lo, hi = pts.min(0), pts.max(0)
    centre = (lo + hi) / 2
    m = Rdeg('x', 12) @ Rdeg('y', yaw) @ T(*-centre)
    half_h = max(hi - lo) * 0.56
    proj = ortho(half_h * size[0] / size[1], half_h)
    # Lights stay fixed in the world while the model turns, so light the turned normals.
    return to_image(rasterise(transformed(quads, m), proj, np.eye(4), size, background(size, 'flat')))


SWING_FRAMES = (0.1, 0.22, 0.32, 0.42, 0.6, 0.8)
POSE_VIEW = ((-2.4, 1.7, 2.7), (0.0, 0.95, 0.3))


def pose_strip(model, width):
    """Third person, the hold animation: standing, walking, then each attack at the end of its
    wind-up, its strike and its follow-through (the keyframe times, as the generator wrote them)."""
    hold = model['weapon']['hold']
    frames = [('standing', hold_pose(hold)), ('walking', hold_pose(hold, walk=0.7))]
    for i, keys in enumerate(hold['attacks']):
        for k in keys[1:-1]:
            frames.append(('attack %d, t %.2f' % (i + 1, k['t']), hold_pose(hold, 'attack', k['t'], i)))
    cols = 6
    cw = (width - (cols - 1) * 4) // cols
    ch = int(cw * 1.1)
    rows = (len(frames) + cols - 1) // cols
    out = Image.new('RGB', (width, rows * (ch + 18) + 22), (30, 31, 36))
    d = ImageDraw.Draw(out)
    d.text((6, 4), 'third person: how it is held (WeaponPose), front-right view', fill=(230, 230, 230))
    for i, (label, body) in enumerate(frames):
        x, y = (i % cols) * (cw + 4), 22 + (i // cols) * (ch + 18)
        out.paste(third_person(model, POSE_VIEW[0], POSE_VIEW[1], (cw, ch), body), (x, y + 16))
        d.text((x + 4, y + 2), label, fill=(200, 200, 200))
    return out


def sheet(model, title, swing='vanilla'):
    """The full preview, ending in a strip of the first-person swing: 'vanilla', or 'greatsword'
    for VeigosBladeClient's."""
    fp_day, fp_night = first_person(model), first_person(model, night=True)
    tp_front = third_person(model, eye=(-1.9, 1.55, 2.6), target=(-0.1, 1.05, 0.2))
    tp_back = third_person(model, eye=(-1.6, 2.1, -2.8), target=(-0.1, 1.15, 0.2))
    turns = [turntable(model, a) for a in (0, 50, 90)]
    W = fp_day.width * 2 + 16
    H = fp_day.height + 540 + 70
    hand, label = {'vanilla': (vanilla_hand, 'vanilla'),
                   'greatsword': (greatsword_hand, 'VeigosBladeClient')}[swing]
    frames = [first_person(model, (320, 180), hand=hand(p)) for p in SWING_FRAMES]
    strip = Image.new('RGB', (W, 180 + 24), (30, 31, 36))
    for i, im in enumerate(frames):
        strip.paste(im, (i * (320 + 4), 20))
    ImageDraw.Draw(strip).text((6, 4), 'swing (%s): progress ' % label +
                               ' / '.join('%.2f' % p for p in SWING_FRAMES), fill=(230, 230, 230))
    H += strip.height
    poses = pose_strip(model, W) if model.get('weapon', {}).get('hold') else None
    if poses:
        H += poses.height
    out = Image.new('RGB', (W, H), (30, 31, 36))
    out.paste(strip, (0, H - strip.height))
    if poses:
        out.paste(poses, (0, H - strip.height - poses.height))
    d = ImageDraw.Draw(out)
    out.paste(fp_day, (0, 24))
    out.paste(fp_night, (fp_day.width + 16, 24))
    d.text((6, 6), title + '  -  first person, right hand, day / night (emissive faces stay lit)', fill=(230, 230, 230))
    y = fp_day.height + 50
    d.text((6, y - 18), 'third person (stand-in player, front and behind)        turntable 0 / 50 / 90 degrees, no display transform',
           fill=(230, 230, 230))
    x = 0
    for im in [tp_front, tp_back] + turns:
        out.paste(im, (x, y))
        x += im.width + 8
    return out


def fit_first_person(points, rx, ry, scale, size=(960, 540)):
    """A first-person display transform that puts model points on chosen screen spots.

    points: [((x, y, z) in model pixels, (u, v) in 0..1 of the screen, from the top left)].
    The X and Y rotation and the scale are fixed; the Z rotation and the translation are solved
    (Gauss-Newton, numeric Jacobian). Two points pin all four. Returns the display entry."""
    pts = np.array([p for p, _ in points], float)
    targ = np.array([t for _, t in points], float)
    proj = perspective(70, size[0] / size[1])

    def screen(params):
        rz, tx, ty, tz = params
        disp = T(tx / 16, ty / 16, tz / 16) @ Rdeg('x', rx) @ Rdeg('y', ry) @ Rdeg('z', rz) @ S(scale)
        ph = np.c_[pts, np.ones(len(pts))] @ (proj @ FIRST_PERSON_HAND @ disp @ T(-0.5, -0.5, -0.5) @ S(1 / 16)).T
        ndc = ph[:, :2] / ph[:, 3:4]
        return np.c_[(ndc[:, 0] + 1) / 2, (1 - ndc[:, 1]) / 2].ravel()

    p = np.array([10.0, 0.0, 0.0, 0.0])
    for _ in range(100):
        r = screen(p) - targ.ravel()
        jac = np.stack([(screen(p + d) - screen(p - d)) / 2e-4 for d in np.eye(4) * 1e-4], axis=1)
        step = np.linalg.lstsq(jac, -r, rcond=None)[0]
        p += step
        if np.abs(step).max() < 1e-7:
            break
    rz, tx, ty, tz = (round(float(v), 2) for v in p)
    return {'rotation': [rx, ry, rz], 'translation': [tx, ty, tz], 'scale': [scale] * 3}


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('model', help='e.g. ascension_combat:item/veigos_blade')
    ap.add_argument('--out', default=os.path.join(ROOT, 'design', 'combat-items', '3d'))
    ap.add_argument('--name', help='output file stem (default: the model path\'s last part)')
    ap.add_argument('--swing', choices=('vanilla', 'greatsword'), default='vanilla',
                    help='first-person swing to show: vanilla, or VeigosBladeClient\'s greatsword')
    ap.add_argument('--fit', metavar='RX,RY,SCALE', help='instead of a preview, print a first-person '
                    'display entry putting each --point on its screen spot (see fit_first_person)')
    ap.add_argument('--point', action='append', default=[], metavar='X,Y,Z:U,V',
                    help='for --fit: a model point and where on the screen it should sit (0..1)')
    args = ap.parse_args()
    if args.fit:
        rx, ry, sc = (float(v) for v in args.fit.split(','))
        pts = [(tuple(float(v) for v in a.split(':')[0].split(',')), tuple(float(v) for v in a.split(':')[1].split(',')))
               for a in args.point]
        print(json.dumps(fit_first_person(pts, rx, ry, sc)))
        return
    model = load_model(args.model)
    name = args.name or args.model.split('/')[-1]
    os.makedirs(args.out, exist_ok=True)
    path = os.path.join(args.out, name + '_hand.png')
    sheet(model, name, args.swing).save(path)
    print('wrote', path)


if __name__ == '__main__':
    main()
