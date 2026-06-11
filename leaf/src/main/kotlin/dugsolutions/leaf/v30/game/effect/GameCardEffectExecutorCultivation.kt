package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.table.Table

open class GameCardEffectExecutorCultivation {

    open operator fun invoke(
        table: Table,
        player: Player,
        card: GameCard
    ) {
        // TODO: implement cultivation-side game card effects.
    }
}
