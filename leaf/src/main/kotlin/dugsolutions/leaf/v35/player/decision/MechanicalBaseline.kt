package dugsolutions.leaf.v35.player.decision

/**
 * @deprecated The old "Mechanical Baseline" name conflated two different
 * concepts. Use [MechanicalControl] for deterministic engine behavior and
 * [HumanBaseline] for the simulation baseline.
 */
@Deprecated(
    message = "Use MechanicalControl; HumanBaseline is the simulation baseline",
    replaceWith = ReplaceWith("MechanicalControl")
)
object MechanicalBaseline {
    const val NAME: String = MechanicalControl.NAME
    const val STRATEGY_LEVEL: Int = MechanicalControl.STRATEGY_LEVEL
    val rules: Map<DecisionArea, String>
        get() = MechanicalControl.rules

    fun createDirector(): DecisionDirector =
        MechanicalControl.createDirector()
}
