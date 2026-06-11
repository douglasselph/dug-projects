package dugsolutions.leaf.v30.battle.domain

data class Result(
    val rows: Map<BattleStrikeRow, StrikeRowResult>
) {
    operator fun get(row: BattleStrikeRow): StrikeRowResult {
        return rows.getValue(row)
    }
}

data class StrikeRowResult(
    val row: BattleStrikeRow,
    val winners: List<Int>,
    val wounded: List<Int>
)
