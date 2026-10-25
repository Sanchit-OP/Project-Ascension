"""Everything about the book that can be wrong, caught at build time instead of in somebody's game.

A mistake here is silent in the game: a quest naming a misspelt item shows a "missing item" stand-in and
can never be completed; a dependency on a slug that does not exist is dropped; a line of text that gives
the story away is read by every player. This is the same idea as tools/check-data.py.

Checked:
  * slugs are unique, dependencies resolve and do not loop, positions do not stack quests on each other
  * items, advancements, dimensions and structures that belong to us exist (vanilla ones are checked
    by shape here, and for real by QuestChecks on a server)
  * pictures and quest shapes exist in the compat jar
  * a project page (a blueprint): its finale exists, every quest that is not marked optional leads to it, its sheet is the shape of its picture,
    and the skill points it advertises are what the growth milestone for the finale's advancement really pays
  * no text uses a guarded word (pack/quests/guarded-words.txt)
"""

import glob
import json
import os
import re
import struct

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

# What fits the book's panels. A page is read in a game screen, not a document: say less.
TITLE_MAX = 30
SUBTITLE_MAX = 44
LINES_MAX = 5
LINE_MAX = 160

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


_ADVANCEMENTS = None


def _advancement_exists(namespace, path):
    """Whether one of our modules ships data/<namespace>/advancement/<path>.json. The files are listed once per run."""
    global _ADVANCEMENTS
    if _ADVANCEMENTS is None:
        found = set()
        for dirpath, dirs, names in os.walk(os.path.join(ROOT, 'modules')):
            dirs[:] = [d for d in dirs if d not in ('build', '.gradle', 'run')]
            parts = dirpath.replace(os.sep, '/').split('/')
            if 'advancement' not in parts or 'data' not in parts:
                continue
            at = parts.index('advancement')
            for name in names:
                if name.endswith('.json'):
                    # .../data/<namespace>/advancement/<rest>/<name>.json
                    base = parts[:at]
                    if len(base) >= 2 and base[-2] == 'data':
                        rest = parts[at + 1:] + [name[:-5]]
                        found.add(base[-1] + ':' + '/'.join(rest))
        _ADVANCEMENTS = found
    return f'{namespace}:{path}' in _ADVANCEMENTS


def _structure_exists(namespace, path):
    pattern = os.path.join(ROOT, 'modules', '**', 'src', 'main', 'resources', 'data', namespace,
                           'worldgen', 'structure', *path.split('/')) + '.json'
    return bool(glob.glob(pattern, recursive=True))


def _png_size(path):
    """(width, height) of a PNG, read from its header."""
    with open(path, 'rb') as handle:
        head = handle.read(24)
    if head[:8] != bytes([0x89]) + b'PNG' + bytes([0x0D, 0x0A, 0x1A, 0x0A]):
        return None
    return struct.unpack('>II', head[16:24])


def milestone_points():
    """{advancement id: (points, shared)} for every growth milestone file in the repository."""
    found = {}
    pattern = os.path.join(ROOT, 'modules', '**', 'src', 'main', 'resources', 'data', '*', 'ascension_combat', 'growth_milestone', '**', '*.json')
    for path in glob.glob(pattern, recursive=True):
        with open(path, encoding='utf-8') as handle:
            data = json.load(handle)
        if 'advancement' in data:
            found[data['advancement']] = (int(data.get('points', 0)), bool(data.get('shared', False)))
    return found


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


class _Checker:
    """One run over one book. Each method looks at one kind of thing and adds to `problems`."""

    def __init__(self, book):
        self.book = book
        self.problems = []
        self.lang = _lang_keys()
        self.guarded = _guarded()
        self.shapes = _shapes_available()
        self.dimensions = VANILLA_DIMENSIONS | {p.surface for p in chart.load_planets()} | {chart.SPACE}
        self.quests = {}            # 'chapter.slug' -> (chapter, quest)
        self.milestones = milestone_points()

    # -- the whole book -------------------------------------------------------------------------

    def run(self):
        chapter_slugs = [c.slug for c in self.book.chapters]
        if len(set(chapter_slugs)) != len(chapter_slugs):
            self.problems.append(f'duplicate chapter slugs: {chapter_slugs}')
        self._index_quests()
        for chapter in self.book.chapters:
            self._chapter(chapter)
        self._dependency_loops()
        return self.problems

    def _index_quests(self):
        for chapter in self.book.chapters:
            for quest in chapter.quests:
                key = f'{chapter.slug}.{quest.slug}'
                if key in self.quests:
                    self.problems.append(f'duplicate quest {key}')
                self.quests[key] = (chapter, quest)

    # -- small questions ------------------------------------------------------------------------

    @staticmethod
    def _ref(chapter, reference):
        return reference if '.' in reference else f'{chapter.slug}.{reference}'

    def _text(self, where, text):
        for word in self.guarded:
            if re.search(r'(?<![A-Za-z])' + re.escape(word) + r'(?![A-Za-z])', text, re.IGNORECASE):
                self.problems.append(f'{where}: uses the guarded word "{word}": {text!r}')

    def _texture(self, where, path):
        if path in DYNAMIC_TEXTURES:
            return
        if not os.path.exists(os.path.join(COMPAT_ASSETS, *path.split('/'))):
            self.problems.append(f'{where}: picture {path} is missing from the compat jar')

    def _item(self, where, item_id):
        if not _ID.match(item_id):
            self.problems.append(f'{where}: "{item_id}" is not an item id')
            return
        namespace, path = item_id.split(':', 1)
        if (_our(namespace) and f'item.{namespace}.{path}' not in self.lang
                and f'block.{namespace}.{path}' not in self.lang):
            self.problems.append(f'{where}: we have no item "{item_id}"')

    def _icon(self, where, icon):
        if isinstance(icon, Icon):
            if icon.item:
                self._item(where, icon.item)
            if icon.texture:
                self._texture(where, icon.texture)

    # -- a page ---------------------------------------------------------------------------------

    def _chapter(self, chapter):
        where = f'chapter {chapter.slug}'
        self._text(where + ' title', chapter.title)
        self._text(where + ' subtitle', chapter.subtitle)
        self._icon(where + ' icon', chapter.icon)
        if chapter.shape not in self.shapes:
            self.problems.append(f'{where}: no quest shape "{chapter.shape}"')

        for image in chapter.images:
            self._texture(f'{where} image {image.slug}', image.texture)
            if image.shown_after and self._ref(chapter, image.shown_after) not in self.quests:
                self.problems.append(
                    f'{where} image {image.slug}: shown after "{image.shown_after}", which is not a quest')
        slugs = [i.slug for i in chapter.images]
        if len(set(slugs)) != len(slugs):
            self.problems.append(f'{where}: duplicate image slugs')

        placed = []
        for quest in chapter.quests:
            self._quest(chapter, quest, placed)
        # A way in is a quest that waits for nothing on its own page (a project page may wait for another page: a lock, never a gate).
        if chapter.quests and not any(not [d for d in q.deps if '.' not in d or d.startswith(chapter.slug + '.')] for q in chapter.quests):
            self.problems.append(f'{where}: every quest depends on another, so the page has no way in')
        if chapter.blueprint:
            self._blueprint(chapter)

    def _blueprint(self, chapter):
        where = f'chapter {chapter.slug} (blueprint)'
        blueprint = chapter.blueprint
        by_slug = {q.slug: q for q in chapter.quests}
        if blueprint.finale not in by_slug:
            self.problems.append(f'{where}: the finale "{blueprint.finale}" is not a quest of the page')
            return
        # Every quest that is not a side note leads to the finale, as every need on a roadmap does.
        local = {}
        for quest in chapter.quests:
            for ref in quest.deps:
                slug = ref.split('.', 1)[1] if ref.startswith(chapter.slug + '.') else ref
                if slug in by_slug:
                    local.setdefault(slug, []).append(quest.slug)
        reaches = set()

        def walk(slug):
            if slug in reaches:
                return
            reaches.add(slug)
            for child in local.get(slug, []):
                walk(child)
        # "reaches the finale" is the same as "is an ancestor of it": walk up from the finale instead
        parents = {q.slug: [r.split('.', 1)[1] if r.startswith(chapter.slug + '.') else r for r in q.deps] for q in chapter.quests}
        stack = [blueprint.finale]
        ancestors = set()
        while stack:
            current = stack.pop()
            for parent in parents.get(current, []):
                if parent in by_slug and parent not in ancestors:
                    ancestors.add(parent)
                    stack.append(parent)
        for quest in chapter.quests:
            if blueprint.strict and quest.slug != blueprint.finale and quest.slug not in ancestors and not quest.optional:
                self.problems.append(f'{where}: {quest.slug} does not lead to the finale {blueprint.finale} (make it optional, or give the finale a dependency on it)')
        if blueprint.box is None:
            self.problems.append(f'{where}: the sheet has not been laid out')
        else:
            left, top, right, bottom = blueprint.box
            for quest in chapter.quests:
                if not (left <= quest.x <= right and top <= quest.y <= bottom):
                    self.problems.append(f'{where}: {quest.slug} is off the sheet')
        sheets = [i for i in chapter.images if i.slug == 'sheet']
        if len(sheets) != 1:
            self.problems.append(f'{where}: expected one sheet picture, found {len(sheets)}')
        else:
            path = os.path.join(COMPAT_ASSETS, *sheets[0].texture.split('/'))
            if os.path.exists(path):
                size = _png_size(path)
                if size is None:
                    self.problems.append(f'{where}: the sheet is not a PNG')
                elif abs(size[0] / size[1] - sheets[0].width / sheets[0].height) > 0.004:
                    self.problems.append(f'{where}: the sheet picture is {size[0]}x{size[1]} but the page wants {sheets[0].width}x{sheets[0].height} '
                                         f'(run python tools/generate-blueprint-art.py)')
        finale = by_slug[blueprint.finale]
        advancements = [t.args['advancement'] for t in finale.tasks if t.kind == 'advancement']
        paid = [self.milestones[a] for a in advancements if a in self.milestones]
        pays = sum(p[0] for p in paid)
        if pays != blueprint.points:
            self.problems.append(f'{where}: the page says its finale pays {blueprint.points} skill point(s) but the growth milestones for '
                                 f'{advancements or "no advancement"} pay {pays}')

    # -- a quest --------------------------------------------------------------------------------

    def _quest(self, chapter, quest, placed):
        q = f'{chapter.slug}.{quest.slug}'
        self._text(q + ' title', quest.title)
        self._text(q + ' subtitle', quest.subtitle)
        if len(quest.title) > TITLE_MAX:
            self.problems.append(f'{q}: title is {len(quest.title)} characters (the panel fits {TITLE_MAX})')
        if len(quest.subtitle) > SUBTITLE_MAX:
            self.problems.append(f'{q}: subtitle is {len(quest.subtitle)} characters (the panel fits {SUBTITLE_MAX})')
        if not quest.text:
            self.problems.append(f'{q}: no text')
        if len(quest.text) > LINES_MAX:
            self.problems.append(f'{q}: {len(quest.text)} lines of text (a quest says at most {LINES_MAX})')
        for line in quest.text:
            if not isinstance(line, str) or not line.strip():
                self.problems.append(f'{q}: an empty line of text')
            if isinstance(line, str) and len(line) > LINE_MAX:
                self.problems.append(f'{q}: a line of {len(line)} characters (at most {LINE_MAX}): {line[:40]!r}...')
            self._text(q + ' text', line)
        self._icon(q + ' icon', quest.icon)
        if quest.shape and quest.shape not in self.shapes:
            self.problems.append(f'{q}: no quest shape "{quest.shape}"')
        if not quest.tasks:
            self.problems.append(f'{q}: a quest with no task can never be completed')

        if quest.requirement not in (None, 'all_completed', 'one_completed', 'all_started', 'one_started'):
            self.problems.append(f'{q}: dependency requirement "{quest.requirement}" is not one FTB Quests knows')
        if quest.requirement and not quest.deps:
            self.problems.append(f'{q}: a dependency requirement with no dependencies')
        for dep in quest.deps:
            if self._ref(chapter, dep) not in self.quests:
                self.problems.append(f'{q}: depends on "{dep}", which is not a quest')
            if self._ref(chapter, dep) == q:
                self.problems.append(f'{q}: depends on itself')
        for task in quest.tasks:
            self._task(q, task)
        for reward in quest.rewards:
            self._reward(q, reward)

        for other_x, other_y, other in placed:
            if abs(quest.x - other_x) < MIN_SPACING and abs(quest.y - other_y) < MIN_SPACING:
                self.problems.append(f'{q}: sits on top of {other} ({quest.x},{quest.y})')
        placed.append((quest.x, quest.y, quest.slug))

    def _task(self, q, task):
        if task.kind == 'item':
            self._item(q + ' task', task.args['item'])
        elif task.kind == 'advancement':
            namespace, path = task.args['advancement'].split(':', 1)
            if _our(namespace):
                if not _advancement_exists(namespace, path):
                    self.problems.append(f'{q}: we have no advancement "{namespace}:{path}"')
            elif namespace == 'minecraft' and not path.startswith(VANILLA_ADVANCEMENT_ROOTS):
                self.problems.append(f'{q}: "{namespace}:{path}" is not a vanilla advancement path')
        elif task.kind == 'structure':
            namespace, path = task.args['structure'].lstrip('#').split(':', 1)
            if _our(namespace) and not _structure_exists(namespace, path):
                self.problems.append(f'{q}: we have no structure "{namespace}:{path}"')
        elif task.kind == 'dimension':
            if task.args['dimension'] not in self.dimensions:
                self.problems.append(f'{q}: unknown dimension "{task.args["dimension"]}"')

    def _reward(self, q, reward):
        if reward.kind == 'item':
            self._item(q + ' reward', reward.args['item'])
            if reward.args['count'] < 1:
                self.problems.append(f'{q}: a reward of nothing')

    # -- loops ----------------------------------------------------------------------------------

    def _dependency_loops(self):
        """Quests refuses a book whose dependencies go round in a circle."""
        state = {}

        def visit(key, trail):
            if state.get(key) == 'done':
                return
            if state.get(key) == 'open':
                self.problems.append('dependency loop: ' + ' -> '.join(trail + [key]))
                return
            state[key] = 'open'
            chapter, quest = self.quests[key]
            for dep in quest.deps:
                target = self._ref(chapter, dep)
                if target in self.quests:
                    visit(target, trail + [key])
            state[key] = 'done'

        for key in self.quests:
            visit(key, [])


def check(book):
    """Returns a list of problems (empty when the book is sound)."""
    return _Checker(book).run()
