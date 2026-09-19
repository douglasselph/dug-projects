package dugsolutions.leaf.v30.battle.eval

import dugsolutions.leaf.v30.battle.domain.BattleGridSnapshot
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.battle.domain.Result
import dugsolutions.leaf.v30.battle.domain.StrikeRowResult

class BattleEvaluator {

    private companion object {
        const val WOUND_THRESHOLD = 5
    }

    operator fun invoke(
        snapshot: BattleGridSnapshot,
        resolved: Set<BattleStrikeRow> = emptySet()
    ): Result {
        return Result(
            rows = BattleStrikeRow.entries.associateWith { row ->
                if (row in resolved) {
                    StrikeRowResult(row, winners = emptyList(), wounded = emptyList())
                } else {
                    evaluateRow(snapshot, row)
                }
            }
        )
    }

    private fun evaluateRow(
        snapshot: BattleGridSnapshot,
        row: BattleStrikeRow
    ): StrikeRowResult {
        val scores = snapshot.columns
            .map { column -> column.playerId to column.squares.getValue(row) }
            .filterNot { (_, square) -> square.hasBulwarkToken }
            .map { (playerId, square) -> playerId to square.total }
        val highScore = scores.maxOfOrNull { it.second } ?: 0
        val allTie = scores.map { it.second }.distinct().size <= 1
        val winners = if (allTie) {
            emptyList()
        } else {
            scores.filter { it.second == highScore }.map { it.first }
        }
        val wounded = if (allTie) {
            emptyList()
        } else {
            scores.filter { (_, score) -> highScore - score >= WOUND_THRESHOLD }.map { it.first }
        }
        return StrikeRowResult(
            row = row,
            winners = winners,
            wounded = wounded
        )
    }

}
