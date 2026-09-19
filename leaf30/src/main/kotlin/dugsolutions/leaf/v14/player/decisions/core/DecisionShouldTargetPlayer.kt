package dugsolutions.leaf.v14.player.decisions.core

import dugsolutions.leaf.v14.player.Player

interface DecisionShouldTargetPlayer {

    operator fun invoke(target: Player, amount: Int): Boolean

}
