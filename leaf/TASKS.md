# Leaf & Let Die - Active Research and Engineering Tasks

**Status date:** November 2, 2026

This is the **active top-level backlog**. It is intentionally shorter than the many historical phase/checkpoint documents under `doc/`.

A historical task ID appearing in an older document does **not** mean that task is still open. Inspect current source/tests/results, then use this file as the current prioritization unless the user explicitly changes it.

## Current priority: all-card sanity and pricing audit

The next major design goal is to examine the current Plant catalog and answer two separate questions for every card:

1. **Effect sanity:** Is the effect meaningfully useful somewhere, effectively dead, or so strong that credible alternatives/counters cannot compete?
2. **Purchase/pricing sanity:** At the card's actual cost, printed VP, grafting constraints, timing, and opportunity cost, is acquiring it strategically worthwhile relative to legal alternatives?

Do not collapse these into one metric. A good effect can be overpriced; a weak effect can still be worth buying for VP; a strong effect can be healthy if sufficiently costly/contextual/counterable.

### A1 - Generalize the question runner beyond one bespoke card study

**Status:** OPEN - highest-priority engineering capability.

The R0-R3/Sapping-era framework and R3B/R3B.1 infrastructure demonstrate much of the needed machinery, but the project still needs a clean reusable definition for an arbitrary current Plant question.

Acceptance target:

- one declarative/specifiable question identifies card by current name/ID/fingerprint;
- exact Grove, player count, Round/Wisp/game-rule configuration are recorded;
- official effect, suppressed effect, candidate effect and/or cost comparison are explicit;
- targeted discovery, unrestricted search, frozen comparison, adapted comparison, held-out evaluation, and countersearch can be selected without a new one-off launcher for every card;
- report records actual acquisitions, activations, mechanical outputs, substitutes, development, Strike/VP outcomes, uncertainty, and provenance;
- a **second unrelated Plant** can run through the same path without bespoke program rewrites.

This corresponds to the governing research document's **R4 - Reusable questions** direction.

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

## Parallel research stream: Battle pacing / Three Engagements

Candidate B remains an experiment, not an adopted game rule.

### B1 - Battle-structure runtime selection

**Status:** IMPLEMENTED IN CURRENT SOURCE; verify in the user's checkout before relying on it.

`data/research/resync/game-rules-current.properties` supports:

```properties
battle.structure=CANONICAL
# or THREE_ENGAGEMENTS
```

The default is canonical. New `GameConfig` instances read the setting. Candidate B requires the simulation runtime provider; production-only use must remain canonical. The explicit A/B experiment runner still selects A/B directly and intentionally ignores this file.

See `doc/BATTLE_STRUCTURE_CONFIGURATION.md`.

### B2 - Validate Task 7 fairness/counterfactual extension

**Status:** OPEN.

Task 7 source/reporting exists, but `doc/TASK7_STATUS.md` explicitly says the package was not fully validated.

If Battle pacing becomes the current design priority, next work is to:

- compile/run the Task 7 extension against the current source;
- validate all paired cohorts without dropped failures;
- condition last-actor outcomes on starting row strength/resources rather than reading raw rates causally;
- compare reference B versus controlled ordering changes carefully;
- add/confirm canonical-A comparators where required;
- measure High-row Wound cascade into later Engagements.

### B3 - Human pacing calibration before tabletop-time claims

**Status:** OPEN / empirical.

Task 6 provides transparent pacing proxies, not measured human minutes. Before making a real-world duration claim:

- calibrate handoff/execution/reassessment/decision coefficients from playtest video/audio timestamps where available;
- compare passive-finished waiting separately from tactically consequential observation;
- report fast/base/slow sensitivity ranges;
- do not equate JVM CPU time with tabletop time.

### B4 - Adoption decision

**Status:** NOT READY / design decision, not code completion.

Do not make Three Engagements canonical merely because it runs. Adoption should consider:

- human downtime improvement;
- last-actor/order advantage;
- High-row Wound cascade;
- support-resource stranding;
- card/resource balance changes;
- learning burden;
- whether the three-row tactical puzzle remains strong.

Until adopted explicitly, `CANONICAL` remains the default.

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

- Preserve canonical game mechanics unless the user explicitly approves a rule change.
- Current standard Round structure is **3/2/2**.
- Sunlight Round effects gain a **Sunlight token**, never legacy Raise +3.
- Roll Reward 1 -> Critter and 2 -> Wisp applies according to current rules in both Cultivation and Battle; do not restore obsolete restrictions from old notes.
- Keep canonical data, research overrides, and game-level experimental rules distinct.
- Use current card names as primary report labels and IDs/fingerprints for traceability.
- Do not silently change frozen weights or promote experimental effects to canonical.
- Do not claim balance from Human Baseline comparisons alone.
- Do not treat repeated legal opportunities inside one game as independent samples.
- If a run or cohort fails, do not drop the failure and analyze the remainder as though complete.
- Long-running runs must preserve provenance, deterministic settings, checkpoint/resume, visible progress, and one upload-ready archive.

