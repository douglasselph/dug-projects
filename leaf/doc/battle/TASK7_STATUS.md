# Task 7 continuation status

This patch introduces an **optional** `--task7-counterfactual true` experiment that replays the same legal opening independently and rotates each of Candidate B's Engagement starters by one clockwise seat. It adds `task7_initial_rows.csv` (opening row strength, ready Plants, Support inventory, seat) and `task7_counterfactual.csv` (per-player B-reference vs B-rotation outcomes). The preexisting `task7_row_actors.csv` remains unchanged.

The new Task 7 report script resamples whole paired Battles for confidence intervals, avoiding the false independence of players from the same game.

**Not validated:** Kotlin compilation and full paired experiments have not completed in this environment. **Not established:** a properly conditioned causal excess last-actor advantage comparing A versus B, controlled A ordering, and High-row Wound cascade attribution. The new order-rotation check is a sensitivity test, not an estimate of the causal effect of being last.

Run after installing in project root:

```
python3 bin/a03_verify_boundaries.py
./gradlew compileSimulationKotlin
./gradlew simulationTest --tests '*BattlePacingABBatchTest' --tests '*ThreeEngagementBattleTest'
bash doc/battle/run_battle_pacing_ab.sh --samples 10 --players 2,3,4 --battles 1,2,3 --policy human-guided --scenario natural --task7-counterfactual true --out output/experiments/battle-pacing/task7-pilot
python3 doc/tools/battle/battle_pacing_task7_report.py output/experiments/battle-pacing/task7-pilot
```

Do not treat this as a completed, validated Task 7 acceptance package.
