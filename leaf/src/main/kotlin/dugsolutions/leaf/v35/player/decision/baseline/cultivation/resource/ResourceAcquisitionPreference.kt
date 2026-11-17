package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.ResourceGoal
import dugsolutions.leaf.v35.player.decision.baseline.ResourceInclination
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore

/**
 * A small, explainable Human Baseline preference applied *after* the existing
 * contextual valuation of a legal Round resource action.
 *
 * This is not a learned weight or a substitute for the pip forecast. It only
 * resolves near-ties in favor of a player's stated tendency and soft reserve.
 * Neutral is a literal no-op, preserving the frozen legacy baseline.
 */
object ResourceAcquisitionPreference {
    private const val FIRST_MISSING_UNIT_BONUS = 8
    private const val ADDITIONAL_MISSING_UNIT_BONUS = 2
    private const val MAX_LIKE_BONUS = 12
    private const val TARGET_REACHED_PENALTY = -4
    private const val AVOID_BONUS = -12

    fun apply(
        score: PriorityScore,
        resource: String,
        goal: ResourceGoal,
        currentlyHeld: Int,
        viable: Boolean = true
    ): PriorityScore {
        require(currentlyHeld >= 0) { "Held resource count cannot be negative" }
        if (!viable || score.total <= 0 || goal.inclination == ResourceInclination.NEUTRAL) {
            return score
        }
        val difference = goal.softTarget - currentlyHeld
        val influence = when (goal.inclination) {
            ResourceInclination.NEUTRAL -> 0
            ResourceInclination.LIKE -> if (difference > 0) {
                (FIRST_MISSING_UNIT_BONUS + (difference - 1) * ADDITIONAL_MISSING_UNIT_BONUS)
                    .coerceAtMost(MAX_LIKE_BONUS)
            } else TARGET_REACHED_PENALTY
            ResourceInclination.AVOID -> AVOID_BONUS
        }
        val explanation = when (goal.inclination) {
            ResourceInclination.LIKE -> if (difference > 0) {
                "Personality likes $resource: $currentlyHeld held; soft aim ${goal.softTarget}"
            } else {
                "Personality likes $resource but its soft aim ${goal.softTarget} is met"
            }
            ResourceInclination.AVOID -> "Personality tends to avoid $resource (not forbidden)"
            ResourceInclination.NEUTRAL -> return score
        }
        return score.adjusted(influence, explanation)
    }
}
