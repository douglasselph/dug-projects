package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ActionRound
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

open class RoundActionExecutor {

    open operator fun invoke(
        table: Table,
        player: Player,
        card: RoundCard,
        action: ActionRound
    ) {
        // TODO: route round action effects once effect implementations are modeled.
    }
}
