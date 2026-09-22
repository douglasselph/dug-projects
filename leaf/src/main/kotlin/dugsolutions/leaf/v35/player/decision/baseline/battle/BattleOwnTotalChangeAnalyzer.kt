package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.abs

/**
 * Projects signed changes to the actor's total in one or more Strike Rows.
 *
 * The helper is intentionally mechanical and generic. Callers retain their own
 * realization type and decide whether a resulting action is worth spending.
 */
class BattleOwnTotalChangeAnalyzer(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val actionAnalyzer: BattleActionAnalyzer = BattleActionAnalyzer(
        swingEvaluator = BattleSwingEvaluator(policy)
    )
) {
    operator fun <T> invoke(
        context: DecisionContext,
        realization: T,
        changesByRow: Map<StrikeRow, Double>,
        mode: BattleAnalysisMode
    ): BattleActionAnalysis<T>? {
        if (changesByRow.isEmpty()) return null
        val rowChanges = changesByRow.map { (row, change) ->
            val before = rowAssessor(context, row)
            if (!before.available) return null
            BattleActionRowChange(
                before = BattleActionRowState.from(before),
                after = projectedAfterOwnTotalChange(context, before, change)
            )
        }
        return actionAnalyzer(
            context = context,
            candidate = BattleActionRealization(
                realization = realization,
                mode = mode,
                rowChanges = rowChanges
            )
        )
    }

    operator fun <T> invoke(
        context: DecisionContext,
        realization: T,
        row: StrikeRow,
        change: Double,
        mode: BattleAnalysisMode
    ): BattleActionAnalysis<T>? =
        invoke(
            context = context,
            realization = realization,
            changesByRow = mapOf(row to change),
            mode = mode
        )

    private fun projectedAfterOwnTotalChange(
        context: DecisionContext,
        before: BattleRowAssessment,
        change: Double
    ): BattleActionRowState {
        val ownAfter = before.ownTotal + change
        val opponents = before.participatingOpponentTotals
            .mapValues { (_, total) -> total.toDouble() }
        val allTotals = opponents.values + ownAfter
        val high = allTotals.maxOrNull()
        val everyoneTied =
            allTotals.size > 1 &&
                high != null &&
                allTotals.all { total -> abs(total - high) <= EPSILON }
        val currentlyWinning =
            !everyoneTied && high != null && abs(ownAfter - high) <= EPSILON
        val scoreMargin = opponents.values.maxOrNull()?.let { ownAfter - it }

        val battle = requireNotNull(context.battle)
        val liveThreatTotal = battle.row(before.row).players
            .asSequence()
            .filterNot { it.withdrawn }
            .filterNot { it.playerId == context.self.id }
            .filterNot { battle.isDone(it.playerId) }
            .maxOfOrNull { it.total.toDouble() }
        val liveThreatMargin = liveThreatTotal?.let { ownAfter - it }
        val woundRisk =
            !currentlyWinning &&
                !everyoneTied &&
                high != null &&
                high - ownAfter >= WOUND_MARGIN - EPSILON
        val securedForNow =
            currentlyWinning &&
                (liveThreatTotal == null ||
                    requireNotNull(liveThreatMargin) >=
                    policy.battleSecuredLead(context) - EPSILON)

        return BattleActionRowState(
            row = before.row,
            available = true,
            currentlyWinning = currentlyWinning,
            woundRisk = woundRisk,
            securedForNow = securedForNow,
            scoreMargin = scoreMargin,
            liveThreatMargin = liveThreatMargin,
            ownTotal = ownAfter,
            opponentTotals = opponents
        )
    }

    private companion object {
        const val WOUND_MARGIN = 5.0
        const val EPSILON = 1e-9
    }
}
