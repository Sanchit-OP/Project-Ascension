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
]


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

    for file, where, message in findings:
        print('%s %s %s' % (os.path.relpath(file, ROOT), where, message), file=sys.stderr)
    if findings:
        print('check-data: %d problem(s) in %d advancement file(s)' % (len(findings), len(files)), file=sys.stderr)
        return 1
    print('check-data: %d advancement file(s), no problems' % len(files))
    return 0


if __name__ == '__main__':
    sys.exit(main())
