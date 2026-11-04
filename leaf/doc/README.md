# Documentation guide

The repository [README](../README.md) explains the purpose and working agreement; [TASKS](../TASKS.md) is the current backlog. **Those two are the front door.** File names from old checkpoints are provenance, not a new to-do list.

## Governing research and operating references

- [Research goals and experiment design](LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md) — evidence standards, effect utility versus purchase pricing, strategic diversity
- [Operational readiness](engineering/SIMULATION_OPERATIONAL_READINESS.md) — verified gates versus unvalidated balance conclusions
- [Launch architecture](engineering/SIMULATION_LAUNCH_ARCHITECTURE.md) — direct Java, persistent JVMs, checkpoints and long-run contracts
- [How to use the simulator](engineering/USING_THE_SIMULATOR.md) and [architecture](engineering/CODE_ARCHITECTURE.md)
- [Current tabletop rulebook v38-01](rules/Leaf_and_Let_Die_v38-01.pdf) — adopted tabletop rules; [v37-03](legacy/Leaf_and_Let_Die_v37-03.pdf) is retained for historical comparison; research overrides are distinct

## By subject

| Directory | What belongs there |
| --- | --- |
| [battle/](battle/) | Adopted v38 Battle status, ruleset configuration, historical Candidate-B pacing/fairness analysis, implementation checkpoints and reports |
| [policies/human-baseline/](policies/human-baseline/) | Human Baseline policy, C04 calibration and decision verification |
| [policies/learning/](policies/learning/) | Trained/frozen strategy research and policy-family investigations |
| [research/experiments/](research/experiments/) | Card-effect experiment methods, Sapping work, sanity controls and R-series implementation records |
| [research/studies/](research/studies/) | Market, Round, token, Compost and card-specific investigations |
| [research/telemetry/](research/telemetry/) | Development, Buy disagreement, effect coverage and reporting artifacts |
| [research/provenance/](research/provenance/) | Protected research boundaries, source-of-truth and historical rule/data decisions |
| [engineering/](engineering/) | Launcher architecture, simulation tooling, test and execution instructions |
| [tools/](tools/) | Executable Python helpers and report generators (not narrative documentation) |
| [rules/](rules/) | Current rulebook |
| [legacy/](legacy/) | Superseded handoffs, old rulebook, archived implementation notes and backups |

## Active pointers

- [Current v38 Battle implementation/verification status](battle/V38_01_CURRENT_STATUS.md), [Battle rules configuration](battle/BATTLE_STRUCTURE_CONFIGURATION.md), and [historical v37 pacing A/B status](battle/BATTLE_PACING_RESEARCH_STATUS.md)
- [Human Baseline](policies/human-baseline/HUMAN_BASELINE.md) and [Battle baseline](policies/human-baseline/HUMAN_BASELINE_BATTLE.md)
- [Effect sanity gate](research/experiments/card-balance/EFFECT_SANITY_GATE.md) and [effect-aware decisions](research/experiments/card-balance/EFFECT_AWARE_DECISIONS.md)
- [Protected baseline](research/provenance/protected-baseline/PROTECTED_BASELINE.json) — deliberately remains at its established path because verification tools consume it

Old implementation IDs within file names are retained to preserve provenance. **The subject folder, not the ID, defines the navigation hierarchy.** New documents should use descriptive names without milestone prefixes.

Executable shell acceptance/verification scripts with existing callers remain at their established paths until their invocation contracts are separately migrated. Python reporting utilities are organized in `tools/` and callers have been updated.


## Additional subject guides

- [Compost experiment records](research/studies/compost/README.md)
- [Cursor IDE setup and troubleshooting](engineering/cursor/README.md)
- [Historical player-count data reorganization](legacy/PLAYER_COUNT_DATA_REORGANIZATION.txt) (archived; do not execute its delete-and-replace instructions)
