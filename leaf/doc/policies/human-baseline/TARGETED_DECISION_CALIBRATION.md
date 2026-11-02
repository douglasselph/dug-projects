# Targeted Decision Calibration

`bin/targeted-decision-calibration` is the reusable Human Baseline decision-observation runner. It records only decisions in which the requested card/effect was a legal scored alternative; it is not one script per card.

Examples:

    bin/targeted-decision-calibration --target=mulch --games=500
    bin/targeted-decision-calibration --target=Flower_17_02 --games=500
    bin/targeted-decision-calibration --target=Root_05_02 --games=500

The optional reasoning channel now preserves all legal alternatives and compact immutable observations. Normal simulations leave reasoning disabled and pay no alternative-snapshot cost.

Reports include opportunity/chosen counts, target base/final utility and components, Draw utility, shared Refresh and preservation adjustments, recycle/future-availability adjustments, target facts, close rejected alternatives, next same-player decision, final VP, and limited Step-6 provenance where applicable.

Mulch buckets are `roll × sides × Battle-next × upcoming-dice quality`. Forget-Me-Not buckets are `discarded sides × recycle distance × Battle-next × upcoming-dice quality`. Other cards use next phase and Creature face-up/face-down counts as the initial generic bucket.

The runner deliberately does not retain whole games after extraction and does not introduce a general causal graph. Battle attribution remains limited to the provenance architecture already established for the first research questions.

## Optional targeted counterfactual mode

Counterfactual mode is research-only and intentionally narrow. It currently supports `Root_05_02` and `Flower_17_02` only.

Examples:

    bin/targeted-decision-calibration --target=Root_05_02 --games=200 --counterfactual=true
    bin/targeted-decision-calibration --target=Flower_17_02 --games=200 --counterfactual=true

For each sample, two games are created with the same mechanical seed and the same strategy seed. Player 1 is the affected Human Baseline player. Both branches run normal Human Baseline until the first decision at which the requested Plant and Draw are simultaneously legal. The normal Human Baseline chooser is called in both branches *before* the result is overridden, so strategy-RNG consumption is matched through that intervention decision. One branch then forces USE EFFECT and the other forces DRAW. Only that first qualifying decision is forced; all later decisions return to ordinary Human Baseline.

This is a controlled comparison only up to the intervention. Once USE EFFECT and DRAW produce different state, later legal choices can differ. That can change both strategy-RNG and mechanical-RNG call counts. The streams still began from identical seeds, but later random events are therefore not guaranteed to be event-for-event matched. Treat final VP/win differences as paired downstream observations, not as a perfectly isolated causal estimate of the one action.

Use this mode to test explicit hypotheses suggested by the ordinary opportunity report—for example, whether using Forget-Me-Not in a long-recycle D20 opportunity tends to improve near-future Battle access relative to Draw. Do not use it to replace Human Baseline scoring with exhaustive look-ahead.
