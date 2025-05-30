package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.decisions.core.DecisionBestBloomCard

class DecisionBestBloomCardBaseline : DecisionBestBloomCard {

    override fun invoke(cards: List<GameCard>): GameCard {
        return cards.first()
    }

}
