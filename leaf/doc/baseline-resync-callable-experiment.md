# Task 4 — Callable baseline resync experiment

- Compatibility Main remains `BaselineResyncCardBalanceMainKt` and delegates to `BaselineResyncCardBalanceExperiment.execute(args)`.
- `BaselineResyncCardBalanceOptions` owns the legacy `--key=value` argument defaults and validation.
- `BaselineResyncCardBalanceExperiment` loads shared catalogs once per invocation, resolves overrides, creates one game at a time and runs the legacy sequence.
- `BaselineResyncCardBalanceReporter` writes the original TSV rows directly to the supplied writer; it does not retain game references.
- The previously implemented reusable `experiment.orchestration` package is not wired in yet. That is deliberately reserved for Task 5 (multi-chunk single-JVM execution). Calling another `main()` is not required.
- No CLI flags, TSV columns, seeds, progress cadence or chunk orchestration have been changed.

## Verification

`./gradlew simulationTest --tests '*BaselineResyncCardBalanceOptionsTest'`

Then run the existing Task 1 `verify` benchmark and compare TSV hashes (or TSV bytes) against its captured baseline, with the same research inputs and Git revision recorded separately. Note that different Git revisions change the run identity in the Bash wrapper.

Gradle validation in the patch-building environment timed out before giving a result. Do not claim a successful build until the local tests pass.
