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
    // Player ids for the players who won this strike row.
    val winners: List<Int>,
    // Player ids for the players who were wounded on this strike row.
    val wounded: List<Int>
)
