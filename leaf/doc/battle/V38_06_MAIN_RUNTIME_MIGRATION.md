# V38-6 — Main-only Three Engagements runtime

**Patch status:** Authored November 5, 2026; **Ubuntu Kotlin acceptance pending**.
**Previous gate:** V38-5 targeted tests passed: 16/16, two successful Gradle runs (`leaf-v38-interactions-20261105-141414-2152905.tgz`).

## Problem and approach

The adopted `THREE_ENGAGEMENTS` default was previously implemented by classes
inside `src/simulation/` and supplied to `RoundCoordinator` at runtime through
`ServiceLoader`. Running only the main source set could not execute the default
and correctly threw an exception. Silent fallback to `LEGACY_WHOLE_BATTLE` was
never allowed.

V38-6 **relocates, rather than duplicates or rewrites**, six existing scheduler,
selector, timing and Human adapter source files into `src/main/` with their
original fully qualified class names. This avoids changing research imports,
experiment structures, output schemas or selected policies. The historical
`BattlePacingWispDeck` (filtered experimental Wisp deck) and
`BattlePacingRuleset` cohort labels remain in `src/simulation/` and are **not**
used by normal v38 gameplay. The main executor is
`dugsolutions.leaf.v35.game.round.battle.ThreeEngagementRoundExecutor`.

`GameModule` injects the main executor directly. `RoundCoordinator` also
constructs that executor if given a concrete `BattleRound` without an explicit
third executor, so independently constructed production coordinators can run.
It fails clearly for a fake/unsupported legacy Battle executor without an
explicit v38 executor. The old provider registration is removed; the provider
interface remains as deprecated compatibility surface.

No Plant, Wisp, Round, resource, scoring, Battle rule, or chooser algorithm was
intentionally modified. A/B research manifest SHA paths now point to the new
physical source locations so prior output fingerprints cannot silently omit
relevant scheduler source. No historical studies are reclassified as v38.

## Installation (from top-level `leaf` on Ubuntu)

```bash
unzip -o ~/Downloads/leaf-v38-main-runtime-changed-files.zip -d ..
bin/install_v38_main_runtime
bin/verify_v38_main_runtime
```

The relocation installer checks SHA-256 of all six *original* simulation
copies and the service-registration file **before removing any of them**. If
one has changed locally, the installer stops instead of deleting it. It is
idempotent after the first successful run. The existing source paths must be
removed to avoid duplicate Kotlin class declarations. Record the moved/deleted
paths in the Git commit (`git add -A` after reviewing `git status --short`).

The verification runner collects the main-only and simulation tests, Gradle
exit codes, JUnit XML, source hashes and logs in an upload-ready `~/Downloads`
archive and prints `UPLOAD THIS FILE:` on success or failure.

## Acceptance / constraints

- `src/test` source set can compile/run without `src/simulation` and finish a
  1-Cultivation / 1-Battle game using the **actual Three Engagements** engine;
  this must produce 3 `StrikeResolved` and 3 Doom events.
- Explicit `LEGACY_WHOLE_BATTLE` in the **same main-only classpath** finishes
  with three Strikes and one whole-Battle Doom event.
- Simulation runtime keeps using the main implementation and passes the
  v38 regression matrix (2/3/4 players, both structures), Wisp/Sunlight
  scheduler test cases and routing tests.
- The legacy pacing A/B code still compiles and has updated SHA provenance.
- No duplicate Kotlin FQCNs remain and no simulation-only service provider is
  required for main code.
- The runner returns PASS only if both Gradle commands exit 0 and every
  selected JUnit suite is present with zero failures/errors/skips.

**Known limitation:** Kotlin tests cannot be executed in the artifact-writing
environment because the project's Gradle distribution/dependencies are not
available offline. Treat the patch as **unverified** until the Ubuntu archive
is reviewed. If tests identify a defect, fix only the reproducible defect
before proceeding to V38-7 and the research question runner.
