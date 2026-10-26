package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCard

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
    private val consequenceEvaluator: BattleConsequenceEvaluator =
        BattleConsequenceEvaluator(placementAnalyzer = BattleDiePlacementAnalyzer(policy))
) {
    operator fun invoke(
        context: DecisionContext,
        roundCard: RoundCard,
        action: BattleMainAction
    ): PriorityScore {
        val intrinsic = if (action == BattleMainAction.Draw) {
            BattleDrawUtility.score(context, consequenceEvaluator)
        } else BattleMainPriority.intrinsicScore(
            context = context,
            roundCard = roundCard,
            action = action,
            cardScorers = cardScorers
        )
        return when (action) {
            BattleMainAction.Draw -> intrinsic
            is BattleMainAction.ActivatePlant,
            BattleMainAction.RoundEffect1,
            BattleMainAction.RoundEffect2 -> intrinsic
        }
    }

}
