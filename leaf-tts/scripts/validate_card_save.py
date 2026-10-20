#!/usr/bin/env python3
"""Fail-closed validation of every card's permanent ID, display name and effect.

Usage: python3 scripts/validate_card_save.py output/TS_Save_latest.json
"""
import argparse
import json
import re
from pathlib import Path


def clean_effect(value):
    value = re.sub(r'<cultivation\s*/>', 'Cultivation:', value, flags=re.I)
    value = re.sub(r'<battle\s*/>', 'Battle:', value, flags=re.I)
    value = re.sub(r'<[^>]*>', '', value)
    value = re.sub(r'Battle:\s*Battle:', 'Battle:', value)
    return '\n'.join(x.strip() for x in value.strip().splitlines()).strip()


def image_ids(obj):
    result = set()
    for meta in obj.get('CustomDeck', {}).values():
        url = meta.get('FaceURL', '').replace('\\', '/')
        name = Path(url).name
        name = re.sub(r'\[face[^]]*\](?=\.[^.]+$)', '', name, flags=re.I)
        name = re.sub(r'\.[^.]+$', '', name)
        if name:
            result.add(name)
    return result


def validate_save(data, catalog, save_label='<in-memory>'):
    all_info = {}
    for category in ('plants', 'rounds', 'wisps'):
        for key, info in catalog[category].items():
            if key in all_info:
                raise ValueError(f'Duplicate ID in generated card config: {key}')
            all_info[key] = (category, info)

    counts = {'plants': 0, 'rounds': 0, 'wisps': 0}
    plant_stacks = {}
    individual_plants = {}
    errors = []

    def err(o, reason, expected, found, location):
        errors.append(
            f'CARD VALIDATION ERROR: {reason}\n'
            f'  Save: {save_label}\n  Location: {location}\n'
            f'  GUID: {o.get("GUID", "<missing>")}\n  Type: {o.get("Name", "<missing>")}\n'
            f'  GMNotes: {o.get("GMNotes", "<missing>")!r}\n'
            f'  Nickname: {o.get("Nickname", "<missing>")!r}\n'
            f'  Description: {o.get("Description", "<missing>")!r}\n'
            f'  Face image IDs: {sorted(image_ids(o))}\n'
            f'  Expected: {expected!r}\n  Found: {found!r}'
        )

    def visit(items, location, parent_design=None):
        for obj in items:
            typ = obj.get('Name')
            path = f'{location}/{obj.get("Nickname", "<unnamed>")} [{obj.get("GUID", "?")}]'
            next_parent = parent_design
            if typ in ('Card', 'DeckCustom'):
                images = image_ids(obj)
                raw_note = (obj.get('GMNotes') or '').strip()
                # Mixed decks may legitimately hold many designs, but MUST contain
                # individual cards; they cannot be silently accepted as an empty stack.
                mixed = typ == 'DeckCustom' and len(images) > 1
                is_catalog_card = bool((images & all_info.keys()) or raw_note or parent_design or 'Plant Card Catalog' in location or 'Round Cards' in location or 'Wisp Deck' in location)
                if not is_catalog_card:
                    # VP tokens and printed helper/reference cards are not CSV designs.
                    pass
                elif mixed:
                    if not obj.get('ContainedObjects'):
                        err(obj, 'Mixed deck has no inspectable cards', 'nonempty ContainedObjects', [], path)
                    unknown = images - all_info.keys()
                    if unknown:
                        err(obj, 'Unknown artwork in mixed deck', 'IDs in CSV config', sorted(unknown), path)
                    next_parent = None
                elif is_catalog_card:
                    if not raw_note:
                        err(obj, 'Missing permanent card ID', 'GMNotes must contain CSV design ID', raw_note, path)
                    elif raw_note not in all_info:
                        err(obj, 'Unrecognized permanent card ID', 'ID in CSV config', raw_note, path)
                    else:
                        category, info = all_info[raw_note]
                        counts[category] += 1
                        next_parent = raw_note
                        if images and images != {raw_note}:
                            err(obj, 'ID/artwork mismatch', {raw_note}, sorted(images), path)
                        if parent_design and parent_design != raw_note:
                            err(obj, 'Stack contains wrong card design', parent_design, raw_note, path)
                        if category == 'plants':
                            title = f"[{info['cost']}] {info['title']}"
                            desc = clean_effect(info['effect'])
                            if typ == 'DeckCustom':
                                plant_stacks[raw_note] = plant_stacks.get(raw_note, 0) + 1
                            else:
                                individual_plants[raw_note] = individual_plants.get(raw_note, 0) + 1
                        elif category == 'wisps':
                            title, desc = info['title'], clean_effect(info['effect'])
                        else:
                            one, two = info['effect_1_title'], info['effect_2_title']
                            title = f'{one} / {two}'
                            desc = f'{one}: {clean_effect(info["effect_1_text"])}\n{two}: {clean_effect(info["effect_2_text"])}'
                        if not title.strip() or not desc.strip():
                            err(obj, 'CSV produced blank display name/effect', 'nonblank name/effect', (title, desc), path)
                        if obj.get('Nickname') != title:
                            err(obj, 'Displayed name does not match CSV', title, obj.get('Nickname'), path)
                        if obj.get('Description') != desc:
                            err(obj, 'Displayed effect does not match CSV', desc, obj.get('Description'), path)
                if not (obj.get('Nickname') or '').strip() or not (obj.get('Description') or '').strip():
                    err(obj, 'Blank name or description', 'both fields nonblank',
                        (obj.get('Nickname'), obj.get('Description')), path)
            visit(obj.get('ContainedObjects', []), path, next_parent)

    visit(data.get('ObjectStates', []), 'ObjectStates')
    # Every Plant design should be present in one six-card catalog stack.
    for key, info in sorted(catalog['plants'].items()):
        if plant_stacks.get(key, 0) != 1:
            errors.append(f'PLANT CATALOG ERROR: {key}: expected 1 stack, found {plant_stacks.get(key, 0)}')
        if individual_plants.get(key, 0) < int(info['quantity']):
            errors.append(f'PLANT CATALOG ERROR: {key}: expected at least {info["quantity"]} individual cards, found {individual_plants.get(key, 0)}')
    if errors:
        more = f'\n... and {len(errors)-12} more errors' if len(errors)>12 else ''
        raise ValueError('\n\n'.join(errors[:12]) + more + f'\n\nValidation FAILED: {len(errors)} errors. Save NOT published/written.')
    print(f'Card validation PASSED: {counts["plants"]} Plant objects, '
          f'{counts["rounds"]} Round cards, {counts["wisps"]} Wisp cards; '
          f'{len(plant_stacks)} Plant stacks. No blank or mismatched card metadata.')
    return counts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('save', type=Path)
    parser.add_argument('--config', type=Path, default=Path('config/card_catalog_effects.json'))
    args = parser.parse_args()
    validate_save(json.loads(args.save.read_text()), json.loads(args.config.read_text()), str(args.save))


if __name__ == '__main__':
    main()
