"""Held 3D models for the combat weapons, drawn in game by the combat module's WeaponRenderer.

Run from the repository root:
    python tools/generate-combat-models.py [--no-previews]

Writes, under the combat module's assets:
    weapon_models/<id>.json          the model: nested parts turned by any angle, cubes, face UVs
    textures/entity/weapon/<id>.png  its painted texture; <id>_glow.png is the glow layer
    models/item/<id>_3d.json         builtin/entity, so WeaponRenderer draws it, and the hand transforms
and the previews in design/combat-items/3d/. Hand edits to any of those are lost on the next run:
edit this file. The flat inventory icons, and the <id>.json wrappers that show the icon in the
GUI and this model in hand, are tools/generate-combat-art.py's.

Each weapon is modelled here the way Blockbench models are made: parts, each turned about its own
pivot by any angle, holding boxes. Slim boxes and angled parts give real weapon proportions and
curves; the first round, pixel maps extruded one unit per pixel, read as toys (Sanchit,
2026-10-07). Every box face gets its own rectangle of the texture, painted from the box's
material: a palette ramp with lit and shaded bevels, a little grain, and for glowing materials a
matching texel in the glow layer, drawn additively and full-bright in game.

League of Legends items, drawn to look like their League counterparts in our own art (Sanchit,
2026-10-06): nothing is traced, sampled or copied from Riot's images or from another mod.
Palettes come from the sprites in generate-combat-art.py, so icon and model match. Seeded, so a
rerun reproduces every file. Needs Pillow and, for the previews, numpy.
"""
import importlib.util
import json
import math
import os
import random

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
NS = 'ascension_combat'


# --- Geometry -------------------------------------------------------------------------------
class Part:
    """A pivot and a rotation (degrees; Z . Y . X, X applied first, as WeaponModel and vanilla's
    ModelPart), boxes in the parent's coordinates, and child parts that turn with it."""

    def __init__(self, pivot=(0, 0, 0), rot=(0, 0, 0)):
        self.pivot, self.rot = tuple(pivot), tuple(rot)
        self.cubes, self.children = [], []

    def add(self, pivot=(0, 0, 0), rot=(0, 0, 0)):
        p = Part(pivot, rot)
        self.children.append(p)
        return p

    def box(self, frm, to, mat, skip=()):
        assert all(a < b for a, b in zip(frm, to)), (frm, to)
        self.cubes.append((tuple(frm), tuple(to), mat, tuple(skip)))
        return self

    def centred(self, centre, size, mat, skip=()):
        return self.box([c - s / 2 for c, s in zip(centre, size)], [c + s / 2 for c, s in zip(centre, size)],
                        mat, skip)

    def bar(self, centre, length, width, thick, angle, mat):
        """A box `length` long along its own y axis, centred on `centre`, turned `angle` degrees
        about z (0 = upright, positive = tipped toward -x)."""
        self.add(centre, (0, 0, angle)).centred(centre, (width, length, thick), mat)

    def arc(self, centre, radius, a0, a1, n, width, thick, mat, taper=0.0, z=8.0):
        """n bars following a circle's arc from angle a0 to a1 (degrees, 0 = +x, counterclockwise),
        each `width` across the arc and `thick` deep; taper thins the ends, for a crescent."""
        step = (a1 - a0) / n
        chord = 2 * radius * math.sin(math.radians(abs(step)) / 2) * 1.15
        for i in range(n):
            a = math.radians(a0 + step * (i + 0.5))
            c = (centre[0] + radius * math.cos(a), centre[1] + radius * math.sin(a), z)
            mid = 1 - abs((i + 0.5) / n * 2 - 1)  # 0 at the ends, 1 in the middle
            w = width * (1 - taper * (1 - mid))
            self.bar(c, chord, w, thick * (1 - 0.5 * taper * (1 - mid)), math.degrees(a), mat)


def mat_rotation(rot):
    rx, ry, rz = (math.radians(v) for v in rot)

    def rx_(v):
        c, s = math.cos(rx), math.sin(rx)
        return (v[0], c * v[1] - s * v[2], s * v[1] + c * v[2])

    def ry_(v):
        c, s = math.cos(ry), math.sin(ry)
        return (c * v[0] + s * v[2], v[1], -s * v[0] + c * v[2])

    def rz_(v):
        c, s = math.cos(rz), math.sin(rz)
        return (c * v[0] - s * v[1], s * v[0] + c * v[1], v[2])
    return lambda v: rz_(ry_(rx_(v)))


def item_rotation(rot):
    """ItemTransform's rotation, Rx . Ry . Rz: Z is applied first, the other way round from a
    model part's mat_rotation."""
    fx, fy, fz = (mat_rotation(r) for r in ((rot[0], 0, 0), (0, rot[1], 0), (0, 0, rot[2])))
    return lambda v: fx(fy(fz(v)))


# --- Materials and painting -----------------------------------------------------------------
FACE_AXES = {  # face -> (axis along u, axis along v): which box dimensions its texture spans
    'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2),
}
BROAD = ('north', 'south')


class Mat:
    """How a box looks. ramp: colours dark to light. level: where on the ramp it sits. bevel:
    steps lighter on a face's top and left texels, darker on its bottom and right, so edges read
    as bevelled. grain: random texel jitter, in ramp steps. bands: a stripe every n texels along
    v (a wrapped grip). across: a function (u in 0..1) -> level offset for broad faces, for a
    blade's bright edges or a ridge. glow: (colour, strength 0..1) drawn additively, full-bright."""

    def __init__(self, ramp, level, bevel=1.0, grain=0.35, bands=0, across=None, glow=None, side_level=None):
        self.ramp, self.level, self.bevel, self.grain = ramp, level, bevel, grain
        self.bands, self.across, self.glow = bands, across, glow
        self.side_level = level if side_level is None else side_level

    def paint(self, face, w, h, rng):
        base = [[None] * w for _ in range(h)]
        glow = [[None] * w for _ in range(h)]
        top = len(self.ramp) - 1
        for y in range(h):
            for x in range(w):
                lv = self.level if face in BROAD else self.side_level
                if face in BROAD and self.across:
                    lv += self.across((x + 0.5) / w)
                if self.bevel and w > 2 and h > 2:
                    if y == 0 or x == 0:
                        lv += self.bevel
                    elif y == h - 1 or x == w - 1:
                        lv -= self.bevel
                if self.bands and (y // self.bands) % 2:
                    lv -= 1
                lv += rng.uniform(-self.grain, self.grain)
                base[y][x] = self.ramp[max(0, min(top, int(round(lv))))]
                if self.glow:
                    colour, strength = self.glow
                    k = strength * (0.85 + 0.15 * rng.random())
                    if face in BROAD and self.across:
                        k *= max(0.0, min(1.0, 1 + self.across((x + 0.5) / w) * 0.25))
                    glow[y][x] = tuple(int(c * k) for c in art.hexc(colour)[:3]) + (255,)
        return base, glow


# --- Weapons --------------------------------------------------------------------------------
WEAPONS = {}


class Weapon:
    def __init__(self, name, density=2, tex=128, seed=1):
        self.name, self.density, self.tex, self.seed = name, density, tex, seed
        self.root = Part()
        self.display = {}
        self.scale = (0.85, 0.68)  # third person, first person, before any override in display
        self.hold = None  # how a humanoid holds and swings it (HoldAnimation's "hold" section)
        self.first_person = None  # how the player holds it in their own view (FirstPersonHold)

    def build(self):
        """Lay every box face out on the texture (shelf packing, tallest first), paint it, and
        return (model json, base image, glow image or None)."""
        faces = []
        for part, frm, to, mat, skip in self._cubes():
            dims = [b - a for a, b in zip(frm, to)]
            for f, (au, av) in FACE_AXES.items():
                if f in skip:
                    continue
                w = max(1, int(math.ceil(dims[au] * self.density - 1e-6)))
                h = max(1, int(math.ceil(dims[av] * self.density - 1e-6)))
                faces.append([part, frm, to, mat, f, w, h, None])
        order = sorted(range(len(faces)), key=lambda i: (-faces[i][6], -faces[i][5], i))
        x = y = shelf = 0
        for i in order:
            w, h = faces[i][5], faces[i][6]
            if x + w > self.tex:
                x, y, shelf = 0, y + shelf, 0
            assert y + h <= self.tex, '%s: texture too small for its faces' % self.name
            faces[i][7] = (x, y)
            x += w
            shelf = max(shelf, h)
        base = Image.new('RGBA', (self.tex, self.tex), (0, 0, 0, 0))
        glow = Image.new('RGBA', (self.tex, self.tex), (0, 0, 0, 0))
        rng = random.Random(self.seed)
        any_glow = False
        uvs = {}
        for part, frm, to, mat, f, w, h, (u, v) in faces:
            b, g = mat.paint(f, w, h, rng)
            for yy in range(h):
                for xx in range(w):
                    base.putpixel((u + xx, v + yy), art.hexc(b[yy][xx]))
                    if g[yy][xx]:
                        glow.putpixel((u + xx, v + yy), g[yy][xx])
                        any_glow = True
            uvs[(id(part), frm, to, f)] = [u, v, u + w, v + h]

        def part_json(p):
            out = {}
            if any(p.pivot):
                out['pivot'] = [num(c) for c in p.pivot]
            if any(p.rot):
                out['rotation'] = [num(c) for c in p.rot]
            if p.cubes:
                out['cubes'] = [{'from': [num(c) for c in frm], 'to': [num(c) for c in to],
                                 'faces': {f: uvs[(id(p), frm, to, f)] for f in FACE_AXES if f not in skip}}
                                for frm, to, mat, skip in p.cubes]
            if p.children:
                out['children'] = [part_json(c) for c in p.children]
            return out

        tex_id = '%s:textures/entity/weapon/%s' % (NS, self.name)
        model = {'_comment': 'generated by tools/generate-combat-models.py; edit that instead',
                 'texture': tex_id + '.png'}
        if any_glow:
            model['glow_texture'] = tex_id + '_glow.png'
        model['texture_size'] = [self.tex, self.tex]
        model['parts'] = [part_json(self.root)]
        if self.hold:
            model['hold'] = self.hold
        if self.first_person:
            model['first_person'] = self.first_person
        return model, base, (glow if any_glow else None)

    def _cubes(self, part=None):
        part = part or self.root
        for frm, to, mat, skip in part.cubes:
            yield part, frm, to, mat, skip
        for c in part.children:
            yield from self._cubes(c)


def num(v):
    v = round(v, 4)
    return int(v) if v == int(v) else v


def weapon(name, **kw):
    w = Weapon(name, **kw)
    WEAPONS[name] = w
    return w


# --- Veigo's Blade: Viego's sword ------------------------------------------------------------
# Hilt to tip, as his model render reads: an anchor pommel whose horns point at the blade, a
# shaft with a glowing socket, the great guard (a hub round a glowing jewel, arms ending in
# crescents that open outward), the grip his hand holds, a small horned guard, and a long, narrow,
# straight blade, about two thirds of the whole, with a glowing ridge. Spectral jade throughout.
_V = art.SPRITES['veigos_blade'][1]
JADE = [_V['K'], _V['d'], _V['m'], _V['l'], _V['w']]  # dark to light
V_BLADE = Mat(JADE, 2.4, bevel=0, grain=0.12, across=lambda u: 1.0 if u < 0.2 or u > 0.8 else 0.0,
              glow=(_V['G'], 0.22), side_level=3.0)
V_RIDGE = Mat(JADE, 2.6, bevel=0, grain=0.1, glow=(_V['G'], 0.75), side_level=2.0)
V_GUARD = Mat(JADE, 2.2, bevel=1.0, grain=0.3)
V_GRIP = Mat(JADE, 1.4, bevel=0, grain=0.2, bands=1)
V_JEWEL = Mat(JADE, 2.0, bevel=0.6, grain=0.1, glow=(_V['g'], 0.7))

veigo = weapon('veigos_blade', seed=11)
r = veigo.root
Z = 8.0
# Hilt to tip, model pixels up from the pommel's foot. The hilt is long, as his is, with room for
# two Minecraft fists (four pixels wide): his main hand on the grip under the small guard, the off
# hand on the shaft under the great guard.
POMMEL_Y = 2.6          # the pommel crescent's centre
SHAFT = (3.0, 7.0)      # the off hand's grip
GUARD_Y = 8.6           # the great guard's hub
GRIP = (10.1, 16.1)     # the main hand's grip
SMALL_Y = 16.5          # the small guard
BLADE = (16.95, 43.0)   # straight, then a five-pixel taper to the point
# Blade: 2.4 wide, 0.7 deep, with a raised ridge.
_tip = BLADE[1] - 5
for y0, y1, wdt in [(BLADE[0], _tip, 2.4)] + [(_tip + i, _tip + i + 1, w) for i, w in
                                              enumerate((2.1, 1.8, 1.4, 1.0, 0.5))]:
    r.box((Z - wdt / 2, y0, Z - 0.35), (Z + wdt / 2, y1, Z + 0.35), V_BLADE)
    rw = min(0.9, wdt * 0.4)
    if wdt > 0.6:
        r.box((Z - rw / 2, y0 + 0.02, Z - 0.5), (Z + rw / 2, y1 - 0.02, Z + 0.5), V_RIDGE)
# Small guard: a short bar with horns curving up beside the blade.
r.centred((Z, SMALL_Y, Z), (4.6, 0.9, 1.3), V_GUARD)
for side in (-1, 1):
    r.bar((Z + side * 2.35, SMALL_Y + 0.9, Z), 1.6, 0.6, 1.0, side * -28, V_GUARD)
# Grip: wrapped, turned 45 degrees about y so its square section reads rounder.
_g = (GRIP[0] + GRIP[1]) / 2
r.add((Z, _g, Z), (0, 45, 0)).centred((Z, _g, Z), (1.15, GRIP[1] - GRIP[0], 1.15), V_GRIP)
# Great guard: a hub turned into a diamond round a jewel, arms out to crescents opening outward.
r.add((Z, GUARD_Y, Z), (0, 0, 45)).centred((Z, GUARD_Y, Z), (2.3, 2.3, 1.3), V_GUARD)
r.add((Z, GUARD_Y, Z), (0, 0, 45)).centred((Z, GUARD_Y, Z), (1.0, 1.0, 1.9), V_JEWEL)
for side in (-1, 1):
    r.bar((Z + side * 2.6, GUARD_Y, Z), 2.6, 0.75, 0.9, 90, V_GUARD)
    # The arm meets the crescent's back; the crescent's centre lies beyond it, so it opens outward.
    r.arc((Z + side * 5.8, GUARD_Y), 1.9, 115 if side > 0 else -65, 245 if side > 0 else 65, 6, 0.8, 0.9,
          V_GUARD, taper=0.6)
# Shaft below the guard, with the glowing diamond socket.
_s = (SHAFT[0] + SHAFT[1]) / 2
r.add((Z, _s, Z), (0, 45, 0)).centred((Z, _s, Z), (0.9, SHAFT[1] - SHAFT[0], 0.9), V_GRIP)
r.add((Z, SHAFT[0] + 0.9, Z), (0, 0, 45)).centred((Z, SHAFT[0] + 0.9, Z), (0.75, 0.75, 1.3), V_JEWEL)
# Pommel: a deep crescent whose horns point up at the blade, on a stem up to the shaft.
r.arc((Z, POMMEL_Y), 1.6, 170, 370, 7, 0.75, 0.9, V_GUARD, taper=0.6)
r.centred((Z, (1.0 + SHAFT[0]) / 2, Z), (0.8, SHAFT[0] - 1.0, 0.8), V_GUARD)
MAIN_HAND = (Z, GRIP[1] - 2.2, Z)   # the fist's middle, just under the small guard
OFF_HAND = (Z, _s, Z)
veigo.grip = MAIN_HAND[:2]
# How he holds it, third person (WeaponPose plays this). Walking or standing he trails it in one
# hand, the tip on the ground behind; attacking he takes it in both hands: a slash from the right,
# a slash from the left, a stab. Keyframes over the swing; angles in degrees as HumanoidModel's:
# an arm's x raises it forward when negative, its y swings it to the holder's right; body_twist
# turns the torso right; the item's z tips the blade forward then down (blade elevation = forearm
# elevation + 90 - z, so 90 lays it along arms held straight out).
DRAG = {'right_arm': [15, 0, 8], 'item': [0, 6, 118]}
_rest = dict(DRAG, left_arm=[0, 0, 0])


def _slash(side):
    """A two-handed horizontal cut from `side` (1: from the right, -1: from the left) across, the
    hands at chest height and the blade laid level along the arms (z = forearm elevation + 90)."""
    return [dict(_rest, t=0),
            {'t': 0.2, 'right_arm': [-85, side * 70, 0], 'left_arm': [-85, side * 85, 0], 'body_twist': side * 30,
             'item': [0, 0, 62]},
            {'t': 0.45, 'right_arm': [-72, side * -5, 0], 'left_arm': [-72, side * 12, 0], 'body_twist': 0,
             'item': [0, 0, 72]},
            {'t': 0.62, 'right_arm': [-66, side * -75, 0], 'left_arm': [-66, side * -58, 0], 'body_twist': side * -30,
             'item': [0, 0, 80]},
            dict(_rest, t=1)]


STAB = [dict(_rest, t=0),
        {'t': 0.22, 'right_arm': [-40, -10, 0], 'left_arm': [-40, 20, 0], 'body_twist': 15, 'item': [0, 0, 40]},
        {'t': 0.45, 'right_arm': [-80, -5, 0], 'left_arm': [-80, 15, 0], 'body_twist': -10, 'item': [0, 0, 80]},
        {'t': 0.6, 'right_arm': [-80, -5, 0], 'left_arm': [-80, 15, 0], 'body_twist': -10, 'item': [0, 0, 80]},
        dict(_rest, t=1)]
veigo.hold = {'grip': list(MAIN_HAND), 'idle': DRAG, 'walk_swing': 0.25,
              'attacks': [_slash(1), _slash(-1), STAB]}
veigo.scale = (0.75, 0.68)  # third person: about the wielder's height, as Viego's is
# First person (WeaponFirstPerson): the player's own arms on it. Idle, one hand, held like vanilla
# holds a sword: hilt low at the right, blade up and away, out of the middle of the view.
# Attacking, both hands: the blade laid flat (edge leading) and swept across from one side, or
# drawn back and thrust. pos is where the main hand's grip sits before the camera (blocks: x
# right, y up, -z ahead); rotation turns the blade about it (Z . Y . X; [-90, yaw, 0] lays it level
# pointing ahead, then yaws it: negative to the right, positive to the left).
_fp_idle = {'pos': [0.55, -0.6, -1.0], 'rotation': [-25, -35, -25]}


def _fp_slash(side):
    """Cocked to `side` (1: right), the blade laid flat pointing that way, swept across the view."""
    return [dict(_fp_idle, t=0),
            {'t': 0.2, 'pos': [side * 0.45, -0.3, -1.0], 'rotation': [-90, side * -80, side * 15], 'two_hands': True},
            {'t': 0.45, 'pos': [0.05, -0.4, -1.05], 'rotation': [-90, side * 50, 0], 'two_hands': True},
            {'t': 0.62, 'pos': [side * -0.35, -0.45, -1.0], 'rotation': [-90, side * 100, side * -10],
             'two_hands': True},
            dict(_fp_idle, t=1)]


# The stab: drawn back low at the right, the point aimed at the crosshair, then driven at it.
_FP_STAB = [dict(_fp_idle, t=0),
            {'t': 0.22, 'pos': [0.35, -0.6, -0.75], 'rotation': [-75, 8, 0], 'two_hands': True},
            {'t': 0.45, 'pos': [0.15, -0.45, -1.25], 'rotation': [-80, 6, 0], 'two_hands': True},
            {'t': 0.6, 'pos': [0.15, -0.45, -1.25], 'rotation': [-80, 6, 0], 'two_hands': True},
            dict(_fp_idle, t=1)]
# The arms come up from shoulders below the bottom of the view, not beside the camera, so they
# hold the hilt without filling the screen.
veigo.first_person = {'scale': 0.55, 'right_hand': list(MAIN_HAND), 'left_hand': list(OFF_HAND),
                      'right_shoulder': [0.55, -1.4, -0.15], 'left_shoulder': [-0.55, -1.4, -0.15],
                      'idle': _fp_idle, 'attacks': [_fp_slash(1), _fp_slash(-1), _FP_STAB]}


# --- Display transforms ---------------------------------------------------------------------
def upright_handheld(grip, scale_tp=0.85, scale_fp=0.68):
    """Vanilla's handheld transforms, turned for a model drawn upright instead of diagonal.

    ItemTransform is T . Rx . Ry . Rz . S about the model's centre, so an extra Rz(-45), which
    tips our upright model onto vanilla's diagonal, folds exactly into Z. Then translate so the
    grip lands where vanilla's handle is: t = t_vanilla + s . R . (handle - Rz(-45) . grip), with
    handle and grip measured from the centre (8, 8, 8)."""
    vanilla_handle = (3.5 - 8, 3.5 - 8, 0)  # a vanilla sword's handle, model pixels
    g = (grip[0] - 8, grip[1] - 8, 0)
    turned = mat_rotation((0, 0, -45))(g)
    shift = [vanilla_handle[i] - turned[i] for i in range(3)]
    out = {}
    for ctx, (r_, t, sc) in {'thirdperson': ([0, -90, 55], [0, 4.0, 0.5], scale_tp),
                             'firstperson': ([0, -90, 25], [1.13, 3.2, 1.13], scale_fp)}.items():
        moved = item_rotation(r_)(shift)
        tr = [round(max(-80, min(80, t[i] + sc * moved[i])), 2) for i in range(3)]
        # The left hand takes the same values: ItemTransform mirrors them for that hand.
        out[ctx + '_righthand'] = out[ctx + '_lefthand'] = {
            'rotation': [r_[0], r_[1], r_[2] - 45], 'translation': tr, 'scale': [sc] * 3}
    return out


# --- Output ---------------------------------------------------------------------------------
def item_model(w):
    icon = art.HELD_3D[w.name]
    display = upright_handheld(w.grip, *w.scale)
    display.update(w.display)
    return {'parent': 'minecraft:builtin/entity', 'gui_light': 'front',
            'textures': {'particle': '%s:item/%s' % (NS, icon)}, 'display': display}


def write():
    model_dir = os.path.join(ASSETS, 'weapon_models')
    tex_dir = os.path.join(ASSETS, 'textures', 'entity', 'weapon')
    item_dir = os.path.join(ASSETS, 'models', 'item')
    for d in (model_dir, tex_dir):
        os.makedirs(d, exist_ok=True)
    for name, w in WEAPONS.items():
        model, base, glow = w.build()
        with open(os.path.join(model_dir, name + '.json'), 'w', newline='\n') as f:
            f.write(json.dumps(model, indent=1) + '\n')
        base.save(os.path.join(tex_dir, name + '.png'))
        if glow:
            glow.save(os.path.join(tex_dir, name + '_glow.png'))
        with open(os.path.join(item_dir, name + '_3d.json'), 'w', newline='\n') as f:
            f.write(json.dumps(item_model(w), indent=2) + '\n')


def previews():
    """design/combat-items/3d/<id>_hand.png: each weapon in hand, through render-item-model.py."""
    out_dir = os.path.join(ROOT, 'design', 'combat-items', '3d')
    os.makedirs(out_dir, exist_ok=True)
    render = _load('render_item_model', 'render-item-model.py')
    for n in WEAPONS:
        model = render.load_model('%s:item/%s' % (NS, n))
        render.sheet(model, n).save(os.path.join(out_dir, n + '_hand.png'))


if __name__ == '__main__':
    import sys
    write()
    if '--no-previews' not in sys.argv:
        previews()
    print('wrote combat weapon models and design/combat-items/3d/ previews')
