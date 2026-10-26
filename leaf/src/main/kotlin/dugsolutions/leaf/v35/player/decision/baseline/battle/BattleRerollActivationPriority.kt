package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * C04: expected Battle consequence of a reroll, integrated over the *actual*
 * possible faces. Averaging die faces first is incorrect near tie/win/wound
 * thresholds, because Strike-row tactical value is nonlinear.
 *
 * This supports the pure reroll-until-3+ effect only; compound rerolls with
 * draws or opponent changes require their complete-effect projectors.
 */
class BattleRerollActivationPriority(
    private val ownTotal: BattleOwnTotalChangeAnalyzer = BattleOwnTotalChangeAnalyzer()
) {
    fun bestTacticalValue(context: DecisionContext, effect: GameEffect): Double? {
        if (effect != GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS) return null
        val battle = context.battle ?: return null
        return battle.rows.asSequence().filterNot { it.closed }.flatMap { row ->
            row.forPlayer(context.self.id)?.takeUnless { it.withdrawn }?.dice.orEmpty()
                .asSequence().map { die -> row.row to die }
        }.mapNotNull { (row, die) ->
            // A D4 rerolled until 3+ yields 3 or 4 with equal probability.
            // Any die with fewer than three sides cannot resolve this effect.
            if (die.sides < 3) return@mapNotNull null
            val consequences = (3..die.sides).mapNotNull { face ->
                ownTotal(
                    context, die.handIndex, row, (face - die.value).toDouble(),
                    BattleAnalysisMode.EXPECTED
                )?.tacticalValue
            }
            if (consequences.size != die.sides - 2) null else consequences.average()
        }.maxOrNull()
    }

    fun score(context: DecisionContext, effect: GameEffect, intrinsic: PriorityScore): PriorityScore? =
        bestTacticalValue(context, effect)?.let { tactical ->
            BattleProjectedActionScore.fromCompleteProjection(
                tactical,
                "Expected Strike-row consequence across all legal 3+ reroll faces for $effect"
            )
        }
}
