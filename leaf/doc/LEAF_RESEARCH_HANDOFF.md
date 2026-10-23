# Leaf & Let Die — research and simulation handoff

**Purpose:** Portable context for a new ChatGPT conversation. Supply this Markdown file along with the latest `leaf(YYYYMMDD-HHMMSS).tgz` source archive and whatever experiment-result `.tar.gz`/`.tgz` archives the next task requires. **Source archive and canonical CSV files supersede this historical note** if they differ.

## Game and canonical data

- *Leaf & Let Die*, a card/dice deck-builder with Cultivation and Battle, 2–4 players. Dice D4–D20; one die pays for one purchased item, and overpayment is allowed. Resources (Bees/Worms/etc.) can modify purchasing and Battle choices under the game's actual rules.
- Official card CSVs are the source of truth: `data/Cultivation_Cards - Root.csv`, `data/Cultivation_Cards - VF.csv`, `data/Wisp_Cards.csv`, `data/Turn_Cards.csv`. The last holds the official `Round_Cards.csv` content under the simulator's legacy filename.
- The supplied official Flower IDs can have leading `z` for Tabletop Simulator; the loader normalizes to internal `Flower_...` IDs. **ID numbers are mutable/alphabetical. Card names are more stable.** Report `Card Name (current ID)`, and use a card-definition fingerprint to distinguish a renamed/rebalanced effect; don't join historical experimental data solely on numeric IDs.
- Active `data/research/resync/resync-current.csv` and `round-resync-current.csv` are expected to have header-only contents after the official October 2026 migration. Historical overrides are archived. **Verify canonical-vs-override setup before every run**.
- Official starting effects: Petal To Die 4 gain/steal + optional D4 cycling, Sapping Snapdragon S1; Petal P1–P4 and Sapping S2 are research variants, not defaults. O Edelweiss: twice choose Play another face-up Plant (then spend face down) or flip another Plant either direction, sequentially.

## Human Baseline and expert work so far

- Human Baseline state projection efforts 5B.1–5B.12A: arithmetic-mean unknown dice (D4 2.5, D6 3.5, D8 4.5, D10 5.5, D12 6.5, D20 10.5), deterministic hypothetical state, no mechanical RNG in scoring, Plant/Battle/resource evaluations, activation-target alignment, decision diagnostics, choose-then-execute tests, calibration and fixed-seed repeatability.
- Human Baseline does **not learn** from games; its behavior is Kotlin scoring code. Simulations produce measurements, not automatic policy changes. Expert runs learn candidate weights that require explicit evaluation and promotion; never silently replace approved weights.
- Official-card baseline runs: 4-player 2,400 games, 3-player 4,800, 2-player 4,800; corresponding full archive was `official-human-baseline-all-results.tgz` (contains historical experiments too—select the official matching manifest/fingerprint!). The current Buy-expert official results archive is `current-market-confirmation-results.tar.gz` from `bin/human_expert_resync_batch expert-buy` with five independent learners at each of 2p/3p/4p. Preserve resulting weights, seat/seed information, and original typed reports.
- Buy experts use learned purchase weights; other choices remain Human Baseline. A learned Buy player's 59.42% (2p), 53.21% (3p), or 39.64% (4p) held-out win share is a gain over an all-Human-Baseline control near 1/N, **not** a fully optimized whole-game expert. Expert losses are compatible with substantial dice luck, opponents and unoptimized non-Buy policy. Assess relative increase over 1/N, margins, and uncertainty; no guaranteed win is expected.
- Expert-vs-expert is important for strategic evaluation but purchase preference alone is not evidence a card causes wins. Use matched causal interventions (with/without or alternate effect), fixed-seed seat rotation, comparable expert strength, separate training/evaluation seeds, cross-policy replications, and report win-share and VP deltas with uncertainty. We should also measure per-strike wins/VP and actual end-game VP contributed where attribution is possible, recognizing indirect resource effects and opportunity costs.

## Key research principles

**“A card's attractiveness should be measured by what a strong player willingly gives up to obtain it; its actual strength should be measured by whether obtaining it improves competitive outcomes.”**

- A high `selected_purchases / legal_opportunities` rate can be trivial if a card was the only feasible buy. A low rate can be normal for an expensive card, especially with graft restrictions. Legal opportunities are repeated decision events, **not** independent trials/games.
- Better preference telemetry: number and identities of *simultaneously legal* competing options, choice over a higher-cost option, actual die(s) committed and overpayment, Critters spent to reach an otherwise unavailable price, alternative purchases forgone, round/VP situation.
- Each die pays for one item. Aggregating all die faces into one total can claim unaffordable options are affordable (e.g. a die showing 9 and another showing 12 sum to 21, but without suitable modifiers neither die individually pays for a cost-17 item). Each purchase uses its legal die/payment allocation and Buy order, not pooled faces. Don't assume a D20 spent on cost 5 was a meaningless waste without looking at additional legal alternatives and timing.
- Prefer transparent observed contextual metrics over arbitrary single-number “card valuation” formulas. Distinguish observed correlations, model preferences, and causality. Distinguish direct Strike VP, end-game printed VP, and indirect effect on future outcomes.
- Cards under particular scrutiny: Queen’s Blossom (intended marquee Flower, strong in human playtests), Vine and Punishment, Vine Yield, Vine and Dine, Forget-Me-Not, Transplant Tulip, Root Cause, Petal To Die 4, Sapping Snapdragon. Don't automatically nerf/buff based on baseline or one trained learner's purchases.
- The existing structured reports include `card-opportunity-summary.tsv`, `card-substitution-summary.tsv`, `card-opportunity-by-round.tsv`, `card-substitutions-by-learner.tsv`, `learner-win-shares.tsv`; use them before creating new telemetry. See `EXPERT_BUY_PREFERENCE_AUDIT.md` and `official-expert-buy-preference-audit.csv` for the most recent named-card analysis.

## User workflow/preferences — treat as requirements

- Ubuntu, Kotlin/Gradle `leaf/` repository. User is an experienced programmer. Prefer **incremental changed-files ZIPs** with paths relative to repository root plus a suggested Git commit message, not Git patches. Avoid changes while a long-running study is in flight. Never claim Kotlin tests passed unless actually run; user can run full `./gradlew test integrationTest simulationTest` and return errors.
- Long-running scripts: **visible progress, per-stage counts, 60-second heartbeat when appropriate, elapsed, projected ETA and expected finish**. Use shared `bin/lib/experiment_run_timing.sh` where applicable.
- Preferred invocation is **`command |& tee ~/Downloads/descriptive-name.log`**, not detached `nohup`. Ensure proper pipeline failure status and resume behavior.
- Every completed long run should **create and verify one upload-ready `.tgz`/`.tar.gz` automatically**, print `UPLOAD THIS FILE: /absolute/path` prominently, and retain working results directory for resuming/debugging. Include manifest (Git commit, clean/dirty status, source/card/round/weight fingerprints), seeds, settings, complete study and results summaries in archive. Keep official and historical cohorts separate by fingerprints.
- Reporting: **use card names as primary labels**, current ID parenthetically. Provide comprehensive, inspectable CSV and Markdown outputs. Do not lose source identity if card ID changes.
- Source archive (`leaf(....).tgz`) typically uploaded when requesting coding; avoid requesting already available archives.

## Near-term roadmap

1. Preserve and interpret completed official Human Baseline cohorts (12,000 games), plus official Buy-expert held-out results (15 learners).
2. Improve named-card Buy preference reporting from existing opportunity/substitution telemetry (completed in accompanying reports). Then selectively add **joint legal choice-set and actual payment resource** telemetry; do not rerun large training solely for an arbitrary new score.
3. Train/evaluate other expert decision families, measure incremental performance contributions (Buy alone versus successive policy upgrades), then seat-balanced expert-vs-expert evaluation.
4. Controlled causal per-card/effect experiments with confidence intervals and state-aware Strike/VP attributions; specifically Petal P1–P4 and Sapping S1–S2 after synchronized reference policy.
5. Only alter official card effects when repeated balanced evidence and human playtesting justify it; preserve archives and version tags for comparisons.

## First instructions to a fresh assistant

Inspect the attached latest source/archive and `doc`/`bin` runner configuration before recommending commands or patches. Treat the named-card comparison as descriptive, not causal. Confirm any run's official data and fingerprints. Answer the immediate concrete request, provide only modified files where code edits are requested, include test steps and warnings when unverified, and favor meaningful research questions over unnecessary scoring rewrites.
