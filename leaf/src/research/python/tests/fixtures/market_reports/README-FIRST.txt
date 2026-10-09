CURRENT MARKET REPORTS — READ FIRST

These reports are generated from typed Kotlin market-evaluation JSON loaded and
validated by Python.  No market fact is reconstructed by parsing human-readable
Kotlin console output.

Primary reports:
  card-purchases.tsv
      Per learner/card raw learned exposure and purchase counts, plus control
      purchases and held-out outcome context.
  card-summary.tsv
      Per-card aggregation across independent learners at each player count.
  slot-summary.tsv
      Per-slot aggregation across the four cards in each legal type/cost slot.
  zero-purchase-cards.tsv
      Card-summary rows with zero learned purchases across all evaluated learners.
  one-learner-only-cards.tsv
      Card-summary rows purchased by exactly one evaluated learner.
  vine-9-summary.tsv
      Card-summary rows for the VINE / cost-9 slot.
  learner-win-shares.tsv
      CONTROL and LEARNED held-out win shares directly from structured results.
  card-opportunity-summary.tsv
      Buy-decision opportunity diagnostics: market availability, affordability,
      graftability, legality, actual selection, and explicit pass/Done behavior.
  slot-opportunity-summary.tsv
      The same opportunity diagnostics aggregated by Plant type/cost slot.
  card-opportunity-by-round.tsv
      Market/affordable/legal/selected opportunity counts by Cultivation round.
  card-substitution-summary.tsv
      When a Plant was legal but rejected, what was selected instead, aggregated
      across learners. Includes exact Plant identity or die identity/cost.
  card-substitutions-by-learner.tsv
      The same rejected-legal substitution evidence separated by learner.

Semantics:
  - grove_exposures and total_grove_exposures use LEARNED-side direct Grove
    exposure counts from typed telemetry.
  - purchase/exposure is purchases divided by the corresponding learned exposure.
  - a zero exposure denominator is rendered as NA, never as a fabricated 0.0.
  - best_learner_win_share_pct means the best held-out win share among learners
    that actually purchased the card.  It is NA when no learner purchased it.
  - raw purchase and exposure counts are retained in every aggregate report.
  - opportunity reports distinguish "in the market" from "affordable",
    "graftable", "legal now", and "actually selected". This is specifically
    intended to separate tier-access/economic problems from card-preference problems.
  - player_done_while_legal counts a strong rejection signal: the player ended
    its Buy turn while that Plant was a legal purchase.
  - substitution reports condition on the target Plant being legal and not
    selected, then report the actual alternative purchase (or PLAYER_DONE).

Before any report is written, the writer validates complete 36-card market
membership, unique identities, legal four-card slots, purchase/exposure
consistency, per-card purchase reconciliation, slot reconciliation, and
whole-market reconciliation.  Invalid inputs fail loudly instead of producing
plausible-looking TSV output.
