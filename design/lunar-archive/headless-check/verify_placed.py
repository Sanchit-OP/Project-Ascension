"""Throwaway: compare what the server actually placed with the two templates, block for block.

Reads the structure start for archive_test:archive from the Moon's region files (its pieces'
positions and rotations), then every non-void template cell is rotated and offset into world
space and compared with the world's block state there. Also reads block entities: plinth entries,
chest loot tables. And measures the ground around the entrance against the paving row.
"""
import glob
import os
import struct
import sys
import zlib
from collections import Counter

sys.path.insert(0, 'E:/code/Project-Ascension-archive/tools/worldgen-diagnostics')
sys.path.insert(0, 'E:/code/Project-Ascension-archive/tools')
from scan_region import parse_nbt  # noqa: E402
import importlib.util  # noqa: E402

spec = importlib.util.spec_from_file_location('rsp', 'E:/code/Project-Ascension-archive/tools/render-structure-preview.py')
rsp = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rsp)

REGION = 'E:/code/Project-Ascension-archive/run/server/archive-test/dimensions/ascension_worlds/moon/region'
TPL = 'E:/code/Project-Ascension-archive/modules/worlds/src/main/resources/data/ascension_worlds/structure/lunar_archive'


def chunks():
    for path in glob.glob(REGION + '/*.mca'):
        with open(path, 'rb') as f:
            header = f.read(4096)
            if len(header) < 4096:
                continue
            for i in range(1024):
                if header[i * 4 + 3] == 0:
                    continue
                f.seek(int.from_bytes(header[i * 4:i * 4 + 3], 'big') * 4096)
                length = struct.unpack('>I', f.read(4))[0]
                if f.read(1)[0] != 2:
                    continue
                yield parse_nbt(zlib.decompress(f.read(length - 1)))


def state_name(entry):
    props = entry.get('Properties', {})
    if not props:
        return entry['Name']
    return entry['Name'] + '[' + ','.join(f'{k}={v}' for k, v in sorted(props.items())) + ']'


sections = {}
bes = {}
start = None
for ch in chunks():
    cx, cz = ch['xPos'], ch['zPos']
    for s in ch.get('sections', []):
        if 'block_states' in s:
            sections[(cx, s['Y'], cz)] = s
    for be in ch.get('block_entities', []):
        bes[(be['x'], be['y'], be['z'])] = be
    for sid, st in ch.get('structures', {}).get('starts', {}).items():
        if 'archive' in sid and st.get('id') != 'INVALID':
            start = st


def block(x, y, z):
    s = sections.get((x >> 4, y >> 4, z >> 4))
    if s is None:
        return None
    bs = s['block_states']
    pal = bs['palette']
    if len(pal) == 1 or 'data' not in bs:
        return state_name(pal[0])
    bits = max(4, (len(pal) - 1).bit_length())
    idx = (y & 15) * 256 + (z & 15) * 16 + (x & 15)
    per = 64 // bits
    v = (bs['data'][idx // per] & 0xFFFFFFFFFFFFFFFF) >> ((idx % per) * bits) & ((1 << bits) - 1)
    return state_name(pal[v])


CW = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'}


def rot_state(st, rot):
    n = {'NONE': 0, 'CLOCKWISE_90': 1, 'CLOCKWISE_180': 2, 'COUNTERCLOCKWISE_90': 3}[rot]
    name, props = rsp.split_state(st)
    for _ in range(n):
        if 'facing' in props and props['facing'] in CW:
            props['facing'] = CW[props['facing']]
        if 'axis' in props and props['axis'] != 'y':
            props['axis'] = 'z' if props['axis'] == 'x' else 'x'
    if not props:
        return name
    return name + '[' + ','.join(f'{k}={v}' for k, v in sorted(props.items())) + ']'


def rot_pos(x, z, rot):
    return {'NONE': (x, z), 'CLOCKWISE_90': (-z, x), 'CLOCKWISE_180': (-x, -z),
            'COUNTERCLOCKWISE_90': (z, -x)}[rot]


def find_piece(name, vox, tags):
    """/place structure records no start, so recover each piece's origin and rotation from a
    unique anchor block, then keep the rotation under which the most template cells match."""
    if name == 'vault':
        tpos = next(p for p, s in vox.items() if s.endswith(':archive_orrery'))
        wpos = world_find(':archive_orrery')
    else:
        tpos = next(p for p, t in tags.items() if t.get('entry', '').endswith('the_road'))
        wpos = [p for p, b in bes.items() if b.get('entry', '').endswith('the_road')]
    best = None
    sample = list(vox.items())[::37]
    for rot in ('NONE', 'CLOCKWISE_90', 'CLOCKWISE_180', 'COUNTERCLOCKWISE_90'):
        dx, dz = rot_pos(tpos[0], tpos[2], rot)
        for w in wpos:
            origin = (w[0] - dx, w[1] - tpos[1], w[2] - dz)
            score = sum(1 for (x, y, z), st in sample
                        if block(origin[0] + rot_pos(x, z, rot)[0], origin[1] + y,
                                 origin[2] + rot_pos(x, z, rot)[1]) == rot_state(st, rot))
            if best is None or score > best[0]:
                best = (score, origin, rot)
    return best[1], best[2]


def world_find(suffix):
    out = []
    for (cx, sy, cz), s in sections.items():
        pal = s['block_states']['palette']
        if any(state_name(e).endswith(suffix) for e in pal):
            for y in range(16):
                for z in range(16):
                    for x in range(16):
                        wx, wy, wz = cx * 16 + x, sy * 16 + y, cz * 16 + z
                        if (block(wx, wy, wz) or '').endswith(suffix):
                            out.append((wx, wy, wz))
    return out


