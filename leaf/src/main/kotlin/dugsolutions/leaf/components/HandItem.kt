package dugsolutions.leaf.components

import dugsolutions.leaf.components.die.Die

sealed class HandItem {
    data class Card(val card: GameCard) : HandItem()
    data class Dice(val die: Die) : HandItem()
} 