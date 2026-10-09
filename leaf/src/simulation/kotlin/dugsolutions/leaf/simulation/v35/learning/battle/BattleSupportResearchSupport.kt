package dugsolutions.leaf.simulation.v35.learning.battle

import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyStrategy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

internal fun learnedBattleSupportFactory(
    weights: LearnedBattleSupportWeights,
    buyWeights: LearnedBuyWeights? = null
): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create() = create(StrategyRandomizer.create())
    override fun create(strategyRandomizer: StrategyRandomizer) = create(strategyRandomizer, DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink) =
        HumanBaselineDecisionDirector(strategyRandomizer = strategyRandomizer, reasoningSink = reasoningSink)
            .createDirector().let { baseline ->
                var director = baseline.copy(battleSupport = LearnedBattleSupportPolicy(weights))
                if (buyWeights != null) {
                    director = director.copy(buy = LearnedBuyStrategy(buyWeights, baseline.buy))
                }
                director
            }
}
