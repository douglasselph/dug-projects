# Current-card research readiness (NC-25–27)

**November 13, 2026 checkpoint** — a passing mechanical gate is necessary, but it is not a card-balance assessment.

## Evidence received

The uploaded `leaf-current-card-mechanics-20261113-131420.tgz` reports `overall_status=PASS`, official catalog fingerprint `1c3ae395d2c24db6db70954b7d81b15b1f4ee6afcec8eb31c0e5a3b12c808ea9`, and the following JUnit totals:

| Suite | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: |
| `test` | 1,807 | 0 | 0 |
| `integrationTest` | 88 | 0 | 0 |
| `simulationTest` | 253 | 0 | 1 |

The skipped test is `BaselineLongRunMemoryTest.three thousand successive games do not demonstrate unbounded retained heap`. The 2/3/4-player × two-Battle-structure test suite is present and passed in this run. The archive recorded Git HEAD `c726183563828c8a580d8dd4e2f9372e8a21965f` and `git_dirty=yes-or-unavailable`. This proves the listed suites ran, **not** exact source equality with every uncommitted later change.

## Next gate: two different questions using current rules

Run from within `leaf/` on Ubuntu after installing this increment. **Use a mechanical archive from the current checkout**; rerun the mechanical gate if HEAD or official CSVs changed.

```bash
# Existing mechanical acceptance (only necessary if current files differ)
bash bin/verify_current_card_mechanics

# Fast, read-only preflight. Replace the filename with the actual latest PASS file.
python3 bin/verify_current_card_research_readiness.py \
  --plan-only \
  --mechanical-archive ~/Downloads/leaf-current-card-mechanics-20261113-131420.tgz

# Execute one v38 two-question pilot. The underlying launcher compiles ONCE,
# then uses isolated direct-Java processes and its existing resumable stages.
python3 bin/verify_current_card_research_readiness.py \
  --run \
  --mechanical-archive ~/Downloads/leaf-current-card-mechanics-20261113-131420.tgz
```

The new wrapper uses `bin/verify_v38_question_runner` (which already owns the compile, process isolation and checkpoint handling). It does **not** introduce another Gradle build, a second trainer, or a distinct policy system. The existing `plant_question_evidence.py` validates each finished pilot. It checks paired official-versus-suppressed observations, untreated matchups, zero measurable output from suppressed effects, seat rotation, plan and ruleset fingerprints, and the adapted/countersearch evidence. The extra NC-27 gate also requires actual purchase and resolution of both official effects in the primary held-out cohort.

The last printed `UPLOAD THIS FILE:` contains the mechanical source archive, the v38 question-pilot archive, `evidence.json`, `ASSESSMENT.md`, a status manifest and logs. **A failed or incomplete pilot still produces a diagnostic archive**; it is not silently excluded. A successful functional report remains `Investigate — insufficient evidence` for both cards.

If a finished pilot archive already exists for **this exact current ruleset**, its evidence can be checked without rerunning games:

```bash
python3 bin/verify_current_card_research_readiness.py \
  --pilot-archive ~/Downloads/leaf-v38-question-runner-verification-EXACTDATE.tgz \
  --mechanical-archive ~/Downloads/leaf-current-card-mechanics-20261113-131420.tgz
```

The November 5 pilot is a **historical cohort**. If its ruleset fingerprint differs from current code or CSVs, the verifier rejects it rather than relabeling it as November 13 evidence.

## What does not pass yet

- This is a **two-card, two-player, one-Grove engineering smoke test**, not a 36-Plant balance audit. The fixed budget is too small for meaningful effect-size confidence intervals.
- Only the **Buy** decision family is adapted. Human Baseline activation/targeting and other companion policies may fail to use an effect optimally.
- Suppression is tested for a direct Raise and a multi-target opponent effect; it is not complete causal/output coverage of all Plant, Wisp and Round mechanics.
- **NC-25** still needs a reviewable deterministic coverage matrix for all 36 Plants and revised Wisps/Rounds, including boundary states, timing chains and actual state changes.
- **NC-24** still needs broader learned-weight/policy-feature compatibility and valuation audits.
- **A2 positive-control sensitivity** still requires independently verified, *mechanically exercised* exaggerated effects and countersearch under current v38 rules. A large `+20 VP` signal alone is not proof of sensitivity to non-VP exploits.
- Designer approvals in `doc/rules/CARD_EFFECT_INTERPRETATIONS.md` remain provisional for conditional upgrades, Saplink repeated Raises, Wisp trash selection and immediate-use dice.

**Next after this gate**: inspect the new pilot evidence, add specific tests for under-covered mechanics and a current-rules non-VP positive-control calibration; only then consider a pilot 36-Plant screen. Keep effect utility distinct from purchase-price utility throughout.
