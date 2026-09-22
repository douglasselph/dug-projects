package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCard
import kotlin.math.roundToInt

/**
 * Scores Step-4 First Main actions under the approved Human Baseline contract.
 *
 * Existing card/Round scoring remains the intrinsic value. B7 adds shared tactical
 * Battle analysis where the First-Main request already contains enough information
 * to project honestly: Draw uses the expected next die and its best current legal
 * expected placement. It does not roll mechanically and it does not pre-commit the
 * eventual post-roll row.
 *
 * Target-dependent Plant effects deliberately keep their existing intrinsic/context
 * score in this checkpoint. Their shared action/target projection belongs to B15,
 * where the actual Effect request legality and downstream target choice are aligned
 * rather than duplicated here.
 */
class BattleFirstMainPriority(
    private val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val diePlacementAnalyzer: BattleDiePlacementAnalyzer =
        BattleDiePlacementAnalyzer(policy)
) {
    operator fun invoke(
        context: DecisionContext,
        roundCard: RoundCard,
        action: BattleMainAction
    ): PriorityScore {
        val intrinsic = BattleMainPriority.intrinsicScore(
            context = context,
            roundCard = roundCard,
            action = action,
            cardScorers = cardScorers
        )
        return when (action) {
            BattleMainAction.Draw -> scoreExpectedDraw(context, intrinsic)
            is BattleMainAction.ActivatePlant,
            BattleMainAction.RoundEffect1,
            BattleMainAction.RoundEffect2 -> intrinsic
        }
    }

    private fun scoreExpectedDraw(
        context: DecisionContext,
        intrinsic: PriorityScore
    ): PriorityScore {
        val nextSides = context.self.board.supply.minOfOrNull { it.sides }
            ?: context.self.board.discard.minOfOrNull { it.sides }
            ?: return intrinsic
        val expected = DieValueHeuristics.expectedRoll(nextSides)
        val best = diePlacementAnalyzer(
            context = context,
            dieValue = expected,
            mode = BattleAnalysisMode.EXPECTED
        ).maxByOrNull { it.tacticalValue }
            ?: return intrinsic
        val adjustment = best.tacticalValue.roundToInt()
        if (adjustment == 0) return intrinsic

        val transition = best.swing.rowSwings.singleOrNull()?.transition
        val transitionText = transition?.takeIf { it != BattleTransition.NONE }
            ?.let { " ${it.name}" }
            .orEmpty()
        return intrinsic.adjusted(
            adjustment,
            "Expected D$nextSides best Battle placement ${best.realization}:$transitionText swing ${format(best.tacticalValue)}"
        )
    }

    private fun format(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
}
