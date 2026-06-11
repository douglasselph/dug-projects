package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.table.Table

open class GameCardEffectExecutorBattle {

    open operator fun invoke(
        table: Table,
        player: Player,
        action: MainAction.ExecuteCard
    ) {
        // TODO: implement battle-side game card effects.
    }
}
