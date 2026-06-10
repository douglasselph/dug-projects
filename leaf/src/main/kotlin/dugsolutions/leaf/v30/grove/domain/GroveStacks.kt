package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.cards.domain.GameCard

class GroveStacks {
    private val stacks: MutableMap<GroveStackID, GroveStack> = mutableMapOf()

    fun add(card: GameCard, amount: Int = 1): GroveStack {
        require(amount >= 0) { "Cannot add a negative amount: $amount" }
        val stackId = GroveStackID.from(card.type, card.cost)
        val stack = stacks.getOrPut(stackId) { GroveStack(card) }
        stack.requireSameCard(card)
        stack.add(amount)
        return stack
    }

    fun remove(card: GameCard, amount: Int = 1): Boolean {
        require(amount >= 0) { "Cannot remove a negative amount: $amount" }
        val stackId = GroveStackID.from(card.type, card.cost)
        val stack = stacks[stackId] ?: return false
        stack.requireSameCard(card)
        return stack.remove(amount)
    }

    fun remove(stackId: GroveStackID, amount: Int = 1): Boolean {
        require(amount >= 0) { "Cannot remove a negative amount: $amount" }
        return stacks[stackId]?.remove(amount) ?: false
    }

    fun getStack(stackId: GroveStackID): GroveStack? {
        return stacks[stackId]
    }

    fun getCard(stackId: GroveStackID): GameCard? {
        return stacks[stackId]?.card
    }

    fun getCount(stackId: GroveStackID): Int {
        return stacks[stackId]?.count ?: 0
    }

    fun setCount(stackId: GroveStackID, count: Int) {
        val stack = stacks[stackId]
            ?: throw IllegalArgumentException("Cannot set count for empty grove stack: $stackId")
        stack.setCount(count)
    }
}
