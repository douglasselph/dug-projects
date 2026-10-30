# Task 5B.10 — Buy and Main Action opportunity-cost review

## Implemented

- Added `PlantAcquisitionUsability` as a **Buy-only** bounded adjustment. A newly bought Plant is face down; its phase-specific PLAY base cannot simply be treated as instantly earned. The adjustment uses legal graft sites and known remaining Cultivation/Battle rounds, with capped phase-specific opportunity weights. Unknown schedule is neutral; zero remaining rounds is a -12 future-activation penalty; VP remains independently valued by the existing scoring rule.
- `PurchasePriority` integrates that adjustment without changing die Buy valuation, payment planning, diversity, or end-game VP policy. No PLAY scorer invokes the Buy-only adjustment.
- Corrected immediate upgraded-roll arithmetic in Cultivation Round's keep-upgrade scorer: `(sides+1)/2.0`, rounded only at final priority scoring; no RNG.
- Regression tests for unknown schedules, zero remaining opportunities, and bounded monotonic opportunity estimates.

## Opportunity-cost review: what is and is not comparable

1. **Cultivation Main**: `HumanBaselineCultivationStrategy.score` compares `DrawPriority.score`, `PlantActivationPriority.score`, and phase Round effect priorities as `PriorityScore`. All consume a Main Action, but the numerical bases are calibrated behavioral preferences, not a common utility unit. Dedicated projections improve ordering of choices but do not guarantee that a 100-point Plant equals a 100-point Draw in final VP.
2. **Draw**: fixed base 35 plus four times the mean of the *smallest-sided currently accessible die*, no simulated rolling; currently no purchase-threshold or future action conditional benefit in its priority. This is a material calibration candidate, but changing it alone would alter established policy across all card effects.
3. **Round effects**: Water, Sunlight, Mulch, Compost have tailored scorers; unknown effects use a fixed 45. The keep-upgrade scorer previously truncated its expected die face; corrected. Remaining generic 45 is an audit item whenever a newly introduced Round effect is used.
4. **Plant PLAY**: specialized projections plus Plant refresh/preservation opportunity adjustments; still contains card-specific fixed bases and occasional projected bonuses. Main Action opportunity cost is *implicit* in the comparison; subtracting an extra Draw score from only Plants would double-charge their action cost.
5. **Plant BUY**: `PurchasePriority` starts from `item.cost*10`, adds small card acquire value, VP projection, diversity and slot policies. Newly added usability adjustment does not equate acquisition with immediate activation. Its small cap deliberately preserves the existing cost-tier-first policy.
6. **Buy planner**: `HumanBaselineBuyPlanner` uses category choice, payment-group enumeration, overpay preference, and up-to-two-item lookahead. Acquisition utility is not a complete expected-net-VP optimization. A future task should check purchase selection across different combinations, not simply increase card valuation coefficients.

## Further recommendations before freezing baseline

- Measure Plant-activation vs Draw vs Round-effect choice shares and actual follow-through in seeded scenario diagnostics, and calibrate against observable expert behavior, not solely priority magnitudes.
- Compute Draw's actual buy-tier and available Battle-grid consequences using existing `ExpectedDrawProjection`, while retaining a cap and not charging other Main Actions twice.
- Audit conditional card effects for **legal usable activation windows**, not just rounds remaining. Card placement topology and round count are known; exact future flip/refresh, target availability, and chosen action slots are not.
- Keep projected VP purchase value distinct from activation opportunity; end-game scoring does not require activating the Plant.
- Review integer-valued cost purchasing with *one die per item*, rather than equating total die values with a single pooled payment.

## Validation

Targeted local Gradle invocation timed out after the build started. Compile and tests are NOT verified. Gameplay rules, CSV research configurations, and mechanical RNG untouched.
