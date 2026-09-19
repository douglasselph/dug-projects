package dugsolutions.leaf.v14.player.decisions.core

import dugsolutions.leaf.v14.cards.domain.GameCard

interface DecisionBestBloomAcquisitionCard {

    operator fun invoke(cards: List<GameCard>): GameCard

}
