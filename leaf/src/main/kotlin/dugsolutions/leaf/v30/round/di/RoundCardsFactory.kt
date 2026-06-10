package dugsolutions.leaf.v30.round.di

import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.round.domain.RoundCards

class RoundCardsFactory {

    operator fun invoke(cards: List<RoundCard>): RoundCards {
        return RoundCards(cards)
    }
}
