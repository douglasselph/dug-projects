package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.wisp.domain.WispCard

sealed interface MainAction {
    data object PullDie : MainAction
    data class DoRoundAction(val roundAction: RoundAction) : MainAction
    data class ExecuteCard(
        val card: GameCard,
        val target: ExecuteTarget? = null
    ) : MainAction
    data class DoWispCard(val card: WispCard): MainAction
}

sealed interface ExecuteTarget {
    data class PlayerDie(
        val player: Player,
        val die: Die
    ) : ExecuteTarget
}
