# Battle pacing checkpoint 3: Vine and Dine locked-target repair

## Input and failure

Inspected `leaf(20261030-203333).tgz`, the `battle-pacing(1).tgz` outputs, and
`doc/SIMULATION_LAUNCH_ARCHITECTURE.md` before editing.

The 1,000-sample Mechanical Control run stopped at **4 players, Battle 3,
zero-based sample 965**, with:

- `seed=2030476706`
- `strategySeed=2030481720`
- `wispSeed=2030490548`
- `VineAndDineEffect.execute` attempting to Raise a D4 in a resolved Strike.

The lock is correct. `VineAndDineEffect.legalChoices` formerly used
`handChoices(actor)` regardless of phase, even though Candidate B retains
locked-row dice in Hand through Doom. It could offer a locked die as a legal
target and Trash a Critter before failing to Raise the die.

## Fix

In Battle, Vine and Dine now uses the existing `battleHandChoices(request)`
selector so only actor Hand dice physically placed in **unresolved** Strike rows
are eligible. In Cultivation, its targets are unchanged. In canonical v37.03,
rows remain open during all actions, so the normal Battle selection is unchanged.

Added focused tests covering (1) one open and one locked die, with an attempted
locked target excluded, and (2) an all-locked Hand being ineligible despite an
owned Critter. No Battle scheduler, per-row lock semantics, or reward rules changed.

## Launch architecture compliance

The existing `doc/run_battle_pacing_ab.sh` already compiled once via Gradle and
executed **one Java process for the full requested experiment**, so it was not
using repeated Gradle launches per game. This patch preserves that structure,
uses the project's `bin/lib/run_simulation_java`, and packages completed **or
failed/partial** results as uniquely named `.tgz` files in `~/Downloads`.
Its original `--out` directory and CSV/manifest contracts are unchanged; the
Java exit code is preserved on failure. Runs remain create-new (no in-place
resume/append was added).

## Inspection of uploaded results

The corrected 100-sample run has all nine complete cohorts. The 1,000-sample
run has eight complete 1,000-pair cohorts and 965 **recorded** pairs in the
ninth (4 players, Battle 3); its summary contains only the eight completed
cohorts. The failed run **cannot be accepted as a complete 9,000-pair study**.

| Players | Battle | Completed pairs | B-A Battle VP / player | Reported 95% CI |
|---:|---:|---:|---:|---|
| 2 | 1 | 1000 | +0.021500 | [-0.023912, +0.066912] |
| 2 | 2 | 1000 | +0.085000 | [+0.035167, +0.134833] |
| 2 | 3 | 1000 | +0.094500 | [+0.049490, +0.139510] |
| 3 | 1 | 1000 | -0.057667 | [-0.097861, -0.017473] |
| 3 | 2 | 1000 | -0.004333 | [-0.045782, +0.037115] |
| 3 | 3 | 1000 | -0.045667 | [-0.085930, -0.005403] |
| 4 | 1 | 1000 | -0.098250 | [-0.138529, -0.057971] |
| 4 | 2 | 1000 | -0.088500 | [-0.126779, -0.050221] |
| 4 | 3 | 965 recorded | incomplete | not accepted |

**Interpretation limit:** All eight completed Mechanical Control cohorts report
**zero Support Actions in A and zero in B**. These are useful correctness and
basic action-scheduler probes, not evidence for/against faster tabletop play,
Support caps, final-Battle resource stranding, or causal last-mover advantage.
The `seconds` column measures JVM simulation throughput, **not human Battle
pacing**. Human-guided paired runs and further legality coverage are needed.

## Reproduce the failed sample as an independent one-sample cohort

Sample indices start at zero. Shift the base seeds by `965 * {1009,1013,1021}`
so index zero reproduces the exact former sample-965 mechanical, strategy and
Wisp seeds for players=4, Battle=3:

```bash
bash doc/run_battle_pacing_ab.sh \
  --samples 1 --players 4 --battles 3 --policy mechanical \
  --seed 2027076685 --strategy-seed 2027081545 --wisp-seed 2027090265 \
  --out output/experiments/battle-pacing/replay-vine-and-dine-965
```

Do not reuse an existing output path. The modified launcher now copies a
results archive to `~/Downloads` after successful **or failed** runs.

## Verification status

- **Verified locally**: exact source path of the failure; patch targets the
  existing common Battle legal-die selector; Bash syntax; mocked launcher
  success/failure exit-code and archive-copy behavior; A03 guard (480 files,
  zero unexplained changes), and 19 A03 Python boundary tests.
- **Not verified locally**: Kotlin unit compilation/tests and the exact failed
  seed. An offline Gradle test attempt timed out before reporting tests.
- **Not performed**: statistical post-fix runs, human-time calibration, or
  a full audit of every Plant/Wisp target path.
