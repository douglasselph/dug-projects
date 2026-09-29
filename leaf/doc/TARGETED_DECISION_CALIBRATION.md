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
