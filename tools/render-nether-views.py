"""Views of the Nether as a player sees it, rendered from a real generated world. No build or runtime
dependency; needs Pillow and numpy.

Planet 3 is vanilla's own terrain opened to the sky (ADR-0017), so its look depends on ground nobody
drew: the plateaus, the lava seas, the warped forest round the gate. The other preview tools draw a
structure on a flat plain. This one reads the chunks a dedicated server generated (the region files
of any test world's DIM-1) and ray-marches them with the haze the sky file sets, the biome's own fog
colour, the dimension's block light and its ambient light, so a design can be judged where it will
stand: the view down from a ship at y 216, the view up from a plateau, the gate ruin and the colony
ruins pasted onto the real ground.

Used as a module by tools/generate-sky-art.py and tools/generate-nether-colony.py (their previews);
run directly it renders one view:

    python tools/render-nether-views.py <DIM-1/region dir> --eye x y z --target x y z [--out p.png]

Colours: our own blocks are drawn from their own textures. Vanilla blocks are drawn in a small table
of rough colours with a little per-block noise, never from vanilla's textures (art guide rule 1), so
everything this writes can be committed. `--vanilla-textures <dir>` samples an unpacked vanilla
`assets/minecraft` instead, for a local look at tone only; such a render is never committed.

The world itself is read-only here: regions are opened for reading, nothing is written back.
"""
import argparse
import importlib.util
import json
import math
import os
import random
import struct
import zlib

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


rsp = _load('render_structure_preview', 'render-structure-preview.py')

AIR = 'minecraft:air'


# --- reading a world ------------------------------------------------------------------------------

def _unpack(longs, bits, count):
    """Palette indices packed `64 // bits` to a long, none spanning two (1.16+)."""
    per = 64 // bits
    words = np.frombuffer(longs, dtype='>u8').astype(np.uint64)
    shifts = (np.arange(per, dtype=np.uint64) * np.uint64(bits))
    vals = (words[:, None] >> shifts[None, :]) & np.uint64((1 << bits) - 1)
    return vals.reshape(-1)[:count].astype(np.int64)


class RegionWorld:
    """A dimension's region folder, read lazily chunk by chunk. Block states are kept as strings
    ('ns:id[k=v,...]', properties sorted, as rsp.state_string writes them) in one palette shared by
    everything read, so a box of the world is a small integer array."""

    def __init__(self, region_dir):
        self.dir = region_dir
        self.palette = [AIR]
        self.index = {AIR: 0}
        self.biome_palette = []
        self.biome_index = {}
        self._chunks = {}

    def _state_id(self, state):
        i = self.index.get(state)
        if i is None:
            i = self.index[state] = len(self.palette)
            self.palette.append(state)
        return i

    def _biome_id(self, b):
        i = self.biome_index.get(b)
        if i is None:
            i = self.biome_index[b] = len(self.biome_palette)
            self.biome_palette.append(b)
        return i

    def chunk(self, cx, cz):
        """{section y: (block ids [y][z][x] 16^3, biome ids [y][z][x] 4^3)} or None if not generated."""
        key = (cx, cz)
        if key in self._chunks:
            return self._chunks[key]
        path = os.path.join(self.dir, f'r.{cx >> 5}.{cz >> 5}.mca') if self.dir else None
        out = None
        if path and os.path.exists(path):
            with open(path, 'rb') as f:
                f.seek(4 * ((cx & 31) + (cz & 31) * 32))
                loc = f.read(4)
                sector = int.from_bytes(loc[:3], 'big')
                if loc[3]:
                    f.seek(sector * 4096)
                    length = struct.unpack('>I', f.read(4))[0]
                    kind = f.read(1)[0]
                    raw = f.read(length - 1)
                    if kind == 2:
                        data = zlib.decompress(raw)
                        n = struct.unpack_from('>H', data, 1)[0]
                        root, _ = rsp._nbt(data, 3 + n, 10)
                        if root.get('Status', '').endswith('full'):
                            out = self._sections(root)
        self._chunks[key] = out
        return out

    def _sections(self, root):
        out = {}
        for s in root.get('sections', []):
            bs = s.get('block_states')
            if not bs:
                continue
            pal = [self._state_id(rsp.state_string(e)) for e in bs['palette']]
            if len(pal) == 1:
                blocks = np.full(4096, pal[0], np.uint16)
            else:
                bits = max(4, (len(pal) - 1).bit_length())
                blocks = np.array(pal, np.uint16)[_unpack(bs['data'], bits, 4096)]
            bio = s.get('biomes', {})
            bpal = [self._biome_id(b) for b in bio.get('palette', ['minecraft:the_void'])]
            if len(bpal) == 1:
                biomes = np.full(64, bpal[0], np.uint16)
            else:
                bits = (len(bpal) - 1).bit_length()
                biomes = np.array(bpal, np.uint16)[_unpack(bio['data'], bits, 64)]
            out[s['Y']] = (blocks.reshape(16, 16, 16), biomes.reshape(4, 4, 4))
        return out

    def box(self, x0, x1, y0, y1, z0, z1):
        """Block ids over the inclusive box, as an array indexed [x - x0, y - y0, z - z0]. Ungenerated
        chunks read as air."""
        nx, ny, nz = x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1
        out = np.zeros((nx, ny, nz), np.uint16)
        for cx in range(x0 >> 4, (x1 >> 4) + 1):
            for cz in range(z0 >> 4, (z1 >> 4) + 1):
                ch = self.chunk(cx, cz)
                if not ch:
                    continue
                for sy, (blocks, _) in ch.items():
                    bx0, by0, bz0 = cx * 16, sy * 16, cz * 16
                    ax0, ax1 = max(x0, bx0), min(x1, bx0 + 15)
                    ay0, ay1 = max(y0, by0), min(y1, by0 + 15)
                    az0, az1 = max(z0, bz0), min(z1, bz0 + 15)
                    if ax0 > ax1 or ay0 > ay1 or az0 > az1:
                        continue
                    part = blocks[ay0 - by0:ay1 - by0 + 1, az0 - bz0:az1 - bz0 + 1, ax0 - bx0:ax1 - bx0 + 1]
                    out[ax0 - x0:ax1 - x0 + 1, ay0 - y0:ay1 - y0 + 1, az0 - z0:az1 - z0 + 1] = part.transpose(2, 0, 1)
        return out

    def biome(self, x, y, z):
        ch = self.chunk(x >> 4, z >> 4)
        if not ch or (y >> 4) not in ch:
            return None
        return self.biome_palette[ch[y >> 4][1][(y & 15) >> 2, (z & 15) >> 2, (x & 15) >> 2]]

    def top(self, x, z, y_from=255, solid=None):
        """The highest non-air block's y in a column (or the first `solid(state)` one)."""
        col = self.box(x, x, 0, y_from, z, z)[0, :, 0]
        for y in range(y_from, -1, -1):
            st = self.palette[col[y]]
            if st != AIR and (solid is None or solid(st)):
                return y
        return None


# --- how blocks look -------------------------------------------------------------------------------
#
# Rough colours for the vanilla blocks the Nether shows, picked by eye (art guide rule 1: no vanilla
# pixels in anything committed). Each is (r, g, b) on its lit top face; noise and face shading are
# added when drawn.

VANILLA = {
    'netherrack': (98, 40, 40), 'nether_quartz_ore': (120, 74, 70), 'nether_gold_ore': (118, 62, 44),
    'ancient_debris': (96, 70, 62), 'magma_block': (150, 66, 30), 'glowstone': (196, 152, 92),
    'soul_sand': (80, 62, 50), 'soul_soil': (74, 56, 46), 'bone_block': (226, 222, 204),
    'basalt': (76, 76, 82), 'polished_basalt': (90, 90, 94), 'smooth_basalt': (72, 72, 78),
    'blackstone': (42, 36, 42), 'gilded_blackstone': (66, 48, 36), 'polished_blackstone': (54, 50, 58),
    'polished_blackstone_bricks': (48, 43, 50), 'cracked_polished_blackstone_bricks': (44, 38, 44),
    'chiseled_polished_blackstone': (54, 50, 58), 'polished_blackstone_brick_slab': (48, 43, 50),
    'polished_blackstone_slab': (54, 50, 58), 'blackstone_slab': (42, 36, 42),
    'polished_blackstone_brick_stairs': (48, 43, 50), 'polished_blackstone_stairs': (54, 50, 58),
    'blackstone_stairs': (42, 36, 42), 'polished_blackstone_brick_wall': (48, 43, 50),
    'blackstone_wall': (42, 36, 42), 'polished_blackstone_wall': (54, 50, 58),
    'gold_block': (240, 200, 64), 'raw_gold_block': (214, 160, 50),
    'quartz_block': (232, 226, 218), 'quartz_bricks': (230, 224, 216), 'quartz_pillar': (232, 226, 220),
    'chiseled_quartz_block': (228, 222, 214), 'smooth_quartz': (232, 226, 218), 'quartz_slab': (232, 226, 218),
    'nether_bricks': (46, 22, 27), 'cracked_nether_bricks': (42, 20, 25), 'chiseled_nether_bricks': (48, 24, 29),
    'red_nether_bricks': (70, 10, 12), 'nether_brick_fence': (46, 22, 27), 'nether_brick_slab': (46, 22, 27),
    'nether_brick_stairs': (46, 22, 27), 'nether_brick_wall': (46, 22, 27),
    'crimson_nylium': (128, 30, 32), 'warped_nylium': (40, 112, 100), 'nether_wart_block': (112, 6, 6),
    'warped_wart_block': (22, 116, 118), 'crimson_stem': (92, 28, 32), 'warped_stem': (58, 60, 78),
    'crimson_hyphae': (92, 28, 32), 'warped_hyphae': (58, 60, 78), 'shroomlight': (236, 146, 72),
    'crimson_roots': (126, 8, 20), 'warped_roots': (20, 140, 124), 'nether_sprouts': (20, 140, 124),
    'crimson_fungus': (140, 30, 30), 'warped_fungus': (30, 130, 120), 'weeping_vines': (122, 4, 4),
    'weeping_vines_plant': (122, 4, 4), 'twisting_vines': (20, 140, 124), 'twisting_vines_plant': (20, 140, 124),
    'obsidian': (20, 16, 30), 'crying_obsidian': (34, 12, 62), 'lava': (226, 110, 28), 'fire': (232, 140, 40),
    'soul_fire': (60, 180, 200), 'gravel': (112, 104, 100), 'bedrock': (70, 70, 70),
    'chain': (54, 58, 70), 'lantern': (200, 150, 80), 'soul_lantern': (80, 150, 170), 'iron_bars': (110, 110, 112),
    'chest': (128, 92, 52), 'barrel': (110, 82, 50), 'anvil': (60, 60, 62), 'chipped_anvil': (60, 60, 62),
    'damaged_anvil': (60, 60, 62), 'blast_furnace': (80, 80, 82), 'smithing_table': (60, 50, 50),
    'cauldron': (60, 60, 64), 'lava_cauldron': (210, 100, 30), 'brown_mushroom': (140, 104, 80),
    'red_mushroom': (190, 40, 40), 'spawner': (40, 50, 60), 'cobweb': (220, 220, 220),
    'netherite_block': (66, 60, 62), 'respawn_anchor': (40, 20, 70), 'stone': (126, 126, 126),
}

# Light each emits (vanilla's block properties, the numbers the game uses).
EMITS = {
    'lava': 15, 'fire': 15, 'glowstone': 15, 'shroomlight': 15, 'lantern': 15, 'jack_o_lantern': 15,
    'soul_fire': 10, 'soul_lantern': 10, 'crying_obsidian': 10, 'magma_block': 3, 'respawn_anchor': 0,
    'brown_mushroom': 1, 'nether_portal': 11, 'sea_lantern': 15,
}
# Drawn full-bright: lava and fire, and the blocks that are their own light.
GLOWS = {'lava': 1.0, 'fire': 1.0, 'soul_fire': 1.0, 'magma_block': 0.85, 'shroomlight': 0.95,
         'glowstone': 0.95, 'lantern': 0.9, 'soul_lantern': 0.9, 'crying_obsidian': 0.45}
# Not full cubes: they let light through, and are drawn as a sparse speckle where a ray crosses them.
SPARSE = {'crimson_roots': 0.35, 'warped_roots': 0.35, 'nether_sprouts': 0.25, 'crimson_fungus': 0.3,
          'warped_fungus': 0.3, 'weeping_vines': 0.4, 'weeping_vines_plant': 0.4, 'twisting_vines': 0.4,
          'twisting_vines_plant': 0.4, 'fire': 0.5, 'soul_fire': 0.5, 'chain': 0.25, 'iron_bars': 0.3,
          'brown_mushroom': 0.3, 'red_mushroom': 0.3, 'cobweb': 0.2, 'nether_brick_fence': 0.45,
          'lantern': 0.5, 'soul_lantern': 0.5}


def _short(state):
    name, _ = rsp.split_state(state)
    return name.split(':', 1)[1]


class Looks:
    """Per palette id: colour, glow, opacity, light emitted, half height. Our own blocks are drawn
    from their own textures (through rsp.state_colour, which follows blockstate, model and texture);
    vanilla's from VANILLA, or, given a local vanilla asset folder, from vanilla's textures for a
    scratch tone check that is never committed."""

    def __init__(self, palette, vanilla_assets=None):
        self.vanilla_assets = vanilla_assets
        self._tex = {}
        n = len(palette)
        self.colour = np.zeros((n, 3), np.float32)
        self.glow = np.zeros(n, np.float32)
        self.solid = np.zeros(n, bool)          # stops a ray (full or partial block)
        self.sparse = np.zeros(n, np.float32)   # chance a ray crossing it is stopped (plants, chains)
        self.opaque = np.zeros(n, bool)         # stops light
        self.emit = np.zeros(n, np.uint8)
        self.half = np.zeros(n, np.int8)        # 1 bottom slab, 2 top slab
        self.textures = [None] * n              # (top image, side image) as float arrays, or None
        self.states = list(palette)
        for i, st in enumerate(palette):
            self._describe(i, st)

    def _describe(self, i, st):
        name, props = rsp.split_state(st)
        short = name.split(':', 1)[1]
        if name in ('minecraft:air', 'minecraft:cave_air', 'minecraft:void_air', 'minecraft:structure_void'):
            return
        lit = props.get('lit', 'true') == 'true'
        self.solid[i] = True
        self.emit[i] = EMITS.get(short, 0) if lit or short in ('lava', 'fire', 'soul_fire') else 0
        if name.startswith('ascension_worlds:'):
            c = rsp.state_colour(st)
            self.colour[i] = np.array(c, np.float32) / 255 if c else 0.5
            if short == 'inscribed_gatestone' and lit:
                self.emit[i], self.glow[i] = 9, 0.6
            if short == 'archive_orrery':
                self.emit[i] = 10
            self.textures[i] = self._our_textures(st)
        else:
            c = VANILLA.get(short)
            if c is None:
                c = rsp.colour_of(name) or (120, 120, 120)
            self.colour[i] = np.array(c, np.float32) / 255
            self.glow[i] = GLOWS.get(short, 0.0) if lit or short in GLOWS else 0.0
            if self.vanilla_assets:
                self.textures[i] = self._vanilla_textures(name, props)
        if short in SPARSE:
            self.sparse[i] = SPARSE[short]
        if short.endswith('_slab'):
            self.half[i] = {'bottom': 1, 'top': 2}.get(props.get('type'), 0)
        self.opaque[i] = (short not in SPARSE and short != 'lava' and not short.endswith('_slab')
                          and not short.endswith('_stairs') and not short.endswith('_wall')
                          and short not in ('chest', 'barrel', 'anvil', 'cauldron', 'lava_cauldron',
                                            'colony_plinth', 'archive_plinth', 'archive_orrery', 'spawner'))

    # textures: two 16 x 16 float images per block, the top and a side
    def _image(self, path):
        if path not in self._tex:
            im = Image.open(path).convert('RGBA')
            im = im.crop((0, 0, im.width, im.width)).resize((16, 16), Image.NEAREST)
            self._tex[path] = np.asarray(im, np.float32)[:, :, :3] / 255
        return self._tex[path]

    def _our_textures(self, st):
        name, props = rsp.split_state(st)
        ns, path = name.split(':')
        model = rsp._variant_model(ns, path, props)
        if not model:
            return None
        base, _ = rsp._model_textures(model)
        if not base:
            return None
        top = base
        mns, mpath = model.split(':', 1) if ':' in model else ('minecraft', model)
        mp = rsp._find(mns, 'models', mpath + '.json')
        if mp:
            tex = json.load(open(mp)).get('textures', {})
            for k in ('top', 'end', 'all'):
                t = tex.get(k)
                if t and not t.startswith('#'):
                    tns, tpath = t.split(':') if ':' in t else ('minecraft', t)
                    found = rsp._find(tns, 'textures', tpath + '.png')
                    if found:
                        top = found
                        break
        return self._image(top), self._image(base)

    def _vanilla_textures(self, name, props):
        """Scratch only: vanilla's own texture for a state, from an unpacked asset folder."""
        short = name.split(':', 1)[1]
        root = os.path.join(self.vanilla_assets, 'textures', 'block')
        cand_side = [short, short + '_side', short.replace('_slab', '').replace('_stairs', '').replace('_wall', ''),
                     short.replace('_bricks_slab', '_bricks').replace('_brick_slab', '_bricks')
                     .replace('_brick_stairs', '_bricks').replace('_brick_wall', '_bricks'),
                     short.replace('_slab', 's').replace('_stairs', 's'), short + '_still']
        cand_top = [short + '_top', short.replace('_slab', '_top')] + cand_side
        if short in ('lava',):
            cand_side = cand_top = ['lava_still']
        if short == 'crimson_nylium' or short == 'warped_nylium':
            cand_top, cand_side = [short], [short + '_side']
        if short in ('quartz_block', 'smooth_quartz', 'quartz_slab'):
            cand_top, cand_side = ['quartz_block_top'], ['quartz_block_side']
        if short in ('chest',):
            return None

        def first(cands):
            for c in cands:
                p = os.path.join(root, c + '.png')
                if os.path.exists(p):
                    return self._image(p)
            return None
        side = first(cand_side)
        top = first(cand_top)
        if side is None and top is None:
            return None
        return (top if top is not None else side), (side if side is not None else top)


def _hash(*cs):
    """A cheap positional hash of integer arrays, 0..1: per-block noise that is the same every run."""
    h = np.zeros(np.shape(cs[0]), np.uint64)
    for c, k in zip(cs, (73856093, 19349663, 83492791, 2654435761, 97531)):
        h ^= np.asarray(c, np.int64).astype(np.uint64) * np.uint64(k)
    h ^= h >> np.uint64(13)
    h *= np.uint64(1274126177)
    h ^= h >> np.uint64(16)
    return (h & np.uint64(0xFFFF)).astype(np.float32) / 65535.0


# --- light ----------------------------------------------------------------------------------------

def block_light(ids, looks):
    """Vanilla's block light over the box: each emitter's level, losing one a step, through anything
    that does not stop light. Light from outside the box is ignored, so its edges err dark."""
    level = looks.emit[ids].astype(np.int16)
    passes = ~looks.opaque[ids]
    for _ in range(14):
        nb = np.zeros_like(level)
        nb[1:] = np.maximum(nb[1:], level[:-1])
        nb[:-1] = np.maximum(nb[:-1], level[1:])
        nb[:, 1:] = np.maximum(nb[:, 1:], level[:, :-1])
        nb[:, :-1] = np.maximum(nb[:, :-1], level[:, 1:])
        nb[:, :, 1:] = np.maximum(nb[:, :, 1:], level[:, :, :-1])
        nb[:, :, :-1] = np.maximum(nb[:, :, :-1], level[:, :, 1:])
        level = np.where(passes, np.maximum(level, nb - 1), level)
    return np.clip(level, 0, 15).astype(np.uint8)


NETHER_AMBIENT = 0.1      # dimension_type/the_nether.json


def lightmap(ambient=NETHER_AMBIENT, gamma=0.5, sky_darken=0.2):
    """The game's lightmap for a world with no sky light, as LightTexture builds it: for each block
    light level 0-15, the (r, g, b) a surface is multiplied by. Block light is warm (red kept,
    blue lost) and the ambient light keeps level 0 from black; then the video setting's gamma lift
    (0.5 is the default) and the small pull toward grey. sky_darken 0.2 is the Nether's fixed
    midnight. No flicker."""
    def level_brightness(n):
        f = n / 15.0
        b = f / (4.0 - 3.0 * f)
        return b + ambient * (1 - b) if ambient else b
    sky = np.array([sky_darken, sky_darken, 1.0]) * 0.65 + 0.35
    f1 = sky_darken * 0.95 + 0.05
    out = np.zeros((16, 3), np.float32)
    for j in range(16):
        f8 = level_brightness(0) * f1
        f9 = level_brightness(j) * 1.5
        c = np.array([f9, f9 * ((f9 * 0.6 + 0.4) * 0.6 + 0.4), f9 * (f9 * f9 * 0.6 + 0.4)])
        c = c + sky * f8
        c = c + (0.75 - c) * 0.04
        c = np.clip(c, 0, 1)
        lifted = 1 - (1 - c) ** 4
        c = c + (lifted - c) * gamma
        c = c + (0.75 - c) * 0.04
        out[j] = np.clip(c, 0, 1)
    return out


LIGHTMAP = lightmap()


# Directional face shade where the dimension has constant ambient light (the Nether): up and down
# alike, north/south brighter than east/west.
NETHER_SHADE = {(0, 1): 0.9, (0, -1): 0.9, (2, 1): 0.8, (2, -1): 0.8, (1, 1): 0.6, (1, -1): 0.6}
# keyed by (axis, sign of the normal); axis 0 x, 1 y, 2 z


# --- the ray march -----------------------------------------------------------------------------------

class Scene:
    """A box of the world (plus anything pasted into it), ready to draw."""

    def __init__(self, world, x0, x1, y0, y1, z0, z1):
        self.world = world
        self.origin = np.array([x0, y0, z0])
        self.ids = world.box(x0, x1, y0, y1, z0, z1)
        self.palette = world.palette           # shared, grows as states are pasted

    @classmethod
    def flat(cls, x0, x1, y0, y1, z0, z1, ground_y, ground='minecraft:netherrack', top=None):
        """No world: a level plain of `ground` up to ground_y (its top row `top` if given), for
        judging a structure on its own."""
        scene = cls.__new__(cls)
        scene.world = RegionWorld(None)
        scene.origin = np.array([x0, y0, z0])
        scene.ids = np.zeros((x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1), np.uint16)
        scene.palette = scene.world.palette
        scene.ids[:, :ground_y - y0 + 1, :] = scene.state_id(ground)
        if top:
            scene.ids[:, ground_y - y0, :] = scene.state_id(top)
        return scene

    def state_id(self, state):
        return self.world._state_id(state)

    def set(self, x, y, z, state):
        i, j, k = x - self.origin[0], y - self.origin[1], z - self.origin[2]
        if 0 <= i < self.ids.shape[0] and 0 <= j < self.ids.shape[1] and 0 <= k < self.ids.shape[2]:
            self.ids[i, j, k] = self.state_id(state)

    def get(self, x, y, z):
        i, j, k = x - self.origin[0], y - self.origin[1], z - self.origin[2]
        if 0 <= i < self.ids.shape[0] and 0 <= j < self.ids.shape[1] and 0 <= k < self.ids.shape[2]:
            return self.palette[self.ids[i, j, k]]
        return None

    def paste(self, vox, at, rotation=0, void_keeps=True):
        """Paste a template's cells ({(x, y, z): state}) with its (0, 0, 0) at `at`, turned
        `rotation` quarter turns clockwise seen from above (vanilla's Rotation order). Absent cells
        are structure_void and leave the world alone."""
        sx = max(p[0] for p in vox) + 1
        sz = max(p[2] for p in vox) + 1
        for (x, y, z), st in vox.items():
            if st == 'minecraft:structure_void':
                continue
            rx, rz = rotate_xz(x, z, rotation, sx, sz)
            self.set(at[0] + rx, at[1] + y, at[2] + rz, rotate_state(st, rotation))

    def render(self, eye, target, size=(960, 540), fov=70.0, haze=(24.0, 160.0), fog=None,
               render_distance=192.0, bodies=(), vanilla_assets=None, max_steps=900, light=None):
        return render(self, eye, target, size, fov, haze, fog, render_distance, bodies, vanilla_assets,
                      max_steps, light)


ROT_FACING = {'north': 0, 'east': 1, 'south': 2, 'west': 3}
FACINGS = ['north', 'east', 'south', 'west']


def rotate_xz(x, z, rotation, sx, sz):
    """StructureTemplate's own rotation of a template cell about its origin corner, then shifted
    back into the positive box (as vanilla does when it places a piece at its bounding box)."""
    r = rotation % 4
    if r == 0:
        return x, z
    if r == 1:       # clockwise 90: (x, z) -> (-z, x)
        return sz - 1 - z, x
    if r == 2:
        return sx - 1 - x, sz - 1 - z
    return z, sx - 1 - x


def rotate_state(st, rotation):
    r = rotation % 4
    if r == 0 or '[' not in st:
        return st
    name, props = rsp.split_state(st)
    if 'facing' in props and props['facing'] in ROT_FACING:
        props['facing'] = FACINGS[(ROT_FACING[props['facing']] + r) % 4]
    if 'axis' in props and r % 2 == 1:
        props['axis'] = {'x': 'z', 'z': 'x'}.get(props['axis'], props['axis'])
    return name + '[' + ','.join(f'{k}={v}' for k, v in sorted(props.items())) + ']'


def camera(eye, target, size, fov):
    """Per-pixel ray directions (h, w, 3). x east, y up, z south; right = forward x up, so a block
    east of a target seen looking north lands on the right of the picture."""
    w, h = size
    f = np.array(target, float) - np.array(eye, float)
    f /= np.linalg.norm(f)
    up = np.array([0.0, 1.0, 0.0])
    if abs(f[1]) > 0.999:
        up = np.array([0.0, 0.0, -1.0])
    r = np.cross(f, up)
    r /= np.linalg.norm(r)
    u = np.cross(r, f)
    half = math.tan(math.radians(fov) / 2)
    xs = (np.arange(w) + 0.5) / w * 2 - 1
    ys = 1 - (np.arange(h) + 0.5) / h * 2
    gx, gy = np.meshgrid(xs * half * w / h, ys * half)
    d = f[None, None, :] + gx[..., None] * r[None, None, :] + gy[..., None] * u[None, None, :]
    return d / np.linalg.norm(d, axis=2, keepdims=True)


def _smoothstep(a, b, x):
    t = np.clip((x - a) / max(b - a, 1e-6), 0, 1)
    return t * t * (3 - 2 * t)


def render(scene, eye, target, size, fov, haze, fog, render_distance, bodies, vanilla_assets, max_steps,
           light):
    looks = Looks(scene.palette, vanilla_assets)
    ids = scene.ids
    if light is None:
        light = block_light(ids, looks)
    w, h = size
    dirs = camera(eye, target, size, fov).reshape(-1, 3)
    n = dirs.shape[0]
    o = np.array(eye, float) - scene.origin
    shape = np.array(ids.shape)

    # Amanatides & Woo, every ray at once
    pos = np.floor(o).astype(np.int64)
    pos = np.broadcast_to(pos, (n, 3)).copy()
    step = np.sign(dirs).astype(np.int64)
    step[step == 0] = 1
    with np.errstate(divide='ignore'):
        tdelta = np.abs(1.0 / dirs)
        nxt = np.where(dirs > 0, pos + 1 - o, o - pos)
        tmax = nxt * tdelta
    tmax[~np.isfinite(tmax)] = np.inf
    tdelta[~np.isfinite(tdelta)] = np.inf
    hit_t = np.full(n, np.inf)
    hit_id = np.zeros(n, np.int64)
    hit_axis = np.zeros(n, np.int64)
    hit_cell = np.zeros((n, 3), np.int64)
    prev_cell = pos.copy()
    active = np.arange(n)
    last_axis = np.full(n, 1, np.int64)
    t_now = np.zeros(n)
    for _ in range(max_steps):
        if active.size == 0:
            break
        tm = tmax[active]
        axis = np.argmin(tm, axis=1)
        t_now[active] = tm[np.arange(active.size), axis]
        prev_cell[active] = pos[active]
        pos[active, axis] += step[active, axis]
        tmax[active, axis] += tdelta[active, axis]
        last_axis[active] = axis
        p = pos[active]
        inside = np.all((p >= 0) & (p < shape), axis=1)
        horiz = np.hypot(p[:, 0] + scene.origin[0] + 0.5 - eye[0], p[:, 2] + scene.origin[2] + 0.5 - eye[2])
        alive = inside & (horiz <= render_distance)
        # rays that leave the box upward or sideways are done (they see the sky)
        gone = ~alive
        cur = active[alive]
        pc = p[alive]
        cid = ids[pc[:, 0], pc[:, 1], pc[:, 2]].astype(np.int64)
        solid = looks.solid[cid]
        sp = looks.sparse[cid]
        if np.any(sp > 0):
            wx, wy, wz = (pc + scene.origin).T
            # a fixed 4 x 4 coverage pattern on the face the ray enters, so a plant is the same
            # speckle from frame to frame and ray to ray, not noise
            ent = o[None, :] + dirs[cur] * t_now[cur, None]
            sub = np.floor(ent * 4).astype(np.int64)
            solid = solid & ((sp == 0) | (_hash(sub[:, 0], sub[:, 1], sub[:, 2]) < sp))
        halfm = looks.half[cid]
        if np.any(halfm > 0):
            # a slab: where the ray crosses the cell, is it in the filled half?
            tin = t_now[cur]
            yin = o[1] + dirs[cur, 1] * tin - pc[:, 1]
            tout = np.min(tmax[cur], axis=1)
            yout = o[1] + dirs[cur, 1] * tout - pc[:, 1]
            lo, hi = np.minimum(yin, yout), np.maximum(yin, yout)
            in_half = np.where(halfm == 1, lo < 0.5, np.where(halfm == 2, hi > 0.5, True))
            solid = solid & in_half
        hit = cur[solid]
        hit_t[hit] = t_now[hit]
        hit_id[hit] = cid[solid]
        hit_axis[hit] = last_axis[hit]
        hit_cell[hit] = pos[hit]
        keep = np.ones(active.size, bool)
        keep[np.where(gone)[0]] = False
        alive_idx = np.where(alive)[0]
        keep[alive_idx[solid]] = False
        active = active[keep]

    # sky: the fog colour, and any bodies (drawn after the sky, before terrain)
    fog_c = np.array(fog if fog is not None else (0.2, 0.03, 0.03), np.float32)
    img = np.broadcast_to(fog_c, (n, 3)).copy()
    for body in bodies:
        img = draw_body(img, dirs, body)

    hit = np.isfinite(hit_t)
    hi = np.where(hit)[0]
    cid = hit_id[hi]
    axis = hit_axis[hi]
    sgn = -step[hi, axis]                                  # the face normal points back at the ray
    base = looks.colour[cid].copy()
    # texture or noise
    pt = o[None, :] + dirs[hi] * hit_t[hi, None]
    cell = hit_cell[hi]
    frac = pt - cell
    tex_u = np.where(axis == 0, frac[:, 2], frac[:, 0])
    tex_v = np.where(axis == 1, frac[:, 2], 1 - frac[:, 1])
    ui = np.clip((tex_u * 16).astype(int), 0, 15)
    vi = np.clip((tex_v * 16).astype(int), 0, 15)
    has_tex = np.array([looks.textures[c] is not None for c in cid], bool)
    if np.any(has_tex):
        for c in np.unique(cid[has_tex]):
            sel = (cid == c) & has_tex
            top, side = looks.textures[c]
            on_top = axis[sel] == 1
            t = np.where(on_top[:, None], top[vi[sel], ui[sel]], side[vi[sel], ui[sel]])
            base[sel] = t
    wx, wy, wz = (cell + scene.origin).T
    nt = ~has_tex
    noise = _hash(wx[nt], wy[nt], wz[nt])
    pix = _hash(wx[nt] * 16 + ui[nt], wy[nt] * 16 + vi[nt], wz[nt] * 16 + axis[nt], axis[nt])
    base[~has_tex] *= (0.92 + 0.16 * noise[:, None]) * (0.9 + 0.2 * pix[:, None])
    # face shade and light: the light in the cell the ray stood in before the hit
    shade = np.ones(hi.size, np.float32)
    for (ax, s), k in NETHER_SHADE.items():
        shade[(axis == ax) & (sgn == s)] = k
    pc = np.clip(prev_cell[hi], 0, shape - 1)
    lv = light[pc[:, 0], pc[:, 1], pc[:, 2]]
    lit = base * shade[:, None] * LIGHTMAP[lv]
    g = looks.glow[cid][:, None]
    col = lit * (1 - g) + base * g
    # haze: smoothstep from start to end on the true distance, toward the fog colour
    dist = hit_t[hi]
    fogk = _smoothstep(haze[0], haze[1], dist)[:, None]
    col = col * (1 - fogk) + fog_c[None, :] * fogk
    img[hi] = col
    out = np.clip(img.reshape(h, w, 3), 0, 1)
    return Image.fromarray((out * 255 + 0.5).astype(np.uint8))


def draw_body(img, dirs, body):
    """A sky body as the game draws it at AFTER_SKY: a disc of `texture` at yaw/elevation, its face
    added to the sky when additive, with a soft halo. Unlit (it glows), as the Nether's sun is."""
    yaw, elev, size = math.radians(body['yaw']), math.radians(body['elevation']), math.radians(body['angular_size'])
    # Minecraft yaw: 0 south (+z), 90 west (-x)
    d = np.array([-math.sin(yaw) * math.cos(elev), math.sin(elev), math.cos(yaw) * math.cos(elev)])
    up = np.array([0.0, 1.0, 0.0])
    right = np.cross(d, up)
    right /= np.linalg.norm(right)
    upv = np.cross(right, d)
    cosang = dirs @ d
    front = cosang > 0
    u = (dirs @ right) / np.maximum(cosang, 1e-6)
    v = (dirs @ upv) / np.maximum(cosang, 1e-6)
    half = math.tan(size / 2)
    uu, vv = u / half, v / half
    r = np.hypot(uu, vv)
    tex = np.asarray(Image.open(body['texture_path']).convert('RGBA'), np.float32) / 255
    th, tw = tex.shape[:2]
    inside = front & (r <= 1)
    tx = np.clip(((uu + 1) / 2 * tw).astype(int), 0, tw - 1)
    ty = np.clip(((1 - vv) / 2 * th).astype(int), 0, th - 1)
    face = tex[ty, tx]
    add = face[:, :3] * face[:, 3:4] * body.get('strength', 1.0)
    img = img.copy()
    img[inside] = img[inside] + add[inside] if body.get('additive', True) else face[inside, :3]
    glow = body.get('glow')
    if glow is not None:
        gc = np.array([(glow >> 16) & 255, (glow >> 8) & 255, glow & 255], np.float32) / 255
        ring = front & (r > 1) & (r < 1.08)
        a = (1 - (r - 1) / 0.08) * 0.6
        img[ring] = img[ring] + gc[None, :] * a[ring, None]
    return img


# --- pasting a gateway site the way GatewayFramePiece writes it -------------------------------------------

def frame_cells(frame_path):
    d = json.load(open(frame_path))
    cells = {}
    for y, layer in enumerate(d['layers']):
        for z, row in enumerate(layer):
            for x, ch in enumerate(row):
                if ch != ' ':
                    cells[(x, y, z)] = ch
    return d, cells


def gateway_site_states(frame_path, site_path):
    """{(x, y, z) in frame-local space: state} for a site and its frame as pre-generation writes them:
    site cells from their key, frame cells from the site's frame_palette or their `place`, the
    field cleared to air, the keystone unattuned, gatestone unlit. Local +x is the viewer's right,
    +z runs back from the front."""
    fd, fcells = frame_cells(frame_path)
    sd, scells = frame_cells(site_path)
    off = sd['frame_offset']
    pal = sd.get('frame_palette', {})
    out = {}
    for (x, y, z), ch in scells.items():
        e = sd['key'][ch]
        out[(x - off[0], y - off[1], z - off[2])] = e['block']
    for (x, y, z), ch in fcells.items():
        if ch == fd['field']:
            out[(x, y, z)] = AIR
            continue
        e = fd['key'][ch]
        if 'block' in e:
            st = e['block']
            if st.endswith('gateway_keystone'):
                st += '[active=false]'
            if st.endswith('inscribed_gatestone'):
                st += '[lit=false]'
        else:
            st = pal.get(ch, e['place'])
        out[(x, y, z)] = default_state(st)
    return out, sd, off


def default_state(st):
    """The state pre-generation places for a bare block id."""
    if '[' in st:
        return st
    short = st.split(':', 1)[1]
    if short.endswith('_slab'):
        return st + '[type=bottom,waterlogged=false]'
    if short in ('polished_basalt', 'basalt', 'quartz_pillar', 'warped_stem', 'crimson_stem', 'warped_hyphae',
                 'crimson_hyphae', 'bone_block', 'anorthosite_pillar') or short.endswith('_pillar'):
        return st + '[axis=y]'
    return st


def paste_gateway(scene, frame_path, site_path, origin):
    """Write a site and its frame facing south with the frame's local (0, 0, 0) at `origin` (local x
    -> east, local z -> north), then the foundation: under each bottom-layer site cell, its own
    block down through anything replaceable, at most 8."""
    states, sd, off = gateway_site_states(frame_path, site_path)
    ox, oy, oz = origin

    def world(x, y, z):
        return ox + x, oy + y, oz - z
    for (x, y, z), st in states.items():
        scene.set(*world(x, y, z), default_state(st))
    bottom = -off[1]
    for (x, y, z), st in states.items():
        if y != bottom or st == AIR:
            continue
        wx, wy, wz = world(x, y, z)
        for depth in range(1, 9):
            cur = scene.get(wx, wy - depth, wz)
            if cur is None or not replaceable(cur):
                break
            scene.set(wx, wy - depth, wz, default_state(st))


REPLACEABLE = ('minecraft:air', 'minecraft:cave_air', 'minecraft:lava', 'minecraft:fire', 'minecraft:soul_fire',
               'minecraft:crimson_roots', 'minecraft:warped_roots', 'minecraft:nether_sprouts',
               'minecraft:twisting_vines', 'minecraft:twisting_vines_plant', 'minecraft:weeping_vines',
               'minecraft:weeping_vines_plant')


def replaceable(state):
    return rsp.split_state(state)[0] in REPLACEABLE


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('region')
    ap.add_argument('--eye', type=float, nargs=3, required=True)
    ap.add_argument('--target', type=float, nargs=3, required=True)
    ap.add_argument('--radius', type=int, default=160)
    ap.add_argument('--haze', type=float, nargs=2, default=(24, 160))
    ap.add_argument('--fov', type=float, default=70)
    ap.add_argument('--vanilla-textures')
    ap.add_argument('--out', default='nether-view.png')
    a = ap.parse_args()
    world = RegionWorld(a.region)
    ex, ey, ez = a.eye
    r = a.radius
    scene = Scene(world, int(ex) - r, int(ex) + r, 0, 255, int(ez) - r, int(ez) + r)
    biome = world.biome(int(ex), min(int(ey), 120), int(ez))
    fog = FOG.get(biome, (0.2, 0.03, 0.03))
    img = scene.render(a.eye, a.target, fov=a.fov, haze=tuple(a.haze), fog=fog,
                       vanilla_assets=a.vanilla_textures)
    img.save(a.out)
    print('wrote', a.out, 'biome', biome)


# The Nether biomes' own fog colours (their biome files' fog_color), the colour of the haze and of
# the sky overhead wherever the sky file sets no colour of its own.
def _rgb(v):
    return ((v >> 16) & 255) / 255, ((v >> 8) & 255) / 255, (v & 255) / 255


FOG = {
    'minecraft:nether_wastes': _rgb(0x330808), 'minecraft:crimson_forest': _rgb(0x330303),
    'minecraft:warped_forest': _rgb(0x1A051A), 'minecraft:soul_sand_valley': _rgb(0x1B4745),
    'minecraft:basalt_deltas': _rgb(0x685F70),
}


if __name__ == '__main__':
    main()
