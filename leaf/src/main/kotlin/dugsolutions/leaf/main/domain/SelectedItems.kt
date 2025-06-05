package dugsolutions.leaf.main.domain

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die

data class SelectedItems(
    val cards: List<GameCard> = emptyList(),
    val floralCards: List<GameCard> = emptyList(),
    val dice: List<Die> = emptyList()
)
