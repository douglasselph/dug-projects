#!/usr/bin/env python3
"""Build a fresh Leaf & Let Die TTS save from prepared assets.

Milestone 7: all 266 cards, 69 native dice, shared tokens with explicit
front/back artwork, Butterflies, Main Action tokens, and player components
(Plant Cores, Player Dice Supplies, Dice Discard Bins, and Gnomes) inside
 the Game Box, plus the Battle Glade, four double-sided helper cards,
 a wide Grove Setup reference, and a single
18-card Round Card stack. Dice and tokens use temporary bags until physical
trays and player boxes are implemented.

Local preview requires Steam/Proton to reach Windows Z:/home/... file paths.
Hosted mode requires card URLs in the manifest and non-card URLs in
config/asset_urls.json (fallback: config/token_urls.json).
"""
import argparse
import copy
import json
from pathlib import Path
import re
import sys

CATEGORIES = ('root', 'vine', 'flower', 'cultivation', 'battle', 'wisp')
EXPECTED = {
    'root': (12, 72), 'vine': (12, 72), 'flower': (12, 72),
    'cultivation': (6, 12), 'battle': (6, 6), 'wisp': (11, 32)
}

# Current v37 dice counts. Graft Bed D4 compartment starts empty.
GRAFT_BED = {6: 9, 8: 9, 10: 9, 12: 9, 20: 9}
PLAYER_START = {4: 3, 6: 3}
PLAYER_COLORS = ('Blue', 'Green', 'Red', 'Yellow')

# v37.01 rulebook, Components (page 3). The 18 Critters are provisionally
# divided equally between Bees and Worms; adjust here if needed.
TOKEN_COUNTS = {
    'Bee': 9, 'Worm': 9, 'Water': 9, 'Mulch': 9, 'Sunlight': 9,
    'Victory': 25, 'Victory-5': 8,
}
BUTTERFLY_COLORS = ('Green', 'Purple', 'Red', 'Yellow')

# Manufacturing Outline v8.2, pp. 1-3. Dimensions are finished sizes
# except the *unassembled* supply/discard dielines; the current TTS placards
# represent those flat cutouts, not assembled 3-D boxes.
#
# TTS calibration: native TTS playing card measures approximately 3.06 game
# units along its long side, corresponding to a 3.5-inch Plant Card.
# A Custom_Tile at scale 1 is approximately 2 game units wide/high.
# These references supply *consistent relative sizes*. For sub-millimeter
# accuracy, calibrate CARD_LONG_UNITS and TILE_BASE_UNITS from in-game bounds.
CARD_LONG_UNITS = 3.06
# Screenshot D6 (~330 px) vs 1 3/16-inch Main Action token (~470 px).
# Expected width ratio: 1.1875 / 0.5 = 2.375; observed ~470/330.
# Increase flat pieces and cards without modifying native dice.
DICE_REFERENCE_CORRECTION = 1.67
BATTLE_GLADE_PLAY_MULTIPLIER = 1.5
TILE_BASE_UNITS = 2.0
PLANT_CARD_HEIGHT_IN = 3.5
UNITS_PER_INCH = CARD_LONG_UNITS / PLANT_CARD_HEIGHT_IN * DICE_REFERENCE_CORRECTION

CARD_SIZES_IN = {
    'root': (2.5, 3.5), 'vine': (2.5, 3.5), 'flower': (2.5, 3.5),
    'cultivation': (4.0, 3.0), 'battle': (4.0, 3.0),
    'wisp': (2.0, 2.0),
}
TILE_SIZES_IN = {
    'Bee': (1.25, 1.25), 'Worm': (1.25, 1.25),
    'Water': (1.5, 1.0), 'Mulch': (1.5, 1.0),
    'Sunlight': (1.1875, 1.1875), 'Main Action': (1.1875, 1.1875),
    'Victory': (1.5, 1.5), 'Victory-5': (1.5, 1.5),
    'Butterfly': (2.0, 2.0),
    'Plant Core': (2.75, 4.0),
    'Player Dice Supply': (3.4, 4.68), # finished interior, as flat play-area proxy
    'Dice Discard Bin': (3.25, 5.0), # user's corrected tabletop dimensions
    'Gnome': (1.0, 2.5), # user's corrected temporary flat silhouette
    'Battle Glade': (6.5, 8.375),
    'Grove Setup Reference': (8.25, 8.25 * 10.65 / 24.56), # artwork aspect ratio
}
HELPER_SIZE_IN = (4.0, 5.0)


def tile_scales(width_in, height_in, image_aspect):
    """TTS Custom_Tile bases its X footprint on the art's aspect ratio.

    Correct X for that intrinsic aspect ratio. The previous code applied
    width/height *and* the image aspect, unintentionally narrowing portraits.
    """
    if image_aspect <= 0:
        raise ValueError('Invalid image aspect ratio')
    return (round(width_in * UNITS_PER_INCH / TILE_BASE_UNITS / image_aspect, 6),
            round(height_in * UNITS_PER_INCH / TILE_BASE_UNITS, 6))


def card_scale(height_in):
    return round(height_in / PLANT_CARD_HEIGHT_IN * DICE_REFERENCE_CORRECTION, 6)


def scale_card_object(obj, height_in):
    """TTS cards follow image aspect ratio; uniformly scale both axes."""
    val = card_scale(height_in)
    obj['Transform'].update(scaleX=val, scaleZ=val)
    return obj



def guid_factory(reserved):
    counter = 0xB00000
    def new_guid():
        nonlocal counter
        while True:
            result = f'{counter:06x}'
            counter += 1
            if result not in reserved:
                reserved.add(result)
                return result
    return new_guid


def all_guids(obj):
    if isinstance(obj, dict):
        if isinstance(obj.get('GUID'), str):
            yield obj['GUID'].lower()
        for v in obj.values():
            yield from all_guids(v)
    elif isinstance(obj, list):
        for v in obj:
            yield from all_guids(v)


def transform(x=0, y=1.2, z=0, ry=180):
    return dict(posX=x, posY=y, posZ=z, rotX=0, rotY=ry, rotZ=0,
                scaleX=1, scaleY=1, scaleZ=1)


def base_object(name, guid, nick='', desc=''):
    return dict(GUID=guid, Name=name, Transform=transform(),
                Nickname=nick, Description=desc, GMNotes='',
                ColorDiffuse=dict(r=1.0, g=1.0, b=1.0),
                Locked=False, Grid=True, Snap=True,
                LuaScript='', LuaScriptState='', XmlUI='')


def bag(guid, nick, items, desc=''):
    b = base_object('Bag', guid, nick, desc)
    b['Bag'] = {'Order': 0}
    b['ContainedObjects'] = items
    return b


def image_url(card, side, project, mode):
    rel = card.get(side)
    if not rel:
        raise ValueError(f"{card['id']}: missing {side} file in manifest")
    full = (project / rel).resolve()
    if not full.is_file():
        raise ValueError(f'{card["id"]}: file not found: {full}')
    if mode == 'hosted':
        url = card.get('tts', {}).get(side + '_url', '').strip()
        if not url.startswith(('https://', 'http://')):
            raise ValueError(f'{card["id"]}: missing hosted tts.{side}_url')
        return url
    return 'Z:' + full.as_posix()


def clean_effect(value):
    """Render source CSV timing tags as readable labels; keep effect wording."""
    value = re.sub(r'<cultivation\s*/>', 'Cultivation:', value, flags=re.I)
    value = re.sub(r'<battle\s*/>', 'Battle:', value, flags=re.I)
    value = re.sub(r'<[^>]*>', '', value)
    value = re.sub(r'Battle:\s*Battle:', 'Battle:', value)
    return '\n'.join(line.strip() for line in value.strip().splitlines()).strip()


def display_info(card, catalog):
    """Lookup by immutable manifest design ID, never current displayed title."""
    category = card['category']
    section = 'plants' if category in ('root', 'vine', 'flower') else (
        'wisps' if category == 'wisp' else 'rounds')
    ident = card['id']
    info = catalog[section].get(ident)
    if info is None:
        raise ValueError(f'CARD DATA ERROR: no CSV row for manifest ID {ident!r} '
                         f'(category={category!r}); expected config section {section!r}; '
                         f'found {len(catalog[section])} entries')
    if section == 'plants':
        if info['type'].lower() != category:
            raise ValueError(f'CARD DATA ERROR: {ident}: CSV type {info["type"]!r} '
                             f'conflicts with manifest category {category!r}')
        if int(card['quantity']) != info['quantity']:
            raise ValueError(f'CARD DATA ERROR: {ident}: manifest quantity {card["quantity"]} '
                             f'!= CSV quantity {info["quantity"]}')
        return f"[{info['cost']}] {info['title']}", clean_effect(info['effect'])
    if int(card['quantity']) != info['quantity']:
        raise ValueError(f'CARD DATA ERROR: {ident}: manifest quantity {card["quantity"]} '
                         f'!= CSV quantity {info["quantity"]}')
    if section == 'wisps':
        return info['title'], clean_effect(info['effect'])
    first, second = info['effect_1_title'], info['effect_2_title']
    return f'{first} / {second}', (f'{first}: {clean_effect(info["effect_1_text"])}\n'
                                  f'{second}: {clean_effect(info["effect_2_text"])}')


def make_deck(card, deck_num, make_guid, project, mode, catalog):
    qty = int(card['quantity'])
    if qty < 2:
        raise ValueError('make_deck requires at least 2 cards')
    front = image_url(card, 'front', project, mode)
    back = image_url(card, 'back', project, mode)
    custom = {str(deck_num): {'FaceURL': front, 'BackURL': back,
                              'NumWidth': 1, 'NumHeight': 1,
                              'BackIsHidden': True, 'UniqueBack': False, 'Type': 0}}
    card_id = deck_num * 100
    category = card['category']
    title, description = display_info(card, catalog)
    if not title.strip() or not description.strip():
        raise ValueError(f'CARD DATA ERROR: {card["id"]}: blank generated title/effect: {title!r}, {description!r}')
    cards = []
    for _ in range(qty):
        c = base_object('Card', make_guid(), title, description)
        c['GMNotes'] = card['id']  # permanent design ID, independent of player-facing title
        c.update(CardID=card_id, SidewaysCard=False, CustomDeck=copy.deepcopy(custom), Hands=True)
        c['Transform'] = transform()
        scale_card_object(c, CARD_SIZES_IN[category][1])
        cards.append(c)
    deck = base_object('DeckCustom', make_guid(), title, description)
    deck['GMNotes'] = card['id']
    scale_card_object(deck, CARD_SIZES_IN[category][1])
    deck.update(DeckIDs=[card_id] * qty, CustomDeck=custom,
                ContainedObjects=cards, Hands=True, SidewaysCard=False)
    return deck


def make_multi_deck(cards, next_num, make_guid, project, mode, title, catalog):
    all_cards = []
    custom = {}
    ids = []
    for item in cards:
        key = next_num
        next_num += 1
        definition = {'FaceURL': image_url(item, 'front', project, mode),
                      'BackURL': image_url(item, 'back', project, mode),
                      'NumWidth': 1, 'NumHeight': 1, 'BackIsHidden': True,
                      'UniqueBack': False, 'Type': 0}
        custom[str(key)] = definition
        ident = key * 100
        for _ in range(int(item['quantity'])):
            card_title, description = display_info(item, catalog)
            if not card_title.strip() or not description.strip():
                raise ValueError(f'CARD DATA ERROR: {item["id"]}: blank generated title/effect: {card_title!r}, {description!r}')
            c = base_object('Card', make_guid(), card_title, description)
            c['GMNotes'] = item['id']  # each card in a mixed Round/Wisp deck keeps its own ID
            c.update(CardID=ident, SidewaysCard=False, CustomDeck={str(key): definition}, Hands=True)
            scale_card_object(c, CARD_SIZES_IN[item['category']][1])
            ids.append(ident)
            all_cards.append(c)
    if len(all_cards) < 2:
        raise ValueError(f'{title}: not enough cards')
    d = base_object('DeckCustom', make_guid(), title,
                    'Mixed card stack: take cards out to read their individual effects.')
    # Each grouped deck contains identical physical sizes.
    physical_sizes = {CARD_SIZES_IN[item['category']] for item in cards}
    if len(physical_sizes) != 1:
        raise ValueError(f'{title}: cards of different physical sizes cannot form one stack: {physical_sizes}')
    scale_card_object(d, next(iter(physical_sizes))[1])
    d.update(DeckIDs=ids, ContainedObjects=all_cards, CustomDeck=custom,
             Hands=True, SidewaysCard=False)
    return d, next_num


def build_dice(game, make_guid):
    templates = {}
    def visit(node):
        if isinstance(node, dict):
            name = node.get('Name', '')
            if isinstance(name, str) and re.fullmatch(r'Die_(4|6|8|10|12|20)', name):
                sides = int(name[4:])
                if sides not in templates:
                    templates[sides] = node
            for child in node.get('ContainedObjects', []):
                visit(child)

    for obj in game.get('ObjectStates', []):
        visit(obj)
    missing = {4, 6, 8, 10, 12, 20}.difference(templates)
    if missing:
        raise ValueError(f'Cannot find native die templates for sizes: {sorted(missing)}')

    def dice(sides, qty):
        result = []
        for _ in range(qty):
            die = copy.deepcopy(templates[sides])
            die['GUID'] = make_guid()
            die['Nickname'] = f'D{sides}'
            die['Description'] = ''
            die['LuaScript'] = ''
            die['LuaScriptState'] = ''
            die['XmlUI'] = ''
            die['Locked'] = False
            result.append(die)
        return result

    small = bag(make_guid(), 'Dice Tray 1 - D4 D6 D8',
                dice(6, GRAFT_BED[6]) + dice(8, GRAFT_BED[8]),
                'Temporary tray container. D4 compartment starts empty.')
    large = bag(make_guid(), 'Dice Tray 2 - D10 D12 D20',
                dice(10, GRAFT_BED[10]) + dice(12, GRAFT_BED[12]) + dice(20, GRAFT_BED[20]),
                'Temporary tray container.')
    player_dice = {
        color: dice(4, PLAYER_START[4]) + dice(6, PLAYER_START[6])
        for color in PLAYER_COLORS
    }
    return [small, large], player_dice


def load_noncard_url_map(project):
    for relative in ('config/asset_urls.json', 'config/token_urls.json'):
        path = project / relative
        if path.is_file():
            return json.loads(path.read_text(encoding='utf-8'))
    return {}


def artwork_url(project, relative_file, mode, hosted_urls):
    path = (project / relative_file).resolve()
    if not path.is_file():
        raise ValueError(f'Missing image: {relative_file} ({path})')
    if mode == 'local':
        return 'Z:' + path.as_posix()
    url = hosted_urls.get(relative_file, '')
    if not isinstance(url, str) or not url.startswith(('https://', 'http://')):
        raise ValueError(f'Missing hosted URL in config/asset_urls.json for {relative_file}')
    return url


def pick_art(project, directory, stem):
    matches = [p for extension in ('.png', '.jpg', '.jpeg', '.webp')
               if (p := project / directory / (stem + extension)).is_file()]
    if len(matches) != 1:
        raise ValueError(f'Expected one image for {directory}/{stem}; found {len(matches)}')
    return matches[0].relative_to(project).as_posix()


def make_flat_piece(project, stem, directory, nickname, guid, mode, hosted_urls,
                    reverse_stem=None, scale=1.0, thickness=0.1,
                    stackable=True, description='', tile_shape=0, size_in=None,
                    transparency_cutout=False):
    front = pick_art(project, directory, stem)
    back = pick_art(project, directory, reverse_stem) if reverse_stem else front
    o = base_object('Custom_Token' if transparency_cutout else 'Custom_Tile',
                    guid, nickname, description)
    o['Transform'] = transform()
    if size_in is not None:
        # Read the real pixel geometry, rather than assuming every image is square.
        # The image's intrinsic aspect ratio is *also* applied by TTS.
        from PIL import Image
        with Image.open(project / front) as img:
            iw, ih = img.size
        sx, sz = tile_scales(*size_in, iw / ih)
    else:
        sx = sz = scale
    o['Transform'].update(scaleX=sx, scaleY=1.0, scaleZ=sz)
    o['CustomImage'] = {
        'ImageURL': artwork_url(project, front, mode, hosted_urls),
        'ImageSecondaryURL': artwork_url(project, back, mode, hosted_urls),
        'ImageScalar': 1.0,
        'WidthScale': 0.0,
    }
    if transparency_cutout:
        o['CustomImage']['CustomToken'] = {
            'Thickness': thickness, 'MergeDistancePixels': 2.0,
            'StandUp': False, 'Stackable': bool(stackable)}
    else:
        o['CustomImage']['CustomTile'] = {
            'Type': tile_shape, 'Thickness': thickness,
            'Stackable': bool(stackable), 'Stretch': True}
    o['Hands'] = False
    return o


def make_vp_stack(project, stem, nickname, qty, make_guid, mode, hosted_urls):
    """Create a small square card stack for VP-1 or VP-5."""
    front = pick_art(project, 'assets/tokens', 'Token_ ' + stem)
    definition = {
        'FaceURL': artwork_url(project, front, mode, hosted_urls),
        'BackURL': artwork_url(project, front, mode, hosted_urls),
        'NumWidth': 1, 'NumHeight': 1,
        'BackIsHidden': True, 'UniqueBack': False, 'Type': 0,
    }
    deck_num = int(make_guid(), 16)
    card_id = deck_num * 100
    cards=[]
    for _ in range(qty):
        c = base_object('Card', make_guid(), nickname, 'Small square VP card.')
        c.update(CardID=card_id, SidewaysCard=False, CustomDeck={str(deck_num): definition}, Hands=True)
        scale_card_object(c, TILE_SIZES_IN['Victory'][1])
        cards.append(c)
    deck = base_object('DeckCustom', make_guid(), nickname, 'Small square VP card stack.')
    scale_card_object(deck, TILE_SIZES_IN['Victory'][1])
    deck.update(DeckIDs=[card_id]*qty, ContainedObjects=cards, CustomDeck={str(deck_num): definition},
                Hands=True, SidewaysCard=False)
    return deck


def build_tokens(project, make_guid, mode, hosted_urls):
    """Prepare shared tray bags and per-player action tokens.

    Wisp deck and butterflies are combined by main(), because the deck is
    generated by the separate card-building routine.
    """
    nutrient_and_critter_names = ('Bee', 'Worm', 'Water', 'Mulch', 'Sunlight')
    token_tray_items = []
    for name in nutrient_and_critter_names:
        for _ in range(TOKEN_COUNTS[name]):
            token_tray_items.append(make_flat_piece(
                project, 'Token_ ' + name, 'assets/tokens', name,
                make_guid(), mode, hosted_urls, size_in=TILE_SIZES_IN[name],
                thickness=0.10, stackable=True,
                transparency_cutout=name in ('Water', 'Mulch'),
                tile_shape=(2 if name in ('Bee', 'Worm') else
                            3 if name == 'Sunlight' else 0)))

    vp_items = [
        make_vp_stack(project, 'Victory', 'VP-1 Cards', TOKEN_COUNTS['Victory'],
                      make_guid, mode, hosted_urls),
        make_vp_stack(project, 'Victory-5', 'VP-5 Cards', TOKEN_COUNTS['Victory-5'],
                      make_guid, mode, hosted_urls),
    ]

    butterflies = [make_flat_piece(
        project, f'Token_ Butterfly {color}', 'assets/butterflies',
        f'{color} Butterfly', make_guid(), mode, hosted_urls,
        reverse_stem=f'Token_ Butterfly {color} (Back)',
        size_in=TILE_SIZES_IN['Butterfly'], thickness=0.08, stackable=True,
        tile_shape=2,
        description='Double-sided round Butterfly token.')
        for color in BUTTERFLY_COLORS]

    player_actions = {}
    for color in PLAYER_COLORS:
        player_actions[color] = [make_flat_piece(
            project, f'Assist_ Main Action Token ({color})', 'assets/tokens',
            f'{color} Main Action', make_guid(), mode, hosted_urls,
            size_in=TILE_SIZES_IN['Main Action'], thickness=0.10, stackable=True,
            tile_shape=3,
            description='One of two Main Action tokens for this player.')
            for _ in range(2)]

    trays = [
        bag(make_guid(), 'Token Tray - Critters and Nutrients', token_tray_items,
            'Bee, Worm, Water, Mulch, Sunlight. Temporary container for physical Token Tray.'),
        bag(make_guid(), 'VP Tray', vp_items,
            'Victory Point tokens. Temporary container for physical VP Tray.'),
    ]
    total = len(token_tray_items) + sum(TOKEN_COUNTS[n] for n in ('Victory', 'Victory-5')) + len(butterflies) + sum(map(len, player_actions.values()))
    assert total == sum(TOKEN_COUNTS.values()) + 4 + 8
    return trays, butterflies, player_actions, total


def build_player_components(project, make_guid, mode, hosted_urls, player_dice, player_actions):
    result = []
    for color in PLAYER_COLORS:
        items = [
            make_flat_piece(project, f'Placard_ Plant Core ({color})', 'assets/player',
                            f'{color} Plant Core', make_guid(), mode, hosted_urls,
                            size_in=TILE_SIZES_IN['Plant Core'], thickness=0.12, stackable=False,
                            description='Player Plant Core placard.'),
            make_flat_piece(project, 'Player Box Supply', 'assets/player',
                            'Player Dice Supply', make_guid(), mode, hosted_urls,
                            size_in=TILE_SIZES_IN['Player Dice Supply'], thickness=0.12, stackable=False,
                            description='Player Dice Supply placard.'),
            make_flat_piece(project, 'Placard_ Discard Bin', 'assets/player',
                            'Dice Discard Bin', make_guid(), mode, hosted_urls,
                            size_in=TILE_SIZES_IN['Dice Discard Bin'], thickness=0.12, stackable=False,
                            description='Player Dice Discard Bin placard.'),
            make_flat_piece(project, f'Assist_ Gnome {color} Front', 'assets/player',
                            f'{color} Gnome', make_guid(), mode, hosted_urls,
                            reverse_stem=f'Assist_ Gnome {color} Back',
                            size_in=TILE_SIZES_IN['Gnome'], thickness=0.10, stackable=False,
                            description='Two-sided Gnome marker.'),
        ]
        items.extend(player_dice[color])
        items.extend(player_actions[color])
        assert len(items) == 12, (color, len(items))
        result.append(bag(make_guid(), f'{color} Player Components', items,
                          'Plant Core, Supply, Discard Bin, Gnome, 3 D4, 3 D6, 2 Main Action tokens.'))
    return result, len(PLAYER_COLORS) * 12



def make_helper_card(project, front_stem, back_stem, nickname,
                     deck_num, make_guid, mode, hosted_urls, description=''):
    """One normal flippable TTS card (not a mirrored token)."""
    front = pick_art(project, 'assets/help', front_stem)
    back = pick_art(project, 'assets/help', back_stem)
    definition = {
        'FaceURL': artwork_url(project, front, mode, hosted_urls),
        'BackURL': artwork_url(project, back, mode, hosted_urls),
        'NumWidth': 1, 'NumHeight': 1,
        'BackIsHidden': True, 'UniqueBack': False, 'Type': 0,
    }
    obj = base_object('Card', make_guid(), nickname, description)
    scale_card_object(obj, HELPER_SIZE_IN[1])
    obj.update(CardID=deck_num * 100,
               CustomDeck={str(deck_num): definition},
               Hands=True, SidewaysCard=False)
    return obj


def build_reference_materials(project, make_guid, mode, hosted_urls,
                              first_deck_num):
    """Unpacked reference materials from the physical retail game box."""
    glade = make_flat_piece(
        project, 'Placard_ Battle Glade', 'assets/boards',
        'Battle Glade', make_guid(), mode, hosted_urls,
        size_in=tuple(d * BATTLE_GLADE_PLAY_MULTIPLIER
                      for d in TILE_SIZES_IN['Battle Glade']),
        thickness=0.12, stackable=False,
        description='Shared Battle Glade board; remove from the Game Box during setup.',
        tile_shape=0)
    # Two copies of each 4 x 5-inch, double-sided helper card.
    round_order = [make_helper_card(
        project, 'Help_ Cultivate Round Order', 'Help_ Battle Round Order',
        'Cultivation / Battle Round Helper', first_deck_num,
        make_guid, mode, hosted_urls,
        description='4-inch-wide reference. Cultivation on front, Battle on back.')
        for _ in range(2)]
    common = [make_helper_card(
        project, 'Help_ Common Rules', 'Help_ Helpers & Tokens',
        'Common Rules / Helpers & Tokens', first_deck_num + 1,
        make_guid, mode, hosted_urls,
        description='4-inch-wide reference. Common Rules on front, Helpers & Tokens on back.')
        for _ in range(2)]

    # Grove Setup artwork is 24.56 x 10.65, scaled to 8.25 inches wide.
    # Height follows artwork aspect ratio, not the old manufacturing outline.
    # Use the same image on both sides to keep text readable when flipped.
    grove_setup = make_flat_piece(
        project, 'Help_ Grove Setup', 'assets/help',
        'Grove Setup Reference', make_guid(), mode, hosted_urls,
        size_in=TILE_SIZES_IN['Grove Setup Reference'], thickness=0.10, stackable=False,
        description='Grove Setup: 8.25-inch-wide reference with natural aspect ratio; same image both sides.',
        tile_shape=0)
    return [glade, *round_order, *common, grove_setup]


def validate(cards):
    seen = set()
    for c in cards:
        if c['id'] in seen:
            raise ValueError(f"Duplicate card ID: {c['id']}")
        seen.add(c['id'])
        if c['category'] not in CATEGORIES:
            raise ValueError(f"Unrecognized card category {c['category']}")
    for cat, (designs, total) in EXPECTED.items():
        chosen = [c for c in cards if c['category'] == cat]
        actual = (len(chosen), sum(int(c['quantity']) for c in chosen))
        if actual != (designs, total):
            raise ValueError(f'{cat}: expected {(designs, total)} (designs, copies); got {actual}')


def parse_args():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--project', type=Path, default=Path(__file__).resolve().parent.parent,
                    help='leaf-tts project root (defaults to parent of tools/)')
    ap.add_argument('--mode', choices=('local', 'hosted'), default='local',
                    help='local uses Z:/home/... paths inside Steam Proton; hosted uses manifest URLs')
    ap.add_argument('--install', action='store_true', help='also install as a new numbered TTS save')
    ap.add_argument('--saves-dir', type=Path,
                    help='TTS Saves folder; defaults to known Steam Snap Proton path')
    return ap.parse_args()


def main():
    a = parse_args()
    project = a.project.expanduser().resolve()
    # CSV files are the editable source of truth. Always refresh derived config.
    import subprocess
    subprocess.run([sys.executable, str(project / 'scripts/generate_card_config.py')],
                   check=True, cwd=project)
    manifest_path = project / 'output/preparation/tts_asset_manifest.json'
    source = project / 'source/TS_Save_8.json'
    if not manifest_path.is_file() or not source.is_file():
        sys.exit(f'Missing {manifest_path if not manifest_path.is_file() else source}')
    cards = json.loads(manifest_path.read_text(encoding='utf-8'))['cards']
    catalog_file = project / 'config/card_catalog_effects.json'
    if not catalog_file.is_file():
        raise ValueError(f'Missing generated card data: {catalog_file}. Run scripts/update_card_data.sh')
    catalog = json.loads(catalog_file.read_text(encoding='utf-8'))
    validate(cards)
    for card in cards:
        display_info(card, catalog)  # fail before creating any objects
    game = json.loads(source.read_text(encoding='utf-8'))
    mk_guid = guid_factory(set(all_guids(game)))
    preserved = [copy.deepcopy(o) for o in game['ObjectStates'] if o['Name'] in ('HandTrigger', 'LayoutZone')]
    hosted_urls = load_noncard_url_map(project)

    items = []
    deck_num = 100
    plants = []
    for cat in ('root', 'vine', 'flower'):
        for card in sorted((c for c in cards if c['category'] == cat), key=lambda c: c['id']):
            plants.append(make_deck(card, deck_num, mk_guid, project, a.mode, catalog))
            deck_num += 1
    plant_catalog_bag = bag(mk_guid(), 'Plant Card Catalog', plants,
                            '36 stacks: 12 Roots, 12 Vines, 12 Flowers. Six copies of each design.')
    items.append(plant_catalog_bag)

    # Retail packaging: all 12 Cultivation and 6 Battle Round Cards are
    # one physical stack, not separate decks or bags. Their setup order is
    # chosen by the player later; no scripted ordering/shuffling happens here.
    round_cards = sorted(
        (c for c in cards if c['category'] in ('cultivation', 'battle')),
        key=lambda c: (c['category'] != 'cultivation', c['id']))
    round_deck, deck_num = make_multi_deck(
        round_cards, deck_num, mk_guid, project, a.mode, 'Round Cards - All 18', catalog)
    items.append(round_deck)

    wisps = sorted((c for c in cards if c['category'] == 'wisp'),
                   key=lambda c: c['id'])
    wisp_deck, deck_num = make_multi_deck(
        wisps, deck_num, mk_guid, project, a.mode, 'Wisp Deck', catalog)
    items.extend(build_reference_materials(
        project, mk_guid, a.mode, hosted_urls, deck_num))
    deck_num += 2

    dice_trays, player_dice = build_dice(game, mk_guid)
    items.extend(dice_trays)
    token_trays, butterflies, player_actions, num_tokens = build_tokens(project, mk_guid, a.mode, hosted_urls)
    items.extend(token_trays)
    items.append(bag(mk_guid(), 'Wisp and Butterfly Tray', [wisp_deck, *butterflies],
                     'Wisp deck and four Butterflies. Temporary container for the physical tray.'))
    player_bags, num_player_pieces = build_player_components(project, mk_guid, a.mode, hosted_urls, player_dice, player_actions)
    items.extend(player_bags)

    box = bag(mk_guid(), 'Game Box - Leaf & Let Die', items,
              'WORK IN PROGRESS: cards (including one 18-card Round stack), 69 dice, tray-organized tokens, four player bags, Battle Glade, and four helper cards plus Grove Setup. Custom 3D trays not built yet.')
    box['Transform'] = transform(0, 1.3, 0, 0)
    game['ObjectStates'] = preserved + [box]
    game['SaveName'] = ('Leaf & Let Die - Cards Dice Tokens Players Glade Helpers (LOCAL)'
                        if a.mode == 'local'
                        else 'Leaf & Let Die - Cards Dice Tokens Players Glade Helpers (HOSTED)')
    game['LuaScript'] = ''
    game['LuaScriptState'] = ''
    if 'TabStates' in game:
        for tab in game['TabStates'].values():
            if isinstance(tab, dict) and tab.get('title') == 'Rules':
                tab['body'] = ('WORK IN PROGRESS: See the v37.01 PDF rulebook. '
                               'Cards, dice, tokens, player placards, Battle Glade, and helper cards are included; '
                               'physical trays are not included yet.')

    output_dir = project / 'output'
    output_dir.mkdir(parents=True, exist_ok=True)
    output = output_dir / f'Leaf_and_Let_Die_cards_dice_tokens_players_glade_helpers_{a.mode}.json'
    sys.path.insert(0, str(project / 'scripts'))
    from validate_card_save import validate_save
    validate_save(game, json.loads(catalog_file.read_text(encoding='utf-8')), str(output))
    output.write_text(json.dumps(game, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    sizing_report = {
        'source': 'Manufacturing Outline v8.2',
        'calibration': {'card_long_units': CARD_LONG_UNITS, 'tile_base_units': TILE_BASE_UNITS, 'units_per_inch': UNITS_PER_INCH, 'screenshot_dice_reference_factor': DICE_REFERENCE_CORRECTION, 'battle_glade_play_multiplier': BATTLE_GLADE_PLAY_MULTIPLIER},
        'cards_inches': CARD_SIZES_IN, 'helper_cards_inches': HELPER_SIZE_IN,
        'tiles_inches': TILE_SIZES_IN,
        'notes': [
            'Grove Setup: 8.25 inches wide, with natural 24.56:10.65 source aspect ratio.',
            'Wisp count remains 32 from manifest, not 36 in manufacturing outline.',
            'Supply 3.4x4.68 and Discard 3.25x5.0 inches are tabletop proxy sizes, not unfolded manufacturing dielines.',
            'Water and Mulch use PNG alpha-cutout Custom_Token objects. Their reverse may appear mirrored; front artwork is prioritized.',
            'Gameplay Battle Glade is enlarged 1.5x beyond its physical 6.5 x 8.375-inch dimensions.',
            'Gnome is a flat proxy with silhouette dimensions, not the final 3D meeple.',
            'Card/tile baseline unit calibration should be confirmed via getBoundsNormalized() in TTS.',
        ],
    }
    (output_dir / 'tts_sizing_report.json').write_text(json.dumps(sizing_report, indent=2) + '\n', encoding='utf-8')

    print(f'Generated {output}: {len(plants)} Plant stacks, 216 Plant cards, 18 Round Cards, 32 Wisps.')
    print('Dice: Graft Bed 9 each of D6/D8/D10/D12/D20 (45); four player sets of 3 D4 + 3 D6 (24). Total 69.')
    print(f'Tokens: {num_tokens} total, including 4 Butterflies and 8 player Main Action tokens.')
    print(f'Player components: 4 bags × 12 items (4 placards/markers, 6 dice, 2 action tokens); {num_player_pieces} objects total.')
    print('5 temporary tray bags: 2 Dice Trays, Token Tray, Wisp and Butterfly Tray, VP Tray.')
    print('Reference materials: Battle Glade + 2 Round Order helpers + 2 Common Rules/Helpers & Tokens helpers + 1 Grove Setup reference.')
    print('All 18 Round Cards are a single mixed-design physical deck inside the Game Box.')
    print('Relative dimensions corrected for source-image aspect ratios; Water/Mulch use PNG transparency cutouts.')
    print('NOTE: Grove Setup is 8.25 in wide with artwork aspect ratio; Wisp count remains 32 (outline says 36).')
    print('Custom 3D trays and game box model are not included yet.')

    if a.install:
        saves_dir = a.saves_dir or (Path.home() / 'snap/steam/common/.local/share/Steam/steamapps/compatdata/286160/pfx/drive_c/users/steamuser/Documents/My Games/Tabletop Simulator/Saves')
        saves_dir = saves_dir.expanduser().resolve()
        if not saves_dir.is_dir():
            sys.exit(f'TTS saves directory does not exist: {saves_dir}')
        folder = saves_dir / 'Leaf and Let Die - Generated'
        folder.mkdir(exist_ok=True)
        n = 1
        while (folder / f'TS_Save_{n}.json').exists():
            n += 1
        destination = folder / f'TS_Save_{n}.json'
        with destination.open('x', encoding='utf-8') as handle:
            handle.write(output.read_text(encoding='utf-8'))
        print(f'Installed NEW save: {destination}')

    if a.mode == 'local':
        print('LOCAL ARTWORK TEST ONLY: paths use Proton Z:/...; not usable by remote reviewers.')
        print('If images fail to load, use online Component Studio caches and --mode hosted.')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, json.JSONDecodeError) as ex:
        sys.exit(f'ERROR: {ex}')
