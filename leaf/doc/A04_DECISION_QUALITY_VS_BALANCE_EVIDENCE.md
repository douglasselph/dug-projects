# A04 — Separate Human Baseline decision quality from Plant balance evidence

**Status:** research-interpretation guardrails implemented. No new learner, trained weights, card changes, new balance results, or countersearch are claimed here.

## Two independent questions

1. **Decision quality (Human Baseline control):** Given a *known* legal nine-card Grove, player Hand, phase, dice, tokens, targets, and opponent state, are its selected actions and effect targets plausibly human? Confirm with owner-reviewed scenario acceptance ranges, not only unit assertions. Cultivation expected-pip scoring and Battle strike-outcome scoring will be calibrated together, including Round actions, shared resource values, and decision variability. Passing this gate establishes a **plausible control**, not card balance or optimal play.
2. **Research strength (card-balance evidence):** When the full 36-card four-per-slot catalog is screened through legal nine-card Groves (one chosen Plant per slot) and the canonical **3/2/2** research round schedule, can **strong target-seeking learned strategies** successfully exploit each Plant, and can independently trained **expert counterstrategies** respond? Are any cards robustly dead or dominant *after* accounting for effects, opportunity, interactions, contexts, and opposition? This gate requires independent evidence beyond baseline plausibility.

Do not conflate `Human Baseline makes a reasonable move`, `Buy learner buys card`, `card effect resolves`, `card causes measured outcome`, and `card is balanced`. They are different claims.

## What is in the current source (as of A03, preceding expected-pip rewrite)

- `bin/r3a_plant_screen`: learns **Buy only** via unrestricted and targeted evolutionary populations, uses Human Baseline for other decision families, evaluates frozen champions in matched held-out matchups, and outputs `all-plant-screen.csv` where every tested card remains `Investigate` and `evidence_sufficient=False`; untested cards are `Not run`. Its confidence interval is fixed-policy game-sampling uncertainty only.
- `bin/r3b_effect_sanity_gate`: official / disabled / extreme effect comparisons, including frozen-policy paired comparisons; this is a **calibration** of effect sensitivity, not proof of balance.
- `bin/r3b1_effect_isolation`: one-sided frozen Buy-policy interventions with untreated-opponent controls, including Root Four More Raise +20, Queen's Blossom Draw 4, and immediate Gain 20 VP. Some changes are mechanically limited by die faces, Supply, or Battle placement capacity. It does not train an expert response to the experimental power.
- Other learned policy families and weight files already exist in the repository. Their existence is **not evidence** that they were jointly trained, used, or validated as expert opponents in R3A/R3B/R3B.1.

## Nonnegotiable research boundaries

- Preserve A01 source provenance, A02 **3/2/2** default, the complete 36-card catalog/four options per legal slot, and exactly one card per slot in each nine-card Grove.
- Preserve A03 canonical effects, rules, prices, research overrides, weights and learner families unless a separately authorized task says otherwise. Human Baseline scoring work may change how it *chooses*, not what effects *do*.
- Do not optimize or certify an equal purchase count / activation count across all cards. Contextually weak, contextual combo-driven, niche defense, and purposefully situational cards can all be healthy.
- Passive/end-game Plant value must not be judged by `EffectResolved` alone. A learner's wins and total final VP must not be attributed causally to the focused card without controls.
- Matchup `targeted_vs_unrestricted` is *not* best-response/counterstrategy search. No-op / Raise +20 / Draw 4 / VP +20 are **instrument controls**, not automatically certified weak/strong balance cases.

## A04 decision-quality acceptance (before interpreting R3 balance studies)

| Evidence | Requirement |
|---|---|
| Complete scenario states | Legal Grove, Hand and dice faces, available Supply and Round effects, resources, phase, Battle rows/opponents, legal actions, and card effects |
| Blind owner judgments | Owner states acceptable choices and optionally probability/frequency bands **before** viewing the algorithm's outcome; retain the original expectations |
| Full comparison | Log each available candidate: expected-pip or Battle projection, resource/future-value adjustments, decision variability, final score, and final action/targets |
| Repeatability | `DV=0` deterministic; `DV=1/2` seeded and reproducible; multiple seeds tested for acceptance bands |
| Integration proof | Execute selected actions through actual engine and compare expected resource/VP/placement consequences; do not confuse scorer tests with actual effect mechanics |
| Calibration controls | Disabled, capped Raise +20, constrained Draw 4, and Gain 20 VP produce interpretable mechanical/decision responses |

**Result label:** `Human Baseline scenario-calibrated` is permissible. **Not:** `36 Plants balanced`.

## Evidence checklist required for EACH card before moving beyond Investigate

1. **Exposure and legal contexts.** Multiple distinct nine-slot Groves with the focused card, realistic opponent/Buy alternatives, both early/late game situations, and representative randomized seeds. Document actual exposure and purchase opportunity denominator.
2. **Target-seeking search adequacy.** Multiple search seeds / independent learner populations; targeted objective, target acquisition, use and success measured on held-out games; failed exploration clearly labeled. Buy-only search cannot substitute for card-activation, target-selection or Battle expertise.
3. **Effect/value attribution.** Effects and passive/end-game value tracked separately; concrete die pip, resource, Strike VP, other VP and Wound outcomes; controlled official/disabled/variant runs or matched decision continuations where scientifically valid. Track changed decisions after interventions rather than assuming identical games.
4. **Strong strategy exploitation.** Expand beyond Buy: use appropriate learned policy families (e.g., Cultivation Main, effect targets, Battle Main/Support, resources, Wisp and Buy), combining or training them with careful policy boundaries and no train/test leakage. Must demonstrate successful use of the specific card, not just high total wins.
5. **Independent expert counterplay.** Deliberately train/choose an opponent to counter the *frozen specialist*, evaluate a truly new held-out seed/seat schedule, and repeat or alternate best responses to test exploit persistence. `targeted_vs_unrestricted` alone is insufficient.
6. **Breadth and replication.** Repeated searches, varied legal Groves, player counts where relevant, diverse opponents, credible opponent combinations, and representative round/game contexts. Record the full training and evaluation budget and missing opportunities.
7. **Statistical uncertainty and comparators.** Appropriate paired analyses, effect size and intervals, search-selection uncertainty, multiplicity across 36 Plants, and meaningful card-substitution benchmarks. Do not treat one noisy win percentage as an independent verdict.
8. **Evidence-supported verdict.** Review mechanism + tests + tactical evidence. `No concern demonstrated` is not `proved perfectly balanced`; `Strong balance concern` requires robust causal disadvantage/dominance against tested counterplay. `Investigate` remains correct where any major evidence category is missing. `Not run` means no valid study.

## Later task sequence (A04 does not implement these steps)

- **Master expected-pip decision calibration:** one shared decision scale within Cultivation and within Battle; draw die by `(sides + 1)/2`, actual capped pip deltas, shared token resource estimates, reasonable VP conversion and decision variability. Remove *obsolete* Sunlight Raise +3 in a separately approved mechanics correction; official Sunlight is **gain a Sunlight token**.
- **Human-approved scenario matrix:** create and obtain owner judgments for ~52 concrete market/Hand/Battle tests, including acceptance frequencies. Test actual decisions and effect results on real game states; preserve initial owner answers through code changes.
- **R3B.2 instrument sensitivity:** repeat official/no-op/Root Four More Raise +20/Queen's Blossom Draw 4/Gain 20 VP controls after calibration; require meaningful effect exposure and diagnostics. A successful positive control does not authorize a card-balance conclusion.
- **Stronger specialist training and counters:** targeted learning over relevant decision families; independent experts and best-response opponents; external held-out challenges. Evaluate against frozen Human Baseline and specialist controls, not just one learned Buy rival.
- **Long-running all-36-card study:** check all four alternatives in each of the nine legal catalog slots, each evaluated in legal nine-card Groves. Preserve exposure, effect contribution, utility, opponent adaptation, uncertainties, complete provenance, and final per-card assessment. Do not assert all cards balanced from lack of detected wins/losses.

## A04 tests and tracker

- `doc/A04_CARD_BALANCE_EVIDENCE_TRACKER.csv` lists **36 cards** with all A04 balance-evidence dimensions explicitly pending; it is a scope and progress checklist, **not** a simulated conclusion or a new canonical card dataset. `Not run` in this tracker means **the complete A04 evidence review has not been run**, not that no exploratory experiment involving the card has ever been executed. In particular, Root Four More and Queen's Blossom have prior R3B.1 instrument-calibration evidence, but neither has a completed expert-counterplay balance review.
- `bin/tests/test_a04_research_evidence_boundary.py` executes synthetic R3A reporting scenarios, including a specialist that wins **100%** of held-out games and one with **no purchases or resolutions**. Neither is automatically marked balanced or dead. It also verifies the catalog tracker has nine legal slots × four cards each, and that R3B's deliberate extreme-card controls remain clearly separated.

From the project root:

```bash
python3 -m unittest discover -s bin/tests -p 'test_a04_research_evidence_boundary.py' -v
python3 bin/a03_verify_boundaries.py
python3 -m unittest discover -s bin/tests -p 'test_*.py' -v
```

A04 does not run Kotlin, generate balance results, edit canonical data, or certify any card. Continue using separate simulation result archives for actual evidence.
