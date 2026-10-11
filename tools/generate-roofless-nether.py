#!/usr/bin/env python3
"""Builds the roofless Nether's noise settings (ADR-0017) from vanilla's own.

Planet 3 is the real Nether opened to the sky. Vanilla's Nether is solid rock up to a bedrock
ceiling: its terrain density is pushed toward solid over the top 24 blocks of the noise (y 104 to
128), and its surface rule lays bedrock and plain netherrack over the top 5. This takes vanilla's
`minecraft:nether` noise settings and changes exactly that, nothing else:

- The top slide's target flips from solid (+0.9375) to air (-0.9375), and starts lower
  (TOP_SLIDE_FROM), so the rock thins out into open sky instead of closing into a ceiling.
- The bedrock roof rule and the "top 5 blocks are netherrack" rule are dropped, so exposed tops get
  their biome's ground (nylium, soul sand, basalt) like any other floor.

Everything else (biomes, the lava sea at y 32, the caves below, the bedrock floor, the noise
router's other fields) stays vanilla's, so fortresses, bastions, ancient debris and every Nether
mob arrive through their usual biome tags.

Reads vanilla's file from the Minecraft resources jar ModDevGradle unpacks, so the output follows
the game version rather than a copy kept by hand.

    python tools/generate-roofless-nether.py
"""
import glob
import json
import os
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'modules', 'worlds', 'src', 'main', 'resources', 'data', 'ascension_worlds',
                   'worldgen', 'noise_settings', 'nether.json')
VANILLA = 'data/minecraft/worldgen/noise_settings/nether.json'

# Where the rock starts thinning toward open sky. Vanilla closes its ceiling from 104.
TOP_SLIDE_FROM = 104
TOP_SLIDE_TO = 128
# Vanilla's ceiling target is +0.9375 (solid); the same size, negative, is air.
SKY_TARGET = -0.9375


def vanilla_settings():
    jars = glob.glob(os.path.join(ROOT, 'modules', '*', 'build', 'moddev', 'artifacts', '*minecraft-resources.jar'))
    jars += glob.glob(os.path.join(ROOT, 'modules', 'compat', '*', 'build', 'moddev', 'artifacts', '*minecraft-resources.jar'))
    if not jars:
        raise SystemExit('No Minecraft resources jar found: run any Gradle build first.')
    with zipfile.ZipFile(jars[0]) as jar:
        return json.loads(jar.read(VANILLA))


def find_top_slide(node):
    """The `mul(y_clamped_gradient 1 -> 0 toward the top, add(-0.9375, base noise))` term."""
    if isinstance(node, dict):
        if node.get('type') == 'minecraft:add' and node.get('argument1') == 0.9375:
            inner = node['argument2']
            gradient = inner.get('argument1', {}) if isinstance(inner, dict) else {}
            if gradient.get('type') == 'minecraft:y_clamped_gradient' and gradient.get('from_value') == 1.0:
                return node
        for value in node.values():
            found = find_top_slide(value)
            if found:
                return found
    return None


def open_the_sky(settings):
    slide = find_top_slide(settings['noise_router']['final_density'])
    if slide is None:
        raise SystemExit("Vanilla's top slide was not where expected: the Nether's settings changed.")
    gradient = slide['argument2']['argument1']
    offset = slide['argument2']['argument2']
    gradient['from_y'] = TOP_SLIDE_FROM
    gradient['to_y'] = TOP_SLIDE_TO
    slide['argument1'] = SKY_TARGET
    offset['argument1'] = -SKY_TARGET


def drop_the_roof(settings):
    rules = settings['surface_rule']['sequence']
    kept = [rule for rule in rules if not touches_top(rule)]
    if len(rules) - len(kept) != 2:
        raise SystemExit(f'Expected to drop 2 roof rules, would drop {len(rules) - len(kept)}.')
    settings['surface_rule']['sequence'] = kept


def touches_top(rule):
    text = json.dumps(rule.get('if_true', {}))
    return 'below_top' in text


def main():
    settings = vanilla_settings()
    open_the_sky(settings)
    drop_the_roof(settings)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8', newline='\n') as out:
        json.dump(settings, out, indent=2)
        out.write('\n')
    print('wrote', os.path.relpath(OUT, ROOT))


if __name__ == '__main__':
    main()
