package dugsolutions.leaf.v14.player.decisions.baseline

import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.player.decisions.core.DecisionBestBloomAcquisitionCard

class DecisionBestBloomAcquisitionCardBaseline : DecisionBestBloomAcquisitionCard {

    override fun invoke(cards: List<GameCard>): GameCard {
        return cards.first()
    }

}
