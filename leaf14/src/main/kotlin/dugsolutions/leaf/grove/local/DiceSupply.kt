package dugsolutions.leaf.grove.local

import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.DieSides

class DiceSupply(initialSupply: Map<Int, Int> = emptyMap()) {
    private val _supply = initialSupply.toMutableMap()

    companion object {
        val VALID_DICE_SIDES = setOf(4, 6, 8, 10, 12, 20)
        const val DEFAULT_QUANTITY = 4

        fun createDefault(): DiceSupply =
            DiceSupply(VALID_DICE_SIDES.associateWith { DEFAULT_QUANTITY })

        fun empty(): DiceSupply =
            DiceSupply(VALID_DICE_SIDES.associateWith { 0 })
    }

    val allDice: Dice
        get() {
            val dice = Dice()
            return dice
        }

    fun getQuantity(sides: Int): Int = _supply[sides] ?: 0

    fun getAvailableSides(): List<Int> = _supply.filterValues { it > 0 }.keys.sorted()

    fun removeDie(sides: Int): Boolean {
        if (!VALID_DICE_SIDES.contains(sides)) return false
        val currentQuantity = getQuantity(sides)
        if (currentQuantity <= 0) return false
        
        _supply[sides] = currentQuantity - 1
        return true
    }

    fun hasDie(sides: Int): Boolean {
        if (!VALID_DICE_SIDES.contains(sides)) return false
        val currentQuantity = getQuantity(sides)
        return currentQuantity >= 1
    }

    fun addDie(sides: Int, count: Int = 1): Boolean {
        if (!VALID_DICE_SIDES.contains(sides)) return false
        val currentQuantity = getQuantity(sides)
        _supply[sides] = currentQuantity + count
        return true
    }

    fun total(): Int = _supply.values.sum()

    fun getAffordableSides(availablePips: Int): List<Int> =
        getAvailableSides().filter { it <= availablePips }.sorted()

    fun clear() {
        _supply.clear()
    }

    fun add(count: Int, die: Int) {
        if (!VALID_DICE_SIDES.contains(die)) return
        val currentQuantity = getQuantity(die)
        _supply[die] = currentQuantity + count
    }

    fun addMany(sides: DieSides, count: Int) {
        _supply[sides.value] = count
    }
} 
