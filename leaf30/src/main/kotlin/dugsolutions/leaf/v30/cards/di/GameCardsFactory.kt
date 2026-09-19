package dugsolutions.leaf.v30.cards.di

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.cards.domain.GameCards

class GameCardsFactory {

    operator fun invoke(cards: List<GameCard>): GameCards {
        return GameCards(cards)
    }

}
