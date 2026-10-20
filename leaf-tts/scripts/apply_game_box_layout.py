#!/usr/bin/env python3
"""Apply the user's exact TTS game-box arrangement to the staged save.

Run this after individual tray/player/game-box rebuilds. Native TTS token stacks
are restored in place of loose generated pieces. Repeated runs are idempotent.
"""
import argparse
import copy
import json
import re
from pathlib import Path

ap = argparse.ArgumentParser()
ap.add_argument('--save', type=Path, default=Path('output/TS_Save_latest.json'))
ap.add_argument('--layout', type=Path, default=Path('config/game_box_layout.json'))
ap.add_argument('--card-effects', type=Path, default=Path('config/card_catalog_effects.json'))
a = ap.parse_args()

layout = json.loads(a.layout.read_text())
card_info = json.loads(a.card_effects.read_text())
data = json.loads(a.save.read_text())
objs = data['ObjectStates']

# A card's immutable design ID lives in GMNotes. Its visible name/description
# can change freely without affecting future rebuilds. Existing saves from before
# this rule are migrated exactly once from a card's image filename.
CARD_EFFECTS = {
    **card_info['plants'], **card_info['wisps'], **card_info['rounds'],
}


def card_failure(obj, problem, expected=None, found=None, parent=None):
    """Never silently skip a card whose identity is missing or contradictory."""
    urls = [a.get('FaceURL', '') for a in obj.get('CustomDeck', {}).values()]
    raise ValueError(
        f"\nCARD ID ERROR: {problem}\n"
        f"  Object type: {obj.get('Name', '<missing>')}\n"
        f"  GUID: {obj.get('GUID', '<missing>')}\n"
        f"  Nickname: {obj.get('Nickname', '<missing>')!r}\n"
        f"  GMNotes: {obj.get('GMNotes', '<missing>')!r}\n"
        f"  Parent: {parent or '<top level>'}\n"
        f"  Expected: {expected if expected is not None else 'a design ID in config/card_catalog_effects.json'}\n"
        f"  Found: {found if found is not None else '<none>'}\n"
        f"  Image FaceURLs: {urls or '<none>'}\n"
        f"  Action: Fix the object's GMNotes/design ID or the source mapping; "
        f"the save was NOT written."
    )


def image_design_ids(obj):
    """Extract original IDs solely for migration and consistency checks."""
    found = set()
    for asset in obj.get('CustomDeck', {}).values():
        url = asset.get('FaceURL', '').replace('\\', '/')
        name = Path(url).name
        name = re.sub(r'\[face[^]]*\](?=\.[^.]+$)', '', name, flags=re.I)
        name = re.sub(r'\.[^.]+$', '', name)
        if name:
            found.add(name)
    return found


def source_card_id(obj, expected_category=None, parent_id=None, parent_label=None):
    """Read persistent ID; migrate old saves from an unequivocal image/parent ID.

    expected_category restricts the lookup to a known card class; a missing card
    or mapping in such a context always raises a detailed exception.
    """
    note = (obj.get('GMNotes') or '').strip()
    urls = image_design_ids(obj)
    known_urls = urls & CARD_EFFECTS.keys()
    is_card = obj.get('Name') in ('Card', 'DeckCustom')
    known_note = note in CARD_EFFECTS
    # A mixed Round deck has multiple face URLs, so it isn't a single card.
    if not is_card:
        return ''
    if expected_category is None and not (known_note or known_urls or
            obj.get('Nickname') in CARD_EFFECTS):
        return ''

    if note and known_note:
        key = note
        if urls and key not in urls:
            card_failure(obj, 'Persistent ID conflicts with artwork',
                         expected=key, found=sorted(urls), parent=parent_label)
    elif note and not known_note:
        if expected_category is not None or known_urls:
            card_failure(obj, 'Persistent GMNotes ID is not defined in the mapping',
                         expected=f'ID in {expected_category or "known"} mapping',
                         found=note, parent=parent_label)
        return ''
    elif len(known_urls) == 1 and len(urls) == 1:
        key = next(iter(known_urls))
    elif len(urls) == 0 and parent_id:
        # A child lacking its own image may inherit a verified *single-design* stack ID.
        key = parent_id
    elif obj.get('Nickname') in CARD_EFFECTS and not urls:
        key = obj['Nickname']
    elif expected_category is None and len(urls) > 1 and obj.get('Name') == 'DeckCustom':
        # A mixed Round/Wisp deck; its individual cards still require IDs.
        return ''
    else:
        card_failure(obj, 'Unable to identify a unique card design',
                     expected=expected_category or parent_id,
                     found=sorted(urls), parent=parent_label)

    if expected_category is not None and key not in card_info[expected_category]:
        card_failure(obj, 'Identified card is not in the expected category mapping',
                     expected=expected_category, found=key, parent=parent_label)
    # Persist the immutable ID for all later runs, including already-renamed cards.
    obj['GMNotes'] = key
    return key


def decorate_plant(obj, parent_id=None, parent_label='Plant Card Catalog'):
    """Validate and label every Plant stack and card; never skip missing IDs."""
    key = source_card_id(obj, 'plants', parent_id, parent_label)
    info = card_info['plants'][key]
    obj['Nickname'] = f"{info['cost']} - {info['title']}"
    obj['Description'] = clean_effect(info['effect'])
    for child in obj.get('ContainedObjects', []):
        if child.get('Name') not in ('Card', 'DeckCustom'):
            card_failure(child, 'Non-card object in Plant design stack',
                         expected=key, parent=obj.get('GUID'))
        child_key = source_card_id(child, 'plants', key, obj.get('GUID'))
        if child_key != key:
            card_failure(child, 'Card in wrong Plant design stack',
                         expected=key, found=child_key, parent=obj.get('GUID'))
        decorate_plant(child, key, obj.get('GUID'))


def apply_plant_catalog(catalog):
    stacks = catalog.get('ContainedObjects', [])
    seen = set()
    for stack in stacks:
        if stack.get('Name') != 'DeckCustom':
            card_failure(stack, 'Expected Plant design stack',
                         expected='DeckCustom', parent=catalog.get('GUID'))
        key = source_card_id(stack, 'plants', parent_label=catalog.get('GUID'))
        if key in seen:
            card_failure(stack, 'Duplicate Plant design stack',
                         expected='36 distinct Plant designs', found=key,
                         parent=catalog.get('GUID'))
        seen.add(key)
        decorate_plant(stack, parent_label=catalog.get('GUID'))
    missing = sorted(set(card_info['plants']) - seen)
    extra = sorted(seen - set(card_info['plants']))
    if missing or extra:
        raise ValueError(
            f'\nPLANT CATALOG ERROR: expected {len(card_info["plants"])} '
            f'unique designs, found {len(seen)}.\n'
            f'  Missing IDs: {missing}\n  Unexpected IDs: {extra}\n'
            f'  Catalog GUID: {catalog.get("GUID")}\n'
            '  The save was NOT written.'
        )


def walk(objects):
    for item in objects:
        yield item
        yield from walk(item.get('ContainedObjects', []))


def clean_effect(value):
    """Keep only the printed effect, rendering timing icons as plain words."""
    value = re.sub(r'<cultivation\s*/>', 'Cultivation:', value, flags=re.I)
    value = re.sub(r'<battle\s*/>', 'Battle:', value, flags=re.I)
    value = re.sub(r'<[^>]*>', '', value)
    value = re.sub(r'Battle:\s*Battle:', 'Battle:', value)
    return '\n'.join(line.strip() for line in value.strip().splitlines()).strip()


# Move every identifiable existing object to its exact manually approved pose.
# This includes dice, four player-box assemblies, VP/Wisp trays, sack and placard.
updated = 0
for obj in objs:
    pose = layout['poses'].get(obj.get('GUID'))
    if pose is not None:
        obj['Transform'] = copy.deepcopy(pose)
        updated += 1

# TTS combined the 45 original loose tokens into five native stacks. Preserve
# that outcome even if rebuild_token.sh was run after the manual arrangement.
old_token_guids = {f'{n:06x}' for n in range(int('f10000',16), int('f1002c',16)+1)}
stack_guids = {o['GUID'] for o in layout['native_token_stacks']}
objs[:] = [o for o in objs if o.get('GUID') not in old_token_guids and o.get('GUID') not in stack_guids]
objs.extend(copy.deepcopy(layout['native_token_stacks']))

# TTS tooltip copy: short operational guidance, not a full rules explanation.
box = next((o for o in objs if o.get('GUID') == 'f0d001'), None)
if box:
    box['Nickname'] = 'Leaf & Let Die — Game Box Simulation'
    box['Description'] = (
        'Welcome to Leaf & Let Die! This is an approximate simulation of the retail game box. '
        'Components are laid out here so you can unpack and set up the game. '
        'The player boxes are represented as separate trays and pieces rather than exactly '
        'how they nest in the real package. Start by taking the Rulebook out of the Game Box Sack '
        'and following its First Game instructions.'
    )

sack = next((o for o in objs if o.get('GUID') == 'b001f3'), None)
if sack:
    sack['Nickname'] = 'Game Box Sack'
    sack['Description'] = (
        'Storage for the game box contents not laid out on the placard. '
        'Take out the Rulebook first. You can also put unused or off-table components '
        'back into this bag to keep the table clear. Items laid out on the Game Box '
        'placard are the pieces intended to come out onto the table during setup.'
    )
    for item in sack.get('ContainedObjects', []):
        if item.get('Nickname','').startswith('Leaf & Let Die') and item.get('Name') == 'Custom_PDF':
            item['Description'] = 'START HERE: Take out and read this rulebook to set up and learn Leaf & Let Die.'


# Catalog is a separate bag on the placard, beside the Game Box Sack.
# When rebuilding an older save, pull the Catalog out of the Sack exactly once.
# Keep any already-external Catalog in place on subsequent rebuilds.
CATALOG_GUID = 'b000fc'
CATALOG_POS = {'posX': -20.2, 'posY': 1.05, 'posZ': -6.83,
               'rotX': 0, 'rotY': 180, 'rotZ': 0,
               'scaleX': 1, 'scaleY': 1, 'scaleZ': 1}
external_catalog = next((o for o in objs if o.get('GUID') == CATALOG_GUID), None)
if sack:
    sack_items = sack.get('ContainedObjects', [])
    nested_catalog = next((o for o in sack_items if o.get('GUID') == CATALOG_GUID), None)
    sack['ContainedObjects'] = [o for o in sack_items if o.get('GUID') != CATALOG_GUID]
    if external_catalog is None and nested_catalog is not None:
        external_catalog = nested_catalog
        external_catalog['Transform'] = dict(CATALOG_POS)
        objs.append(external_catalog)
if external_catalog:
    external_catalog['Nickname'] = 'Plant Card Catalog'
    external_catalog['Description'] = (
        'Plant Creature cards organized by Roots, Vines, and Flowers. '
        'Take out the card designs needed for setup and play.'
    )

# Rename plant decks and individual cards ANYWHERE in the save, regardless of
# whether the Catalog is inside a sack, on the placard, or a deck is on the table.
# This also handles the literal zFlower_ file prefix and ALL 12 Vine designs.
# Validate the complete catalog first; only then update cards removed from it.
if external_catalog is None:
    raise ValueError('PLANT CATALOG ERROR: Plant Card Catalog b000fc is absent. Save NOT written.')
apply_plant_catalog(external_catalog)
catalog_card_guids = {item.get('GUID') for item in walk(external_catalog.get('ContainedObjects', []))}
for obj in walk(objs):
    if obj.get('Name') in ('Card', 'DeckCustom') and obj.get('GUID') not in catalog_card_guids:
        if image_design_ids(obj) & card_info['plants'].keys() or obj.get('GMNotes') in card_info['plants']:
            decorate_plant(obj, parent_label='outside Plant Card Catalog')

# Round cards and Wisps can be in decks/bags or directly on the table.
# Recursing over the full tree also covers a card removed from its deck.
def rename_effect_cards(obj, parent_label='<top level>', expected_category=None):
    """Validate every card in known Round/Wisp decks, including mixed decks."""
    if obj.get('Name') in ('Card', 'DeckCustom'):
        imgs = image_design_ids(obj)
        note = obj.get('GMNotes', '')
        cat = ('wisps' if note in card_info['wisps'] or imgs & card_info['wisps'].keys()
               else 'rounds' if note in card_info['rounds'] or imgs & card_info['rounds'].keys()
               else expected_category)
        if cat:
            # Multiple image IDs = a mixed deck, not an individual card ID.
            if len(imgs) > 1 and obj.get('Name') == 'DeckCustom':
                if not all(name in card_info[cat] for name in imgs):
                    card_failure(obj, 'Unknown design in a mixed deck',
                                 expected=cat, found=sorted(imgs), parent=parent_label)
            else:
                key = source_card_id(obj, cat, parent_label=parent_label)
                info = card_info[cat][key]
                if cat == 'wisps':
                    obj['Nickname'] = info['title']
                    obj['Description'] = clean_effect(info['effect'])
                else:
                    first, second = info['effect_1_title'], info['effect_2_title']
                    obj['Nickname'] = f'{first} / {second}'
                    obj['Description'] = f"{first}: {clean_effect(info['effect_1_text'])}\n{second}: {clean_effect(info['effect_2_text'])}"
    else:
        cat = expected_category
    for nested in obj.get('ContainedObjects', []):
        rename_effect_cards(nested, obj.get('GUID', '<missing GUID>'), cat)

for top in objs:
    rename_effect_cards(top)

# Remove development-only labels and add practical object descriptions.
for o in objs:
    name = o.get('Nickname','')
    if 'Physical Test' in name:
        o['Nickname'] = name.replace(' — Physical Test', '').replace('Physical Test', '').strip()
        name = o['Nickname']
    if 'Player Supply Tray' in name:
        color = name.split()[0]
        o['Description'] = (
            f'{color} Player Supply: simulated base of the player component box. '
            'In the physical game, dice and player pieces are stored in this box. '
            'During play, use the Supply as described in the rulebook.'
        )
    elif 'Discard Bin Lid' in name:
        o['Description'] = (
            'Simulated Discard Bin cover for the player component box. '
            'Remove it during setup; use the Dice Discard Bin as directed by the rules.'
        )
    elif 'Butterfly' in name and o.get('Name') != 'Custom_Model':
        o['Description'] = 'Reroll 1 die. Choose the new or original result.'
    elif name.startswith('VP Tray'):
        o['Description'] = 'Victory Point supply tray. Keep the VP cards here during play.'
    elif name.startswith('Wisp & Butterfly Tray'):
        o['Description'] = 'Shared Wisp and Butterfly supply tray. Follow the rulebook when gaining or using these helpers.'
    elif name.startswith('Token Tray'):
        o['Description'] = 'Shared supplies of Critters and nutrients (Bee, Worm, Sunlight, Water, Mulch).'
    elif 'Dice Tray' in name and o.get('Name')=='Custom_Model':
        o['Description'] = 'Shared Graft Bed dice supply. Take and buy dice according to the rulebook.'

# Butterflies may occur in bags too; ensure their tooltip states only the effect.
for o in walk(objs):
    if 'Butterfly' in o.get('Nickname', '') and o.get('Name') != 'Custom_Model':
        o['Description'] = 'Reroll 1 die; flip this Butterfly face down.'

# The Rules notes tab still contained an obsolete warning that no trays exist.
notes=data.get('TabStates',{}).get('0')
if notes and notes.get('title')=='Rules':
    notes['body'] = (
        'Leaf & Let Die — Blind Playtest v37.01. Start by taking the Rulebook PDF '
        'out of the Game Box Sack and following its First Game instructions. '
        'The Game Box placard is an approximate packaging simulation; all game trays '
        'and player supplies are arranged on it for unpacking.'
    )

# Fail closed: verify ALL cards in final save before touching the staged file.
from validate_card_save import validate_save
validate_save(data, card_info, str(a.save))
a.save.write_text(json.dumps(data, indent=2)+'\n')
print(f'Applied {updated} saved object placements, restored five token stacks, updated descriptions.')
print('Updated staged save:', a.save)
