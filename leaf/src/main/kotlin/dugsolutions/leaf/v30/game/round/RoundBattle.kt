package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleGridSnapshot
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

class RoundBattle(
    table: Table,
    card: RoundCard,
    chronicle: Chronicle = GameChronicle(),
    private val battle: Battle = table.battle
) : RoundBase(table, card, chronicle) {

    fun prepare() {
        battle.setup(table.players)
    }

    override fun performMainActions() {
        val snapshot = battle.snapshot()
        val playersById = table.players.associateBy { it.id }
        snapshot.playerIdsInGridOrder.asReversed().forEach { playerId ->
            playersById[playerId]?.let { player ->
                performMainAction(player, snapshot)
            }
        }
    }

    private fun performMainAction(
        player: Player,
        snapshot: BattleGridSnapshot
    ) {
        when (
            player.decisionDirector.chooseMainActionBattle(
                Decision.ChooseMainActionBattle(
                    player = player,
                    roundCard = card,
                    table = table,
                    battleGridSnapshot = snapshot
                )
            )
        ) {
            MainAction.PullDie -> player.drawDiceWithRefresh()
            is MainAction.DoRoundAction -> {
            }
            is MainAction.ExecuteCard -> {
            }
        }
    }

    fun performSupportActions() {

    }
}
