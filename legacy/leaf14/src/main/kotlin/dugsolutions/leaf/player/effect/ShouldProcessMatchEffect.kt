package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.common.domain.game.GamePhase
import dugsolutions.leaf.common.domain.game.GameTime


class ShouldProcessMatchEffect(
    private val gameTime: GameTime
) {

    /**
     * Determine if it makes sense at all to apply this match effect.
     * This would only be false, if the match is OnRoll and the effect in question only applies during the Cultivation Phase.
     */
    operator fun invoke(card: GameCard): Boolean {
        if (gameTime.phase == GamePhase.CULTIVATION) {
            return true
        }
        return when (card.matchEffect) {

            else -> true
        }
    }
}
