package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId

/** Research-only ruleset identity. Neither value changes canonical scheduling. */
enum class BattlePacingRuleset { V37_03, THREE_ENGAGEMENTS }

/** Frozen, independent of any later die reroll, removal or move. */
data class EngagementOrder(
    val row: StrikeRow,
    val starter: PlayerId,
    val clockwiseActors: List<PlayerId>,
    val highestShowingByPlayer: Map<PlayerId, Int>,
    val columnByPlayer: Map<PlayerId, Int>
) {
    /** One event record per action opportunity; a decision/action is NOT performed here. */
    fun traceLines(): List<String> = clockwiseActors.mapIndexed { index, id ->
        "ENGAGEMENT_ORDER,row=${row.name},position=${index + 1},player=${id.value}," +
            "starter=${starter.value},column=${columnByPlayer.getValue(id)}," +
            "highestShowing=${highestShowingByPlayer.getValue(id)}"
    }
}

/**
 * Candidate B's only new scheduling calculation, pure and RNG-free.
 *
 * [physicalSeatsClockwise] is Game.players order, NOT Grid column order.
 * Highest die means maximum currently showing in this row alone; empty = 0.
 * Equal highest values are resolved by the leftmost Grid column; second-highest
 * dice are deliberately ignored. Recompute only at the NEXT Engagement start.
 */
object EngagementOrderSelector {
    fun select(
        battle: BattleState,
        physicalSeatsClockwise: List<PlayerId>,
        row: StrikeRow
    ): EngagementOrder {
        val columns = battle.grid.playerIdsInGridOrder
        require(physicalSeatsClockwise.size == columns.size &&
            physicalSeatsClockwise.toSet() == columns.toSet()) {
            "Physical seats must be a permutation of the Battle participants"
        }
        require(!battle.grid.isRowClosed(row)) { "Cannot engage closed Strike $row" }

        val highest = columns.associateWith { id ->
            battle.grid.square(id, row).dice.maxOfOrNull { it.value } ?: 0
        }
        val column = columns.withIndex().associate { (index, id) -> id to index }
        val starter = columns.maxWithOrNull(
            compareBy<PlayerId> { highest.getValue(it) }
                .thenBy { -column.getValue(it) }
        ) ?: error("Battle has no players")
        val start = physicalSeatsClockwise.indexOf(starter)
        val order = physicalSeatsClockwise.drop(start) + physicalSeatsClockwise.take(start)
        return EngagementOrder(
            row = row,
            starter = starter,
            clockwiseActors = order.toList(),
            highestShowingByPlayer = highest.toMap(),
            columnByPlayer = column.toMap()
        )
    }
}
