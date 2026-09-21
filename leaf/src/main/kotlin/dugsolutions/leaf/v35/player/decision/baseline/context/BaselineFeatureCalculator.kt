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
 * Builds the shared snapshot of "how am I doing right now?" information used by
 * Human Baseline strategies when evaluating a decision.
 *
 * Many different decisions need the same background facts. For example:
 *
 * - Is the player's Plant Creature or dice pool behind its expected development target?
 * - How many legal growth positions remain on the Creature?
 * - Which resources should normally be preserved for later use?
 * - During Battle, how badly does the player need help in each Strike Row?
 *
 * Those calculations do not depend on a particular Plant card, Wisp, purchase,
 * or action. They describe the player's current situation in a general,
 * strategy-neutral way.
 *
 * This class calculates those common facts once and packages them into
 * [BaselineFeatures]. The individual Human Baseline scorers can then combine
 * those shared features with information specific to the choice being evaluated.
 *
 * For example, a Buy scorer can use the development information to decide
 * whether a Plant card or die is more desirable, while a Battle card scorer
 * can use Row Need to decide how valuable an effect would be in a particular
 * Strike Row.
 *
 * Keeping these calculations here avoids duplicating the same logic throughout
 * the individual decision strategies and helps ensure that all Human Baseline
 * decisions use the same definitions of development, reserves, topology, and
 * Battle need.
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
