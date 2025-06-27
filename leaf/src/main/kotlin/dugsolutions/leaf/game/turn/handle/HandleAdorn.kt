package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

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
