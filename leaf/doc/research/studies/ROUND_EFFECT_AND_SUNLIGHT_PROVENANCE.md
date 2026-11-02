# Round Effect Choice and Sunlight Battle Provenance

This research instrumentation distinguishes Round-card exposure from action-decision opportunities.

For each effective Round effect slot, evaluation can now report:

- card/effect exposure,
- legal Main-action decision opportunities,
- illegal decision points,
- cases identified as blocked by an empty finite shared Grove supply,
- selections/uses,
- legal opportunities declined, and
- use per legal opportunity.

`RoundEffectChoice` records the effective experiment-aware Round effects and the legal high-level Main-action kinds at the decision boundary. For `GAIN_SUNLIGHT_TOKEN`, it also preserves the actor's current Sunlight count, remaining unrevealed Battles, whether Battle is next, and the selected high-level Main action. Final VP and win share are joined only during evaluation and are explicitly descriptive associations, not causal estimates.

Cultivation decisions after both normal Main Actions have already been consumed are not counted as Round-effect opportunities merely because Support/Done decisions still occur.

## Shared-resource blockage

The opportunity record contains a narrow flag for Round-effect illegality visibly caused by an empty shared Grove resource where that mapping is unambiguous (currently Water, Sunlight, Mulch, Worms, and the generic two-Critter gain). Other illegal states remain simply illegal rather than receiving a guessed cause.

## Sunlight Battle provenance

When a Sunlight Support funds an ordinary Battle Main Action, the existing immediate-die-effect provenance ledger is scoped to that action. Immediate die-value effects created inside the scope are marked `sunlightFunded`. At Strike resolution the normal contribution ledger can then identify whether that die contribution:

- contributed value to a resolved Strike,
- appeared on a winning Strike,
- was individually winner-decisive,
- was individually Wound-decisive, or
- carried associated Battle VP.

This is intentionally narrow provenance, not a general causal graph. In particular, `Sunlight -> Draw -> die later placed` is not attributed by this patch. Nor are arbitrary non-die Plant effects reverse-inferred from later wins.
