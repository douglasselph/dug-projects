package dugsolutions.leaf.main.domain

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die

data class SelectedItems(
    val cards: List<GameCard> = emptyList(),
    val floralCards: List<GameCard> = emptyList(),
    val dice: List<Die> = emptyList()
)
