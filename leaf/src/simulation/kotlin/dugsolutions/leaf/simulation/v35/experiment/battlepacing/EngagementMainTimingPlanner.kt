package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleAnalysisMode
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleDiePlacementAnalyzer
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * An auditable timing decision, separate from the v37 choice of WHICH Main
 * action to execute. Scores are expected-Draw row-impact projections, not
 * a claim that Plant / Round-effect outcomes have been simulated.
 */
internal data class EngagementMainTimingDecision(
    val row: StrikeRow,
    val mainsRemaining: Int,
    val openRows: List<StrikeRow>,
    val projectedDrawScores: Map<StrikeRow, Double>,
    val plannedRows: List<StrikeRow>,
    val spendNow: Boolean,
    val reason: Reason
) {
    enum class Reason { ROW_UNAVAILABLE, MUST_SPEND_TO_USE_BOTH, EXPECTED_DRAW_PROJECTION, NO_DRAW_PROJECTION }

    fun trace(playerId: Int): String = buildString {
        append("ENGAGEMENT_MAIN_PLAN,player=$playerId,row=$row,mainsRemaining=$mainsRemaining")
        append(",open=${openRows.joinToString("|")},planned=${plannedRows.joinToString("|")}")
        append(",spend=$spendNow,reason=$reason")
        append(",drawScores=${projectedDrawScores.entries.joinToString("|") { "${it.key}:${it.value}" }}")
    }
}

/**
 * Checkpoint P1: pin down the two-token / three-Engagement scheduling contract.
 * The actual v37 Main chooser and target-choice machinery are not replaced.
 *
 * IMPORTANT: The expected Draw forecast below is still only a *temporary*
 * row-allocation proxy. A later task must forecast the actual Plant / Round /
 * Draw candidates with the canonical consequence-based scorers, across ALL
 * open rows, before using these cohorts for competitive balance claims.
 */
internal class EngagementMainTimingPlanner(
    private val placement: BattleDiePlacementAnalyzer = BattleDiePlacementAnalyzer()
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
