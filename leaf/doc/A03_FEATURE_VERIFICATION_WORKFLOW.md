# A03 verification in every feature delivery

Run `python3 bin/a03_verify_boundaries.py` after installing each changes-only ZIP.

- **PASS (zero changes):** no manifest edit needed.
- **FAIL:** inspect every protected change; do not assume it belongs to the most recent feature. Compare source with the accepted version and confirm the change is intentional.
- For intentional protected changes, review and accept each exact file (no wildcards) using the helper in `doc/`:

  ```bash
  python3 doc/a03_accept_reviewed.py \
    --approve-path bin/check_sunlight_rule_parity.py \
    --reason 'B08 reviewed Sunlight parity audit checker' \
    --accept
  python3 bin/a03_verify_boundaries.py
  git add doc/A03_PROTECTED_BASELINE.json
  ```

The helper first refuses any unapproved changes, calls A03's own `--propose-baseline`, verifies the proposal changes only approved entries, keeps a backup and installs the proposal atomically. It reruns the unqualified A03 checker; on verification failure, it restores the previous manifest. Omit `--accept` for a dry-run preview. Do **not** approve unexpected data, mechanics, policy, or tool changes merely to get a green test.

The feature verification record should include the plain A03 outcome, the exact approved path(s) and rationale where needed, test commands and actual results. This process guards research scope, not gameplay correctness.
