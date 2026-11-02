#!/usr/bin/env python3
"""Read one completed Battle-pacing A/B directory; no simulations or mutations.

Usage: python3 doc/tools/battle/battle_pacing_task5_report.py output/experiments/battle-pacing/<run>
Reports model-observed mechanics and VP, NEVER human minutes or causal turn order.
"""
import argparse
import csv
from pathlib import Path
from statistics import mean


def rows(path):
    with path.open(newline='', encoding='utf-8') as f:
        return list(csv.DictReader(f))


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('directory', type=Path)
    opt = ap.parse_args()
    root = opt.directory
    if (root / 'FAILURE.txt').exists():
        ap.error('FAILURE.txt exists: this experiment did not complete; do not report as a valid cohort')
    manifest = (root / 'manifest.txt').read_text(encoding='utf-8')
    options = dict(line.split('=', 1) for line in manifest.splitlines() if '=' in line and not line.startswith('sha256:'))
    summaries = rows(root / 'summary.csv')
    selection = rows(root / 'opening_selection.csv')
    cohorts = rows(root / 'selection_summary.csv')
    pairs = rows(root / 'pairs.csv')
    if len(summaries) != len(cohorts) or not summaries:
        ap.error('Incomplete summaries; check experiment output')
    expected_samples = int(options['samples_per_cohort'])
    for cohort in cohorts:
        players, battle = int(cohort['players']), int(cohort['battle'])
        n = int(cohort['acceptedPairs'])
        if n != expected_samples:
            ap.error(f'incomplete {players}p Battle {battle}: {n}/{expected_samples}')
        matching = [r for r in pairs if int(r['players']) == players and int(r['battle']) == battle]
        if len(matching) != n * players:
            ap.error(f'pairs.csv count mismatch for {players}p Battle {battle}')
        matched_accepted = [r for r in selection if int(r['players']) == players and int(r['battle']) == battle and r['accepted'] == 'true']
        if len(matched_accepted) != n:
            ap.error(f'opening_selection.csv accepted count mismatch for {players}p Battle {battle}')
        if int(cohort['attemptedOpenings']) != len([r for r in selection if int(r['players']) == players and int(r['battle']) == battle]):
            ap.error(f'opening_selection.csv attempted count mismatch for {players}p Battle {battle}')
    print('Battle pacing Task 5 — completed simulator outcomes')
    print(f"Scenario: {options.get('scenario', 'natural')}; policy: {options['policy']}; pairs per cohort: {expected_samples}")
    print('Important: Support action counts are execution counts, NOT elapsed human time.')
    print('Players Battle | Attempts | A Supports/player | B Supports/player | Initial inventory/player | Left A/B per player | B-A VP/player (95% CI) | B-A Wounds/player')
    for s in summaries:
        c = next(r for r in cohorts if r['players'] == s['players'] and r['battle'] == s['battle'])
        players, n = int(s['players']), int(s['completedPairs'])
        denom = players * n
        a_support = int(s['baselineSupportActions']) / denom
        b_support = int(s['candidateSupportActions']) / denom
        cohort_pairs = [p for p in pairs if p['players'] == s['players'] and p['battle'] == s['battle']]
        initial_inventory = mean(int(p['initialSupports']) for p in cohort_pairs)
        a_remaining = mean(int(p['baselineRemainingSupports']) for p in cohort_pairs)
        b_remaining = mean(int(p['candidateRemainingSupports']) for p in cohort_pairs)
        print(f"{players:>7} {s['battle']:>6} | {int(c['attemptedOpenings']):>8} | {a_support:>17.2f} | {b_support:>17.2f} | "
              f"{initial_inventory:>8.2f} | {a_remaining:.2f}/{b_remaining:.2f} | "
              f"{s['pairedDeltaBattleVPPerPlayer']:>8} [{s['deltaVP_CI95Low']}, {s['deltaVP_CI95High']}] | {s['deltaWoundsPerPlayer']}")
    print('No duration/last-actor causal conclusion follows from these Task 5 summaries.')


if __name__ == '__main__':
    main()
