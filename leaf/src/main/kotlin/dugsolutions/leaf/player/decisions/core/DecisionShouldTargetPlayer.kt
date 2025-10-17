package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.player.Player

interface DecisionShouldTargetPlayer {

    operator fun invoke(target: Player, amount: Int): Boolean

}
