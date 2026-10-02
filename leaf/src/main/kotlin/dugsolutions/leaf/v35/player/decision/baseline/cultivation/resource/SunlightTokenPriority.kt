package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Simple ordinary-human value for banking a Sunlight token for Battle.
 *
 * The first token is intentionally attractive relative to a weak Draw, the
 * second remains useful, and additional stored tokens have diminishing value.
 * If the public round pattern says no Battle remains, a new token has no
 * practical value under the current Battle-only spending rule.
 */
object SunlightTokenPriority {
    private const val FIRST_TOKEN_SCORE = 65
    private const val SECOND_TOKEN_SCORE = 55
    private const val ADDITIONAL_TOKEN_SCORE = 40

    fun score(context: DecisionContext): PriorityScore {
        if (context.progress.battleRoundsRemaining == 0) {
            return PriorityScore(0).adjusted(0, "No Battle remains for stored Sunlight")
        }
        return when (context.self.board.sunlight) {
            0 -> PriorityScore(FIRST_TOKEN_SCORE).adjusted(0, "Bank first Sunlight for a Battle extra Main")
            1 -> PriorityScore(SECOND_TOKEN_SCORE).adjusted(0, "A second Battle extra Main remains useful")
            else -> PriorityScore(ADDITIONAL_TOKEN_SCORE).adjusted(0, "Stored Sunlight already provides multiple Battle extra Mains")
        }
    }
}
