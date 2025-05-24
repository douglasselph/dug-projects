package dugsolutions.leaf.game.acquire.domain

import dugsolutions.leaf.components.die.Die
import kotlin.math.max

data class Credits(
    val list: MutableList<Credit> = mutableListOf()
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
                    is Credit.CredAdjustDie -> max(0, ele.value)
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

    val adjustList: List<Int>
        get() {
            val result = mutableListOf<Int>()
            for (ele in list) {
                when (ele) {
                    is Credit.CredAdjustDie -> result.add(ele.value)
                    else -> {}
                }
            }
            return result
        }

    val numSetToMax: Int
        get() {
            var result = 0
            for (ele in list) {
                when (ele) {
                    is Credit.CredSetToMax -> result++
                    else -> {}
                }
            }
            return result
        }

    fun contains(credit: Credit): Boolean {
        return list.contains(credit)
    }
}
