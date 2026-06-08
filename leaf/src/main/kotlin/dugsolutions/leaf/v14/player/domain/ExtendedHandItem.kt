package dugsolutions.leaf.v14.player.domain

import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.random.die.Die

sealed class ExtendedHandItem {
    data class Card(val card: GameCard) : ExtendedHandItem()
    data class Dice(val die: Die) : ExtendedHandItem()
    data class FloralArray(val card: GameCard) : ExtendedHandItem()
}
