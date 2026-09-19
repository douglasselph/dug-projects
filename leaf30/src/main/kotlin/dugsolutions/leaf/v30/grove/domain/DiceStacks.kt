package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.random.die.DieSides

class DiceStacks {
    private val stacks: MutableMap<DieSides, DiceStack> = DieSides.entries
        .associateWith { DiceStack(it) }
        .toMutableMap()

    fun add(sides: DieSides, amount: Int = 1): DiceStack {
        require(amount >= 0) { "Cannot add a negative amount: $amount" }
        val stack = requireStack(sides)
        stack.add(amount)
        return stack
    }

    fun remove(sides: DieSides, amount: Int = 1): Boolean {
        require(amount >= 0) { "Cannot remove a negative amount: $amount" }
        return requireStack(sides).remove(amount)
    }

    fun getStack(sides: DieSides): DiceStack {
        return requireStack(sides)
    }

    fun getCount(sides: DieSides): Int {
        return requireStack(sides).count
    }

    fun setCount(sides: DieSides, count: Int) {
        requireStack(sides).setCount(count)
    }

    fun asList(): List<DiceStack> {
        return DieSides.entries.map { requireStack(it) }
    }

    private fun requireStack(sides: DieSides): DiceStack {
        return stacks[sides]
            ?: throw IllegalArgumentException("No DiceStack for sides=$sides")
    }
}
