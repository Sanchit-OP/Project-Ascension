#!/usr/bin/env python3
"""The combat items' build paths (docs/progression/build-paths.md, resource-economy.md section 5).

One table below says everything: the 6 basic components, the 17 epic components, the 6 boss hearts,
and what makes each of the 26 combat items. This script writes, from that table alone:

  * the 29 component and heart textures (original pixel art, seeded by nothing: it is deterministic),
  * their item models,
  * every shapeless crafting recipe (components, boots, legendaries),
  * the hearts' loot tables and loot modifiers (Warden and Wither in combat; the Cataclysm bosses in
    ascension-compat-cataclysm),
  * their English names (merged into the lang file, existing keys untouched).

Materials from other modules are asked for by `c:` tag, never by id (ADR-0003, ADR-0011).
Re-running reproduces every file byte for byte.

Run:  python tools/generate-build-paths.py
"""

import importlib.util
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
HERE = os.path.dirname(os.path.abspath(__file__))
COMBAT = os.path.join(ROOT, 'modules', 'combat', 'src', 'main', 'resources')
CATA = os.path.join(ROOT, 'modules', 'compat', 'cataclysm', 'src', 'main', 'resources')


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


rune = load('rune_art', 'generate-rune-art.py')
hexc, shade = rune.hexc, rune.shade

# ---------------------------------------------------------------------------------------------
# The table
# ---------------------------------------------------------------------------------------------

TI = '#c:ingots/titanium'          # Earth
HE = '#c:raw_materials/helium'     # the Moon


def mc(name):
    return 'minecraft:' + name


def ours(name):
    return 'ascension_combat:' + name


# name: (display, shape, ramp, world accent, ingredients). Ingredients are (id or #tag, count).
BASICS = {
    'long_sword': ('Long Sword', 'sword', 'titanium', None, [(TI, 3), (mc('stick'), 1)]),
    'dagger': ('Dagger', 'dagger', 'titanium', None, [(TI, 2), (mc('stick'), 1)]),
    'cloth_armor': ('Cloth Armor', 'vest', 'cloth', None, [(mc('leather'), 5)]),
    'null_magic_mantle': ('Null-Magic Mantle', 'cloak', 'amethyst', None,
                          [(mc('amethyst_shard'), 4), (mc('echo_shard'), 1)]),
    'ruby_crystal': ('Ruby Crystal', 'crystal', 'ruby', None, [(mc('diamond'), 1), (mc('redstone'), 4)]),
    'basic_boots': ('Boots', 'boot', 'cloth', None, [(mc('leather'), 2), (TI, 1)]),
}

EPICS = {
    # The Moon's helium
    'pickaxe': ('Pickaxe', 'pick', 'titanium', 'moon', [(ours('long_sword'), 1), (ours('dagger'), 1), (HE, 3)]),
    'caulfields_warhammer': ('Caulfield\'s Warhammer', 'hammer', 'titanium', 'moon',
                             [(ours('long_sword'), 2), (HE, 6)]),
    'giants_belt': ('Giant\'s Belt', 'belt', 'ruby', 'moon',
                    [(ours('ruby_crystal'), 2), (ours('cloth_armor'), 1), (HE, 3)]),
    'recurve_bow': ('Recurve Bow', 'bow', 'titanium', 'moon', [(mc('bow'), 1), (ours('dagger'), 1), (HE, 3)]),
    'chain_vest': ('Chain Vest', 'vest', 'titanium', 'moon', [(ours('cloth_armor'), 2), (HE, 3)]),
    'seekers_armguard': ('Seeker\'s Armguard', 'bracer', 'cloth', 'moon',
                         [(ours('cloth_armor'), 1), (ours('dagger'), 1), (HE, 3)]),
    'last_whisper': ('Last Whisper', 'sword', 'cold', 'moon',
                     [(ours('long_sword'), 1), (ours('dagger'), 1), (HE, 4)]),
    'noonquiver': ('Noonquiver', 'bow', 'cold', 'moon', [(ours('dagger'), 2), (mc('string'), 1), (HE, 3)]),
    # Planet 3: the Nether
    'bf_sword': ('B. F. Sword', 'sword', 'dark', 'nether', [(ours('long_sword'), 2), (mc('netherite_ingot'), 1)]),
    'phage': ('Phage', 'sword', 'ruby', 'nether', [(ours('long_sword'), 1), (ours('ruby_crystal'), 1), (mc('blaze_powder'), 4)]),
    'kindlegem': ('Kindlegem', 'orb', 'ember', 'nether', [(ours('ruby_crystal'), 1), (mc('magma_cream'), 2)]),
    'vampiric_scepter': ('Vampiric Scepter', 'scepter', 'ruby', 'nether',
                         [(ours('long_sword'), 1), (mc('ghast_tear'), 2)]),
    'serrated_dirk': ('Serrated Dirk', 'dagger', 'dark', 'nether', [(ours('dagger'), 2), (mc('quartz'), 2)]),
    'negatron_cloak': ('Negatron Cloak', 'cloak', 'amethyst', 'nether',
                       [(ours('null_magic_mantle'), 2), (mc('glowstone_dust'), 2)]),
    'spectres_cowl': ('Spectre\'s Cowl', 'cloak', 'dark', 'nether',
                      [(ours('null_magic_mantle'), 1), (ours('ruby_crystal'), 1), (mc('nether_wart'), 2)]),
    'hearthbound_axe': ('Hearthbound Axe', 'axe', 'ember', 'nether',
                        [(ours('long_sword'), 1), (ours('dagger'), 1), (mc('blaze_rod'), 2)]),
    'executioners_calling': ('Executioner\'s Calling', 'axe', 'dark', 'nether',
                             [(ours('long_sword'), 1), (ours('dagger'), 1), (mc('wither_skeleton_skull'), 1)]),
}

# name: (display, entity loot table, killed by a player, palette)
HEARTS = {
    'warden_heart': ('Heart of the Warden', 'minecraft:entities/warden', 'sculk'),
    'ender_guardian_heart': ('Heart of the Ender Guardian', 'cataclysm:entities/ender_guardian', 'end'),
    'ender_golem_heart': ('Heart of the Ender Golem', 'cataclysm:entities/ender_golem', 'stone'),
    'netherite_monstrosity_heart': ('Heart of the Netherite Monstrosity', 'cataclysm:entities/netherite_monstrosity', 'dark'),
    'ignis_heart': ('Heart of Ignis', 'cataclysm:entities/ignis', 'ember'),
    'wither_heart': ('Heart of the Wither', 'minecraft:entities/wither', 'wither'),
}
CATACLYSM_HEARTS = ['ender_guardian_heart', 'ender_golem_heart', 'netherite_monstrosity_heart', 'ignis_heart']

# The 26 combat items. Three tiers, each on its own line: Earth (basics) is implied everywhere.
LEGENDARIES = {
    # Weapons, melee
    'veigos_blade': [('pickaxe', 1), ('serrated_dirk', 1), ('long_sword', 1)],
    'darius_axe': [('phage', 1), ('caulfields_warhammer', 1)],
    'spear_of_shojin': [('last_whisper', 1), ('hearthbound_axe', 1), ('dagger', 1)],
    'divine_sunderer': [('bf_sword', 1), ('caulfields_warhammer', 1), ('ignis_heart', 1)],
    'darken_blade': [('bf_sword', 1), ('vampiric_scepter', 1), ('pickaxe', 1)],
    'ragefang': [('phage', 1), ('pickaxe', 1), ('dagger', 1)],
    # The hardest weapon: three epics and the Warden's heart.
    'endless_hunger': [('bf_sword', 1), ('giants_belt', 1), ('vampiric_scepter', 1), ('warden_heart', 1)],
    'wraithblade': [('serrated_dirk', 1), ('last_whisper', 1), ('dagger', 1)],
    # Ranged
    'mortal_reminder': [('last_whisper', 1), ('recurve_bow', 1), ('executioners_calling', 1)],
    'heartwood_of_pallas': [('recurve_bow', 1), ('noonquiver', 1), ('spectres_cowl', 1)],
    'galeforce': [('noonquiver', 1), ('recurve_bow', 1), ('kindlegem', 1)],
    # Helmets, chestplates, leggings
    'protean_mask': [('spectres_cowl', 1), ('chain_vest', 1), ('ruby_crystal', 1)],
    'edge_of_night': [('negatron_cloak', 1), ('seekers_armguard', 1), ('dagger', 1)],
    'gargoyle_stoneplate': [('chain_vest', 1), ('negatron_cloak', 1), ('cloth_armor', 1)],
    'guardian_wing': [('chain_vest', 1), ('spectres_cowl', 1), ('ender_guardian_heart', 1)],
    'rocketbelt': [('giants_belt', 1), ('kindlegem', 1), ('ruby_crystal', 1)],
    'collectors_pants': [('executioners_calling', 1), ('seekers_armguard', 1), ('cloth_armor', 1)],
    'unending_despair': [('giants_belt', 1), ('spectres_cowl', 1), ('wither_heart', 1)],
    # Off-hand
    'zonias_hourglass': [('seekers_armguard', 1), ('kindlegem', 1), ('netherite_monstrosity_heart', 1)],
    'steraks_harness': [('giants_belt', 1), ('vampiric_scepter', 1), ('ender_golem_heart', 1)],
    # Boots: never a heart. Four can be made from Earth alone.
    'ninja_tabi': [('basic_boots', 1), ('cloth_armor', 1)],
    'mercury_treads': [('basic_boots', 1), ('null_magic_mantle', 1)],
    'swifty': [('basic_boots', 1), ('dagger', 1)],
    'berserkers_greaves': [('basic_boots', 1), ('dagger', 2)],
    'wizards_agony': [('basic_boots', 1), (HE, 3)],
    'tasted_blood': [('basic_boots', 1), (mc('ghast_tear'), 2)],
}

DISPLAY = {
    'veigos_blade': None,  # existing items keep their existing names
}

# ---------------------------------------------------------------------------------------------
# Art
# ---------------------------------------------------------------------------------------------


class Pix:
    """A silhouette built from pixels and lines, with the masks `shade` wants."""

    def __init__(self):
        self.size = 16
        self.fill = set()
        self.shine = set()

    def px(self, x, y):
        if 1 <= x <= 14 and 1 <= y <= 14:
            self.fill.add((x, y))

    def line(self, x0, y0, x1, y1, thick=1):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            x = round(x0 + (x1 - x0) * i / n)
            y = round(y0 + (y1 - y0) * i / n)
            self.px(x, y)
            if thick > 1:
                self.px(x + 1, y)
                self.px(x, y + 1) if thick > 2 else None

    def rows(self, y0, spans):
        for i, (a, b) in enumerate(spans):
            for x in range(a, b + 1):
                self.px(x, y0 + i)

    def masks(self):
        return {'fill': set(self.fill), 'groove': set(), 'shine': set(self.shine) & self.fill}


def shape_pixels(kind):
    s = Pix()
    if kind == 'sword':
        s.line(4, 11, 13, 2, 2)
        s.line(3, 8, 7, 12)          # guard
        s.line(2, 13, 4, 11)         # grip
        s.shine |= {(8, 7), (10, 5), (12, 3)}
    elif kind == 'dagger':
        s.line(6, 10, 12, 3, 2)
        s.line(4, 9, 8, 13)
        s.line(3, 13, 5, 11)
        s.shine |= {(9, 6), (11, 4)}
    elif kind == 'axe':
        s.line(3, 13, 11, 4)
        s.rows(2, [(8, 12), (8, 13), (9, 13), (10, 13), (11, 12), (12, 12)])
        s.shine |= {(9, 3), (10, 4)}
    elif kind == 'hammer':
        s.line(3, 13, 10, 7)
        s.rows(2, [(6, 12), (6, 13), (6, 13), (6, 13), (7, 12), (9, 11)])
        s.shine |= {(7, 3), (8, 3)}
    elif kind == 'pick':
        s.line(3, 13, 10, 6)
        s.line(4, 4, 7, 3)
        s.line(7, 3, 12, 5)
        s.line(12, 5, 13, 9)
        s.shine |= {(8, 3), (10, 4)}
    elif kind == 'vest':
        s.rows(2, [(3, 5), (3, 5), (3, 12), (3, 12), (3, 12), (4, 11), (4, 11), (4, 11),
                   (4, 11), (4, 11), (4, 11), (4, 11)])
        for y in range(2, 6):
            for x in range(6, 10):
                s.fill.discard((x, y)) if y < 4 else None
        s.rows(2, [(10, 12), (10, 12)])
        s.shine |= {(4, 4), (5, 3), (4, 6)}
    elif kind == 'cloak':
        s.rows(2, [(6, 9), (5, 10), (5, 10), (4, 11), (4, 11), (3, 12), (3, 12), (3, 12),
                   (2, 13), (2, 13), (3, 12), (4, 11)])
        s.shine |= {(6, 3), (5, 5), (4, 7)}
    elif kind == 'crystal':
        s.rows(2, [(7, 8), (6, 9), (5, 10), (4, 11), (4, 11), (4, 11), (5, 10), (5, 10), (6, 9), (6, 9), (7, 8), (7, 8)])
        s.shine |= {(7, 4), (6, 5), (6, 6)}
    elif kind == 'boot':
        s.rows(2, [(5, 8), (5, 8), (5, 8), (5, 8), (5, 8), (5, 8), (4, 9), (3, 13), (3, 13), (3, 13), (2, 13)])
        s.shine |= {(5, 3), (5, 5), (4, 9)}
    elif kind == 'belt':
        s.rows(5, [(1, 14), (1, 14), (1, 14), (1, 14), (1, 14), (1, 14)])
        s.rows(4, [(6, 9)])
        s.rows(11, [(6, 9)])
        s.shine |= {(2, 6), (3, 6), (7, 8)}
    elif kind == 'bow':
        s.line(11, 2, 4, 8)
        s.line(4, 8, 11, 14)
        s.line(11, 2, 11, 14)        # the string
        s.line(5, 8, 12, 8)          # an arrow
        s.shine |= {(8, 4), (6, 7)}
    elif kind == 'bracer':
        s.rows(3, [(5, 10), (4, 11), (4, 11), (4, 11), (4, 11), (4, 11), (4, 11), (4, 11), (5, 10), (5, 10)])
        s.shine |= {(5, 4), (5, 6)}
    elif kind == 'orb':
        s.rows(3, [(6, 9), (5, 10), (4, 11), (4, 11), (4, 11), (4, 11), (4, 11), (4, 11), (5, 10), (6, 9)])
        s.shine |= {(6, 5), (5, 6)}
    elif kind == 'scepter':
        s.line(4, 13, 10, 6)
        s.rows(2, [(9, 11), (8, 12), (8, 12), (9, 11)])
        s.shine |= {(9, 3), (8, 4)}
    elif kind == 'heart':
        s.rows(3, [(3, 6), (2, 7), (2, 7), (2, 13), (2, 13), (2, 13), (3, 12), (4, 11), (5, 10), (6, 9), (7, 8)])
        s.rows(3, [(9, 12), (8, 13)])
        s.shine |= {(4, 5), (3, 6), (10, 5)}
    else:
        raise KeyError(kind)
    return s


# Ramps: outline, dark, mid, light, highlight. Off teal (the gateway network's), apart in value.
RAMPS = {
    'titanium': ['#1d2330', '#4b5870', '#7d8da8', '#aab9d0', '#e0e9f6'],
    'cloth': ['#2a1d12', '#5a3d22', '#8c6238', '#b98a56', '#e0b88a'],
    'amethyst': ['#1f1438', '#4a2f86', '#7a58c0', '#a888e6', '#e0d0ff'],
    'ruby': ['#3a0a14', '#7e1830', '#c0304e', '#e8607c', '#ffc0cc'],
    'cold': ['#161c2e', '#3a4a78', '#6a80b8', '#98b0e4', '#d8e6ff'],
    'dark': ['#0e0c14', '#2c2838', '#54506a', '#7c7894', '#b8b4d0'],
    'ember': ['#3a1204', '#8a2c0c', '#d4561c', '#f08a34', '#ffd070'],
    'sculk': ['#06161a', '#0e3a42', '#1c6a72', '#3aa0a0', '#9af0e0'],
    'end': ['#120a22', '#3a1a68', '#6a3ca8', '#a070e0', '#e8c8ff'],
    'stone': ['#1a1620', '#46404e', '#6e6878', '#968fa0', '#cfc8d8'],
    'wither': ['#0a0a0c', '#26262c', '#46464e', '#6e6e78', '#b0b0bc'],
}
# A world's mark on a component: three pixels at the hilt's corner.
ACCENTS = {
    'moon': ['#8a6a14', '#e8c43c', '#fff0a0'],      # helium's yellow line
    'nether': ['#6a1008', '#e0481c', '#ffa040'],
}


def draw_component(kind, palette, accent):
    ramp = [hexc(h) for h in RAMPS[palette]]
    img = shade(shape_pixels(kind), ramp)
    if accent:
        c = [hexc(h) for h in ACCENTS[accent]]
        for (x, y), col in zip(((1, 1), (2, 1), (1, 2)), (c[2], c[1], c[1])):
            img.putpixel((x, y), col)
        img.putpixel((2, 2), c[0])
    return img


def draw_heart(palette):
    """The hearts are our own design: a faceted stone heart with a pulse of light in its core."""
    ramp = [hexc(h) for h in RAMPS[palette]]
    img = shade(shape_pixels('heart'), ramp)
    for (x, y) in ((6, 6), (7, 6), (6, 7)):
        img.putpixel((x, y), ramp[4])
    img.putpixel((8, 8), ramp[3])
    for x in range(5, 11):
        img.putpixel((x, 11), ramp[1]) if img.getpixel((x, 11))[3] else None
    return img


# ---------------------------------------------------------------------------------------------
# Writing
# ---------------------------------------------------------------------------------------------

def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def ingredient(spec):
    return {'tag': spec[1:]} if spec.startswith('#') else {'item': spec}


def recipe(result, parts):
    ingredients = []
    for spec, count in parts:
        ingredients += [ingredient(spec)] * count
    assert len(ingredients) <= 9, (result, len(ingredients))
    return {'type': 'minecraft:crafting_shapeless', 'category': 'equipment',
            'ingredients': ingredients, 'result': {'id': ours(result), 'count': 1}}


def lang_entries():
    out = {}
    for name, v in {**BASICS, **EPICS}.items():
        out['item.ascension_combat.' + name] = v[0]
    for name, v in HEARTS.items():
        out['item.ascension_combat.' + name] = v[0]
    return out


def main():
    tex = os.path.join(COMBAT, 'assets', 'ascension_combat', 'textures', 'item')
    mdl = os.path.join(COMBAT, 'assets', 'ascension_combat', 'models', 'item')
    rec = os.path.join(COMBAT, 'data', 'ascension_combat', 'recipe', 'build')
    os.makedirs(tex, exist_ok=True)
    os.makedirs(mdl, exist_ok=True)

    components = {**BASICS, **EPICS}
    for name, (_, kind, palette, accent, parts) in components.items():
        draw_component(kind, palette, accent).save(os.path.join(tex, name + '.png'))
        write_json(os.path.join(rec, name + '.json'), recipe(name, parts))
    for name, (_, _, palette) in HEARTS.items():
        draw_heart(palette).save(os.path.join(tex, name + '.png'))
    for name in list(components) + list(HEARTS):
        write_json(os.path.join(mdl, name + '.json'),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'ascension_combat:item/' + name}})

    known = set(components) | set(HEARTS)
    for name, parts in LEGENDARIES.items():
        resolved = []
        for spec, count in parts:
            resolved.append((ours(spec) if spec in known else spec, count))
        write_json(os.path.join(rec, name + '.json'), recipe(name, resolved))

    # Heart drops. One heart per kill, only when a player did it.
    def heart_table(name):
        return {'type': 'minecraft:entity', 'pools': [{
            'rolls': 1,
            'entries': [{'type': 'minecraft:item', 'name': ours(name)}],
            'conditions': [{'condition': 'minecraft:killed_by_player'}]}]}

    def modifier(table_id, loot_id, conditions=None):
        data = {'type': 'neoforge:add_table',
                'conditions': [{'condition': 'neoforge:loot_table_id', 'loot_table_id': loot_id}],
                'table': table_id}
        if conditions:
            data = {'neoforge:conditions': conditions, **data}
        return data

    combat_data = os.path.join(COMBAT, 'data', 'ascension_combat')
    cata_data = os.path.join(CATA, 'data', 'ascension_compat_cataclysm')
    for name, (_, loot_id, _) in HEARTS.items():
        if name in CATACLYSM_HEARTS:
            write_json(os.path.join(cata_data, 'loot_table', 'entities', name + '.json'), heart_table(name))
            write_json(os.path.join(cata_data, 'loot_modifiers', name + '.json'),
                       modifier('ascension_compat_cataclysm:entities/' + name, loot_id,
                                [{'type': 'neoforge:mod_loaded', 'modid': 'ascension_combat'}]))
        else:
            write_json(os.path.join(combat_data, 'loot_table', 'entities', name + '.json'), heart_table(name))
            write_json(os.path.join(combat_data, 'loot_modifiers', name + '.json'),
                       modifier('ascension_combat:entities/' + name, loot_id))

    # Global loot modifier lists: add ours, keep what is there.
    def merge_entries(path, ids):
        with open(path, encoding='utf-8') as f:
            data = json.load(f)
        for i in ids:
            if i not in data['entries']:
                data['entries'].append(i)
        write_json(path, data)

    merge_entries(os.path.join(COMBAT, 'data', 'neoforge', 'loot_modifiers', 'global_loot_modifiers.json'),
                  ['ascension_combat:' + n for n in HEARTS if n not in CATACLYSM_HEARTS])
    merge_entries(os.path.join(CATA, 'data', 'neoforge', 'loot_modifiers', 'global_loot_modifiers.json'),
                  ['ascension_compat_cataclysm:' + n for n in CATACLYSM_HEARTS])

    # Names: add what is missing, never touch what is there.
    lang_path = os.path.join(COMBAT, 'assets', 'ascension_combat', 'lang', 'en_us.json')
    with open(lang_path, encoding='utf-8') as f:
        lang = json.load(f)
    for k, v in lang_entries().items():
        lang.setdefault(k, v)
    write_json(lang_path, lang)

    print('wrote %d components, %d hearts, %d legendary recipes' % (len(components), len(HEARTS), len(LEGENDARIES)))


if __name__ == '__main__':
    main()
