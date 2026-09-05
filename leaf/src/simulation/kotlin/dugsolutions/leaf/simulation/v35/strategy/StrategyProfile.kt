package dugsolutions.leaf.simulation.v35.strategy

import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.DecisionArea
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.MechanicalBaseline

/**
 * Named simulation strategy configuration.
 *
 * [levels] describes the sophistication of each independent decision area.
 * [decisionFactory] creates a fresh director per player/game so future stateful
 * or learned strategies never leak runtime/training state between games.
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

    /**
     * Builds a new profile by changing exactly one strategic dimension.
     *
     * Example for future simulation code:
     *
     * mechanical.withDecisionArea("Simple Buy", BUY, SIMPLE_HEURISTIC) {
     *     it.copy(buy = SimpleBuyStrategy())
     * }
     *
     * [transform] is applied to a fresh director each time the new profile is
     * instantiated, preserving isolation for stateful strategies.
     */
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
        /** Stable Strategy-0 control profile. */
        fun mechanicalBaseline(): StrategyProfile =
            StrategyProfile(
                name = MechanicalBaseline.NAME,
                levels = DecisionArea.entries.associateWith {
                    StrategyLevel.MECHANICAL_BASELINE
                },
                decisionFactory = PlayerDecisionFactory.mechanicalBaseline()
            )
    }
}
