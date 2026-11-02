# D7 — Final Development Reports

D7 is a **read-only reporting layer** over D4/D5 `PlayerDevelopmentSnapshot` records. It does not attach trackers, start games, or change policies. D9 will connect it to the evaluator, attaching each tracker **at game setup** and capturing a snapshot at the game's end.

## Inputs

One `PlayerDevelopmentResult` per player/game/policy supplies game ID, player ID, player count, control/learned policy, learner ID, win/loss (or unknown), final VP, Strike wins, and a D4/D5 snapshot. `generate(results, catalog)` or `write(results, directory, catalog)` accepts an optional **complete official Plant catalog** of `DevelopmentPlantIdentity`. Supply the catalog in production to ensure that a card never purchased still gets explicit zero rows in every observed cohort; without it, the report can only enumerate cards present in at least one supplied snapshot.

Policies/learners and WIN/LOSS/UNKNOWN are **separate cohorts**, plus ALL (every game of that learner including losses). Individual observations retain the game and player identifiers for traceability. ALL includes both winning and losing games. These are observational correlations; card ownership does not demonstrate causal contribution to a win.

## Six TSV files

- `development-players.tsv`: one row per observed player with VP, strike wins, Plant counts, die counts, removals, and upgrade count.
- `development-plants.tsv`: **one row per actual Plant copy**, display name, current ID, cost, graft side/coordinates, final fate (`GRAFTED`, `SNIPPED`, etc). All acquired copies remain visible even when removed.
- `development-dice.tsv`: **one row per recorded die ownership identity**, acquired and final sides, final fate (`OWNED`, `DOOM`, `TRASH`, `SWAPPED_OUT`, `STOLEN`, `MULCH_STORED`, etc), and upgrade count. Records of transferred dice belong to the appropriate player's history.
- `development-card-summary.tsv`: per named card and learner/cohort, copies acquired, player games containing card, average copies/player, percentage of player games with card, surviving grafted copies, Snipped copies, and other removed. Repeats remain repeats; a player with three Queen's Blossoms contributes three copies but one acquiring-player game.
- `development-dice-summary.tsv`: D4/D6/D8/D10/D12/D20 rows with acquired-at-size counts, still-owned-at-end counts by final size, removals by final size, and upgrade counts. **The `upgrades_to_size` field is intentionally blank**: D4 tracks upgrade counts and latest size but not each intermediary upgrade target. `acquired_at_size` includes starting dice and die ownership received from transfers and may not be treated as purchase counts.
- `development-shape-summary.tsv`: final grafted Root/Vine/Flower averages plus acquired Plant, Snipped, retained die, upgrade, VP, and strike-win averages. Exact topology is preserved in the per-copy Plant report, not condensed into a single ambiguous shape index.

## Scope and cautions

- The complete catalog is mandatory for research-level **zero-acquisition** statements. D7 does not infer catalogs from `cardId` patterns.
- D4/D5 retains a graft's coordinates and side from the tracked `CreatureCard` object. D7 exports them as given; physical card placement details can be reconstructed from the per-copy rows.
- These reports describe **what was acquired or retained**, not each causal contribution to VP or why a card was purchased. Direct VP attribution is a separate future research task.
- `won`/`finalVp`/`strikeWins` must come from authoritative evaluated game outcomes, not be guessed from the final collection.
- Assets already present at tracker attach time are included as beginning inventory. The tracker cannot retrospectively reconstruct earlier purchases. Attach at game setup.
- `PlayerDevelopmentTracker` currently labels starting versus acquired dice internally, but the snapshot exposes final fate rather than a separately preserved acquisition origin. Accordingly D7 cannot distinguish a purchased die from a starting die without future provenance enrichment. Similarly transfers are ownership events and may be counted at both owners across distinct snapshots; that is intentional for **per-player history**, not a global production total.
- Source definitions and research fingerprints should accompany the run archive in D9. D7 output is deterministic given records and catalog.
- No evaluation runner is connected in D7; report files are emitted only when a caller explicitly invokes `PlayerDevelopmentReports.write`.

## Testing

`./gradlew test --tests '*PlayerDevelopmentReportsTest'`
