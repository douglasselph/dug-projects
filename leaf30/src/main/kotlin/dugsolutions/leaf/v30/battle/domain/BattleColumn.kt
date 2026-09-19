package dugsolutions.leaf.v30.battle.domain

import dugsolutions.leaf.v30.common.Critter

class BattleColumn(
    val playerId: Int
) {
    private val squares = BattleStrikeRow.entries.associateWith { BattleSquare() }.toMutableMap()

    fun get(row: BattleStrikeRow): BattleSquare {
        return squares.getValue(row)
    }

    fun add(
        row: BattleStrikeRow,
        item: BattleItem
    ): BattleColumn {
        get(row).add(item)
        return this
    }

    fun remove(
        row: BattleStrikeRow,
        item: BattleItem
    ): Boolean {
        return get(row).remove(item)
    }

    fun clear() {
        squares.values.forEach { it.clear() }
    }

    fun replaceCritter(
        from: Critter,
        to: Critter
    ): Int {
        return squares.values.sumOf { it.replaceCritter(from, to) }
    }

    fun drainCritters(): List<Critter> {
        return squares.values.flatMap { it.drainCritters() }
    }

    fun snapshot(): BattleColumnSnapshot {
        return BattleColumnSnapshot(
            playerId = playerId,
            squares = BattleStrikeRow.entries.associateWith { row -> get(row).snapshot() }
        )
    }
}

data class BattleColumnSnapshot(
    val playerId: Int,
    val squares: Map<BattleStrikeRow, BattleSquareSnapshot>
)
