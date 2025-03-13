package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player

interface DecisionShouldProcessTrashEffect {

    operator fun invoke(card: GameCard): Boolean
    fun reset()

}
