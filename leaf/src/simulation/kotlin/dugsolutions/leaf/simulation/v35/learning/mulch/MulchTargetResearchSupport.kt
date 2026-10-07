package dugsolutions.leaf.simulation.v35.learning.mulch

import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.mulch.LearnedMulchTargetPolicy
import dugsolutions.leaf.v35.player.decision.learned.mulch.LearnedMulchTargetWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

internal fun learnedMulchTargetFactory(weights: LearnedMulchTargetWeights): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create()=create(StrategyRandomizer.create(),DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer:StrategyRandomizer)=create(strategyRandomizer,DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer:StrategyRandomizer,reasoningSink:DecisionReasoningSink)=
        HumanBaselineDecisionDirector(strategyRandomizer=strategyRandomizer,reasoningSink=reasoningSink).createDirector().let{b->
            b.copy(mulchTarget=LearnedMulchTargetPolicy(weights))
        }
}
