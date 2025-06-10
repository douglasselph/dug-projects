package dugsolutions.leaf.game.acquire.domain

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.random.die.Die

sealed class Credit {

    data class CredDie(val die: Die) : Credit()
    data class CredAddToTotal(val amount: Int) : Credit()
    data class CredReduceCost(val type: FlourishType, val amount: Int): Credit()

}
