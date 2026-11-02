# Human Baseline Targeted Decision Certification

This checkpoint extends the reusable targeted decision-calibration runner; it does not create per-card scripts.

## Important structural finding

The newly approved Saplink Trellis and Bloom Backbone ownership influences were correctly expressed as `BaselineInfluencer`s, but the normal two-purchase `HumanBaselineBuyPlanner` selected the best Plant at a cost using `PurchasePriority` directly and did not apply owned-card influences. Therefore those synergies could affect the legacy/fallback score-engine path while being absent from the primary planned-Buy path.

This is a **LOGIC GAP**, not a calibration issue. The accompanying change routes the planner's same-cost Plant comparison through `BaselineInfluenceRegistry` before applying the optional purchase-score modifier. This makes Saplink/Bloom ownership synergy effective in the primary Buy planner without duplicating either card in generic Buy logic.

Bee-loved Bloom is unchanged; its existing Bee acquisition influence remains the single implementation.

## Targeted runner extensions

The existing runner now accepts readable aliases for the requested cards and records additional state buckets:

- Saplink Trellis / Bloom Backbone: qualifying synergy-Plant count, total die headroom, and actually realizable sequential +1 Raises.
- Bee-loved Bloom: Bee count and current Bee value.
- Petal To Die 4: owned D4 count plus visible Gain-D4 and best Trash-D4 branch scores.
- Transplant Tulip: Hand die count and current Hand value.
- O Edelweiss: spent/face-down Plant count.
- Existing Mulch and Forget-Me-Not specialized buckets are retained.
- Root Four More, Root Cause, and Queen's Blossom continue to expose common next-phase / face-up / face-down state plus their score components, Refresh, Preservation, Draw comparison, and close rejected alternatives.

The runner continues to retain only compact extracted opportunity records across games.

## Certification status

A numerical PASS / CALIBRATION NEEDED / LOGIC GAP classification requires the targeted simulations to execute. The build environment used for this checkpoint cannot download Gradle 8.13 because `services.gradle.org` is not resolvable, so no chosen rates or state-bucket distributions were fabricated.

The only classification justified before those runs is:

- **Saplink Trellis ownership Buy synergy — LOGIC GAP FOUND AND FIXED in this change set.**
- **Bloom Backbone ownership Buy synergy — LOGIC GAP FOUND AND FIXED in this change set.**

The remaining requested targets are **pending empirical certification**, not presumed PASS.

## Local certification commands

First compile and run focused tests:

```bash
./gradlew test compileSimulationKotlin --no-daemon
```

Then run the same reusable runner (200 games is the normal starting sample):

```bash
bin/targeted-decision-calibration --target=mulch --games=200
bin/targeted-decision-calibration --target=root-four-more --games=200
bin/targeted-decision-calibration --target=root-cause --games=200
bin/targeted-decision-calibration --target=forget-me-not --games=200
bin/targeted-decision-calibration --target=queens-blossom --games=200
bin/targeted-decision-calibration --target=saplink-trellis --games=200
bin/targeted-decision-calibration --target=bloom-backbone --games=200
bin/targeted-decision-calibration --target=bee-loved-bloom --games=200
bin/targeted-decision-calibration --target=petal-to-die --games=200
bin/targeted-decision-calibration --target=transplant-tulip --games=200
bin/targeted-decision-calibration --target=o-edelweiss --games=200
```

For Forget-Me-Not and Root Four More, the existing optional controlled counterfactual mode remains available.

## Interpretation gate

Classify each target only after examining meaningful buckets, not just its global chosen percentage:

- **PASS:** action ordering and target/branch behavior move in the intended direction across the important states.
- **CALIBRATION NEEDED:** reasoning structure is right but numerical scale causes repeated implausible ordering against Draw/other alternatives.
- **LOGIC GAP:** a relevant state variable, branch, target consequence, or shared influence is absent or structurally misapplied.

Do not optimize scores in this checkpoint.
