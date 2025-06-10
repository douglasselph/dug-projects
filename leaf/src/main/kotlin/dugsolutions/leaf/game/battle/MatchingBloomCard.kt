package dugsolutions.leaf.game.battle

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith

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
