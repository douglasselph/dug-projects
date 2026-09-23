package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.EffectDiePairChoice

/**
 * Projects one complete Root Kindred source/target pair in the current Battle.
 *
 * The source die is read but not changed. The target die is set exactly to the
 * source's current value, so the target row receives the signed value delta.
 * Legal pair generation remains owned by the effect handler; this helper only
 * evaluates an already-legal pair without mutating game state.
 */
class BattleSetDieToMatchAnalyzer(
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val ownTotalChangeAnalyzer: BattleOwnTotalChangeAnalyzer =
        BattleOwnTotalChangeAnalyzer(policy)
) {
    operator fun <T> invoke(
        context: DecisionContext,
        pair: EffectDiePairChoice,
        realization: T
    ): BattleActionAnalysis<T>? {
        val row = context.battle?.rows?.firstOrNull { row ->
            row.forPlayer(context.self.id)?.dice?.any {
                it.handIndex == pair.target.index
            } == true
        }?.row ?: return null

        return ownTotalChangeAnalyzer(
            context = context,
            realization = realization,
            row = row,
            change = (pair.source.value - pair.target.value).toDouble(),
            mode = BattleAnalysisMode.DETERMINISTIC
        )
    }
}
