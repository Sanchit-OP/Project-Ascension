"""Combat item art for ascension_combat: every item's 16 x 16 sprite, the bows' draw frames,
Veigo's Blade's inventory icon, every armour piece's worn layer, and their item models.

Run from the repository root:
    python tools/generate-combat-art.py

Writes textures/item/, textures/models/armor/ and models/item/ under the combat module's assets
(not veigos_blade_3d.json, which tools/generate-viegos-blade.mjs owns), and the previews in
design/combat-items/. Hand edits to any of those are lost on the next run: edit this file.

These are League of Legends items, drawn to look like their League counterparts (Sanchit,
2026-10-06): same silhouette, palette and signature details, in our own pixels. Every sprite is a
pixel map typed in below, one character per pixel; nothing is traced, sampled or scaled from
Riot's images. The brief is docs/technical/combat-art-reference.md; this batch is
docs/technical/combat-art.md. Needs Pillow.
"""
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..'))
ASSETS = os.path.join(ROOT, 'modules', 'combat', 'src', 'main', 'resources', 'assets', 'ascension_combat')

SPRITES = {}


def sprite(name, palette, rows, glow=''):
    assert len(rows) == 16, (name, len(rows))
    rows = [r.ljust(16, '.') for r in rows]
    for y, r in enumerate(rows):
        assert len(r) == 16, (name, y, r)
        for c in r:
            assert c == '.' or c in palette, (name, y, c)
    SPRITES[name] = (rows, palette, glow)


def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def render(name, part='all'):
    rows, pal, glow = SPRITES[name]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, c in enumerate(r):
            if c == '.':
                continue
            g = c in glow
            if part == 'all' or (part == 'base') != g:
                img.putpixel((x, y), hexc(pal[c]))
    return img


# --- Veigo's Blade: Viego's sword, spectral jade --------------------------------------------
sprite('veigos_blade', {
    'K': '#0a2824', 'd': '#1b6b58', 'm': '#2fae8a', 'l': '#69e0b8', 'w': '#d6fff1',
    'g': '#8affe4', 'G': '#3ad6b4',
}, [
    "..............KK",
    ".............KwK",
    "....K.......KwgK",
    "...KmK.....KlgK.",
    "..KmlK....KlgK..",
    "..KmlK...KlgK...",
    "..KmmK.KKlgK....",
    "...KmmKmlgK.....",
    "....KmmddKmK....",
    ".....KddK.K.....",
    "....KdmddKKKK...",
    "...KdGKKmmmmmK..",
    "..KdmK..KdddK...",
    ".KmmK....KKK....",
    "KmKmK...........",
    ".K.K............",
], glow='gG')

# --- Darken Blade: Aatrox's Darkin Blade ----------------------------------------------------
sprite('darken_blade', {
    'K': '#100a10', 'd': '#262532', 'm': '#43455a', 'l': '#6e7290', 'w': '#a9aec6',
    'R': '#e8283e', 'Y': '#ff8a6a', 'h': '#2a1a22', 'H': '#4a3038', 'b': '#5a2a30', 'B': '#7a3a40',
}, [
    ".............KK.",
    "...........KKmRK",
    ".........KKlmRdK",
    "........KllmYddK",
    ".......KKlmYdddK",
    "..K...KllmYddKK.",
    ".KhKKKKlmYdddK..",
    "..KhHllmYddKK...",
    "...KhlmRdddK....",
    "....KHRhKHKK....",
    "....KhhhHhKhK...",
    "...KbBKKKKhK....",
    "..KbBK....K.....",
    "KKbBK...........",
    "hbBK............",
    "hhK.............",
], glow='RY')

# --- Darius's Axe: the Black Cleaver --------------------------------------------------------
sprite('darius_axe', {
    'K': '#0e0d12', 'd': '#25262e', 'm': '#3c414c', 'l': '#6f7a86', 'w': '#b8c4cc',
    'b': '#4a2c1c', 'B': '#6e4630', 's': '#5a606a', 'S': '#3a3e46', 'c': '#15151b',
}, [
    ".....KdK.....K..",
    "....KlmK....KsK.",
    "..KKlmmmK..KsSK.",
    ".KdldddddKKsSdK.",
    "..KlddddddsSdmK.",
    ".KdKlddddsSdlldK",
    "..K.KlddsSdclwK.",
    ".....KKsSdmclwK.",
    ".....KsSdmmmcwdK",
    "....KBbKdmmmlwK.",
    "...KBbKKllllwK..",
    "..KBbK..KllwdK..",
    ".KBbK....KwKK...",
    "KBbK......K.....",
    "BbK.............",
    "bK..............",
])

# --- Wraithblade: Youmuu's Ghostblade -------------------------------------------------------
sprite('wraithblade', {
    'K': '#141018', 's': '#5c6070', 'm': '#9aa0b0', 'l': '#d4d8e2', 'w': '#f4f6fa',
    'o': '#8a5a1e', 'y': '#d8a040', 'Y': '#f6d880', 'p': '#3a1c4a', 'P': '#5e3274',
    'g': '#f090ff', 'G': '#b040e0', 'f': '#2a1a36',
}, [
    ".............KK.",
    "............KwlK",
    "...........KwfsK",
    "...K......KwfsK.",
    "..KYK....KwgsK..",
    "..KyyK.KKwfsK...",
    "...KyYKowlsK....",
    "....KyogoKK.....",
    ".....KoGoK......",
    ".....KoooyKK....",
    "....KpPKKKyYK...",
    "...KpPK..KyyYK..",
    "..KpPK....KKK...",
    ".KYyK...........",
    ".KyK............",
    "..K.............",
], glow='gG')

# --- Endless Hunger -------------------------------------------------------------------------
sprite('endless_hunger', {
    'K': '#120a10', 'k': '#24181e', 'y': '#d0a040', 'Y': '#f4d070', 'R': '#e0306a',
    'h': '#2a1a1a', 'H': '#4a2a2a',
}, [
    "................",
    "......KKKKKK....",
    "....KKyyyyyyKK..",
    "...KyykkkkkkyyK.",
    "..KyykkkRRRRRyyK",
    "..KykkkRKKKKKKyK",
    ".KykkkRK......K.",
    ".KykkRK.........",
    "KykkRK..........",
    "KykkRK..........",
    "KykkRK..........",
    ".KyyyK..........",
    ".KyhHyK.........",
    ".KhHRK..........",
    "KhHyyK..........",
    ".KKKyyK.........",
], glow='R')

# --- Ragefang: Guinsoo's Rageblade ----------------------------------------------------------
sprite('ragefang', {
    'K': '#2a0c06', 'y': '#ffc838', 'Y': '#fff0a0', 'o': '#f07818', 'r': '#b8300c',
    'g': '#c89030', 'G': '#f0c860', 'h': '#3a1e0e', 'H': '#5e3418',
}, [
    ".............KK.",
    ".........KKKKorK",
    "........KYyyoroK",
    ".......KKKyoroyY",
    "......KYyyoroyyK",
    ".....KKKyoroyYK.",
    "....KYyyoroyyK..",
    "...KgKyoroyYK...",
    "...KGgKKKKKK....",
    "....KKgKK.......",
    "...KhHKgGK......",
    "..KhHK.KgK......",
    ".KhHK...K.......",
    "KhHK............",
    "ggK.............",
    "gK..............",
], glow='Y')

# --- Spear of Shojin ------------------------------------------------------------------------
sprite('spear_of_shojin', {
    'K': '#0e2010', 'l': '#d8ffa0', 'm': '#7ee03a', 'd': '#2a7a20', 'w': '#f4ffe0',
    'y': '#d0a040', 'Y': '#f4d470', 'o': '#8a5a20', 'h': '#3a2a1a', 'H': '#5a4028',
}, [
    ".............Kmw",
    "............Klmd",
    "............Klmd",
    ".......K.K.KlmdK",
    "......KoKYKllmdK",
    ".......KYyymmdK.",
    "......KoyyoddK..",
    "......KhyodoK...",
    ".....KhHKKKK....",
    "....KhHK........",
    "...KyyK.........",
    "..KhHK..........",
    ".KyyK...........",
    "KhHK............",
    "hHK.............",
    "HK..............",
])

# --- Divine Sunderer ------------------------------------------------------------------------
sprite('divine_sunderer', {
    'K': '#14100a', 'y': '#e0a830', 'Y': '#ffe080', 'o': '#9a6a18', 'e': '#2ab060',
    'n': '#1a2040', 'N': '#33406e',
}, [
    "................",
    "................",
    "............KK..",
    "..........KKyYK.",
    ".........KoyeyK.",
    "........KoyyeyYK",
    ".......KoNyyeyYK",
    "......KnnyyyeyK.",
    ".....KnnyyyeyYK.",
    "....KnyeeeeyyK..",
    "...KnNYyyyyYK...",
    "..KnNKKKYYKK....",
    ".KnNK...KK......",
    "KnNK............",
    "nNK.............",
    "NK..............",
])


# --- Helmets --------------------------------------------------------------------------------
sprite('protean_mask', {
    'K': '#0c0614', 'k': '#1e1230', 'p': '#3a2458', 'P': '#5a3a86', 'l': '#8a68c0',
    'R': '#ff3a70',
}, [
    "...K...KK...K...",
    "...KK.KlPK.KK...",
    "K..KPKKlPKKPK..K",
    "KK.KpPplPPpPK.KK",
    ".KKpPPllPPPPpKK.",
    "..KpPlPPPPPPPpK.",
    ".KpPPkkPPkkPPpK.",
    ".KpPkRRkkRRkPpK.",
    ".KpPPkPRPPkPPpK.",
    ".KpRPPPPRPPPRpK.",
    ".KppRPPRPPPRppK.",
    "..KppPPPRPPppK..",
    "...KkppPRPppkK..",
    "....KkKKkKKkK...",
    ".....K..K..K....",
    "................",
], glow='R')

sprite('edge_of_night', {
    'K': '#0a0816', 'p': '#261f50', 'P': '#3e357a', 'l': '#6a5fb4', 'L': '#9a90e0',
    'd': '#06040c', 'e': '#c8b8ff',
}, [
    "................",
    ".......KK.......",
    "......KLPK......",
    ".....KLPPpK.....",
    "....KLPPPPpK....",
    "...KLPPKKPPpK...",
    "...KlPKddKPpK...",
    "..KlPKddddKPpK..",
    "..KlPKeddeKPpK..",
    "..KlPKddddKPpK..",
    ".KlPPKddddKPPpK.",
    ".KlPpKddddKpPpK.",
    "KlPPpKKddKKpPPpK",
    "KlPPppPKKPppPPpK",
    ".KKKKKK..KKKKKK.",
    "................",
], glow='e')

# --- Chestplates ----------------------------------------------------------------------------
sprite('gargoyle_stoneplate', {
    'K': '#22262c', 's': '#5e646c', 'S': '#8a9098', 'l': '#b6bcc2', 'w': '#dde1e4',
    'g': '#d88a24', 'G': '#ffd060',
}, [
    "................",
    ".KKKKK....KKKKK.",
    "KwlllSK..KSSSlsK",
    "KlllSSSKKSSSSssK",
    "KlsKlSSgSSSSKSsK",
    "KssKlSgGSSgSKssK",
    ".KKKlSSgSgGSKKK.",
    "...KlSSSgSSSK...",
    "...KlSSSSgSsK...",
    "...KlSSSSGSsK...",
    "...KsSSSSSSsK...",
    "...KsssSSsssK...",
    "...KKKKKKKKKK...",
    "................",
    "................",
    "................",
], glow='G')

sprite('guardian_wing', {
    'K': '#1a1408', 'w': '#ffffff', 'W': '#d6dce8', 'f': '#a8b0c4', 'y': '#c8902a',
    'Y': '#f4c850', 'L': '#fff0b0', 'c': '#4ae8ff', 'C': '#c8ffff',
}, [
    "K..............K",
    "wK............Kw",
    "WwK..........KwW",
    "fWwKKKK..KKKKwWf",
    "KfWwLYYKKYYywWfK",
    ".KfwYLYYYYYywfK.",
    "..KKYYyCcyYyKK..",
    "...KYyCCccyyK...",
    "...KYYyccyYyK...",
    "...KYLYyyYYyK...",
    "...KYYYYYYyyK...",
    "....KyYYYYyK....",
    "....KKyyyyKK....",
    "......KKKK......",
    "................",
    "................",
], glow='cC')

# --- Leggings -------------------------------------------------------------------------------
sprite('rocketbelt', {
    'K': '#1e1408', 'b': '#7a4e22', 'B': '#b07a34', 'y': '#e0b048', 'Y': '#fff0a0',
    'c': '#3a8aff', 'C': '#b8e8ff', 't': '#5a5a66', 'T': '#9a9aa8', 'f': '#8ad8ff', 'F': '#e8faff',
}, [
    "................",
    "KK.KKKKKKKKKKKK.",
    "TtKyYYyCcyyyyTtK",
    "TtKyyyyCcyyyyTtK",
    "ttKbbbbbbbbbbttK",
    "KKKBBbbKKBBbbKK.",
    "FFKBBbbKKBBbbFFK",
    "ffKBBbbKKBBbbffK",
    "fKKBBbbKKBBbbfK.",
    "K.KyyyyKKyyyyK..",
    "..KBBbbKKBBbbK..",
    "..KBBbbKKBBbbK..",
    "..KBBbbKKBBbbK..",
    "..KBBbbKKBBbbK..",
    "...KKKK..KKKK...",
    "................",
], glow='cCfF')

sprite('collectors_pants', {
    'K': '#12141a', 's': '#2a2e36', 'm': '#4a5260', 'l': '#7a8494', 'y': '#d0a040', 'Y': '#f6dc8a',
}, [
    "................",
    "...KKKKKKKKKK...",
    "..KyYYyyyyyyyK..",
    "..KssssyyssssK..",
    "..KssssssssssK..",
    "..KlmssKKlmssK..",
    "..KlyssKKlyssK..",
    "..KmmysKKmmysK..",
    "..KmyssKKmyssK..",
    "..KmmssKKmmssK..",
    "..KmmssKKmmssK..",
    "..KmmysKKmmysK..",
    "..KmyssKKmyssK..",
    "..KmmssKKmmssK..",
    "...KKKK..KKKK...",
    "................",
])

sprite('unending_despair', {
    'K': '#060a0a', 'k': '#101a18', 'v': '#22342e', 'g': '#3a5a4c', 'c': '#30e8d8', 'C': '#c0fff8',
}, [
    "................",
    "...KKKKKKKKKK...",
    "..KggggCcggggK..",
    ".KKvvvcCccvvvKK.",
    "KvKvvvvvvvvvvKvK",
    ".KvvvkkKKvvkkvK.",
    "..KvckkKKvvckK..",
    "..KvvkkKKvvkkK..",
    "..KvvckKKvckkK..",
    ".KvvvkkKKvvkkvK.",
    "..KvckkKKvvckK..",
    "..KvvkkKKvvkkK..",
    "..KvvkkKKvvkkK..",
    "..KvvkkKKvvkkK..",
    "...KKKK..KKKK...",
    "................",
], glow='cC')

# --- Boots ----------------------------------------------------------------------------------
sprite('mercury_treads', {
    'K': '#1c120c', 'b': '#4a3226', 'B': '#6e4e3a', 'L': '#94704e', 's': '#6a7480', 'S': '#a8b2bc',
    'w': '#dfe5ea', 'y': '#c89430', 'Y': '#f0cc60', 'c': '#2a46b8', 'C': '#7aa0ff',
}, [
    "................",
    "...KKKKKKK......",
    "...KwSSSSsK.....",
    "...KLBBBBbK.....",
    "...KLBBBBbK.....",
    "...KLBYYYbK.....",
    "...KLYCcYbK.....",
    "...KLYccYbK.....",
    "...KLBYYBbbK....",
    "...KLBBBBBBbbK..",
    "...KLBBBBBBBSwSK",
    "...KbBBBBBBBSSsK",
    "...KSSSSSSSSSSsK",
    "...KKKKKKKKKKKK.",
    "................",
    "................",
])

sprite('ninja_tabi', {
    'K': '#141614', 's': '#5e6268', 'S': '#9a9ea4', 'w': '#dcdfe2', 'o': '#3a4826', 'O': '#5a6e38',
    'L': '#7a8e52', 'g': '#3a3c40', 'G': '#6a6e74',
}, [
    "................",
    "........KKKKKK..",
    ".......KwSSSSsK.",
    ".......KSwSSssK.",
    "......KKKKKKKKK.",
    ".......KOLOOoK..",
    "...K...KOLOOoK..",
    "..KwK.KSwSSSsK..",
    ".KwSSKSwSSSSsK..",
    "KwSSSSSSSSSssK..",
    ".KSSOLOOOOOOoK..",
    ".KGGGGGGGGGGGK..",
    "..KKKKKKKKKKK...",
    "................",
    "................",
    "................",
])

sprite('swifty', {
    'K': '#2a2420', 'c': '#e8e0d0', 'C': '#c8bca8', 'd': '#9a8a74', 'f': '#ffffff', 'F': '#dce4ee',
    'b': '#8a9cb4', 'n': '#5a3a28', 'e': '#f4f8ff', 'E': '#b8c8dc',
}, [
    "................",
    "......KKKKKKK...",
    ".....KffFfFfFK..",
    "....KfFfffFfFKK.",
    ".....KKcCCCdKeEK",
    "......KcCCCdKeEK",
    "......KcCCCdKEeK",
    "......KcCCCdKKK.",
    ".....KccCCCdK...",
    "..KKKccCCCCdK...",
    ".KcccCCCCCCdK...",
    "KccCCCCCCCddK...",
    "KnnnnnnnnnnnK...",
    ".KKKKKKKKKKK....",
    "................",
    "................",
])

sprite('berserkers_greaves', {
    'K': '#141210', 's': '#5a626c', 'S': '#9aa4ae', 'w': '#e2e8ee', 'b': '#4a2e1c', 'B': '#7a5032',
    'L': '#a0704a', 'y': '#ffe040', 'Y': '#fffbc0',
}, [
    ".y..............",
    "..yKKKKKKK......",
    "..KwSSSSSsK.....",
    "..KSwSSSssK..y..",
    "..KSwSSSssK.y...",
    ".yKKKKKKKKK..Y..",
    "..KSwSSSssK.....",
    "..KSSSSsssK.....",
    "..KLBBBBBbbKK...",
    "..KLBBBBBBBbbK..",
    "..KLBBBBBBBBbbK.",
    "..KbBBBBBBBBBbK.",
    "..KbbbbbbbbbbbK.",
    "...KKKKKKKKKKK..",
    ".Y...........y..",
    "................",
], glow='yY')

sprite('wizards_agony', {
    'K': '#0c0a18', 'n': '#1c2034', 'N': '#343a5a', 'l': '#5a6488', 'y': '#b88a2a', 'Y': '#f0c860',
    's': '#a8aec0', 'o': '#141418', 'O': '#5a5a6a', 'p': '#b040ff', 'P': '#e0a0ff',
}, [
    "................",
    ".......KKKKKK...",
    "......KYYyyyyK..",
    "......KssNNNnK..",
    "......KsNNNNnp..",
    "......KYyyyyyK..",
    "......KlNNNNnp..",
    ".....KlNNNNNnK..",
    ".s.KKlNNNNNNnp..",
    "..KoOKNNNNNNnK..",
    ".KoOooNNNNNNnK..",
    "sKooooNNNNNnnK..",
    "..KoKYyyyyyyyK..",
    ".s.s.KKKKKKKKp..",
    "................",
    "................",
], glow='pP')

sprite('tasted_blood', {
    'K': '#2a0810', 'w': '#f6ece8', 'W': '#e2c6c6', 'g': '#9a6a70', 'r': '#c0203a', 'R': '#ff4a60',
    'd': '#5a0c1a', 't': '#ffffff',
}, [
    "...K..K..K......",
    "..KwKKwKKwK.....",
    "..KwwwwwwwWK....",
    "..KwwwwWWWWK....",
    "..KwwwWWWrWK....",
    "..KwwWWWrrWK....",
    "..KwwWWrrWWK....",
    "..KwwWWrWWWWKK..",
    "..KwwWrWWWWWWWWK",
    "..KwwWWWWWWtdtdK",
    "..KwwWWWWWWdRddK",
    "..KwwWWWWWWdtdtK",
    "..KggggggggggggK",
    "...KKKKKKKKKKKK.",
    "................",
    "................",
], glow='R')

# --- Off-hand -------------------------------------------------------------------------------
sprite('zonias_hourglass', {
    'K': '#1e1408', 'o': '#8a5a14', 'y': '#d8a030', 'Y': '#ffe080', 'g': '#9ec8e8', 'G': '#e0f4ff',
    's': '#e09030', 'S': '#ffc860',
}, [
    "..KKKKKKKKKKKK..",
    "..KYYYyyyyyyoK..",
    "..KooyyyyyyooK..",
    "..KyKGGggggKyK..",
    "..KyKGsSSssKyK..",
    "..KyyKsSssKyyK..",
    "..KyyyKSsKyyyK..",
    "..KyyyKSgKyyyK..",
    "..KyyKgSggKyyK..",
    "..KyKGgSgggKyK..",
    "..KyKggsSggKyK..",
    "..KyKsSSSssKyK..",
    "..KooyyyyyyooK..",
    "..KyyyyyyyyooK..",
    "..KKKKKKKKKKKK..",
    "................",
], glow='sS')

sprite('steraks_harness', {
    'K': '#1a0a12', 'd': '#3a1626', 'm': '#5e2a3e', 'l': '#8a4a62', 'L': '#b87890', 'c': '#e8d0d8',
    'C': '#b8a0a8', 'r': '#a01830',
}, [
    "....K..K..K.....",
    "...KcKKcKKcK....",
    "...KcCKcCKcCK...",
    "..KLlmLlmLlmmK..",
    "..KlmmdlmdlmdK..",
    ".KKlLmmmLmmmmdK.",
    "KcKlmmdmmmdmmdK.",
    "KlKlLmmmmmmmdK..",
    "KlmKlmmmmmmdK...",
    ".KlmKmmmmmmdK...",
    "..KKdmmmmmddK...",
    "...KrrrrrrrrK...",
    "...KdmmmmmmdK...",
    "...KdmmmmmmdK...",
    "...KKKKKKKKKK...",
    "................",
])

# --- Bows: body grids; the string and arrow are drawn per frame by bow_frame() ---------------
BOWS = {}


def bow(name, palette, rows, string, arrow, glow=''):
    sprite(name, palette, rows, glow)
    BOWS[name] = (string, arrow)


bow('mortal_reminder', {
    'K': '#16141c', 's': '#5a6070', 'm': '#9aa2b2', 'l': '#e2e6ee', 'y': '#d8a83c',
    'p': '#4e4060', 'P': '#7a6a90',
}, [
    ".............K..",
    ".........KKKKlKK",
    "......K.Klysssss",
    ".....KlKssymmmKK",
    ".....KppmmmKKK..",
    "...KKpPmKKK.....",
    "..KlppmK........",
    "...KsmK.........",
    "..KsmK..........",
    ".KlsmK..........",
    ".KyymK..........",
    ".KsmK...........",
    ".KsmK...........",
    "KlsmK...........",
    ".KsK............",
    ".KsK............",
], string='#c8ccd4', arrow=('#5a4a3a', '#eef2f8', '#7a6a90'))

bow('heartwood_of_pallas', {
    'K': '#0e0812', 'v': '#2a1838', 'V': '#4a2a6a', 'M': '#e050b0', 'L': '#b080f0',
    'W': '#f0e0ff',
}, [
    "..K...........Kv",
    ".KvK.....KKKKKvK",
    "KvKvK..KKvvvMvvv",
    ".KvvKKKvvMvVVVKK",
    "..KKvvLvVVVVVVK.",
    "...KvWLVVKKKKK..",
    "...KLLVKK.......",
    "..KvvVK.........",
    "..KvVVK.........",
    ".KvMVK..........",
    ".KvvVK..........",
    ".KvVVK..........",
    ".KMVVK..........",
    ".KvVVK..........",
    "KvvKK...........",
    "vKvK............",
], string='#c890e8', arrow=('#5a1830', '#ff4a8a', '#e050b0'), glow='MLW')

bow('galeforce', {
    'K': '#162030', 'w': '#f0f4fa', 'l': '#b4c0d0', 'c': '#5ae0f0', 'C': '#b8fbff',
}, [
    "..........KKwK..",
    ".........KwwKKKK",
    "........KwKKwwww",
    ".....KKKwwwwllKK",
    "....KCcwlllKKK..",
    "...KCcwlKKK.....",
    "...KcwlK........",
    "...KwlK.........",
    "..KwlK..........",
    ".KwwlK..........",
    "KwKwlK..........",
    "KwKwK...........",
    "wKwlK...........",
    "KKwlK...........",
    ".KwK............",
    ".KwK............",
], string='#d8f4ff', arrow=('#5a6a80', '#ffffff', '#5ae0f0'), glow='cC')


def bow_frame(name, stage):
    """The body plus its string, and for stage 0..2 a drawn arrow (vanilla bow's layout)."""
    rows, pal, glow = SPRITES[name]
    string, (shaft, head, fletch) = BOWS[name]
    g = [list(r) for r in rows]
    pal = dict(pal, **{'~': string, '=': shaft, '>': head, '^': fletch})
    a, b = (13, 3), (3, 13)

    def line(p, q, ch):
        n = max(abs(q[0] - p[0]), abs(q[1] - p[1]))
        for i in range(n + 1):
            x = round(p[0] + (q[0] - p[0]) * i / n)
            y = round(p[1] + (q[1] - p[1]) * i / n)
            g[y][x] = ch
    if stage is None:
        line(a, b, '~')
    else:
        n = 10 + stage
        line(a, (n, n), '~')
        line(b, (n, n), '~')
        for i in range(1, 9):
            g[n - i][n - i] = '='
        h = n - 9
        g[h][h] = '>'
        g[h][h + 1] = '>'
        g[h + 1][h] = '>'
        g[n][n - 1] = '^'
        g[n - 1][n] = '^'
    key = '%s_%s' % (name, 'idle' if stage is None else stage)
    SPRITES[key] = ([''.join(r) for r in g], pal, glow)
    return key


# --- Worn armour: 64 x 32 armour-layer textures ---------------------------------------------
# Each armour item has its own material (CombatArmorMaterials), so each paints only the boxes its
# slot renders: a helmet the head (and the hat overlay, which floats half a pixel out), a
# chestplate the body and arms, leggings the body's waist and legs (layer 2), boots the bottom of
# the legs. Faces are named as in the humanoid box layout; x runs right/front/left/back around a
# box, y runs down from the top of the box.
HEAD, HAT = (0, 0, 8, 8, 8), (32, 0, 8, 8, 8)
BODY, ARM, LEG = (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (0, 16, 4, 12, 4)
ARMOUR = {}


def box_faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h),
            'back': (u + d + w + d, v + d, w, h)}


def paint(img, box, fn, rows=None):
    """fn(face, x, y, w, h) -> '#rrggbb' or None. rows=(first, last) limits the side faces, and
    drops the top or bottom face when the rows don't reach it."""
    u, v, w, h, d = box
    for face, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
        if rows and ((face == 'top' and rows[0] > 0) or (face == 'bottom' and rows[1] < h - 1)):
            continue
        for y in range(fh):
            if rows and face not in ('top', 'bottom') and not rows[0] <= y <= rows[1]:
                continue
            for x in range(fw):
                c = fn(face, x, y, fw, fh)
                if c:
                    img.putpixel((fx + x, fy + y), hexc(c))


def armour(name, layer):
    def reg(fn):
        ARMOUR[name] = (layer, fn)
        return fn
    return reg


def side(face):
    return face in ('right', 'front', 'left', 'back')


@armour('protean_mask', 1)
def _(img):
    k, p, P, l, R = '#1e1230', '#3a2458', '#5a3a86', '#8a68c0', '#ff3a70'

    def head(f, x, y, w, h):
        if f == 'front':
            if y == 3:
                return R if x in (1, 2, 5, 6) else k
            if y in (2, 4):
                return k if y == 2 and x in (1, 2, 5, 6) else P
            if (x, y) in ((3, 5), (4, 6), (3, 7), (1, 6), (6, 6)):
                return R
            return l if y == 0 and x in (2, 5) else P if y < 6 else p
        if f == 'top':
            return l if x in (3, 4) else P
        return k if (x + y) % 4 == 0 and y > 1 else p if y >= h - 2 else P
    paint(img, HEAD, head)

    def crest(f, x, y, w, h):
        if f == 'front' and y == 0 and x in (1, 3, 4, 6):
            return l if x in (3, 4) else P
        if f in ('right', 'left') and y == 0 and x in (2, 5):
            return P
        return None
    paint(img, HAT, crest)


@armour('edge_of_night', 1)
def _(img):
    d, p, P, l, L, e = '#06040c', '#261f50', '#3e357a', '#6a5fb4', '#9a90e0', '#c8b8ff'

    def hood(f, x, y, w, h):
        if f == 'front':
            if y >= 2 and 1 <= x <= 6:
                return e if y == 4 and x in (2, 5) else d
            return L if y == 0 else l if x in (0, 7) else P
        if f == 'top':
            return L if y == w - 1 else P
        return p if x % 3 == 1 else P
    paint(img, HEAD, hood)

    def peak(f, x, y, w, h):
        return P if (f == 'top' and y < 3 and 3 <= x <= 4) or (f == 'back' and y == 0 and 2 <= x <= 5) else None
    paint(img, HAT, peak)


@armour('gargoyle_stoneplate', 1)
def _(img):
    s, S, l, w, g, G = '#5e646c', '#8a9098', '#b6bcc2', '#dde1e4', '#d88a24', '#ffd060'
    cracks = {(1, 2), (2, 3), (3, 4), (4, 4), (5, 5), (6, 3), (6, 4), (5, 7), (4, 8), (4, 9)}

    def body(f, x, y, wd, h):
        if f == 'front' and (x, y) in cracks:
            return G if (x, y) in ((3, 4), (5, 5)) else g
        if f == 'back' and (x, y) in ((2, 3), (3, 4), (3, 5), (5, 6), (4, 7)):
            return g
        return w if y == 0 else l if y < 3 else s if y >= h - 1 else S
    paint(img, BODY, body)

    def pauldron(f, x, y, wd, h):
        if f == 'top':
            return w
        if y > 5:
            return None
        return s if y == 5 else l if y < 2 else S
    paint(img, ARM, pauldron)


@armour('guardian_wing', 1)
def _(img):
    y_, Y, L, c, C, wt, W, fe = '#c8902a', '#f4c850', '#fff0b0', '#4ae8ff', '#c8ffff', '#ffffff', '#d6dce8', '#a8b0c4'

    def body(f, x, y, w, h):
        if f == 'front':
            if (x, y) in ((3, 3), (4, 3), (3, 4), (4, 4), (3, 5), (4, 5)):
                return C if (x, y) == (3, 3) else c
            if 2 <= x <= 5 and 2 <= y <= 6:
                return Y
            return L if y == 0 else y_ if y >= h - 2 else Y
        if f == 'back':
            # two folded wings
            wing = (x in (1, 6) and 1 <= y <= 9) or (x in (2, 5) and 0 <= y <= 7) or (x in (0, 7) and 3 <= y <= 10)
            return (W if y > 6 else wt) if wing else y_
        return Y if y < h - 2 else y_
    paint(img, BODY, body)

    def feathers(f, x, y, w, h):
        if f == 'top':
            return wt
        if y > 4:
            return None
        return fe if y == 4 else W if y == 3 else wt
    paint(img, ARM, feathers)


@armour('rocketbelt', 2)
def _(img):
    b, B, yy, Y, c, C, t, fl = '#7a4e22', '#b07a34', '#e0b048', '#fff0a0', '#3a8aff', '#b8e8ff', '#5a5a66', '#8ad8ff'

    def belt(f, x, y, w, h):
        if f == 'front' and x in (3, 4) and y in (8, 9):
            return C if (x, y) == (3, 8) else c
        return Y if y == 7 else yy if y < 10 else b
    paint(img, BODY, belt, rows=(7, 11))

    def leg(f, x, y, w, h):
        if f == 'right' and 1 <= y <= 4 and x in (1, 2):
            return t
        if f == 'right' and y in (5, 6) and x in (1, 2):
            return fl
        return yy if y == 4 else B if x < w - 1 else b
    paint(img, LEG, leg, rows=(0, 9))


@armour('collectors_pants', 2)
def _(img):
    s, m, l, yy, Y = '#2a2e36', '#4a5260', '#7a8494', '#d0a040', '#f6dc8a'

    def belt(f, x, y, w, h):
        if f == 'front' and x in (3, 4) and y in (8, 9):
            return Y if y == 8 else yy
        return yy if y == 8 else s if y > 9 else m
    paint(img, BODY, belt, rows=(7, 11))

    def leg(f, x, y, w, h):
        if f in ('front', 'right') and (x, y) in ((1, 2), (2, 3), (1, 4), (2, 6), (1, 7)):
            return yy
        return l if x == 0 and f == 'front' else m if y < 8 else s
    paint(img, LEG, leg, rows=(0, 9))


@armour('unending_despair', 2)
def _(img):
    k, v, g, c, C = '#101a18', '#22342e', '#3a5a4c', '#30e8d8', '#c0fff8'

    def belt(f, x, y, w, h):
        if f == 'front' and x in (3, 4) and y in (8, 9):
            return C if (x, y) == (3, 8) else c
        return g if y == 7 else v if y < 10 else k
    paint(img, BODY, belt, rows=(7, 11))

    def leg(f, x, y, w, h):
        if side(f) and (x + 2 * y) % 7 == 0:
            return c
        return g if y == 0 else v if (x + y) % 3 else k
    paint(img, LEG, leg, rows=(0, 9))


def boot(img, fn):
    paint(img, LEG, fn, rows=(6, 11))


@armour('mercury_treads', 1)
def _(img):
    b, B, L, s, S, Y, cc = '#4a3226', '#6e4e3a', '#94704e', '#6a7480', '#a8b2bc', '#f0cc60', '#2a46b8'

    def fn(f, x, y, w, h):
        if f == 'bottom':
            return s
        if y == 6:
            return S
        if f == 'front' and y >= 9:
            return S
        if f == 'right' and y in (7, 8) and x in (1, 2):
            return cc if (x, y) == (2, 8) else Y
        return L if x == 0 and f == 'front' else B if y < 11 else b
    boot(img, fn)


@armour('ninja_tabi', 1)
def _(img):
    S, w, O, o, G = '#9a9ea4', '#dcdfe2', '#5a6e38', '#3a4826', '#6a6e74'

    def fn(f, x, y, wd, h):
        if f == 'bottom':
            return G
        if y in (6, 7):
            return w if y == 6 and x == 0 else S
        if y in (8, 9):
            return O if y == 8 else o
        if f == 'front' and y == 10:
            return w if x % 2 == 0 else S
        return S if y == 10 else G
    boot(img, fn)


@armour('swifty', 1)
def _(img):
    c, C, f_, F, n, e = '#e8e0d0', '#c8bca8', '#ffffff', '#dce4ee', '#5a3a28', '#b8c8dc'

    def fn(f, x, y, w, h):
        if f == 'bottom':
            return n
        if y == 6:
            return f_ if (x + y) % 2 else F
        if f == 'right' and y in (7, 8) and x in (0, 1):
            return e if y == 8 else f_
        return n if y == 11 else c if y < 9 else C
    boot(img, fn)


@armour('berserkers_greaves', 1)
def _(img):
    S, w, s, B, b = '#9aa4ae', '#e2e8ee', '#5a626c', '#7a5032', '#4a2e1c'

    def fn(f, x, y, wd, h):
        if f == 'bottom':
            return b
        if y <= 8:
            return w if y == 6 else s if y == 8 else S
        return b if y == 11 else B
    boot(img, fn)


@armour('wizards_agony', 1)
def _(img):
    n, N, yy, s, o = '#1c2034', '#343a5a', '#b88a2a', '#a8aec0', '#141418'

    def fn(f, x, y, w, h):
        if f == 'bottom':
            return o
        if y in (6, 11):
            return yy
        if f == 'front' and y in (7, 8) and x in (1, 2):
            return s
        return N if y < 9 else n
    boot(img, fn)


@armour('tasted_blood', 1)
def _(img):
    wt, W, g, r, d, t = '#f6ece8', '#dcc8c4', '#b09494', '#c0203a', '#5a0c1a', '#ffffff'

    def fn(f, x, y, w, h):
        if f == 'bottom':
            return g
        if f == 'front' and y >= 9:
            return (t if x % 2 == 0 else d) if y in (9, 11) else d
        if f in ('right', 'left') and x + 6 == y + (1 if f == 'left' else 0):
            return r
        return wt if y < 8 else W
    boot(img, fn)


def armour_layer(name):
    layer, fn = ARMOUR[name]
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    fn(img)
    return layer, img


# --- Output ---------------------------------------------------------------------------------
SWORDS = ['darken_blade', 'darius_axe', 'endless_hunger', 'ragefang', 'spear_of_shojin', 'wraithblade',
          'divine_sunderer']
FLAT = ['protean_mask', 'edge_of_night', 'gargoyle_stoneplate', 'guardian_wing', 'rocketbelt',
        'collectors_pants', 'unending_despair', 'mercury_treads', 'ninja_tabi', 'swifty',
        'berserkers_greaves', 'wizards_agony', 'tasted_blood', 'zonias_hourglass', 'steraks_harness']
GLOW = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}
BOW_DISPLAY = {
    'thirdperson_righthand': {'rotation': [-80, 260, -40], 'translation': [-1, -2, 2.5]},
    'thirdperson_lefthand': {'rotation': [-80, -280, 40], 'translation': [-1, -2, 2.5]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13]},
    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13]},
}


# Vanilla bakes anything whose parents end at item/generated with its own generator, which
# ignores custom loaders: a glowing model must parent NeoForge's copies of those transforms.
NEOFORGE_PARENT = {'minecraft:item/generated': 'neoforge:item/default',
                   'minecraft:item/handheld': 'neoforge:item/default-tool'}


def item_model(parent, tex, has_glow, **extra):
    m = {'parent': NEOFORGE_PARENT[parent] if has_glow else parent}
    if has_glow:
        m['loader'] = 'neoforge:item_layers'
    m['textures'] = {'layer0': 'ascension_combat:item/' + tex}
    if has_glow:
        m['textures']['layer1'] = 'ascension_combat:item/%s_glow' % tex
        m['neoforge_data'] = {'layers': {'1': GLOW}}
    m.update(extra)
    return m


def build_all():
    """Every 16 x 16 sprite, base and glow composited: what the inventory shows. For previews."""
    for b in BOWS:
        for st in (None, 0, 1, 2):
            bow_frame(b, st)
    return {n: render(n) for n in SPRITES}


def write():
    import json
    tex_dir = os.path.join(ASSETS, 'textures', 'item')
    model_dir = os.path.join(ASSETS, 'models', 'item')
    armour_dir = os.path.join(ASSETS, 'textures', 'models', 'armor')
    for d in (tex_dir, model_dir, armour_dir):
        os.makedirs(d, exist_ok=True)

    def save_sprite(key, tex):
        render(key, 'base').save(os.path.join(tex_dir, tex + '.png'))
        if SPRITES[key][2]:
            render(key, 'glow').save(os.path.join(tex_dir, tex + '_glow.png'))
        return bool(SPRITES[key][2])

    def save_model(name, m):
        with open(os.path.join(model_dir, name + '.json'), 'w', newline='\n') as f:
            f.write(json.dumps(m, indent=2) + '\n')

    for n in SWORDS:
        save_model(n, item_model('minecraft:item/handheld', n, save_sprite(n, n)))
    for n in FLAT:
        save_model(n, item_model('minecraft:item/generated', n, save_sprite(n, n)))
    for n in BOWS:
        glow = save_sprite(bow_frame(n, None), n)
        overrides = []
        for st, pull in ((0, None), (1, 0.65), (2, 0.9)):
            tex = '%s_pulling_%d' % (n, st)
            save_sprite(bow_frame(n, st), tex)
            m = {'parent': 'ascension_combat:item/' + n,
                 'textures': item_model('minecraft:item/generated', tex, glow)['textures']}
            save_model(tex, m)
            pred = {'pulling': 1} if pull is None else {'pulling': 1, 'pull': pull}
            overrides.append({'predicate': pred, 'model': 'ascension_combat:item/' + tex})
        save_model(n, item_model('minecraft:item/generated', n, glow, display=BOW_DISPLAY,
                                 overrides=overrides))
    # Veigo's Blade: the 3D model stays in hand; the flat icon is used in the inventory, on the
    # ground and in item frames, where the 3D one read as a thin line.
    save_model('veigos_blade_icon', item_model('minecraft:item/handheld', 'veigos_blade_icon',
                                               save_sprite('veigos_blade', 'veigos_blade_icon')))
    icon = {'parent': 'ascension_combat:item/veigos_blade_icon'}
    save_model('veigos_blade', {
        'loader': 'neoforge:separate_transforms', 'gui_light': 'front',
        'base': {'parent': 'ascension_combat:item/veigos_blade_3d'},
        'perspectives': {'gui': icon, 'ground': icon, 'fixed': icon},
    })
    for n in ARMOUR:
        layer, img = armour_layer(n)
        img.save(os.path.join(armour_dir, '%s_layer_%d.png' % (n, layer)))


ORDER = (['veigos_blade'] + SWORDS + list(BOWS) + FLAT)
SLOT_PARTS = {'helmet': ['head', 'hat'], 'chest': ['body', 'arms'], 'legs': ['body', 'legs'], 'feet': ['legs']}
SLOT = dict([(n, 'helmet') for n in FLAT[:2]] + [(n, 'chest') for n in FLAT[2:4]] +
            [(n, 'legs') for n in FLAT[4:7]] + [(n, 'feet') for n in FLAT[7:13]])


def figure(layers):
    """A blocky figure, front / back / right side, each layer's box faces pasted flat (armour
    inflation ignored). layers: [(64 x 32 texture, parts)]."""
    out = Image.new('RGBA', (56, 32), (0, 0, 0, 0))

    def put(tex, box, fname, x, y, flip=False):
        fx, fy, fw, fh = box_faces(*box)[fname]
        im = tex.crop((fx, fy, fx + fw, fy + fh))
        out.alpha_composite(im.transpose(Image.FLIP_LEFT_RIGHT) if flip else im, (x, y))
    for tex, parts in layers:
        for f, ox in (('front', 0), ('back', 20)):
            for p, box, xy in (('head', HEAD, (4, 0)), ('hat', HAT, (4, 0)), ('body', BODY, (4, 8))):
                if p in parts:
                    put(tex, box, f, ox + xy[0], xy[1])
            near, far = (0, 12) if f == 'front' else (12, 0)
            if 'arms' in parts:
                put(tex, ARM, f, ox + near, 8)
                put(tex, ARM, f, ox + far, 8, True)
            if 'legs' in parts:
                put(tex, LEG, f, ox + (4 if f == 'front' else 8), 20)
                put(tex, LEG, f, ox + (8 if f == 'front' else 4), 20, True)
        for p, box, xy in (('head', HEAD, (0, 0)), ('hat', HAT, (0, 0)), ('body', BODY, (2, 8)),
                           ('legs', LEG, (2, 20)), ('arms', ARM, (2, 8))):
            if p in parts:
                put(tex, box, 'right', 40 + xy[0], xy[1])
    return out


def previews():
    """design/combat-items/: every sprite (and the bows' draw frames) on an inventory-grey
    background, and each armour piece worn by a plain figure. Our art only; League references
    stay in design/reference/, which git ignores."""
    from PIL import ImageDraw
    out_dir = os.path.join(ROOT, 'design', 'combat-items')
    os.makedirs(out_dir, exist_ok=True)
    sp = build_all()
    s, cols = 4, 9
    cell = 16 * s + 10
    rows = (len(ORDER) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * cell + 8, rows * (cell + 12) + 16 * s + 30), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    for i, n in enumerate(ORDER):
        x, y = 8 + (i % cols) * cell, 8 + (i // cols) * (cell + 12)
        d.rectangle([x - 2, y - 2, x + 16 * s + 1, y + 16 * s + 1], fill=(139, 139, 139, 255))
        sheet.alpha_composite(sp[n].resize((16 * s, 16 * s), Image.NEAREST), (x, y))
        d.text((x, y + 16 * s + 2), n[:12], fill=(40, 40, 40, 255))
    y = 8 + rows * (cell + 12)
    for i, (b, st) in enumerate((b, st) for b in BOWS for st in ('idle', 0, 1, 2)):
        sheet.alpha_composite(sp['%s_%s' % (b, st)].resize((32, 32), Image.NEAREST), (8 + i * 38, y))
    sheet.save(os.path.join(out_dir, 'icons.png'))

    skin = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    paint(skin, HEAD, lambda f, x, y, w, h: '#2a2a3a' if f == 'front' and y == 4 and x in (2, 5) else '#c89a78')
    paint(skin, BODY, lambda f, x, y, w, h: '#3a6ea8')
    paint(skin, ARM, lambda f, x, y, w, h: '#3a6ea8' if y < 8 else '#c89a78')
    paint(skin, LEG, lambda f, x, y, w, h: '#2a3a6a')
    s, cols = 4, 3
    cw, ch = 56 * s + 80, 32 * s + 24
    worn = Image.new('RGBA', (cols * cw, ((len(ARMOUR) + cols - 1) // cols) * ch), (44, 48, 56, 255))
    d = ImageDraw.Draw(worn)
    for i, n in enumerate(ARMOUR):
        x, y = (i % cols) * cw, (i // cols) * ch
        fig = figure([(skin, ['head', 'body', 'arms', 'legs']), (armour_layer(n)[1], SLOT_PARTS[SLOT[n]])])
        worn.alpha_composite(sp[n].resize((48, 48), Image.NEAREST), (x + 8, y + 22))
        worn.alpha_composite(fig.resize((56 * s, 32 * s), Image.NEAREST), (x + 66, y + 18))
        d.text((x + 8, y + 4), n, fill=(230, 230, 230, 255))
    worn.save(os.path.join(out_dir, 'worn.png'))


if __name__ == '__main__':
    write()
    previews()
    print('wrote combat art and design/combat-items/ previews')
