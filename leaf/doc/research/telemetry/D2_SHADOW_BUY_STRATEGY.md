# D2 — Shadow Human Baseline Buy policy

`ShadowBuyStrategy` is an **opt-in BuyStrategy decorator**. It compares a real
strategy's choice against a separately instantiated Human Baseline in the **same**
`ChoosePurchaseRequest`, emits the D1 classification, and returns the real
choice/payment unchanged. No observations are persisted yet; that is D3.

## Continuation and safety

- Install one decorator **per player**; never share planners across players.
- Give `humanBaseline` an explicit shadow-only seed, independent of mechanical
  and player strategy random sources.
- The shadow is a stateful Buy planner. If item **and payment** match the actual
  policy, the next purchase can reuse its planned continuation.
- If they differ, a new shadow planner is instantiated at the **next actual
  request**, rather than projecting hypothetical choices into the game.
- A Buy phase starting at zero, a skipped payment, or an unexpected purchase
  count also resets the shadow.
- This is an *on-policy* comparison: the reference is asked what it would do in
  the expert's observed state, not in an imaginary parallel game.
- Individual shadow resets restart the separately seeded RNG. This is a
  deterministic reference but not a replay of a Human Baseline player's
  historical random stream; D3/D8 reporting should document this distinction.

## Boundaries

D2 provides a reusable observer and focused lifecycle tests only. It does not
wire into the evaluator yet, record contextual snapshots, or run simulations.
Those are D3–D10. In particular, installing the decorator is opt-in, so normal
simulations retain their current behavior.
