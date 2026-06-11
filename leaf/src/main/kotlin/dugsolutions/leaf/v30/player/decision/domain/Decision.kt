package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.battle.domain.BattleGridSnapshot
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

sealed interface Decision {

    data class ChooseCritter(
        val player: Player,
        val availableCritters: List<Critter>
    ) : Decision

    data class ChooseMainActionCultivation(
        val player: Player,
        val roundCard: RoundCard,
        val table: Table,
        val actionsRemaining: Int
    ) : Decision

    data class ChooseMainActionBattle(
        val player: Player,
        val roundCard: RoundCard,
        val table: Table,
        val battleGridSnapshot: BattleGridSnapshot,
        val actionsRemaining: Int
    ) : Decision

    data class ChooseItemsToBuy(
        val player: Player,
        val grove: Grove
    ) : Decision

    data class ChooseCardsToRefreshWithWorms(
        val player: Player
    ) : Decision
}
