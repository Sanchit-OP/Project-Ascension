"""Lays a project page out as a dependency chart, the way a roadmap is drawn: the needs at the top, the goal at the bottom.

A project page (docs/progression/dr-stone-roadmap.md) is a graph of quests that lead to one finale. Hand-placing thirty quests is where
pages go wrong (two stacked, a line through a quest), so these pages are placed by this module, from their dependencies, whenever the book
is read. Adding a quest means writing it and its dependencies; it finds its own place.

How:
  1. A quest's row is the length of its longest chain of dependencies on this page (only plain slugs count: a 'page.slug' reference is another
     page and draws no line here). Quests that nothing on the page needs are pulled down to the last row, so the finale sits at the bottom
     with the other ends of the branches beside it.
  2. Within a row, quests are ordered by where their parents are, swept down and up a few times so lines cross less.
  3. Each quest is put under the middle of its parents, then pushed apart where two would touch, row by row, down and up.
  4. A row wider than ROW_MAX is split into several lines.

Positions are written into the quests (x, y) and the page's sheet size is recorded in `chapter.blueprint.box`, which the picture and the
sheet's Image are made from. Deterministic: the same page gives the same picture.
"""

from quests.model import LANDMARK, Image

X_GAP = 2.5          # between neighbours in a row, in position units
Y_GAP = 2.2          # between rows
LINE_GAP = 1.5       # between the lines of one wide row
ROW_MAX = 8          # quests in one line of a row
MARGIN_X = 2.4       # sheet beyond the outermost quest, left and right
MARGIN_TOP = 2.6     # sheet above the first row (the sheet's heading sits here)
MARGIN_BOTTOM = 4.2  # below the last row (the title block sits here)
MIN_WIDTH = 18.4     # a sheet is never narrower than this: the legend and the title block have to fit side by side
FINALE_SIZE = 1.5

# FTB Quests draws a picture `w` units wide `w * button` pixels wide but puts an object `x` units away `x * (button + spacing)` pixels away
# (see tools/quests/chart.py); the sheet is a picture, the quests are objects.
POSITION_SCALE = 1.5 / (1.5 + 0.25)


def _local_parents(chapter):
    """{slug: [parent slugs on this page]}"""
    slugs = {q.slug for q in chapter.quests}
    parents = {}
    for quest in chapter.quests:
        local = []
        for ref in quest.deps:
            slug = ref.split('.', 1)[1] if ref.startswith(chapter.slug + '.') else ref
            if '.' not in slug and slug in slugs:
                local.append(slug)
        parents[quest.slug] = local
    return parents


def _rows(chapter, parents):
    """The row of every quest. On a strict page (every quest leads to the finale) each need sits as low as it can, right above whatever uses
    it, so no line runs the length of the page; a side note (optional) hangs one row under its lowest parent. Anywhere else, a quest's row is
    the length of its longest chain of dependencies."""
    quests = {q.slug: q for q in chapter.quests}
    children = {slug: [] for slug in quests}
    for slug, ps in parents.items():
        for p in ps:
            children[p].append(slug)
    memo = {}

    def depth(slug, trail=()):
        if slug in memo:
            return memo[slug]
        if slug in trail:
            raise ValueError(f'dependency loop on {chapter.slug}: {" -> ".join(trail + (slug,))}')
        value = 0 if not parents[slug] else 1 + max(depth(p, trail + (slug,)) for p in parents[slug])
        memo[slug] = value
        return value

    asap = {slug: depth(slug) for slug in quests}
    finale = chapter.blueprint.finale if chapter.blueprint else None
    if finale not in quests or not chapter.blueprint.strict:
        return asap
    last = asap[finale]

    late = {}

    def lowest(slug):
        """The lowest row this quest can sit in, or None for a side note."""
        if slug in late:
            return late[slug]
        if slug == finale:
            value = last
        else:
            kids = [c for c in children[slug] if not quests[c].optional]
            if not kids:
                value = None if quests[slug].optional else last      # a dangling need (the validator reports it) goes to the bottom
            else:
                value = min(lowest(c) for c in kids) - 1
        late[slug] = value
        return value

    row = {}
    for slug in sorted(quests, key=lambda s: (asap[s], s)):
        fixed = lowest(slug)
        if fixed is not None:
            row[slug] = max(fixed, asap[slug])
        else:
            row[slug] = max((row[p] for p in parents[slug]), default=-1) + 1
    return row


def _mean(values, default):
    values = list(values)
    return sum(values) / len(values) if values else default


def layered(chapter):
    """Place every quest of the chapter and record the sheet's size. Returns the sheet box (left, top, right, bottom)."""
    parents = _local_parents(chapter)
    children = {slug: [] for slug in parents}
    for slug, ps in parents.items():
        for p in ps:
            children[p].append(slug)
    row_of = _rows(chapter, parents)
    quests = {q.slug: q for q in chapter.quests}
    order = {q.slug: i for i, q in enumerate(chapter.quests)}
    finale = chapter.blueprint.finale if chapter.blueprint else None
    last = max(row_of.values()) if row_of else 0

    rows = [[] for _ in range(last + 1)]
    for q in chapter.quests:
        rows[row_of[q.slug]].append(q.slug)

    # Start: the order the page was written in, the finale in the middle of its row.
    x = {}
    for members in rows:
        if finale in members:
            members.remove(finale)
            members.insert(len(members) // 2, finale)
        for i, slug in enumerate(members):
            x[slug] = (i - (len(members) - 1) / 2) * X_GAP

    def sweep(downward):
        indices = range(1, len(rows)) if downward else range(len(rows) - 2, -1, -1)
        for r in indices:
            members = rows[r]
            neighbours = parents if downward else children

            def centre(slug):
                return _mean((x[n] for n in neighbours[slug] if row_of[n] != r), x[slug])

            members.sort(key=lambda s: (centre(s), order[s]))
            desired = {s: centre(s) for s in members}
            placed = {}
            previous = None
            for s in members:
                value = desired[s] if previous is None else max(desired[s], placed[previous] + X_GAP)
                placed[s] = value
                previous = s
            shift = _mean((desired[s] - placed[s] for s in members), 0.0)
            for s in members:
                x[s] = placed[s] + shift

    for _ in range(4):
        sweep(True)
        sweep(False)
    sweep(True)

    # Split a row that is too wide into lines, and give the rows their heights.
    y = {}
    cursor = 0.0
    for members in rows:
        lines = [members[i:i + ROW_MAX] for i in range(0, len(members), ROW_MAX)] or [[]]
        if len(lines) > 1:
            for line in lines:
                for i, slug in enumerate(line):
                    x[slug] = (i - (len(line) - 1) / 2) * X_GAP
        for n, line in enumerate(lines):
            for slug in line:
                y[slug] = cursor + n * LINE_GAP
        cursor += (len(lines) - 1) * LINE_GAP + Y_GAP

    # The finale is bigger and, if it is not already the lowest, goes below everything else's row.
    for slug, quest in quests.items():
        quest.x = round(x[slug], 2)
        quest.y = round(y[slug], 2)
    if finale in quests:
        quests[finale].size = FINALE_SIZE
        quests[finale].shape = LANDMARK

    # Centre the page on x = 0 and start it at y = 0.
    xs = [q.x for q in chapter.quests]
    middle = (min(xs) + max(xs)) / 2 if xs else 0.0
    for quest in chapter.quests:
        quest.x = round(quest.x - middle, 2)
    left = min(q.x for q in chapter.quests) - MARGIN_X
    right = max(q.x for q in chapter.quests) + MARGIN_X
    if right - left < MIN_WIDTH:
        pad = (MIN_WIDTH - (right - left)) / 2
        left, right = left - pad, right + pad
    top = min(q.y for q in chapter.quests) - MARGIN_TOP
    bottom = max(q.y for q in chapter.quests) + MARGIN_BOTTOM
    box = (round(left, 2), round(top, 2), round(right, 2), round(bottom, 2))
    if chapter.blueprint:
        chapter.blueprint.box = box
    return box


def sheet_image(chapter, texture):
    """The Image object for a laid-out page: the sheet, centred on its box, under everything."""
    left, top, right, bottom = chapter.blueprint.box
    return Image('sheet', texture, round((left + right) / 2, 3), round((top + bottom) / 2, 3),
                 round((right - left) / POSITION_SCALE, 3), round((bottom - top) / POSITION_SCALE, 3), order=0)


def fit_box(chapter):
    """The sheet for a page that keeps its own hand-placed layout: the quests and any pictures already on it, plus the margins."""
    xs = [q.x for q in chapter.quests]
    ys = [q.y for q in chapter.quests]
    left, right, top, bottom = min(xs), max(xs), min(ys), max(ys)
    for image in chapter.images:
        # An Image is centred on (x, y) and is drawn at `width` picture units, which are POSITION_SCALE of a position unit
        w = image.width * POSITION_SCALE / 2
        h = image.height * POSITION_SCALE / 2
        left, right = min(left, image.x - w), max(right, image.x + w)
        top, bottom = min(top, image.y - h), max(bottom, image.y + h)
    left, right, top, bottom = left - MARGIN_X, right + MARGIN_X, top - MARGIN_TOP, bottom + MARGIN_BOTTOM
    if right - left < MIN_WIDTH:
        pad = (MIN_WIDTH - (right - left)) / 2
        left, right = left - pad, right + pad
    box = (round(left, 2), round(top, 2), round(right, 2), round(bottom, 2))
    chapter.blueprint.box = box
    return box


def blueprint_page(chapter, number, of, motif, finale, points=0, place=True, strict=True):
    """Turns a chapter into a project page: sets its blueprint, places its quests (unless `place` is false: the page keeps its own layout)
    and adds the sheet under everything. Call it once, last."""
    from quests.model import Blueprint
    chapter.blueprint = Blueprint(number=number, of=of, motif=motif, finale=finale, points=points, strict=strict)
    if place:
        layered(chapter)
    else:
        fit_box(chapter)
    sheet = sheet_image(chapter, f'textures/quests/blueprint/{chapter.slug}.png')
    sheet.order = -1
    chapter.images.insert(0, sheet)
    return chapter
