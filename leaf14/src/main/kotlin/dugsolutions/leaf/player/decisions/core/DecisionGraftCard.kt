package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.cards.domain.GameCard

interface DecisionGraftCard {

    suspend operator fun invoke(possibleCards: List<GameCard>): GameCard?

}
