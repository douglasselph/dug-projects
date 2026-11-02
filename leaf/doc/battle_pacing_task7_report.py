#!/usr/bin/env python3
"""Task 7 paired balance, observed actual last actor and B starter-rotation counterfactual.

Input: Task 7 paired experiment directory. Reports per-sample cluster bootstrap intervals.
An observational last-actor correlation is never labeled causal.
"""
import csv, math, statistics, sys, random
from pathlib import Path
from collections import defaultdict

def read(p):
    with p.open(newline='') as f: return list(csv.DictReader(f))
def mean(x): return sum(x)/len(x) if x else float('nan')
def interval(x):
    if len(x)<2: return (float('nan'),float('nan'))
    rng=random.Random(7107); n=len(x)
    y=sorted(mean([x[rng.randrange(n)] for _ in range(n)]) for _ in range(1200))
    return (y[30],y[1169])
def fmt(x): return f'{x:.3f}' if math.isfinite(x) else 'NA'
def summarize(p):
    pair=read(p/'pairs.csv'); actors=read(p/'task7_row_actors.csv')
    initial=read(p/'task7_initial_rows.csv') if (p/'task7_initial_rows.csv').exists() else []
    cf=read(p/'task7_counterfactual.csv') if (p/'task7_counterfactual.csv').exists() else []
    groups=defaultdict(list)
    for r in pair: groups[(r['players'],r['battle'])].append(r)
    out=['# Task 7 — Battle fairness and balance', '',
         'Paired starting states; confidence intervals resample **whole Battles**, not player rows.',
         'Observational actor comparisons are confounded; counterfactual rotates Candidate B starter',
         'by one seat per Engagement while replaying the same legal opening and decision policy.', '',
         '## Paired outcomes and stranded Supports','',
         '| Cohort | Battles | B−A VP/player [95% CI] | B−A unused Supports/player [95% CI] |',
         '|---|---:|---:|---:|']
    for cohort, rows in sorted(groups.items(), key=lambda x:tuple(map(int,x[0]))):
        by=defaultdict(list)
        for r in rows: by[r['sample']].append(r)
        vp=[mean([float(r['candidateBattleVP'])-float(r['baselineBattleVP']) for r in rr]) for rr in by.values()]
        unused=[mean([float(r['candidateRemainingSupports'])-float(r['baselineRemainingSupports']) for r in rr]) for rr in by.values()]
        a,b=interval(vp); c,d=interval(unused)
        out.append(f'| {cohort[0]}p / B{cohort[1]} | {len(by)} | {fmt(mean(vp))} [{fmt(a)}, {fmt(b)}] | {fmt(mean(unused))} [{fmt(c)}, {fmt(d)}] |')
    out+=['','## Actual last consequential actor (descriptive only)','',
        '| Cohort | Rule | Rows | Has consequential actor | Last actor changes nonwin to win |',
        '|---|---|---:|---:|---:|']
    ac=defaultdict(list)
    for r in actors: ac[(r['players'],r['battle'],r['ruleSet'])].append(r)
    for key,rr in sorted(ac.items(),key=lambda x:(int(x[0][0]),int(x[0][1]),x[0][2])):
        has=sum(bool(r['lastMeaningfulActor']) for r in rr)
        flipped=sum(r['swungIntoWinningStatus']=='true' for r in rr)
        out.append(f'| {key[0]}p/B{key[1]} | {key[2]} | {len(rr)} | {has} | {flipped} |')
    if initial:
        inp={(r['players'],r['battle'],r['sample'],r['row'],r['player']):r for r in initial}
        out+=['','## Last-actor context — opening strength and resources','',
             '| Rule | Opening row margin (mean) | Opening Supports (mean) | Ready Plants (mean) |',
             '|---|---:|---:|---:|']
        for rule in ['A_BASELINE','B_CANDIDATE']:
            rr=[]
            for a in actors:
                if a['ruleSet'] != rule or not a['lastMeaningfulActor']: continue
                x=inp.get((a['players'],a['battle'],a['sample'],a['row'],a['lastMeaningfulActor']))
                if x: rr.append(x)
            out.append(f'| {rule} | {fmt(mean([float(x["openingMargin"]) for x in rr]))} | {fmt(mean([float(x["initialSupports"]) for x in rr]))} | {fmt(mean([float(x["initialReadyPlants"]) for x in rr]))} |')
        out+=['','These are covariate descriptions, not adjusted last-actor effect estimates.']
    if cf:
        out+=['','## Controlled B starter-rotation replay','',
             '| Cohort | Battles | Rotated−ordinary B VP/player [95% CI] | Δ stranded Supports/player | Δ Wounds/player |',
             '|---|---:|---:|---:|---:|']
        bycohort=defaultdict(lambda:defaultdict(list))
        for x in cf: bycohort[(x['players'],x['battle'])][x['sample']].append(x)
        for key, by in sorted(bycohort.items(),key=lambda x:tuple(map(int,x[0]))):
            v=[mean([float(r['deltaBattleVP']) for r in rr]) for rr in by.values()]
            u=[mean([float(r['rotatedUnusedSupports'])-float(r['referenceUnusedSupports']) for r in rr]) for rr in by.values()]
            w=[mean([float(r['rotatedWounds'])-float(r['referenceWounds']) for r in rr]) for rr in by.values()]
            lo,hi=interval(v)
            out.append(f'| {key[0]}p/B{key[1]} | {len(by)} | {fmt(mean(v))} [{fmt(lo)}, {fmt(hi)}] | {fmt(mean(u))} | {fmt(mean(w))} |')
        out+=['','A one-seat starter rotation tests whether the **specified** B starter rule is sensitive to order. It does NOT isolate the advantage of being the final actor, and it is not an equivalent A-order intervention.']
    else: out+=['','No counterfactual file. Rerun with `--task7-counterfactual true`.']
    out+=['','## Limits and adoption decision','',
          'No credible adjusted A-versus-B excess last-actor causal estimate is available here.',
          'The snapshot-based last-actor flag identifies consequential row changes but does not attribute',
          'VP or Wounds causally to the final actor; indirect earlier-row changes are not fully isolated.',
          'High-Strike Wound cascades likewise require targeted intervention or state-level trace attribution.',
          'Do not adopt B unchanged solely from the pacing result until these ambiguities are resolved.']
    path=p/'task7_fairness_report.md'; path.write_text('\n'.join(out)+'\n')
    print(path)
if __name__=='__main__':
    if len(sys.argv)!=2: raise SystemExit('usage: battle_pacing_task7_report.py RESULTS_DIR')
    summarize(Path(sys.argv[1]))
