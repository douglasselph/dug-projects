package dugsolutions.leaf.player.domain

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die

sealed class HandItem {
    data class aCard(val card: GameCard) : HandItem()
    data class aDie(val die: Die) : HandItem()
} 
