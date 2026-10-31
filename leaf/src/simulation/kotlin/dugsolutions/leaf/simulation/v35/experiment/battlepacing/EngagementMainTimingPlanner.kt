package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleAnalysisMode
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleDiePlacementAnalyzer
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.round.domain.RoundCard

/**
 * An auditable timing decision, separate from the v37 choice of WHICH Main
 * action to execute. Task 2 uses action-specific v37 tactical score sensitivity
 * across unresolved rows; these heuristic points are NOT objective VP.
 */
internal data class EngagementMainTimingDecision(
    val row: StrikeRow,
    val mainsRemaining: Int,
    val openRows: List<StrikeRow>,
    /** Kept for Task 1 source compatibility; Task 2 fills it with action-specific row scores. */
    val projectedDrawScores: Map<StrikeRow, Double>,
    val plannedRows: List<StrikeRow>,
    val spendNow: Boolean,
    val reason: Reason,
    val actionPreviews: List<BaselineMainActionForecast> = emptyList()
) {
    val projectedActionRowScores: Map<StrikeRow, Double> get() = projectedDrawScores

    enum class Reason { ROW_UNAVAILABLE, MUST_SPEND_TO_USE_BOTH, EXPECTED_DRAW_PROJECTION, NO_DRAW_PROJECTION, ACTION_SPECIFIC_PROJECTION, UNATTRIBUTED_ACTIONS }

    fun trace(playerId: Int): String = buildString {
        append("ENGAGEMENT_MAIN_PLAN,player=$playerId,row=$row,mainsRemaining=$mainsRemaining")
        append(",open=${openRows.joinToString("|")},planned=${plannedRows.joinToString("|")}")
        append(",spend=$spendNow,reason=$reason")
        append(",rowScores=${projectedDrawScores.entries.joinToString("|") { "${it.key}:${it.value}" }}")
        append(",actionPreviews=${actionPreviews.joinToString("|") { it.trace() }}")
    }
}

/**
 * Task 2: forecast real v37 Main candidates across open rows without consuming
 * mechanical or strategy RNG. Preserve the Task 1 pure scheduling seam/tests.
 */
internal class EngagementMainTimingPlanner(
    private val placement: BattleDiePlacementAnalyzer = BattleDiePlacementAnalyzer(),
    private val forecaster: BaselineMainActionForecaster = BaselineMainActionForecaster()
) {
    fun shouldUseMain(context: DecisionContext, row: StrikeRow, mainsRemaining: Int): Boolean =
        plan(context, row, mainsRemaining).spendNow

    fun plan(context: DecisionContext, row: StrikeRow, mainsRemaining: Int): EngagementMainTimingDecision {
        require(mainsRemaining in 1..2)
        val open = StrikeRow.entries.filter { candidate ->
            candidate.ordinal >= row.ordinal &&
                context.battle?.rows?.firstOrNull { it.row == candidate }?.closed == false
        }
        // No hypothetical roll or mechanical RNG consumption in the forecast.
        val nextSides = context.self.board.supply.minOfOrNull { it.sides }
            ?: context.self.board.discard.minOfOrNull { it.sides }
        val values = if (nextSides != null && row in open) {
            placement(context, (nextSides + 1) / 2.0, BattleAnalysisMode.EXPECTED, open)
                .associate { it.realization to it.tacticalValue }
        } else emptyMap()
        return planFromScores(row, mainsRemaining, open, values)
    }

    /**
     * Production research entry point. Inspect *every legal* Draw, Plant and
     * Round-effect candidate with the same v37 First/Final Main scoring as the
     * actual chooser, but on immutable one-open-row projections. This schedules
     * the opportunity; v37 still chooses the actual action/targets when taken.
     */
    fun plan(
        context: DecisionContext,
        row: StrikeRow,
        mainsRemaining: Int,
        roundCard: RoundCard,
        legalMains: List<BattleMainAction>
    ): EngagementMainTimingDecision {
        require(mainsRemaining in 1..2)
        val open = StrikeRow.entries.filter { candidate ->
            candidate.ordinal >= row.ordinal &&
                context.battle?.rows?.firstOrNull { it.row == candidate }?.closed == false
        }
        if (row !in open) return planFromScores(row, mainsRemaining, open, emptyMap())
        val previews = forecaster.forecast(context, roundCard, legalMains, mainsRemaining, open)
        val rowScores = open.associateWith { strike ->
            previews.maxOfOrNull { it.rowOpportunity[strike] ?: 0.0 } ?: 0.0
        }
        val attributed = rowScores.values.any { it > 0.0 }
        // A non-row action (e.g. Bloom VP) has no target-row forecast; do
        // not make up one. Without modeled tactical row benefit, use the
        // earliest Engagement to preserve later options, unless forced.
        val rankedPlan = planFromScores(row, mainsRemaining, open,
            if (attributed) rowScores else emptyMap())
        return rankedPlan.copy(
            projectedDrawScores = rowScores,
            actionPreviews = previews,
            reason = when {
                rankedPlan.reason == EngagementMainTimingDecision.Reason.MUST_SPEND_TO_USE_BOTH ->
                    EngagementMainTimingDecision.Reason.MUST_SPEND_TO_USE_BOTH
                attributed -> EngagementMainTimingDecision.Reason.ACTION_SPECIFIC_PROJECTION
                else -> EngagementMainTimingDecision.Reason.UNATTRIBUTED_ACTIONS
            }
        )
    }

    companion object {
        /** Deterministic and directly testable; no strategy or mechanical RNG. */
        internal fun planFromScores(
            row: StrikeRow,
            mainsRemaining: Int,
            openRows: List<StrikeRow>,
            scores: Map<StrikeRow, Double>
        ): EngagementMainTimingDecision {
            require(mainsRemaining in 1..2)
            val open = StrikeRow.entries.filter { it in openRows && it.ordinal >= row.ordinal }
            if (row !in open) return EngagementMainTimingDecision(
                row, mainsRemaining, open, scores, emptyList(), false,
                EngagementMainTimingDecision.Reason.ROW_UNAVAILABLE
            )
            // With two normal Mains and two remaining Engagements, passing is
            // not allowed to accidentally strand a token; similarly at Low.
            val forced = open.size <= mainsRemaining
            val projected = scores.isNotEmpty()
            val ranked = if (projected) {
                open.sortedWith(compareByDescending<StrikeRow> { scores[it] ?: Double.NEGATIVE_INFINITY }
                    .thenBy { it.ordinal })
            } else open
            val planned = ranked.take(mainsRemaining)
            val reason = when {
                forced -> EngagementMainTimingDecision.Reason.MUST_SPEND_TO_USE_BOTH
                projected -> EngagementMainTimingDecision.Reason.EXPECTED_DRAW_PROJECTION
                else -> EngagementMainTimingDecision.Reason.NO_DRAW_PROJECTION
            }
            return EngagementMainTimingDecision(
                row, mainsRemaining, open, scores, planned,
                forced || !projected || row in planned, reason
            )
        }

        /** Backward-compatible pure entry point used by existing timing tests. */
        internal fun shouldSpendInRow(
            row: StrikeRow,
            mainsRemaining: Int,
            openRows: List<StrikeRow>,
            scores: Map<StrikeRow, Double>
        ): Boolean = planFromScores(row, mainsRemaining, openRows, scores).spendNow
    }
}
