package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.cards.domain.GameCards

data class CardsToRefresh(
    val cards: GameCards = GameCards(emptyList())
)
