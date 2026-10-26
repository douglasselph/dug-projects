package dugsolutions.leaf.v35.player.decision.baseline.battle

/**
 * Single provisional Battle decision-point policy shared by ordinary Main candidates.
 * This is NOT fitted to human preferences. Calibrate coefficients against held-out
 * mixed-action judgments before certifying C04; never substitute Cultivation pips.
 * Zero-benefit legal options still have origin; impossible acquisitions score zero.
 */
internal object BattleUtilityPolicy {
    const val ORIGIN = 35
    const val IMMEDIATE_NON_STRIKE_VP = 3
    const val WISP_OPTION = 5
    const val OPPONENT_WISP_DENIAL = 2
    const val FUTURE_DIE_ACCESS_ONE_BATTLE = 0.5
    const val FUTURE_DIE_ACCESS_MULTIPLE_BATTLES = 0.75
}
