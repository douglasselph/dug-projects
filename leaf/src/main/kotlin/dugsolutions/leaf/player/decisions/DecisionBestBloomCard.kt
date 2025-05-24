package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.components.GameCard

interface DecisionBestBloomCard {

    operator fun invoke(cards: List<GameCard>): GameCard

}
