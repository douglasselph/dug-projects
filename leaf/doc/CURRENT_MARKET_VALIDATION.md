# Current Market Validation

This patch adds two experiments for validating the **exact current Plant market** rather than searching alternate market configurations.

## Reporting architecture

The current-market sanity and confirmation experiments use structured
machine-readable telemetry emitted by Kotlin and consumed by the Python
market package.

**Rule: Human-readable evaluator output is for people. Research reports
must consume structured machine-readable telemetry.**

The public commands remain:

```bash
bin/experiment_current_market_sanity
bin/experiment_current_market_confirmation
```

Both are thin launchers into the shared Python market-experiment runner.
They do not parse evaluator console output.

## Design

Both experiments intentionally isolate Buy strategy:

- Buy is the only learned policy family.
- All other decisions remain Human Baseline.
- Every Buy learner starts from **zero learned weights**, using the current Buy policy file only as a card-manifest/schema template.
- Training and held-out evaluation use random legal Groves so all 36 market cards receive opportunity.
- 2p, 3p, and 4p are tested separately.
- Held-out seed/Grove schedules are shared across learners within a player count.
- The exact current Plant and Round resync files are archived with results.

This directly addresses the concern that prior "fresh" market learners inherited the existing 4p Buy champion as their starting point.

## First: sanity check

```bash
bin/experiment_current_market_sanity
```

Defaults:

- 3 learners per player count
- 5 generations
- population 8
- 35 training games per candidate
- 500 held-out matched samples per learner

Purpose: quickly determine whether the previously observed complete rejection of the Vine-9 slot reproduces from genuinely neutral starts.

## Then: long confirmation

```bash
bin/experiment_current_market_confirmation
```

Defaults:

- 5 learners per player count
- 8 generations
- population 10
- 50 training games per candidate
- 2,500 held-out matched samples per learner

Purpose: confirmation-scale current-market validation after the sanity screen.

## Reports

Each experiment creates:

- `reports/card-purchases.tsv` — learner-by-card exposure and purchase detail
- `reports/card-summary.tsv` — aggregate card demand, exposure, rates, and learner participation
- `reports/slot-summary.tsv` — aggregate demand and exposure by Root/Vine/Flower cost slot
- `reports/zero-purchase-cards.tsv` — cards purchased zero times by every learner for that player count
- `reports/one-learner-only-cards.tsv` — cards purchased by exactly one learner
- `reports/vine-9-summary.tsv` — focused Vine-9 view
- `reports/learner-win-shares.tsv` — held-out control and learner win shares
- `reports/card-opportunity-summary.tsv` — per-card affordability, graftability, legality, selection, and pass/Done diagnostics
- `reports/slot-opportunity-summary.tsv` — the same diagnostics aggregated by type/cost slot
- `reports/card-opportunity-by-round.tsv` — opportunity and selection timing by Cultivation round
- `reports/README-FIRST.txt` — interpretation and provenance guidance

These reports are produced from validated `leaf.market-evaluation` JSON,
not from evaluator console text.

A zero from one learner is not a problem. Repeated zero purchase across successful independent learners, especially an entire legal slot such as Vine 9, is a meaningful balance signal.

These experiments remain Learned-vs-Human and therefore establish **economic market health**, not full expert-vs-expert strategic irreducibility. That is appropriate for the immediate task of deciding whether the physical card market can be rebuilt around the current costs/effects.
