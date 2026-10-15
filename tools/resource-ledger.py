#!/usr/bin/env python3
"""The resource ledger: every item our modules register, where a player gets it, and what uses it.

    python tools/resource-ledger.py           rewrite docs/progression/resource-ledger.md
    python tools/resource-ledger.py --check   change nothing; exit 1 if anything below is wrong

Everything is read from the modules' own data, so the ledger cannot drift from the game: recipes,
loot tables, loot modifiers, ore features and the biomes they are placed in, planet terrain,
structure templates (followed from each structure's start pool through its jigsaw links) and gate
ruin sites. What data cannot say lives in tools/resource-ledger-rules.json: which items are
equipment, which are used by code, the names of places, and the known gaps with their reasons.

--check runs with the Gradle build. It fails when:
  - one of our items has no source, and the rules do not excuse it (a known gap, or creative
    only by design, like the Archive's unbreakable plinths);
  - a material has no use, and the rules do not excuse it. A material is an item that is not a
    block, not equipment and not used by code;
  - the rules excuse an item that no longer needs it, so the list of gaps can only shrink;
  - a loot modifier is missing from global_loot_modifiers.json, so it would never run;
  - a structure has no name in the rules;
  - the ledger on disk is not what this script writes.

Why it exists: docs/progression/resource-economy.md, principle 6. Titanium was mined and looted for
a month with nothing that used it; this would have failed the day it was added. Standard library
only, so the build needs nothing but Python.
"""
import collections
import glob
import gzip
import io
import json
import os
import struct
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RULES = os.path.join(ROOT, 'tools', 'resource-ledger-rules.json')
OUT = os.path.join(ROOT, 'docs', 'progression', 'resource-ledger.md')


# --- reading the modules --------------------------------------------------------------------

def modules():
    """(module name, resources dir), e.g. ('worlds', .../modules/worlds/src/main/resources)."""
    for pattern in ('modules/*/src/main/resources', 'modules/compat/*/src/main/resources'):
        for res in sorted(glob.glob(os.path.join(ROOT, pattern))):
            name = os.path.relpath(res, os.path.join(ROOT, 'modules')).replace(os.sep, '/')
            yield name[:-len('/src/main/resources')], res


MODULES = list(modules())


def data(kind, ext='.json'):
    """(module, resource id, file) for every data/<namespace>/<kind>/**<ext>."""
    for module, res in MODULES:
        base = os.path.join(res, 'data')
        for f in sorted(glob.glob(os.path.join(base, '*', *kind.split('/'), '**', '*' + ext), recursive=True)):
            rel = os.path.relpath(f, base).replace(os.sep, '/')
            ns, rest = rel.split('/', 1)
            yield module, f'{ns}:{rest[len(kind) + 1:-len(ext)]}', f


def load(f):
    with open(f, encoding='utf-8') as fh:
        return json.load(fh)


def nbt(path):
    """A gzipped NBT file as nested dicts and lists."""
    buf = io.BytesIO(gzip.open(path).read())

    def rd(fmt):
        return struct.unpack('>' + fmt, buf.read(struct.calcsize('>' + fmt)))[0]

    def string():
        return buf.read(rd('H')).decode('utf-8')

    def payload(t):
        if t in (1, 2, 3, 4, 5, 6):
            return rd('bhiqfd'[t - 1])
        if t == 7:
            return buf.read(rd('i'))
        if t == 8:
            return string()
        if t == 9:
            et, n = rd('b'), rd('i')
            return [payload(et) for _ in range(n)]
        if t == 10:
            out = {}
            while (tt := rd('b')) != 0:
                key = string()
                out[key] = payload(tt)
            return out
        if t in (11, 12):
            n = rd('i')
            size = 4 if t == 11 else 8
            return list(struct.unpack('>%d%s' % (n, 'i' if t == 11 else 'q'), buf.read(size * n)))
        raise ValueError(f'NBT tag type {t} in {path}')

    rd('b')
    string()
    return payload(10)


def walk(node):
    """Every dict inside a JSON value."""
    if isinstance(node, dict):
        yield node
        for v in node.values():
            yield from walk(v)
    elif isinstance(node, list):
        for v in node:
            yield from walk(v)


def add(lst, x):
    """Append unless already there: a block built into a structure is found once, not per piece."""
    if x not in lst:
        lst.append(x)


def pretty(rid):
    """A readable name for an id we have no translation for: minecraft:echo_shard -> Echo Shard."""
    return rid.split(':')[-1].split('/')[-1].replace('_', ' ').title()


# --- what exists ----------------------------------------------------------------------------

LANG, ITEMS = {}, {}             # translation key -> text; item id -> {name, module, block}
for module, res in MODULES:
    for f in glob.glob(os.path.join(res, 'assets', '*', 'lang', 'en_us.json')):
        ns = os.path.basename(os.path.dirname(os.path.dirname(f)))
        lang = load(f)
        LANG.update(lang)
        for key, text in lang.items():
            kind, _, rest = key.partition('.')
            if kind not in ('item', 'block') or not rest.startswith(ns + '.') or rest.count('.') != 1:
                continue
            path = rest.split('.', 1)[1]
            if kind == 'block' and not os.path.exists(os.path.join(res, 'assets', ns, 'models', 'item', path + '.json')):
                continue                                          # a block with no item, e.g. a gate's field
            ITEMS[f'{ns}:{path}'] = {'name': text, 'module': module, 'block': kind == 'block'}


def name(rid):
    if rid.startswith('#'):
        return 'anything in `' + rid + '`'
    ns, _, path = rid.partition(':')
    for kind in ('item', 'block', 'entity'):
        if f'{kind}.{ns}.{path}' in LANG:
            return LANG[f'{kind}.{ns}.{path}']
    return pretty(rid)


ITEM_TAGS, BLOCK_TAGS = collections.defaultdict(set), collections.defaultdict(set)
for registry, tags in (('item', ITEM_TAGS), ('block', BLOCK_TAGS)):
    for _, tag, f in data(f'tags/{registry}'):
        for v in load(f).get('values', []):
            tags[tag].add(v['id'] if isinstance(v, dict) else v)


def members(tag, tags=ITEM_TAGS, seen=None):
    seen = seen or set()
    out = set()
    for v in tags.get(tag, ()):
        if v.startswith('#'):
            if v[1:] not in seen:
                seen.add(v[1:])
                out |= members(v[1:], tags, seen)
        else:
            out.add(v)
    return out


RULES_DATA = load(RULES)
ERRORS = []


def matches(rule_map, item):
    """The reason a rule map gives for this item, matching `namespace:*` too."""
    if item in rule_map:
        return rule_map[item]
    return rule_map.get(item.split(':')[0] + ':*')


def fixed(rule, item):
    """Whether a gap list names this item, so the entry must go once the gap is closed. Only a
    named entry counts: `namespace:*` excuses a whole module by design and is never stale."""
    return item in RULES_DATA[rule]


# --- places -------------------------------------------------------------------------------

PLANET_OF_DIM = {}               # dimension id -> planet name
for _, pid, f in data('ascension_worlds/planet'):
    ns, path = pid.split(':')
    PLANET_OF_DIM[load(f)['surface']] = LANG.get(f'planet.{ns}.{path}', pretty(pid))
BIOME_TAG_DIM = {'#minecraft:is_overworld': 'minecraft:overworld', '#minecraft:is_nether': 'minecraft:the_nether'}

DIM_OF_BIOME = {}
NOISE_OF_DIM = {}
for _, dim, f in data('dimension'):
    gen = load(f).get('generator', {})
    if isinstance(gen.get('settings'), str):
        NOISE_OF_DIM[dim] = gen['settings']
    for d in walk(gen.get('biome_source', {})):
        if isinstance(d.get('biome'), str):                       # multi_noise
            DIM_OF_BIOME[d['biome']] = dim
        for b in d.get('biomes', []) if isinstance(d.get('biomes'), list) else []:
            if isinstance(b, str):                                # checkerboard, as the Moon's
                DIM_OF_BIOME[b] = dim


def planet_of_biomes(biomes, what):
    """The planets these biomes are on. A biome the ledger cannot place is an error, so a new
    world's ore never reaches the ledger as "somewhere"."""
    dims = set()
    for b in biomes:
        if b in BIOME_TAG_DIM:
            dims.add(BIOME_TAG_DIM[b])
        elif b in DIM_OF_BIOME:
            dims.add(DIM_OF_BIOME[b])
        else:
            ERRORS.append(f'{what} is placed in biome {b}, which no dimension the ledger reads lists')
    return sorted(PLANET_OF_DIM.get(d, pretty(d)) for d in dims)


FOUND = collections.defaultdict(list)     # block id -> [where it generates]

# Terrain: a planet's default block and everything its surface rules place.
for _, noise, f in data('worldgen/noise_settings'):
    dims = [d for d, n in NOISE_OF_DIM.items() if n == noise]
    if not dims:
        continue
    planet = PLANET_OF_DIM.get(dims[0], pretty(dims[0]))
    settings = load(f)
    blocks = {settings.get('default_block', {}).get('Name')}
    blocks |= {d['result_state']['Name'] for d in walk(settings.get('surface_rule', {})) if 'result_state' in d}
    for b in sorted(x for x in blocks if x):
        FOUND[b].append(f"{planet}'s terrain")

# Ores and other features, at the heights and in the biomes they are placed.
FEATURE_BLOCKS = {}
for _, fid, f in data('worldgen/configured_feature'):
    FEATURE_BLOCKS[fid] = sorted({d['Name'] for d in walk(load(f)) if isinstance(d.get('Name'), str)})
PLACED = {}
for _, pid, f in data('worldgen/placed_feature'):
    placed = load(f)
    height = ''
    for p in placed.get('placement', []):
        if p.get('type') == 'minecraft:height_range':
            h = p['height']
            ends = []
            for k in ('min_inclusive', 'max_inclusive'):
                (anchor, value), = h[k].items()
                ends.append(str(value) if anchor == 'absolute' else f'{anchor.replace("_", " ")} + {value}')
            height = f'y {ends[0]} to {ends[1]}'.replace('-', '−')
    PLACED[pid] = (placed['feature'], height)
FEATURE_BIOMES = collections.defaultdict(set)
for _, bid, f in data('worldgen/biome'):
    for step in load(f).get('features', []):
        for pid in step:
            FEATURE_BIOMES[pid].add(bid)
for _, _, f in data('neoforge/biome_modifier'):
    mod = load(f)
    if mod.get('type') != 'neoforge:add_features':
        continue
    biomes = mod['biomes'] if isinstance(mod['biomes'], list) else [mod['biomes']]
    for pid in (mod['features'] if isinstance(mod['features'], list) else [mod['features']]):
        FEATURE_BIOMES[pid] |= set(biomes)
TOOL_NEED = {}
for tier in ('stone', 'iron', 'diamond'):
    for b in members(f'minecraft:needs_{tier}_tool', BLOCK_TAGS):
        TOOL_NEED[b] = f'{"an" if tier[0] in "aeiou" else "a"} {tier} pickaxe or better'
for pid, (fid, height) in PLACED.items():
    for b in FEATURE_BLOCKS.get(fid, []):
        if b.startswith('minecraft:'):
            continue
        for planet in planet_of_biomes(FEATURE_BIOMES.get(pid, ()), f'feature {pid}'):
            detail = ', '.join(x for x in (height, f'needs {TOOL_NEED[b]}' if b in TOOL_NEED else '') if x)
            FOUND[b].append(f"{planet}'s rock" + (f', {detail}' if detail else ''))

# Structures: from each structure's start pool, through every template's jigsaw links.
TEMPLATE_FILES = {tid: f for _, tid, f in data('structure', '.nbt')}
POOLS = {pid: load(f) for _, pid, f in data('worldgen/template_pool')}
STRUCTURE_NAMES = RULES_DATA['structures']
TABLE_PLACES = collections.defaultdict(set)   # chest loot table -> structure names
for _, sid, f in data('worldgen/structure'):
    starts = [d['start_pool'] for d in walk(load(f)) if isinstance(d.get('start_pool'), str)]
    if not starts:
        continue
    if sid not in STRUCTURE_NAMES:
        ERRORS.append(f'structure {sid} has no name in {os.path.relpath(RULES, ROOT)} ("structures")')
        continue
    label = STRUCTURE_NAMES[sid]
    pools, seen_pools, templates = list(starts), set(), set()
    while pools:
        pid = pools.pop()
        if pid in seen_pools or pid not in POOLS:
            continue
        seen_pools.add(pid)
        for d in walk(POOLS[pid]):
            loc = d.get('location')
            if isinstance(loc, str) and loc in TEMPLATE_FILES and loc not in templates:
                templates.add(loc)
                t = nbt(TEMPLATE_FILES[loc])
                for b in t.get('blocks', []):
                    tag = b.get('nbt', {})
                    if isinstance(tag.get('pool'), str):
                        pools.append(tag['pool'])
                    if isinstance(tag.get('LootTable'), str):
                        TABLE_PLACES[tag['LootTable']].add(label)
                palette = t.get('palette') or (t.get('palettes') or [[]])[0]
                for state in palette:
                    if state['Name'] not in ('minecraft:air', 'minecraft:structure_void', 'minecraft:jigsaw'):
                        add(FOUND[state['Name']], f'{label}, built into it')

# Gate ruin sites: the blocks around a pre-generated gate. The frame itself cannot be broken
# (PregeneratedGates), so only the site's own blocks count.
for _, site, f in data('ascension_worlds/gateway_site'):
    label = STRUCTURE_NAMES.get(site)
    if label is None:
        ERRORS.append(f'gate site {site} has no name in {os.path.relpath(RULES, ROOT)} ("structures")')
        continue
    for v in load(f).get('key', {}).values():
        if 'block' in v:
            add(FOUND[v['block']], f'{label}, built into it')


# --- recipes --------------------------------------------------------------------------------

RECIPE_VERB = {
    'minecraft:crafting_shaped': 'Crafting', 'minecraft:crafting_shapeless': 'Crafting',
    'minecraft:smelting': 'Furnace', 'minecraft:blasting': 'Blast furnace',
    'minecraft:smoking': 'Smoker', 'minecraft:campfire_cooking': 'Campfire',
    'minecraft:stonecutting': 'Stonecutter', 'minecraft:smithing_transform': 'Smithing',
}


def slots(recipe):
    """Ingredient slots, each a list of alternative ids (items, or #tags)."""
    if 'key' in recipe:
        raw = list(recipe['key'].values())
    elif 'ingredients' in recipe:
        raw = recipe['ingredients']
    else:
        raw = [recipe[k] for k in ('ingredient', 'template', 'base', 'addition') if k in recipe]
    out = []
    for s in raw:
        alts = []
        for e in (s if isinstance(s, list) else [s]):
            if isinstance(e, dict) and 'item' in e:
                alts.append(e['item'])
            elif isinstance(e, dict) and 'tag' in e:
                alts.append('#' + e['tag'])
        if alts and alts not in out:
            out.append(alts)
    return out


def outputs(recipe):
    res = recipe.get('results', recipe.get('result'))
    res = res if isinstance(res, list) else [res]
    return [r.get('id') or r.get('item') if isinstance(r, dict) else r for r in res if r]


MADE = collections.defaultdict(list)      # item -> [(verb, slots, note)]
USED = collections.defaultdict(list)      # item -> [(verb, output, note)]
for module, rid, f in data('recipe'):
    recipe = load(f)
    conds = recipe.get('neoforge:conditions', [])
    if any(c.get('type') == 'neoforge:false' for c in conds):
        continue                                                  # switched off, e.g. Create's backtanks
    note = ', '.join(f'with {c["modid"]} installed' for c in conds if c.get('type') == 'neoforge:mod_loaded')
    verb = RECIPE_VERB.get(recipe.get('type'), pretty(recipe.get('type', '?')).title())
    ss = slots(recipe)
    for out in outputs(recipe):
        MADE[out].append((verb, ss, note))
        for alts in ss:
            for a in alts:
                for item in (members(a[1:]) if a.startswith('#') else [a]):
                    add(USED[item], (verb, out, note))


# --- loot -----------------------------------------------------------------------------------

TABLES = {tid: load(f) for _, tid, f in data('loot_table')}
REGISTERED = set()
for _, _, f in data('loot_modifiers'):
    if f.replace(os.sep, '/').endswith('neoforge/loot_modifiers/global_loot_modifiers.json'):
        REGISTERED |= set(load(f).get('entries', []))
MOB_OF_TABLE = {}
for _, mid, f in data('loot_modifiers'):
    mod = load(f)
    if mod.get('type') != 'neoforge:add_table':
        continue
    if mid not in REGISTERED:
        ERRORS.append(f'loot modifier {mid} is not in global_loot_modifiers.json, so it never runs')
    for c in mod.get('conditions', []):
        if c.get('condition') == 'neoforge:loot_table_id':
            MOB_OF_TABLE[mod['table']] = c['loot_table_id']


def count_text(functions):
    for fn in functions:
        if fn.get('function') == 'minecraft:set_count':
            c = fn['count']
            if isinstance(c, dict) and 'min' in c:
                return f'{c["min"]} to {c["max"]}'
            return str(c)
    return '1'


def conditions_text(conds):
    notes = []
    for c in conds:
        kind = c.get('condition', '')
        if kind == 'minecraft:killed_by_player':
            notes.append('killed by a player')
        elif kind == 'minecraft:match_tool' and 'silk_touch' in json.dumps(c):
            notes.append('with Silk Touch')
        elif kind in ('minecraft:random_chance', 'minecraft:random_chance_with_enchanted_bonus'):
            chance = c.get('chance', c.get('unenchanted_chance'))
            notes.append(f'{chance:.0%} chance' if isinstance(chance, (int, float)) else 'by chance')
    return notes


def loot_entries(table, seen=()):
    """(item, how many, notes) for everything a table can give, following nested tables."""
    for pool in table.get('pools', []):
        entries = pool.get('entries', [])
        total = sum(e.get('weight', 1) for e in entries)
        rolls = pool.get('rolls', 1)
        pool_notes = conditions_text(pool.get('conditions', []))
        for e in entries:
            yield from entry_items(e, total, rolls, pool_notes, seen)


def entry_items(e, total, rolls, notes, seen):
    notes = notes + conditions_text(e.get('conditions', []))
    for fn in e.get('functions', []):                     # which tablet: an archive entry set on it
        components = fn.get('components')
        entry = components.get('ascension_worlds:archive_entry') if isinstance(components, dict) else None
        if isinstance(entry, str):
            notes = notes + [f'tablet: {pretty(entry)}']
    kind = e.get('type')
    if kind == 'minecraft:item':
        share = '' if total <= e.get('weight', 1) else f'weight {e.get("weight", 1)} of {total}'
        if isinstance(rolls, dict):
            share += f'{", " if share else ""}{rolls["min"]} to {rolls["max"]} rolls'
        yield e['name'], count_text(e.get('functions', [])), notes + ([share] if share else [])
    elif kind == 'minecraft:loot_table' and isinstance(e.get('value'), str) and e['value'] not in seen:
        yield from loot_entries(TABLES.get(e['value'], {}), seen + (e['value'],))
    for child in e.get('children', []):
        yield from entry_items(child, total, rolls, notes, seen)


FROM = collections.defaultdict(list)       # item -> [source lines]
for tid, table in sorted(TABLES.items()):
    ns, path = tid.split(':', 1)
    for item, count, notes in loot_entries(table):
        how = ', '.join([count] + notes)
        lines = []
        if path.startswith('blocks/'):
            block = f'{ns}:{path[len("blocks/"):]}'
            mined = 'Mined' if block == item else f'Mined from {name(block)}'
            lines = [f'{mined}: {where}' + (f'; {", ".join(notes)}' if notes else '') for where in FOUND.get(block, [])]
        elif tid in MOB_OF_TABLE or path.startswith('entities/'):
            mob = MOB_OF_TABLE.get(tid, tid).replace('entities/', '')
            lines = [f'Dropped by the {name(mob)}: {how}']
        elif TABLE_PLACES.get(tid):
            lines = [f'Chests in {place}: {how}' for place in sorted(TABLE_PLACES[tid])]
        else:
            ERRORS.append(f'loot table {tid} gives {item} but nothing we ship uses the table')
        for line in lines:
            add(FROM[item], line)


# --- the ledger -----------------------------------------------------------------------------

FRAMES_OF_BLOCK = collections.defaultdict(list)   # block -> gateway frames it can be built into
for _, frame, f in data('ascension_worlds/gateway_frame'):
    for v in load(f).get('key', {}).values():
        for b in (members(v['tag'], BLOCK_TAGS) if 'tag' in v else {v.get('block')}):
            if b:
                add(FRAMES_OF_BLOCK[b], frame)


def made_lines(item):
    by_verb = collections.defaultdict(list)
    for verb, ss, note in MADE.get(item, []):
        text = ' + '.join(' or '.join(name(a) for a in alts) for alts in ss) or 'nothing'
        add(by_verb[verb], text + (f' ({note})' if note else ''))
    return [f'{verb}: {"; or ".join(texts)}' for verb, texts in by_verb.items()]


def used_lines(item):
    by_verb = collections.defaultdict(list)
    for verb, out, note in USED.get(item, []):
        add(by_verb[verb], name(out) + (f' ({note})' if note else ''))
    lines = [f'{verb} into {", ".join(texts)}' for verb, texts in by_verb.items()]
    for tag, why in RULES_DATA['tag_uses'].items():
        if item in members(tag):
            lines.append(f'{why} (`#{tag}`)')
    for frame in FRAMES_OF_BLOCK.get(item, []):
        lines.append(f'Builds a gateway frame ({name(frame)})')
    why = matches(RULES_DATA['used_by_code'], item)
    if why:
        lines.append(why)
    return lines


GAPS, CREATIVE = [], []                     # (item, what is missing, why)
for item, info in sorted(ITEMS.items()):
    sources = made_lines(item) + FROM.get(item, [])
    gap = matches(RULES_DATA['no_source_yet'], item)
    by_design = matches(RULES_DATA['creative_only'], item)
    if not sources and not gap and not by_design:
        ERRORS.append(f'{item} has no source: no recipe, loot or worldgen gives it. Add one, or say why in the rules')
    elif sources and (fixed('no_source_yet', item) or fixed('creative_only', item)):
        ERRORS.append(f'{item} has a source now: remove it from the gap lists in the rules')
    elif not sources:
        (CREATIVE if by_design else GAPS).append((item, 'no source', by_design or gap))
    info['from'] = sources

    equipment = matches(RULES_DATA['equipment'], item)
    info['material'] = not info['block'] and not equipment and not matches(RULES_DATA['used_by_code'], item)
    uses = used_lines(item)
    info['uses'] = uses
    if info['material']:
        excused = matches(RULES_DATA['no_use_yet'], item)
        if not uses and not excused:
            ERRORS.append(f'{item} is a material with no use: nothing crafts with it. Add a use, or say why in the rules')
        elif uses and fixed('no_use_yet', item):
            ERRORS.append(f'{item} has a use now: remove it from "no_use_yet" in the rules')
        elif excused:
            GAPS.append((item, 'no use', excused))
for rule in ('no_source_yet', 'creative_only', 'no_use_yet', 'used_by_code', 'equipment'):
    for item in RULES_DATA[rule]:
        if not item.endswith(':*') and item not in ITEMS:
            ERRORS.append(f'the rules name {item} in "{rule}", but no module registers it')


def render():
    out = [
        '# Resource ledger',
        '',
        '> **Generated** by `tools/resource-ledger.py` from every module\'s data. Do not edit it by',
        '> hand: change the data, or `tools/resource-ledger-rules.json` for what the data cannot say,',
        '> then run the script. The build runs it with `--check`, which fails if this page is stale,',
        '> if one of our items has no source, or if a material has no use',
        '> (`resource-economy.md`, principle 6).',
        '',
        '## Known gaps',
        '',
        'Excused by the rules, with the reason. This list should only get shorter.',
        '',
    ]
    grouped = collections.defaultdict(list)                 # one line per reason, not per item
    for item, missing, why in GAPS:
        grouped[(missing, why)].append(ITEMS[item]['name'])
    for (missing, why), names in grouped.items():
        out.append(f'- **{missing.capitalize()}** ({len(names)}): {", ".join(names)}. {why}')
    out += ['', '## Creative only, by design', '',
            'No survival source, on purpose. The rules say why.', '']
    for item, _, why in CREATIVE:
        out.append(f'- **{ITEMS[item]["name"]}** (`{item}`): {why}')
    for module in [m for m, _ in MODULES]:
        mine = [(i, info) for i, info in sorted(ITEMS.items()) if info['module'] == module]
        if not mine:
            continue
        out += ['', f'## ascension-{module.replace("compat/", "compat-")}']
        for title, keep in (('Materials', lambda i: i['material']),
                            ('Items', lambda i: not i['material'] and not i['block']),
                            ('Blocks', lambda i: i['block'])):
            group = [(i, info) for i, info in mine if keep(info)]
            if not group:
                continue
            out += ['', f'### {title}']
            for item, info in group:
                out += ['', f'**{info["name"]}** · `{item}`']
                out += [f'- From: {line}' for line in info['from']] or ['- From: *nothing yet*']
                if info['uses'] or info['material']:
                    out += [f'- Used: {line}' for line in info['uses']] or ['- Used: *nothing yet*']
    return '\n'.join(out) + '\n'


def main():
    check = '--check' in sys.argv[1:]
    text = render()
    current = open(OUT, encoding='utf-8').read() if os.path.exists(OUT) else ''
    stale = current.replace('\r\n', '\n') != text
    if not check and stale:
        with open(OUT, 'w', encoding='utf-8', newline='\n') as fh:
            fh.write(text)
        print(f'wrote {os.path.relpath(OUT, ROOT)}')
    if check and stale:
        ERRORS.append(f'{os.path.relpath(OUT, ROOT)} is stale: run python tools/resource-ledger.py')
    for e in ERRORS:
        print('ledger:', e, file=sys.stderr)
    print(f'{len(ITEMS)} items, {len(GAPS)} known gaps, {len(ERRORS)} problems')
    return 1 if ERRORS else 0


if __name__ == '__main__':
    sys.exit(main())
