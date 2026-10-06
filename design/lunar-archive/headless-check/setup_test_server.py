"""Throwaway headless test for the Lunar Archive templates. Never committed.

Prepares run/server in the design worktree: a throwaway world (archive-test), RCON on, and a test
datapack that stands in for the logic session's worldgen (a jigsaw structure using the start pool
-> entrance.nbt and the vault pool -> vault.nbt), so /place structure can exercise the real
jigsaw join, start height and template parsing.
"""
import json
import os
import shutil

WT = 'E:/code/Project-Ascension-archive'
MAIN = 'E:/code/Project-Ascension'
SRV = os.path.join(WT, 'run/server')
os.makedirs(SRV, exist_ok=True)
shutil.copy(os.path.join(MAIN, 'run/server/eula.txt'), SRV)
props = open(os.path.join(MAIN, 'run/server/server.properties'), encoding='utf-8').read().splitlines()
out = []
for line in props:
    k = line.split('=', 1)[0]
    if k == 'level-name':
        line = 'level-name=archive-test'
    elif k == 'enable-rcon':
        line = 'enable-rcon=true'
    elif k == 'rcon.password':
        line = 'rcon.password=archive-test-local'
    out.append(line)
open(os.path.join(SRV, 'server.properties'), 'w', encoding='utf-8', newline='\n').write('\n'.join(out) + '\n')

DP = os.path.join(SRV, 'archive-test/datapacks/archive-test')


def w(rel, obj):
    p = os.path.join(DP, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)


def pool(location):
    return {'fallback': 'minecraft:empty', 'elements': [{'weight': 1, 'element': {
        'element_type': 'minecraft:single_pool_element', 'location': location,
        'projection': 'rigid', 'processors': 'minecraft:empty'}}]}


w('pack.mcmeta', {'pack': {'pack_format': 48, 'description': 'Lunar Archive template test (throwaway)'}})
w('data/archive_test/worldgen/template_pool/start.json', pool('ascension_worlds:lunar_archive/entrance'))
w('data/ascension_worlds/worldgen/template_pool/lunar_archive/vault.json', pool('ascension_worlds:lunar_archive/vault'))
w('data/archive_test/worldgen/structure/archive.json', {
    'type': 'minecraft:jigsaw', 'biomes': '#ascension_worlds:moon_biomes', 'step': 'underground_structures',
    'spawn_overrides': {}, 'terrain_adaptation': 'none', 'start_pool': 'archive_test:start', 'size': 2,
    'start_height': {'absolute': -21}, 'project_start_to_heightmap': 'WORLD_SURFACE_WG',
    'max_distance_from_center': 80, 'use_expansion_hack': False})
print('ready:', SRV)
