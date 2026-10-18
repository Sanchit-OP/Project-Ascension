"""Turns the Book (tools/quests/model.py) into the files FTB Quests reads from config/ftbquests/quests/.

Layout, as FTB Quests 2101.1.36 loads it (read from the jar's BaseQuestFile, not guessed):

    data.snbt                 the book's own settings (and the file version, 13)
    chapter_groups.snbt       chapter groups; we have none
    chapters/<name>.snbt      one per page: id, group, order_index, filename, quests, quest_links, images
    lang/en_us.snbt           every title, subtitle and description, keyed  <type>.<id>.<field>

An id is `object_id(kind, slug)`. Nothing here depends on a running game.
"""

from quests import chart
from quests.model import object_id
from quests.snbt import Long, dumps

MOD = 'ascension_compat_ftbquests'
FILE_VERSION = 13          # BaseQuestFile.VERSION in 2101.1.36
DEFAULT_SHAPE = 'ascension_star'


def _texture(path):
    return f'{MOD}:{path}'


def _icon(icon):
    """An item stack: either the item, or Quests' custom-icon item carrying the picture's name."""
    if icon is None:
        return None
    if icon.item:
        return {'id': icon.item, 'count': 1}
    return {'id': 'ftbquests:custom_icon', 'count': 1, 'components': {'ftbquests:icon': _texture(icon.texture)}}


def _qid(chapter_slug, slug):
    return object_id('quest', f'{chapter_slug}.{slug}')


def _resolve(ref, chapter_slug):
    """'slug' (same chapter) or 'chapter.slug' to a quest id."""
    if '.' in ref:
        other, slug = ref.split('.', 1)
        return _qid(other, slug)
    return _qid(chapter_slug, ref)


def _task(chapter, quest, index, task):
    out = {'id': object_id('task', f'{chapter.slug}.{quest.slug}.{index}')}
    if task.kind == 'checkmark':
        out['type'] = 'checkmark'
    elif task.kind == 'item':
        out['type'] = 'item'
        out['item'] = task.args['item']
        out['count'] = Long(task.args['count'])
    elif task.kind == 'advancement':
        out['type'] = 'advancement'
        out['advancement'] = task.args['advancement']
        out['criterion'] = ''
    elif task.kind == 'dimension':
        out['type'] = 'dimension'
        out['dimension'] = task.args['dimension']
    elif task.kind == 'structure':
        out['type'] = 'structure'
        out['structure'] = task.args['structure']
    else:
        raise ValueError(f'unknown task kind {task.kind!r} in {chapter.slug}.{quest.slug}')
    return out


def _reward(chapter, quest, index, reward):
    out = {'id': object_id('reward', f'{chapter.slug}.{quest.slug}.{index}')}
    if reward.kind == 'item':
        out['type'] = 'item'
        out['item'] = reward.args['item']
        out['count'] = reward.args['count']
    elif reward.kind == 'xp_levels':
        out['type'] = 'xp_levels'
        out['xp_levels'] = reward.args['xp_levels']
    else:
        raise ValueError(f'unknown reward kind {reward.kind!r} in {chapter.slug}.{quest.slug}')
    return out


def _quest(chapter, quest):
    out = {'id': _qid(chapter.slug, quest.slug), 'x': float(quest.x), 'y': float(quest.y)}
    if quest.shape:
        out['shape'] = quest.shape
    if quest.size != 1.0:
        out['size'] = float(quest.size)
    icon = _icon(quest.icon)
    if icon:
        out['icon'] = icon
    if quest.deps:
        out['dependencies'] = [_resolve(ref, chapter.slug) for ref in quest.deps]
    if quest.hide_until == 'visible':
        out['hide_until_deps_visible'] = True
    elif quest.hide_until == 'complete':
        out['hide_until_deps_complete'] = True
    if quest.optional:
        out['optional'] = True
    if quest.secret:
        out['invisible'] = True
    out['tasks'] = [_task(chapter, quest, i, t) for i, t in enumerate(quest.tasks)]
    if quest.rewards:
        out['rewards'] = [_reward(chapter, quest, i, r) for i, r in enumerate(quest.rewards)]
    return out


def _image(chapter, image):
    out = {
        'id': object_id('image', f'{chapter.slug}.{image.slug}'),
        'x': float(image.x),
        'y': float(image.y),
        'width': float(image.width),
        'height': float(image.height),
        'image': _texture(image.texture),
        'order': image.order,
    }
    if image.alpha != 255:
        out['alpha'] = image.alpha
    if image.shown_after:
        out['dependency'] = _resolve(image.shown_after, chapter.slug)
    return out


def chapter_text(chapter, order_index):
    data = {
        'id': object_id('chapter', chapter.slug),
        'group': '',
        'order_index': order_index,
        'filename': chapter.slug,
        'default_quest_shape': chapter.shape,
    }
    icon = _icon(chapter.icon)
    if icon:
        data['icon'] = icon
    data['quests'] = [_quest(chapter, q) for q in chapter.quests]
    data['quest_links'] = []
    data['images'] = [_image(chapter, i) for i in chapter.images]
    return dumps(data)


def lang_text(book):
    entries = {}
    for chapter in book.chapters:
        cid = object_id('chapter', chapter.slug)
        entries[f'chapter.{cid}.title'] = chapter.title
        if chapter.subtitle:
            entries[f'chapter.{cid}.chapter_subtitle'] = [chapter.subtitle]
        for quest in chapter.quests:
            qid = _qid(chapter.slug, quest.slug)
            entries[f'quest.{qid}.title'] = quest.title
            if quest.subtitle:
                entries[f'quest.{qid}.quest_subtitle'] = quest.subtitle
            entries[f'quest.{qid}.quest_desc'] = list(quest.text)
    return dumps(entries, quote_keys=True)


def data_text():
    return dumps({
        'version': FILE_VERSION,
        'default_quest_shape': DEFAULT_SHAPE,
        # Never a gate (ADR-0018): a task counts the moment it is done, whether or not the quests before it are.
        'progression_mode': 'flexible',
    })


def all_files(book):
    """{path relative to config/ftbquests/quests: text} for the whole book."""
    files = {
        'data.snbt': data_text(),
        'chapter_groups.snbt': dumps({'chapter_groups': []}),
        'lang/en_us.snbt': lang_text(book),
    }
    for index, chapter in enumerate(book.chapters):
        files[f'chapters/{chapter.slug}.snbt'] = chapter_text(chapter, index)
    return files


def chart_layout_text():
    return chart.layout_text()
