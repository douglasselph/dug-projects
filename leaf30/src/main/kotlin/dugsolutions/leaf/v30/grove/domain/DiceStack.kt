package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.random.die.DieSides

class DiceStack(
    val sides: DieSides,
    initialCount: Int = 0
) {
    var count: Int = 0
        private set

    init {
        setCount(initialCount)
    }

    fun setCount(value: Int) {
        require(value >= 0) { "DiceStack count cannot be negative: $value" }
        count = value
    }

    fun add(amount: Int = 1) {
        require(amount >= 0) { "Cannot add a negative amount: $amount" }
        count += amount
    }

    fun remove(amount: Int = 1): Boolean {
        require(amount >= 0) { "Cannot remove a negative amount: $amount" }
        if (amount > count) return false
        count -= amount
        return true
    }
}
