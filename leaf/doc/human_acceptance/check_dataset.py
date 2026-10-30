#!/usr/bin/env python3
"""Schema/partition/immutability sanity check; does not certify legality or strategy."""
import csv,json,pathlib,sys
root=pathlib.Path(__file__).resolve().parent
with (root/'scenarios.csv').open(newline='') as f: rows=list(csv.DictReader(f))
with (root/'blind_review.csv').open(newline='') as f: blind=list(csv.DictReader(f))
sealed=json.loads((root/'ai_proposals_pre_scoring.json').read_text())
assert len(rows)==len(blind)==len(sealed)==28
assert len({x['scenario_id'] for x in rows})==28
assert sum(r['partition']=='HELD_OUT' for r in rows)==8
assert sum(r['partition']=='DEVELOPMENT' for r in rows)==20
assert all(r['players'] in ('2','3','4') and all(s in r['three_strike_rows'] for s in ('TOP','MID','BOT')) for r in rows)
assert all(r['proposed_author']=='AI_DRAFT_NOT_HUMAN' and r['ai_choice']=='' for r in rows)
assert all('proposed_preference' not in r and 'ai_choice' not in r for r in blind)
assert all(a['scenario_id']==b['scenario_id']==c['scenario_id'] for a,b,c in zip(rows,blind,sealed))
print('C04 human preference fixture dataset: PASS (28; 20 development; 8 held-out; reviewer responses unfilled)')
