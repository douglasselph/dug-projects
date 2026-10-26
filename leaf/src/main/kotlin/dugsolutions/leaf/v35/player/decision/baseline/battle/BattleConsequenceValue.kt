package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * C04: one tactical unit for an already projected, legal Battle realization.
 * All components come from the existing Strike-row consequence engine, never
 * from a card-name priority or from raw pips alone.
 *
 * Strike VP is reported alongside the swing, not added again: the swing
 * already rewards transitions to wins / wound avoidance. Adding VP here would
 * silently count the same transition twice.
 */
data class BattleConsequenceValue<T>(
    val target: T,
    val mode: BattleAnalysisMode,
    val tacticalPreference: Double,
    val projectedStrikeVpDelta: Int,
    val transitions: List<BattleTransition>,
    val legal: Boolean
)

class BattleConsequenceEvaluator(
    private val rowAnalyzer: BattleActionAnalyzer = BattleActionAnalyzer(),
    private val placementAnalyzer: BattleDiePlacementAnalyzer = BattleDiePlacementAnalyzer()
) {
    fun <T> evaluate(
        context: DecisionContext,
        realization: BattleActionRealization<T>
    ): BattleConsequenceValue<T> {
        val analysis = rowAnalyzer(context, realization)
        return analysis.toConsequence()
    }

    /** Capacity/closed/withdrawn filtering is delegated to the one placement projector. */
    fun placedDie(
        context: DecisionContext,
        faceValue: Double,
        mode: BattleAnalysisMode
    ): List<BattleConsequenceValue<StrikeRow>> =
        placementAnalyzer(context, faceValue, mode).map { it.toConsequence() }

    private fun <T> BattleActionAnalysis<T>.toConsequence(): BattleConsequenceValue<T> =
        BattleConsequenceValue(
            target = realization,
            mode = mode,
            tacticalPreference = tacticalValue,
            projectedStrikeVpDelta = vpImpact.gain,
            transitions = swing.rowSwings.map { it.transition },
            legal = true
        )
}
