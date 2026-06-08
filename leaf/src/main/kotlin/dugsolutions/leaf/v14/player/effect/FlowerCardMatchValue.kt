package dugsolutions.leaf.v14.player.effect

import dugsolutions.leaf.v14.cards.domain.FlourishType
import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.cards.domain.MatchWith
import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.player.Player

class FlowerCardMatchValue(
    private val floralBonusCount: FloralBonusCount,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(player: Player, bloomCard: GameCard): Int {
        if (bloomCard.type != FlourishType.BLOOM) {
            return 0
        }
        if (bloomCard.matchWith !is MatchWith.Flower) {
            return 0
        }
        val flowerCardId = bloomCard.flowerCardId ?: return 0
        val cards = player.decisionDirector.flowerSelectDecision().value
        val cardIds = cards.map { it.id }
        cards.forEach { card ->
            player.removeCardFromFloralArray(card.id)
            player.addCardToHand(card.id)
        }
        if (cards.isNotEmpty()) {
            chronicle(Moment.USE_FLOWERS(player, cards))
        }
        return floralBonusCount(cardIds, flowerCardId)
    }
}
