package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.wisp.domain.WispCard

open class WispCardEffectExecutor {

    open operator fun invoke(
        table: Table,
        player: Player,
        card: WispCard
    ) {
        // TODO: implement wisp card effects once effect implementations are modeled.
    }
}
