# D1 — Buy disagreement measurement contract

This contract defines comparison semantics only. D2 adds safe shadow policy execution; D3 adds event capture. **D1 does not change gameplay or trained weights.**

## Unit of observation

One `ChoosePurchaseRequest` game state, evaluated hypothetically by both Human Baseline and frozen Buy Expert. No comparison of different-game purchases constitutes a disagreement observation. A Buy phase can produce multiple observations as the actual state changes after purchases.

Capture origin (`expert_state` vs `baseline_state`) and report these populations separately. Distinguish the factual acting policy from the hypothetical observer; do not treat correlated decisions within games as independent games.

## Decision identity and categories

The item is either `Done`, a Plant identified for reporting by its displayed `title` (with current CSV `name`/ID saved separately), or a die identified by sides. Same title with renumbered ID is the same **reported item**; catalog/effect fingerprints must distinguish altered effects or costs. Duplicate instances of the same named Plant count as the same *choice of item*, not as a specific physical instance.

Each observation receives exactly one category:

- `BOTH_DONE`: both end buying
- `SAME_ITEM`: same named Plant or same die size
- `DIFFERENT_ITEM`: two different items, including Plant vs die
- `EXPERT_BUYS_BASELINE_DONE`: expert buys, baseline stops
- `BASELINE_BUYS_EXPERT_DONE`: baseline buys, expert stops

`overall disagreement rate = (different item + expert buys/baseline done + baseline buys/expert done) / all comparable decision observations`.

Payment is **not** part of the D1 item comparison. The same item paid for with different combinations of dice and Critters remains an item agreement; a later payment-diagnostic field may classify this separately. A card may be purchased using **multiple dice combined with Critter values**; never assume a separate die pays for exactly one item.

## Per-card denominators

For each Plant title, every comparable observation is classified `both`, `baseline_only`, `expert_only`, or `neither`. Include cards with zero selections in reporting.

- `disagreement_when_either = (baseline_only + expert_only) / (both + baseline_only + expert_only)`
- `expert_rejection_given_baseline = baseline_only / (both + baseline_only)`
- `expert_only_given_expert = expert_only / (both + expert_only)`

Zero denominators produce missing/null, **never zero percent**. `neither` is counted for completeness but not included in the card-conditional disagreement denominator. All figures are **policy preference**, not card causal value.

## Outcome / later comparisons

Only the acting player's real actions affect games. Keep per-game final VP, win/tie/loss, strike-row wins and exact surviving/removed named-card and die multisets for D4–D7. These are **descriptive outcome associations**. Do not attribute a win or VP difference causally to a single card without separate controlled intervention or matched continuation. Shadow decisions must not alter RNG, baseline cached plans, or game state (D2/D8 tests).

## Event-schema requirements for D3

At minimum: schema_version, source/catalog/effect fingerprints, game seed and ID, player count, game/seat/player policy, learner identity, observer origin, Cultivation round and Buy choice index, actual and shadow item choices (type, title, ID, cost or die size), category, available legal alternatives, dice and Critter resource snapshots, and optionally actual payment when recorded. Retain decision-level events for reproducibility and aggregate reconciliation; never fabricate payment or alternative data absent from the request.
