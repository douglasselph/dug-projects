# C05 — phase-scoped, explicit preference units (increment)

The authoritative Cultivation direct unit is expected incremental spending pips;
Battle uses Strike-row tactical utility. These raw scales are **not** comparable,
and neither raw scale can be compared against legacy integer `PriorityScore` values.

`PhasePreference` and `ActionValue.comparePreference()` now reject unconverted
cross-phase or cross-unit comparisons. `ActionValue` defaults to explicit
`LEGACY_PRIORITY_POINTS` to preserve existing C01 call sites; new direct-unit
valuations must opt in to their actual unit.

`LegacyPreferenceBridge` makes two existing translations explicit:
- Cultivation Draw expected pips: old `35 + round(4 * expectedPips)` priority.
- Battle Draw projected tactical swing: old rounded tactical *adjustment*, added
  to its existing intrinsic priority. This is not pip conversion.

Both paths retain their existing priority ranking for this increment. Other
Plant, Round, resource, Battle targeting and Support paths still return
`PriorityScore`, so the global action-selector does **not** yet enforce phase-unit
homogeneity automatically. The remaining calibration work must move all
competing choices to phase-appropriate direct utility (or one intentional
conversion boundary) before C05 may be considered fully complete.

No game rules, CSV data, learned policies or weights change.
