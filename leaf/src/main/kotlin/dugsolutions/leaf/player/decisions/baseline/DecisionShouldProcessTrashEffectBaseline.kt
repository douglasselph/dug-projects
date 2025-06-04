package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.local.GroveNearingTransition

/**
 * Strategy for determining when to process a card's trash effect.
 *
 * Algorithm:
 * When the number of cards in 3 or more piles in the Grove is 1 or less, then start trashing everything if it is a seedling.
 * Otherwise just don't. Need AI behavior to determine best time otherwise. This is later.
 */
class DecisionShouldProcessTrashEffectBaseline(
    private val groveNearingTransition: GroveNearingTransition
) : DecisionShouldProcessTrashEffect {

    // region public

    override suspend fun invoke(card: GameCard): DecisionShouldProcessTrashEffect.Result {
        // If card has no effects, always trash
        if (card.primaryEffect == null && card.matchEffect == null) {
            return DecisionShouldProcessTrashEffect.Result.TRASH
        }
        if (card.type != FlourishType.SEEDLING) {
            return DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH
        }
        if (groveNearingTransition()) {
            return DecisionShouldProcessTrashEffect.Result.TRASH
        }
        return DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH
    }

    /**
     * Call this in-between games to reset the tracking of seen cards.
     */
    override fun reset() {
    }

    // endregion public

}
