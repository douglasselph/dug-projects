#!/usr/bin/env python3
"""Prepare Leaf & Let Die artwork for a repeatable Tabletop Simulator build.

No files under assets/ are changed. Outputs a manifest, CSV card list, audit
report, and URL mapping template. This does not upload artwork to TTS/Steam.

Usage: python3 tools/prepare_tts_assets.py --root .
"""
from __future__ import annotations
import argparse
import csv
import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

IMAGE_EXTS = {'.png', '.jpg', '.jpeg', '.webp'}
CARD_RE = re.compile(r'^(?P<id>.+)\[(?P<side>face|back),(?P<count>\d+)\]$', re.I)
EXPECTED = {'root': (12, 72), 'vine': (12, 72), 'flower': (12, 72),
            'cultivation': (6, 12), 'battle': (6, 6), 'wisp': (11, 32)}


def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def classify(rel):
    p = rel.parts
    if len(p) >= 4 and p[:2] == ('assets', 'plants'):
        return {'roots': 'root', 'vines': 'vine', 'flowers': 'flower'}.get(p[2])
    if len(p) >= 3 and p[:2] == ('assets', 'rounds'):
        return 'battle' if rel.name.startswith('Battle_') else 'cultivation' if rel.name.startswith('Resource_') else None
    if len(p) >= 3 and p[:2] == ('assets', 'wisps'):
        return 'wisp'
    return None


def inspect_image(path):
    try:
        from PIL import Image
    except ImportError:
        return {'note': 'Install Pillow to validate image sizes/transparency: python3 -m pip install Pillow'}
    try:
        with Image.open(path) as img:
            info = {'width': img.width, 'height': img.height, 'mode': img.mode}
            # Only scan alpha when alpha is actually present; never change assets.
            if 'A' in img.getbands():
                low, high = img.getchannel('A').getextrema()
                info['has_transparency'] = low < 255
            return info
    except Exception as exc:
        return {'error': f'{type(exc).__name__}: {exc}'}


def run(root, dest):
    asset_dir = root / 'assets'
    if not asset_dir.is_dir():
        raise SystemExit(f'Missing assets directory: {asset_dir}')
    dest.mkdir(parents=True, exist_ok=True)
    files = sorted(p for p in asset_dir.rglob('*') if p.is_file())
    images = [p for p in files if p.suffix.lower() in IMAGE_EXTS]
    issues, warnings = [], []
    grouped = defaultdict(lambda: {'face': None, 'back': None, 'count': None, 'category': None})
    others = []
    wisp_back = next((p for p in images if p.parent == asset_dir / 'wisps' and p.stem.lower() == '_back'), None)
    for path in images:
        rel = path.relative_to(root)
        category = classify(rel)
        match = CARD_RE.match(path.stem) if category else None
        if match:
            key = (category, match['id'])
            rec = grouped[key]
            quantity = int(match['count'])
            side = match['side'].lower()
            if rec[side] is not None:
                issues.append(f'Duplicate {side} for {category}:{match["id"]}: {rec[side]} and {rel}')
            rec[side] = rel.as_posix()
            if rec['count'] is not None and rec['count'] != quantity:
                issues.append(f'Quantity mismatch between face/back: {category}:{match["id"]}')
            rec['count'] = quantity
            rec['category'] = category
        elif path != wisp_back:
            others.append(path)
    cards = []
    for (category, id_), d in sorted(grouped.items()):
        if not d['face']:
            issues.append(f'Missing face: {category}:{id_}')
        if not d['back']:
            if category == 'wisp' and wisp_back:
                d['back'] = wisp_back.relative_to(root).as_posix()
            else:
                issues.append(f'Missing back: {category}:{id_}')
        cost = None
        if category in ('root', 'vine', 'flower'):
            m = re.match(r'^(?:z?Flower|Root|Vine)_(\d+)_(\d+)$', id_, re.I)
            if m:
                cost = int(m[1])
            else:
                warnings.append(f'Unexpected Plant card ID pattern: {id_}')
        cards.append({'id': id_, 'category': category, 'cost': cost, 'quantity': d['count'],
                      'front': d['face'], 'back': d['back'],
                      'tts': {'front_url': '', 'back_url': ''}})
    totals = Counter()
    designs = Counter()
    for c in cards:
        totals[c['category']] += c['quantity'] or 0
        designs[c['category']] += 1
    for category, (exp_designs, exp_copies) in EXPECTED.items():
        if designs[category] != exp_designs or totals[category] != exp_copies:
            issues.append(f'{category}: found {designs[category]} designs/{totals[category]} cards; expected {exp_designs}/{exp_copies}')
    if wisp_back is None:
        issues.append('Missing shared Wisp back assets/wisps/_back.(jpg/png)')
    # Include all non-card artwork: trays, tokens, documentation images, etc.
    other_entries = []
    for p in sorted(others):
        rel = p.relative_to(root).as_posix()
        other_entries.append({'path': rel, 'category': p.relative_to(asset_dir).parts[0], 'tts_url': ''})
    for p in files:
        if p.suffix.lower() not in IMAGE_EXTS and p.suffix.lower() != '.pdf':
            warnings.append(f'Non-image asset (retained, but not prepared): {p.relative_to(root)}')
    # A local path cannot be used as a shareable TTS URL; require published HTTPS URLs later.
    referenced_images = sorted(set([x for c in cards for x in (c['front'], c['back']) if x] + [x['path'] for x in other_entries]))
    urls = {path: '' for path in referenced_images}
    manifest = {
        'schema_version': 1,
        'description': 'Leaf & Let Die v37 artwork inventory. Empty TTS URLs must be hosted before building a portable save.',
        'cards': cards,
        'other_images': other_entries,
        'source_documents': sorted(p.relative_to(root).as_posix() for p in files if p.suffix.lower() == '.pdf'),
        'card_counts': {k: {'designs': designs[k], 'physical_cards': totals[k]} for k in EXPECTED},
        'issues': issues, 'warnings': warnings,
    }
    (dest / 'tts_asset_manifest.json').write_text(json.dumps(manifest, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    (dest / 'tts_hosting_template.json').write_text(json.dumps(urls, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    with (dest / 'tts_card_inventory.csv').open('w', newline='', encoding='utf-8') as out:
        writer = csv.DictWriter(out, fieldnames=['id','category','cost','quantity','front','back'])
        writer.writeheader()
        for c in cards: writer.writerow({k: c[k] for k in writer.fieldnames})
    sections = [
        '# Leaf & Let Die — TTS Asset Preparation Report', '',
        f'- Project: `{root}`',
        f'- Images discovered: **{len(images)}**',
        f'- Distinct cards: **{len(cards)}**',
        f'- Card copies represented: **{sum(totals.values())}**',
        f'- Other artwork files: **{len(other_entries)}**',
        f'- Problems: **{len(issues)}**; notices: **{len(warnings)}**', '',
        '## Card totals', '', '| Category | Designs | Copies | Expected designs | Expected copies |',
        '|---|---:|---:|---:|---:|',
    ]
    for cat,(ed,ec) in EXPECTED.items():
        sections.append(f'| {cat} | {designs[cat]} | {totals[cat]} | {ed} | {ec} |')
    sections.extend(['', '## Files by artwork category', ''])
    dir_counts = Counter(x['category'] for x in other_entries)
    for cat,n in sorted(dir_counts.items()):sections.append(f'- {cat}: {n} image(s)')
    sections.extend(['', '## Issues', ''])
    sections.extend([f'- {x}' for x in issues] or ['None.'])
    sections.extend(['', '## Notices', ''])
    sections.extend([f'- {x}' for x in warnings] or ['None.'])
    sections.extend(['', '## Selected image checks', ''])
    sample_paths = [root / c['front'] for c in cards[:1] if c['front']]
    sample_paths += [root / x['path'] for x in other_entries if x['category'] in ('trays','boards')][:2]
    for path in sample_paths:
        sections.append(f'- `{path.relative_to(root)}`: {inspect_image(path)}')
    sections.extend(['', '## Next stage', '',
       '1. Review `tts_asset_manifest.json` and `tts_card_inventory.csv`.',
       '2. Set physical quantities and object behavior for tokens, trays, player pieces, and dice in a component manifest.',
       '3. Publish images to stable HTTPS URLs (Component Studio caches or Steam Cloud).',
       '4. Fill `tts_hosting_template.json` with the exact hosted URL for every image.',
       '5. Build the TTS custom decks, trays, and Game Box JSON in the subsequent build stage.',
       '', '**Important:** this preparation script does not upload files or generate a loadable TTS save.'])
    (dest / 'tts_asset_report.md').write_text('\n'.join(sections)+'\n', encoding='utf-8')
    print(f'Found {len(images)} images, {len(cards)} card designs, {sum(totals.values())} card copies.')
    for cat in EXPECTED:print(f'  {cat}: {designs[cat]} designs, {totals[cat]} cards')
    print(f'Generated: {dest / "tts_asset_manifest.json"}')
    print(f'Generated: {dest / "tts_card_inventory.csv"}')
    print(f'Generated: {dest / "tts_hosting_template.json"}')
    print(f'Generated: {dest / "tts_asset_report.md"}')
    if issues:
        print(f'ERROR: {len(issues)} issue(s); see report',file=sys.stderr)
        return 1
    print('Card front/back and card-copy checks passed.')
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent.parent, help='leaf-tts root directory')
    parser.add_argument('--out', type=Path, default=None, help='output directory (default: <root>/output/preparation)')
    args=parser.parse_args()
    root=args.root.expanduser().resolve()
    dest=(args.out or root/'output'/'preparation').expanduser().resolve()
    raise SystemExit(run(root,dest))

if __name__=='__main__':main()
