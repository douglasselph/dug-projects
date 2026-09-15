package dugsolutions.leaf.v35.player.decision.baseline.support

import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.support.ButterflyRollChoice
import dugsolutions.leaf.v35.player.decision.support.ChooseButterflyRollRequest
import dugsolutions.leaf.v35.player.decision.support.SupportStrategy

/** Once the Butterfly has been rolled, a normal player keeps the higher visible result. */
class HumanBaselineSupportStrategy(
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine()
) : SupportStrategy {
    override fun chooseButterflyRoll(request: ChooseButterflyRollRequest): ButterflyRollChoice =
        if (request.rerolledValue > request.originalValue) {
            ButterflyRollChoice.REROLLED
        } else {
            ButterflyRollChoice.ORIGINAL
        }
}
