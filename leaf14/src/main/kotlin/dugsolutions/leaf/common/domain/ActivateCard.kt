package dugsolutions.leaf.common.domain

import dugsolutions.leaf.cards.domain.GameCard

data class ActivateCard(
    val card: GameCard,
    val token: Token
)
