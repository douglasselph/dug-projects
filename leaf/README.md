# Leaf & Let Die - Simulation Research Repository

This repository contains the **analysis-only Kotlin rules engine and research tooling** for the board game **Leaf & Let Die**. It is not a player-facing implementation. Its purpose is to make concrete game-design questions **testable, reproducible, and interpretable**.

## Start here - especially for a new ChatGPT/coding conversation

Before proposing code changes or experiments:

1. Read this `README.md`.
2. Read [`TASKS.md`](TASKS.md) for the **current active backlog and deferred work**.
3. Read [`doc/LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md`](doc/LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md) for the governing research philosophy and evidence standards.
4. Read [`doc/engineering/SIMULATION_LAUNCH_ARCHITECTURE.md`](doc/engineering/SIMULATION_LAUNCH_ARCHITECTURE.md) before changing launchers, runners, training/evaluation scripts, checkpointing, or long-running experiments.
5. Read [`doc/engineering/SIMULATION_OPERATIONAL_READINESS.md`](doc/engineering/SIMULATION_OPERATIONAL_READINESS.md) before making claims about the current simulator's operational readiness.
6. For current v38 Battle work, read [`doc/battle/V38_01_CURRENT_STATUS.md`](doc/battle/V38_01_CURRENT_STATUS.md) and [`doc/battle/BATTLE_STRUCTURE_CONFIGURATION.md`](doc/battle/BATTLE_STRUCTURE_CONFIGURATION.md). The older [`BATTLE_PACING_RESEARCH_STATUS.md`](doc/battle/BATTLE_PACING_RESEARCH_STATUS.md) documents the pre-adoption v37 research, not current rules.
7. Inspect the **current source and data files themselves** before claiming a mechanic, policy, effect, task, or experiment is implemented or current.

Do **not** treat every older task/checkpoint document under `doc/` as an active task. Many are retained implementation or research records. [`doc/README.md`](doc/README.md) distinguishes current governing documents from historical detail.

---

## What this code is for

The simulator is a scientific instrument for answering design questions such as:

- Is a Plant effect meaningfully useful, effectively dead, or dangerously dominant?
- Is a card reasonably priced at its actual cost, printed VP, grafting constraints, activation opportunity cost, and alternatives?
- Do Wisps or Round effects create excessive random advantage or crowd out strategic choices?
- Are multiple genuinely different development paths viable?
- Can expert play collapse into a simple generally applicable recipe such as "always take X" or "ignore Y"?
- Does better decision-making materially improve outcomes despite substantial dice luck?
- Do rules such as Battle sequencing create order bias, runaway effects, or excessive passive downtime?
- Can a rule, option, or subsystem be simplified or removed without sacrificing meaningful strategy?

The desired output is **not** merely a stronger AI, a trained weights file, or a win rate against Human Baseline. The desired output is a **defensible, actionable answer to a specific game-design question**, with evidence, uncertainty, limitations, and a sensible next experiment.

## Governing design goal: strategic irreducibility

A strong strategy is not automatically a design problem. A learned policy beating Human Baseline is expected.

The serious failure is **strategic collapse**: after strong play and counterplay develop, winning can still be summarized by a short, low-context prescription that remains superior across ordinary game states and adaptive opponents.

Healthy expert play should continue to branch. Different strong approaches may emphasize Plants, dice, Mulch, Water, Sunlight, Compost, particular synergies, or Battle tactics, provided their success depends on context, timing, combinations, opportunity cost, and opponent response.

Healthy design does **not** require equal purchase rates or equal action frequencies. A niche card or action can be healthy. A frequently used option can also be healthy if its correct use is genuinely contextual and counterable.

See [`doc/LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md`](doc/LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md).

---

## Card balance: keep effect utility and purchase utility separate

Every Plant-card investigation should distinguish at least these two questions:

**Effect utility** - Does resolving the effect meaningfully improve competitive outcomes compared with an otherwise equivalent suppressed/alternative effect?

**Purchase utility / pricing** - Is acquiring the card at its actual cost strategically worthwhile compared with the legal alternatives, after considering printed VP, grafting, timing, payment/overpayment, activation opportunity cost, and what the player gives up instead?

A card can therefore have:

- a useful effect but be overpriced;
- a weak effect but still be purchasable for printed VP or positional reasons;
- a strong effect that is sufficiently costly/contextual to remain healthy;
- a good price and a dominant effect, which is a different problem.

Purchase frequency is evidence, not a verdict. A card is not automatically weak because one limited policy rarely buys it, and not automatically dominant because one learner buys it often.

The current high-priority research direction is an **all-card sanity and pricing audit** that can reliably find both dead/underpowered effects and dominant effects while separately identifying suspicious card pricing. See [`TASKS.md`](TASKS.md).

---

## Evidence discipline

The project separates three different kinds of work:

- **Unit tests**: does an individual rule/helper/scorer behave as intended?
- **Integration tests**: does the real engine produce the correct result in a fully specified legal situation?
- **Simulation research**: what tendencies emerge over repeated complete games or paired states under controlled strategies, seeds, seats, cards, and experimental conditions?

Important rules for interpreting evidence:

- Human Baseline is a **control, diagnostic, and opponent**, not the objective function defining good play.
- Mechanical Control is deliberately naive and exists for deterministic correctness/smoke work.
- Learned-policy behavior is not automatically strategic truth; verify held-out success, policy expressiveness, actual effect use, and counterplay.
- An effect that was not adequately exercised cannot receive a reassuring balance verdict.
- Missing search coverage, missing countersearch, weak policy capability, or wide uncertainty means **Investigate**, not "balanced".
- Distinguish observed correlation, policy preference, causal intervention, and human-time estimates.
- For matched experiments, preserve seeds/seats and report the real independence unit; repeated decisions inside one game are not independent trials.
- Do not silently combine historical results across changed card definitions, data fingerprints, rules, or policy weights.

---

## Current state - November 4, 2026

### Operational simulator

The October 30 operational readiness gate passed in the supplied project history: Kotlin regressions, Mechanical Control and Human Baseline complete-game smoke tests, A03 protected-boundary verification, and a small experimental-control probe. That establishes **operational usability**, not a 36-card balance certificate or perfect Human Baseline calibration.

Human Baseline Battle behavior is usable for exploratory research. Detailed C04 human-preference fitting remains intentionally deferred unless a concrete experiment exposes a pathological choice or a research blocker.

The source contains the C01-C05 calibration increments. C04 remains diagnostic/partial, and C05 explicitly states that broad phase-wide preference unification is not complete. Do not restart the entire calibration backlog merely because those historical phases exist.

### Card-effect research infrastructure

The source contains R0/R1/R2/R3-era research infrastructure, including targeted/unrestricted searches, effect overrides, R3B/R3B.1 controls, one-sided effect isolation, effect-output telemetry, deterministic/provenance safeguards, and existing Buy/development reporting. The latest governing research document treats the initial Sapping-oriented R0-R3 path as completed enough to move toward reusable questions rather than continuing to build one bespoke study at a time.

R3B.1a added effect-aware Human Baseline handling for the simple research controls (no-op, +20 VP, Raise +20 and Draw 4), closing an important calibration defect. The remaining limitation is broader: **mechanical execution and local effect-aware scoring are still not the same as discovering the strongest strategy built around an arbitrary effect**. An evaluation must establish that the tested policies can recognize and exploit the relevant effect/combination well enough before making strong underpowered/dominant claims.

The v38 official card-text synchronization changed **definition fingerprints** even where card effect semantics were unchanged. The Root Four More and Sapping Snapdragon *example question specifications* have been deliberately re-fingerprinted to the current official CSV rows (without changing their explicit historical `LEGACY_WHOLE_BATTLE` selection). Their old fingerprints remain historical provenance, not current valid inputs. On this documentation checkpoint, **80 Python research-tool tests passed** after the example correction; these do not exercise the Kotlin game engine or certify the old experiment artifacts against v38.

### v38-01 Battle rules migration and validation

The **Three Engagements** Battle system is now the **adopted rule and code default**, not merely experimental Candidate B. The existing whole-Battle implementation remains selectable strictly for historical comparisons:

- `THREE_ENGAGEMENTS` — v38 default, even if the optional settings file/property is missing.
- `LEGACY_WHOLE_BATTLE` — explicit pre-v38 whole-Battle override. The former `CANONICAL` selector is invalid and rejected with a migration hint.

The optional setting is `battle.structure` in `data/research/resync/game-rules-current.properties`. A game can also override the choice via `GameConfig`.

Source-level changes now include separate **Resolve → Doom → Cleanup** for each High/Middle/Low Strike, lowest-value-only row Doom, row-scoped dice/Critter cleanup, a **single final Refresh**, and Wisp's Resolve finishing an unresolved Strike immediately. The authoritative Root and Vine/Flower CSV wordings were synchronized (15 entries; 14 added converter mappings); the Wisp's Resolve text is present; and Compost has an unavailable-next-size Trash fallback. These changes are implemented and have focused tests, but **do not interpret their presence as complete execution or tabletop rules-fidelity certification**.

The new [`V38BattleStructureFullGameRegressionTest`](src/simulationTest/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/V38BattleStructureFullGameRegressionTest.kt) runs **2/3/4 players × both Battle structures**, replayed with identical seeds, for the standard 3/2/2 ten-round game. **As of this archive there is no attached Ubuntu test output establishing that all six cases passed.** Run the matrix and full regression suite before declaring the v38 migration verified. Additional special-effect interaction tests and a full v38 rules-compliance audit remain open.

**Runtime limitation:** the adopted scheduler is still in `src/simulation/` and requires the simulation runtime provider. A main-only runtime cannot execute the adopted default; tests that intentionally cover the old production path explicitly select legacy behavior. This is an architectural follow-up, not a reason to silently run legacy when v38 is selected.

The historical v37 Battle-pacing A/B studies and R3A research adapters remain useful, but are **not automatically valid evidence about v38 gameplay**; historical R3A runs are explicitly pinned to legacy. Task 6 modeled pacing estimates remain uncalibrated against human minutes, and Task 7 fairness/counterfactual acceptance remains open. See [`doc/battle/V38_01_CURRENT_STATUS.md`](doc/battle/V38_01_CURRENT_STATUS.md) for current status and [`doc/battle/BATTLE_PACING_RESEARCH_STATUS.md`](doc/battle/BATTLE_PACING_RESEARCH_STATUS.md) for the historical experiment record.

---

## Rules and data sources of truth

Before a serious experiment, record and verify the exact sources actually loaded.

### Canonical card data

The official data files are the primary simulator sources for current card definitions:

```text
data/Cultivation_Cards - Root.csv
data/Cultivation_Cards - VF.csv
data/Wisp_Cards.csv
data/Round_Cards.csv
```

**Canonical catalog contract:** `Round_Cards.csv` is the only official Round CSV;
`Turn_Cards.csv` has been removed. Run the catalog audit before research:

```bash
python3 bin/audit_authoritative_cards.py --out ~/Downloads/leaf-cards-manifest.json
```

The [catalog source-of-truth guide](doc/research/provenance/card-catalogs/CARD_CATALOG_SOURCE_OF_TRUTH.md) describes the four official input files, validation boundaries, and reproducible manifests. Active code, tests, and documentation are named for their purpose rather than transient task IDs; milestone IDs belong in `TASKS.md` and historical records.

It validates and fingerprints the four canonical files. Every authoritative effect text now has a deliberate `GameEffect` identity, but 16 revised mechanical identities remain explicitly unimplemented. `CardEffectReadiness` rejects incomplete effects before simulation instead of treating them as old behavior or harmless no-ops. **Full-game runs against the updated catalog remain blocked until the individual effect implementations and policies are ready.**

Card **names** are preferred human-facing identifiers. IDs are trace keys and can change as catalogs are reorganized. Preserve fingerprints/checksums when comparing historical results.

### Research resync files

`data/research/resync/resync-current.csv` and `round-resync-current.csv` are opt-in research overlays. In the current source snapshot they are header-only after the October official-data migration. Do not assume old resync interventions are still active.

Game-level experimental rules belong separately in `data/research/resync/game-rules-current.properties`; do not put Battle-structure selection into Plant-card override CSVs.

### Round structure

The intended current research/game structure is **3/2/2**:

```text
3 Cultivation -> Battle -> 2 Cultivation -> Battle -> 2 Cultivation -> Battle
```

### Rulebook

The **authoritative current tabletop rulebook** is [`doc/rules/Leaf_and_Let_Die_v38-01.pdf`](doc/rules/Leaf_and_Let_Die_v38-01.pdf). The previous [`v37-03`](doc/legacy/Leaf_and_Let_Die_v37-03.pdf) and [`v36-01`](doc/legacy/Leaf_and_Let_Die_v36-01.pdf) rulebooks are historical references only. For the simulator, the official CSVs above are the card-definition source of truth; a research override is **never** automatically an official rule.

---

## Architecture and policy roles

Important source sets:

```text
src/main/          production rules engine
src/test/          unit tests
src/integration/   deterministic real-engine scenario tests
src/simulation/    research, training, evaluation, experiments
src/simulationTest/ simulation-layer tests
```

Current strategic roles include:

- **Mechanical Control** - deterministic/legal/naive correctness control.
- **Human Baseline** - hand-authored approximation of reasonable experienced play; control, not strategic truth.
- **Learned/frozen policy families** - modular decision-family policies used for stronger or isolated experiments.
- **Targeted/research policies** - experiment-specific exploration and countersearch where needed.

Keep mechanical randomness separate from strategy randomness. Preserve deterministic seeds and provenance.

For deeper architecture, see [`doc/engineering/CODE_ARCHITECTURE.md`](doc/engineering/CODE_ARCHITECTURE.md) and [`doc/engineering/USING_THE_SIMULATOR.md`](doc/engineering/USING_THE_SIMULATOR.md).

---

## Operational and developer workflow requirements

The user is an experienced programmer working on Ubuntu. Prefer small, reviewable, reproducible changes.

When changing source or research tooling:

- inspect the **current source first**;
- preserve canonical mechanics unless a rule change is explicitly requested;
- respect A03 protected boundaries and existing research/provenance contracts;
- prefer focused legal-state tests and the smallest credible experiment;
- keep strategy RNG separate from mechanical RNG;
- do not silently replace frozen policies, canonical data, or approved effects;
- say exactly which tests/commands actually ran and which did not;
- never claim a test passed merely because code compiled or a script parsed.

When delivering changes, prefer a **changes-only ZIP rooted at the repository root**. ZIP entries should begin with paths such as `src/`, `bin/`, `data/`, `doc/`, `README.md`, or `TASKS.md` - not an extra enclosing `leaf/` directory. Exclude `build/`, `.gradle/`, `output/`, caches, and temporary files. Provide a suggested Git commit message.

Installation commands should assume the ZIP is in `~/Downloads`, for example:

```bash
unzip -o ~/Downloads/<changed-files.zip>
```

For long-running research runs, preserve the compile-once/direct-Java and persistent-JVM architecture where appropriate, while retaining process isolation where policy/condition state requires it. Runs should be resumable, visibly report current stage plus whole-batch progress, elapsed time, ETA and projected finish time, use `|& tee`, and produce **one verified upload-ready archive copied to `~/Downloads`** with an obvious `UPLOAD THIS FILE:` path.

See [`doc/engineering/SIMULATION_LAUNCH_ARCHITECTURE.md`](doc/engineering/SIMULATION_LAUNCH_ARCHITECTURE.md).

---

## Quick verification

Operational gate:

```bash
bash doc/SIMULATION_OPERATIONAL_READINESS.sh
cat output/readiness/latest/summary.txt
```

Optional small experimental-control probe:

```bash
bash doc/SIMULATION_OPERATIONAL_READINESS.sh --extremes
```

Standard source-set regression commands:

```bash
./gradlew test
./gradlew integrationTest
./gradlew simulationTest
```

A convenient combined regression is:

```bash
./gradlew test integrationTest simulationTest
```

For the required v38 deterministic full-game matrix (**six cases, two seeded executions each**):

```bash
set -o pipefail
./gradlew simulationTest \
  --tests '*V38BattleStructureFullGameRegressionTest' \
  --rerun-tasks |& tee ~/Downloads/leaf-v38-full-game-regression.log
```

The matrix is **present, not independently confirmed passing** in this archive. Preserve the resulting JUnit report/log and record any failures in `TASKS.md` before running large research batches. The old readiness gate predates the v38 migration; its October 30 PASS is not a v38 certificate. Do not treat an operational PASS as proof of balance, statistical power, policy optimality, or human pacing.

---

## Documentation map

For current work, use these as the front door:

- [`TASKS.md`](TASKS.md) - active and deferred backlog.
- [`doc/README.md`](doc/README.md) - documentation map and current-vs-historical guidance.
- [`doc/LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md`](doc/LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md) - research mission, evidence standards, card assessment method, strategy-diversity goals.
- [`doc/engineering/SIMULATION_LAUNCH_ARCHITECTURE.md`](doc/engineering/SIMULATION_LAUNCH_ARCHITECTURE.md) - execution architecture and operational constraints.
- [`doc/engineering/SIMULATION_OPERATIONAL_READINESS.md`](doc/engineering/SIMULATION_OPERATIONAL_READINESS.md) - stop-and-use-it gate.
- [`doc/battle/V38_01_CURRENT_STATUS.md`](doc/battle/V38_01_CURRENT_STATUS.md) - current Battle adoption, migration, verification and open work.
- [`doc/battle/BATTLE_STRUCTURE_CONFIGURATION.md`](doc/battle/BATTLE_STRUCTURE_CONFIGURATION.md) - current simulation Battle-selection configuration.
- [`doc/battle/BATTLE_PACING_RESEARCH_STATUS.md`](doc/battle/BATTLE_PACING_RESEARCH_STATUS.md) - historical v37 pacing A/B research status; not the v38 adoption status.
- [`doc/engineering/CODE_ARCHITECTURE.md`](doc/engineering/CODE_ARCHITECTURE.md) - programming architecture.
- [`doc/engineering/USING_THE_SIMULATOR.md`](doc/engineering/USING_THE_SIMULATOR.md) - practical simulator workflow.
- [`doc/engineering/TESTING_AND_VERIFICATION.md`](doc/engineering/TESTING_AND_VERIFICATION.md) - verification boundaries and commands.

The repository is the handoff. A fresh conversation should not require a separate stand-alone Codex/status prompt if it has the current source tree and reads the files above.


## Repository-root file conventions

The root keeps `README.md` (orientation), `TASKS.md` (active backlog), and `AGENTS.md` (coding-agent Kotlin style rules). Gradle configuration and wrapper entry points stay at the root. Subject-specific historical notes live under `doc/`; see [documentation index](doc/README.md).

### v38 reusable Plant-question research checkpoint

`V38-6` main-runtime integration passed on November 5 (20 focused Kotlin tests), and `V38-7` passed its seven-stage Ubuntu v38 two-question pilot. Its direct-Java output was independently reviewed with `bin/plant_question_evidence.py`, which produces comparable `evidence.json` and `ASSESSMENT.md` reports after verifying original stage-archive checksums, paired suppression and ruleset identities. Run `python3 bin/plant_question_evidence.py ~/Downloads/leaf-v38-question-runner-verification-20261105-144745.tgz` to recreate the report without new games. See `doc/research/experiments/card-balance/PLANT_QUESTION_EVIDENCE.md` and `TASKS.md`. The pilot is intentionally too small for any card-balance conclusion; old legacy Battle cohorts remain separate.
