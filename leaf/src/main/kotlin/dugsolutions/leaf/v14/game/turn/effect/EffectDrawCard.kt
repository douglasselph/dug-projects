package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.player.Player

class EffectDrawCard(
    private val cardManager: CardManager,
    private val chronicle: GameChronicle
) {

    // TODO: Unit test
    operator fun invoke(player: Player, fromDiscard: Boolean = false) {
        val result = if (fromDiscard) {
            player.drawCardFromDiscard()
        } else {
            player.drawCard()
        }
        val card = result.cardId?.let { id -> cardManager.getCard(id) }
        card?.let {
            chronicle(Moment.DRAW_CARD(player, card.id, result.reshuffleDone))
            player.cardsToPlay.add(card)
        }
    }
}
