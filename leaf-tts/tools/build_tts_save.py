#!/usr/bin/env python3
"""Build a fresh Leaf & Let Die card-box TTS save from prepared assets.

Milestone 1: 36 Plant stacks (216 cards), 12 Cultivation, 6 Battle,
32 Wisp cards, stored in labeled containers inside the Game Box.
Other physical components and trays are deliberately NOT built yet.

Local preview requires Steam/Proton to reach Windows Z:/home/... file paths.
Hosted mode requires tts.front_url/back_url entries in the manifest.
"""
import argparse
import copy
import json
import os
from pathlib import Path
import re
import shutil
import sys

CATEGORIES = ('root', 'vine', 'flower', 'cultivation', 'battle', 'wisp')
EXPECTED = {'root': (12, 72), 'vine': (12, 72), 'flower': (12, 72),
            'cultivation': (6, 12), 'battle': (6, 6), 'wisp': (11, 32)}


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
    # Windows Proton maps Linux / to Z:/ by default. This is for local testing only.
    return 'Z:' + full.as_posix()


def make_deck(card, deck_num, make_guid, project, mode):
    qty = int(card['quantity'])
    if qty < 2:
        raise ValueError('make_deck requires at least 2 cards')
    front = image_url(card, 'front', project, mode)
    back = image_url(card, 'back', project, mode)
    custom = {str(deck_num): {'FaceURL': front, 'BackURL': back,
                              'NumWidth': 1, 'NumHeight': 1,
                              'BackIsHidden': True, 'UniqueBack': False, 'Type': 0}}
    card_id = deck_num * 100
    cost = card.get('cost')
    category = card['category']
    title = card['id']
    description = f'{category.title()} card' + (f' • Cost {cost}' if cost is not None else '')
    cards = []
    for _ in range(qty):
        c = base_object('Card', make_guid(), title, description)
        c.update(CardID=card_id, SidewaysCard=False, CustomDeck=copy.deepcopy(custom), Hands=True)
        c['Transform'] = transform()
        cards.append(c)
    deck = base_object('DeckCustom', make_guid(), title, description)
    deck.update(DeckIDs=[card_id]*qty, CustomDeck=custom,
                ContainedObjects=cards, Hands=True, SidewaysCard=False)
    return deck


def make_multi_deck(cards, next_num, make_guid, project, mode, title):
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
        ident = key*100
        for _ in range(int(item['quantity'])):
            c = base_object('Card', make_guid(), item['id'], item['category'].title() + ' card')
            c.update(CardID=ident, SidewaysCard=False, CustomDeck={str(key): definition}, Hands=True)
            ids.append(ident)
            all_cards.append(c)
    if len(all_cards) < 2:
        raise ValueError(f'{title}: not enough cards')
    d = base_object('DeckCustom', make_guid(), title)
    d.update(DeckIDs=ids, ContainedObjects=all_cards, CustomDeck=custom,
             Hands=True, SidewaysCard=False)
    return d, next_num


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
    manifest_path = project / 'output/preparation/tts_asset_manifest.json'
    source = project / 'source/TS_Save_8.json'
    if not manifest_path.is_file() or not source.is_file():
        sys.exit(f'Missing {manifest_path if not manifest_path.is_file() else source}')
    cards = json.loads(manifest_path.read_text(encoding='utf-8'))['cards']
    validate(cards)
    game = json.loads(source.read_text(encoding='utf-8'))
    mk_guid = guid_factory(set(all_guids(game)))
    preserved = [copy.deepcopy(o) for o in game['ObjectStates'] if o['Name'] in ('HandTrigger','LayoutZone')]
    items = []
    deck_num = 100  # separate deck index per design, no conflict with preserved objects
    plants = []
    for cat in ('root', 'vine', 'flower'):
        for card in sorted((c for c in cards if c['category'] == cat), key=lambda c:c['id']):
            plants.append(make_deck(card, deck_num, mk_guid, project, a.mode))
            deck_num += 1
    catalog = bag(mk_guid(), 'Plant Card Catalog', plants,
                  '36 stacks: 12 Roots, 12 Vines, 12 Flowers. Six copies of each design.')
    items.append(catalog)
    for cat,title in [('cultivation','Cultivation Round Cards'),('battle','Battle Round Cards'),('wisp','Wisp Deck')]:
        group = sorted((c for c in cards if c['category'] == cat), key=lambda c:c['id'])
        deck, deck_num = make_multi_deck(group, deck_num, mk_guid, project, a.mode, title)
        # Storing a deck in a bag does not shuffle it; player must shuffle during setup.
        items.append(deck)
    box = bag(mk_guid(), 'Game Box - Leaf & Let Die', items,
              'WORK IN PROGRESS: cards only; trays, dice and tokens not built yet.')
    box['Transform'] = transform(0, 1.3, 0, 0)
    game['ObjectStates'] = preserved + [box]
    game['SaveName'] = 'Leaf & Let Die - Card Asset Build (LOCAL)' if a.mode == 'local' else 'Leaf & Let Die - Card Asset Build (HOSTED)'
    game['LuaScript'] = ''
    game['LuaScriptState'] = ''
    if 'TabStates' in game:
        for tab in game['TabStates'].values():
            if isinstance(tab, dict) and tab.get('title') == 'Rules':
                tab['body'] = 'WORK IN PROGRESS: See the v37.01 PDF rulebook. This save contains cards only.'
    output_dir = project / 'output'
    output_dir.mkdir(parents=True,exist_ok=True)
    output = output_dir / f'Leaf_and_Let_Die_cards_{a.mode}.json'
    output.write_text(json.dumps(game, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    print(f'Generated {output}: {len(plants)} Plant stacks, 216 Plant cards, 18 Round Cards, 32 Wisps.')
    print('Other physical components are not included yet.')
    if a.install:
        saves_dir = a.saves_dir or (Path.home() / 'snap/steam/common/.local/share/Steam/steamapps/compatdata/286160/pfx/drive_c/users/steamuser/Documents/My Games/Tabletop Simulator/Saves')
        saves_dir = saves_dir.expanduser().resolve()
        if not saves_dir.is_dir():
            sys.exit(f'TTS saves directory does not exist: {saves_dir}')
        folder = saves_dir / 'Leaf and Let Die - Generated'
        folder.mkdir(exist_ok=True)
        # Never overwrite a save: select a free TS_Save_<N>.json in our generated folder.
        n=1
        while (folder/f'TS_Save_{n}.json').exists():
            n += 1
        destination = folder/f'TS_Save_{n}.json'
        with destination.open('x',encoding='utf-8') as handle:
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
