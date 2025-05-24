package dugsolutions.leaf.player.domain

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die

sealed class ExtendedHandItem {
    data class Card(val card: GameCard) : ExtendedHandItem()
    data class Dice(val die: Die) : ExtendedHandItem()
    data class FloralArray(val card: GameCard) : ExtendedHandItem()
}
