"""Tests for the quest generator (tools/quests). Run with:

    python -m unittest discover -s tools/quests/tests -t tools

Standard library only. They check the generator's own promises: the writer's text, stable ids, that the
files it makes agree with each other, the chart's arithmetic, and that the validator really catches a
bad book (a validator that finds nothing wrong is worth nothing until it has been seen to find something).
"""

import math
import os
import re
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, '..', '..'))
ROOT = os.path.abspath(os.path.join(TOOLS, '..'))
sys.path.insert(0, TOOLS)
sys.path.insert(0, os.path.join(ROOT, 'pack', 'quests'))

from quests import chart, emit, validate  # noqa: E402
from quests.model import (Book, Chapter, Quest, advancement, checkmark, dimension, give, item,  # noqa: E402
                          item_icon, object_id, picture, structure)
from quests.snbt import Long, dumps  # noqa: E402


class SnbtTest(unittest.TestCase):

    def test_scalars(self):
        self.assertEqual('5', dumps(5).strip())
        self.assertEqual('5L', dumps(Long(5)).strip())
        self.assertEqual('1.5d', dumps(1.5).strip())
        self.assertEqual('0.0d', dumps(0.0).strip())
        self.assertEqual('true', dumps(True).strip())
        self.assertEqual('"a \\"b\\" \\\\ c"', dumps('a "b" \\ c').strip())

    def test_a_bool_is_not_written_as_an_int(self):
        self.assertEqual('true', dumps(True).strip())
        self.assertEqual('false', dumps(False).strip())

    def test_compounds_use_tabs_and_no_commas(self):
        text = dumps({'id': 'X', 'inner': {'a': 1}})
        self.assertEqual('{\n\tid: "X"\n\tinner: {\n\t\ta: 1\n\t}\n}\n', text)

    def test_lists_of_scalars_are_inline_and_of_compounds_are_not(self):
        self.assertIn('["a", "b"]', dumps({'k': ['a', 'b']}))
        text = dumps({'k': [{'a': 1}, {'a': 2}]})
        self.assertIn('[\n\t\t{\n\t\t\ta: 1', text)

    def test_keys_with_dots_are_quoted_only_when_asked(self):
        self.assertIn('a.b: 1', dumps({'a.b': 1}))
        self.assertIn('"a.b": 1', dumps({'a.b': 1}, quote_keys=True))
        self.assertIn('"has space": 1', dumps({'has space': 1}))

    def test_nan_cannot_be_written(self):
        with self.assertRaises(ValueError):
            dumps(float('nan'))


class IdTest(unittest.TestCase):

    def test_an_id_is_sixteen_uppercase_hex_digits_and_never_small(self):
        for slug in ('a', 'first_days.open_ledger', 'x' * 200):
            oid = object_id('quest', slug)
            self.assertRegex(oid, r'^[0-9A-F]{16}$')
            self.assertGreaterEqual(int(oid, 16), 1 << 40)
            self.assertLess(int(oid, 16), 1 << 63, 'kept positive')

    def test_an_id_is_stable(self):
        # If this changes, every player's progress in every existing world is stranded.
        self.assertEqual('738C3477565410E7', object_id('quest', 'first_days.open_ledger'))

    def test_the_kind_is_part_of_the_id(self):
        self.assertNotEqual(object_id('quest', 'x'), object_id('task', 'x'))


def _quest(slug, **kwargs):
    defaults = dict(x=0, y=0, title='T', text=['Go.'], tasks=[checkmark()])
    defaults.update(kwargs)
    return Quest(slug=slug, **defaults)


def _book(*quests):
    return Book([Chapter('c', 'C', '', picture('textures/quests/icons/planets.png'),
                         quests=list(quests), shape='ascension_star')])


class BookTest(unittest.TestCase):
    """The real book, as written in pack/quests."""

    @classmethod
    def setUpClass(cls):
        from book import BOOK
        cls.book = BOOK
        cls.files = emit.all_files(BOOK)

    def test_the_real_book_has_no_problems(self):
        self.assertEqual([], validate.check(self.book))

    def test_every_object_id_in_the_chapter_files_is_unique(self):
        ids = []
        for name, text in self.files.items():
            if name.startswith('chapters/'):
                ids += re.findall(r'\bid: "([0-9A-F]{16})"', text)
        self.assertEqual(len(ids), len(set(ids)))
        self.assertGreater(len(ids), 20)

    def test_every_dependency_names_a_quest_that_exists(self):
        quest_ids, deps = set(), []
        for name, text in self.files.items():
            if name.startswith('chapters/'):
                for chunk in re.split(r'\n\t\t\{\n', text)[1:]:
                    quest_ids.add(re.match(r'\t\t\tid: "([0-9A-F]{16})"', chunk).group(1))
                    for line in re.findall(r'dependencies: \[(.*?)\]', chunk):
                        deps += re.findall(r'"([0-9A-F]{16})"', line)
        self.assertTrue(deps)
        for dep in deps:
            self.assertIn(dep, quest_ids)

    def test_every_translation_key_belongs_to_an_object_in_a_chapter_file(self):
        chapters = ''.join(t for n, t in self.files.items() if n.startswith('chapters/'))
        ids = set(re.findall(r'\bid: "([0-9A-F]{16})"', chapters))
        keys = re.findall(r'^\t"(\w+)\.([0-9A-F]{16})\.(\w+)": ', self.files['lang/en_us.snbt'], re.M)
        self.assertTrue(keys)
        for kind, oid, field in keys:
            self.assertIn(oid, ids, f'{kind}.{oid}.{field}')
            self.assertIn(field, ('title', 'quest_subtitle', 'quest_desc', 'chapter_subtitle'))

    def test_every_quest_has_a_title_and_text_in_the_lang_file(self):
        lang = self.files['lang/en_us.snbt']
        for chapter in self.book.chapters:
            for quest in chapter.quests:
                qid = emit.quest_id(chapter, quest)
                self.assertIn(f'"quest.{qid}.title"', lang)
                self.assertIn(f'"quest.{qid}.quest_desc"', lang)

    def test_the_progression_mode_never_gates(self):
        self.assertIn('progression_mode: "flexible"', self.files['data.snbt'])

    def test_each_world_is_on_the_planets_page_where_the_chart_puts_it(self):
        planets = next(c for c in self.book.chapters if c.slug == 'planets')
        by_slug = {q.slug: q for q in planets.quests}
        for planet in chart.load_planets():
            quest = by_slug[planet.id]
            ux, uy = planet.units
            self.assertAlmostEqual(ux, quest.x, places=2)
            self.assertAlmostEqual(uy, quest.y, places=2)

    def test_the_marker_picture_covers_the_chart_exactly(self):
        planets = next(c for c in self.book.chapters if c.slug == 'planets')
        chart_image = next(i for i in planets.images if i.slug == 'chart')
        marker = next(i for i in planets.images if i.slug == 'you_are_here')
        self.assertEqual((chart_image.x, chart_image.y, chart_image.width, chart_image.height),
                         (marker.x, marker.y, marker.width, marker.height))
        self.assertGreater(marker.order, chart_image.order)


class ChartTest(unittest.TestCase):

    def test_earth_is_the_middle(self):
        self.assertEqual(chart.CENTER, chart.project(0, 0))

    def test_the_range_lands_on_the_edge_and_a_quarter_of_it_halfway(self):
        x, y = chart.project(chart.RANGE_BLOCKS, 0)
        self.assertAlmostEqual(chart.CENTER[0] + chart.RADIUS, x)
        x, y = chart.project(0, chart.RANGE_BLOCKS / 4)
        self.assertAlmostEqual(chart.CENTER[1] + chart.RADIUS / 2, y)

    def test_beyond_the_range_stays_on_the_edge(self):
        x, y = chart.project(10 * chart.RANGE_BLOCKS, 0)
        self.assertAlmostEqual(chart.CENTER[0] + chart.RADIUS, x)

    def test_north_is_up_and_east_is_right(self):
        east = chart.project(1000, 0)
        north = chart.project(0, -1000)
        self.assertGreater(east[0], chart.CENTER[0])
        self.assertLess(north[1], chart.CENTER[1])

    def test_the_layout_lists_every_planet_and_enough_sample_points(self):
        layout = chart.layout()
        self.assertEqual(len(chart.load_planets()), len(layout['planets']))
        self.assertGreaterEqual(len(layout['golden']), 8)
        for gx, gz, px, py in layout['golden']:
            x, y = chart.project(gx, gz)
            self.assertAlmostEqual(px, x, places=5)
            self.assertAlmostEqual(py, y, places=5)

    def test_the_picture_fits_the_chart(self):
        self.assertLess(chart.RADIUS, chart.SIZE / 2)
        self.assertAlmostEqual(chart.UNIT_PX * chart.UNITS_ACROSS, chart.SIZE)

    def test_the_theme_spacing_is_the_one_the_chart_assumes(self):
        theme = os.path.join(chart.ROOT, 'modules', 'compat', 'ftbquests', 'src', 'main', 'resources', 'assets',
                             'ftbquests', 'ftb_quests_theme.txt')
        with open(theme, encoding='utf-8') as handle:
            found = re.search(r'^quest_spacing:\s+([0-9.]+)', handle.read(), re.M)
        self.assertIsNotNone(found, 'the theme file has no quest_spacing')
        self.assertAlmostEqual(float(found.group(1)), chart.QUEST_SPACING)

    def test_a_world_is_where_the_picture_says_it_is(self):
        # FTB Quests draws a picture w units wide at w * button pixels and an object x units out at
        # x * (button + spacing) pixels (see chart.py). Both must name the same spot, or a world sits off
        # the rings and the marker.
        for planet in chart.load_planets():
            picture = [(p - c) / chart.SIZE * chart.UNITS_ACROSS * chart.BUTTON_SIZE
                       for p, c in zip(planet.pixel, chart.CENTER)]
            placed = [u * (chart.BUTTON_SIZE + chart.SPACING_SIZE) for u in planet.units]
            for a, b in zip(picture, placed):
                self.assertAlmostEqual(a, b, places=6, msg=planet.id)


class ValidatorCatchesTest(unittest.TestCase):
    """Each of these books is wrong in one way; the validator must say so."""

    def problems(self, book):
        return '\n'.join(validate.check(book))

    def test_a_clean_book_is_clean(self):
        self.assertEqual([], validate.check(_book(_quest('a'), _quest('b', x=2, deps=['a']))))

    def test_a_missing_dependency(self):
        self.assertIn('depends on "ghost"', self.problems(_book(_quest('a', deps=['ghost']))))

    def test_a_dependency_loop(self):
        text = self.problems(_book(_quest('a', deps=['b']), _quest('b', x=2, deps=['a'])))
        self.assertIn('dependency loop', text)

    def test_a_quest_on_top_of_another(self):
        self.assertIn('sits on top of', self.problems(_book(_quest('a'), _quest('b'))))

    def test_a_quest_with_no_task(self):
        self.assertIn('no task', self.problems(_book(_quest('a', tasks=[]))))

    def test_a_guarded_word_in_a_title_or_in_text(self):
        self.assertIn('guarded word', self.problems(_book(_quest('a', title='The Observers'))))
        self.assertIn('guarded word', self.problems(_book(_quest('a', text=['Tacita waits.']))))

    def test_a_guarded_word_is_matched_as_a_word_and_in_any_case(self):
        self.assertIn('guarded word', self.problems(_book(_quest('a', text=['the OBSERVER saw']))))
        self.assertEqual([], validate.check(_book(_quest('a', text=['Observation is useful.']))))

    def test_an_item_of_ours_that_does_not_exist(self):
        text = self.problems(_book(_quest('a', icon=item_icon('ascension_worlds:no_such_item'))))
        self.assertIn('we have no item', text)
        text = self.problems(_book(_quest('a', tasks=[item('ascension_worlds:no_such_item', 1)])))
        self.assertIn('we have no item', text)
        text = self.problems(_book(_quest('a', rewards=[give('ascension_atmosphere:no_such', 1)])))
        self.assertIn('we have no item', text)

    def test_an_item_of_ours_that_does_exist_is_fine(self):
        self.assertEqual([], validate.check(_book(
            _quest('a', icon=item_icon('ascension_worlds:titanium_ingot'),
                   tasks=[item('ascension_atmosphere:oxygen_tank', 1)]))))

    def test_an_advancement_of_ours_that_does_not_exist(self):
        text = self.problems(_book(_quest('a', tasks=[advancement('ascension_worlds:gates/nowhere')])))
        self.assertIn('no advancement', text)

    def test_an_unknown_dimension(self):
        self.assertIn('unknown dimension', self.problems(_book(_quest('a', tasks=[dimension('nope:nowhere')]))))

    def test_a_missing_picture_and_a_missing_shape(self):
        self.assertIn('missing from the compat jar',
                      self.problems(_book(_quest('a', icon=picture('textures/quests/nowhere.png')))))
        self.assertIn('no quest shape', self.problems(_book(_quest('a', shape='hexagram'))))

    def test_a_duplicate_slug(self):
        self.assertIn('duplicate quest', self.problems(_book(_quest('a'), _quest('a', x=3))))

    def test_text_that_does_not_fit_the_panel(self):
        self.assertIn('title is', self.problems(_book(_quest('a', title='T' * 31))))
        self.assertIn('subtitle is', self.problems(_book(_quest('a', subtitle='s' * 45))))
        self.assertIn('lines of text', self.problems(_book(_quest('a', text=['x'] * 6))))
        self.assertIn('a line of', self.problems(_book(_quest('a', text=['y' * 161]))))
        self.assertEqual([], validate.check(_book(_quest('a', title='T' * 30, subtitle='s' * 44,
                                                          text=['y' * 160] * 5))))

    def test_a_structure_of_ours_that_does_not_exist(self):
        self.assertIn('we have no structure',
                      self.problems(_book(_quest('a', tasks=[structure('ascension_worlds:no_such_place')]))))
        self.assertEqual([], validate.check(_book(
            _quest('a', tasks=[structure('ascension_worlds:lunar_archive')]))))
        self.assertEqual([], validate.check(_book(_quest('a', tasks=[structure('minecraft:mineshaft')]))))

    def test_a_page_with_no_way_in(self):
        text = self.problems(_book(_quest('a', deps=['b']), _quest('b', x=2, deps=['a'])))
        self.assertIn('no way in', text)


class ProjectPagesTest(unittest.TestCase):
    """The nine project pages (docs/progression/dr-stone-roadmap.md): laid out by dependency, ids kept, and a validator that notices a bad one."""

    @classmethod
    def setUpClass(cls):
        from book import BOOK
        cls.book = BOOK
        cls.pages = [c for c in BOOK.chapters if c.blueprint]

    def test_there_are_nine_projects_numbered_in_order(self):
        self.assertEqual(list(range(1, 10)), sorted(c.blueprint.number for c in self.pages))
        self.assertTrue(all(c.blueprint.of == 9 for c in self.pages))

    def test_a_moved_quest_keeps_the_id_of_the_page_it_was_written_on(self):
        spin = next(c for c in self.book.chapters if c.slug == 'spin')
        press = next(q for q in spin.quests if q.slug == 'press')
        self.assertEqual('road_up', press.legacy)
        self.assertEqual(object_id('quest', 'road_up.press'), emit.quest_id(spin, press))
        # and a reference from another page lands on that same id
        files = emit.all_files(self.book)
        wings = files['chapters/wings.snbt']
        self.assertIn(object_id('quest', 'road_up.press'), wings)

    def test_a_new_quest_is_named_by_its_own_page(self):
        spin = next(c for c in self.book.chapters if c.slug == 'spin')
        line = next(q for q in spin.quests if q.slug == 'line')
        self.assertIsNone(line.legacy)
        self.assertEqual(object_id('quest', 'spin.line'), emit.quest_id(spin, line))

    def test_the_finale_is_on_the_lowest_row_of_a_laid_out_page(self):
        for chapter in self.pages:
            if not chapter.blueprint.strict or chapter.slug == 'the_moon':
                continue      # the Moon has side notes under its finale
            finale = next(q for q in chapter.quests if q.slug == chapter.blueprint.finale)
            self.assertEqual(max(q.y for q in chapter.quests), finale.y, chapter.slug)

    def test_a_layout_is_the_same_every_time(self):
        import importlib
        from quests import layout
        chapter = next(c for c in self.pages if c.slug == 'crude')
        before = [(q.slug, q.x, q.y) for q in chapter.quests]
        layout.layered(chapter)
        self.assertEqual(before, [(q.slug, q.x, q.y) for q in chapter.quests])

    def test_every_project_page_passes_the_validator(self):
        self.assertEqual([], validate.check(self.book))

    def test_a_page_that_misstates_its_points_is_caught(self):
        chapter = next(c for c in self.pages if c.slug == 'brass')
        real = chapter.blueprint.points
        try:
            chapter.blueprint.points = real + 1
            problems = validate.check(self.book)
        finally:
            chapter.blueprint.points = real
        self.assertTrue(any('skill point' in p and 'brass' in p for p in problems), problems)

    def test_a_need_that_does_not_lead_to_the_finale_is_caught(self):
        chapter = next(c for c in self.pages if c.slug == 'skyline')
        stray = Quest(slug='stray', title='Stray', text=['x'], tasks=[checkmark()])
        chapter.quests.append(stray)
        try:
            problems = validate.check(self.book)
        finally:
            chapter.quests.remove(stray)
        self.assertTrue(any('stray does not lead to the finale' in p for p in problems), problems)


if __name__ == '__main__':
    unittest.main()
