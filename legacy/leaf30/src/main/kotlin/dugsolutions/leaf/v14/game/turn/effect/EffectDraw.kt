package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.player.Player

class EffectDraw(
    private val cardManager: CardManager,
    private val chronicle: GameChronicle
) {

    // TODO: Unit test
    operator fun invoke(player: Player) {
        val countCards = player.cardsInSupplyCount
        val countDice = player.diceInSupplyCount
        if (countCards > countDice) {
            val result = player.drawCard()
            result.cardId?.let { cardId ->
                cardManager.getCard(cardId)?.let { card ->
                    player.cardsToPlay.add(card)
                }
                chronicle(Moment.DRAW_CARD(player, cardId, result.reshuffleDone))
            }
        } else {
            val result = player.drawDie()
            result.die?.let { die ->
                chronicle(Moment.DRAW_DIE(player, die, result.reshuffleDone))
            }
        }
    }
}
