#!/usr/bin/env python3
"""Task 7 descriptive audit: observational associations, NOT turn-order causal effects.

Usage: python3 doc/battle_pacing_task7_report.py EXPERIMENT_DIRECTORY
Works on Task 6 outputs; newer runs also supply task7_row_actors.csv.
"""
import csv, sys, math, statistics
from collections import defaultdict
from pathlib import Path

def rows(p):
    with p.open(newline='') as f: return list(csv.DictReader(f))

def mean(v): return statistics.mean(v) if v else float('nan')
def fmt(x): return 'NA' if math.isnan(x) else f'{x:.3f}'
def report(folder):
    p=Path(folder); pairs=rows(p/'pairs.csv'); strikes=rows(p/'strikes.csv')
    bycohort=defaultdict(list)
    for r in pairs: bycohort[(int(r['players']),int(r['battle']))].append(r)
    lines=['# Task 7 — observational balance audit', '',
        'All paired differences below use the same sampled opening; no intervention randomizes actor order.',
        'The legacy Task 6 strikes file has B-only last actor: it CANNOT establish excess last-mover advantage.',
        'B actor correlations are confounded by opportunity order, pre-action row strength and resources.', '']
    lines += ['## Paired outcome/resource changes','',
        '| Players / Battle | Pairs | Δ Battle VP/player B−A | Δ Supports remaining/player | Δ win-share B−A |',
        '|---|---:|---:|---:|---:|']
    for key,rr in sorted(bycohort.items()):
        n=len({r['sample'] for r in rr}); d=lambda a,b: mean([float(x[a])-float(x[b]) for x in rr]);
        wins=mean([(x['candidateWouldWin']=='true')-(x['baselineWouldWin']=='true') for x in rr])
        lines.append(f'| {key[0]} / {key[1]} | {n} | {fmt(d("candidateBattleVP","baselineBattleVP"))} | {fmt(d("candidateRemainingSupports","baselineRemainingSupports"))} | {fmt(wins)} |')
    lines += ['', '## B-only last-offered-actor diagnostics (legacy, not actual last consequential actor)', '']
    for key in sorted(bycohort):
        rr=[r for r in strikes if (int(r['players']),int(r['battle']))==key]
        exposed=[r for r in rr if r['lastHadAction']=='true']
        lines.append(f'- {key[0]} players, Battle {key[1]}: {len(exposed)}/{len(rr)} last offered actor had action; {sum(r["lastSwing"]=="true" for r in exposed)} recorded swings. These are not causal estimates.')
    actor=p/'task7_row_actors.csv'
    if actor.exists():
        rec=rows(actor)
        lines += ['', '## Task 7 matched row actor telemetry', '',
            '| Cohort | Rule | Rows | Consequential row-actor found | Actor moved from non-winner to winner | Mean changed windows |',
            '|---|---|---:|---:|---:|---:|']
        groups=defaultdict(list)
        for r in rec: groups[(int(r['players']),int(r['battle']),r['ruleSet'])].append(r)
        for (n,b,rule),rr in sorted(groups.items()):
            lines.append(f'| {n}p/B{b} | {rule} | {len(rr)} | {sum(bool(x["lastMeaningfulActor"]) for x in rr)} | {sum(x["swungIntoWinningStatus"]=="true" for x in rr)} | {fmt(mean([int(x["rowChanges"]) for x in rr]))} |')
        lines += ['', 'Caution: row-total status changes are not the same as a *causal* action effect on strike VP or Wounds.']
    else: lines += ['', '## Missing equivalent actor telemetry', '',
        '`task7_row_actors.csv` is absent. Re-run the instrumented Task 7 runner to compare A and B actor associations.']
    lines += ['', '## Still required before adoption', '',
        'Controlled actor-order replays on identical legal openings, baseline row-margin/resource conditioning,',
        'per-row wound/VP attribution and high-to-middle-to-low causal cascade analysis are not established by this file.',
        'Do not adopt or reject Candidate B on the basis of these associations alone.']
    output=p/'task7_observational_audit.md';output.write_text('\n'.join(lines)+'\n');print(output); print('\n'.join(lines[:15]))
if __name__=='__main__':
    if len(sys.argv)!=2: raise SystemExit('Usage: battle_pacing_task7_report.py EXPERIMENT_DIRECTORY')
    report(sys.argv[1])
