package dugsolutions.leaf.grove

import dugsolutions.leaf.cards.domain.GameCard

class SelectPossibleCards(
    private val grove: Grove
) {
    operator fun invoke(): List<GameCard> {
        return grove.getTopShowingCards()
    }
}
