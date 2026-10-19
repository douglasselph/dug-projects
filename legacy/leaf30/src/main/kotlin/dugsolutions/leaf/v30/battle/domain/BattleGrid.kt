package dugsolutions.leaf.v30.battle.domain

import dugsolutions.leaf.v30.common.Critter

class BattleGrid(
    playerIds: List<Int>
) {

    companion object {
        const val NUM_COLUMNS = 4
        const val NUM_STRIKE_ROWS = 3
    }

    private val columns = playerIds.map { BattleColumn(it) }

    init {
        require(playerIds.size == NUM_COLUMNS) { "Battle grid requires exactly $NUM_COLUMNS player columns: ${playerIds.size}" }
        require(playerIds.distinct().size == playerIds.size) { "Battle grid player ids must be unique: $playerIds" }
    }

    // Player ids are returned left-to-right by battle grid column order.
    val playerIdsInGridOrder: List<Int>
        get() = columns.map { it.playerId }

    fun getColumn(playerId: Int): BattleColumn {
        return columns.firstOrNull { it.playerId == playerId }
            ?: throw IllegalArgumentException("No battle column for player id: $playerId")
    }

    fun getSquare(
        playerId: Int,
        row: BattleStrikeRow
    ): BattleSquare {
        return getColumn(playerId).get(row)
    }

    fun add(
        playerId: Int,
        row: BattleStrikeRow,
        item: BattleItem
    ): BattleGrid {
        getColumn(playerId).add(row, item)
        return this
    }

    fun remove(
        playerId: Int,
        row: BattleStrikeRow,
        item: BattleItem
    ): Boolean {
        return getColumn(playerId).remove(row, item)
    }

    fun clear() {
        columns.forEach { it.clear() }
    }

    fun replaceCritter(
        playerId: Int,
        from: Critter,
        to: Critter
    ): Int {
        return getColumn(playerId).replaceCritter(from, to)
    }

    fun drainCritters(): List<Critter> {
        return columns.flatMap { it.drainCritters() }
    }

    fun snapshot(): BattleGridSnapshot {
        return BattleGridSnapshot(columns.map { it.snapshot() })
    }

}

data class BattleGridSnapshot(
    val columns: List<BattleColumnSnapshot>
) {
    // Player ids are returned left-to-right by battle grid column order.
    val playerIdsInGridOrder: List<Int>
        get() = columns.map { it.playerId }
}
