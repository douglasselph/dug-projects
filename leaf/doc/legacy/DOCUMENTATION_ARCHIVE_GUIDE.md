# Documentation organization — conservative archive pass

Use `python3 bin/organize_docs.py` to preview the moves. It scans the current repository and **will not move a document if any other text file refers to its filename**. It moves only milestone and task-note Markdown files (C04, 5B tasks, Sunlight migration B01–B08, and Phase C1–C5). It does **not** delete files, move executable scripts, alter canonical sources, or touch the A03 protected JSON.

## Commands

```bash
python3 bin/organize_docs.py
python3 bin/organize_docs.py --apply
git status --short
bash doc/SIMULATION_OPERATIONAL_READINESS.sh
```

If the resulting layout is undesirable, **before making unrelated edits**:

```bash
python3 bin/organize_docs.py --undo
```

The script records moves in `doc/legacy/archive/doc-moves-manifest.json`. Commit that manifest if you want a permanent audit of the archived locations. Git-tracked files are moved with `git mv`.

## Why the other documents stay

- Keep launch scripts, authoritative specs, research provenance, generated data and A03 protected files in their original paths unless their callers are updated together.
- Keep the current front-door/operational docs (`../README.md`, `../TASKS.md`, `LEAF_RESEARCH_GOALS_AND_EXPERIMENT_DESIGN.md`, `SIMULATION_OPERATIONAL_READINESS.md`, `SIMULATION_LAUNCH_ARCHITECTURE.md`, `BATTLE_PACING_RESEARCH_STATUS.md`, `USING_THE_SIMULATOR.md`) easy to find. `LEAF_RESEARCH_HANDOFF.md` is retained as historical provenance rather than the active handoff.
- `doc/README.md` is the maintained documentation map. When adding/moving current documentation, update it and verify its relative links. Historical files may remain in place for provenance even when they are no longer listed as front-door documents.
- Do not delete older rulebook PDFs or research notes automatically; they may be important for reproducibility.
- Passing a tiny experimental probe is not proof of statistical sensitivity or 36-card balance.

**Known tradeoff:** conservative retention means some historical notes remain at the top level when another file mentions them. That is intentional; moving all of them safely requires updating each caller and testing its relative links.
