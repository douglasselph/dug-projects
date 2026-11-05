# v38-01 full-game deterministic Battle-routing regression

## Purpose and status

This checkpoint adds a **simulationTest** suite covering every supported player count
(2, 3, 4) under **both selectable Battle structures** (`THREE_ENGAGEMENTS` and
`LEGACY_WHOLE_BATTLE`). Each case runs the full standard **3/2/2** sequence
(7 Cultivation + 3 Battle = 10 Rounds) **twice** with the same mechanical and strategy
seeds, freshly constructing the mutable Game each time within a reused application.

**Completed evidence (November 5, 2026):** Ubuntu verification archive
`leaf-v38-verification-20261105-135919-2148522.tgz` (HEAD `3304fa6f1351376d249ab2be443a44532a8ed3cd`)
contains passing targeted/full JUnit results and both Gradle exit codes 0:
6/6 targeted dynamic cases, 1740/1740 unit tests, 88/88 integration tests, and
242/243 simulation tests. The only skip is `BaselineLongRunMemoryTest`,
an intentionally opt-in 3,000-game stress test. The strict checker accepts **only**
that exact exceptional skip; all other skipped/failed/missing cases remain a gate failure.
The replay/ruleset evidence is accepted as **V38-4 PASS**; this is not coverage of every special card interaction.

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

## November 5 verification handoff: strict Ubuntu runner

A follow-up patch strengthens `V38BattleStructureFullGameRegressionTest`:

- Compare each v38 Doom event against the exact **pre-Doom** row snapshot,
  proving the minimum-value group (not merely any one value) was Trashed.
- Confirm every Doom is confined to its just-resolved Strike and follows
  that Strike's `StrikeResolved` event. This pairs events by chronology so
  a Wisp-resolved *future* Strike does not break the assertion.
- Verify that every resolved row has one non-Refresh cleanup per player after
  Doom, with no pieces reclaimed again during the final Refresh.

The deterministic matrix remains **execution unverified** until Gradle runs.
This patch does not change the Battle rules, card CSVs, policies or engine.

For an automatic run on Ubuntu from the top-level `leaf` directory:

```bash
bin/verify_v38_full_game
```

This executes the targeted matrix and, if it passes, the complete
`test integrationTest simulationTest` matrix using `--rerun-tasks`. It retains:

- Both Gradle console logs and the actual JUnit XML copied immediately after
  each command (so the second suite cannot overwrite targeted evidence).
- The six exact dynamic-case identifiers with pass/fail status; full-suite test
  counts; missing/skipped/failing cases are **not** accepted as success.
- Git SHA/status if available, rulebook/official CSV and source hashes, Java/
  Gradle versions, timestamps and exit codes in a time-stamped directory.
- One `.tgz` in `~/Downloads` even when the gate **fails**, for upload and
  diagnosis. The wrapper prints `UPLOAD THIS FILE:` with its absolute path.

The JUnit checker has independent, dependency-free tests:

```bash
python3 -m unittest discover -s bin/tests -p test_verify_v38_junit.py -v
```

A passing wrapper concludes only that the **specified regression suite** ran;
additional forced-card edge-case tests remain under V38-5, and neither this
matrix nor its replay assertions establish game balance.

**Execution environment note:** in the November 5 packaging environment,
Gradle 8.13 was not installed, and the wrapper's attempt to download the
Gradle distribution failed due to DNS/network unavailability. The Python
checker tests passed (6/6). Do not reinterpret this as a Kotlin pass.
