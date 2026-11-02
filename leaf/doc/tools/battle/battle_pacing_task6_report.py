#!/usr/bin/env python3
"""Validate and summarize a completed Task-6 Battle-pacing duration/downtime run.

The reported minutes are HUMAN-TIME MODEL ESTIMATES derived from simulator event counts.
They are not measured playtest minutes. The script also writes a small SVG chart.
"""
import argparse
import csv
import math
from collections import defaultdict
from pathlib import Path
from statistics import mean


def rows(path):
    with path.open(newline='', encoding='utf-8') as f:
        return list(csv.DictReader(f))


def num(v):
    return float(v) if v not in ('', 'NA') else math.nan


def fmt_min(seconds):
    return f"{seconds / 60.0:.1f}m"


def validate(root, options, cohorts, legacy, battles, players_rows, windows, sensitivity, pacing_summary, profiles):
    expected = int(options['samples_per_cohort'])
    requested_players = [x.strip() for x in options['players'].split(',') if x.strip()]
    requested_battles = [x.strip() for x in options['battle_ordinals'].split(',') if x.strip()]
    requested = {(p, b) for p in requested_players for b in requested_battles}
    observed = {(c['players'], c['battle']) for c in cohorts}
    if observed != requested:
        missing = sorted(requested - observed)
        extra = sorted(observed - requested)
        raise ValueError(f'cohort set mismatch; missing={missing} extra={extra}')
    profile_names = [p['name'] for p in profiles]
    if not profile_names:
        raise ValueError('timing_model.csv has no profiles')
    if len(set(profile_names)) != len(profile_names):
        raise ValueError('duplicate timing profile names')
    if len(legacy) != len(cohorts):
        raise ValueError('summary.csv / selection_summary.csv cohort mismatch')

    window_keys = set()
    grouped_windows = defaultdict(list)
    for w in windows:
        key = (w['players'], w['battle'], w['sample'], w['ruleSet'], w['window'])
        if key in window_keys:
            raise ValueError(f'duplicate pacing window: {key}')
        window_keys.add(key)
        grouped_windows[key[:-1]].append(int(w['window']))
    for key, indices in grouped_windows.items():
        if sorted(indices) != list(range(1, len(indices) + 1)):
            raise ValueError(f'non-contiguous pacing windows: {key}')

    for c in cohorts:
        p, b = c['players'], c['battle']
        n = int(c['acceptedPairs'])
        if n != expected:
            raise ValueError(f'incomplete {p}p Battle {b}: {n}/{expected}')
        br = [r for r in battles if r['players'] == p and r['battle'] == b]
        pr = [r for r in players_rows if r['players'] == p and r['battle'] == b]
        sr = [r for r in sensitivity if r['players'] == p and r['battle'] == b]
        ps = [r for r in pacing_summary if r['players'] == p and r['battle'] == b]
        wr = [r for r in windows if r['players'] == p and r['battle'] == b]
        if len(br) != n * 2 * len(profile_names):
            raise ValueError(f'pacing_battles row mismatch for {p}p Battle {b}')
        if len(pr) != n * 2 * len(profile_names) * int(p):
            raise ValueError(f'pacing_players row mismatch for {p}p Battle {b}')
        if len(sr) != n * len(profile_names):
            raise ValueError(f'pacing_sensitivity row mismatch for {p}p Battle {b}')
        if len(ps) != len(profile_names):
            raise ValueError(f'pacing_summary row mismatch for {p}p Battle {b}')
        if not wr:
            raise ValueError(f'no pacing windows for {p}p Battle {b}')
        # Each paired sample must have one A and one B trace with the same opening hash.
        for sample in range(n):
            sb = [r for r in br if int(r['sample']) == sample and r['profile'] == profile_names[0]]
            if {r['ruleSet'] for r in sb} != {'A_BASELINE', 'B_CANDIDATE'}:
                raise ValueError(f'missing A/B battle rows for {p}p Battle {b} sample {sample}')
            if len({r['openingSha'] for r in sb}) != 1:
                raise ValueError(f'unpaired opening hash for {p}p Battle {b} sample {sample}')
        # Cross-check the new instrumentation against the pre-existing Task-5 action totals.
        old = next(r for r in legacy if r['players'] == p and r['battle'] == b)
        first_profile = [r for r in br if r['profile'] == profile_names[0]]
        a_supports = sum(int(r['supports']) for r in first_profile if r['ruleSet'] == 'A_BASELINE')
        b_supports = sum(int(r['supports']) for r in first_profile if r['ruleSet'] == 'B_CANDIDATE')
        if a_supports != int(old['baselineSupportActions']) or b_supports != int(old['candidateSupportActions']):
            raise ValueError(f'Support telemetry mismatch for {p}p Battle {b}: pacing={a_supports}/{b_supports} legacy={old["baselineSupportActions"]}/{old["candidateSupportActions"]}')
    return profile_names


def svg_chart(root, summaries, profile='typical'):
    data = sorted((r for r in summaries if r['profile'] == profile), key=lambda r: (int(r['players']), int(r['battle'])))
    if not data:
        return None
    width, height, left, top, bottom = 1040, 430, 70, 35, 90
    plot_h = height - top - bottom
    max_s = max(max(num(r['AmeanSeconds']), num(r['BmeanSeconds'])) for r in data)
    scale = plot_h / max_s if max_s else 1.0
    group_w = (width - left - 20) / len(data)
    bar_w = min(30, group_w * .28)
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
             '<rect width="100%" height="100%" fill="white"/>',
             '<text x="70" y="22" font-family="sans-serif" font-size="16">Task 6 typical-player modeled Battle duration (not measured play time)</text>',
             f'<line x1="{left}" y1="{top}" x2="{left}" y2="{top+plot_h}" stroke="black"/>',
             f'<line x1="{left}" y1="{top+plot_h}" x2="{width-20}" y2="{top+plot_h}" stroke="black"/>']
    for i in range(5):
        value = max_s * i / 4
        y = top + plot_h - value * scale
        parts.append(f'<line x1="{left-4}" y1="{y:.1f}" x2="{width-20}" y2="{y:.1f}" stroke="#ddd"/>')
        parts.append(f'<text x="{left-8}" y="{y+4:.1f}" text-anchor="end" font-family="sans-serif" font-size="11">{value/60:.1f}m</text>')
    for i, r in enumerate(data):
        cx = left + group_w * (i + .5)
        a, b = num(r['AmeanSeconds']), num(r['BmeanSeconds'])
        ya, yb = top + plot_h - a*scale, top + plot_h - b*scale
        parts.append(f'<rect x="{cx-bar_w-2:.1f}" y="{ya:.1f}" width="{bar_w:.1f}" height="{a*scale:.1f}" fill="#777"/>')
        parts.append(f'<rect x="{cx+2:.1f}" y="{yb:.1f}" width="{bar_w:.1f}" height="{b*scale:.1f}" fill="#bbb"/>')
        parts.append(f'<text x="{cx:.1f}" y="{top+plot_h+18}" text-anchor="middle" font-family="sans-serif" font-size="11">{r["players"]}p B{r["battle"]}</text>')
        parts.append(f'<text x="{cx:.1f}" y="{top+plot_h+34}" text-anchor="middle" font-family="sans-serif" font-size="10">{num(r["meanSavingsPercent"]):+.1f}%</text>')
    parts += [f'<rect x="{left}" y="{height-34}" width="14" height="14" fill="#777"/><text x="{left+20}" y="{height-22}" font-family="sans-serif" font-size="11">A v37.03</text>',
              f'<rect x="{left+105}" y="{height-34}" width="14" height="14" fill="#bbb"/><text x="{left+125}" y="{height-22}" font-family="sans-serif" font-size="11">B three-engagement</text>', '</svg>']
    out = root / 'task6-duration-chart.svg'
    out.write_text('\n'.join(parts), encoding='utf-8')
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('directory', type=Path)
    opt = ap.parse_args()
    root = opt.directory
    if (root / 'FAILURE.txt').exists():
        ap.error('FAILURE.txt exists: this experiment did not complete; do not report it as a valid cohort')
    manifest = (root / 'manifest.txt').read_text(encoding='utf-8')
    options = dict(line.split('=', 1) for line in manifest.splitlines() if '=' in line and not line.startswith('sha256:'))
    needed = ['selection_summary.csv','summary.csv','timing_model.csv','pacing_windows.csv','pacing_battles.csv','pacing_players.csv','pacing_sensitivity.csv','pacing_summary.csv']
    for name in needed:
        if not (root / name).is_file():
            ap.error(f'missing {name}')
    cohorts = rows(root/'selection_summary.csv'); legacy = rows(root/'summary.csv')
    profiles = rows(root/'timing_model.csv'); windows = rows(root/'pacing_windows.csv')
    battles = rows(root/'pacing_battles.csv'); player_rows = rows(root/'pacing_players.csv')
    sensitivity = rows(root/'pacing_sensitivity.csv'); summaries = rows(root/'pacing_summary.csv')
    try:
        profile_names = validate(root, options, cohorts, legacy, battles, player_rows, windows, sensitivity, summaries, profiles)
    except ValueError as e:
        ap.error(str(e))

    lines = []
    lines.append('Battle pacing Task 6 — validated human-time MODEL estimates')
    lines.append(f"Scenario: {options.get('scenario','natural')}; policy: {options['policy']}; paired samples/cohort: {options['samples_per_cohort']}")
    lines.append('IMPORTANT: minutes below are model-based estimates from simulator-observed actions/decisions, NOT measured human playing time.')
    lines.append('Validation: complete cohorts; paired opening hashes; contiguous/no-duplicate decision windows; Task-6 Support totals cross-check legacy Task-5 totals.')
    lines.append('')
    lines.append('Typical assumptions — mean duration and player-experienced pacing')
    lines.append('Players Battle | A mean | B mean | savings | mean player wait A/B | mean terminal wait A/B | windows A/B | handoffs A/B | supports A/B')
    typical = sorted((r for r in summaries if r['profile']=='typical'), key=lambda r:(int(r['players']),int(r['battle'])))
    for r in typical:
        lines.append(f"{int(r['players']):>7} {int(r['battle']):>6} | {fmt_min(num(r['AmeanSeconds'])):>6} | {fmt_min(num(r['BmeanSeconds'])):>6} | "
                     f"{fmt_min(num(r['meanSavingsSeconds'])):>6} ({num(r['meanSavingsPercent']):+.1f}%) | "
                     f"{fmt_min(num(r['AmeanPlayerWait']))}/{fmt_min(num(r['BmeanPlayerWait']))} | "
                     f"{fmt_min(num(r['AmeanTerminalWait']))}/{fmt_min(num(r['BmeanTerminalWait']))} | "
                     f"{num(r['AmeanDecisionWindows']):.1f}/{num(r['BmeanDecisionWindows']):.1f} | "
                     f"{num(r['AmeanHandoffs']):.1f}/{num(r['BmeanHandoffs']):.1f} | {num(r['AmeanSupports']):.1f}/{num(r['BmeanSupports']):.1f}")
    target = [r for r in summaries if r['players']=='4' and r['battle']=='3']
    if target:
        lines.append('')
        lines.append('Four-player final Battle sensitivity')
        lines.append('Profile | A mean/median/P95 | B mean/median/P95 | mean savings | mean terminal wait A/B | extra B planning/unit to erase modeled saving')
        for r in target:
            lines.append(f"{r['profile']:<14} | {fmt_min(num(r['AmeanSeconds']))}/{fmt_min(num(r['AmedianSeconds']))}/{fmt_min(num(r['AP95Seconds']))} | "
                         f"{fmt_min(num(r['BmeanSeconds']))}/{fmt_min(num(r['BmedianSeconds']))}/{fmt_min(num(r['BP95Seconds']))} | "
                         f"{fmt_min(num(r['meanSavingsSeconds']))} ({num(r['meanSavingsPercent']):+.1f}%) | "
                         f"{fmt_min(num(r['AmeanTerminalWait']))}/{fmt_min(num(r['BmeanTerminalWait']))} | "
                         f"{num(r['meanBreakEvenExtraCandidatePlanningSecondsPerLaterStrikeUnit']):.2f}s")
    lines.append('')
    lines.append('Interpretation guardrail: calibrate the timing parameters with physical playtest timestamps before treating any modeled percentage as a real-world time reduction.')
    report = '\n'.join(lines) + '\n'
    print(report, end='')
    (root/'task6-report.txt').write_text(report, encoding='utf-8')
    chart = svg_chart(root, summaries)
    if chart:
        print(f'Chart: {chart}')


if __name__ == '__main__':
    main()
