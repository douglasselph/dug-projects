package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.player.Player

class EffectDraw(
    private val cardManager: CardManager
) {

    /** CardEffect.DRAW */
    operator fun invoke(player: Player) {
        val countCards = player.cardsInSupplyCount
        val countDice = player.diceInSupplyCount
        if (countCards > countDice) {
            player.drawCard()?.let { cardId ->
                cardManager.getCard(cardId)?.let { card ->
                    player.cardsToPlay.add(card)
                }
            }
        } else {
            player.drawDie()
        }
    }
}
