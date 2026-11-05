# Leaf & Let Die - Active Research and Engineering Tasks

**Status date:** November 4, 2026

This is the **active top-level backlog**. It is intentionally shorter than the many historical phase/checkpoint documents under `doc/`.

A historical task ID appearing in an older document does **not** mean that task is still open. Inspect current source/tests/results, then use this file as the current prioritization unless the user explicitly changes it.

## Immediate engineering gate — validate the adopted v38-01 rules

The current source contains the following **implemented** v38 changes. They still require the precise validation below; source presence, converter parsing, isolated smoke tests, and historical October 30 readiness do **not** establish end-to-end v38 compliance.

| ID | Work | Source status / acceptance |
| --- | --- | --- |
| **V38-1** | Per-Strike **Resolve → Doom → Cleanup**, lowest-value-only Doom, row-only dice/Critter return, final Refresh, Wisp's Resolve early finish | **Implemented in source** with focused tests; full regression results not supplied in this archive |
| **V38-2** | Make `THREE_ENGAGEMENTS` the default and preserve `LEGACY_WHOLE_BATTLE` explicitly | **Implemented**, including missing-file fallback and rejection of old `CANONICAL`; production-only runtime remains limited |
| **V38-3** | Synchronize two official Plant CSVs, 14 converter additions, Wisp's Resolve definition and Compost unavailable-upgrade Trash fallback | **Implemented**; prior focused converter/valuation smoke checks passed, but complete Gradle regression verification is not recorded here |
| **V38-4** | Execute deterministic **2/3/4-player × both Battle structures** 3/2/2 full-game matrix (two identical-seed runs each), plus `test integrationTest simulationTest` | **TESTS STRENGTHENED / EXECUTION UNVERIFIED**. `bin/verify_v38_full_game` now runs both Gradle gates, checks exact JUnit case coverage and saves logs/XML/provenance into one `~/Downloads` archive, including on failure. Its Python checker passes six unit tests; Gradle execution remains required on Ubuntu. |
| **V38-5** | Audit special interactions not proved by the matrix (Wisp's Resolve mid-action and multiple uses; Wispquake locked rows; Pollen Theft ownership; Root & Scoot withdrawal; Sunlight action accounting; per-row cleanup/Refresh and other card targeting) | **OPEN** — use legal deterministic fixtures and v38 rulebook, not aggregate win rates |
| **V38-6** | Decide whether/how to move the adopted scheduler into `src/main/` so a main-only runtime can execute the default | **OPEN** architectural limitation; never silently fall back to legacy |
| **V38-7** | Record rulebook/CSV/ruleset fingerprints and distinguish archived v37 cohorts from new v38 research | **OPEN research provenance gate** for new comparative studies; existing historical R3A adapters intentionally force legacy |

**Run next on Ubuntu from the repository root** (recommended single-command evidence collection):

```bash
bin/verify_v38_full_game
```

Or run the underlying commands manually:

```bash
set -o pipefail
./gradlew simulationTest \
  --tests '*V38BattleStructureFullGameRegressionTest' \
  --rerun-tasks |& tee ~/Downloads/leaf-v38-full-game-regression.log
./gradlew test integrationTest simulationTest \
  |& tee ~/Downloads/leaf-v38-all-regressions.log
```

Do not treat authored test classes as passed tests. The current authority is [`doc/rules/Leaf_and_Let_Die_v38-01.pdf`](doc/rules/Leaf_and_Let_Die_v38-01.pdf) together with the official card CSVs. Current-state details are in [`doc/battle/V38_01_CURRENT_STATUS.md`](doc/battle/V38_01_CURRENT_STATUS.md).

## Parallel research follow-up — Human weak-Plant Refresh judgment

A focused Cultivation Human Baseline adjustment adds bounded, seeded strategy variability for activating weak/no-effect cards when other cards are spent, with a modest long-horizon Refresh nudge. Worm Flip works in both directions, and Human Baseline can spend it to flip the last face-up Plant down and enable natural Cleanup Refresh; Water still provides immediate group Refresh. See `doc/policies/human-baseline/WEAK_PLANT_REFRESH_JUDGMENT.md`.

**Research gate after v38 mechanical acceptance:** Run targeted regression tests and a small paired question pilot, inspect weak-Plant/Refresh decision traces and outcome changes, then resume exploratory 36-Plant screening under an explicitly recorded ruleset. Earlier suppression results are not conclusive balance verdicts and legacy-rule cohorts must not be relabeled as v38 evidence.

## Current priority: all-card sanity and pricing audit

After the v38 correctness gate, the next major design goal is to examine the current Plant catalog and answer two separate questions for every card:

1. **Effect sanity:** Is the effect meaningfully useful somewhere, effectively dead, or so strong that credible alternatives/counters cannot compete?
2. **Purchase/pricing sanity:** At the card's actual cost, printed VP, grafting constraints, timing, and opportunity cost, is acquiring it strategically worthwhile relative to legal alternatives?

Do not collapse these into one metric. A good effect can be overpriced; a weak effect can still be worth buying for VP; a strong effect can be healthy if sufficiently costly/contextual/counterable.

### A1 - Generalize the question runner beyond one bespoke card study

**Status:** IN PROGRESS — A1.1–A1.3 complete; A1.4/A1.5 partial and awaiting broader JVM validation; A1.6 exploratory 36-card foundation exists. Once v38 is mechanically verified, run the small catalog pilot under an explicitly supported/recorded Battle structure before any all-36 exploratory first pass. The current legacy R3A adapters do not yet execute v38 questions.**

This is the correct next step for the current repository. The source already contains much of the machinery needed for reusable Plant research, but it is spread across several generations of focused experiments:

- `R3APlantScreenMain` already accepts an arbitrary current Plant target and legal nine-card Grove, and can run targeted versus unrestricted Buy-policy searches, **but its historical runner is pinned to LEGACY_WHOLE_BATTLE**;
- `PlantExperimentConfig` / `PlantExperimentConfigLoader` already provide generic research-only Plant overrides without mutating canonical card data;
- R3B/R3B.1 already provide disabled/positive controls, seat-scoped effect isolation, actual mechanical-output telemetry, fixed-policy comparisons, resumability and provenance safeguards;
- R1/R2 remain Sapping-specific, while R3B/R3B.1 orchestration and the R3A `effect-variant` path still contain card-specific control names/guards for Root Four More and Queen's Blossom;
- there is not yet one declarative question definition that can drive the whole research path for an arbitrary Plant without adding another bespoke launcher.

**A1 definition of done:** a second, structurally unrelated Plant question can be run through the same declarative question file and the same runner, with no Kotlin/Python source edits between questions. The generic path must preserve existing launch/checkpoint/provenance contracts and produce a standard evidence bundle. This is an **engineering reuse gate**, not proof that arbitrary effects are already strategically understood; that remains A2.

#### A1.1 - Inventory and freeze the reusable question-runner boundaries

**Status:** COMPLETE — source inventory and frozen-boundary note: [`doc/research/experiments/card-balance/REUSABLE_QUESTION_RUNNER_ARCHITECTURE.md`](doc/research/experiments/card-balance/REUSABLE_QUESTION_RUNNER_ARCHITECTURE.md). No runtime behavior changed.

Before changing code, map the current implementation into reusable responsibilities and explicitly decide what the generalized runner will reuse rather than rewrite.

Required work:

- inventory R1, R2, R3A, R3B and R3B.1 launchers/runners and identify which pieces provide question definition, search, evaluation, effect override, telemetry, reporting, checkpointing and archiving;
- inventory the current `PlantExperimentConfig`/loader, seat-scoped research-effect mechanism, R3A target/Grove validation, D1-D10 development telemetry and R3B.1 mechanical-output telemetry;
- identify all currently card-specific assumptions in the generic-looking path, especially `effect-variant` names, target-ID guards, hard-coded card lists, result labels and launcher control matrices;
- record which existing output/checkpoint/manifests must remain backward-compatible or receive an explicit new schema/version;
- define the smallest generic ownership boundary: **question specification -> validated conditions/stages -> existing search/evaluation engines -> common evidence/reporting**.

**Acceptance gate:** a short implementation note or code-level design map identifies the exact classes/scripts to retain, wrap, generalize or leave historical. No game rule, policy or experiment behavior changes in A1.1.

#### A1.2 - Add a declarative Plant question specification and strict validator

**Status:** COMPLETE — schema v1 validator, two example questions, and negative tests; see [`PLANT_QUESTION_SPECIFICATION.md`](doc/research/experiments/card-balance/PLANT_QUESTION_SPECIFICATION.md). The two example target fingerprints were deliberately updated to match v38 official card-text definitions; **80 Python research-tool tests passed** after this correction. Stage execution is still partial under A1.4–A1.5 and remains pinned to legacy in historical R3A adapters.

Introduce one human-readable question definition that is data, not a new launcher script. Keep the format deliberately small; do not invent a large experiment DSL.

The specification must be able to identify/record:

- stable `question_id`, human question/hypothesis and target Plant title plus current ID/fingerprint;
- exact legal nine-card Grove and player count(s);
- Round/Wisp/game-rule configuration, including the current 3/2/2 setup and **adopted v38 THREE_ENGAGEMENTS** versus explicitly selected historical **LEGACY_WHOLE_BATTLE** rules;
- named comparison conditions such as official, suppressed/no-effect, candidate effect, and later price/cost alternatives;
- whether an intervention applies to all players or only a target seat/role;
- requested research stages: targeted discovery, unrestricted discovery, frozen-policy comparison, adapted/reoptimized comparison, held-out evaluation and countersearch;
- policy/weight inputs, seeds, seat rotation, search/evaluation budgets and practical-effect thresholds needed for reproducibility.

Validation must fail clearly for stale/unknown card IDs, target cards absent from the Grove, illegal type/cost slots, duplicate Grove cards, unknown effects/overrides, invalid player counts, missing referenced weights/files, incompatible stage settings or a question whose stated target fingerprint no longer matches current data.

**Acceptance gate:** deterministic parser/validator tests cover at least two unrelated Plant specifications plus invalid/stale cases. Merely parsing a file is not enough; the resolved spec must print the exact effective card/Grove/rules/policy inputs that will be tested.

#### A1.3 - Generalize experiment conditions and effect/price override resolution

**Status:** COMPLETE — generic fail-closed condition resolver and focused tests; see [`PLANT_QUESTION_CONDITIONS.md`](doc/research/experiments/card-balance/PLANT_QUESTION_CONDITIONS.md). Execution integration remains A1.4.

Replace the current generic-path dependence on card-specific condition names with a reusable condition model.

Required work:

- represent canonical, suppressed, candidate-effect and explicit cost/scoring/type interventions through the existing `PlantExperimentConfig` machinery wherever possible;
- preserve seat-scoped interventions for one-sided fixed-policy comparisons without leaking the experimental effect to control opponents;
- remove Root Four More / Queen's Blossom target-ID conditionals from the generalized path; a condition should resolve from the question specification plus an executable effect/override, not from a special launcher branch;
- keep canonical CSV/card definitions immutable; candidate mechanics that do not yet exist still require a deliberately implemented/tested executable `GameEffect` or equivalent before a spec may reference them;
- preserve legal Grove resolution when type/cost changes are explicitly being studied; do not silently create an illegal 4x9 market.

**Acceptance gate:** the same condition resolver can express existing no-op, +20 VP, Root Four More Raise +20, Queen's Blossom Draw 4 and Sapping's alternate effect without target-specific runner code. Existing isolation/leakage tests remain valid or are replaced by equivalent generic tests.

#### A1.4 - Introduce one reusable question execution plan and launcher

**Status:** PARTIAL — declarative planning, strict official R3A adapter and a verified-in-upload two-player official/suppressed target-seat frozen comparison. Expanded Buy-only adapted/countersearch stages are implemented but await JVM verification; remaining stages and player counts still require A1.5.

Create one top-level entry point (name is implementation choice, e.g. `bin/plant_question`) that reads a validated question specification and expands it into an explicit stage/condition plan.

Required behavior:

- `--plan` prints every condition, Grove, role/policy, seed block, seat treatment, game budget and expected output location without running games;
- selected stages can be enabled/disabled from the question specification rather than by creating another one-off script;
- reuse existing R3A/R3B/R3B.1 engines/adapters initially instead of rewriting learning, game execution or telemetry;
- compile once, then use direct-Java/persistent JVMs or isolated JVMs according to the existing launch architecture and state-isolation requirements;
- preserve deterministic seeds, locks, resumable checkpoints, whole-batch progress/ETA, manifest validation and one verified upload-ready archive copied to `~/Downloads`;
- a failed cohort/stage is retained as a failure and blocks a misleading aggregate assessment; it is never silently dropped.

**Acceptance gate:** `--plan` for two unrelated questions is generated by the same launcher and contains no card-specific Python/Kotlin branches beyond the data in the question files and executable effect registry.

#### A1.5 - Generalize stage adapters for discovery, comparison and countersearch

**Status:** IN PROGRESS — generic target-seat suppressed frozen comparison is verified by uploaded pilot integrity checks; an adapted Buy training/evaluation and frozen-specialist countersearch pilot is implemented but requires Ubuntu JVM verification. Stronger opponent portfolios, additional effect conditions and multiplayer remain open.

Make the reusable launcher able to invoke the existing research operations by role rather than by historical experiment name.

Minimum stage adapters:

1. **Targeted discovery** - search for a strategy that actually acquires/uses the target; exploration incentives are training-only.
2. **Unrestricted discovery** - search for competitive play without target-use incentives.
3. **Frozen-policy comparison** - hold policy parameters fixed while changing only the declared research condition where meaningful.
4. **Adapted/reoptimized comparison** - independently search under each condition when the changed effect/price can alter strategy.
5. **Held-out evaluation** - no training bonuses; fair seats and independent held-out seed blocks.
6. **Countersearch** - freeze a strong target-using strategy and search/evaluate an unrestricted challenger against it, then confirm with held-out crossplay.

Keep each stage's policy scope explicit. If only Buy is learned and all other decisions are Human Baseline, the manifest/report must say so. The generalized countersearch adapter being executable is an A1 requirement; proving that it is strategically strong enough for arbitrary effects is an A2 requirement.

**Acceptance gate:** each requested stage can be selected for an arbitrary Plant question without adding a card-specific launcher. Training-only incentives cannot leak into held-out evaluation scores.

#### A1.6 - Standardize the per-question evidence bundle and report contract

**Status:** PARTIAL — first-pass 36-card catalog screen and standardized exploratory evidence index implemented; see [`PLANT_CATALOG_SCREEN.md`](doc/research/experiments/card-balance/PLANT_CATALOG_SCREEN.md). Full research-grade reports (uncertainty, adapted/countersearch, broader effect attribution and multi-Grove validation) remain open.

Create one generic result layout and schema so later A2-A5 work can consume results without knowing whether the historical origin was R2, R3A or R3B.

At minimum retain/report, when available:

- exact question/condition/Grove/source/data/policy fingerprints and seeds;
- target legal exposure, acquisitions, grafted copies and activations/resolutions;
- actual effect-output telemetry already supported by R3B.1, plus explicit indication when an effect family lacks adequate output attribution;
- named Plant development, dice development, substitutions/competing purchases where current telemetry supports them, Strike/VP/Wound outcomes and final results;
- search success/failure, policy scope, held-out sample counts, seat allocation and uncertainty;
- fixed-policy versus adapted comparisons as distinct estimands;
- countersearch status and best challenger evidence;
- an evidence-sufficiency section that can say **Investigate** rather than manufacture a reassuring verdict.

Do not make A1 invent missing causal measurements just to fill columns. Missing effect-family telemetry should be explicit and flow into A2.

**Acceptance gate:** two unrelated questions emit the same schema/layout and can be summarized by one report reader/aggregator. Card names are primary labels; IDs/fingerprints remain trace keys.

#### A1.7 - Prove the generalized path with two unrelated Plant questions

**Status:** BLOCKED on accepted A1.4–A1.6 JVM execution, stage coverage and evidence-bundle contracts (A1.2–A1.3 are complete).

Run a deliberately small engineering/functional vertical slice, not a balance study.

Use at least:

- one simple/direct Plant effect already covered by calibration infrastructure (for example Root Four More or Queen's Blossom); and
- one structurally different Plant question (preferably Sapping Snapdragon because its alternate effect and older R1/R2 path provide a strong reuse/parity check).

For each, run at least official versus suppressed/no-effect plus whatever existing candidate effect is already executable and useful for parity. The **only** per-question change should be the question specification and referenced override/effect; no source edits between the two runs.

Verify:

- legal Grove and exact target identity;
- targeted and unrestricted search can both execute;
- frozen and adapted/held-out stages produce generic outputs;
- one-sided interventions do not leak to control opponents;
- mechanical output is zero for suppressed effects where that instrumentation applies;
- checkpoints/resume and archive generation work;
- comparable legacy cohorts reproduce or any intentional schema/seed differences are documented rather than hand-waved.

**Acceptance gate:** the second unrelated Plant completes through the same path with no bespoke runner changes. This is the concrete R4 reusable-question milestone.

#### A1.8 - Documentation, compatibility and handoff cleanup

**Status:** BLOCKED on A1.7.

After the vertical slice passes:

- document the new question-file format, runner, stage meanings, output schema and minimal commands;
- update `README.md`, `TASKS.md` and the documentation index to point new work to the reusable runner;
- preserve R1/R2/R3A/R3B/R3B.1 scripts/results for provenance. Convert them to thin compatibility wrappers or mark them historical only after parity/dependency review; do not delete them merely because the generic runner exists;
- record any known unsupported effect families or telemetry gaps as A2 work;
- provide the normal changes-only ZIP, exact tests run/not run and a Git commit message.

**A1 exit gate:** a new Plant-effect or pricing question can normally be added by writing one validated question specification and, only when the mechanic itself is genuinely new, one tested executable effect override. No new experiment architecture or bespoke card launcher is required.

### A2 - Make the balance gate trustworthy for strategically understood effects

**Status:** PARTIAL / OPEN.

R3B.1 established important mechanical isolation and sensitivity controls, including no-effect and +20-VP controls. R3B.1a then made Human Baseline effect-aware for the simple calibration controls (no-op, +20 VP, Raise +20 and Draw 4). Those increments still do **not** prove that arbitrary effects, multi-card synergies or long-horizon strategies are evaluated/exploited as strongly as a capable player could exploit them.

Required before strong all-card conclusions:

- preserve/extend R3B.1a's effective-effect awareness beyond the simple calibration controls when a new effect family requires it;
- record effect opportunities, actual mechanical output, caps/supply/row constraints, and whether an effect changed a meaningful tactical/economic outcome;
- distinguish "strong in wording" from "strong in actual realized state";
- retain **Investigate** when an effect was not adequately exercised;
- require a meaningful positive control in addition to the obvious direct +20-VP control;
- include deliberate counterstrategy search before declaring dominance;
- keep purchase learning and effect-use competence conceptually separate.

Do not solve this by hardcoding special intelligence for every experimental card. Prefer reusable consequence/effect-aware evaluation or targeted search infrastructure.

### A3 - Run the 36-Plant effect sanity audit

**Status:** BLOCKED on A1/A2 being credible enough; not yet a completed all-card study.

For each current Plant:

- enumerate exact current card definition and legal market slot;
- find at least one credible context/build that attempts meaningful use;
- compare effect-on versus effect-suppressed while preserving price/printed VP/grafting;
- search for an exploit and then a counter if the effect looks unusually strong;
- use held-out seeds, seat rotation and appropriate opponent diversity;
- report effect opportunities/resolutions and actual output;
- classify as **No concern demonstrated**, **Investigate**, or **Strong balance concern**, with direction such as weak effect / dominant effect / context-specific / insufficient evidence.

A card is not cleared merely because the simulator completed or a learner purchased it.

### A4 - Run the 36-Plant purchase/pricing audit

**Status:** OPEN; can share infrastructure with A3 but must remain a distinct conclusion.

For each current Plant, evaluate whether the card is worth acquiring **at its actual price** relative to what competent players can buy/do instead.

Required evidence should include:

- legal simultaneous alternatives, not just exposure count;
- actual payment die/dice and Critters, overpayment, and purchase feasibility;
- competing purchases/actions forgone;
- printed VP and likely time remaining to exploit the effect;
- graft prerequisites/space and activation opportunity cost;
- named development/build context and duplicate-copy pressure;
- held-out competitive results for successful card-using and non-card strategies.

If pricing looks suspicious, use a **separate explicit legal market experiment** rather than casually changing a cost in a way that creates an illegal 4x9 Grove structure. Preserve one card per legal type/cost slot in serious market comparisons.

Desired outputs distinguish at least:

- effect useful + price reasonable;
- effect useful + likely overpriced;
- effect weak + price/VP still makes purchase viable;
- card/effect too broadly attractive;
- insufficient evidence.

### A5 - Produce one consolidated per-card report/index

**Status:** OPEN after A1-A4.

The all-card result should produce:

- one concise report per Plant;
- one 36-card index sorted by evidence-backed concern;
- direct links/paths to the underlying matchup/scenario evidence;
- exact source/data/policy fingerprints and run settings;
- no false "all balanced" conclusion when a card was insufficiently exercised.

---

## Existing capability to reuse - do not rebuild casually

The project already contains substantial reusable infrastructure. Before adding a new system, inspect and reuse where applicable:

- canonical Kotlin rules engine and deterministic integration harness;
- Human Baseline and modular learned/frozen policy families;
- D1-D10 Buy/development telemetry and reports;
- R0/R1/R2/R3 research-question work;
- R3A all-Plant screening foundation;
- R3B and R3B.1 research effect overrides, mechanical-output telemetry, one-sided effect isolation, negative/positive controls and resumable experiment infrastructure;
- source/data/weight fingerprints and A03 protected-boundary checks;
- direct-Java/persistent-JVM launch architecture;
- existing market/opportunity/substitution reporting;
- current C01-C05 scoring/calibration infrastructure.

The desired direction is **question-driven experiments on shared machinery**, not another fork or a separate launcher architecture for every card.

---

## Parallel research stream: evaluate the adopted Three Engagements rules

The designer **adopted THREE_ENGAGEMENTS in rulebook v38-01**, independently of whether the earlier v37 pacing/fairness research reached statistical acceptance. This section is now about **post-adoption evidence**, not an adoption decision. Mechanical verification is tracked by V38-1–V38-7 above.

### B1 - Battle-structure runtime selection

**Status:** IMPLEMENTED; main-only runtime limitation is V38-6.

`battle.structure=THREE_ENGAGEMENTS` is the default with or without the optional properties file. `battle.structure=LEGACY_WHOLE_BATTLE` explicitly selects the historical engine. The stale `CANONICAL` enum value is rejected. The research A/B runner selects conditions independently; R3A-based historical research is pinned to legacy. See [`doc/battle/BATTLE_STRUCTURE_CONFIGURATION.md`](doc/battle/BATTLE_STRUCTURE_CONFIGURATION.md).

### B2 - Validate Task 7 fairness/counterfactual extension on the current rules

**Status:** OPEN; the existing Task 7 package was not fully validated and predates adoption.

- Compile and execute the Task 7 extension on the current code; clearly label whether cohorts are v37 legacy or v38.
- Reproduce full 2-/3-/4-player cohorts, including early/mid/final Battles, without dropping failed runs.
- Condition last-actor outcomes on starting row strength, resources, player seat/order and initial Strike status before interpreting advantages.
- Compare the adopted scheduler with controlled order perturbations, while retaining truly comparable legacy A cohorts if useful.
- Measure High-Strike Wound cascades, resource stranding, Wisp effects and per-row Doom under **v38** rather than extrapolating from Candidate B's older end-of-Battle Doom.

### B3 - Human pacing calibration before tabletop-time claims

**Status:** OPEN / empirical.

Task 6 provides transparent pacing proxies, not observed human minutes. Calibrate handoff/execution/reassessment/decision coefficients against playtest video/audio, compare passive-finished waiting separately from consequential observation, report fast/base/slow sensitivity ranges, and never equate JVM CPU time with tabletop time.

### B4 - Post-adoption balance and experience review

**Status:** OPEN; adoption is completed, evaluation is not.

Test actual effects of v38 on last-actor/order bias, High-row Wounds, Support caps, Wisp's Resolve, Doom attrition, card/resource synergies, strategic diversity, learning burden and tabletop downtime. If results reveal a problem, propose a *new, explicit* rule experiment; do not silently revert to the legacy default or claim that earlier v37 measurements proved v38 safe.

---

## Deferred calibration work - do not reopen by default

### C04 and broader Human Baseline fine calibration

**Status:** DEFERRED intentionally.

The source includes C01-C05 increments. C04's reviewed mixed-action acceptance is still diagnostic/partial; C05 establishes explicit phase preference units but states that broad action-family unification is incomplete.

Do **not** turn this into the immediate project again unless a specific experiment shows one of the following:

- a clearly pathological Human Baseline choice that contaminates the research question;
- a rules/legality mismatch;
- failure to recognize or exploit the tested effect strongly enough to make the experiment meaningful;
- inability to detect known extreme controls;
- a specific comparison that cannot be made credibly without a narrow calibration fix.

When such a blocker appears, make the **smallest focused calibration repair**, test it, and return to the research question.

The older master calibration backlog remains useful as a catalog of possible diagnostics, not as a command to execute all phases before doing research.

---

## Later research after the all-card audit

### R5 / wider strategic landscape

**Status:** LATER.

After the reusable all-card method is credible, extend the same machinery toward:

- Wisp effect/luck balance;
- Cultivation and Battle Round-effect balance;
- strategic diversity within fixed nine-card Groves;
- deliberate degenerate strategies (dice-only, Plants-only, cheap-Root VP, Bee/Mulch/Compost extremes, etc.);
- expert-vs-expert counterplay and strategic irreducibility;
- skill-versus-luck estimates across player counts;
- simplification opportunities where complexity contributes little meaningful choice.

Do not require one universal expert policy across 2/3/4 players. Each supported player count should retain meaningful branching and counterplay.

---

## Invariants for all future work

- Preserve the **adopted v38-01 tabletop game mechanics** unless the user explicitly approves another rule change; do not restore older v37 behavior from historical notes.
- Current standard Round structure is **3/2/2**.
- Sunlight Round effects gain a **Sunlight token**, never legacy Raise +3.
- Roll Reward 1 -> Critter and 2 -> Wisp applies according to current rules in both Cultivation and Battle; do not restore obsolete restrictions from old notes.
- Keep official card data, research overrides and game-level rules selection distinct; new v38 balance cohorts must never silently use legacy-only R3A adapters.
- Use current card names as primary report labels and IDs/fingerprints for traceability.
- Do not silently change frozen weights or promote experimental effects to canonical.
- Do not claim balance from Human Baseline comparisons alone.
- Do not treat repeated legal opportunities inside one game as independent samples.
- If a run or cohort fails, do not drop the failure and analyze the remainder as though complete.
- Long-running runs must preserve provenance, deterministic settings, checkpoint/resume, visible progress, and one upload-ready archive.

