package dugsolutions.leaf.v35.player.decision.baseline.reward

import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.tokens.Critter

/** Human Baseline's intentionally small ordinary Bee/Worm preference. */
object CritterRewardPriority {
    const val BASE_SCORE: Int = 50
    const val ORDINARY_PREFERENCE_BONUS: Int = 20

    fun score(critter: Critter, ordinaryPreference: Critter): PriorityScore =
        PriorityScore(base = BASE_SCORE).let { score ->
            if (critter == ordinaryPreference) {
                score.adjusted(
                    ORDINARY_PREFERENCE_BONUS,
                    "Ordinary Critter reward preference"
                )
            } else {
                score
            }
        }

    fun tags(critter: Critter): Set<DecisionTag> =
        setOf(
            when (critter) {
                Critter.BEE -> DecisionTag.ACQUIRE_BEE
                Critter.WORM -> DecisionTag.ACQUIRE_WORM
            }
        )
}
