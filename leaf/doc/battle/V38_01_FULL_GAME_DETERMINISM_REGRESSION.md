# v38-01 full-game deterministic Battle-routing regression

## Purpose and status

This checkpoint adds a **simulationTest** suite covering every supported player count
(2, 3, 4) under **both selectable Battle structures** (`THREE_ENGAGEMENTS` and
`LEGACY_WHOLE_BATTLE`). Each case runs the full standard **3/2/2** sequence
(7 Cultivation + 3 Battle = 10 Rounds) **twice** with the same mechanical and strategy
seeds, freshly constructing the mutable Game each time within a reused application.

**Do not confuse created tests with executed tests.** The new full-game suite was **not
executed in the packaging environment**: the Gradle 8.13 distribution is absent,
and `services.gradle.org` is unreachable. The first task on Ubuntu is to run the
commands below and address any reported failures before treating the v38 engine as
regression-verified. A standalone Kotlin compile/run of `BattleStructureSettings`
did pass (missing file default, both explicit names, legacy name rejected); that
is a configuration smoke test **only**, not full-game evidence.

## Matrix

| Players | Ruleset | Rounds | Fixed seed reruns |
|---:|---|---:|---:|
| 2 | THREE_ENGAGEMENTS | 10 | 2 |
| 2 | LEGACY_WHOLE_BATTLE | 10 | 2 |
| 3 | THREE_ENGAGEMENTS | 10 | 2 |
| 3 | LEGACY_WHOLE_BATTLE | 10 | 2 |
| 4 | THREE_ENGAGEMENTS | 10 | 2 |
| 4 | LEGACY_WHOLE_BATTLE | 10 | 2 |

The tests use the official 36-Plant/Wisp/Round catalogs and the legal first-game
nine-card Grove, with `mechanicalControl()` player decisions for a deterministic
rules-engine baseline. The **full Wisp deck** is retained, including Wisp's Resolve.
This matrix is not a tactical-quality or game-balance evaluation. Separate
Wisp-specific deterministic tests cover forced use of Wisp's Resolve.

## Assertions (per run)

- 10 completed Rounds; precisely 3 Battle Rounds; final scoring and at least one winner.
- Exactly three Strike resolutions per Battle; no duplicate or missing rows.
- `THREE_ENGAGEMENTS`: three independent Doom events per Battle; each contains
  at most one die face value and one Strike row. Three row-cleanup events per
  player, plus one final-refresh cleanup event per player.
- `LEGACY_WHOLE_BATTLE`: one whole-Battle Doom and one Battle-cleanup event
  per player, retaining the historical execution path.
- After the final Battle, all player Dice Hands are empty.
- Replaying the same scenario within the same application produces **identical**
  immutable full-game summaries and typed Chronicle event histories. This also
  checks that mutable state did not leak from the first game into the second.

These are *within-ruleset* replay checks. Outcomes under the two different
rulesets are deliberately **not** asserted to be the same.

## Run on Ubuntu

From the top-level `leaf` repository:

```bash
set -o pipefail
./gradlew simulationTest \
  --tests '*V38BattleStructureFullGameRegressionTest' \
  --rerun-tasks |& tee ~/Downloads/leaf-v38-full-game-regression.log
```

`--rerun-tasks` makes the outcome independent of Gradle's prior test up-to-date
state. A successful JUnit report should contain **six dynamic cases** and a
`V38_FULL_GAME_PASS` line for each. Gradle writes JUnit XML under
`build/test-results/simulationTest/` and the HTML report under
`build/reports/tests/simulationTest/`.

Then run the complete test groups:

```bash
set -o pipefail
./gradlew test integrationTest simulationTest \
  |& tee ~/Downloads/leaf-v38-all-regressions.log
```

If any test fails, preserve the full console output and the failing JUnit XML,
including the specific player count and Battle structure. Avoid launching large
balance research runs while correctness is unresolved.

## Known architectural limitation

`THREE_ENGAGEMENTS` currently runs through the simulation runtime provider;
main-only classpaths do not contain that executor. This matrix intentionally
runs under `simulationTest`, with its simulation classpath. The old `test` and
`integrationTest` source sets still test the legacy production executor where
explicitly selected. Moving the adopted executor into `main` is a separate
architecture change, not part of this regression-only checkpoint.

## Scope of this patch

Only a new test class and this document are added. No game rules, card effects,
configuration, policy weights, round schedulers, data files or experiment launch
scripts are changed.
