package dugsolutions.leaf.simulation.v35.learning.wisp

import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayPolicy
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

internal fun learnedWispPlayFactory(weights: LearnedWispPlayWeights): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create() = create(StrategyRandomizer.create(), DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer) = create(strategyRandomizer, DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink) =
        HumanBaselineDecisionDirector(strategyRandomizer=strategyRandomizer, reasoningSink=reasoningSink).createDirector().let { baseline ->
            baseline.copy(wispPlay=LearnedWispPlayPolicy(weights))
        }
}
