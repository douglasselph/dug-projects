package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.baseline.battle.HumanBaselineBattleStrategy
import dugsolutions.leaf.v35.player.decision.baseline.buy.HumanBaselineBuyStrategy
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.HumanBaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.baseline.effect.HumanBaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.baseline.placement.HumanBaselineCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.baseline.reward.HumanBaselineRewardStrategy
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.support.HumanBaselineSupportStrategy
import dugsolutions.leaf.v35.player.decision.baseline.wound.HumanBaselineWoundStrategy
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer

/**
 * Canonical simulation baseline: simple, reasonable human play.
 *
 * This layer is intentionally separate from [MechanicalControl]. Scoring
 * primitives and a strategy-only random stream now exist; the detailed Human
 * Baseline heuristics are still added incrementally in the later stages.
 */
object HumanBaseline {
    const val NAME: String = "Human Baseline"
    const val STRATEGY_LEVEL: Int = 1

    /**
     * False until the actual Human Baseline decision heuristics replace the
     * temporary Mechanical Control delegates.
     */
    const val HEURISTICS_IMPLEMENTED: Boolean = false

    fun createDirector(
        strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create()
    ): DecisionDirector {
        /*
         * One player owns one strategy stream. All Human Baseline decision
         * areas share this score engine, so equal-score choices are random but
         * can never consume the Game's mechanical RNG.
         */
        val scoreEngine =
            BaselineScoreEngine(strategyRandomizer)

        return DecisionDirector(
            reward = HumanBaselineRewardStrategy(scoreEngine = scoreEngine),
            wound = HumanBaselineWoundStrategy(scoreEngine = scoreEngine),
            placement = HumanBaselineCreaturePlacementStrategy(scoreEngine = scoreEngine),
            cultivation = HumanBaselineCultivationStrategy(scoreEngine = scoreEngine),
            battle = HumanBaselineBattleStrategy(scoreEngine = scoreEngine),
            buy = HumanBaselineBuyStrategy(scoreEngine = scoreEngine),
            support = HumanBaselineSupportStrategy(scoreEngine = scoreEngine),
            effect = HumanBaselineEffectStrategy(scoreEngine = scoreEngine)
        )
    }
}
