package dugsolutions.leaf.player.domain

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die

sealed class ExtendedHandItem {
    data class Card(val card: GameCard) : ExtendedHandItem()
    data class Dice(val die: Die) : ExtendedHandItem()
    data class FloralArray(val card: GameCard) : ExtendedHandItem()
}
