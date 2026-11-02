# Leaf & Let Die - Research Goals and Experiment Design

**Status:** Governing research specification, consolidated November 2, 2026  
**Audience:** Game designer, coding collaborators, and new ChatGPT conversations  
**Scope:** Simulation-based design research for *Leaf & Let Die*. This document does **not** itself adopt or change any game rule.

> **Mission:** Use the simulator as a scientific instrument to discover whether important game elements are meaningfully useful, excessively powerful, badly priced, overly random, or strategically simplifying - and whether the game continues to support multiple credible ways to win under strong play and counterplay.

The primary output is not a trained weights file or a win rate against Human Baseline. It is a **defensible, actionable assessment of a concrete design question**, with traceable evidence, limitations, uncertainty, and the next sensible experiment.

Current active work and deferred items are tracked in [`../TASKS.md`](../TASKS.md). Historical phase/checkpoint documents do not override that task list.

---

## 1. Overall design success condition

*Leaf & Let Die* deliberately contains substantial dice luck. It should nevertheless reward good decisions and preserve meaningful strategic branching.

Players should have real reasons to pursue different combinations of:

- Plant cards and Plant shape;
- dice development;
- card synergies;
- resources such as Mulch, Water, Sunlight, Bees and Worms;
- Wisp timing;
- Battle tactics;
- hybrid development paths.

Not every card must be purchased equally. Not every strategy must win equally often. Niche and context-sensitive options are healthy when they have legitimate situations where strong play values them.

### Strategic irreducibility

The overarching failure to avoid is **strategic collapse**: after experienced/learned play and counterplay are allowed to develop, success can still be compressed into a short generally applicable prescription such as:

- always take one resource whenever legal;
- always pursue one Plant shape;
- always buy one narrow family;
- ignore a major game subsystem;
- always use one helper in one way;
- or follow some other low-context recipe that remains superior after opponents adapt.

A **strong strategy is not the same as a simplified exploit**. Strong strategies are desirable when their success depends on conditions, sacrifices, opponent response, combinations, and timing.

Evidence of healthy depth includes independent strong strategies making meaningfully different choices, counters emerging against strong approaches, and expert play becoming more conditional rather than collapsing toward one universal rule.

### Simplification is also a goal

Complexity is not valuable merely because the simulator can model it. If an option, rule, or subsystem contributes little strategic or experiential value, investigate whether it can be removed or merged without damaging meaningful choice.

---

## 2. Research goals, in priority order

| ID | Research objective | Core question | Main output |
|---|---|---|---|
| **A - Card effect and pricing sanity** | Test a Plant's effect and its actual acquisition economics | Is the effect useful without being dominant? Is the card reasonably priced relative to alternatives? | Per-card effect assessment + purchase/pricing assessment |
| **B - Random/shared effect balance** | Test Wisps and Round Cards | Do accidental rewards or shared effects overwhelm strategic decisions or unfairly favor an approach? | Controlled effect-impact and luck-sensitivity analyses |
| **C - Strategic diversity** | Explore specified nine-card Groves | Are there several genuinely distinct competitive Plant/dice development paths? | Strategy profiles, matchup matrices, diversity assessment |
| **D - Degenerate/dominant approaches** | Deliberately seek exploits | Can dice-only, Plants-only, Bee/Mulch/cheap-Root or other extreme approaches remain broadly superior after counterplay? | Strongest constrained strategies and counters |
| **E - Skill versus luck** | Quantify payoff to good decisions | Does superior strategic/tactical play materially improve outcomes across chance and opponents? | Strength gaps, matched win rates, uncertainty, seat/seed sensitivity |
| **F - Rules/pacing quality** | Test structural rules such as Battle sequencing | Does a rule reduce downtime or complexity without creating order bias, snowballing or strategy damage? | Paired rule comparisons + human-calibrated pacing evidence |

These objectives share one engine and reusable research machinery. They do **not** all require the same learner or experiment design. Answer the question with the **smallest credible experiment**.

---

## 3. The central card-balance distinction: effect utility vs purchase utility

Every Plant investigation must distinguish the card's **effect** from the card's **purchase economics**.

### Effect utility

Does using/resolving the effect improve competitive outcomes compared with an otherwise equivalent suppressed or alternative effect?

A useful method is an effect-suppression comparison that leaves the card's cost, printed VP, grafting and identity unchanged. This helps isolate the special ability from the rest of the card.

### Purchase utility / pricing

At the actual cost and under actual purchase rules, is buying the card strategically worthwhile compared with legal alternatives?

Pricing analysis must account for:

- exact payment feasibility: dice are not one pooled total;
- overpayment and Critter use;
- other simultaneously legal purchases/actions;
- printed VP;
- grafting requirements and layout constraints;
- remaining time to exploit the card;
- activation opportunity cost;
- duplicate-copy pressure;
- what development path is forgone.

A card can therefore have a good effect and still be overpriced, or have a weak effect and still be purchased because its price/VP/context makes the card worthwhile.

**Purchase frequency is a clue, not a verdict.** Zero purchases by a limited policy means that policy avoided it; it does not prove no winning strategy exists. Very frequent purchase by one learner does not prove dominance.

---

## 4. What "Is this card/effect okay?" means

For every Plant, separate at least these questions:

1. **Opportunity** - Did the card appear in a legal usable market, and could a player reasonably acquire, graft and activate it in time?
2. **Discoverability** - Can deliberate search find a coherent strategy that meaningfully uses it? Track actual acquisition and effect use, not merely a positive model score.
3. **Effect contribution** - Compared with a suppressed/alternative effect, what competitive value did the effect actually add?
4. **Purchase value** - At its actual price, was the card worth acquiring compared with legal alternatives?
5. **Dominance** - Do unrestricted strong strategies converge on it broadly, and can credible counterstrategies compete?
6. **Context dependence** - Is the card healthy but specialized to particular Groves, builds, stages, player counts, or opponents?

A situational card can be well balanced while rarely purchased. A card can be widely attractive without being broken if its value is conditional and counterable.

### Per-card report requirements

A completed Plant report should contain:

1. **Identity and question** - card name, current ID, type/slot, cost, printed VP, exact current effect wording, compared variant/suppression, definition fingerprints.
2. **Scope** - exact nine-card Grove(s), 2/3/4-player scope, Round/Wisp/game-rule settings, source revision, independent seeds/learners.
3. **Search coverage** - targeted card-centered search, unrestricted alternatives, countersearch, actual acquisition/use evidence.
4. **Development evidence** - named cards/copy counts, graft survival/Snips, dice by size, upgrades/trash/Doom, resources, timing and activations.
5. **Effect evidence** - legal opportunities, actual resolutions, actual mechanical outputs, caps/supply/row constraints, Strike/VP/Wound changes where attributable.
6. **Purchase/pricing evidence** - simultaneous legal alternatives, actual payment resources/overpayment, alternatives forgone, duplicate pressure and opportunity cost.
7. **Competitive evidence** - matchup/opponent-portfolio win share, total VP, Strike/Battle VP, end-game VP where attributable, uncertainty.
8. **Causal comparison** - matched official-vs-suppressed/variant comparisons, frozen and adapted strategies when appropriate.
9. **Robustness** - fresh held-out seeds, seat rotations, independently learned policies, favorable/unfavorable Groves and counterstrategies.
10. **Decision** - **No concern demonstrated**, **Investigate**, or **Strong balance concern**, plus direction such as weak effect, overpriced, dominant, context-specific, or insufficient evidence.

An experiment cannot conclude **No concern demonstrated** merely because its sample was small or its optimizer failed to find an exploit.

---

## 5. Assessment states and statistical discipline

These are research triage rules, not mathematical definitions of balance.

### No concern demonstrated

Requires adequate search/exercise and held-out precision, at least one genuinely competitive use in appropriate contexts, and no replicated practically large problem after alternatives/counters are considered.

### Investigate

Use when evidence is incomplete or ambiguous, including:

- no/rare actual acquisition or activation;
- policies that cannot competently use the effect;
- conflicting learners;
- wide uncertainty;
- effect heavily constrained by capacity/supply;
- one favorable Grove/opponent only;
- missing countersearch;
- suspicious pricing without a legal-market comparison.

### Strong balance concern

Use only after replicated meaningful evidence and countersearch. Examples include:

- targeted search repeatedly fails to find viable effect use in favorable contexts;
- an effect/purchase creates a large held-out advantage that survives alternative strong strategies and counterplay;
- nearly all robust strategies depend on the same card or compact route;
- the card's actual cost makes an otherwise useful effect consistently uneconomic.

For preliminary two-player matched win-share differences, a 5 percentage-point attention band and 10-point serious-effect band can be useful triage defaults, but they are not universal thresholds and must not be copied directly onto raw 3/4-player shares.

Always report counts, effect-size differences in percentage points, uncertainty, and the actual independence unit. Do not make intervals artificially narrow by treating repeated decisions from one game as independent samples.

---

## 6. Controls before trusting an all-card screen

A balance instrument should prove that it can recognize deliberately broken conditions before issuing reassuring results.

Useful controls include:

- **negative/no-effect control** - preserve cost/VP/card identity but suppress the effect;
- **obvious direct positive control** - e.g. a deliberately absurd immediate VP reward;
- **mechanically meaningful non-VP positive control** - a deliberately stronger effect that is demonstrably exercised rather than merely stronger on paper.

For every control verify:

- the effect actually executed as intended;
- the targeted seat/condition received it and controls did not;
- actual realized output was measured;
- policies had an opportunity and ability to exploit it;
- untreated matched conditions remain equivalent where they should;
- a failed or under-exercised control produces **Investigate**, not a false pass.

R3B/R3B.1 established important parts of this machinery, and R3B.1a added effect-aware Human Baseline scoring/targeting for the simple calibration controls. The remaining concern is broader strategic exploitation: local effect-aware scoring does not prove that the search can discover the strongest multi-turn or combinational strategy around an arbitrary effect.

---

## 7. Beyond Plants

### Wisps

The central question is not whether players choose to acquire Wisps; many are received from rolls. Measure whether Wisp benefit itself, rather than simply having more/better dice or more rolls, creates excessive random advantage.

Track 2-results relative to dice rolled, Wisp gains, actual Wisp identities/effects, held versus played behavior, timing, final VP and matched causal comparisons where feasible.

### Round Cards

Test a specific Round effect against a precise alternative/neutral reference while keeping the rest of the rules matched. Measure tactical/economic consequences, strategy substitution, VP and player/seat effects. Preserve the **actual effect identity**, not merely `ROUND_EFFECT_1`/`ROUND_EFFECT_2`.

### Strategy diversity

For a fixed Grove, search a population of genuinely different strategies and evaluate finalists in a common held-out matchup matrix with rotated seats. Distinguish robust alternatives, specialists and strictly dominated strategies. Five nearly identical learners are not five distinct strategic paths.

### Degenerate strategies

Intentionally search strong versions of hypotheses such as mostly-dice/no-Plants, mostly-Plants, cheap-Root VP, Bee accumulation, Compost-heavy, Mulch-heavy and unrestricted hybrids. If an extreme looks superior, search for counters before calling it dominant.

### Skill versus luck

Compare demonstrably stronger/weaker decision policies under matched rules, seeds and opponents. Better policies winning more often demonstrates skill influence even though luck can still produce frequent upsets. Equal-strength self-play near 50/50 says nothing about skill ceiling by itself.

---

## 8. Shared research architecture

Avoid separate simulator forks or a bespoke program for every question. Prefer one rules engine and reusable question-driven machinery:

1. **Question specification** - exact hypothesis, effect/cost alternatives, Grove, players, rules, policies and practical effect margin.
2. **Immutable scenario/rules manifest** - source revision, canonical and variant data fingerprints, exact Grove, game rules, scoring config, AI code/weights and seeds.
3. **Policy adapters** - Human Baseline, Mechanical Control, frozen experts, contextual/search agents, targeted explorers and counterstrategies under common legal interfaces.
4. **Search harness** - targeted and unrestricted discovery, opponent portfolio, diversity preservation, seat rotation and countersearch.
5. **Controlled evaluation** - frozen/adapted comparisons, matched initial conditions where meaningful, held-out randomness and fair seats.
6. **Shared telemetry** - named acquisition/development, payment/substitution data, effect opportunities/outputs, activations, Strikes/Wounds/VP, strategy profile and outcome.
7. **Question-specific reporting** - verdict first, then evidence, effect/pricing separation, uncertainty, counterexamples and complete reproducibility manifest.

A new card question should ideally require a new scenario/question specification and perhaps one effect override - **not a new experiment architecture**.

---

## 9. Current roadmap

The initial focused card-effect path established substantial reusable machinery:

| Step | Current interpretation |
|---|---|
| **R0 - Correctness audit** | Completed enough for current research; preserve audits/fingerprints |
| **R1 - Question runner MVP** | Completed for initial focused question |
| **R2 - Targeted and unrestricted search** | Implemented for focused studies; generalization still matters |
| **R3 - Competitive verification** | Implemented enough for focused studies, with known policy/effect-awareness limitations |
| **R4 - Reusable questions** | **Current active engineering direction** - generalize to arbitrary Plants, then Wisp/Round questions |
| **R5 - Wider landscape** | Later - strategic diversity, degeneracy, multiplayer, skill/luck |

The all-36-card audit should not begin as a blind giant overnight run merely because these components exist. First prove the generalized path on multiple representative cards and confirm that positive/negative controls are actually exercised and understood.

Human Baseline fine calibration (including detailed C04 follow-up) is **not** the current project goal. Reopen it only when a specific research question is blocked by a concrete decision-quality problem.

Battle pacing / Three Engagements is a parallel research stream; its current state is summarized in [`BATTLE_PACING_RESEARCH_STATUS.md`](battle/BATTLE_PACING_RESEARCH_STATUS.md).

---

## 10. Research integrity and operations

- **Names first:** use card titles as primary labels and current IDs as trace keys. Use fingerprints to join historical results.
- **No fake precision:** zero purchases, failed strategy discovery and causal evidence of a weak effect are different findings.
- **Record what was actually tested:** source revision; data/override hashes; exact Grove/effect wording; policies/weights; seeds; opponents; actual acquisition/activation counts; excluded/incomplete cohorts.
- **Preserve canonical boundaries:** experimental overrides must not silently become official rules/data.
- **Use actual legal states:** never infer card/rule quality from an impossible fixture.
- **No dropped failures:** a failed cohort is not a smaller successful sample.
- **Smallest credible experiment:** correctness pilot first, then increase samples only when more precision can change the decision.
- **Long-run operations:** compile once where appropriate, preserve process isolation when needed, checkpoint/resume, visible whole-batch progress, elapsed/ETA/projected finish, `|& tee`, provenance, and one verified upload-ready archive in `~/Downloads`.
- **Developer delivery:** changes-only ZIP rooted at repository root, exact tests run/not run, and a Git commit message.

---

## 11. Definition of success

The simulator succeeds when, given a concrete design question, it can:

- discover credible ways to use the relevant game element;
- compare it with real alternatives at real opportunity cost;
- separate effect contribution from purchase/pricing contribution;
- search for both failure extremes and counterexamples;
- evaluate under held-out conditions with adequate provenance;
- report uncertainty and limitations honestly;
- and give the designer a clear next action.

Examples include:

- Is Sapping Snapdragon's effect useful at its price?
- Is Queen's Blossom desirable enough for its cost without becoming mandatory?
- Is Bee-loved Bloom dominant only in a specific Bee build or broadly dominant?
- Does Compost provide enough strategic value for its action/tempo cost?
- Are Wisp rewards from rolling 2s overpowering strategy?
- Does one Round effect crowd out Plant activations?
- Does Three Engagements improve Battle pacing without creating last-actor advantage or early-Wound snowballing?

**Principle:** *Discover viable strategic uses; measure real opportunity cost; seek both dead and dominant extremes; actively search for counters; report uncertainty; change the game only when the evidence justifies it.*
