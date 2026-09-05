package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.battle.BaselineBattleStrategy
import dugsolutions.leaf.v35.player.decision.buy.BaselineBuyStrategy
import dugsolutions.leaf.v35.player.decision.cultivation.BaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.effect.BaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.placement.BaselineCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.reward.BaselineRewardStrategy
import dugsolutions.leaf.v35.player.decision.support.BaselineSupportStrategy
import dugsolutions.leaf.v35.player.decision.wound.BaselineWoundStrategy

/**
 * Formal definition of Strategy Level 0: Mechanical Baseline.
 *
 * This policy is intentionally deterministic and intentionally unsophisticated.
 * It exists as the stable control group for simulation experiments. Changes to
 * these rules should therefore be deliberate and accompanied by changes to the
 * baseline contract tests rather than being folded in as incidental "improvements".
 */
object MechanicalBaseline {
    const val NAME: String = "Mechanical Baseline"
    const val STRATEGY_LEVEL: Int = 0

    /** Human-readable contract used by reports and simulation metadata. */
    val rules: Map<DecisionArea, String> = linkedMapOf(
        DecisionArea.CULTIVATION to
            "Prefer Draw while Main Actions remain; otherwise first legal Main Action; " +
                "after both Main Actions, finish instead of taking optional Support Actions.",
        DecisionArea.BATTLE to
            "Prefer Draw for Main Actions; during Support/Final-Main passes, take the final Main Action " +
                "as soon as possible; place a new die in the first legal Strike Row.",
        DecisionArea.BUY to
            "Buy the first legal item; choose a minimum-sufficient payment, breaking equal totals by fewer resources.",
        DecisionArea.GRAFT_PLACEMENT to
            "Choose the first legal graft placement.",
        DecisionArea.WOUND to
            "Choose the first legal Wound resolution.",
        DecisionArea.CRITTER_REWARD to
            "Choose the Critter type owned in smaller quantity; choose Bee on a tie; otherwise choose the first legal type.",
        DecisionArea.BUTTERFLY to
            "Keep the higher of the original and rerolled values; keep the original on a tie.",
        DecisionArea.EFFECT to
            "Use the first legal target or branch, with deterministic first/legal selection for multi-target effects."
    )

    init {
        require(rules.keys == DecisionArea.entries.toSet()) {
            "Mechanical Baseline must define every DecisionArea"
        }
    }

    /** Creates a fresh, independent Level-0 director. */
    fun createDirector(): DecisionDirector =
        DecisionDirector(
            reward = BaselineRewardStrategy(),
            wound = BaselineWoundStrategy(),
            placement = BaselineCreaturePlacementStrategy(),
            cultivation = BaselineCultivationStrategy(),
            battle = BaselineBattleStrategy(),
            buy = BaselineBuyStrategy(),
            support = BaselineSupportStrategy(),
            effect = BaselineEffectStrategy()
        )
}
