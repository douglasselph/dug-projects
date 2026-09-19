package dugsolutions.leaf.v35.player.decision.baseline

import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.baseline.battle.HumanBaselineBattleStrategy
import dugsolutions.leaf.v35.player.decision.baseline.buy.HumanBaselineBuyStrategy
import dugsolutions.leaf.v35.player.decision.baseline.buy.PurchaseScoreModifier
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
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

/**
 * Canonical wiring for the Human Baseline layer.
 *
 * Every decision area shares one score engine, one card-scorer registry and one
 * influence registry. Strategies enumerate legal candidates, score them, apply
 * influences, and use the shared strategy RNG only when the highest-scoring
 * candidates tie.
 */
class HumanBaselineDecisionDirector(
    strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create(),
    reasoningSink: DecisionReasoningSink = DecisionReasoningSink.NONE,
    purchaseScoreModifier: PurchaseScoreModifier = PurchaseScoreModifier.NONE,
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(
        randomizer = strategyRandomizer,
        reasoningSink = reasoningSink
    ),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(cardScorers)
) {
    internal val reward = HumanBaselineRewardStrategy(
        scoreEngine = scoreEngine,
        influenceRegistry = influenceRegistry
    )
    internal val wound = HumanBaselineWoundStrategy(
        scoreEngine = scoreEngine,
        cardScorers = cardScorers,
        influenceRegistry = influenceRegistry
    )
    internal val placement = HumanBaselineCreaturePlacementStrategy(
        scoreEngine = scoreEngine,
        influenceRegistry = influenceRegistry
    )
    internal val cultivation = HumanBaselineCultivationStrategy(
        scoreEngine = scoreEngine,
        cardScorers = cardScorers,
        influenceRegistry = influenceRegistry
    )
    internal val battle = HumanBaselineBattleStrategy(
        scoreEngine = scoreEngine,
        cardScorers = cardScorers,
        influenceRegistry = influenceRegistry
    )
    internal val buy = HumanBaselineBuyStrategy(
        scoreEngine = scoreEngine,
        cardScorers = cardScorers,
        influenceRegistry = influenceRegistry,
        purchaseScoreModifier = purchaseScoreModifier
    )
    internal val support = HumanBaselineSupportStrategy(
        scoreEngine = scoreEngine,
        influenceRegistry = influenceRegistry
    )
    internal val effect = HumanBaselineEffectStrategy(
        scoreEngine = scoreEngine,
        cardScorers = cardScorers,
        influenceRegistry = influenceRegistry
    )

    fun createDirector(): DecisionDirector = DecisionDirector(
        reward = reward,
        wound = wound,
        placement = placement,
        cultivation = cultivation,
        battle = battle,
        buy = buy,
        support = support,
        effect = effect
    )
}
