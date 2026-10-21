#!/usr/bin/env python3
"""Keep Cultivation (12) and Battle (6) Round cards in two placard stacks.

Reuses card objects from the original deck, preserving GUIDs, artwork and CSV IDs.
Safe to run repeatedly; refuses missing, duplicate or malformed Round cards.
"""
import copy


def arrange_round_stacks(data, positions):
    objects = data['ObjectStates']
    sack = next((o for o in objects if o.get('GUID') == 'b001f3'), None)
    if sack is None:
        raise ValueError('ROUND LAYOUT ERROR: Game Box Sack b001f3 not found; save not written')
    original = []
    for source in (objects, sack.get('ContainedObjects', [])):
        original.extend(o for o in source if o.get('GUID') in ('b0010f', 'f62782'))
    if not original:
        raise ValueError('ROUND LAYOUT ERROR: no Round deck found (b0010f / f62782); save not written')
    cards = []
    for stack in original:
        cards.extend(stack.get('ContainedObjects', []))
    # The generated source originally has one mixed deck, with 18 contained cards.
    # The manual TTS arrangement has two, with 12 + 6 contained cards.
    if len(original) not in (1, 2) or len(cards) != 18:
        raise ValueError(f'ROUND LAYOUT ERROR: expected 18 cards in one or two decks; found '
                         f'{len(cards)} cards in {len(original)} decks; save not written')
    groups = {'cultivation': [], 'battle': []}
    guids = set()
    for card in cards:
        note = card.get('GMNotes', '')
        guid = card.get('GUID')
        if guid in guids or not guid:
            raise ValueError(f'ROUND LAYOUT ERROR: duplicate/missing card GUID {guid!r}; save not written')
        guids.add(guid)
        if note.startswith('Resource_'):
            groups['cultivation'].append(card)
        elif note.startswith('Battle_'):
            groups['battle'].append(card)
        else:
            raise ValueError(f'ROUND LAYOUT ERROR: unexpected Round card ID {note!r} '
                             f'on card GUID={guid}; save not written')
    if (len(groups['cultivation']), len(groups['battle'])) != (12, 6):
        raise ValueError(f'ROUND LAYOUT ERROR: expected 12 Cultivation / 6 Battle cards; '
                         f'found {len(groups["cultivation"])} / {len(groups["battle"])}; save not written')
    # Use existing deck object when present. Construct the other from the same
    # original mixed deck; its child cards remain the original distinct objects.
    source = original[0]
    converted = []
    for kind in ('cultivation', 'battle'):
        target_guid = positions[kind]['guid']
        stack = next((o for o in original if o['GUID'] == target_guid), None)
        if stack is None:
            stack = copy.deepcopy(source)
        stack['GUID'] = target_guid
        stack['Name'] = 'DeckCustom' if kind == 'cultivation' else 'Deck'
        stack['Transform'] = copy.deepcopy(positions[kind]['pose'])
        stack['Nickname'] = 'Cultivation Round Cards (12)' if kind == 'cultivation' else 'Battle Round Cards (6)'
        stack['Description'] = ('Cultivation Round Cards, arranged as a separate stack on the Game Box placard.'
                                if kind == 'cultivation' else
                                'Battle Round Cards, arranged as a separate stack on the Game Box placard.')
        stack['GMNotes'] = ''
        stack['ContainedObjects'] = groups[kind]
        stack['DeckIDs'] = [c['CardID'] for c in groups[kind]]
        # For mixed decks each design's atlas reference must be available on its parent.
        decks = {}
        for card in groups[kind]:
            decks.update(card.get('CustomDeck', {}))
        if len(decks) != 6:
            raise ValueError(f'ROUND LAYOUT ERROR: expected 6 distinct {kind} designs, found {len(decks)}; save not written')
        stack['CustomDeck'] = decks
        converted.append(stack)
    ids = {'b0010f', 'f62782'}
    objects[:] = [o for o in objects if o.get('GUID') not in ids]
    sack['ContainedObjects'] = [o for o in sack.get('ContainedObjects', []) if o.get('GUID') not in ids]
    objects.extend(converted)
    return converted
