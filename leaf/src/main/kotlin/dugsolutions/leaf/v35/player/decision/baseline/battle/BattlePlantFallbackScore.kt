package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore

/**
 * Transitional C04 boundary for Plant effects without a complete tactical projection.
 * Remove the card-specific legacy base while retaining observable contextual adjustments.
 * These adjustments are NOT certified tactical utilities; future migration must replace
 * their underlying formulas rather than treating this compatibility bridge as calibration.
 */
internal object BattlePlantFallbackScore {
    private const val ORIGIN = BattleUtilityPolicy.ORIGIN

    fun normalize(intrinsic: PriorityScore): PriorityScore {
        // Research no-ops and other deliberately disqualified actions must never
        // become attractive merely because their obsolete base was normalized.
        if (intrinsic.base <= -1_000) return intrinsic
        return PriorityScore(ORIGIN, intrinsic.adjustments)
    }
}
