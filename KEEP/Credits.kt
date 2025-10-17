package dugsolutions.leaf.game.acquire.domain

import dugsolutions.leaf.random.die.Die
import kotlin.math.max

data class Credits(
    val list: List<Credit> = listOf()
) {

    val isNotEmpty: Boolean
        get() = list.isNotEmpty()

    val size: Int
        get() = list.size

    val pipTotal: Int
        get() {
            var total = 0
            for (ele in list) {
                total += when (ele) {
                    is Credit.CredDie -> ele.die.value
                    is Credit.CredAddToTotal -> ele.amount
                    else -> 0
                }
            }
            return total
        }

    val addToTotal: Int
        get() {
            var total = 0
            for (ele in list) {
                total += when (ele) {
                    is Credit.CredAddToTotal -> ele.amount
                    else -> 0
                }
            }
            return total
        }

    val dieList: List<Die>
        get() {
            val result = mutableListOf<Die>()
            for (ele in list) {
                when (ele) {
                    is Credit.CredDie -> result.add(ele.die)
                    else -> {}
                }
            }
            return result
        }

    fun contains(credit: Credit): Boolean {
        return list.contains(credit)
    }
}
