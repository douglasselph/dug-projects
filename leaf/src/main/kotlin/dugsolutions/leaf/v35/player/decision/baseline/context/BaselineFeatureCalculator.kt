package dugsolutions.leaf.v35.player.decision.baseline.context

import dugsolutions.leaf.v35.player.decision.baseline.common.DevelopmentTargetConfig
import dugsolutions.leaf.v35.player.decision.baseline.common.DevelopmentTargetHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.GraftTopologyEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.common.ResourceReserveHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.ResourceReservePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.RowNeedHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.RowNeedWeights
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Computes the recurring neutral features once for a Human Baseline decision.
 * Card/action-specific scorers can then add their own contextual logic without
 * each reimplementing development, reserve, topology, and row calculations.
 */
class BaselineFeatureCalculator(
    private val developmentConfig: DevelopmentTargetConfig = DevelopmentTargetConfig(),
    private val reservePolicy: ResourceReservePolicy = ResourceReservePolicy(),
    private val rowNeedWeights: RowNeedWeights = RowNeedWeights()
) {
    fun calculate(context: DecisionContext): BaselineFeatures =
        BaselineFeatures(
            development = DevelopmentTargetHeuristics.assess(
                context = context,
                config = developmentConfig
            ),
            openGrowthSlots = GraftTopologyEvaluator.openGrowthSlots(
                context.self.board.creature
            ),
            reserveStatus = ResourceReserveHeuristics.statuses(
                context = context,
                policy = reservePolicy
            ),
            rowNeed = RowNeedHeuristics.calculateAll(
                context = context,
                weights = rowNeedWeights
            )
        )
}
