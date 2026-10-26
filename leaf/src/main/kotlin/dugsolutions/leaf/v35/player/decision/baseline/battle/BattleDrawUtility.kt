package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.roundToInt

/**
 * Battle-specific Draw value: one expected die face may be placed in the best
 * currently legal Strike row. The face value is NOT multiplied by the old
 * Cultivation-specific four-points-per-pip coefficient.
 *
 * Non-tactical benefit of gaining an extra Battle die is valued at one
 * decision point per expected pip. The existing Strike projector supplies
 * tactical win/tie/wound benefit once (including Strike VP, never twice).
 */
internal object BattleDrawUtility {
    private const val ORIGIN = BattleUtilityPolicy.ORIGIN

    fun score(
        context: DecisionContext,
        evaluator: BattleConsequenceEvaluator = BattleConsequenceEvaluator()
    ): PriorityScore {
        val sides = context.self.board.supply.minOfOrNull { it.sides }
            ?: context.self.board.discard.minOfOrNull { it.sides }
            ?: return PriorityScore(0).adjusted(0, "No drawable Battle die")
        val expected = (sides + 1) / 2.0
        val intrinsic = PriorityScore(ORIGIN).adjusted(
            expected.roundToInt(), "Battle D$sides expected die contribution (not Cultivation x4)"
        )
        val best = evaluator.placedDie(context, expected, BattleAnalysisMode.EXPECTED)
            .filter { it.legal }.maxByOrNull { it.tacticalPreference }
            ?: return intrinsic.adjusted(0, "No projectable legal Strike row")
        return intrinsic.adjusted(
            best.tacticalPreference.roundToInt(),
            "Projected Battle Strike consequence at ${best.target}; Strike VP included once"
        )
    }
}
