package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

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
