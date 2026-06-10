package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.RoundAction
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

open class RoundActionExecutor {

    open fun execute(
        table: Table,
        player: Player,
        card: RoundCard,
        action: RoundAction
    ) {
        // TODO: route round action effects once effect implementations are modeled.
    }
}
