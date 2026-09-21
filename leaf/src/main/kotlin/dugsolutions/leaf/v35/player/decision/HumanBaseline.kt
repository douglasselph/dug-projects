package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

/** Canonical simulation baseline: simple, reasonable human play. */
object HumanBaseline {
    const val NAME: String = "Human Baseline"
    const val STRATEGY_LEVEL: Int = 1
    const val HEURISTICS_IMPLEMENTED: Boolean = true

    fun createDirector(
        strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create(),
        reasoningSink: DecisionReasoningSink = DecisionReasoningSink.NONE,
        policy: HumanBaselinePolicy = HumanBaselinePolicy()
    ): DecisionDirector =
        HumanBaselineDecisionDirector(
            strategyRandomizer = strategyRandomizer,
            reasoningSink = reasoningSink,
            policy = policy
        ).createDirector()
}
