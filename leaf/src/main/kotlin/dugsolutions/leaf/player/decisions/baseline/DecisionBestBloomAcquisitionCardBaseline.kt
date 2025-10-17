package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.decisions.core.DecisionBestBloomAcquisitionCard

class DecisionBestBloomAcquisitionCardBaseline : DecisionBestBloomAcquisitionCard {

    override fun invoke(cards: List<GameCard>): GameCard {
        return cards.first()
    }

}
