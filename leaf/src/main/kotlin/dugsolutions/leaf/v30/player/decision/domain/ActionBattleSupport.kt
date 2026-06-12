package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.wisp.domain.WispCard

sealed interface ActionBattleSupport {
    data class PlayMulchToken(val token: Token.MULCH, val row: BattleStrikeRow): ActionBattleSupport
    data class PlayWaterToken(val onDie: Die? = null, val row: BattleStrikeRow?): ActionBattleSupport
    data class PlayWispCard(val card: WispCard, val wispCardTarget: ExecuteTarget? = null) : ActionBattleSupport
    data object None: ActionBattleSupport
}

