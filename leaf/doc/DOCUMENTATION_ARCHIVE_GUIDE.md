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

The script records moves in `doc/archive/doc-moves-manifest.json`. Commit that manifest if you want a permanent audit of the archived locations. Git-tracked files are moved with `git mv`.

## Why the other documents stay

- Keep launch scripts, authoritative specs, research provenance, generated data and A03 protected files in their original paths unless their callers are updated together.
- Keep the current operational docs (`SIMULATION_OPERATIONAL_READINESS.md`, `SIMULATION_LAUNCH_ARCHITECTURE.md`, `R3B_EFFECT_SANITY_GATE.md`, `USING_THE_SIMULATOR.md`, `LEAF_RESEARCH_HANDOFF.md`) easy to find.
- `doc/README.md` in the source snapshot has pointers to names that may not currently exist. Do not assume it is a verified directory map. Review its links separately before rewriting.
- Do not delete older rulebook PDFs or research notes automatically; they may be important for reproducibility.
- Passing a tiny experimental probe is not proof of statistical sensitivity or 36-card balance.

**Known tradeoff:** conservative retention means some historical notes remain at the top level when another file mentions them. That is intentional; moving all of them safely requires updating each caller and testing its relative links.
