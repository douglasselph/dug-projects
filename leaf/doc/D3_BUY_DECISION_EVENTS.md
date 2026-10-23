# D3 — Structured Buy-policy disagreement events

`BuyDecisionEvent` is one immutable in-memory record for one **same-request**
Human Baseline vs actual Buy-policy recommendation. The D2 `ShadowBuyStrategy`
now takes an optional `onEvent` callback (in addition to its existing
`onComparison` callback). Events are emitted after the two purchase decisions
are obtained; the return value remains the actual policy's choice.

## Fields

- Observer-local monotonic sequence; acting player ID, player count, round and
  Cultivation round, decision phase and prior purchases in the same Buy phase.
- Named Plant or die recommendations and the D1 disagreement classification.
  Plant title is the analytical identity; mutable card ID remains for traceability.
- **Legal options** supplied to the two policies, and the broader market options
  (which may include unaffordable / ungraftable cards).
- Hand dice sides and face values, Bee/Worm counts and their effective values.
  `diceOnlyPurchasingPower` and `aggregatePurchasingPower` are derived from this
  captured snapshot, not from individual-die assumptions.

The event intentionally does **not** infer whether any observed market item was
competitive, whether the choice improved win rate, or what the player paid.
Those are separate analyses. The observer should be installed once per player.

## Scope

D3 supplies an event contract and callback **only**. It does not yet write
files, connect to the evaluation runner, tag run/learner fingerprints, or
produce summary reports; those are subsequent tasks. The host evaluation
runner will own persistence, run metadata, and archive generation.

Events come from the exact actual decision states; the shadow planner is
reinitialized after divergence as specified in D2. Diagnostics must be
explicitly enabled; normal gameplay and training remain unchanged.
