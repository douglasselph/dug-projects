# Human Baseline Draw Reference Audit

Draw is the Human Baseline's opportunity-cost reference for Cultivation Main Actions: "is this action worth giving up Draw?"

## Scored utility

The checkpoint preserves the existing numerical behavior while making its meaning explicit:

- no drawable die: 20;
- action baseline: 35;
- plus 4 priority points per expected pip of the next die;
- next die is the lowest-sided die in Supply, otherwise the lowest-sided die in Discard.

Thus the reference remains deliberately simple and state-sensitive through the next publicly knowable die. The refactor changes score decomposition/observability, not the total Draw priority.

## Audited visible state

`DrawReferenceObservation` captures, without yet assigning speculative score bonuses:

- next die sides and expected roll;
- probability that the roll is a 1 or 2 (the mechanical Roll Reward opportunity; reward *utility* is not yet calibrated);
- owned-dice count;
- average sides of currently observable Supply/Hand/Discard dice;
- Cultivation rounds remaining;
- current purchasing power;
- currently affordable Buy tier and available Buy tiers.

These fields let later targeted research ask whether the stable Draw reference is missing a material ordinary-human consideration without silently hand-tuning it now.

## Deliberately deferred

Battle proximity is not added here. The current `DecisionContext` does not expose the next unrevealed round type. Shared next-phase awareness belongs in the later context-helper checkpoint, where it can be added once with an explicit player-information contract and reused by Draw, preservation, Refresh, Mulch, and card timing.

Current pool quality is observed conservatively as owned-dice count and average die sides. No bonus/penalty is assigned yet because the designer specification has not established a stable mapping from those values to the opportunity cost of one Draw.

Current Buy thresholds are observed but not scored prospectively. A Draw has a random roll, and converting possible threshold crossings into priority points would introduce a new calibration model. Existing threshold-aware effects continue to compare against the unchanged Draw total.

Roll Reward opportunity is similarly observed but not converted into priority points. The chance of rolling 1 or 2 is mechanically known, but Critter/Wisp value is state-dependent and has not yet been calibrated on the Draw-priority scale.

## Research rule

Do not increase Draw sophistication merely because a visible variable exists. Add a score term only when the approved Human Baseline behavior says an ordinary player uses that consideration and targeted observations show that the term improves the reference action's calibration.
