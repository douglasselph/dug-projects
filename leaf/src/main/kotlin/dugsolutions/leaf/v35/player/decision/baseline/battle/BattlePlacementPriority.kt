package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.roundToInt

/**
 * Scores one legal post-roll placement from its actual immediate Battle Swing.
 *
 * The caller supplies the rolled value and one row from the current placement
 * request. Reusing [BattleDiePlacementAnalyzer] keeps post-roll placement aligned
 * with First Main's expected-placement analysis while preserving the information
 * boundary: this path is ACTUAL and never substitutes die expectation for the known
 * result.
 */
class BattlePlacementPriority(
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val diePlacementAnalyzer: BattleDiePlacementAnalyzer =
        BattleDiePlacementAnalyzer(policy)
) {
    operator fun invoke(
        context: DecisionContext,
        row: StrikeRow,
        dieValue: Int
    ): PriorityScore {
        val analysis = checkNotNull(
            diePlacementAnalyzer(
                context = context,
                dieValue = dieValue.toDouble(),
                mode = BattleAnalysisMode.ACTUAL,
                legalRows = listOf(row)
            ).singleOrNull()
        ) {
            "Current Battle context cannot project legal die placement in $row"
        }

        // Actual rolls, board totals, and policy transition scales are integral.
        // Keeping PriorityScore integral therefore preserves exact tactical ties for
        // BaselineScoreEngine/StrategyRandomizer rather than introducing a rounding tie.
        val tacticalValue = analysis.tacticalValue
        val score = tacticalValue.roundToInt()
        check(tacticalValue == score.toDouble()) {
            "Actual Battle placement produced non-integral tactical value $tacticalValue"
        }
        return PriorityScore(base = score)
    }
}
