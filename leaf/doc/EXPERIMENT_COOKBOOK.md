# Experiment Cookbook

This file is a practical set of patterns for adding new balance experiments.

## 1. Experiment checklist

Before implementing an experiment, write down:

```text
Question:
Hypothesis:
Control condition:
Intervention:
What stays fixed:
What rotates:
Primary metric:
Secondary metrics:
Number of games:
Seeds:
Expected report:
Integration proof needed first:
```

If those fields cannot be filled in clearly, the experiment is probably still too vague.

---

## 2. Paired A/B experiment pattern

Use paired samples whenever an intervention can be compared against a matched control.

```text
for sample in 0 until gamesPerSeat:
    seed = baseSeed + sample

    run CONTROL with seed
    run INTERVENTION with the same seed

    compare outcomes
```

When seat can matter, rotate the affected/focused player:

```text
for sample:
    for focusSeat in seats:
        run matched control/intervention using the same sample seed
```

The experiment should avoid allowing one condition to consume a different mechanical RNG sequence merely because strategy tie-breaking changed. Use the separate strategy seed for that purpose.

---

## 3. Focused-card pattern

Use the existing `CardFocusExperiment` as the model.

Question:

> What changes when one player intentionally pursues Card X while everyone else plays Human Baseline?

Controls:

- same card/Grove availability;
- Human Baseline opponents;
- focus seat rotated;
- same sample seed reused across seat rotations.

Metrics:

- win share / winner rate;
- average VP delta;
- acquisition rate;
- copies purchased/surviving;
- activation count;
- card-attributable VP;
- Battle VP;
- seat breakdown.

Possible extensions:

- remove the card entirely;
- force all players to prioritize it;
- focus on a two-card combo;
- compare one copy versus two copies;
- run across multiple Grove compositions.

---

## 4. Early-Wisp windfall pattern

### Question

Does an extreme concentration of early 2s/Wisp rewards give one player an excessive persistent advantage?

### Recommended implementation split

Do **not** implement this only as a simulation shortcut that directly increments a Wisp count. The experiment is specifically about the consequences of an unusual Roll Reward history, so preserve the normal reward path when practical.

Recommended components:

```text
src/integration/.../sanity/...
    exact test proving the intervention produces the intended early rolls/rewards

src/simulation/.../experiment/wisp/
    WispWindfallExperiment.kt
    WispWindfallExperimentSpec.kt
    WispWindfallGameObservation.kt
    WispWindfallAggregator.kt
    WispWindfallResult.kt
    WispWindfallReport.kt

src/simulationTest/.../experiment/wisp/
    spec/aggregator/report tests
```

### Candidate spec

```kotlin
data class WispWindfallExperimentSpec(
    val numPlayers: Int = 4,
    val gamesPerSeat: Int,
    val affectedRounds: Int = 2,
    val forcedWispRewards: Int,
    val baseSeed: Long = 1L,
    val strategyBaseSeed: Long = baseSeed,
    val roundSetup: GameRoundSetup = GameRoundSetup.standard()
)
```

The exact low-level mechanism may need a more precise shape after inspecting where round-aware mechanical randomization can be injected without corrupting later seed matching. Prefer a reusable **experimental mechanical-randomness decorator/intervention** rather than special-casing Wisp logic inside `GameRunner`.

### Control pairing

For each sample and focus seat:

```text
CONTROL:
    ordinary mechanical stream

INTERVENTION:
    same base mechanical stream except the specified early reward-producing rolls
    are replaced/forced according to the experiment spec

AFTER INTERVENTION WINDOW:
    resume the ordinary matched mechanical stream as closely as the intervention design allows
```

The implementation should document precisely how matched randomness is preserved after forced results. If replacing rolls changes RNG consumption, the experiment must either consume the displaced mechanical RNG values or use a pre-generated event stream so later randomness remains paired.

### Metrics

Primary:

- focused win share delta versus matched control.

Secondary:

- final VP delta;
- Wisps gained in first two rounds;
- total Wisps gained;
- Wisps played;
- Wisps Trashed;
- unplayed Wisp VP;
- Wisp Reckoning encounters/forced discards;
- Butterfly ownership changes;
- Battle VP;
- Wounds caused/taken;
- result by seat;
- result by intervention intensity.

### Dose-response experiment

Run several values of `forcedWispRewards` rather than only the worst case. This helps determine whether the effect is approximately linear or whether there is a dangerous threshold.

---

## 5. Big-dice pressure pattern

Question:

> Can a simple strategy that prefers the strongest affordable die outperform the Human Baseline's mixed development?

Implementation approach:

- create a simulation strategy modifier that changes only Buy scoring;
- leave all other decision areas Human Baseline;
- rotate the modified player through seats;
- run matched seeds;
- compare against Human Baseline control.

Metrics:

- win share;
- final VP;
- average die-side total over time;
- dice purchased/upgraded;
- Plant count and Plant VP;
- Battle VP;
- support-resource acquisition/use.

The experiment is stronger if run across multiple Grove configurations.

---

## 6. Battle-order mirror pattern

This is best built in two layers.

### Integration proof

Construct one exact Battle state and show that the harness can replay it with players/columns permuted while all other relevant state remains identical.

### Simulation experiment

Sample many representative Battle states or whole games, replay matched variants with seat/order permutations, and measure deltas.

Metrics:

- Strike wins;
- Battle VP;
- Wounds;
- support resources spent;
- eventual game outcome where whole-game continuation is used.

The key is to control for opening die strength so “went first because they rolled better” is not mistaken for “going first caused the advantage.”

---

## 7. Wound leverage pattern

Track the first Wound event and subsequent outcome.

Avoid stopping at correlation. Add controlled variants where possible:

- same pre-Strike state with Wound threshold/outcome altered by a specific intervention;
- same player strategies and seeds;
- compare resulting score trajectory and final result.

Metrics:

- probability of winning after causing first Wound;
- probability of winning after receiving first Wound;
- Plant cards lost/flipped;
- bonus VP from Wounds;
- later Battle performance;
- comeback frequency.

---

## 8. Comeback / winner-predictability pattern

For every completed game, record snapshots at fixed checkpoints:

```text
after each round
before each Battle
immediately after each Battle
before final scoring
```

For each checkpoint record:

- current VP;
- Plant VP currently on board;
- Wisp VP currently held;
- current rank;
- score gap from leader;
- eventual winner(s).

Aggregate questions:

- how often does the current leader win?
- how often does last place win?
- what score gaps are commonly overcome?
- after which checkpoint does the game become highly predictable?

This is a better runaway-leader measure than simply asking whether winners tend to have been ahead earlier.

---

## 9. Experiment output principles

Every report should identify:

- experiment name/version;
- strategies used;
- intervention;
- player count;
- total games;
- seat rotation method;
- mechanical seed range/base;
- strategy seed range/base;
- Round setup;
- Grove/card selection method;
- primary metric;
- control result;
- intervention result;
- delta;
- per-seat breakdown where relevant.

For large experiments, retain aggregates by default. Enable detailed Chronicle/reasoning only for targeted diagnostic reruns, because high-volume logging can dominate memory and runtime.

---

## 10. From suspicious result to design decision

When an experiment finds something alarming:

1. Reproduce it with the same seeds.
2. Inspect a small number of representative game Chronicles.
3. Verify the relevant rule path with integration tests.
4. Repeat with seat rotation and multiple seed ranges.
5. Try an obvious counter-strategy.
6. Vary the intensity of the suspected exploit.
7. Test across Groves/Round structures.
8. Only then evaluate a rules change.
9. After a rules change, rerun the original experiment unchanged as a regression benchmark.

The simulator is most useful when it makes the design argument inspectable: **what was changed, what stayed constant, how big was the effect, and why did it happen?**
