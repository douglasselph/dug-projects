#!/usr/bin/env python3
"""Stage a rulebook-only change; leave all other save objects untouched."""
import argparse
import json
from pathlib import Path
from rulebook import update_rulebook

ap = argparse.ArgumentParser()
ap.add_argument('--save', type=Path, required=True)
ap.add_argument('--output', type=Path, required=True)
ap.add_argument('--rules-pdf', type=Path)
args = ap.parse_args()
if not args.save.is_file():
    ap.error(f'Input save missing: {args.save}')
data = json.loads(args.save.read_text())
sacks = [o for o in data.get('ObjectStates', []) if o.get('GUID') == 'b001f3']
if len(sacks) != 1:
    ap.error(f'Expected exactly one Game Box Sack b001f3; found {len(sacks)}')
path = update_rulebook(sacks[0], args.rules_pdf)
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')
print(f'Rulebook staged: {path} -> {args.output}')
