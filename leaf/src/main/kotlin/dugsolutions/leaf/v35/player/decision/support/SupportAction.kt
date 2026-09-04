package dugsolutions.leaf.v35.player.decision.support

import dugsolutions.leaf.v35.player.creature.CreatureCardId
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
    data class PlayWisp(val card: WispCard) : SupportAction
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
    val rerolledValue: Int
)

interface SupportStrategy {
    fun chooseButterflyRoll(
        request: ChooseButterflyRollRequest
    ): ButterflyRollChoice
}
