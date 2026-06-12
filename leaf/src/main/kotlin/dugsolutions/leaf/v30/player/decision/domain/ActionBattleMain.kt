package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.wisp.domain.WispCard

sealed interface ActionBattleMain {
    data class PullDie(val row: BattleStrikeRow) : ActionBattleMain
    data class DoRoundAction(val actionRound: ActionRound) : ActionBattleMain
    data class ExecuteCard(
        val card: GameCard,
        val target: ExecuteTarget? = null,
        val row: BattleStrikeRow? = null,
        val row2: BattleStrikeRow? = null,
        val usesAction: Boolean = true
    ) : ActionBattleMain
    data class PlayWispCard(val card: WispCard, val wispCardTarget: ExecuteTarget? = null) : ActionBattleMain
}

