# Chronicle and Output

This document explains how to inspect what actually happened in a Leaf & Let Die engine run and how generated diagnostic/experiment artifacts should be organized.

## 1. The Chronicle already records the game

Every `Game` receives its own `GameChronicle`. Production code records typed `GameEntry` values while the game executes. The current event vocabulary includes:

- Round reveal and completion;
- die rolls and Roll Rewards;
- opening draws;
- Main Actions and Support Actions;
- resolved Plant, Round, and Wisp effects;
- optional scored-decision reasoning;
- Buy Order, purchases, and grafts;
- Battle Order and Strike results;
- Wounds and Doom;
- Refresh and Cleanup;
- upgrades and Trashed dice;
- final scores, winners, and game completion.

This means the core recording mechanism is already suitable for the basic question, "Did a complete game run, and what did everyone do?"

The Chronicle itself is intentionally **in memory**. It should not write files. Keeping recording separate from persistence lets the same engine be used by unit/integration tests, one-game diagnostics, and large simulations without forcing disk I/O on every game.

## 2. Run a complete Mechanical Control game

Use:

```bash
./gradlew runMechanicalGameSmoke
```

The smoke runner uses four **Mechanical Control** players. Mechanical Control is deterministic, legal, and deliberately unsophisticated. Do not use this run to judge balance or the quality of Human Baseline strategy. Its purpose is simpler:

> Prove to yourself that the complete game can run from setup through final scoring, and inspect the chronological evidence of what happened.

The default seed is `13579`. To run another deterministic game:

```bash
./gradlew runMechanicalGameSmoke -PsmokeSeed=24680
```

Reusing the same seed should reproduce the same mechanical game for the same code revision.

## 3. Where the files go

The default run writes:

```text
output/
  smoke/
    mechanical-control/
      seed-13579/
        summary.txt
        chronicle.txt
```

`summary.txt` is the short answer: configuration, rounds completed, Chronicle entry count, final scores, and winner(s).

`chronicle.txt` is the detailed chronological record. It is line-oriented on purpose so it is easy to open in any text editor, search, diff, or inspect beside the code. Visible line IDs are round-local: `01.001`, `01.002`, ... for Round 1, then `02.001`, `02.002`, ... for Round 2. The typed Chronicle still keeps its original globally increasing `GameEntry.sequence`; only the text renderer resets the visible counter at each `RoundRevealed` event. The current format is diagnostic and can be improved later without changing the typed Chronicle data underneath it.

The top-level `output/` directory is generated material and is listed in `.gitignore`.

## 4. What to look for in a smoke Chronicle

For a first pass, do not worry about whether the players made good choices. Mechanical Control is not intended to be smart. Look for structural sanity instead:

1. The game reveals and completes the expected number of rounds.
2. Every player receives opening dice and performs actions.
3. Cultivation progresses through Build, Buy, grafting, and Cleanup.
4. Battle establishes an order, takes Main/Support actions, resolves all three Strikes, runs Doom, and cleans up.
5. Card and Wisp effects appear when triggered rather than silently disappearing.
6. Purchases and grafts correspond to game activity.
7. Dice/resources continue cycling instead of the engine getting stuck.
8. Final scores and winner(s) are recorded.
9. The Chronicle ends with `GAME COMPLETE`.

A strange strategic choice is not necessarily a bug in this run. An impossible state, missing phase, repeated non-progressing sequence, exception, or failure to reach `GAME COMPLETE` is much more relevant.

## 5. Existing automatic whole-game checks

The same general engine path is also exercised automatically by:

```text
src/integration/kotlin/dugsolutions/leaf/integration/v35/sanity/game/WholeGameSanityTest.kt
```

Run it with:

```bash
./gradlew integrationTest --tests '*WholeGameSanityTest'
```

The current test suite includes a fully scripted tiny game plus larger seeded Mechanical Control games. Those tests assert lifecycle/scoring invariants but do not exist to produce a readable external transcript. The smoke runner and the test therefore serve different purposes:

- **WholeGameSanityTest** — automatic pass/fail correctness check.
- **runMechanicalGameSmoke** — human-inspection run with persisted output.

## 6. Output organization for future experiments

Do not put every report directly in `output/`. The first folder should identify the kind of work being run. A useful convention is:

```text
output/
  smoke/
    mechanical-control/
  card-focus/
    <card-or-plan>/
  wisp-windfall/
    <variant>/
  matchup/
    <matchup-name>/
  tournament/
    <tournament-name>/
```

Within an experiment, use deterministic identifiers such as seed ranges, configuration names, or an explicit run ID. The purpose is to make it obvious months later what created a report.

For high-volume experiments, do **not** automatically persist a full Chronicle for every game. That would create huge output and unnecessary I/O. Prefer aggregate reports, and optionally preserve full Chronicles only for selected sample games, anomalies, or reproducible seeds that need investigation.

## 7. Typed data versus presentation

`ChronicleTextRenderer` is deliberately only a presentation layer. Integration assertions and statistical analysis should continue to work from typed `GameEntry` data.

That gives two useful freedoms:

- the text report can become prettier without breaking tests;
- the typed event schema can support statistical analysis without forcing humans to read raw Kotlin `data class` output.

As the reporting layer matures, additional renderers such as CSV/JSON summaries can be added without changing game mechanics.
