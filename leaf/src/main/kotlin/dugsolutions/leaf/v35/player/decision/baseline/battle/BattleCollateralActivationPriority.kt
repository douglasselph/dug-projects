package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import kotlin.math.roundToInt

/**
 * Shares the already existing target-level Battle projection with top-level
 * Plant activation decisions. Only the BEST legal complete realization is
 * credited; a fixed intrinsic effect bonus is not added to that projection.
 *
 * Gust's row branch is evaluated before the actor's unknown reroll, using
 * arithmetic-mean die expectations. Actual resolution and later row choice
 * remain the engine's responsibility.
 */
class BattleCollateralActivationPriority(
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val ownDie: BattleOwnDieCollateralAnalyzer = BattleOwnDieCollateralAnalyzer(policy),
    private val gust: BattleGustOfPetalsTargetAnalyzer = BattleGustOfPetalsTargetAnalyzer(policy),
    private val strikeRow: BattleDeterministicStrikeRowTargetAnalyzer =
        BattleDeterministicStrikeRowTargetAnalyzer(policy)
) {
    fun supports(effect: GameEffect): Boolean = effect in EFFECTS

    fun bestTacticalValue(context: DecisionContext, effect: GameEffect): Double? {
        if (!supports(effect)) return null
        val battle = context.battle ?: return null
        return when (effect) {
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                battle.rows.flatMap { row ->
                    row.forPlayer(context.self.id)?.dice.orEmpty()
                }.mapNotNull { die ->
                    ownDie(context, effect, die.handIndex, die.handIndex)?.tacticalValue
                }.maxOrNull()

            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                battle.rows.flatMap { row ->
                    row.forPlayer(context.self.id)?.dice.orEmpty()
                }.mapNotNull { die ->
                    gust(
                        context,
                        EffectDieChoice(die.handIndex, die.sides, die.value),
                        die.handIndex
                    )?.tacticalValue
                }.maxOrNull()

            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3 ->
                battle.rows.filterNot { it.closed }.mapNotNull { row ->
                    strikeRow(context, effect, row.row)?.tacticalValue
                }.maxOrNull()

            else -> null
        }
    }

    fun score(context: DecisionContext, effect: GameEffect, base: Int): PriorityScore? =
        bestTacticalValue(context, effect)?.let { projected ->
            PriorityScore(base).adjusted(
                projected.roundToInt(),
                "Best projected complete Battle collateral outcome for $effect"
            )
        }

    private companion object {
        val EFFECTS = setOf(
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3
        )
    }
}
