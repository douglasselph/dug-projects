# C04 — Real downstream collateral target parity

The HumanBaselineEffectStrategy.chooseDie production path for Bloom Backflip and Gust of Petals now consumes BattleCollateralActivationPriority.targetProjections, the same complete per-target projections used to value Plant activation.

The regression exercises real chooseDie rather than comparing isolated analyzers. It validates both eligible own-die candidates and verifies the chosen index has maximal projected tactical utility (after the same integer rounding used by preference scoring). The Gust projection still treats the subsequent row decision as future information.

Scope: this does not imply that every compound effect is covered, or that the universal Battle utility coefficients are calibrated. No mechanics or learned policies changed.

Verification: ./gradlew test --tests '*C04CollateralEndToEndTargetChoiceTest' --tests '*C04CollateralTargetProjectionParityTest' && python3 bin/a03_verify_boundaries.py
