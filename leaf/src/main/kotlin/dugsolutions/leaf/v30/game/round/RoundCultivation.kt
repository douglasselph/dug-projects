package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.game.effect.GameCardEffectExecutor
import dugsolutions.leaf.v30.game.effect.RoundActionExecutor
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

class RoundCultivation(
    table: Table,
    card: RoundCard,
    private val roundActionExecutor: RoundActionExecutor = RoundActionExecutor(),
    private val gameCardEffectExecutor: GameCardEffectExecutor = GameCardEffectExecutor()
) : RoundBase(table, card) {

    fun performMainActions() {
        table.players.forEach { player ->
            repeat(ACTIONS_PER_PLAYER) { actionIndex ->
                performMainAction(
                    player = player,
                    actionsRemaining = ACTIONS_PER_PLAYER - actionIndex
                )
            }
        }
    }

    private fun performMainAction(
        player: Player,
        actionsRemaining: Int
    ) {
        when (
            val action = player.decisionDirector.chooseMainAction(
                Decision.ChooseMainAction(
                    player = player,
                    roundCard = card,
                    actionsRemaining = actionsRemaining
                )
            )
        ) {
            MainAction.PullDie -> player.drawDiceWithRefresh()
            is MainAction.DoRoundAction -> {
                roundActionExecutor.execute(
                    table = table,
                    player = player,
                    card = card,
                    action = action.roundAction
                )
            }
            is MainAction.ExecuteCard -> {
                gameCardEffectExecutor.execute(
                    table = table,
                    player = player,
                    card = action.card
                )
            }
        }
    }

    private companion object {
        const val ACTIONS_PER_PLAYER = 2
    }
}
