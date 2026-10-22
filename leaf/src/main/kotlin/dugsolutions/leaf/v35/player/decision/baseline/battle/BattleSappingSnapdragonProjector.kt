package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy

/**
 * Complete deterministic Sapping projection, with the engine's exact ordering:
 * raise the first die (+2, capped), reduce each opponent die in the anchor row
 * (floor 1), and raise the anchor by the *actual* amount removed (capped).
 * S1 uses the first die as anchor. S2 requires a different placed die.
 *
 * The second-choice callback for S2 occurs AFTER the real +2 raise; callers
 * must set [initialRaisePending] to false to avoid counting it twice.
 */
class BattleSappingSnapdragonProjector(
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val analyzer: BattleActionAnalyzer = BattleActionAnalyzer(BattleSwingEvaluator(policy))
) {
    fun ownIndices(context: DecisionContext): List<Int> = context.battle?.rows.orEmpty()
        .flatMap { it.forPlayer(context.self.id)?.dice.orEmpty() }
        .map { it.handIndex }

    fun <T> project(
        context: DecisionContext,
        effect: GameEffect,
        raisedIndex: Int,
        anchorIndex: Int,
        realization: T,
        initialRaisePending: Boolean = true
    ): BattleActionAnalysis<T>? {
        if (effect != GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW &&
            effect != GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW) return null
        if (effect == GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW && raisedIndex != anchorIndex) return null
        if (effect == GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW && raisedIndex == anchorIndex) return null
        val battle = context.battle ?: return null
        val actorId = context.self.id
        val raisedRow = battle.rows.firstOrNull { it.forPlayer(actorId)?.dice?.any { d -> d.handIndex == raisedIndex } == true } ?: return null
        val anchorRow = battle.rows.firstOrNull { it.forPlayer(actorId)?.dice?.any { d -> d.handIndex == anchorIndex } == true } ?: return null
        if (raisedRow.closed || anchorRow.closed || raisedRow.forPlayer(actorId)?.withdrawn != false || anchorRow.forPlayer(actorId)?.withdrawn != false) return null
        val first = raisedRow.forPlayer(actorId)?.dice?.firstOrNull { it.handIndex == raisedIndex } ?: return null
        val initiallyRaised = if (initialRaisePending) (first.value + 2).coerceAtMost(first.sides) else first.value
        val afterInitial = battle.rows.map { row ->
            if (row.row != raisedRow.row || initiallyRaised == first.value) row
            else row.copy(players = row.players.map { player ->
                if (player.playerId != actorId) player
                else player.changed(raisedIndex, initiallyRaised)
            })
        }
        val afterInitialAnchor = afterInitial.first { it.row == anchorRow.row }
        val anchor = afterInitialAnchor.forPlayer(actorId)?.dice?.firstOrNull { it.handIndex == anchorIndex } ?: return null
        var reduced = 0
        val afterDrain = afterInitialAnchor.copy(players = afterInitialAnchor.players.map { player ->
            if (player.playerId == actorId || player.withdrawn) player
            else {
                val updated = player.dice.map { die ->
                    val newValue = (die.value - 2).coerceAtLeast(1)
                    reduced += die.value - newValue
                    die.copy(value = newValue)
                }
                val delta = updated.sumOf { it.value } - player.dice.sumOf { it.value }
                player.copy(dice = updated, dieTotal = player.dieTotal + delta, total = player.total + delta)
            }
        })
        val newAnchorValue = (anchor.value + reduced).coerceAtMost(anchor.sides)
        val finalAnchorRow = afterDrain.copy(players = afterDrain.players.map { player ->
            if (player.playerId == actorId) player.changed(anchorIndex, newAnchorValue) else player
        })
        val projected = battle.copy(rows = afterInitial.map { row ->
            if (row.row == anchorRow.row) finalAnchorRow else row
        })
        val affected = listOf(raisedRow.row, anchorRow.row).distinct()
        val changes = affected.map { row ->
            val before = rowAssessor(context, row)
            val after = rowAssessor(context, row, projected)
            if (!before.available || !after.available) return null
            BattleActionRowChange.from(before, after)
        }
        return analyzer(context, BattleActionRealization(realization, BattleAnalysisMode.DETERMINISTIC, changes))
    }

    fun <T> bestForFirst(
        context: DecisionContext,
        effect: GameEffect,
        firstIndex: Int,
        realization: T
    ): BattleActionAnalysis<T>? {
        val anchors = if (effect == GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW)
            listOf(firstIndex)
        else ownIndices(context).filter { it != firstIndex }
        return anchors.mapNotNull { anchor ->
            project(context, effect, firstIndex, anchor, realization)
        }.maxByOrNull { it.tacticalValue }
    }

    private fun BattlePlayerRowView.changed(index: Int, value: Int): BattlePlayerRowView {
        val before = dice.firstOrNull { it.handIndex == index } ?: return this
        val delta = value - before.value
        return copy(
            dice = dice.map { if (it.handIndex == index) it.copy(value = value) else it },
            dieTotal = dieTotal + delta,
            total = total + delta
        )
    }
}
