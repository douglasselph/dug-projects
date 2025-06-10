package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.FloralBonusCount

class FlowerCardMatchValue(
    private val floralBonusCount: FloralBonusCount
) {

    suspend operator fun invoke(player: Player, bloomCard: GameCard): Int {
        if (bloomCard.type != FlourishType.BLOOM) {
            return 0
        }
        if (bloomCard.matchWith !is MatchWith.Flower) {
            return 0
        }
        val flowerCardId = bloomCard.flowerCardId ?: return 0
        val cards = player.decisionDirector.flowerSelectDecision()
        val cardIds = cards.map { it.id }
        return floralBonusCount(cardIds, flowerCardId)
    }
}
