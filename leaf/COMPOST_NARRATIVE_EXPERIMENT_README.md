# Compost gameplay narrative experiment

## Research question
When a Human Baseline player naturally chooses Compost, what happens in the actual game, and what is the observed seeded continuation if that one action is replaced with Draw or the other legal Round action? Compare original (upgraded die to Discard) and enhanced (roll upgraded die into hand without Roll Rewards) in separate arms. The baseline policies are unchanged, except for the one fork in the alternate game. This is NOT trained-expert analysis.

## Install and smoke test
Unzip patch at repository root. Compile with `./gradlew compileKotlin compileSimulationKotlin`.
`GAMES=5 CHRONICLES=4 OUTPUT_ROOT=output/experiments/compost-narrative-smoke bin/experiment_compost_narrative`
Full run: `GAMES=500 CHRONICLES=30 bin/experiment_compost_narrative`.
Long-run background: `nohup env GAMES=500 CHRONICLES=30 bin/experiment_compost_narrative > compost-narrative.log 2>&1 &`.
Delay supported via `DELAY_SECONDS=...`.

## Output
`summary.txt`: basic per-arm aggregation of single-action alternative-minus-Compost VP.
`original/pairs.tsv`, `enhanced-no-rewards/pairs.tsv`: row per eligible counterfactual, all numeric differences are alternative minus original Compost.
`original/cases/.../`, `enhanced-no-rewards/cases/.../`: full Chronicle for both actual and alternate histories, action decision traces, endgame summary, and detailed score deltas. The cases are FIRST eligible cases, not cherry-picked winners; the larger TSV enables subsequent representative-case selection.
`compost-narrative-results.tar.gz`: upload this archive for narrative investigation.

## Study design and limitations
- Four players, Human Baseline Cultivation Main, Buy, Battle and other decisions; standard 3/2/2 rounds, first-game Grove cards. This intentionally starts with human-like policy; this is NOT evidence about trained-learned expert choices.
- Per game, selects the FIRST naturally occurring Compost decision (any player). The branch changes exactly that selected choice. Draw and the other Round effect are tried only if actually legal.
- Main actions of the rest of the game and opponents are recalculated under original strategies. The paired run starts from the same mechanical and policy random seeds.
- Changing the action can change how many random numbers are consumed, so future randomness might become desynchronized. A single paired outcome is a genuine possible continuation, not a deterministic 'all else equal' causal proof. Patterns across many seeds carry more weight.
- Pairs are correlated if multiple alternatives were tried for one baseline game; do not treat those rows as independent samples.
- Different arms share seeds, but are not guaranteed to have comparable decision states; do not interpret direct across-arm raw win statistics as causal.
- No interactive game snapshot/rewind is stored. This is deterministic whole-game re-execution with one fork.
- This implementation saves full Chronicles for the FIRST N pairs per arm, not the entire 500-game population (avoids huge outputs); the TSV has all evaluated forks.
- To explain *expert avoidance*, a later extension should plug in saved learned Main/Buy weights and replay those policies. The current human-baseline experiment explains cases where Compost is actually selected.
