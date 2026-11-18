# Compost — observed Build decisions against the saved legacy baseline

**Implementation checkpoint: 18 November 2026.** The fixed-seed A/B report runner is implemented and its Python unit tests pass. **Real-game A/B result: PENDING** until the updated Kotlin project runs on Ubuntu. Do not report a numeric change in Compost use, strategy quality, or balance from synthetic test fixtures.

## The immutable legacy reference

The earlier Ubuntu capture was `leaf-human-baseline-build-20261116-214822.tgz` with status `PASS_OBSERVATION_ONLY` (4 games, 2 players, 120 Build decisions, 18 Round resource lineages). It uses normal v38 Three Engagements, the first-game nine-card Grove, base mechanical seed `163000` and strategy seed `263000`. It preserves the historical fixed-priority and percentage-gate policy. The individual TSV rows are **not bundled in the supplied source tarballs**, so this comparison must read the original capture archive from `~/Downloads` (or an explicit alternate path).

The new code changed only ordinary Round Compost's mathematical scorer/target selection and removed its normal willingness gate; other Build action families are not yet on a unified scoring scale. The current snapshot runner is still safe to use. Its manifest and log now identify the *current* valuation instead of incorrectly describing every new capture as unchanged legacy behavior. The historical snapshot is never modified.

## One command from inside `leaf/`

```bash
bin/compare_human_baseline_build
```

With the legacy capture stored somewhere else:

```bash
bin/compare_human_baseline_build /path/to/leaf-human-baseline-build-20261116-214822.tgz
```

The wrapper rejects a missing baseline **before** launching Gradle, runs `bin/capture_human_baseline_build` for the same four seeds using the project's focused Kotlin test and direct-Java launcher, and then runs the Python analysis. It writes `comparison.md`, `comparison.json`, `aligned-build-choices.tsv`, and an execution log under `~/Downloads`; the snapshot launcher writes its standard current-policy `.tgz`. The wrapper also packages the comparison and both input captures into **one verified upload-ready `.tgz`** in `~/Downloads`, printing `UPLOAD THIS FILE:`. No source changes or updates to past results are made during this command.

To compare two completed snapshots without rerunning simulations:

```bash
python3 bin/compare_human_baseline_build.py \
  --before ~/Downloads/leaf-human-baseline-build-20261116-214822.tgz \
  --after ~/Downloads/leaf-human-baseline-build-NEW-TIMESTAMP.tgz \
  --output-dir ~/Downloads/leaf-compost-build-manual-comparison
```

The comparison **refuses** archives with failed capture status, differing rules, standard-round configuration, number of players/games, Grove, seed schedules, official CSV hashes, or scorer inventory. Source/policy hashes are *expected* to differ; their change is why this comparison is being done. Exactly identical random number consumption after strategy divergence is **not** assumed.

## Evidence and interpretation

The report measures:

- **Legal opportunities** to select normal Round Compost, and chosen Compost per legal offer. The old 75%/50%/10% gate sometimes suppressed an *engine-legal* Compost choice before scoring; scorer appearances are not a legal opportunity denominator.
- Counts of Draw, Compost, Plant activation, Water, Sunlight, Mulch, other Round effects, and Support/Done, both overall and at paired ordinal Build decision positions.
- **Scored** Compost-versus-Draw priority margins when both survived any willingness gates. The report distinguishes these from real legal offers.
- Changes in action categories, same recorded legal offers and first observed Build divergence per game, with a TSV for manual inspection.

**Important limit:** matching game/round/player/within-round ordinal and the same legal actions is *not* a same-state decision fixture. Different prior actions, support use, strategy-RNG call count, purchases and later dice can produce different board states. Do not call a changed action “sensible” purely because of usage counts or score rank. The historical TSV lacks actual Hand faces, Supply/Discard ordering and target calculations required for reliable state-by-state valuation judgments.

For concrete judgment, inspect the new `scored-main-candidates.tsv` in the post-change capture, then create targeted same-state Kotlin tests: early D12→D20 low face; late upgrade with no future use; upgrade delayed behind Supply; missing next-size Trash; lost current Buy tier; multi-target selected die matching the forecast. These focused tests exist in `CompostLifetimeValuationTest`, `HumanBaselineCultivationStrategyTest`, and `HumanBaselineEffectStrategyTest`, but full Gradle acceptance remains pending. Even if the observational result looks promising, do not treat that alone as reason to migrate all Plant scoring.

## Verification performed in assistant environment

- Python archive comparison tests: **5 passed**, including seed/catalog/status mismatch rejection, legal opportunity counting, ordinal alignment and report output.
- Standalone Kotlin forecast+target smoke: **passed**; it does not instantiate a complete game.
- Full Gradle and real two-archive A/B: **not run** (Gradle 8.13 unavailable offline; individual old snapshot TSV not supplied in this conversation).

## Project-root ZIP convention

Future source patches must have ZIP entries like `bin/compare_human_baseline_build.py`, `doc/...`, `src/...`, or `README.md` **without a `leaf/` prefix**. Unzip inside the already-open `leaf/` directory using `unzip -o ~/Downloads/<archive>.zip`.
