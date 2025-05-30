package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.components.GameCard

interface DecisionBestBloomCard {

    operator fun invoke(cards: List<GameCard>): GameCard

}
