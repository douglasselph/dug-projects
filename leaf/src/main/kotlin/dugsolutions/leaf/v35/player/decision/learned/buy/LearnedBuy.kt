package dugsolutions.leaf.v35.player.decision.learned.buy

import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import java.nio.file.Path

/** Human Baseline in every decision domain except learned purchase selection. */
object LearnedBuy {
    fun createDirector(weights: LearnedBuyWeights, strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create()): DecisionDirector {
        val baseline = HumanBaselineDecisionDirector(strategyRandomizer = strategyRandomizer).createDirector()
        return baseline.copy(buy = LearnedBuyStrategy(weights, baseline.buy))
    }

    fun createDirector(weightsPath: Path, strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create()): DecisionDirector =
        createDirector(LearnedBuyWeights.load(weightsPath), strategyRandomizer)
}
