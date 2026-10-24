# R2 — Sapping Snapdragon Strategy Discovery (MVP)

## Question and scope

Question: **Can we discover an effective purchasing strategy that deliberately uses Sapping Snapdragon under the official Battle effect and under the experimental different-die effect?** Does a specialization remain competitive when the purchase incentive is removed during held-out play?

R2 is **exploratory strategy discovery**, not a causal card-effect assessment, a proof of card balance, or full-game AlphaZero-like learning. The non-Buy decision domains (Build, Battle, Plant activation, Wisp use, etc.) still use the Human Baseline. The linear learned Buy scorer can express only the state/action relationships in `BuyFeatureExtractor` and may miss sophisticated context-dependent synergies. Failed search or zero held-out purchases means **inadequate evidence**, not "Sapping is underpowered."

## Fixed research conditions

- Two-player games; nine specific official Plants (same as R1): Root Four More, Root Loot, Root Kindred, Shift Happens, Low & Behold, Vine's the Limit, Sapping Snapdragon, Bloom Backflip, Queen’s Blossom.
- `original`: official `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW`.
- `different-die`: experiment-only `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW`.
- Complete official Round and Wisp catalogs; standard 2/2/3 round structure; no modifications to canonical CSVs.
- Independent policy searches per variant. All candidates, opponents, and evaluation games get reproducible mechanical and strategy seed schedules. Opponent/candidate seats rotate. All non-Buy decisions are Human Baseline.

## Four research searches

Two separate evolutionary searches for **each** card version:

1. **Unrestricted:** evolve Buy weights for game wins, no artificial reward for Sapping.
2. **Sapping-focused:** initialize the named Sapping purchase feature with an adjustable bias (`--target-boost`, default 15). During **training only**, fitness is mean win share plus `--target-purchase-bonus` (default 0.20) times the fraction of games purchasing Sapping. If that fraction is below `--target-min-purchase-rate` (default 0.15), subtract 2 from training fitness to reject failed exploration. This is a soft training feasibility constraint: if no candidate manages the minimum, training cannot manufacture a successful specialist.

Every generation, a candidate plays against a mixture of Human Baseline, other current-generation candidates, and a previous-generation champion. This is a lightweight competitive population search, not general self-play reinforcement learning. At the end of the generations, save the best candidate of the final generation. Distinct search processes, weights, and checkpoint directories are used for each variant/objective.

The default research budget is deliberately modest: 4 generations, 6 candidates, 18 training games per candidate, 2 elites; this is a **pilot**, not exhaustive optimization. Increase budgets and independent evolution seeds for more confidence. Training rewards are not estimates of held-out win probabilities.

## Held-out evaluation

For each variant, play three separate two-player matchups, with alternating seats and held-out seed schedules:

- Sapping-focused vs unrestricted
- Sapping-focused vs Human Baseline
- Unrestricted vs Human Baseline

**The evaluation never awards extra fitness for purchasing Sapping.** Each per-player row includes win share, total VP, Strike VP, Sapping purchases and Battle activations, grafted Sapping copies, named Plant purchases and final Plant Creature composition, final ordinary dice, stored/pending Mulch dice. Summary reports distinguish purchase rate, Battle usage, win share with seed-level bootstrap intervals, and final-Plant signature diversity. The report cannot classify the card as balanced or overpowered: the distinct populations trained under the two effect versions may differ for reasons other than the effect change.

## Launcher and outputs

Read `doc/SIMULATION_LAUNCH_ARCHITECTURE.md` before changing either launcher or runner. The Python launcher compiles **once** with `./gradlew --no-daemon writeSimulationRuntimeClasspath`. Each of four training processes and two evaluation processes has its **own long-lived JVM** running many games. No repeated Gradle game launches.

From top-level `leaf`:

```bash
python3 bin/r2_sapping_discovery \
  --generations 4 --population 6 --games 18 --holdout-games 100 \
  |& tee ~/Downloads/r2-sapping-discovery.log
```

For a very small end-to-end smoke test (not scientifically informative):

```bash
python3 bin/r2_sapping_discovery \
  --generations 2 --population 4 --elites 1 --games 8 --holdout-games 20 \
  |& tee ~/Downloads/r2-sapping-smoke.log
```

- `manifest.json`: pinned catalog and source SHA-256, Grove, rules, variant implementations, seeds, policy constraints and budget.
- `original/{targeted,unrestricted}/generation-XXX/`: checked generation scores and ALL candidate weights; `.complete` SHA-256 checkpoint. **If a generation is corrupted, it and all later generations are invalidated and recreated**. A process interruption between generations is resumable.
- `*/champion.weights`: champion from each final generation (training-phase product, not validated superiority).
- `{original,different-die}/holdout/holdout.tsv`: complete per-player matchup observations, with checksum checkpoint.
- `summary.json`, `ASSESSMENT.md`, separate JVM logs, `build.log`, and a single `*.tgz` next to the run directory.
- Progress includes generation/matchup progress, overall batch progress, elapsed time, and projected overall ETA. Re-running the **identical** command skips verified completed generations and evaluations.

Outputs are placed under `output/experiments/r2-sapping-discovery/<fingerprint>/` by default. Different inputs produce different experiment IDs; manifests refuse incompatible reuse. Upload the path printed by `UPLOAD THIS FILE:`; the terminal log alone does not contain gameplay observations.

## Interpretation for R3

R2 can establish that particular **candidate Buy strategies** buy and activate Sapping and perform well or poorly against a small opponent portfolio. It cannot tell if the experimental effect is overpowered, because it does not isolate card ownership from the strategy used to acquire it, cannot simulate tactical policies that exploit its precise targeting beyond Human Baseline, and does not search deliberately for best responses to each champion. R3 should add countersearch, repeated independent seeds, and controlled effect interventions (same policies / same acquisitions when feasible) before upgrading the status from **Investigate**.

We should explicitly measure whether the focused champion ever bought or activated Sapping. If it didn't, treat the focused search as failed. Do not interpret its wins as evidence *for* Sapping.
