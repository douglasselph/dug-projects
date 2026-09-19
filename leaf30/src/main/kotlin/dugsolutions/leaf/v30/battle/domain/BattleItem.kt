package dugsolutions.leaf.v30.battle.domain

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieValue

sealed class BattleItem {
    data object BulwarkToken : BattleItem()
    data class DieItem(val die: Die) : BattleItem()
    data class CritterItem(val critter: Critter) : BattleItem()

    val countsTowardSquareLimit: Boolean
        get() {
            return when (this) {
                BulwarkToken -> false
                is DieItem -> true
                is CritterItem -> true
            }
        }

    val total: Int
        get() {
            return when (this) {
                BulwarkToken -> 0
                is DieItem -> die.value
                is CritterItem -> critter.value
            }
        }

    fun snapshot(): BattleItemSnapshot {
        return when (this) {
            BulwarkToken -> BattleItemSnapshot.BulwarkToken
            is DieItem -> BattleItemSnapshot.DieItem(die.copy)
            is CritterItem -> BattleItemSnapshot.CritterItem(critter)
        }
    }
}

sealed class BattleItemSnapshot {
    data object BulwarkToken : BattleItemSnapshot()
    data class DieItem(val die: DieValue) : BattleItemSnapshot()
    data class CritterItem(val critter: Critter) : BattleItemSnapshot()

    val total: Int
        get() {
            return when (this) {
                BulwarkToken -> 0
                is DieItem -> die.value
                is CritterItem -> critter.value
            }
        }
}
