package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.grove.local.GroveNearingTransition
import dugsolutions.leaf.player.Player

/**
 * Strategy for determining when to process a card's trash effect.
 *
 * Algorithm:
 * When the number of cards in 3 or more piles in the Grove is 1 or less, then start trashing everything if it is a seedling.
 * Otherwise just don't. Need AI behavior to determine best time otherwise. This is later.
 */
class DecisionShouldProcessTrashEffectBaseline(
    private val player: Player,
    private val groveNearingTransition: GroveNearingTransition,
    private val gameTime: GameTime
) : DecisionShouldProcessTrashEffect {

    // region public

    // TODO: Unit tests.
    override suspend fun invoke(card: GameCard): DecisionShouldProcessTrashEffect.Result {
        // If card has no effects, always trash
        if (card.primaryEffect == null && card.matchEffect == null) {
            return DecisionShouldProcessTrashEffect.Result.TRASH
        }
        if (card.type == FlourishType.SEEDLING) {
            if (groveNearingTransition()) {
                return DecisionShouldProcessTrashEffect.Result.TRASH
            }
        } else if (gameTime.phase == GamePhase.BATTLE) {
            if (card.matchWith is MatchWith.Flower) {
                // Count supporting flower cards -- if there are none left then trash
                val matchingFlowerCardId = card.matchWith.flowerCardId
                val countFlowers = player.allCardsInDeck.filter { it.id == matchingFlowerCardId }.size
                if (countFlowers == 0) {
                    return DecisionShouldProcessTrashEffect.Result.TRASH
                }
            } else {
                // Otherwise if we are close to losing might as well trash.
                if (player.allDice.size <= 4) {
                    return DecisionShouldProcessTrashEffect.Result.TRASH
                }
                val totalItems = player.allCardsInDeck.size + player.allDice.size
                if (totalItems <= 7) {
                    return DecisionShouldProcessTrashEffect.Result.TRASH
                }
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
