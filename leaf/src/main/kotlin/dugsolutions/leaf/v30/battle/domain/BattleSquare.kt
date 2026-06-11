package dugsolutions.leaf.v30.battle.domain

class BattleSquare(
    items: List<BattleItem> = emptyList()
) {

    companion object {
        const val MAX_ITEMS = 3
    }

    private val items = items.toMutableList()

    init {
        require(countLimitedItems() <= MAX_ITEMS) { "Battle square can hold at most $MAX_ITEMS dice/critter items: ${countLimitedItems()}" }
    }

    val all: List<BattleItem>
        get() = items.toList()

    val size: Int
        get() = items.size

    val isFull: Boolean
        get() = countLimitedItems() >= MAX_ITEMS

    val isEmpty: Boolean
        get() = items.isEmpty()

    val total: Int
        get() = items.sumOf { it.total }

    fun canAdd(item: BattleItem): Boolean {
        return !item.countsTowardSquareLimit || !isFull
    }

    fun add(item: BattleItem): BattleSquare {
        require(canAdd(item)) { "Battle square can hold at most $MAX_ITEMS dice/critter items" }
        items.add(item)
        return this
    }

    fun remove(item: BattleItem): Boolean {
        return items.remove(item)
    }

    fun clear() {
        items.clear()
    }

    fun snapshot(): BattleSquareSnapshot {
        return BattleSquareSnapshot(items.map { it.snapshot() })
    }

    private fun countLimitedItems(): Int {
        return items.count { it.countsTowardSquareLimit }
    }

}

data class BattleSquareSnapshot(
    val items: List<BattleItemSnapshot>
) {
    val hasBulwarkToken: Boolean
        get() = items.any { it is BattleItemSnapshot.BulwarkToken }

    val total: Int
        get() = items.sumOf { it.total }
}
