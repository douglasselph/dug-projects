package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.domain.GameTime

class ShouldProcessMatchEffect(
    private val gameTime: GameTime
) {

    /**
     * Determine if it makes sense at all to apply this match effect.
     * This would only be false, if the match is OnRoll and the effect in question only applies during the Cultivation Phase.
     */
    operator fun invoke(card: GameCard): Boolean {
        if (card.matchWith !is MatchWith.OnRoll) {
            return true
        }
        if (gameTime.phase == GamePhase.CULTIVATION) {
            return true
        }
        return when (card.matchEffect) {
            CardEffect.REDUCE_COST_ROOT,
            CardEffect.REDUCE_COST_VINE,
            CardEffect.REDUCE_COST_CANOPY,
            CardEffect.FLOURISH_OVERRIDE -> false

            else -> true
        }
    }
}
