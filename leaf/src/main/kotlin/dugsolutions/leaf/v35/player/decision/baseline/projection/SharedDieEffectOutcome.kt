package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.SimpleEffectMagnitude
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.context.DieView

/**
 * C02: one physical outcome calculation for deterministic single-die effects.
 * A null result means this evaluator does not support that effect, not zero benefit.
 * Only legal candidates should be passed here; callers remain responsible for legality.
 * Battle collateral effects continue to use dedicated tactical analyzers.
 */
object SharedDieEffectOutcome {
    fun projectedPips(effect: GameEffect, sides: Int, value: Int): Int? = when (effect) {
        GameEffect.DOUBLE_ONE_DIE -> minOf(sides, value * 2) - value
        GameEffect.RAISE_DIE_PLUS_4, GameEffect.RESEARCH_RAISE_DIE_PLUS_20 ->
            DieValueHeuristics.actualRaiseGain(sides, value, requireNotNull(SimpleEffectMagnitude.raiseAmount(effect)))
        GameEffect.RAISE_ANY_DIE_PLUS_1,
        GameEffect.RAISE_D8_PLUS_1,
        GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE,
        GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
        GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE ->
            DieValueHeuristics.actualRaiseGain(sides, value, 1)
        GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
        GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW ->
            DieValueHeuristics.actualRaiseGain(sides, value, 2)
        GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE -> DieValueHeuristics.flipGain(sides, value)
        GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX,
        GameEffect.SET_DIE_UP_TO_D12_TO_MAX -> DieValueHeuristics.setToMaximumGain(sides, value)
        else -> null
    }

    fun projectedPips(effect: GameEffect, die: DieView): Int? =
        projectedPips(effect, die.sides, die.value)

    fun bestProjectedPips(effect: GameEffect, dice: List<DieView>): Int? {
        if (dice.isEmpty()) return null
        val candidates = when (effect) {
            GameEffect.RAISE_D8_PLUS_1 -> dice.filter { it.sides == 8 }
            GameEffect.SET_DIE_UP_TO_D12_TO_MAX -> dice.filter { it.sides <= 12 }
            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX -> {
                val lowest = dice.minOf { it.value }
                dice.filter { it.value == lowest }
            }
            else -> dice
        }
        return candidates.mapNotNull { projectedPips(effect, it) }.maxOrNull()
    }
}
