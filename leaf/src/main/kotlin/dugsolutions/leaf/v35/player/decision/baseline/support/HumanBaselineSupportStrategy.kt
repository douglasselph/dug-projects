package dugsolutions.leaf.v35.player.decision.baseline.support

import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.support.ButterflyRollChoice
import dugsolutions.leaf.v35.player.decision.support.ChooseButterflyRollRequest
import dugsolutions.leaf.v35.player.decision.support.SupportStrategy

/** Score the two visible Butterfly outcomes through the common baseline pipeline. */
class HumanBaselineSupportStrategy(
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry()
) : SupportStrategy {
    override fun chooseButterflyRoll(request: ChooseButterflyRollRequest): ButterflyRollChoice =
        scoreEngine.chooseValue(
            context = request.context,
            candidates = listOf(
                DecisionCandidate(
                    choice = ButterflyRollChoice.ORIGINAL,
                    score = PriorityScore(request.originalValue * 5)
                ),
                DecisionCandidate(
                    choice = ButterflyRollChoice.REROLLED,
                    score = PriorityScore(request.rerolledValue * 5)
                )
            ),
            influenceRegistry = influenceRegistry
        )
}
