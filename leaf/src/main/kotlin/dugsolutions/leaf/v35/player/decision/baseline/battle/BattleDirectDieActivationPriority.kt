package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.projection.SharedDieEffectOutcome
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * C04: evaluates an actual Battle-board die modification through the same
 * Strike-row consequence projector used by other Battle candidate families.
 * Only straightforward single-die effects are supported: a null result means
 * the candidate needs a different (possibly multi-target) projector.
 * Do not award the projected Strike VP a second time.
 */
class BattleDirectDieActivationPriority(
    private val ownTotal: BattleOwnTotalChangeAnalyzer = BattleOwnTotalChangeAnalyzer()
) {
    fun bestTacticalValue(context: DecisionContext, effect: GameEffect): Double? {
        if (effect !in DIRECT_EFFECTS) return null
        val battle = context.battle ?: return null
        val actor = context.self.id
        val ownDice = battle.rows.filterNot { it.closed }.flatMap { row ->
            row.forPlayer(actor)?.takeUnless { it.withdrawn }?.dice.orEmpty().map { die -> row.row to die }
        }
        val lowestValue = if (effect == GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX)
            ownDice.minOfOrNull { it.second.value } else null
        return ownDice.mapNotNull { (row, die) ->
            if (effect == GameEffect.RAISE_D8_PLUS_1 && die.sides != 8) return@mapNotNull null
            if (effect == GameEffect.SET_DIE_UP_TO_D12_TO_MAX && die.sides > 12) return@mapNotNull null
            if (lowestValue != null && die.value != lowestValue) return@mapNotNull null
            val delta = SharedDieEffectOutcome.projectedPips(effect, die.sides, die.value)
                ?: return@mapNotNull null
            ownTotal(context, die.handIndex, row, delta.toDouble(), BattleAnalysisMode.DETERMINISTIC)
                ?.tacticalValue
        }.maxOrNull()
    }

    fun score(context: DecisionContext, effect: GameEffect, intrinsic: PriorityScore): PriorityScore? =
        bestTacticalValue(context, effect)?.let { tactical ->
            BattleProjectedActionScore.fromCompleteProjection(
                tactical,
                "Best legal single-die Battle Strike consequence for $effect (no duplicate Strike VP)"
            )
        }

    companion object {
        val DIRECT_EFFECTS: Set<GameEffect> = setOf(
            GameEffect.DOUBLE_ONE_DIE,
            GameEffect.RAISE_DIE_PLUS_4,
            GameEffect.RESEARCH_RAISE_DIE_PLUS_20,
            GameEffect.RAISE_ANY_DIE_PLUS_1,
            GameEffect.RAISE_D8_PLUS_1,
            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE,
            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX,
            GameEffect.SET_DIE_UP_TO_D12_TO_MAX
        )
    }
}
