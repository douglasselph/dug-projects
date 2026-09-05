package dugsolutions.leaf.simulation.v35.strategy

import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.DecisionArea
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.HumanBaseline
import dugsolutions.leaf.v35.player.decision.MechanicalControl

/**
 * Named simulation strategy configuration.
 *
 * [levels] describes each independent decision area. [decisionFactory] creates
 * a fresh director for each player/game so stateful strategies never leak.
 */
class StrategyProfile(
    val name: String,
    levels: Map<DecisionArea, StrategyLevel>,
    val decisionFactory: PlayerDecisionFactory
) {
    val levels: Map<DecisionArea, StrategyLevel> = levels.toMap()

    init {
        require(name.isNotBlank()) { "Strategy profile name cannot be blank" }
        require(this.levels.keys == DecisionArea.entries.toSet()) {
            "Strategy profile must classify every DecisionArea"
        }
    }

    fun levelFor(area: DecisionArea): StrategyLevel =
        levels.getValue(area)

    fun createDirector(): DecisionDirector =
        decisionFactory.create()

    fun withDecisionArea(
        name: String,
        area: DecisionArea,
        level: StrategyLevel,
        transform: (DecisionDirector) -> DecisionDirector
    ): StrategyProfile =
        StrategyProfile(
            name = name,
            levels = levels + (area to level),
            decisionFactory = PlayerDecisionFactory {
                transform(decisionFactory.create())
            }
        )

    companion object {
        /** Deterministic engine/test control profile. */
        fun mechanicalControl(): StrategyProfile =
            StrategyProfile(
                name = MechanicalControl.NAME,
                levels = DecisionArea.entries.associateWith {
                    StrategyLevel.MECHANICAL_CONTROL
                },
                decisionFactory = PlayerDecisionFactory.mechanicalControl()
            )

        /** Canonical simulation baseline. */
        fun humanBaseline(): StrategyProfile =
            StrategyProfile(
                name = HumanBaseline.NAME,
                levels = DecisionArea.entries.associateWith {
                    StrategyLevel.HUMAN_BASELINE
                },
                decisionFactory = PlayerDecisionFactory.humanBaseline()
            )

        /** Canonical shorthand for simulation callers. */
        fun baseline(): StrategyProfile =
            humanBaseline()

        /** Backward-compatible old name for the Level-0 control profile. */
        @Deprecated("Use mechanicalControl()")
        fun mechanicalBaseline(): StrategyProfile =
            mechanicalControl()
    }
}
