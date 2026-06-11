package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.round.domain.RoundCardType
import dugsolutions.leaf.v30.table.Table

open class GameCardEffectExecutor(
    private val cultivationExecutor: GameCardEffectExecutorCultivation = GameCardEffectExecutorCultivation(),
    private val battleExecutor: GameCardEffectExecutorBattle = GameCardEffectExecutorBattle()
) {

    open operator fun invoke(
        table: Table,
        player: Player,
        action: MainAction.ExecuteCard
    ) {
        when (table.currentRoundType) {
            RoundCardType.CULTIVATION -> cultivationExecutor(table, player, action)
            RoundCardType.BATTLE -> battleExecutor(table, player, action)
        }
    }
}
