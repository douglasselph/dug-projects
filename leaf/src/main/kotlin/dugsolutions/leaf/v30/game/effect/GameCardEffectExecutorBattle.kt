package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.table.Table

open class GameCardEffectExecutorBattle {

    open operator fun invoke(
        table: Table,
        player: Player,
        card: GameCard
    ) {
        // TODO: implement battle-side game card effects.
    }
}
