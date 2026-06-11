package dugsolutions.leaf.v30.battle.domain

class BattleSquare(
    items: List<BattleItem> = emptyList()
) {
    private val items = items.toMutableList()

    init {
        require(items.size <= MAX_ITEMS) { "Battle square can hold at most $MAX_ITEMS items: ${items.size}" }
    }

    val all: List<BattleItem>
        get() = items.toList()

    val size: Int
        get() = items.size

    val isFull: Boolean
        get() = size >= MAX_ITEMS

    val isEmpty: Boolean
        get() = items.isEmpty()

    fun add(item: BattleItem): BattleSquare {
        require(!isFull) { "Battle square can hold at most $MAX_ITEMS items" }
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

    companion object {
        const val MAX_ITEMS = 3
    }
}

data class BattleSquareSnapshot(
    val items: List<BattleItemSnapshot>
)
