package dugsolutions.leaf.v35.player.decision.learned.cultivation

import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyStrategy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer

/** Human Baseline everywhere except learned Cultivation Main; optionally learned Buy too. */
object LearnedCultivationMain {
    fun createDirector(
        weights: LearnedCultivationMainWeights,
        strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create(),
        buyWeights: LearnedBuyWeights? = null
    ): DecisionDirector {
        val baseline = HumanBaselineDecisionDirector(strategyRandomizer = strategyRandomizer).createDirector()
        val withMain = baseline.copy(cultivationMain = LearnedCultivationMainPolicy(weights))
        return if (buyWeights == null) withMain
        else withMain.copy(buy = LearnedBuyStrategy(buyWeights, baseline.buy))
    }
}
