package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

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
