package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

class EffectDrawCard(
    private val cardManager: CardManager,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, fromDiscard: Boolean = false) {
        val cardId = if (fromDiscard) {
            player.drawCardFromDiscard()
        } else {
            player.drawCard()
        }
        val card = cardId?.let { id -> cardManager.getCard(id) }
        card?.let {
            chronicle(Moment.DRAW_CARD(player, card.id))
            player.cardsToPlay.add(card)
        }
    }
}
