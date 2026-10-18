"""Everything about the book that can be wrong, caught at build time instead of in somebody's game.

A mistake here is silent in the game: a quest naming a misspelt item shows a "missing item" stand-in and
can never be completed; a dependency on a slug that does not exist is dropped; a line of text that gives
the story away is read by every player. This is the same idea as tools/check-data.py.

Checked:
  * slugs are unique, dependencies resolve and do not loop, positions do not stack quests on each other
  * items, advancements, dimensions and structures that belong to us exist (vanilla ones are checked
    by shape here, and for real by QuestChecks on a server)
  * pictures and quest shapes exist in the compat jar
  * no text uses a guarded word (pack/quests/guarded-words.txt)
"""

import glob
import json
import os
import re

from quests import chart
from quests.model import Icon

ROOT = chart.ROOT
COMPAT_ASSETS = os.path.join(ROOT, 'modules', 'compat', 'ftbquests', 'src', 'main', 'resources',
                             'assets', 'ascension_compat_ftbquests')
GUARDED_WORDS = os.path.join(ROOT, 'pack', 'quests', 'guarded-words.txt')

# Created by the game at run time, so there is no file to find.
DYNAMIC_TEXTURES = {'textures/quests/planets/you_are_here.png'}

STOCK_SHAPES = {'circle', 'diamond', 'gear', 'heart', 'hexagon', 'none', 'octagon', 'pentagon', 'rsquare', 'square'}

VANILLA_ADVANCEMENT_ROOTS = ('story/', 'nether/', 'end/', 'adventure/', 'husbandry/')
VANILLA_DIMENSIONS = {'minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end'}

OUR_NAMESPACES = ('ascension_',)

MIN_SPACING = 0.75      # quests closer than this overlap on the page

_ID = re.compile(r'^[a-z0-9_.-]+:[a-z0-9_./-]+$')


def _our(namespace):
    return namespace.startswith(OUR_NAMESPACES)


def _lang_keys():
    keys = set()
    modules = os.path.join(ROOT, 'modules')
    for dirpath, _, names in os.walk(modules):
        if 'build' in dirpath.split(os.sep):
            continue
        if dirpath.replace('\\', '/').endswith('/lang') and 'en_us.json' in names:
            with open(os.path.join(dirpath, 'en_us.json'), encoding='utf-8') as handle:
                keys.update(json.load(handle).keys())
    return keys


def _advancement_exists(namespace, path):
    pattern = os.path.join(ROOT, 'modules', '**', 'src', 'main', 'resources', 'data', namespace,
                           'advancement', *path.split('/')) + '.json'
    return bool(glob.glob(pattern, recursive=True))


def _guarded():
    words = []
    if os.path.exists(GUARDED_WORDS):
        with open(GUARDED_WORDS, encoding='utf-8') as handle:
            for line in handle:
                line = line.split('#', 1)[0].strip()
                if line:
                    words.append(line)
    return words


def _shapes_available():
    shapes = set(STOCK_SHAPES)
    # Quests finds shapes by folder name under textures/shapes in the ftbquests namespace, ours included.
    folder = os.path.join(os.path.dirname(COMPAT_ASSETS), 'ftbquests', 'textures', 'shapes')
    if os.path.isdir(folder):
        shapes.update(name for name in os.listdir(folder) if os.path.isdir(os.path.join(folder, name)))
    return shapes


def check(book):
    """Returns a list of problems (empty when the book is sound)."""
    problems = []
    lang = _lang_keys()
    guarded = _guarded()
    shapes = _shapes_available()
    planet_dims = {p.surface for p in chart.load_planets()}
    dimensions = VANILLA_DIMENSIONS | planet_dims | {chart.SPACE}

    chapter_slugs = [c.slug for c in book.chapters]
    if len(set(chapter_slugs)) != len(chapter_slugs):
        problems.append(f'duplicate chapter slugs: {chapter_slugs}')

    quests = {}
    for chapter in book.chapters:
        for quest in chapter.quests:
            key = f'{chapter.slug}.{quest.slug}'
            if key in quests:
                problems.append(f'duplicate quest {key}')
            quests[key] = (chapter, quest)

    def ref(chapter, reference):
        return reference if '.' in reference else f'{chapter.slug}.{reference}'

    def text_ok(where, text):
        for word in guarded:
            if re.search(r'(?<![A-Za-z])' + re.escape(word) + r'(?![A-Za-z])', text, re.IGNORECASE):
                problems.append(f'{where}: uses the guarded word "{word}": {text!r}')

    def texture_ok(where, path):
        if path in DYNAMIC_TEXTURES:
            return
        if not os.path.exists(os.path.join(COMPAT_ASSETS, *path.split('/'))):
            problems.append(f'{where}: picture {path} is missing from the compat jar')

    def item_ok(where, item_id):
        if not _ID.match(item_id):
            problems.append(f'{where}: "{item_id}" is not an item id')
            return
        namespace, path = item_id.split(':', 1)
        if _our(namespace) and f'item.{namespace}.{path}' not in lang and f'block.{namespace}.{path}' not in lang:
            problems.append(f'{where}: we have no item "{item_id}"')

    def icon_ok(where, icon):
        if isinstance(icon, Icon):
            if icon.item:
                item_ok(where, icon.item)
            if icon.texture:
                texture_ok(where, icon.texture)

    for chapter in book.chapters:
        where = f'chapter {chapter.slug}'
        text_ok(where + ' title', chapter.title)
        text_ok(where + ' subtitle', chapter.subtitle)
        icon_ok(where + ' icon', chapter.icon)
        if chapter.shape not in shapes:
            problems.append(f'{where}: no quest shape "{chapter.shape}"')

        for image in chapter.images:
            texture_ok(f'{where} image {image.slug}', image.texture)
            if image.shown_after and ref(chapter, image.shown_after) not in quests:
                problems.append(f'{where} image {image.slug}: shown after "{image.shown_after}", which is not a quest')
        slugs = [i.slug for i in chapter.images]
        if len(set(slugs)) != len(slugs):
            problems.append(f'{where}: duplicate image slugs')

        placed = []
        for quest in chapter.quests:
            q = f'{chapter.slug}.{quest.slug}'
            text_ok(q + ' title', quest.title)
            text_ok(q + ' subtitle', quest.subtitle)
            if not quest.text:
                problems.append(f'{q}: no text')
            for line in quest.text:
                if not isinstance(line, str) or not line.strip():
                    problems.append(f'{q}: an empty line of text')
                text_ok(q + ' text', line)
            icon_ok(q + ' icon', quest.icon)
            if quest.shape and quest.shape not in shapes:
                problems.append(f'{q}: no quest shape "{quest.shape}"')
            if not quest.tasks:
                problems.append(f'{q}: a quest with no task can never be completed')
            for dep in quest.deps:
                if ref(chapter, dep) not in quests:
                    problems.append(f'{q}: depends on "{dep}", which is not a quest')
                if ref(chapter, dep) == q:
                    problems.append(f'{q}: depends on itself')

            for task in quest.tasks:
                if task.kind == 'item':
                    item_ok(q + ' task', task.args['item'])
                elif task.kind == 'advancement':
                    namespace, path = task.args['advancement'].split(':', 1)
                    if _our(namespace):
                        if not _advancement_exists(namespace, path):
                            problems.append(f'{q}: we have no advancement "{namespace}:{path}"')
                    elif namespace == 'minecraft' and not path.startswith(VANILLA_ADVANCEMENT_ROOTS):
                        problems.append(f'{q}: "{namespace}:{path}" is not a vanilla advancement path')
                elif task.kind == 'dimension':
                    if task.args['dimension'] not in dimensions:
                        problems.append(f'{q}: unknown dimension "{task.args["dimension"]}"')
            for reward in quest.rewards:
                if reward.kind == 'item':
                    item_ok(q + ' reward', reward.args['item'])
                    if reward.args['count'] < 1:
                        problems.append(f'{q}: a reward of nothing')

            for other_x, other_y, other in placed:
                if abs(quest.x - other_x) < MIN_SPACING and abs(quest.y - other_y) < MIN_SPACING:
                    problems.append(f'{q}: sits on top of {other} ({quest.x},{quest.y})')
            placed.append((quest.x, quest.y, quest.slug))

    # Loops: Quests refuses a book whose dependencies go round in a circle.
    state = {}

    def visit(key, trail):
        if state.get(key) == 'done':
            return
        if state.get(key) == 'open':
            problems.append('dependency loop: ' + ' -> '.join(trail + [key]))
            return
        state[key] = 'open'
        chapter, quest = quests[key]
        for dep in quest.deps:
            target = ref(chapter, dep)
            if target in quests:
                visit(target, trail + [key])
        state[key] = 'done'

    for key in quests:
        visit(key, [])

    return problems
