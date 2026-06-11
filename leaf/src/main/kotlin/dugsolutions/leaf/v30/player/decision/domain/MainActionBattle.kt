package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.wisp.domain.WispCard

sealed interface MainActionBattle {
    data class PullDie(val row: BattleStrikeRow) : MainActionBattle
    data class DoRoundAction(val roundAction: RoundAction) : MainActionBattle
    data class ExecuteCard(
        val card: GameCard,
        val target: ExecuteTarget? = null
    ) : MainActionBattle
    data class PlayWispCard(val card: WispCard, val wispCardTarget: ExecuteTarget? = null): MainActionBattle
    data class PlayMulchToken(val token: Token.MULCH, val row: BattleStrikeRow): MainActionBattle
    data class PlayWaterToken(val onDie: Die? = null, val row: BattleStrikeRow?): MainActionBattle
}

