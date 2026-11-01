package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.domain.BattleGridRowSnapshot
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId

/** Observational row-actor record. Never conflates an offered turn with a consequential action. */
internal data class RowActorEvidence(
    val row: StrikeRow,
    val lastActor: PlayerId?,
    val lastWindow: Int?,
    val precedingTotals: Map<PlayerId, Int>,
    val resultingTotals: Map<PlayerId, Int>,
    val rowChanges: Int,
    val actorChangedWinningStatus: Boolean,
    val beforeLeadMargin: Int?,
    val afterLeadMargin: Int?
)

/** A consequential row action changes actual score/withdrawal/dice/critters, even with no change of leader. */
internal fun rowActorEvidence(observed: BattlePacingObserved, row: StrikeRow): RowActorEvidence {
    fun snapshot(rows: List<BattleGridRowSnapshot>) = rows.firstOrNull { it.row == row }
    fun totals(s: BattleGridRowSnapshot?) = s?.squares?.filterNot { it.withdrawn }
        ?.associate { it.playerId to it.total } ?: emptyMap()
    fun signature(s: BattleGridRowSnapshot?) = s?.squares?.map { square ->
        listOf(square.playerId.value, square.withdrawn, square.dice.map { Triple(it.assetId, it.value, it.sides) },
            square.critters.map { it.critter to it.value })
    }
    fun winner(t: Map<PlayerId, Int>, id: PlayerId): Boolean {
        if (t.size < 2 || !t.containsKey(id) || t.values.toSet().size == 1) return false
        return t.getValue(id) == t.values.maxOrNull()
    }
    var last: BattlePacingWindowObserved? = null
    var before: Map<PlayerId, Int> = emptyMap()
    var after: Map<PlayerId, Int> = emptyMap()
    var changes = 0
    observed.windows.forEach { w ->
        if (w.actionCount > 0 && signature(snapshot(w.beforeRows)) != signature(snapshot(w.afterRows))) {
            last = w
            before = totals(snapshot(w.beforeRows))
            after = totals(snapshot(w.afterRows))
            changes++
        }
    }
    val id = last?.playerId
    fun margin(t: Map<PlayerId, Int>): Int? {
        val actor = id ?: return null
        val own = t[actor] ?: return null
        val other = t.filterKeys { it != actor }.values.maxOrNull() ?: return null
        return own - other
    }
    return RowActorEvidence(row, id, last?.index, before, after, changes,
        id != null && !winner(before, id) && winner(after, id), margin(before), margin(after))
}
