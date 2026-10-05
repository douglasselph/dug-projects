package dugsolutions.leaf.simulation.v35.learning.cultivation.support

import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

internal fun learnedCultivationSupportFactory(weights: LearnedCultivationSupportWeights): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create() = create(StrategyRandomizer.create(), DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer) = create(strategyRandomizer, DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink) =
        HumanBaselineDecisionDirector(strategyRandomizer=strategyRandomizer, reasoningSink=reasoningSink).createDirector().let { baseline ->
            baseline.copy(cultivationSupport=LearnedCultivationSupportPolicy(weights))
        }
}
