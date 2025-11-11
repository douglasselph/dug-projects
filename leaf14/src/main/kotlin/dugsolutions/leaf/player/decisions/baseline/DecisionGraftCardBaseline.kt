package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionGraftCard

class DecisionGraftCardBaseline(
    private val player: Player
) : DecisionGraftCard {

    override suspend fun invoke(possibleCards: List<GameCard>): GameCard? {
        return null
    }

}
