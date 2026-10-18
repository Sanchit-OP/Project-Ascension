"""Writes SNBT the way FTB Quests saves it: tabs, one entry per line, no commas.

The generator builds plain Python values and this turns them into text. Types:

    dict      a compound          { key: value }  (keys in insertion order)
    list      a list              [ ... ]
    str       a string            "..."
    bool      a byte              true / false
    int       an int              5
    Long      a long              5L
    float     a double            1.5d

Why a writer of our own: the files are read by FTB Library's own SNBT reader, not by a generic parser,
and the safest input for a reader is what the mod itself writes. (`QuestChecks` proves, on a real
server, that Quests accepts what comes out of here.)
"""

import math
import re

_BARE_KEY = re.compile(r'^[A-Za-z0-9_.+\-]+$')


class Long(int):
    """An int written with an L suffix."""


def _string(value):
    escaped = value.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n')
    return '"' + escaped + '"'


def _key(key):
    return key if _BARE_KEY.match(key) else _string(key)


def _scalar(value):
    if isinstance(value, bool):
        return 'true' if value else 'false'
    if isinstance(value, Long):
        return f'{int(value)}L'
    if isinstance(value, int):
        return str(value)
    if isinstance(value, float):
        if math.isnan(value) or math.isinf(value):
            raise ValueError(f'cannot write {value} as SNBT')
        text = repr(value)
        return text + 'd'
    if isinstance(value, str):
        return _string(value)
    raise TypeError(f'cannot write {type(value).__name__} as SNBT: {value!r}')


def dumps(value, depth=0, quote_keys=False):
    """The SNBT text for a value, ending in a newline at the top level."""
    text = _write(value, depth, quote_keys)
    return text + '\n' if depth == 0 else text


def _write(value, depth, quote_keys):
    tab = '\t' * depth
    inner = '\t' * (depth + 1)
    if isinstance(value, dict):
        if not value:
            return '{ }'
        lines = ['{']
        for key, item in value.items():
            name = _string(key) if quote_keys else _key(key)
            lines.append(f'{inner}{name}: {_write(item, depth + 1, quote_keys)}')
        lines.append(tab + '}')
        return '\n'.join(lines)
    if isinstance(value, (list, tuple)):
        if not value:
            return '[ ]'
        if all(not isinstance(item, (dict, list, tuple)) for item in value):
            return '[' + ', '.join(_scalar(item) for item in value) + ']'
        lines = ['[']
        for item in value:
            lines.append(f'{inner}{_write(item, depth + 1, quote_keys)}')
        lines.append(tab + ']')
        return '\n'.join(lines)
    return _scalar(value)
