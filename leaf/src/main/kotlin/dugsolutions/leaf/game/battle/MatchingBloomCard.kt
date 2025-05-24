package dugsolutions.leaf.game.battle

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith

class MatchingBloomCard(
    private val cardManager: CardManager
) {
    operator fun invoke(flowerCard: GameCard): GameCard? {
        return cardManager.getCardsByType(FlourishType.BLOOM)
            .find { bloomCard ->
                when (bloomCard.matchWith) {
                    is MatchWith.Flower -> bloomCard.matchWith.flowerCardId == flowerCard.id
                    else -> false
                }
            }
    }
}
