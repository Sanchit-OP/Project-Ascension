#!/usr/bin/env python3
"""Lint for data mistakes Minecraft accepts without a word.

Minecraft's codecs ignore a field they do not know, so a misspelt condition does not fail: it
vanishes, and what was a narrow condition becomes an empty one that is always true. Nothing logs.
Each rule below is a mistake that has really been made here.

  python tools/check-data.py          exits 1 and lists every finding
  python tools/check-data.py --list   prints the rules

It runs with every build (root `checkData` task, a dependency of each module's `check`). Standard
library only.
"""

import glob
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

RULES = [
    ('location predicate: "structure" -> "structures"',
     'A location predicate takes "structures" (plural). With "structure" the condition is empty, always '
     'true, and a discovery advancement is earned by standing anywhere. It gave every new player nine '
     'free skill points (2026-10-18).'),
    ('enchantment attribute effect: id, attribute, amount, operation',
     'A minecraft:attributes effect needs all four, spelt exactly, and an attribute that exists. A '
     'missing or misspelt one is dropped or fails the whole enchantment, and a stat id outside '
     'ascension_combat:enchantment/ shows as "Other" on the sheet (ADR-0022).'),
    ('our enchantment: a name in the lang file, and a place in the loot and table tags',
     'An enchantment with no lang key shows its raw key ("enchantment.ascension_combat.x") on every book and '
     'tooltip. One missing from minecraft:non_treasure is never offered by the table, villagers or loot, and '
     'one missing from ascension_combat:own never turns up in the structure chests. Neither logs anything.'),
]

ATTRIBUTE_KEYS = {'id', 'attribute', 'amount', 'operation'}
OPERATIONS = {'add_value', 'add_multiplied_base', 'add_multiplied_total'}
OUR_ATTRIBUTES = {'armor', 'magic_resist', 'omnivamp', 'lethality',
                  'armor_penetration_percent', 'magic_penetration_percent'}


def walk(node, path=()):
    """Yields (path, dict) for every object in a JSON document."""
    if isinstance(node, dict):
        yield path, node
        for key, value in node.items():
            yield from walk(value, path + (key,))
    elif isinstance(node, list):
        for index, value in enumerate(node):
            yield from walk(value, path + (index,))


def check_advancement(file, document):
    findings = []
    for path, obj in walk(document):
        if path and path[-1] == 'location' and 'structure' in obj:
            findings.append((file, '/'.join(str(p) for p in path),
                             'uses "structure"; a location predicate takes "structures"'))
    return findings


def check_enchantment(file, document):
    findings = []
    effects = document.get('effects', {}) if isinstance(document, dict) else {}
    for index, effect in enumerate(effects.get('minecraft:attributes', [])):
        where = 'effects/minecraft:attributes/%d' % index
        missing = ATTRIBUTE_KEYS - set(effect)
        extra = set(effect) - ATTRIBUTE_KEYS
        if missing or extra:
            findings.append((file, where, 'keys must be exactly %s (missing %s, unknown %s)'
                             % (sorted(ATTRIBUTE_KEYS), sorted(missing), sorted(extra))))
            continue
        if effect['operation'] not in OPERATIONS:
            findings.append((file, where, 'operation "%s" is not one of %s' % (effect['operation'], sorted(OPERATIONS))))
        attribute = effect['attribute']
        if attribute.startswith('ascension_combat:'):
            if attribute.split(':', 1)[1] not in OUR_ATTRIBUTES:
                findings.append((file, where, 'attribute "%s" is not one of ours' % attribute))
            if not effect['id'].startswith('ascension_combat:enchantment/'):
                findings.append((file, where, 'id "%s" must start with ascension_combat:enchantment/' % effect['id']))
    return findings


def check_our_enchantments(module_resources):
    """Every enchantment this module defines under its own namespace has a name and is in both tags."""
    findings = []
    data = os.path.join(module_resources, 'data', 'ascension_combat')
    names = sorted(os.path.splitext(os.path.basename(f))[0]
                   for f in glob.glob(os.path.join(data, 'enchantment', '*.json')))
    if not names:
        return findings

    def values(path):
        full = os.path.join(module_resources, 'data', *path)
        if not os.path.exists(full):
            return set()
        with open(full, encoding='utf-8') as handle:
            return set(json.load(handle).get('values', []))

    lang_path = os.path.join(module_resources, 'assets', 'ascension_combat', 'lang', 'en_us.json')
    with open(lang_path, encoding='utf-8') as handle:
        lang = json.load(handle)
    own = values(('ascension_combat', 'tags', 'enchantment', 'own.json'))
    table = values(('minecraft', 'tags', 'enchantment', 'non_treasure.json'))
    for name in names:
        file = os.path.join(data, 'enchantment', name + '.json')
        key = 'ascension_combat:' + name
        if 'enchantment.ascension_combat.' + name not in lang:
            findings.append((file, '', 'has no "enchantment.ascension_combat.%s" in en_us.json' % name))
        if key not in own:
            findings.append((file, '', 'is not in tags/enchantment/own.json (no structure loot)'))
        if key not in table:
            findings.append((file, '', 'is not in minecraft tags/enchantment/non_treasure.json (not offered)'))
    return findings


def main():
    if '--list' in sys.argv:
        for name, why in RULES:
            print('-', name, '\n   ', why)
        return 0

    findings = []
    pattern = os.path.join(ROOT, 'modules', '**', 'src', 'main', 'resources', 'data', '*', 'advancement', '**', '*.json')
    files = sorted(glob.glob(pattern, recursive=True))
    for file in files:
        with open(file, encoding='utf-8') as handle:
            try:
                document = json.load(handle)
            except json.JSONDecodeError as error:
                findings.append((file, '', 'is not valid JSON: %s' % error))
                continue
        findings.extend(check_advancement(file, document))

    enchantments = sorted(glob.glob(os.path.join(ROOT, 'modules', '**', 'src', 'main', 'resources', 'data', '*',
                                                 'enchantment', '**', '*.json'), recursive=True))
    for file in enchantments:
        with open(file, encoding='utf-8') as handle:
            try:
                findings.extend(check_enchantment(file, json.load(handle)))
            except json.JSONDecodeError as error:
                findings.append((file, '', 'is not valid JSON: %s' % error))
    files = files + enchantments
    for resources in glob.glob(os.path.join(ROOT, 'modules', '*', 'src', 'main', 'resources')):
        findings.extend(check_our_enchantments(resources))

    for file, where, message in findings:
        print('%s %s %s' % (os.path.relpath(file, ROOT), where, message), file=sys.stderr)
    if findings:
        print('check-data: %d problem(s) in %d file(s)' % (len(findings), len(files)), file=sys.stderr)
        return 1
    print('check-data: %d file(s), no problems' % len(files))
    return 0


if __name__ == '__main__':
    sys.exit(main())
