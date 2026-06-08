package dugsolutions.leaf.v14.main.domain

import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.random.die.Die

data class SelectedItems(
    val cards: List<GameCard> = emptyList(),
    val floralCards: List<GameCard> = emptyList(),
    val dice: List<Die> = emptyList()
)
