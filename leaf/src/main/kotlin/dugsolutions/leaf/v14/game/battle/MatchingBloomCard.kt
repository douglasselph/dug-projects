package dugsolutions.leaf.v14.game.battle

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.cards.domain.FlourishType
import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.cards.domain.MatchWith

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
