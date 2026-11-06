# Leaf & Let Die - Active Research and Engineering Tasks

**Status date:** November 5, 2026 — catalog audit and Round loader migration implemented

This is the **active top-level backlog**. It is intentionally shorter than the many historical phase/checkpoint documents under `doc/`.

A historical task ID appearing in an older document does **not** mean that task is still open. Inspect current source/tests/results, then use this file as the current prioritization unless the user explicitly changes it.

## NEXT ACTIVE PROGRAM — November 5 authoritative CSV integration (NC-01–NC-27)

**Status: NC-01 and NC-02 implemented and statically verified; NC-03 onward open.** This section was originally prepared by comparing the newly attached
`leaf(20261105-234756).tgz` with `leaf(20261105-234502).tgz`, and checking the then-current Kotlin
converter, registry, action scheduler, effect handlers, policy references, and selected tests.
The user has designated the newly supplied files under `data/` as **the new card source of truth**.
They supersede older v38-01 card definitions *for card content*, but do not silently change the
adopted **Three Engagements** Battle structure or the 3/2/2 Round schedule.

**Start here, before A2/A3 or new large research runs.** Previously verified V38-4–V38-7
results belong to the **previous card catalog**. Preserve those results and do not label the new
catalog verified until the new tests and full-game matrix pass.

### Audited input and why a migration is required

| File | New inventory | Comparison against immediately preceding archive |
| --- | --- | --- |
| `data/Cultivation_Cards - Root.csv` | 12 Roots, 4 per cost 5/7/9 | **2** changed effects |
| `data/Cultivation_Cards - VF.csv` | 12 Vines + 12 Flowers, 4 per legal cost slot | **11** changed Plant rows, including two title/effect swaps and printed VP-icon changes |
| `data/Wisp_Cards.csv` | 12 Wisp definitions, **32 physical copies** by `quantity` (includes Wispquake quantity 0) | **2** rewritten play effects; **1** Wisp renamed/rekeyed and reworded |
| **Canonical** `data/Round_Cards.csv` | 12 Round definitions, 18 copies | **2** revised Battle Round effects (Surge, Swell); now the single official Round path |
| `data/Turn_Cards.csv` | **Absent** in the November 5 23:57 source archive | Obsolete file intentionally removed by the user; not a fallback |
| `data/research/resync/*-current.csv` | Empty override tables (headers only) | Unchanged; not a substitute for checking the official catalogs |

**NC-02 source conflict resolved (source routing only):** The user deleted
`data/Turn_Cards.csv` in `leaf(20261105-235751).tgz`. Official Kotlin loading,
active research provenance, R0, Sunlight parity, regression evidence scripts and
current documentation now use `data/Round_Cards.csv`. Historical protected-baseline
manifests and legacy documents retain the old path/hash as *archival evidence*.
The new Surge/Swell effect text is authoritative, but its converter/execution support
remains outstanding in **NC-03, NC-21 and NC-22**. Full Kotlin game execution with
these revised CSVs is **not** claimed to pass until those tasks are complete.

**Static converter audit:** `GameEffectConverter` has no exact mapping for **12 revised Plant
texts**, **3 Wisp texts**, and **2 new Round texts**: **17 unmapped effect texts** in the intended
new authoritative set. The remaining changed Plant row, `Vine_11_03` (Rise and Vine), already
maps to `RAISE_ALL_DICE_PLUS_2` but still needs identity, printed-VP, policy and test review.
This is a source-level check, **not** a Gradle/Kotlin test run. `PlantCardRegistry`,
`WispCardRegistry`, and `RoundCardRegistry` reject `GameEffect.UNKNOWN`; new-catalog loading
will therefore fail until the mappings and implementations are updated.

New incoming SHA-256 fingerprints (record these in manifests, not as permanently hardcoded
expected hashes):

```text
c1bc70b7d02db2fef139744169170b99b36ae43fbfeca48e7c6e0996cfe6c897  data/Cultivation_Cards - Root.csv
7471bd8c0f05d6f738d9683c930de27cdd33659dd82158db3ce5446fd8e278ad  data/Cultivation_Cards - VF.csv
b2ef7563ca1a09d15af2b6a1f12a016469a14fbfd5d2ea54eb85cb59658470aa  data/Wisp_Cards.csv
7f6378cb62076da69ad16ffe7454b92e708940922b5e4d40a9525478afcd1f1b  data/Round_Cards.csv
# historical e0be7625d20d2689fb4772b2d12919f28f88d68098eda39d8a4d2af992604e93  data/Turn_Cards.csv (archived prior baseline, not present)
```

### Work order and acceptance gates

Complete in the order shown. A checkbox means **work still to do**, not a claim that all underlying
features are missing; many reusable primitives already exist. Keep implementation in small,
reviewable commits and changed-files ZIPs rooted at `leaf/`. Do not change official CSV content
without an explicit design decision.

#### P0 — Establish one executable source of truth (do first)

- [x] **NC-01 — Freeze and validate incoming catalogs.** A new read-only
  `bin/audit_authoritative_cards.py` validates all four canonical CSVs, verifies
  36 Plant definitions / nine 4-option legal slots, normalized IDs and duplicate
  rejection, 12 Wisp definitions / 32 copies (Wispquake 0), and 12 Round
  definitions / 18 copies (6 Battle ×1; 6 Cultivation ×2). It emits exact
  definition rows, per-card and per-file fingerprints, git HEAD/dirty status
  when available, and a composite catalog fingerprint. The pinned manifest is
  `doc/research/provenance/card-catalogs/CARD_CATALOG_SNAPSHOT_2026-11-05.json`.
  Run `python3 bin/audit_authoritative_cards.py --out ~/Downloads/leaf-cards-manifest.json`.
  The supplied tarball lacks Git metadata, so its snapshot says unavailable;
  Ubuntu will record the actual checkout. **Static audit passed; no claim of
  executable card-effect coverage.**
- [x] **NC-02 — Migrate official Round source to `Round_Cards.csv`.**
  `CardDataFiles.ROUND_CARD_LIST` now selects the new file; active provenance,
  R0 input audit, Sunlight rule parity, applicable evidence scripts, and active
  research scripts are updated. No code falls back to the obsolete file; the
  audit rejects it if it reappears beside the canonical file. Historical
  baselines remain intact. Tests cover exact new Surge/Swell raw wording and
  strict path selection. **Filename/provenance migration complete; runtime
  effect mappings and whole-game regression remain blocked by NC-03/NC-21/NC-22.**
- [ ] **NC-03 — Convert all exact new texts, then fail closed.** Add the required `GameEffect`
  identities and exact `GameEffectConverter` mappings for the 17 new/unmatched texts; reuse an
  existing effect **only** where its semantics are genuinely identical. Preserve historical
  aliases for replay, not for current-rule substitution. Update `GameEffectConverterTest`,
  `OfficialCardSourceTest` and Wisp/Round registry tests to assert *all* official entries are
  recognized and have executable routing. **Done:** zero UNKNOWN definitions across all 36
  Plants, 12 Wisps and both effects on each of 12 Round cards.
- [ ] **NC-04 — Record minimal rule interpretations before coding ambiguous effects.** Write
  explicit testable rulings for (a) how free Bee/Worm/Butterfly Support is acquired, retained and
  whether multiple activations stack; (b) whether free uses remain legal after two paid Supports
  and how they interact with one-opportunity-per-Engagement and Sunlight; (c) whether a partially
  possible two-step upgrade takes a one-step fallback, skips missing sizes, or fails; (d) how
  repeated **"Raise 2 dice"** selects distinct/repeated dice when fewer than two are legal;
  (e) whether a Wisp owner may select themselves, including the currently played Wisp, for
  forced Wisp Trash; (f) how **"Use it now"** behaves in Battle when no Strike Square has room.
  Resolve what existing core rules settle; flag only genuine ambiguities for Doug rather than
  inventing behavior. **Done:** short decisions/fixture matrix in `doc/` and tests tied to it.

#### P1 — Plant mechanics: revised effect execution (13 changed rows)

- [ ] **NC-05 — Root Awakening (`Root_07_01`, cost 7).** Replace unconditional one-step
  `UPGRADE_DIE_AND_USE_NOW` with **two steps if the selected die shows 3; otherwise one step;
  use the replacement immediately**. Cover D4–D20 ladder, missing inventory, roll rewards,
  ownership, player Dice Hand and live Battle Grid placement, and capacity/locked rows. Do not
  reuse Wisp Overgrowth's “skip missing” semantics without an explicit rule basis.
- [ ] **NC-06 — Root Appreciation (`Root_07_02`, cost 7).** Retain gain-1-Worm and stacking
  **+1 per Worm per activation this round**; add **"Battle: Playing a Worm costs no Support
  Action."** This is a Battle action-economy rule, not just a different Critter value.
- [ ] **NC-07 — Vine Yield (`Vine_07_04`, cost 7).** Replace flat Raise +1 with **Raise one die
  +1 per grafted Flower**. Count the correct current grafted cards (face-up/down as the rules
  define), choose and cap the target die, and verify both phases and Battle row legality.
- [ ] **NC-08 — Saplink Trellis (`Vine_09_03`, cost 9).** The cost-9 row used to be Rise and
  Vine. It is now Saplink Trellis: **for each Root or Vine, Raise 2 dice +1**. Implement
  multi-target/per-graft iteration and legal target decisions, maxima and capacities. Its
  incoming VP icon is **1 VP**, not the previous 2 at this ID.
- [ ] **NC-09 — Berry Tasty (`Vine_11_02`, cost 11).** Change **all owned D8s +1 → +2**,
  observing die-face cap, phase and Battle row constraints. Reuse the all-D8 mechanism with an
  explicit magnitude, not a card-name shortcut.
- [ ] **NC-10 — Rise and Vine (`Vine_11_03`, cost 11).** This ID was Saplink Trellis; it is now
  **Raise all your dice +2** (already a known effect) with incoming **2 VP**. Swap name,
  policy/scorer identity and effect semantics consistently with NC-08; update ID/title-based
  fixtures, market selection, research overrides and development reporting.
- [ ] **NC-11 — Alluring Nectar (`Flower_11_01`, cost 11).** Keep gain/steal Butterfly and
  refresh all owned Butterflies; add **"Battle: Using a Butterfly costs no Support Action."**
  Cover theft, unavailable colors, Refresh, ordinary resource consumption and free-use timing.
- [ ] **NC-12 — Bloom Backbone (`Flower_11_02`, cost 11).** Replace **one die +1 per grafted
  Vine/Flower** with **all your dice +1 for each grafted Vine or Flower**. Test per-die caps,
  card counting, repeated raises, filled/locked Battle rows and absence of dice.
- [ ] **NC-13 — Someday Sunflower (`Flower_11_04`, cost 11).** This was Transplant Tulip;
  now **Gain 2 Sunlight**. Implement bounded Grove supply and ownership, add/verify
  provenance and a reusable count-based gain, and update old Transplant-specific decisions,
  scorer/market identity and tests. Test gaining 0/1/2 when supply is constrained.
- [ ] **NC-14 — Bee-loved Bloom (`Flower_14_01`, cost 14).** Keep gain/steal one Bee and
  stacking **+2 Bee value per activation this round**; add **"Battle: Using a Bee costs no
  Support Action."** Cover stolen/placed Bees and same-round repeat activations.
- [ ] **NC-15 — Bloom Backflip (`Flower_14_02`, cost 14).** Replace old **Raise +1 and flip
  every higher opposing die in the same Strike** with **flip one of your dice; in Battle,
  flip one opponent's die**. Implement both chosen targets with valid face/opposite-face
  semantics, lock restrictions, no-reward rules and the phase-specific second step.
- [ ] **NC-16 — Petal To Die 4 (`Flower_14_04`, cost 14).** Remove old gain-or-steal path;
  instead **gain a D4 and use it now**, then **discard any number of your D4s and draw the
  same number of dice**. Keep proper supply scarcity, draw order, Roll Rewards, exact dice
  ownership, replacement/placement in a Battle row, and zero-D4 edge cases. Preserve old
  research variants as opt-in history only.
- [ ] **NC-17 — Forget-Me-Not (`Flower_17_02`, cost 17).** Replace **roll one die from
  Discard into Hand** with **choose a die from Discard and "Use it now"**. Confirm the
  intended new phase-specific behavior (possibly existing machinery in Cultivation, but
  Battle grid entry/capacity is an added requirement), then test it explicitly.

#### P2 — Wisp and Round mechanics (not secondary for correctness)

- [ ] **NC-18 — Wisp of Honor (`Wisp_Award_VP`).** Replace old **play for +1 VP** with
  **choose one player who has Wisps; that player Trashes one**. This includes the actor if
  allowed by the rule decision. Implement target choice, the target's Wisp choice, timing and
  supply/empty-hand behavior. Preserve its printed **2 end-game VP** if unplayed.
- [ ] **NC-19 — Berry Patient (`Wisp_Award_VP2`).** Replace old **play for +1 VP** with
  **each player who has Wisps Trashes one**. Include self as the wording specifies, resolve
  simultaneous/ordered selection deterministically, and preserve printed **3 end-game VP**.
- [ ] **NC-20 — Wisp's Last Word (new ID `Wisps_Last_Word`).** Replace retired
  `Wisps_Resolve` identity and old `"Wisp's Resolve"` title with **"Wisp's Last Word"**:
  **immediately finish any unresolved Strike: Resolve → Doom → Cleanup**. Reuse the verified
  v38 per-row `StrikeFinishCoordinator` where behavior matches; update *all* hard-coded
  references in Wisp evaluators, tests, fixture names, scenario specs, frozen-weight
  compatibility and reports. Confirm it can target the current or later unresolved row and
  cannot touch an already closed row. Keep the legacy alias only for historical replay if needed.
- [ ] **NC-21 — Round Surge, `Battle_Bloom_Surge`.** In new `Round_Cards.csv`, Surge is
  **"Gain up to a D12. Place in Discard."**, not fixed D12. Add legal die-size selection
  from available D4/D6/D8/D10/D12 as defined, handle zero available supply and record actual
  gained type. Do not silently map to `GAIN_D12_TO_DISCARD`.
- [ ] **NC-22 — Round Swell, `Battle_Scatter_Swell`.** It is now
  **"Gain up to a D8. Use it now."**, replacing fixed **D10 to Discard**. Add legal
  die-size selection and immediate Battle placement/roll/rewards without exceeding a Strike
  Square's 3-die capacity or changing locked rows. Its incoming image/icon fields still
  refer to D12/D10: **flag for card-layout correction**, not a simulator assumption.

#### P3 — Cross-cutting scheduling, decisions, and regressions

- [ ] **NC-23 — Fix free-Support scheduling end to end.** The adopted Three Engagements
  scheduler currently assumes **2 total paid Support uses** and enforces
  `actionCount <= 3` (1 Main + 2 Supports) inside
  `src/main/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/ThreeEngagementBattle.kt`.
  A newly free Worm/Bee/Butterfly use must not consume a paid slot, but must consume the
  resource/ready Butterfly normally. Update legal generation, action counting, policy
  opportunities, Wisp/Sunlight interactions and telemetry; prevent free-action infinite loops.
  Ensure legacy Battle comparison is either supported consistently or explicitly constrained.
- [ ] **NC-24 — Update legal choices, strategy/policy valuation, and reports.** Audit
  `GameEffectDecisionRequirements`, family handlers, Human Baseline Plant/Wisp/Round scorers,
  `BattleRoundEffectPriority`, multi-die target strategy, learned/frozen effect features,
  support-capacity/reachability and direct/indirect tactical projections. Replace stale effect
  and title assumptions (especially old Transplant Tulip, Saplink/Rise identities,
  `Wisps_Resolve`, gain-1-VP Wisps and Swell D10). Preserve historical policies and mark
  fingerprint-incompatible weights instead of silently using them for the new catalog.
- [ ] **NC-25 — Establish deterministic coverage for *every* changed effect and catalog.** Test
  current CSV → converter → correct handler → exact state changes, with no synthetic hard-coded
  definitions taking the place of the new official CSV. Cover both phases where applicable,
  Critter/Butterfly free actions, chaining, scarcity, caps, multi-target Raise, choice legality,
  immediate-use dice, last-row/locked-row cases, Wisp self-targeting, Wispquake, Doom and
  Cleanup. Verify printed VP, name/cost/tier, physical quantity and 4-per-slot invariants.
- [ ] **NC-26 — Run full v38 and research smoke gates on new definitions.** Execute focused
  new tests, `./gradlew test integrationTest simulationTest`, and the verified deterministic
  2/3/4-player × THREE_ENGAGEMENTS/LEGACY_WHOLE_BATTLE full-game 3/2/2 matrix. Then run
  **two structurally different revised-card questions** through the generic v38 runner,
  checking actual activation, suppressed-effect zero output, policy scope, provenance,
  resumability and report completeness. These are functional checks, **not balance verdicts**.
- [ ] **NC-27 — Rebaseline provenance, update handoff and release in small patches.** Make the
  question runner, manifest, official CSV hash collection and `data/README.md` identify the
  new Round path and all four canonical catalogs; flag archived results/policy weights as
  historical. Keep experimental overrides opt-in, and update the known-effect catalog,
  documentation, `TASKS.md` statuses and repeatable Ubuntu commands. Provide changed-files
  ZIPs rooted at `leaf/`, a Git commit message per increment and verified failure evidence.

**Stop/go:** NC-01–NC-03 establish that the new catalog is loadable; NC-04–NC-24 establish
real effect and policy parity; NC-25–NC-27 establish that the simulator and generic research
runner can be trusted on **this** card set. Only then progress to A2 targeted evidence-strength
checks and A3/A4 all-36-card effect/purchase analysis. A2 enhancements can be built narrowly
when an individual effect actually requires them; do **not** start an unrelated generalized AI
rewrite or multi-hour balance run now.

**Scope of this planning increment:** updated `TASKS.md` only. No changes have yet been made
in Kotlin, official CSV data, test expectations, learned weights or the Battle rules. No new
Gradle tests were run; catalog differences and converter mismatches above were established
by static inspection of the supplied archives and current source.

---

## Immediate engineering gate — validate the adopted v38-01 rules

The current source contains the following **implemented** v38 changes. They still require the precise validation below; source presence, converter parsing, isolated smoke tests, and historical October 30 readiness do **not** establish end-to-end v38 compliance.

| ID | Work | Source status / acceptance |
| --- | --- | --- |
| **V38-1** | Per-Strike **Resolve → Doom → Cleanup**, lowest-value-only Doom, row-only dice/Critter return, final Refresh, Wisp's Resolve early finish | **Implemented in source** with focused tests; full regression results not supplied in this archive |
| **V38-2** | Make `THREE_ENGAGEMENTS` the default and preserve `LEGACY_WHOLE_BATTLE` explicitly | **Implemented**, including missing-file fallback and rejection of old `CANONICAL`; production-only runtime remains limited |
| **V38-3** | Synchronize two official Plant CSVs, 14 converter additions, Wisp's Resolve definition and Compost unavailable-upgrade Trash fallback | **Implemented**; prior focused converter/valuation smoke checks passed, but complete Gradle regression verification is not recorded here |
| **V38-4** | Execute deterministic **2/3/4-player × both Battle structures** 3/2/2 full-game matrix (two identical-seed runs each), plus `test integrationTest simulationTest` | **PASS — November 5 Ubuntu archive reviewed.** All six targeted cases, 1,740 unit tests, 88 integration tests and 242 simulation tests passed; Gradle returned zero for both runs. One explicitly opt-in 3,000-game memory stress test was skipped by design. The strict checker now permits *only* that named skip. Evidence: `leaf-v38-verification-20261105-135919-2148522.tgz`, git `3304fa6f1351376d249ab2be443a44532a8ed3cd`. |
| **V38-5** | Audit special interactions not proved by the matrix (Wisp's Resolve mid-action and multiple uses; Wispquake locked rows; Pollen Theft ownership; Root & Scoot withdrawal; Sunlight action accounting; per-row cleanup/Refresh and other card targeting) | **PASS — November 5 Ubuntu evidence reviewed.** Both focused Gradle runs succeeded; 16 tests passed (Wispquake 3, Wisp's Resolve 4, cross-effect 2, Engagement scheduler 7), zero failed/skipped. Evidence: `leaf-v38-interactions-20261105-141414-2152905.tgz`. This closes the focused V38-5 acceptance, not a claim of exhaustive all-card interactions. See `doc/battle/V38_01_SPECIAL_INTERACTION_AUDIT.md`. |
| **V38-6** | Move Three Engagements scheduler into `src/main/` and verify a main-only runtime executes the adopted default | **PASS — November 5 Ubuntu evidence reviewed.** Main-only tests 2, Battle routing 4, full-game default routing 1, full-game matrix 6, scheduler 7: **20 passed, zero failed/skipped.** Evidence: `leaf-v38-main-runtime-20261105-142950-2153929.tgz`, Git `7fbad74cf11364de7818aeab92cd84fa6ab47e14`; see `doc/battle/V38_06_MAIN_RUNTIME_MIGRATION.md`. |
| **V38-7** | Record rulebook/CSV/ruleset fingerprints and distinguish archived v37 cohorts from new v38 research | **PASS — November 5 Ubuntu evidence reviewed.** Seven pilot stages completed successfully: Python question/provenance tests, Kotlin compile/option tests, and both paired-frozen and adapted/countersearch stages for Root Four More and Sapping Snapdragon. Each stage archive is checksum-verified; ruleset fingerprint is v38-01 `THREE_ENGAGEMENTS`. Evidence: `leaf-v38-question-runner-verification-20261105-144745.tgz`. Engineering verification only; no card-balance verdict. |

**V38-4 through V38-7 passed. Optional repeat of the provenance/question-runner pilot:**

```bash
bin/verify_v38_question_runner --plan-only
bin/verify_v38_question_runner
```

**To repeat the full v38 regression evidence collection** (optional after code changes):

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

**Status:** IN PROGRESS — A1.1–A1.3 complete; A1.4/A1.5 v38 two-card functional path verified on Ubuntu. The new A1.6 evidence reader produces a common schema and human report for that pilot; A1.7 functional vertical slice passed, while its full arbitrary-condition acceptance remains open. Unsupported effect/price conditions, additional player counts, robust uncertainty, and A2 learning strength remain open.**

This is the correct next step for the current repository. The source already contains much of the machinery needed for reusable Plant research, but it is spread across several generations of focused experiments:

- `R3APlantScreenMain` accepts an arbitrary current Plant target and legal nine-card Grove, and can run targeted versus unrestricted Buy-policy searches; its direct historical runner defaults to legacy, and the new question adapter explicitly selects either v38 or legacy;
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

**Status:** PARTIAL — official/suppressed paired-frozen and adapted/countersearch Buy-only execution passed November 5 v38 Ubuntu pilot for two unrelated cards. Unsupported conditions/player counts and full declared-stage coverage remain open.

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

**Status:** IN PROGRESS — November 5 v38 Ubuntu pilot verified frozen, adapted Buy and countersearch stages for Root Four More and Sapping Snapdragon. Stronger opponent portfolios, additional conditions and multiplayer remain open.

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

**Status:** PARTIAL — November 5 v38 two-card evidence reader now consumes the verified pilot archive, verifies cross-stage identity/controls, and produces a common `evidence.json` + `ASSESSMENT.md` with explicit insufficiency findings. See [`PLANT_QUESTION_EVIDENCE.md`](doc/research/experiments/card-balance/PLANT_QUESTION_EVIDENCE.md). Arbitrary conditions, adequate uncertainty, effect-specific attribution and multi-Grove studies remain open.

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

**Status:** FUNCTIONAL v38 TWO-CARD SLICE PASS — November 5 Ubuntu pilot successfully ran both unrelated card questions using one generic path, with discovery, paired-frozen, adapted and countersearch stages. A1.6 common evidence reader verified those outputs. Full acceptance remains open for arbitrary effects/conditions and research-quality reports.

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

