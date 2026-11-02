# Market Buy-Opportunity Diagnostics

This telemetry exists to answer a narrower question than purchase/exposure:
**when a Plant is not bought, was it inaccessible, unaffordable, ungraftable, or consciously passed?**

The current-market evaluator records a typed `BuyDecision` Chronicle entry at every affected player's Buy decision, including terminal decisions where no legal purchase exists. Human-readable console output is not used for research data.

For every Plant stack still physically available at that decision, the entry records:

- current effective cost;
- remaining physical supply;
- current total Buy purchasing power (Hand dice + current Critter values);
- whether the Plant is affordable;
- whether it has a legal graft placement;
- whether it is a legal purchase now;
- number of purchases already made in this Buy phase;
- Cultivation-round number;
- whether the player purchased something, voluntarily ended the Buy turn, or had no legal items;
- the selected item when a purchase was made.

The structured market JSON schema v2 aggregates those typed events separately for CONTROL and LEARNED. It preserves raw counts for market availability, affordability, graftability, legality, selection, voluntary Done while legal, legal opportunities after prior purchases, legal competition from a higher-cost Plant, purchasing-power sums, and Cultivation-round buckets.

## Reports

`card-opportunity-summary.tsv` is the primary per-card diagnostic. Important columns:

- `market_card_opportunities`: the card had supply at a Buy decision;
- `affordable_rate`: fraction of those opportunities where current purchasing power met its cost;
- `graftable_rate`: fraction where the Plant Creature had at least one legal placement;
- `legal_rate`: fraction where both affordability and graftability made it a legal choice;
- `selection_per_legal`: actual purchases divided by legal opportunities;
- `player_done_while_legal`: strong evidence of deliberate rejection/saving; the player ended its Buy turn while this card was legal;
- `legal_with_higher_cost_plant`: times this card was legal while some more expensive Plant was also legal;
- `avg_purchasing_power_when_market`: context for how far the economy typically was from the card's cost.

`slot-opportunity-summary.tsv` sums the same card-opportunity counts by Plant type/cost slot. The denominators are **card-opportunities**, not unique Buy turns, so use it to compare slots rather than to count Buy phases.

`card-opportunity-by-round.tsv` shows market, affordable, legal, and selected counts by Cultivation round. This distinguishes a tier that only becomes reachable very late from a card that is reachable early but rejected.

## Interpretation examples

A low-purchase tier with a very low `affordable_rate` but a high `selection_per_legal` is primarily an **economic/access** signal: players usually cannot buy it, but often do when they can.

A high `affordable_rate` and `legal_rate` combined with a very low `selection_per_legal` is a **preference/value** signal: the card is available to buy but the policy usually chooses something else.

A high `player_done_while_legal` rate is especially useful for detecting saving behavior. If a 4p player repeatedly ends Buy while a Flower 14 is legal, then later purchases Flower 17, the Flower-14 tier may be strategically squeezed by the opportunity cost of delaying the 17-cost payoff.

Low `graftable_rate` identifies a different structural issue: the card may be affordable but blocked by Plant Creature placement constraints.

## Resume behavior after schema upgrades

The confirmation runner considers a completed evaluation stale when its JSON schema is older than the current market schema. Existing training weights remain reusable. Therefore, after installing this telemetry on top of a completed schema-v1 confirmation, run the same output root with normal resume enabled. Training steps will remain skipped, while evaluation steps are rerun to produce schema-v2 diagnostic JSON and reports.
