package dugsolutions.leaf.v14.game.turn.handle

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.cards.domain.FlourishType
import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.player.Player

/**
 * Process the ADORN effect immediately since we need the flower cards to be in the FloralArray
 * in order to properly handle BLOOM affects.
 */
class HandleAdorn(
    private val cardManager: CardManager,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player) {
        val flowerCards = player.cardsToPlay.filter { it.type == FlourishType.FLOWER }.toMutableList()
        player.cardsToPlay.removeIf { it.type == FlourishType.FLOWER }
        while (flowerCards.isNotEmpty()) {
            val flowerCard = flowerCards.removeAt(0)
            player.removeCardFromHand(flowerCard.id)
            player.addCardToFloralArray(flowerCard.id)
            val result = player.drawCard()
            val cardId = result.cardId ?: 0
            chronicle(Moment.ADORN(player, flowerCardId = flowerCard.id, drawCardId = cardId))
            if (cardId > 0) {
                chronicle(Moment.DRAW_CARD(player, cardId, result.reshuffleDone)) // TODO: Unit test
                cardManager.getCard(cardId)?.let { card ->
                    if (card.type == FlourishType.FLOWER) {
                        flowerCards.add(card)
                    }
                }
            }
        }
    }

}
