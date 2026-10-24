# R3A — Official Plant screening foundation (exploratory)

## Purpose

One repeatable, resumable research request can screen any subset or all **36 official Plants**, with a targeted and an unrestricted Buy-policy population in each locked nine-card Grove.

**This is Stage 1 screening, not R3 countersearch or proof of effect causality.** Do not interpret a learned strategy containing a card as evidence that the effect caused its wins. Do not interpret no purchases, low effect resolutions, or low wins as proof a card is weak. The report deliberately keeps the balance assessment `Investigate` until the missing causal/counterstrategy evidence has been developed.

## Official inputs and Groves

Read the current `doc/SIMULATION_LAUNCH_ARCHITECTURE.md` before changing launch scripts. `bin/r3a_plant_screen` imports the official CSV catalog using the read-only R0 input audit parser. There must be exactly 36 distinct Plants, four in each nine cost/type slots. IDs in the CSV sometimes start with presentation prefix `z`; these are normalized to the stable IDs `Root_05_02`, `Flower_14_01` etc. R3A does not edit the CSVs, card effects, costs, VP scoring, Round cards or Wisp cards.

- Context 0 uses `FirstGameDefault`, replacing only the focus card's same-cost/type market slot if necessary.
- Contexts 1 and 2 replace **all other eight** cards with deterministically chosen alternatives from their own slot. The selection is SHA-256-derived from the market seed, card ID, slot, and context number.
- Every Grove is validated both by the launcher and by the Kotlin game runner. The manifest lists all nine IDs, names, costs, catalog hashes, source hash, launcher hash, seeds, and search settings.
- These diverse Groves are **exploration contexts**, not controlled one-variable comparisons; they are not independently sampled from a defined population of all Groves.

## Training and holdout

Two distinct training JVMs per card/context:

1. `unrestricted`: evolutionary Buy-policy population. Fitness is wins against an opponent mixture (Human Baseline, contemporaneous candidates, previous champion), **not** specifically the focused card.
2. `targeted`: same search, but with a training-only named-card weight boost, a minimum purchase-rate search constraint, and a small train-only acquisition bonus. For Flower targets, the initial specialist also has an early-Vine enabler preference (default `--flower-enabler-boost 8`) because Flowers cannot be grafted without a Vine. This is still a limited search scaffold and does not guarantee a usable specialist. A failed constraint is flagged as failed exploration. Targeting is not forced to purchase or use the card in evaluation.

One third JVM per card/context evaluates frozen finalists in these 2-player held-out matchups:

- targeted vs unrestricted
- targeted vs Human Baseline
- unrestricted vs Human Baseline

There is **no acquisition bonus in held-out matches**. Seat switches every game. The random-seed schedule is distinct from training; matchups use distinct held-out subranges. The same requested generation/population/games configuration is applied to every card. Unlike R2, no experimental Sapping variant is used: **only the official effect**.

**Important scope limitation:** the learned weights affect *Buy only*. The Human Baseline still makes activation, Build, Battle, Support, Wisp, and other decisions. A policy might buy a card but fail to exploit its effect. Even a successful strategy need not be a successful use of the card's effect.

## Running

Preflight without compiling or executing games:

```bash
bin/r3a_plant_screen --cards pilot --plan
bin/r3a_plant_screen --cards all --contexts 2 --plan
```

Pilot the three distinct effect styles:

```bash
bin/r3a_plant_screen --cards pilot --generations 2 --population 4 --elites 1 --games 8 --holdout-games 30 \
  |& tee ~/Downloads/r3a-pilot.log
```

`pilot` means Root Four More (`Root_05_02`), Bee-loved Bloom (`Flower_14_01`), and Sapping Snapdragon (`Flower_11_03`). Inspect their actual purchase and effect-resolution counts before a full run.

**Suggested next *exploratory* overnight pass only after the pilot validates on Ubuntu:**

```bash
bin/r3a_plant_screen --cards all --contexts 2 --generations 5 --population 8 --elites 2 \
  --games 25 --holdout-games 150 |& tee ~/Downloads/r3a-all-plants.log
```

This schedules 72 card/context studies, **176,400 games** (2,450 per card/context: 2*5*8*25 training games + 3*150 held-out games), not 176,400 independent observations of any one effect. Your actual runtime depends on system performance; the script estimates overall remaining time from measured completed units.

To run a subset use `--cards Root_05_02,Flower_14_01,Flower_11_03`. Each different budget/selection/context/source fingerprint produces a separate output directory. Repeating the **identical** command validates and skips completed stages. Do not modify source mid-run.

`--no-build` is only for an explicitly precompiled, fresh `build/simulation-runtime-classpath.txt` after the user built with `./gradlew --no-daemon writeSimulationRuntimeClasspath` (for example when repeatedly testing locally). Normal execution compiles **once** and uses direct Java for each training/evaluation session. Each JVM processes all games in that stage and is isolated from other stages; there are no per-game JVM or Gradle launches.

## Output and evidence sufficiency

The launcher produces:

- `manifest.json`: frozen source/catalog/Grove/seed/settings provenance.
- `Root_05_02/context-00/unrestricted` and `targeted`: generation checkpoints (`scores.tsv`, candidate weights, `generation.complete` and champion weight).
- `Root_05_02/context-00/holdout/holdout.tsv` and `.complete`: per-player/game observations, win share, total VP, battle Strike VP, total Plant VP, target acquisitions, target Cultivation/Battle/other `EffectResolved` events, grafted target count, exact named Plant acquisitions and final named Plant/dice shape. Capturing an event is not attribution of its downstream benefit.
- `matchup-summary.json`: per-card/context/matchup outcomes and approximate intervals, per-game target engagement, and most common final Plant/dice configurations.
- `all-plant-screen.csv`: exactly 36 rows (unrun cards marked `Not run`), with per-card evidence summaries.
- `ASSESSMENT.md`: readable table, plus explicit limitations.
- One final `.tgz`, printed as `UPLOAD THIS FILE`, containing the manifest, reports, checkpoint evidence, logs, and weights.

**Assessments:** For screened cards, `Investigate` is the only defensible status at this stage. `No concern demonstrated` requires competitive use *and* adequate effect attribution, exposure, and counterstrategy exploration. `Strong balance concern` requires replicated controlled disadvantage or robust dominance under well-tested opposing strategies. Neither is available from this first-pass screen. An unscreened card is `Not run`, not `Investigate`.

### Specifically missing before claiming effect adequacy

1. **Causal outcome contribution.** Total VP, Plant VP and Strike VP belong to the whole player. An `EffectResolved` event is a use count, not VP caused by that use. Future instrumentation should record direct effect consequences (die deltas, resources, score events) and controlled effect removal/matched continuation, where valid.
2. **Exploit/countersearch.** Today's `targeted_vs_unrestricted` is one independently trained opponent, not an adversarially trained best response. R3B must deliberately search for, and validate, counters.
3. **Tactical competence.** Human Baseline still controls Plant activation, so rare activation is also a *policy limitation*.
4. **Search coverage and inference.** An evolutionary search can miss good strategies, especially with the linear Buy scorer. Approximate sample intervals quantify fixed-policy game noise only, not training-seed, Grove-selection or 36-card multiple-comparisons uncertainty.
5. **Card attribution.** A specialist may win because of other cards or VP, not the target's effect. End-game card-specific VP attribution needs separate treatment, especially with conditional scoring.

## Test and verification contract

```
python3 -m unittest discover -s bin/tests -p 'test_r3a_plant_screen.py' -v
./gradlew --no-daemon simulationTest --tests '*R3APlantScreenOptionsTest'
bin/r3a_plant_screen --cards pilot --plan
```

Run the *tiny* three-card end-to-end pilot and re-run the identical command to verify checkpoint skipping and byte-identical `holdout.tsv` files. Corrupting a training checkpoint should trigger recomputation of its generation and descendants; never trust an invalid `.complete` marker. The launcher rejects conflicting manifest fingerprints and validates all held-out sample/seat rows and output hashes.

The core simulator rules, old R1/R2 launchers and historical scripts remain untouched.
