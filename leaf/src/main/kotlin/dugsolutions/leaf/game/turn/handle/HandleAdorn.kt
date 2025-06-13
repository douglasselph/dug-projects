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
            player.addCardToBuddingStack(flowerCard.id)
            val cardId = player.drawCard() ?: 0
            chronicle(Moment.ADORN(player, flowerCardId = flowerCard.id, drawCardId = cardId))
            if (cardId > 0) {
                chronicle(Moment.DRAW_CARD(player, cardId))
                cardManager.getCard(cardId)?.let { card ->
                    if (card.type == FlourishType.FLOWER) {
                        flowerCards.add(card)
                    }
                }
            }
        }
    }

}
