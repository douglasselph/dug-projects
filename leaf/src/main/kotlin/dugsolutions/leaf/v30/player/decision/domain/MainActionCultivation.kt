package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.wisp.domain.WispCard

sealed interface MainActionCultivation {
    data object PullDie : MainActionCultivation
    data class DoRoundAction(val roundAction: RoundAction) : MainActionCultivation
    data class ExecuteCard(
        val card: GameCard,
        val target: ExecuteTarget? = null
    ) : MainActionCultivation
    data class PlayWispCard(val card: WispCard, val target: ExecuteTarget? = null): MainActionCultivation
    data class PlayMulchToken(val token: Token.MULCH): MainActionCultivation
    data class PlayWaterToken(val onDie: Die? = null): MainActionCultivation
}

