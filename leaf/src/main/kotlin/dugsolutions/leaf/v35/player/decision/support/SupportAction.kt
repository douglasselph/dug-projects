package dugsolutions.leaf.v35.player.decision.support

import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Token
import dugsolutions.leaf.v35.wisp.domain.WispCard

/** Immutable reference to one currently visible die in a player's Hand. */
data class HandDieChoice(
    val index: Int,
    val sides: Int,
    val value: Int
) {
    init {
        require(index >= 0) { "Hand die index cannot be negative: $index" }
        require(sides > 0) { "Hand die sides must be positive: $sides" }
        require(value > 0) { "Hand die value must be positive: $value" }
    }
}

/**
 * Shared support-action vocabulary.
 *
 * Cultivation and Battle have different turn sequencing, but these player
 * resources represent the same underlying actions in both phases.
 */
sealed interface SupportAction {
    /**
     * A Wisp Support choice. [decisionProbabilityPercent] is optional strategy
     * trace metadata: it records the raw willingness percentage that allowed a
     * probabilistic Human Baseline play (currently Overgrowth). It is ignored
     * for legality/equality so adding the trace never changes the game action.
     */
    class PlayWisp(
        val card: WispCard,
        val decisionProbabilityPercent: Int? = null
    ) : SupportAction {
        init {
            require(decisionProbabilityPercent == null || decisionProbabilityPercent in 0..100) {
                "Wisp decision probability must be 0..100: $decisionProbabilityPercent"
            }
        }

        fun withDecisionProbability(percent: Int?): PlayWisp =
            PlayWisp(card, percent)

        override fun equals(other: Any?): Boolean =
            other is PlayWisp && card == other.card

        override fun hashCode(): Int = card.hashCode()

        override fun toString(): String =
            buildString {
                append("PlayWisp(card=$card")
                decisionProbabilityPercent?.let { append(", decisionProbabilityPercent=$it") }
                append(')')
            }
    }
    data class UseWaterReroll(val die: HandDieChoice) : SupportAction
    data object UseWaterRefresh : SupportAction
    data class UseMulch(val token: Token.MULCH) : SupportAction
    data class UseWormFlip(val cardId: CreatureCardId) : SupportAction
    data class UseButterfly(
        val butterfly: Butterfly,
        val die: HandDieChoice
    ) : SupportAction
}

enum class ButterflyRollChoice {
    ORIGINAL,
    REROLLED
}

class ChooseButterflyRollRequest(
    val sides: Int,
    val originalValue: Int,
    val rerolledValue: Int,
    val context: DecisionContext = DecisionContext.EMPTY
)

interface SupportStrategy {
    fun chooseButterflyRoll(
        request: ChooseButterflyRollRequest
    ): ButterflyRollChoice
}
