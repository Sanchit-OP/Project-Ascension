#!/usr/bin/env python3
"""Copies the pack's shipped config (pack/config/**) into the test game, and updates saved keys.

  python tools/sync-pack-config.py                 copy pack/config into run/client/config, run/server/config and the
                                                   CurseForge test instance's config, and pack/shaderpacks (the options
                                                   files that sit beside a shader pack) into the instance's shaderpacks
  python tools/sync-pack-config.py --options PATH  also rewrite the keys in an existing options.txt
                                                   (the game must be closed); a backup is kept

A .toml file in pack/config that already exists in the target is merged, not replaced: only the
settings the pack file names are set, so the rest of a mod's config (the player's own choices) stays.
Any other file is copied over.

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
    'key.ascension_combat.ability_legs': 'key.keyboard.unknown',
    'key.ascension_combat.ability_chest': 'key.keyboard.unknown',
    'key.ascension_combat.ability_off_hand': 'key.keyboard.unknown',
    'key.ascension_combat.ability_held': 'key.keyboard.unknown',
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


def toml_settings(lines):
    """Yields (line index, table, key, value) for each single-line setting; comments and arrays over several lines are skipped."""
    table = ''
    for index, line in enumerate(lines):
        header = re.match(r'\s*\[([^\]]+)\]\s*$', line)
        if header:
            table = header.group(1).strip()
            continue
        setting = re.match(r'\s*([A-Za-z0-9_.\-]+)\s*=\s*(.+?)\s*$', line)
        if setting:
            yield index, table, setting.group(1), setting.group(2)


def merge_toml(source, destination):
    """Sets, in an existing toml file, just the settings the pack file names; every other line is kept.

    A pack file that holds only a few lines is how the pack decides a few settings of a mod whose
    config also holds the player's own choices (Distant Horizons). Returns the settings changed.
    """
    with open(source, encoding='utf-8', newline='') as handle:
        wanted = [(table, key, value) for _, table, key, value in toml_settings(handle.read().split('\n'))]
    with open(destination, encoding='utf-8', newline='') as handle:
        lines = handle.read().split('\n')

    present = {(table, key): index for index, table, key, _ in toml_settings(lines)}
    changed = []
    for table, key, value in wanted:
        index = present.get((table, key))
        if index is None:
            print('  absent %s %s (this build of the mod does not have it, or has not written it yet)' % (table, key))
            continue
        old = lines[index]
        ending = '\r' if old.endswith('\r') else ''
        indent = old[:len(old) - len(old.lstrip())]
        new = '%s%s = %s%s' % (indent, key, value, ending)
        if new != old:
            lines[index] = new
            changed.append((table, key))
    if changed:
        with open(destination, 'w', encoding='utf-8', newline='') as handle:
            handle.write('\n'.join(lines))
    return changed


# Folders the pack owns outright: what the pack does not ship is removed from the target, so a page that is
# taken out of the quest book does not linger in a test game. (Anything edited there in the game is
# overwritten too: the repo is the truth, see docs/progression/quests.md.)
MANAGED = [os.path.join('ftbquests', 'quests')]


def prune_managed(target_root):
    removed = 0
    for managed in MANAGED:
        source_root = os.path.join(PACK_CONFIG, managed)
        target = os.path.join(target_root, managed)
        if not os.path.isdir(source_root) or not os.path.isdir(target):
            continue
        wanted = {os.path.relpath(os.path.join(f, n), source_root)
                  for f, _, names in os.walk(source_root) for n in names}
        for folder, _, names in os.walk(target):
            for name in names:
                if os.path.relpath(os.path.join(folder, name), target) not in wanted:
                    os.remove(os.path.join(folder, name))
                    removed += 1
    return removed


def copy_config(target_root):
    copied, merged = 0, 0
    for folder, _, files in os.walk(PACK_CONFIG):
        for name in files:
            source = os.path.join(folder, name)
            destination = os.path.join(target_root, os.path.relpath(source, PACK_CONFIG))
            os.makedirs(os.path.dirname(destination), exist_ok=True)
            if name.endswith('.toml') and os.path.exists(destination):
                changed = merge_toml(source, destination)
                merged += len(changed)
                for table, key in changed:
                    print('  set %s %s in %s' % (table, key, os.path.basename(destination)))
            else:
                shutil.copyfile(source, destination)
                copied += 1
    removed = prune_managed(target_root)
    print('%s: copied %d file(s), changed %d setting(s) in existing toml files%s'
          % (target_root, copied, merged, ', removed %d stale quest file(s)' % removed if removed else ''))


def copy_shaderpack_options(target):
    """pack/shaderpacks/<pack>.zip.txt beside the pack's zip, which the launcher installs itself (we never copy a pack)."""
    base = os.path.join(ROOT, 'pack', 'shaderpacks')
    if not os.path.isdir(base) or not os.path.isdir(target):
        return
    names = [n for n in os.listdir(base) if os.path.isfile(os.path.join(base, n))]
    for name in names:
        shutil.copyfile(os.path.join(base, name), os.path.join(target, name))
    print('%s: copied %d shader option file(s)' % (target, len(names)))


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
    copy_config(os.path.join(ROOT, 'run', 'server', 'config'))
    copy_config(os.path.join(instance_dir(), 'config'))
    copy_shaderpack_options(os.path.join(instance_dir(), 'shaderpacks'))
    for path in args.options:
        rewrite_options(path)


if __name__ == '__main__':
    main()
