package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.baseline.battle.HumanBaselineBattleStrategy
import dugsolutions.leaf.v35.player.decision.baseline.buy.HumanBaselineBuyStrategy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.HumanBaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.baseline.effect.HumanBaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.placement.HumanBaselineCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.baseline.reward.HumanBaselineRewardStrategy
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.support.HumanBaselineSupportStrategy
import dugsolutions.leaf.v35.player.decision.baseline.wound.HumanBaselineWoundStrategy
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer

/** Canonical simulation baseline: simple, reasonable human play. */
object HumanBaseline {
    const val NAME: String = "Human Baseline"
    const val STRATEGY_LEVEL: Int = 1
    const val HEURISTICS_IMPLEMENTED: Boolean = true

    fun createDirector(
        strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create()
    ): DecisionDirector {
        val scoreEngine = BaselineScoreEngine(strategyRandomizer)
        val cardScorers = HumanBaselineCardScorerRegistry()
        val influenceRegistry = BaselineInfluenceRegistry(cardScorers)

        return DecisionDirector(
            reward = HumanBaselineRewardStrategy(
                scoreEngine = scoreEngine,
                influenceRegistry = influenceRegistry
            ),
            wound = HumanBaselineWoundStrategy(scoreEngine = scoreEngine, cardScorers = cardScorers),
            placement = HumanBaselineCreaturePlacementStrategy(scoreEngine = scoreEngine),
            cultivation = HumanBaselineCultivationStrategy(
                scoreEngine = scoreEngine,
                cardScorers = cardScorers,
                influenceRegistry = influenceRegistry
            ),
            battle = HumanBaselineBattleStrategy(
                scoreEngine = scoreEngine,
                cardScorers = cardScorers,
                influenceRegistry = influenceRegistry
            ),
            buy = HumanBaselineBuyStrategy(
                scoreEngine = scoreEngine,
                cardScorers = cardScorers,
                influenceRegistry = influenceRegistry
            ),
            support = HumanBaselineSupportStrategy(scoreEngine = scoreEngine),
            effect = HumanBaselineEffectStrategy(scoreEngine = scoreEngine, cardScorers = cardScorers)
        )
    }
}
