# Plant catalog exploratory screening

This is the **first-pass 36-card effect-screening foundation**, not a certification of balance. It reuses the existing two-player Buy-policy official/suppressed paired comparison. All 36 current Plant definitions are resolved directly from the research catalogs, each with a legal nine-card Grove and the identical question-runner configuration. No Kotlin gameplay code or historic research launcher was changed.

## Operation (from the repository root, `leaf/`)

```bash
python3 -m unittest discover -s bin/tests -p 'test_plant_catalog_screen.py' -v
python3 bin/plant_catalog_screen.py --plan > ~/Downloads/plant-catalog-plan.json
./gradlew test
./gradlew writeSimulationRuntimeClasspath
# First verify the integration on only two cards:
python3 bin/plant_catalog_screen.py --run --limit 2 --no-build |& tee ~/Downloads/plant-catalog-pilot.log
# After reviewing the pilot report, resume and expand to all 36:
python3 bin/plant_catalog_screen.py --run --no-build |& tee ~/Downloads/plant-catalog-screen.log
```

At any point regenerate the current index without running simulations:

```bash
python3 bin/plant_catalog_screen.py --report
```

The completed research report is copied to `~/Downloads/plant-catalog-screen-evidence.tgz`, with `UPLOAD THIS FILE:` printed. The source outputs and full game logs stay under `output/experiments/plant-questions/` and `output/experiments/plant-catalog-screen/` respectively. The compact archive contains standardized per-card findings and question definitions, **not full per-game historical raw evidence**; retain the original per-card archives and logs for detailed review. The batch stops at the first failing card and retains its log; rerunning reuses valid existing checkpoints.

## What the report can and cannot say

- `EXPLORATORY_COMPLETE` means a verified official/suppressed paired-frozen evaluation exists. It does **not** mean the card is balanced.
- `NOT_RUN` and `EVIDENCE_INVALID` remain visibly distinct from complete evidence.
- `actual_resolutions` and `suppressed_resolutions` are from the **targeted vs unrestricted** held-out cohort. An effect with zero official resolutions is **not tested for effect usefulness**.
- `win_delta_pp` is **suppressed minus official** on the same frozen policies. A positive number means suppression improved observed frozen-policy results, not necessarily that the real card is harmful. The companion policy is Human Baseline; Buy is the only optimized decision family.
- No automatic *No concern demonstrated*, *Too weak*, or *Dominant* verdict is issued. All first-pass assessments remain **Investigate** until stronger evidence is available.
- The current pipeline supports **only two players**. Three- and four-player studies and independently varied markets remain future work.
- These per-card Groves share the historical default in the eight non-target slots and therefore cannot demonstrate generality across different Groves.
- Countersearch and adapted-condition stages exist as a separate pilot but are **not automatically executed in the 36-card first pass**. Reserve these costly stages for flagged candidates, and do not merge their data into frozen-policy causal estimates without a clear analysis contract.
- Effect-competence and measurement gaps may make a card look unused/weak. The user should direct attention to cards with very few exercised effects, large or uncertain effect changes, surprising buying behavior, and missing evidence, not treat rankings as final balance findings.

## Deferred work tracked in TASKS.md

1. Larger held-out sets, independent seeds and effect-exposure minimums; confidence intervals and matched inference for every card.
2. Actual effect consequence, including tactical decisions and substitutions, rather than only output totals.
3. Automated adapted learning, independent challengers, reusable countersearch and robust expert portfolios for suspicious cards.
4. Multiple Groves, 3/4 players, varied policy families and rule-configuration coverage.
5. Strong non-VP positive/negative sensitivity controls before claiming the system can detect all kinds of balance defects.
6. Per-card raw evidence packaging, cross-run provenance, cohort progress/ETA, and compact versus full archive options.

The primary objective is to **start identifying where to investigate**. The full 36-Plant balance verdict is a distinct subsequent research milestone.
