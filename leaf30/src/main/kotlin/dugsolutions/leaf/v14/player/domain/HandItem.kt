package dugsolutions.leaf.v14.player.domain

import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.random.die.Die

sealed class HandItem {
    data class aCard(val card: GameCard) : HandItem()
    data class aDie(val die: Die) : HandItem()
} 
