package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.components.GameCard

class DecisionBestBloomCardCoreStrategy : DecisionBestBloomCard {

    override fun invoke(cards: List<GameCard>): GameCard {
        return cards.first()
    }

}
