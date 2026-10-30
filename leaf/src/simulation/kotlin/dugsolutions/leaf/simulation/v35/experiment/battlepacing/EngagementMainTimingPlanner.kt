package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleAnalysisMode
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleDiePlacementAnalyzer
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Experimental timing adaptation, not a replacement for the canonical v37 Main chooser.
 * A Main's identity and all later effect targets still come from the player's production
 * Human Baseline policies. This layer only decides WHICH Engagement may spend a token.
 *
 * For this pilot the row timing forecast is the approved B7 expected-Draw placement
 * analysis. This is a shared tactical proxy, NOT a complete action-specific preview
 * for Plants and Round effects. Do not use as an expert-play balance benchmark.
 */
internal class EngagementMainTimingPlanner(
    private val placement: BattleDiePlacementAnalyzer = BattleDiePlacementAnalyzer()
) {
    fun shouldUseMain(
        context: DecisionContext,
        row: StrikeRow,
        mainsRemaining: Int
    ): Boolean {
        require(mainsRemaining in 1..2)
        val open = StrikeRow.entries.filter { candidate ->
            candidate.ordinal >= row.ordinal &&
                context.battle?.rows?.firstOrNull { it.row == candidate }?.closed == false
        }
        if (row !in open) return false
        // Never lose the last opportunity through a heuristic deferral.
        if (open.size <= mainsRemaining) return true

        val nextSides = context.self.board.supply.minOfOrNull { it.sides }
            ?: context.self.board.discard.minOfOrNull { it.sides }
        val scoreByRow = if (nextSides != null) {
            placement(context, (nextSides + 1) / 2.0, BattleAnalysisMode.EXPECTED)
                .associate { it.realization to it.tacticalValue }
        } else emptyMap()

        // If no row can be projected, don't fabricate a future advantage.
        // This may still be an important Plant/Round-effect opportunity.
        if (scoreByRow.isEmpty()) return true
        return shouldSpendInRow(row, mainsRemaining, open, scoreByRow)
    }

    companion object {
        /** Pure scheduling rule; keeps the first two best projected rows, ties earlier. */
        internal fun shouldSpendInRow(
            row: StrikeRow,
            mainsRemaining: Int,
            openRows: List<StrikeRow>,
            scores: Map<StrikeRow, Double>
        ): Boolean {
            require(mainsRemaining in 1..2)
            if (row !in openRows) return false
            if (openRows.size <= mainsRemaining) return true
            val ranked = openRows.sortedWith(
                compareByDescending<StrikeRow> { scores[it] ?: Double.NEGATIVE_INFINITY }
                    .thenBy { it.ordinal }
            )
            return row in ranked.take(mainsRemaining)
        }
    }
}
