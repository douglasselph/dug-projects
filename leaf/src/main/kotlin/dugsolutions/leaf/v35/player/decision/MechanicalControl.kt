package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.mechanical.battle.MechanicalBattleStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.buy.MechanicalBuyStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.cultivation.MechanicalCultivationStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.effect.MechanicalEffectStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.placement.MechanicalCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.reward.MechanicalRewardStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.support.MechanicalSupportStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.wound.MechanicalWoundStrategy

/**
 * Deterministic engine-control policy.
 *
 * Mechanical Control is intentionally legal, repeatable, and strategically
 * naive. It exists for unit/integration tests, deterministic engine checks,
 * and as a bottom benchmark. It is NOT the simulation baseline used to model
 * ordinary human play.
 */
object MechanicalControl {
    const val NAME: String = "Mechanical Control"
    const val STRATEGY_LEVEL: Int = 0

    /** Human-readable contract used by tests/reports to keep this layer stable. */
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
            "Mechanical Control must define every DecisionArea"
        }
    }

    fun createDirector(): DecisionDirector =
        DecisionDirector(
            reward = MechanicalRewardStrategy(),
            wound = MechanicalWoundStrategy(),
            placement = MechanicalCreaturePlacementStrategy(),
            cultivation = MechanicalCultivationStrategy(),
            battle = MechanicalBattleStrategy(),
            buy = MechanicalBuyStrategy(),
            support = MechanicalSupportStrategy(),
            effect = MechanicalEffectStrategy()
        )
}
