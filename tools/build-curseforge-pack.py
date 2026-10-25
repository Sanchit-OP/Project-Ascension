#!/usr/bin/env python3
"""Builds the CurseForge pack: one zip with manifest.json and an overrides folder.

  python tools/build-curseforge-pack.py              writes build/pack/<name>-<version>.zip and a list of the ids
  python tools/build-curseforge-pack.py --check      builds nothing, prints the same report (for a quick look)

What goes in (docs/technical/pack-extras.md says why each mod is there):

  manifest.json   every mod by CurseForge project id and file id, never as a file: the launcher's own record of the
                  Ascension Dev instance (minecraftinstance.json) for the mods it tracks (with pack/curseforge-pins.json laid
                  over it where CurseForge no longer has the file), and pack/curseforge-extras.json for the ones placed there
                  by hand. Shader packs are left out (pack/pack.json says why).
  overrides/mods  our own jars (ascension_*.jar, built by ./gradlew build), the only jars the pack carries itself.
  overrides/config  everything under pack/config: the settings the pack decides, the first-launch options, the
                  quest book, the keys.

It refuses to go on when a mod is in the instance and in neither list (it would be missing from the pack), when one mod is
named twice, or when a jar of a mod whose third-party distribution is off would be bundled. Standard library only; the
zip is written in a fixed order with fixed dates, so the same inputs make the same bytes.
"""

import argparse
import hashlib
import json
import os
import re
import sys
import zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
PACK = os.path.join(ROOT, 'pack')
OUT = os.path.join(ROOT, 'build', 'pack')
FIXED_DATE = (2026, 10, 21, 0, 0, 0)


def gradle_property(name):
    with open(os.path.join(ROOT, 'gradle.properties'), encoding='utf-8') as handle:
        for line in handle:
            match = re.match(r'\s*%s\s*=\s*(.+?)\s*$' % re.escape(name), line)
            if match:
                return match.group(1)
    sys.exit('%s not found in gradle.properties' % name)


# Our own mods: 17 until 2026-10-22, when ascension-compat-curios became the 18th.
EXPECTED_OUR_JARS = 19


def instance_dir():
    return os.path.dirname(gradle_property('dev_mods_dir').replace('/', os.sep))


def load_json(path):
    with open(path, encoding='utf-8') as handle:
        return json.load(handle)


def tracked_mods(instance, settings, problems):
    """The mods the launcher tracks: (projectID, fileID, name, jar on disk, third-party distribution allowed)."""
    record = load_json(os.path.join(instance, 'minecraftinstance.json'))
    skipped_shaders = []
    mods = []
    for addon in record['installedAddons']:
        if not addon.get('isEnabled', True):
            continue
        section = (addon.get('categorySection') or {}).get('path')
        if section == 'shaderpacks' and 'shaderpacks' in settings.get('leave_out_of_the_manifest', {}):
            skipped_shaders.append(addon['name'])
            continue
        installed = addon.get('installedFile') or {}
        if not installed.get('id'):
            problems.append('%s has no installed file id in the launcher record' % addon['name'])
            continue
        mods.append({'projectID': addon['addonID'], 'fileID': installed['id'], 'name': addon['name'],
                     'jar': addon.get('fileNameOnDisk') or installed.get('fileName'),
                     'allowDistribution': addon.get('allowModDistribution')})
    return mods, skipped_shaders


def apply_pins(mods, problems, notes):
    """pack/curseforge-pins.json: a file the launcher's record names that CurseForge no longer has, swapped for one it does."""
    path = os.path.join(PACK, 'curseforge-pins.json')
    if not os.path.exists(path):
        return
    for pin in load_json(path)['pins']:
        match = [m for m in mods if m['projectID'] == pin['projectID']]
        if not match:
            problems.append('pin for %s: the launcher record has no such mod' % pin['name'])
            continue
        match[0]['fileID'] = pin['fileID']
        match[0]['jar'] = pin['file']
        notes.append('%s: pinned to %s (the record named %s)' % (pin['name'], pin['file'], pin['replaces']))


def extra_mods():
    return [{'projectID': m['projectID'], 'fileID': m['fileID'], 'name': m['name'], 'jar': m['file'],
             'allowDistribution': None}
            for m in load_json(os.path.join(PACK, 'curseforge-extras.json'))['mods']]


def our_jars():
    version = gradle_property('mod_version')
    found = {}
    for folder, _, names in os.walk(os.path.join(ROOT, 'modules')):
        if os.sep + 'build' + os.sep + 'libs' not in folder + os.sep:
            continue
        for name in names:
            if re.fullmatch(r'ascension_[a-z_]+-%s\.jar' % re.escape(version), name):
                found[name] = os.path.join(folder, name)
    return found


def config_files():
    files = {}
    base = os.path.join(PACK, 'config')
    for folder, _, names in os.walk(base):
        for name in names:
            path = os.path.join(folder, name)
            files['overrides/config/' + os.path.relpath(path, base).replace(os.sep, '/')] = path
    return files


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--check', action='store_true', help='report only; write nothing')
    args = parser.parse_args()

    settings = load_json(os.path.join(PACK, 'pack.json'))
    instance = instance_dir()
    problems, notes = [], []

    tracked, shaders = tracked_mods(instance, settings, problems)
    # Mods the launcher's record still tracks but the pack must not carry (pack.json "leave_out_mods": name -> why).
    for name, why in settings.get('leave_out_mods', {}).items():
        if any(m['name'] == name for m in tracked):
            tracked = [m for m in tracked if m['name'] != name]
            notes.append('%s is left out of the manifest: %s' % (name, why))
    apply_pins(tracked, problems, notes)
    extras = extra_mods()
    ours = our_jars()
    configs = config_files()

    # One mod, one entry.
    seen = {}
    for mod in tracked + extras:
        if mod['projectID'] in seen:
            problems.append('%s is named twice (%s and %s)' % (mod['name'], seen[mod['projectID']], mod['name']))
        seen[mod['projectID']] = mod['name']

    # Every jar in the instance is accounted for, and every tracked jar is really there.
    mods_dir = os.path.join(instance, 'mods')
    on_disk = {n for n in os.listdir(mods_dir) if n.endswith('.jar')}
    accounted = {m['jar'] for m in tracked + extras} | set(ours)
    for name in sorted(on_disk - accounted):
        problems.append('%s is in the instance but in no list: it would be missing from the pack' % name)
    for mod in tracked + extras:
        if mod['jar'] not in on_disk:
            notes.append('%s: the instance has no %s (the manifest still names its file id)' % (mod['name'], mod['jar']))

    # Never bundle a jar whose third-party distribution is off (ours are ours).
    for mod in tracked:
        if mod['allowDistribution'] is False:
            notes.append('%s has third-party distribution off: listed by id, never bundled (our server needs it by hand)' % mod['name'])

    if len(ours) != EXPECTED_OUR_JARS:
        problems.append('expected %d built ascension jars, found %d (run ./gradlew build)' % (EXPECTED_OUR_JARS, len(ours)))

    files = sorted(tracked + extras, key=lambda m: (m['projectID'], m['fileID']))
    manifest = {
        'minecraft': {'version': settings['minecraft'],
                      'modLoaders': [{'id': 'neoforge-' + settings['neoforge'], 'primary': True}]},
        'manifestType': 'minecraftModpack',
        'manifestVersion': 1,
        'name': settings['name'],
        'version': settings['version'],
        'author': settings['author'],
        'files': [{'projectID': m['projectID'], 'fileID': m['fileID'], 'required': True} for m in files],
        'overrides': 'overrides',
    }

    print('%s %s for Minecraft %s, NeoForge %s' % (settings['name'], settings['version'], settings['minecraft'], settings['neoforge']))
    print('  mods by id:   %d (%d from the launcher record, %d placed by hand)' % (len(files), len(tracked), len(extras)))
    print('  our own jars: %d' % len(ours))
    print('  config files: %d' % len(configs))
    print('  left out:     %s' % (', '.join(shaders) if shaders else 'nothing'))
    for note in notes:
        print('  note:  ' + note)
    for problem in problems:
        print('  PROBLEM:  ' + problem)

    ids = [{'projectID': m['projectID'], 'fileID': m['fileID'], 'name': m['name']} for m in files]
    if args.check:
        sys.exit(1 if problems else 0)
    if problems:
        sys.exit('not building: fix the problems above')

    os.makedirs(OUT, exist_ok=True)
    stem = '%s-%s' % (settings['name'].replace(' ', ''), settings['version'])
    target = os.path.join(OUT, stem + '.zip')
    entries = {'manifest.json': (json.dumps(manifest, indent=2) + '\n').encode('utf-8')}
    for name, path in ours.items():
        with open(path, 'rb') as handle:
            entries['overrides/mods/' + name] = handle.read()
    for name, path in configs.items():
        with open(path, 'rb') as handle:
            entries[name] = handle.read()
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name in sorted(entries):
            info = zipfile.ZipInfo(name, FIXED_DATE)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            archive.writestr(info, entries[name])
    with open(os.path.join(OUT, stem + '-ids.json'), 'w', encoding='utf-8') as handle:
        json.dump(ids, handle, indent=1)
    digest = hashlib.sha256(open(target, 'rb').read()).hexdigest()
    print('wrote %s (%.1f MB, sha256 %s)' % (os.path.relpath(target, ROOT), os.path.getsize(target) / 1048576, digest[:16]))


if __name__ == '__main__':
    main()
