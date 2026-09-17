package dugsolutions.leaf.simulation.v35.strategy.planned

import dugsolutions.leaf.simulation.v35.strategy.StrategyLevel
import dugsolutions.leaf.simulation.v35.strategy.StrategyProfile
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.DecisionArea
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

/**
 * Human Baseline plus an exact Creature purchase plan.
 *
 * The plan changes only Plant-purchase priorities. Every other decision area,
 * including payment, placement, card play, Battle choices and Wounds, remains
 * ordinary Human Baseline behavior.
 */
object PlannedBaselineStrategy {
    fun profile(
        plan: CreaturePlan,
        name: String = defaultName(plan)
    ): StrategyProfile =
        StrategyProfile(
            name = name,
            levels = DecisionArea.entries.associateWith { StrategyLevel.HUMAN_BASELINE },
            decisionFactory = decisionFactory(plan)
        )

    fun decisionFactory(plan: CreaturePlan): PlayerDecisionFactory {
        val purchaseModifier = CardFocusModifier(plan)
        return object : PlayerDecisionFactory {
            override fun create(): DecisionDirector =
                HumanBaselineDecisionDirector(
                    purchaseScoreModifier = purchaseModifier
                ).createDirector()

            override fun create(
                strategyRandomizer: StrategyRandomizer
            ): DecisionDirector =
                HumanBaselineDecisionDirector(
                    strategyRandomizer = strategyRandomizer,
                    purchaseScoreModifier = purchaseModifier
                ).createDirector()

            override fun create(
                strategyRandomizer: StrategyRandomizer,
                reasoningSink: DecisionReasoningSink
            ): DecisionDirector =
                HumanBaselineDecisionDirector(
                    strategyRandomizer = strategyRandomizer,
                    reasoningSink = reasoningSink,
                    purchaseScoreModifier = purchaseModifier
                ).createDirector()
        }
    }

    private fun defaultName(plan: CreaturePlan): String {
        val summary = plan.targets.entries
            .sortedBy { it.key.cardName }
            .joinToString(", ") { (target, count) -> "${target.cardName}×$count" }
        return "Planned Baseline [$summary]"
    }
}
