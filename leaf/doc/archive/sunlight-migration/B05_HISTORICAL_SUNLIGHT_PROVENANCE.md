# B05 — Historical Sunlight experiment provenance and runtime safety

## Finding

The source snapshot contains two specifically named legacy Sunlight experiments:

* `bin/legacy/experiment_sunlight_token_sanity`: **incompatible for new runs**. Its A arm uses current canonical Round effects while describing them as Raise +3, and its B arm explicitly overrides Sunlight to `GAIN_SUNLIGHT_TOKEN`. Under current rules those arms are not a valid Raise-versus-token treatment comparison. Its results cannot be reproduced by rerunning today's engine without restoring historical source/data in an isolated environment.
* `bin/legacy/experiment_sunlight_cultivation_learning`: explicitly studies `GAIN_SUNLIGHT_TOKEN` using `data/research/round-overrides/sunlight-token.csv`, with the 3/2/2 default. There is no obsolete Raise +3 assumption in its execution configuration; **left unchanged**.

The official `data/Turn_Cards.csv` and current research resync use Sunlight token gain. Do not reintroduce Raise +3 as a gameplay option or silently reinterpret obsolete CSV text.

## Provenance snapshot

Original byte-exact launcher saved as `doc/historical/B05_experiment_sunlight_token_sanity_original.sh`.

Original SHA-256: `734c2290eec9c4cc4e453fff1a28b10a637c1578932ce2bdf22b7961c2578b0a`.

This preserved file is **not runnable under the current engine for faithful historical reproduction**. To reproduce old results, reconstruct an isolated historical checkout containing the older rules/code, card data, frozen policy, mechanical/strategy/Grove seeds and the archived experiment's own manifests. None of those historical full-run artifacts are included in this uploaded source archive (no `output/experiments/` tree). Preserve past logs and archives unchanged; label their old use of “canonical” as historically scoped.

## Live behavior

The original launcher at `bin/legacy/experiment_sunlight_token_sanity` now fails before creating output or launching Gradle/Java, including with `--help`; its error directs users to the preserved byte-exact source. This is an intentional B05 protected launcher change under A03.

Other `bin/legacy` scripts, canonical mechanics, current Round CSV, research overrides, trained/frozen weights and current research launchers are unchanged.

## Verification

- Compare `sha256sum doc/historical/B05_experiment_sunlight_token_sanity_original.sh` with `734c2290eec9c4cc4e453fff1a28b10a637c1578932ce2bdf22b7961c2578b0a`.
- `bash bin/legacy/experiment_sunlight_token_sanity` must exit 2 with an explicit retirement explanation and create no output.
- `python3 bin/a03_verify_boundaries.py --approve-path bin/legacy/experiment_sunlight_token_sanity --reason 'B05 retire misleading obsolete Sunlight comparison and preserve original snapshot'`.
- After review, the normal A03 check requires a separately approved baseline refresh, not silent rebasing.

## Limitations

The archive cannot establish provenance of results stored only on the developer's Ubuntu machine. Those original archived runs should be retained separately and not overwritten. No historical simulations are rerun here.
