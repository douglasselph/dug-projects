package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.cards.domain.GameCard

interface DecisionBestBloomAcquisitionCard {

    operator fun invoke(cards: List<GameCard>): GameCard

}
