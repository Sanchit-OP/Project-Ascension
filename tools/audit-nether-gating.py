#!/usr/bin/env python3
"""What in the pack can only be made with something from the Nether or the End?

  python tools/audit-nether-gating.py                     the audit of every mod in the test instance, printed as a report
  python tools/audit-nether-gating.py --without-patches   the same, ignoring our own Earth recipes (what the mods ask for by themselves)
  python tools/audit-nether-gating.py --item create:blaze_burner   one item's way back to its blocking root, as a chain

The pack's rule (ADR-0018 and Sanchit, 2026-10-25) is that the Nether is a planet you reach by ship, so nothing on the road to the first ship may need
a Blaze rod, Nether quartz, netherrack, glowstone, a Nether wart and the like. This reads the recipes straight out of the jars (vanilla's and every mod's
data folder, with their tags and load conditions) and works out, for every item a recipe makes, whether it can be made from Earth's materials.

How it decides, in plain words
  * An item that no recipe makes is taken to be found or dropped (ores, plants, mob drops, loot), except the ROOTS below, which exist only in the Nether or
    the End. A few items that a recipe also makes (gems, redstone) are named in NATURAL because the recipe is only a reverse of a compression.
  * An item that a recipe makes can be made when ONE recipe has every ingredient makeable (an ingredient is a list of choices: one choice is enough, and a tag
    means any item in it). That repeats until nothing new is found.
  * A recipe with a heat requirement also needs a Blaze Burner under its basin (a superheated one needs a Blaze Cake): those are items too, so a
    burner that only a captured blaze gives blocks everything that is heated.
  * For anything it cannot make it prints the shortest chain back to a root, so the cause is one line.

What it cannot see: a recipe a script adds at runtime, loot-only and trade-only items, a machine's own heat or power rules that are not in a recipe (Petrochem's
tower reads Create's boiler heat), and whether a thing works in play. It is a map for a human to read, not a verdict.

Standard library only. The test instance is found from dev_mods_dir in gradle.properties; vanilla's data comes from Gradle's cache of the 1.21.1 client jar.
"""

import argparse
import collections
import glob
import json
import os
import re
import sys
import zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

# Things that exist only in the Nether (or only in the End, marked below) and that no Earth process makes. Everything these make is downstream.
NETHER = {
    # the blaze and what it leaves
    'minecraft:blaze_rod', 'minecraft:blaze_powder', 'minecraft:magma_cream', 'minecraft:ghast_tear', 'minecraft:wither_skeleton_skull',
    'minecraft:nether_star', 'create:blaze_burner', 'create:lit_blaze_burner',
    # the ground (soul sand and soul soil are not here: a fan's soul fire turns sand and dirt into them, and the fan page of the book gives the first block)
    'minecraft:netherrack', 'minecraft:basalt', 'minecraft:blackstone', 'minecraft:gilded_blackstone',
    'minecraft:glowstone', 'minecraft:glowstone_dust', 'minecraft:quartz', 'minecraft:nether_quartz_ore', 'minecraft:nether_gold_ore', 'minecraft:ancient_debris',
    'minecraft:crimson_stem', 'minecraft:warped_stem', 'minecraft:crimson_nylium', 'minecraft:warped_nylium', 'minecraft:crimson_fungus',
    'minecraft:warped_fungus', 'minecraft:crimson_roots', 'minecraft:warped_roots', 'minecraft:nether_wart', 'minecraft:nether_wart_block',
    'minecraft:warped_wart_block', 'minecraft:shroomlight', 'minecraft:weeping_vines', 'minecraft:twisting_vines',
    'minecraft:netherite_scrap',
    # mobs
    'minecraft:piglin_head', 'minecraft:blaze_spawn_egg',
}
END = {
    'minecraft:chorus_fruit', 'minecraft:chorus_flower', 'minecraft:chorus_plant', 'minecraft:end_stone', 'minecraft:dragon_breath', 'minecraft:elytra',
    'minecraft:shulker_shell', 'minecraft:dragon_egg', 'minecraft:dragon_head', 'minecraft:end_crystal',
}
# Items a recipe makes that are also found in the ground, so "no recipe, so natural" does not apply to them.
NATURAL = {
    'minecraft:magma_block', 'minecraft:obsidian', 'minecraft:crying_obsidian', 'minecraft:diamond', 'minecraft:emerald', 'minecraft:lapis_lazuli', 'minecraft:redstone', 'minecraft:coal', 'minecraft:amethyst_shard',
    'minecraft:clay_ball', 'minecraft:slime_ball', 'minecraft:honeycomb', 'minecraft:honey_bottle', 'minecraft:glow_ink_sac', 'minecraft:copper_ingot',
    'minecraft:iron_ingot', 'minecraft:gold_ingot', 'minecraft:iron_nugget', 'minecraft:gold_nugget', 'minecraft:charcoal', 'minecraft:sugar',
    'minecraft:bone_meal', 'minecraft:wheat', 'minecraft:snowball', 'minecraft:ice', 'minecraft:packed_ice', 'minecraft:blue_ice', 'minecraft:snow_block',
    'minecraft:glass', 'minecraft:sand', 'minecraft:gravel', 'minecraft:clay', 'minecraft:stick', 'minecraft:paper', 'minecraft:string',
    'minecraft:feather', 'minecraft:leather', 'minecraft:rabbit_hide', 'minecraft:scute', 'minecraft:nautilus_shell', 'minecraft:heart_of_the_sea',
    'minecraft:prismarine_shard', 'minecraft:prismarine_crystals', 'minecraft:ender_pearl', 'minecraft:egg', 'minecraft:gunpowder',
}
# Our own Earth recipes: namespaces whose recipes are patches over what the mods ask for.
PATCH_NAMESPACES = {'ascension_compat_create'}

HEAT = {'heated': 'create:blaze_burner', 'superheated': 'create:blaze_cake'}


def instance_mods():
    with open(os.path.join(ROOT, 'gradle.properties'), encoding='utf-8') as handle:
        for line in handle:
            match = re.match(r'\s*dev_mods_dir\s*=\s*(.+?)\s*$', line)
            if match:
                return match.group(1).replace('/', os.sep)
    sys.exit('dev_mods_dir not found in gradle.properties')


def vanilla_jar():
    found = glob.glob(os.path.join(os.path.expanduser('~'), '.gradle', 'caches', 'neoformruntime', 'artifacts', 'minecraft_1.21.1_client.jar'))
    if not found:
        sys.exit('vanilla 1.21.1 client jar not found in the Gradle cache (build the project once)')
    return found[0]


def neoforge_jar():
    """NeoForge's own data folder: the common ("c:") tags that most recipes name."""
    found = glob.glob(os.path.join(os.path.expanduser('~'), '.gradle', 'caches', 'modules-2', 'files-2.1', 'net.neoforged', 'neoforge', '21.1.249', '*',
                                   'neoforge-21.1.249-universal.jar'))
    if not found:
        sys.exit('neoforge-21.1.249-universal.jar not found in the Gradle cache (build the project once)')
    return found[0]


class Data:
    def __init__(self):
        self.tags = {'item': {}, 'fluid': {}}       # kind -> id -> [entries]
        self.raw_recipes = []                       # (namespace, path, dict)
        self.mods = {'minecraft', 'neoforge', 'c'}
        self.unresolved = set()                     # tags that name nothing we could read (their ingredient is taken as makeable)

    def read_jar(self, path):
        try:
            z = zipfile.ZipFile(path)
        except zipfile.BadZipFile:
            return
        names = z.namelist()
        for name in names:
            if name == 'META-INF/neoforge.mods.toml':
                for modid in re.findall(rb'modId\s*=\s*"([^"]+)"', z.read(name)):
                    self.mods.add(modid.decode())
            if not name.startswith('data/') or not name.endswith('.json'):
                continue
            parts = name.split('/')
            if len(parts) < 4:
                continue
            ns, kind = parts[1], parts[2]
            if kind in ('recipe', 'recipes'):
                try:
                    self.raw_recipes.append((ns, '/'.join(parts[3:]), json.loads(z.read(name).decode('utf-8-sig'))))
                except (ValueError, UnicodeDecodeError):
                    pass
            elif kind == 'tags' and len(parts) > 4 and parts[3] in ('item', 'items', 'fluid'):
                key = 'fluid' if parts[3] == 'fluid' else 'item'
                tag_id = '%s:%s' % (ns, '/'.join(parts[4:])[:-5])
                try:
                    doc = json.loads(z.read(name).decode('utf-8-sig'))
                except (ValueError, UnicodeDecodeError):
                    continue
                entry = self.tags[key].setdefault(tag_id, [])
                if doc.get('replace'):
                    entry.clear()
                for value in doc.get('values', []):
                    entry.append(value['id'] if isinstance(value, dict) else value)

    def tag_items(self, kind, tag_id, seen=None):
        seen = seen or set()
        if tag_id in seen:
            return set()
        seen.add(tag_id)
        out = set()
        for value in self.tags[kind].get(tag_id, []):
            if value.startswith('#'):
                out |= self.tag_items(kind, value[1:], seen)
            else:
                out.add(value)
        return out

    def condition(self, cond):
        kind = cond.get('type', '')
        if kind == 'neoforge:mod_loaded':
            return cond.get('modid') in self.mods
        if kind == 'neoforge:not':
            return not self.condition(cond.get('value', {}))
        if kind == 'neoforge:and':
            return all(self.condition(c) for c in cond.get('values', []))
        if kind == 'neoforge:or':
            return any(self.condition(c) for c in cond.get('values', []))
        if kind == 'neoforge:true':
            return True
        if kind == 'neoforge:false':
            return False
        if kind == 'neoforge:tag_empty':
            return not self.tag_items('item', cond.get('tag', ''))
        return True


class Recipe:
    def __init__(self, ns, path, kind, ingredients, results, heat, patch):
        self.ns, self.path, self.kind = ns, path, kind
        self.ingredients = ingredients      # list of (kind, frozenset(candidate ids))
        self.results = results              # list of (kind, id)
        self.heat = heat
        self.patch = patch

    @property
    def name(self):
        return '%s:%s' % (self.ns, self.path[:-5])


def _result_ids(node):
    if isinstance(node, str):
        return [node]
    if isinstance(node, list):
        return [r for n in node for r in _result_ids(n)]
    if isinstance(node, dict):
        return [node.get('id') or node.get('item')] if (node.get('id') or node.get('item')) else []
    return []


def _ingredient(data, node):
    """One ingredient as (kind, set of items it accepts), or None when it is empty or unreadable."""
    if isinstance(node, list):
        union, kind = set(), 'item'
        for sub in node:
            got = _ingredient(data, sub)
            if got:
                kind = got[0]
                union |= got[1]
        return (kind, union) if union else None
    if isinstance(node, str):
        return ('item', {node}) if ':' in node else None
    if not isinstance(node, dict):
        return None
    fluid = 'fluid' in node or 'fluids' in node or ('amount' in node and 'count' not in node and ('tag' in node and node.get('type') == 'neoforge:tag'))
    kind = 'fluid' if fluid else 'item'
    out = set()
    for key in ('item', 'items', 'fluid', 'fluids'):
        value = node.get(key)
        if isinstance(value, str):
            out.add(value[1:] if False else value)
        elif isinstance(value, list):
            out |= {v for v in value if isinstance(v, str)}
    if 'tag' in node:
        members = data.tag_items(kind, node['tag'])
        if not members:
            data.unresolved.add(node['tag'])
            members = {'#unresolved:' + node['tag']}
        out |= members
    for child in node.get('children', []):
        got = _ingredient(data, child)
        if got:
            out |= got[1]
    out = {i[1:] if i.startswith('#') else i for i in out}
    expanded = set()
    for i in out:
        expanded.add(i)
    return (kind, expanded) if expanded else None


def build_recipes(data):
    recipes = []
    for ns, path, doc in data.raw_recipes:
        conds = doc.get('neoforge:conditions', [])
        if conds and not all(data.condition(c) for c in conds):
            continue
        kind = doc.get('type', '')
        ingredients, results = [], []
        heat = (doc.get('heat_requirement') or 'none').lower()
        transitional = None

        def add(node):
            got = _ingredient(data, node)
            if got:
                ingredients.append(got)

        if kind in ('minecraft:crafting_shaped', 'create:mechanical_crafting'):
            for key_node in (doc.get('key') or {}).values():
                add(key_node)
            results = [('item', r) for r in _result_ids(doc.get('result'))]
        elif kind == 'minecraft:crafting_shapeless':
            for node in doc.get('ingredients', []):
                add(node)
            results = [('item', r) for r in _result_ids(doc.get('result'))]
        elif kind == 'minecraft:crafting_transmute':
            add(doc.get('input'))
            add(doc.get('material'))
            results = [('item', r) for r in _result_ids(doc.get('result'))]
        elif kind in ('minecraft:smelting', 'minecraft:blasting', 'minecraft:smoking', 'minecraft:campfire_cooking', 'minecraft:stonecutting'):
            add(doc.get('ingredient'))
            results = [('item', r) for r in _result_ids(doc.get('result'))]
        elif kind == 'minecraft:smithing_transform':
            for key in ('template', 'base', 'addition'):
                add(doc.get(key))
            results = [('item', r) for r in _result_ids(doc.get('result'))]
        elif kind == 'create:sequenced_assembly':
            add(doc.get('ingredient'))
            transitional = (doc.get('transitional_item') or {}).get('id')
            for step in doc.get('sequence', []):
                for node in step.get('ingredients', []):
                    got = _ingredient(data, node)
                    if got and transitional not in got[1]:
                        ingredients.append(got)
            results = [('item', r) for r in _result_ids(doc.get('results'))]
        elif 'ingredients' in doc and 'results' in doc:
            for node in doc['ingredients']:
                add(node)
            for node in doc['results']:
                if isinstance(node, dict):
                    rid = node.get('id') or node.get('item')
                    if rid:
                        results.append(('fluid' if ('amount' in node and 'count' not in node) else 'item', rid))
        else:
            continue
        if not results:
            continue
        if heat in HEAT:
            ingredients.append(('item', {HEAT[heat]}))
        recipes.append(Recipe(ns, path, kind, ingredients, results, heat, ns in PATCH_NAMESPACES))
    return recipes


def solve(recipes):
    """The set of (kind, id) that can be made from Earth, and for every other made item the recipes that make it."""
    made_by = collections.defaultdict(list)
    for r in recipes:
        for res in r.results:
            made_by[res].append(r)
    blocked_roots = {('item', i) for i in NETHER | END}
    feasible = set()

    def natural(key):
        kind, item = key
        if key in blocked_roots:
            return False
        return key not in made_by or item in NATURAL

    for key in list(made_by):
        if natural(key):
            feasible.add(key)

    def has(ing):
        kind, choices = ing
        return any(((kind, c) in feasible) or (((kind, c) not in made_by) and ((kind, c) not in blocked_roots)) for c in choices)

    changed = True
    while changed:
        changed = False
        for r in recipes:
            if all(has(i) for i in r.ingredients):
                for res in r.results:
                    if res not in feasible:
                        feasible.add(res)
                        changed = True
    return made_by, feasible, blocked_roots


def _makeable(kind_choice, made_by, feasible, roots):
    return kind_choice in feasible or (kind_choice not in made_by and kind_choice not in roots)


def distances(made_by, feasible, roots):
    """For every item that cannot be made: how many steps back to a root, and the next item on the way (Bellman-Ford over the recipes)."""
    infinity = 10 ** 6
    dist = {key: 0 for key in roots}
    via = {}
    blocked = [key for key in made_by if key not in feasible]
    changed = True
    while changed:
        changed = False
        for key in blocked:
            if key in roots and dist.get(key) == 0:
                continue
            best = dist.get(key, infinity)
            for r in made_by[key]:
                for kind, choices in r.ingredients:
                    if any(_makeable((kind, c), made_by, feasible, roots) for c in choices):
                        continue
                    for c in choices:
                        d = dist.get((kind, c), infinity) + 1
                        if d < best:
                            best, via[key] = d, (kind, c)
                            dist[key] = d
                            changed = True
    return dist, via


def chain(key, made_by, feasible, roots, via=None):
    """The shortest reason an item cannot be made: a list of item ids ending at a root (or at an item nothing makes)."""
    if via is None:
        _, via = distances(made_by, feasible, roots)
    out, seen = [], set()
    while key not in seen:
        seen.add(key)
        out.append(key[1])
        if key in roots:
            out[-1] += '  [only in the Nether or the End]'
            return out
        if key not in via:
            out.append('(no source found: nothing makes it, and it is not a known natural item)')
            return out
        key = via[key]
    out.append('(loop)')
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--without-patches', action='store_true', help='ignore our own Earth recipes')
    parser.add_argument('--item', help='explain one item (namespace:path)')
    parser.add_argument('--namespaces', default='create,aeronautics,aeroworks,createpropulsion,createaddition,petrochem,sable,ascension_atmosphere,ascension_compat_create',
                        help='comma-separated namespaces to report')
    args = parser.parse_args()

    data = Data()
    data.read_jar(vanilla_jar())
    data.read_jar(neoforge_jar())
    mods = instance_mods()
    for jar in sorted(glob.glob(os.path.join(mods, '*.jar'))):
        data.read_jar(jar)
    recipes = build_recipes(data)
    if args.without_patches:
        recipes = [r for r in recipes if not r.patch]
    made_by, feasible, roots = solve(recipes)

    if args.item:
        key = ('item', args.item)
        if key in feasible:
            print('%s can be made from Earth.' % args.item)
            for r in made_by.get(key, []):
                if all(any(((k, c) in feasible) or (((k, c) not in made_by) and ((k, c) not in roots)) for c in ch) for k, ch in r.ingredients):
                    print('  by %s (%s)%s' % (r.name, r.kind, '  [our Earth recipe]' if r.patch else ''))
        else:
            print(' <- '.join(chain(key, made_by, feasible, roots)))
        return

    wanted = [n.strip() for n in args.namespaces.split(',') if n.strip()]
    print('%d recipes read from %d mods; %s our Earth recipes\n' % (len(recipes), len(data.mods), 'ignoring' if args.without_patches else 'counting'))
    heated = collections.Counter()
    for r in recipes:
        if r.heat in HEAT and r.ns in wanted:
            heated[r.heat] += 1
    _, via = distances(made_by, feasible, roots)
    blocked = [(key, made_by[key]) for key in sorted(made_by) if key not in feasible and key[0] == 'item' and key[1].split(':')[0] in wanted]
    reasons = {key: chain(key, made_by, feasible, roots, via) for key, rs in blocked}
    real = [key for key, rs in blocked if 'no source found' not in reasons[key][-1]]
    loops = [key for key, rs in blocked if 'no source found' in reasons[key][-1]]
    print('Items of %s that cannot be made from Earth: %d (and %d more that only loop among natural stones, listed after)' % (', '.join(wanted), len(real), len(loops)))
    for key in real:
        print('  %-48s %s' % (key[1], ' <- '.join(reasons[key][1:])))
    print('\nLoops among natural stones (not a Nether question): %s' % ', '.join(k[1].split(':')[1] for k in loops))
    print('\nHeated recipes of those mods (they need a Blaze Burner under the basin): %d, superheated: %d' % (heated['heated'], heated['superheated']))
    for r in recipes:
        if r.heat == 'heated' and r.ns in wanted:
            print('  %-60s -> %s' % (r.name, ', '.join(res[1] for res in r.results)))


if __name__ == '__main__':
    main()
