# Leaf & Let Die — Research Goals and Experiment Design

**Status:** Proposed governing specification, 24 October 2026  
**Priority:** Build a reliable answer to **“Is this card effect okay?”** before investing in generalized AI strength.  
**Audience:** The game designer, future coding collaborators, and new ChatGPT conversations.  
**Scope:** Simulation-based design research for *Leaf & Let Die*; **not** a specification to change any current game rule.

> **Mission:** Discover whether particular Plant, Wisp, and Round effects are too weak, too powerful, or appropriately useful—and whether the game supports multiple meaningful ways to win. The simulator is a scientific instrument for testing design questions, **not** principally a machine for beating the Human Baseline.

## 1. What the designer needs to know

The primary output is not a trained weights file or a win rate against the Human Baseline. It is a **defensible, actionable assessment of a concrete design question**, with traceable evidence, limits, and a sensible next experiment.

The game deliberately contains considerable dice luck. It should nevertheless reward skill. Players should have real choices about the *shape* of their Plant Creature and dice collection: a card-oriented build, a dice-oriented build, an effect synergy, or a hybrid should have circumstances in which it is worth pursuing. **Not every strategy needs to win**, and no individual card must be attractive in every Grove. But the game should not systematically collapse into “always buy D20s,” “ignore Plants,” or “everyone must buy this one card.”

The Human Baseline embodies the designer's evolving interpretation of competent play. It is useful as a benchmark, diagnostic, and participant in the opponent pool. It is **not the objective function** defining good strategies. A policy that beats it may have exploited a weakness in the reference rather than found the strongest or most general way to play.

### Research goals, in priority order

| ID | Research objective | Core question | Main output |
|---|---|---|---|
| **A — Card-effect adequacy** | Test a particular Plant effect or proposed change | **Is this card effect okay?** Is it worth acquiring and using in some competitive strategy? Is it too broadly advantageous? | Per-effect evidence and one of three assessment states |
| **B — Random-effect balance** | Test Wisps and Round Cards | Do accidental rewards or shared Round effects overwhelm strategic decisions or unfairly favor an approach? | Controlled effect-impact and luck-sensitivity analyses |
| **C — Strategic diversity** | Explore one specified nine-card Grove | Are there several genuinely distinct, competitive Plant/dice development paths? | Strategy profiles, tournament matchups, diversity assessment |
| **D — Degenerate / dominant approaches** | Deliberately seek exploits | Can a dice-only, Plants-only, Bee-hoarding, repeated-cheap-Root, or other extreme approach beat competent alternatives reliably? | Strongest constrained strategies and available counters |
| **E — Skill versus luck** | Quantify payoff to good decisions | Does superior strategic and tactical play materially improve win probability across chance and opponents? | Strength gaps, matched win rates, uncertainty, seat/seed sensitivity |

These objectives share a simulator, policy interfaces, and reporting infrastructure, but **they do not all need the same training loop**. Answer the posed question with the smallest credible experiment.

## 2. What “Is this card effect okay?” means

For every Plant effect, separate at least four questions:

1. **Opportunity:** Did the card appear in a usable market and could a player reasonably acquire, graft, and activate it in time? This is a screening condition, not proof of value.
2. **Discoverability:** Can a deliberate search find a coherent strategy that makes meaningful use of the card? Merely giving the card a positive score is not enough; track actual acquisition and use.
3. **Competitive value:** Does that strategy hold up against a **varied opponent pool** and against opponents deliberately trained to counter it? How much is lost when the card's effect changes or a different same-tier option is available?
4. **Dominance:** Do unrestricted competitive strategies converge on the card in many contexts? Does using or possessing it create an advantage that other good strategies cannot reasonably contest?

A situational card can be well balanced while rarely purchased. A universally purchased card can be appropriately desirable in some markets. **Purchase frequency is a clue, not a verdict.** In particular, zero purchases by a limited-policy learner means that *learner avoided it*, not that no winning strategy with the card exists.

The evaluation should also recognize the card's **printed VP, acquisition cost, graft prerequisites, timing, activation opportunity cost, and interaction with other cards**. The card effect should not be judged as though acquisition and activation were free.

### Required answer format: a Card Effect Assessment

Every completed card question should produce a concise report with:

1. **Question and alternatives.** Card title, current ID, printed cost/VP, exact current wording, exact candidate wording, and fingerprints of each definition.
2. **Scope.** All nine Grove titles and quantities; 2/3/4 players; Round/Wisp configurations; game revision; number of independent seeds and learners.
3. **Search coverage.** What kinds of strategies were sought, how many independent searches, which alternative and counterstrategies were included, and evidence that the searched card was actually acquired/used.
4. **Development evidence.** Exact named cards and copy counts acquired and retained, Snips, Doom/Trash, dice acquired and remaining, upgrades, relevant activations, and timing, for winners and losers.
5. **Competitive evidence.** Head-to-head and opponent-portfolio win shares, total VP, Battle Strike wins/VP, direct end-game VP where attributable, and uncertainty; distinguish **evaluation without artificial incentives** from training.
6. **Effect-specific causal evidence.** Matched A/B comparisons (current versus proposed wording, or current versus a sensible same-cost comparison), including the opportunity costs and what players buy/do instead.
7. **Robustness.** Fresh held-out seeds, seat rotations, independently learned policies, favorable and unfavorable Groves, counterstrategy searches, and observed failures.
8. **Decision.** **No concern demonstrated**, **Investigate**, or **Strong balance concern**, with **too weak / too strong / context-specific / insufficient evidence** direction, and the next small actionable test.

An experiment cannot honestly conclude **No concern demonstrated** merely because its sample was small or its optimizer did not find an exploit. Missing evidence is reported as **Investigate — insufficient evidence**, not as a clean bill of health.

## 3. Three assessment states: concrete, provisional criteria

These are **decision rules for research triage, not mathematical truths about balance**. The designer should be able to configure practical-effect thresholds before examining results. Start with a **5-percentage-point (pp) attention band** and a **10-pp serious-effect band** for *matched two-player win-share differences*; recalibrate after pilot studies and never directly transfer these cutoffs to raw three-/four-player shares. A meaningful effect could also appear primarily in VP or strategy diversity.

| Assessment | Operational evidence | What to do |
|---|---|---|
| **No concern demonstrated** | Adequate search and held-out precision; at least one genuinely competitive card-using strategy in favorable markets; no replicated practically large A/B win advantage or disadvantage; counters and alternative strategies remain viable | Retain effect; record boundaries and revisit if market/rules change |
| **Investigate** | Zero purchases or uses; conflicting learners; apparent win/VP differences with wide uncertainty; advantages strongly dependent on one market/opponent; card-using strategy has not been adequately searched; or an interesting result without countersearch | Run the smallest targeted experiment resolving the ambiguity |
| **Strong balance concern** | A *replicated, meaningful* advantage or disadvantage after targeted exploration, held-out evaluation, and countersearch. For triage, a matched **≥10 pp** win-share difference whose uncertainty excludes zero, replicated on independent conditions, is a strong quantitative signal—not automatic proof. Alternatively, nearly all robust competitive strategies depend on the card, or substantial targeted search finds no viable use across several favorable contexts | Test a candidate modification; retain counterexamples and uncertainties |

**Direction matters:** “too weak” and “too strong” are separate hypotheses. Different markets may yield different conclusions. A variant can be valuable and situational without being dominant.

**Statistical reporting:** Always show counts, effect-size differences (in **percentage points**, not percent relative change), uncertainty intervals, and the independence unit (game seed/match set, not repeated decisions). Use paired/blocked inference for matched seeds and seats, or cluster/bootstrap appropriately across learner and game blocks; control the risk of cherry-picking across many variants. Do not make the confidence interval narrower by treating repeated buy opportunities within one game as independent data.

**Sample budgets:** Start with a small correctness pilot and an exploratory screen. Increase games *only if the resulting uncertainty could change the assessment*. For a claimed <5-pp equivalence, many more held-out matches may be necessary than for identifying a large ≥10-pp problem. Use power/precision estimates based on observed variance instead of prescribing a giant game count for every question.

**What would not justify a strong conclusion:** One of five experts avoiding a card; zero purchases under a policy that lacks the ability to model its synergy; unpaired winner-versus-loser inventory comparisons; a single favorable seed; a card being acquired in 90% of legal opportunities; or a variant winning against only Human Baseline.

## 4. First reference investigation: Sapping Snapdragon

**Official reference (from the current official Vine/Flower CSV):** **Sapping Snapdragon**, `Flower_11_03`, **cost 11**. Cultivation text: “Raise any die +2.” Its Battle text reduces opposing dice in its Strike Row by 2 and raises a die by the total reduced. **The full canonical wording is authoritative; do not rewrite its targeting rules from memory.**

**Design question:** If the designer changes the effect to say **“Raise one die +2. Choose another die. …”**, requiring distinct dice for the indicated actions, does that improve the card's competitive usefulness or make it too powerful? The ellipsis must be replaced by **the full proposed effect text**, including which phase, which die receives the reduction/raise, valid targets, and whether different dice are mandatory. No simulation begins with ambiguous variant semantics.

### Four linked tests, in order

**S0 — Verify rules and availability.** Lock one nine-card Grove with Sapping Snapdragon, and its exact costs, quantities, Round/Wisp context, graft conditions, and effect implementation. Confirm actual execution with deterministic tests: one die available, multiple dice, different/same targets, no legal second target, Battle Strike Row interactions, and no silent fallback to the original effect. Verify the full intended nine-card Grove rather than accidentally using `FirstGameDefault`.

**S1 — Find whether current Sapping has a viable use.** For the unchanged official effect, independently search both (a) unrestricted policies and (b) policies deliberately exploring Sapping-centered development. Encourage/require exploration **only during search**. Record whether acquired, grafted and activated; what other cards/dice were combined with it; and when it mattered. Evaluate the promising policies **without any bonus for acquisition** against a diverse pool, Human Baseline as one reference, and explicit counters. A negative result warrants checking model expressiveness and search coverage before labeling the card weak.

**S2 — Compare effect wordings.** Implement S1 (canonical) and S2 (precise proposed “another die” variant) as explicit, fingerprinted rules, keeping cost, printed VP, card identity and the other eight Grove slots identical. Run both **frozen-policy tests** (isolates immediate mechanical effect under matched decisions where possible) and **reoptimized-policy tests** (allows different strategies to emerge around the variant). Report win share, VP, Strike effects, actual use, and whether the variant changes the number of viable alternative strategies. Distinguish the effect's strength from merely convincing more policies to buy it.

**S3 — Countersearch and replication.** Train/find opponents that specifically challenge the strongest Sapping-centered approach. Repeat with independent seeds, learners, seat rotations, and at least a few Groves deliberately favorable and unfavorable to Sapping. If a +10-pp-or-larger practical advantage persists and competitors cannot counter it after adequate search, flag a strong *overpower* concern. If neither effect supports competitive use even in favorable settings, flag a possible *underpower* concern—while reporting the search limitations.

### Example of the result the designer should be able to read

> **Question:** Does requiring a *different die* make Sapping Snapdragon too strong?  
> **Assessment:** **Investigate** *(illustrative, not an actual result).*  
> **Matched two-player win-share change, S2 − S1:** +7 pp (95% interval +2 to +12).  
> **Competitive builds:** Two distinct Sapping-using builds discovered, plus two non-Sapping counters.  
> **Why not “Strong”:** The interval includes effects below the serious threshold, and one counter was only tested in one Grove.  
> **Next action:** Run a focused held-out counterstrategy comparison; do not change the official card yet.

The exact result is hypothetical. The actual system must report raw game counts and preserve enough detail to reproduce the figure.

## 5. Beyond Plants: Wisps and Round Cards

### Wisps — rewards from rolling 2s

The primary design question is **not** “Does a player choose to acquire Wisps?” Players receive Wisps through dice rolls and can decide when/how to use them. Test whether fortunate rolls produce disproportionate game-winning advantages.

- Record the **number of 2s relative to the number/type/timing of dice rolled**, Wisp gains actually awarded (respecting limits and Wispquake), Wisps spent, Wisp effects, and final outcome.
- Descriptive warning: a player with unusually many 2s winning **≥75%** of games is worth investigating, *but cannot by itself establish that Wisps are overpowered*. More dice, different die types, round timing, strategy, and already-strong boards confound the association. Compare with the appropriate 1/N baseline and to similarly developed players.
- Causal test: hold starting context/policies and dice RNG as comparable as the engine permits; compare official Wisp reward/use rules against a reduced or alternative Wisp treatment using matched games, tracking downstream state divergence. Evaluate both frozen strategies and adapted strategies. Measure outcome *effect size* and how much the random reward reduces the value of good choices.
- A problem exists when **Wisp benefit itself**, rather than simply having many dice, repeatedly creates an excessive or effectively uncounterable advantage.

### Round Cards — shared event effects

Test a specific Round effect against a precisely specified alternative or neutral reference with otherwise matched rules. Compare the way it shifts VP, strategy matchups, card usefulness, and seat/turn-order outcomes; allow policies to adapt when effect differences are substantial. An effect is not automatically problematic merely because it benefits an already-strong build or raises everyone's average score.

**Scope control:** Bees, Worms, and dice mechanics are not separate balance programs at this time; investigate them only insofar as they help explain a Plant/Wisp/Round effect, an abusive strategy, or the skill-versus-luck question.

## 6. Strategy-discovery and game-quality experiments

### C — Strategic diversity, one fixed Grove

Train/search a *population* of flexible strategies against varied opponents, including one another—not only against Human Baseline. Preserve **quality and diversity**: a strategy's recognizable profile should come from actual named-card copies, graft layout, dice collection, timing, and action behavior, not solely a manually assigned “Bee” or “dice” label. Identify duplicate approaches so five nearly identical learners do not count as five distinct strategies.

Evaluate finalists in a common held-out **matchup matrix** with rotated seats. Distinguish robust alternatives, matchup specialists, and strategies that are strictly outperformed by others. Determine whether a plausible mixture of strategies is stable against newly trained counterstrategies. No fixed number of viable strategies is guaranteed; **finding several** would be positive evidence, not an absolute certification of game quality.

### D — Degenerate or mandatory strategies

Intentionally search for the strongest versions of hypotheses such as **mostly-dice/no-Plants**, **mostly-Plants**, **cheapest-Roots-for-VP**, **Bee-loved Bloom / Bee accumulation**, **Compost-heavy**, and unrestricted hybrid development. Relax search-time constraints for fair held-out assessment where appropriate, while verifying that the alleged exploit was actually attempted. If an extreme build appears universally superior, **search for counters** before declaring it dominant.

### E — Skill versus luck

Compare independent, demonstrably stronger and weaker *decision policies* across identical available rules and a portfolio of opponents. Randomize seats and use paired seeds; measure how reliably strategy selection and tactical execution separately improve wins. Repeated victory of a stronger policy establishes meaningful skill influence even when luck allows frequent upsets. A near-50/50 matchup between **equally strong** self-playing policies says nothing by itself about the game's skill ceiling.

## 7. Architecture: question-driven experiments on shared machinery

Avoid five different simulator forks or an AlphaZero reimplementation by default. Use **one rules engine and a reusable question/experiment framework**:

1. **Question specification:** Exact card/effect question, hypothesis (too weak/strong), current and proposed wordings, Grove, player count, Round/Wisp configuration, search restrictions, comparison controls, and a declared meaningful-effect margin.
2. **Immutable scenario & rules manifest:** Fingerprint official CSVs, effect overrides, all nine specific Grove cards/quantities, runtime code, scoring rules, AI implementation, learner seeds, evaluation seeds, and policy weights. Reject incompatible or stale weights explicitly. Display the actual loaded Grove at startup.
3. **Policy adapters:** Human Baseline, existing frozen Buy experts, contextual/search-capable agents, targeted card explorers, and unconstrained population/self-play agents under common legal-action interfaces. The *policy must be expressive enough* to use situational cards and combinations; otherwise a negative discovery result is invalid as a card judgment.
4. **Search harness:** Reusable candidate policy search, opponent portfolio, diversity preservation, card-targeted exploration, seat rotation, and counterstrategy search. Prefer improving only the decision scopes necessary for the hypothesis, but document which other decisions remain Human Baseline.
5. **Controlled evaluation harness:** Frozen and adapted comparisons, paired starting conditions, independent held-out randomness, fair seat allocation, repeated evaluation, and data integrity/coverage checks.
6. **Shared telemetry:** D1–D10 Buy disagreement events; exact per-player named Plant acquisition, surviving grafts, Snips, dice by size, Doom/Trash, upgrades; relevant activations, Strike wins/VP, direct end-game VP and totals; per-game strategy profiles and outcome. Separate purchase choice from card-effect utility.
7. **Question-specific reporting:** One short verdict first, then evidence tables, effect sizes/intervals, strategy profiles, counterexamples, market-specific and general conclusions, and a reproducibility manifest.

A future question definition could look **conceptually** like this (not an implemented executable format):

```yaml
question_id: sapping-different-die-v1
kind: plant-effect-variant
card_title: Sapping Snapdragon
baseline: canonical-official-effect
candidate: exact-full-text-of-different-die-effect
player_counts: [2]
grove: [nine-explicit-plant-titles-and-definition-fingerprints]
research:
  searches: [unrestricted, card-focused, counterstrategy]
  evaluations: [frozen-policy, adapted-policy, held-out-matchups]
  primary_metric: paired-win-share-difference-pp
  attention_margin_pp: 5
  serious_margin_pp: 10
outputs: [assessment, strategy-profiles, matchups, card-development, raw-evidence]
```

A developer should be able to implement a new design question primarily by **writing a scenario specification and, if needed, one effect variation**—not by constructing a new experiment program each time.

## 8. Execution roadmap and stop/go gates

The first priority is a **minimal end-to-end card-effect study**, not a giant generic learner.

| Step | Work | Acceptance gate |
|---|---|---|
| **R0 — Correctness audit** | Confirm Buy aggregation/payment rules, legal grafting, current card wording, effect execution, actual nine-card Grove, RNG isolation and noninterference; verify source/weight fingerprints | Tiny deterministic tests and diagnostics ON/OFF games match wherever they should |
| **R1 — Question runner MVP** | Read a locked Sapping scenario, run current/variant effects, capture named development and outcomes, emit one assessment-format report | One Sapping A/B experiment reproducible end to end |
| **R2 — Targeted & unrestricted search** | Introduce enough contextual capacity and diversified opponents to discover coherent card-using and card-free approaches; preserve a small varied population | Demonstrate actual card use and at least two meaningfully different tested strategies, **or honestly document failure of search** |
| **R3 — Competitive verification** | Held-out matchup matrices, forced exploration versus unrestricted deployment, counters, matched frozen/adapted effect comparisons, uncertainty | An assessable Sapping verdict, with raw evidence and explicit limits |
| **R4 — Reusable questions** | Generalize specification/runner for other Plants; then Wisp and Round effects | A second named-card question runs through the same framework without bespoke script rewrites |
| **R5 — Wider landscape** | Extend to strategy diversity, degeneracy, multiplayer, skill/luck studies | Results answer Goals C–E without returning to fixed-Human-Baseline-only optimization |

**Avoid premature large runs:** R0 and R1 pilot first, verify that changed effects are truly being exercised, and inspect sample decisions and named-card build histories. Then calibrate search and evaluation budget based on observed information, not the urge to simulate a huge fixed number.

## 9. Research integrity and operations (requirements, not optional extras)

- **Names first:** Use card titles as primary labels and current Component Studio IDs only as trace keys. Join historical results only after matching effect-definition identity/fingerprints. Never silently combine old and current card meanings.
- **No fake precision:** A learner's zero purchases, a failure to find a strategy, and a causal negative effect are different findings. Report each literally. Separate exploratory findings from confirmatory held-out tests.
- **Record what was actually tested:** Source revision; canonical and variant CSV checksums; exact Grove and full wordings; policies and weights; seeds; opponents; counts of successful acquisitions/activations; evaluation design; and excluded/incomplete chunks. Incomplete studies remain explicitly incomplete.
- **Preserve existing assets:** Reuse the Kotlin rules engine, Human Baseline, D1–D10 telemetry, prior weights as seeds/opponents, and archived studies as context—not as definitive balance outcomes. Do not change approved policies or effects silently during a run.
- **Developer workflow:** Ubuntu Kotlin/Gradle repository. For code changes, deliver **changed-files ZIP** rooted at `leaf/`, precise tests run/not run, and a Git commit message. Keep source changes small and reproducible.
- **Long-running study operations:** Resumable chunks, clear current-stage and **whole-batch** progress, elapsed time, ETA **and projected clock finish time**, logs with `|& tee`, checksums, explicit failure conditions, and **one verified upload-ready `.tgz`** at completion. Never require the designer to manually gather dozens of output directories.
- **Handoff:** Keep this specification and a brief run-state manifest in `doc/` for new conversations. The governing goal is *answering design questions*, not endlessly escalating expert-versus-expert training.

## 10. The project's definition of success

The first successful milestone is **not** “a Buy expert wins 60% against Human Baseline.” It is:

> **Given a precise question about Sapping Snapdragon, the program can search for strong ways to use it, compare that card or effect variant against real alternatives, explore opponents and counters, and issue a well-supported assessment—No concern demonstrated, Investigate, or Strong balance concern—with exact named Plant/dice builds and reproducible evidence.**

After that, the same workflow should answer **“Is Bee-loved Bloom too strong?”**, **“Is Queen's Blossom desirable at its cost?”**, **“Does this Compost interaction actually pay off?”**, **“Are Wisp rewards from rolling 2s overpowering strategy?”**, and **“Does this Round effect distort the Grove?”** without reinventing the simulation research process.

**Principle:** *Discover viable strategic uses; test competitive consequences; actively seek counterexamples; report uncertainty; change the game only when evidence justifies it.*
