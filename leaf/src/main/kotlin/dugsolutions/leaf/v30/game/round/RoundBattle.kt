package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleGridSnapshot
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.game.effect.WispCardEffectExecutor
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

class RoundBattle(
    table: Table,
    card: RoundCard,
    chronicle: Chronicle = GameChronicle(),
    private val battle: Battle = table.battle,
    private val wispCardEffectExecutor: WispCardEffectExecutor = WispCardEffectExecutor()
) : RoundBase(table, card, chronicle) {

    private companion object {
        const val ACTIONS_PER_PLAYER = 2
        const val MAX_MAIN_ACTION_ATTEMPTS = 10
    }

    fun prepare() {
        battle.setup(table.players)
    }

    override fun performMainActions() {
        val snapshot = battle.snapshot()
        val playersById = table.players.associateBy { it.id }
        snapshot.playerIdsInGridOrder.asReversed().forEach { playerId ->
            playersById[playerId]?.let { player ->
                var actionsRemaining = ACTIONS_PER_PLAYER
                var attempts = 0
                while (actionsRemaining > 0) {
                    attempts++
                    if (attempts > MAX_MAIN_ACTION_ATTEMPTS) {
                        throw MainActionException(
                            "Exceeded $MAX_MAIN_ACTION_ATTEMPTS main action attempts for player ${player.id}"
                        )
                    }
                    val actionSpent = performMainAction(
                        player = player,
                        snapshot = battle.snapshot(),
                        actionsRemaining = actionsRemaining
                    )
                    if (actionSpent) {
                        actionsRemaining--
                    }
                }
            }
        }
    }

    private fun performMainAction(
        player: Player,
        snapshot: BattleGridSnapshot,
        actionsRemaining: Int
    ): Boolean {
        when (
            val action = player.decisionDirector.chooseMainActionBattle(
                Decision.ChooseMainActionBattle(
                    player = player,
                    roundCard = card,
                    table = table,
                    battleGridSnapshot = snapshot,
                    actionsRemaining = actionsRemaining
                )
            )
        ) {
            MainAction.PullDie -> player.drawDiceWithRefresh()
            is MainAction.DoRoundAction -> {
            }
            is MainAction.ExecuteCard -> {
            }
            is MainAction.DoWispCard -> {
                wispCardEffectExecutor(
                    table = table,
                    player = player,
                    card = action.card
                )
                return false
            }
        }
        return true
    }

    fun performSupportActions() {

    }
}
