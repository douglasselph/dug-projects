package dugsolutions.leaf.v14.player.decisions.baseline

import dugsolutions.leaf.v14.cards.domain.FlourishType
import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.v14.grove.local.GroveNearingTransition

/**
 * Strategy for determining when to process a card's trash effect.
 */
class DecisionShouldProcessTrashEffectBaseline(
    private val groveNearingTransition: GroveNearingTransition,
) : DecisionShouldProcessTrashEffect {

    // region public

    override suspend fun invoke(card: GameCard): DecisionShouldProcessTrashEffect.Result {
        if (card.trashEffect == null) {
            return DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH
        }
        // If card has no effects, always trash
        if (card.primaryEffect == null && card.matchEffect == null) {
            return DecisionShouldProcessTrashEffect.Result.TRASH
        }
        if (card.type == FlourishType.SEEDLING) {
            if (groveNearingTransition()) {
                return DecisionShouldProcessTrashEffect.Result.TRASH
            }
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
