package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.wisp.domain.WispCard

sealed interface ActionCultivation {
    data object PullDie : ActionCultivation
    data class DoRoundAction(val actionRound: ActionRound) : ActionCultivation
    data class ExecuteCard(
        val card: GameCard,
        val target: ExecuteTarget? = null
    ) : ActionCultivation
    data class PlayWispCard(val card: WispCard, val target: ExecuteTarget? = null): ActionCultivation
    data class PlayMulchToken(val token: Token.MULCH): ActionCultivation
    data class PlayWaterToken(val onDie: Die? = null): ActionCultivation
}

