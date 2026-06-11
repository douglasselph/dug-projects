package dugsolutions.leaf.v30.battle.domain

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieValue

sealed class BattleItem {
    data class DieItem(val die: Die) : BattleItem()
    data class CritterItem(val critter: Critter) : BattleItem()

    val total: Int
        get() {
            return when (this) {
                is DieItem -> die.value
                is CritterItem -> critter.value
            }
        }

    fun snapshot(): BattleItemSnapshot {
        return when (this) {
            is DieItem -> BattleItemSnapshot.DieItem(die.copy)
            is CritterItem -> BattleItemSnapshot.CritterItem(critter)
        }
    }
}

sealed class BattleItemSnapshot {
    data class DieItem(val die: DieValue) : BattleItemSnapshot()
    data class CritterItem(val critter: Critter) : BattleItemSnapshot()

    val total: Int
        get() {
            return when (this) {
                is DieItem -> die.value
                is CritterItem -> critter.value
            }
        }
}
