package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.cards.domain.GameCard

class GroveCardStack(
    val card: GameCard,
    initialCount: Int = 0
) {
    var count: Int = 0
        private set

    init {
        setCount(initialCount)
    }

    fun setCount(value: Int) {
        require(value >= 0) { "GroveStack count cannot be negative: $value" }
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

    fun requireSameCard(incoming: GameCard) {
        require(card.id == incoming.id) {
            "GroveStack for ${card.name} cannot hold ${incoming.name}"
        }
    }
}
