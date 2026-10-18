#!/usr/bin/env python3
"""Copies the pack's shipped config (pack/config/**) into the test game, and updates saved keys.

  python tools/sync-pack-config.py                 copy pack/config into run/client/config and the
                                                   CurseForge test instance's config
  python tools/sync-pack-config.py --options PATH  also rewrite the keys in an existing options.txt
                                                   (the game must be closed); a backup is kept

Why two jobs: the Default Options mod applies pack/config/defaultoptions/keybindings.txt to a fresh
install and to any key a player has not yet had saved, but never to a key already in their
options.txt. So a test instance that already has an options.txt needs its saved keys moved too.
A key is only moved if it still holds the default we are replacing (the player never changed it).

Standard library only. The instance is found from dev_mods_dir in gradle.properties.
"""

import argparse
import datetime
import os
import re
import shutil
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
PACK_CONFIG = os.path.join(ROOT, 'pack', 'config')
KEYS_FILE = os.path.join(PACK_CONFIG, 'defaultoptions', 'keybindings.txt')

# What each key held before the pack changed it. A saved key is rewritten only if it still holds this.
OLD_DEFAULTS = {
    'key.ascension_combat.open_character_sheet': 'key.keyboard.v',
    'key.ascension_combat.activate_armor_ability': 'key.keyboard.unknown',
    'key.ftbquests.quests': 'key.keyboard.unknown',
    'key.keybindatlas.open_keyboard': 'key.keyboard.k',
    'key.cataclysm.helmet_ability': 'key.keyboard.c',
    'key.saveToolbarActivator': 'key.keyboard.c',
    'key.loadToolbarActivator': 'key.keyboard.x',
    'key.socialInteractions': 'key.keyboard.p',
    'iris.keybind.reload': 'key.keyboard.r',
    'iris.keybind.toggleShaders': 'key.keyboard.k',
}


def instance_dir():
    with open(os.path.join(ROOT, 'gradle.properties'), encoding='utf-8') as handle:
        for line in handle:
            match = re.match(r'\s*dev_mods_dir\s*=\s*(.+?)\s*$', line)
            if match:
                return os.path.dirname(match.group(1).replace('/', os.sep))
    sys.exit('dev_mods_dir not found in gradle.properties')


def new_keys():
    keys = {}
    with open(KEYS_FILE, encoding='utf-8') as handle:
        for line in handle:
            match = re.match(r'key_([^:]+):([^:\s]+)', line.strip())
            if match:
                keys[match.group(1)] = match.group(2)
    return keys


def copy_config(target_root):
    count = 0
    for folder, _, files in os.walk(PACK_CONFIG):
        for name in files:
            source = os.path.join(folder, name)
            destination = os.path.join(target_root, os.path.relpath(source, PACK_CONFIG))
            os.makedirs(os.path.dirname(destination), exist_ok=True)
            shutil.copyfile(source, destination)
            count += 1
    print('copied %d file(s) into %s' % (count, target_root))


def rewrite_options(path):
    wanted = new_keys()
    with open(path, encoding='utf-8') as handle:
        lines = handle.read().split('\n')
    changed, kept = [], []
    for index, line in enumerate(lines):
        match = re.match(r'key_([^:]+):([^:\s]+)(.*)$', line)
        if not match or match.group(1) not in wanted:
            continue
        name, saved, rest = match.groups()
        if saved == OLD_DEFAULTS.get(name) and saved != wanted[name]:
            lines[index] = 'key_%s:%s%s' % (name, wanted[name], rest)
            changed.append((name, saved, wanted[name]))
        elif saved != wanted[name]:
            kept.append((name, saved))
    if changed:
        backup = '%s.bak-%s' % (path, datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))
        shutil.copyfile(path, backup)
        with open(path, 'w', encoding='utf-8', newline='') as handle:
            handle.write('\n'.join(lines))
        print('backup: %s' % backup)
    for name, old, new in changed:
        print('  moved %-48s %s -> %s' % (name, old.replace('key.keyboard.', ''), new.replace('key.keyboard.', '')))
    for name, saved in kept:
        print('  kept  %-48s %s (the player changed it)' % (name, saved.replace('key.keyboard.', '')))
    print('%s: %d moved, %d kept' % (path, len(changed), len(kept)))


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--options', action='append', default=[], help='an existing options.txt to update')
    args = parser.parse_args()

    copy_config(os.path.join(ROOT, 'run', 'client', 'config'))
    copy_config(os.path.join(instance_dir(), 'config'))
    for path in args.options:
        rewrite_options(path)


if __name__ == '__main__':
    main()
