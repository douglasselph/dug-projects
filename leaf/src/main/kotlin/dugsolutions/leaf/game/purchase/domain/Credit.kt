package dugsolutions.leaf.game.purchase.domain

import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.die.Die

sealed class Credit {

    data class CredDie(val die: Die) : Credit()
    data class CredAddToTotal(val amount: Int) : Credit()
    data class CredAdjustDie(val value: Int) : Credit()
    data object CredSetToMax : Credit()
    data object CredRerollDie : Credit()
    data class CredReduceCost(val type: FlourishType, val amount: Int): Credit()

}
