# Documentation consolidation notes - November 2, 2026

This file records how the external handoff/planning Sources used for the November 2026 consolidation were incorporated. It exists so those external prompt/handoff files can be moved to legacy storage without making them compete with current repository documentation.

## Active source of truth after consolidation

A new conversation should use:

1. repository `README.md`;
2. top-level `TASKS.md`;
3. `doc/LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md`;
4. `doc/SIMULATION_LAUNCH_ARCHITECTURE.md` for run architecture;
5. `doc/SIMULATION_OPERATIONAL_READINESS.md` for operational claims;
6. relevant task-specific current-status docs, especially `doc/BATTLE_PACING_RESEARCH_STATUS.md` for Candidate B.

The repository is now intended to be the normal handoff rather than a separate giant prompt document.

## External Source mapping

### `Codex-v5.3.docx`

Unique durable ideas incorporated into active docs include:

- the simulator is an analysis/research harness, not a player UI;
- the larger success condition is **strategic irreducibility**, not merely beating Human Baseline;
- strong strategy is distinct from a simplified exploit;
- expert-vs-expert/counterplay is required before serious dominance claims;
- healthy choices do not require equal use frequencies;
- simplification is desirable when complexity adds little value;
- current research uses the legal 36-Plant / 4-per-slot / nine-slot market structure;
- Human Baseline is a control rather than strategic truth;
- current developer workflow favors changed-files ZIPs, exact verification claims and preserved research provenance.

Detailed historical empirical findings from that era remain represented by their experiment/policy documents in `doc/` and Git/source history; they are not repeated in the front-door README.

### `LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md`

Promoted into the repository as the governing research specification and updated to include:

- strategic irreducibility;
- effect utility versus purchase/pricing utility;
- all-card pricing sanity as a first-class goal;
- current R0-R5 interpretation;
- effect-awareness limitations and research controls;
- Battle pacing as a parallel structural research stream.

### `Leaf_Master_Calibration_Resume_2026-10-30.md`

Its current-status guidance was incorporated into `README.md` and `TASKS.md`:

- operational readiness is achieved;
- broad C04 fine calibration is deferred rather than a blocker;
- C01-C05 infrastructure exists but does not imply all later calibration work must be executed now;
- the important ultimate Plant goal is a 36-card dead/dominant-effect audit with credible counterplay and effect-vs-purchase separation;
- canonical invariants such as 3/2/2, Sunlight token behavior and protected research boundaries must remain intact.

The old master list remains useful historically, but its phase numbering is no longer the active project task list. `TASKS.md` is the active backlog.

### `Status.docx`

Current durable status incorporated:

- October 30 operational gate passed in the recorded project history;
- Human Baseline Battle is operationally usable;
- C04 detailed calibration is deferred;
- R3B.1 mechanically isolates and measures research effects;
- R3B.1a closes the simple control-specific effect-awareness defect, but arbitrary strategic exploitation remains a broader limitation;
- immediate priority is reliable experimental results rather than general-purpose AI escalation;
- code changes should preserve canonical rules/A03 boundaries and be delivered as reproducible changed-files ZIPs with commit messages.

### `Battle Pacing & Balance Experiment.docx`

The experiment rationale, Candidate A/B distinction, Three-Engagement rule shape, fairness concerns, timing-model cautions and evidence standards are consolidated in:

- `doc/BATTLE_PACING_RESEARCH_STATUS.md`;
- `doc/BATTLE_STRUCTURE_CONFIGURATION.md`;
- the existing detailed Battle pacing checkpoint/task records.

The original brief predates several implemented checkpoints and should no longer be used as the current status document.

### `Launch arch prompt.md`

Its requirements were folded into `README.md` and `doc/SIMULATION_LAUNCH_ARCHITECTURE.md`:

- inspect current source before editing;
- preserve direct-Java/persistent-JVM architecture and required process isolation;
- do not reintroduce repeated Gradle launches casually;
- finished long-run upload archives belong in `~/Downloads`;
- changed-files ZIP install commands assume `~/Downloads`.

### `Leaf_and_Let_Die_v37-03.pdf`

This is **not merely a historical handoff**. A compressed, visually verified repository copy is now stored as:

`doc/Leaf_and_Let_Die_v37-03.pdf`

It remains the authoritative tabletop rules reference until a newer approved rulebook supersedes it. The existing v36 PDF is historical.

### `leaf.tgz`

This is the source snapshot used for the consolidation itself, not a document to merge. The patch produced from this work is intended to be applied to the user's live repository and then committed there.

## What can move to external legacy storage

After applying/committing the consolidation patch, the stand-alone prompt/handoff/planning Sources above can be moved to legacy because their current durable instructions are represented by the repository.

The v37-03 rulebook may also be removed from external Project Sources **only after confirming the repository copy is present in the live checkout**. Keep the newest source archive/result archives needed for whatever current coding or analysis task is underway.

