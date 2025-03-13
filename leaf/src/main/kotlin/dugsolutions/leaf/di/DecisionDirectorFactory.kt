package dugsolutions.leaf.di

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionDirector

class DecisionDirectorFactory(
    private val cardManager: CardManager
) {

    operator fun invoke(player: Player): DecisionDirector {
        return DecisionDirector(player, cardManager)
    }

}
