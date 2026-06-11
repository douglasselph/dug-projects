package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.round.domain.RoundCardType
import dugsolutions.leaf.v30.table.Table

open class GameCardEffectExecutor(
    private val cultivationExecutor: GameCardEffectExecutorCultivation = GameCardEffectExecutorCultivation(),
    private val battleExecutor: GameCardEffectExecutorBattle = GameCardEffectExecutorBattle()
) {

    open operator fun invoke(
        table: Table,
        player: Player,
        card: GameCard
    ) {
        when (table.currentRoundType) {
            RoundCardType.CULTIVATION -> cultivationExecutor(table, player, card)
            RoundCardType.BATTLE -> battleExecutor(table, player, card)
        }
    }
}
