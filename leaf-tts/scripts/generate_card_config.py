#!/usr/bin/env python3
"""Generate the TTS card catalog config from the four editable source CSVs.

Usage: python3 scripts/generate_card_config.py
No hand-maintained card names/effects: edit assets/card_data/*.csv instead.
"""
import argparse
import csv
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FILES = {
    'roots': 'Cultivation_Cards - Root.csv',
    'vf': 'Cultivation_Cards - VF.csv',
    'rounds': 'Round_Cards.csv',
    'wisps': 'Wisp_Cards.csv',
}


def read_csv(path, required):
    if not path.is_file():
        raise ValueError(f'Missing card data CSV: {path}')
    with path.open(encoding='utf-8-sig', newline='') as handle:
        reader = csv.DictReader(handle)
        absent = set(required) - set(reader.fieldnames or ())
        if absent:
            raise ValueError(f'{path}: missing CSV columns: {sorted(absent)}')
        rows = list(reader)
    if not rows:
        raise ValueError(f'{path}: empty card data file')
    for number, row in enumerate(rows, start=2):
        for key in required:
            if not (row.get(key) or '').strip():
                raise ValueError(f'{path}:{number}: blank required column {key!r} for ID {row.get("name")!r}')
    return rows


def build_catalog(csv_dir):
    plants, rounds, wisps = {}, {}, {}
    source_ids = set()
    def register(row, dest, filename, category):
        ident = row['name'].strip()
        if ident in source_ids:
            raise ValueError(f'{filename}: duplicate design ID {ident!r} across CSV files')
        source_ids.add(ident)
        qty = int(row['quantity'])
        if qty < 0:
            raise ValueError(f'{filename}: negative quantity for {ident}')
        return ident, qty

    for filename, category in ((FILES['roots'], 'Root'), (FILES['vf'], None)):
        for row in read_csv(csv_dir / filename, ('name', 'title', 'type', 'cost', 'quantity', 'effect')):
            ident, qty = register(row, plants, filename, category)
            typ = row['type'].strip()
            if typ not in ('Root', 'Vine', 'Flower'):
                raise ValueError(f'{filename}: {ident}: invalid type {typ!r}')
            if (category is not None and typ != category) or (typ == 'Root' and category is None):
                raise ValueError(f'{filename}: {ident}: type {typ!r} in wrong CSV')
            cost = int(row['cost'])
            plants[ident] = {'title': row['title'].strip(), 'effect': row['effect'].strip(),
                             'cost': cost, 'type': typ, 'quantity': qty}

    for row in read_csv(csv_dir / FILES['rounds'], ('name', 'quantity', 'effect_1_title', 'effect_1_text', 'effect_2_title', 'effect_2_text')):
        ident, qty = register(row, rounds, FILES['rounds'], 'Round')
        rounds[ident] = {
            'title': row.get('title', '').strip(), 'quantity': qty,
            'effect_1_title': row['effect_1_title'].strip(),
            'effect_1_text': row['effect_1_text'].strip(),
            'effect_2_title': row['effect_2_title'].strip(),
            'effect_2_text': row['effect_2_text'].strip(),
        }

    for row in read_csv(csv_dir / FILES['wisps'], ('name', 'title', 'quantity', 'effect')):
        ident, qty = register(row, wisps, FILES['wisps'], 'Wisp')
        wisps[ident] = {'title': row['title'].strip(), 'effect': row['effect'].strip(), 'quantity': qty}

    counts = {typ: sum(1 for item in plants.values() if item['type'] == typ) for typ in ('Root','Vine','Flower')}
    if counts != {'Root': 12, 'Vine': 12, 'Flower': 12}:
        raise ValueError(f'Expected 12 designs each of Root/Vine/Flower; found {counts}')
    if len(rounds) != 12:
        raise ValueError(f'Expected 12 Round designs; found {len(rounds)}')
    return {'plants': plants, 'rounds': rounds, 'wisps': wisps}


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--csv-dir', type=Path, default=ROOT / 'assets/card_data')
    ap.add_argument('--output', type=Path, default=ROOT / 'config/card_catalog_effects.json')
    args = ap.parse_args()
    catalog = build_catalog(args.csv_dir)
    output = json.dumps(catalog, ensure_ascii=False, indent=2) + '\n'
    args.output.parent.mkdir(parents=True, exist_ok=True)
    # Write only after successful parsing and full validation.
    args.output.write_text(output, encoding='utf-8')
    print(f'Generated {args.output}: {len(catalog["plants"])} Plants, '
          f'{len(catalog["rounds"])} Round designs, {len(catalog["wisps"])} Wisp CSV entries')


if __name__ == '__main__':
    main()
