# Current Market Validation

This patch adds two experiments for validating the **exact current Plant market** rather than searching alternate market configurations.

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

- `reports/card-purchases.tsv` — learner-by-card purchase totals
- `reports/card-summary.tsv` — aggregate card demand and number of learners buying each card
- `reports/slot-summary.tsv` — aggregate demand by Root/Vine/Flower cost slot
- `reports/zero-purchase-cards.tsv` — cards purchased zero times by every learner for that player count
- `reports/README-FIRST.txt`

A zero from one learner is not a problem. Repeated zero purchase across successful independent learners, especially an entire legal slot such as Vine 9, is a meaningful balance signal.

These experiments remain Learned-vs-Human and therefore establish **economic market health**, not full expert-vs-expert strategic irreducibility. That is appropriate for the immediate task of deciding whether the physical card market can be rebuilt around the current costs/effects.
