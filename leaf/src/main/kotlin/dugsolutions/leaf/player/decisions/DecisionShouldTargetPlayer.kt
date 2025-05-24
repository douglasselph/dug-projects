package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.player.Player

interface DecisionShouldTargetPlayer {

    operator fun invoke(target: Player, amount: Int): Boolean

}
