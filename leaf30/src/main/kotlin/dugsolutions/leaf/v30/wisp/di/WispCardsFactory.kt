package dugsolutions.leaf.v30.wisp.di

import dugsolutions.leaf.v30.wisp.domain.WispCard
import dugsolutions.leaf.v30.wisp.domain.WispCards

class WispCardsFactory {

    operator fun invoke(cards: List<WispCard>): WispCards {
        return WispCards(cards)
    }
}
