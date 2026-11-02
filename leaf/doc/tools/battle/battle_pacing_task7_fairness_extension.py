#!/usr/bin/env python3
"""Additional Task 7 observational fairness diagnostics, no third-party dependencies.

The statistical estimands here are CONDITIONAL ASSOCIATIONS and paired order
perturbation effects. They are not causal estimates of the effect of being last.
"""
import csv, collections, math, random, statistics, sys
from pathlib import Path

def load(p):
    with open(p,newline='') as fh: return list(csv.DictReader(fh))
def avg(xs): return sum(xs)/len(xs) if xs else float('nan')
def ci(xs,seed=731):
    if len(xs)<2:return (float('nan'),float('nan'))
    r=random.Random(seed); n=len(xs)
    arr=sorted(avg([xs[r.randrange(n)] for j in range(n)]) for i in range(2000))
    return arr[49],arr[1949]
def f(x):return f'{x:.3f}' if math.isfinite(x) else 'NA'
def main(directory):
    d=Path(directory)
    actors=load(d/'task7_row_actors.csv')
    initial=load(d/'task7_initial_rows.csv')
    cf=load(d/'task7_counterfactual.csv')
    pairs=load(d/'pairs.csv')
    indexed={(x['players'],x['battle'],x['sample'],x['row'],x['player']):x for x in initial}
    row_strength=collections.defaultdict(list)
    observed=[]
    missing=0
    for x in actors:
        id=x['lastMeaningfulActor']
        if not id:continue
        key=(x['players'],x['battle'],x['sample'],x['row'],id)
        start=indexed.get(key)
        if start is None:missing+=1;continue
        margin=int(start['openingMargin'])
        strength='ahead' if margin>0 else 'tied' if margin==0 else 'behind'
        sup=int(start['initialSupports']); plants=int(start['initialReadyPlants'])
        observed.append(dict(rule=x['ruleSet'],cohort=(x['players'],x['battle']),sample=x['sample'],
            row=x['row'], actor=id, strength=strength, support=sup, plants=plants,
            swing=x['swungIntoWinningStatus'].lower()=='true', margin=margin))
    # Main descriptive summary: stratify by cohort/starting margin/Support inventory/Plant readiness.
    groups=collections.defaultdict(list)
    for x in observed:
        # The bins are intentionally coarsened and explicitly reported; never an adjusted causal effect.
        sup='0-3' if x['support']<=3 else '4-8' if x['support']<=8 else '9+'
        plant='0' if x['plants']==0 else '1-2' if x['plants']<=2 else '3+'
        groups[(x['cohort'],x['strength'],sup,plant,x['rule'])].append(x)
    comparable=[]
    collapsed=collections.defaultdict(dict)
    for (cohort,strength,sup,plant,rule),rs in groups.items():
        collapsed[(cohort,strength,sup,plant)][rule]=rs
    for k,byrule in collapsed.items():
        a=byrule.get('A_BASELINE',[]);b=byrule.get('B_CANDIDATE',[])
        if a and b:comparable.append((k,a,b))
    # Paired reweighting on common observed strata; empirical difference conditional on last acting.
    # A and B are NOT matched actor identities; only the opening scenario itself is paired.
    weighted_num=0.;weighted_den=0
    for _,a,b in comparable:
        n=min(len(a),len(b));weighted_num+=n*(avg([i['swing'] for i in b])-avg([i['swing'] for i in a]));weighted_den+=n
    out=['# Task 7 — Supplementary fairness audit','',
         'This diagnostic uses the supplied real-engine pilot. The last-actor comparisons below are **observational**, not causal. In particular, conditioning on the identity of the last actor may introduce post-treatment selection bias.','',
         f'Last actor opening records joined: {len(observed)}; unmatched: {missing}.',
         f'Comparable cohort × opening-margin × Support × Plant strata: {len(comparable)}.',
         f'Common-strata descriptive difference in swing rate (B−A): {f(weighted_num/weighted_den) if weighted_den else "NA"}.',
         'This is a sensitivity diagnostic, not an estimate of the ruleset-induced last-mover effect.',
         '', '## Opening-strength-conditioned swing frequency','',
         '| Opening status | A observations | A swing rate | B observations | B swing rate |',
         '|---|---:|---:|---:|---:|']
    for strength in ('behind','tied','ahead'):
        a=[x for x in observed if x['rule']=='A_BASELINE' and x['strength']==strength]
        b=[x for x in observed if x['rule']=='B_CANDIDATE' and x['strength']==strength]
        out.append(f'| {strength} | {len(a)} | {f(avg([x["swing"] for x in a]))} | {len(b)} | {f(avg([x["swing"] for x in b]))} |')
    # Key causal estimand we CAN obtain: seat rotation vs ordinary B on same paired opening.
    # VP sum can change due to wound extra VP; individual paired contrasts are valid.
    bycohort=collections.defaultdict(lambda:collections.defaultdict(list))
    for x in cf:bycohort[(x['players'],x['battle'])][x['sample']].append(x)
    out+=['','## Controlled B starter-rotation effects (per-player paired observations)','',
          '| Cohort | Pairs | Mean absolute VP change/player | Players changing VP (%) | Largest individual VP change |',
          '|---|---:|---:|---:|---:|']
    for key, samples in sorted(bycohort.items(),key=lambda x:tuple(map(int,x[0]))):
        absby=[avg([abs(float(x['deltaBattleVP'])) for x in rr]) for rr in samples.values()]
        changes=[sum(float(x['deltaBattleVP'])!=0 for x in rr)/len(rr) for rr in samples.values()]
        maximum=max(abs(float(x['deltaBattleVP'])) for rr in samples.values() for x in rr)
        out.append(f'| {key[0]}p/B{key[1]} | {len(samples)} | {f(avg(absby))} | {f(100*avg(changes))}% | {maximum:g} |')
    out+=['','The signed average rotation effect can cancel even when changing the starter materially redistributes VP between players. These absolute effects measure **order sensitivity**, not necessarily unfairness.',
          '','## Additional checks','']
    expected={ (x['players'],x['battle'],x['sample'],x['openingSha']) for x in pairs }
    actual={ (x['players'],x['battle'],x['sample'],x['openingSha']) for x in cf }
    out.append(f'- Counterfactual replay opening fingerprints contained in paired outcomes: {actual.issubset(expected)}.')
    out.append(f'- Counterfactual opening coverage: {len(actual)} of {len(expected)} paired openings.')
    out.append(f'- Counterfactual player rows: {len(cf)}; A/B actor rows: {len(actors)}.')
    out+=['','## What remains unproved','',
          '1. No A scheduler intervention has been run under an equivalent order perturbation. The B starter-rotation cannot be subtracted from a matching A perturbation to yield an excess causal advantage.',
          '2. Last meaningful actor is an outcome of player policy and opportunities, not a randomized assignment. This audit cannot make its conversion rate causal by adjusting for observed opening cards and resources.',
          '3. The pilot has only 10 matched openings per cohort. A larger pre-registered follow-up should use at least 100 per cohort and deliberate 4-player final/resource stress replications.',
          '4. Neither direct per-action Wound-caused-by-last-actor attribution nor a controlled High-row Wound-suppression replay exists. Thus downstream Wound cascade claims remain untested.',
          '','**Design disposition:** Continue focused physical playtesting and causal fairness research before accepting B unchanged.']
    output=d/'task7_fairness_extension.md';output.write_text('\n'.join(out)+'\n');print(output)
if __name__=='__main__':
    if len(sys.argv)!=2:raise SystemExit('usage: battle_pacing_task7_fairness_extension.py RESULT_DIRECTORY')
    main(sys.argv[1])
