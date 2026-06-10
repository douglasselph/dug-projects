package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.cards.domain.GameCard

class GroveCardStacks {
    private val stacks: MutableMap<GroveCardStackID, GroveCardStack> = mutableMapOf()

    fun add(card: GameCard, amount: Int = 1): GroveCardStack {
        require(amount >= 0) { "Cannot add a negative amount: $amount" }
        val stackId = GroveCardStackID.from(card.type, card.cost)
        val stack = stacks.getOrPut(stackId) { GroveCardStack(card) }
        stack.requireSameCard(card)
        stack.add(amount)
        return stack
    }

    fun reset(card: GameCard, amount: Int): GroveCardStack {
        require(amount >= 0) { "Cannot reset to a negative amount: $amount" }
        val stackId = GroveCardStackID.from(card.type, card.cost)
        val stack = GroveCardStack(card, amount)
        stacks[stackId] = stack
        return stack
    }

    fun remove(card: GameCard, amount: Int = 1): Boolean {
        require(amount >= 0) { "Cannot remove a negative amount: $amount" }
        val stackId = GroveCardStackID.from(card.type, card.cost)
        val stack = stacks[stackId] ?: return false
        stack.requireSameCard(card)
        return stack.remove(amount)
    }

    fun remove(stackId: GroveCardStackID, amount: Int = 1): Boolean {
        require(amount >= 0) { "Cannot remove a negative amount: $amount" }
        return stacks[stackId]?.remove(amount) ?: false
    }

    fun getStack(stackId: GroveCardStackID): GroveCardStack? {
        return stacks[stackId]
    }

    fun getCard(stackId: GroveCardStackID): GameCard? {
        return stacks[stackId]?.card
    }

    fun getCount(stackId: GroveCardStackID): Int {
        return stacks[stackId]?.count ?: 0
    }

    fun setCount(stackId: GroveCardStackID, count: Int) {
        val stack = stacks[stackId]
            ?: throw IllegalArgumentException("Cannot set count for empty grove stack: $stackId")
        stack.setCount(count)
    }
}
