package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice

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

    /** A target's full tactical projection, including opposing-row collateral effects. */
    data class TargetProjection(
        val handIndex: Int?,
        val tacticalValue: Double,
        val strikeRow: StrikeRow? = null
    )

    /**
     * Action valuation and own-die target valuation share these projections.
     * The supplied indices may be restricted to currently legal target choices;
     * an empty set means no legal own-die targets, NOT all targets.
     */
    fun targetProjections(
        context: DecisionContext,
        effect: GameEffect,
        legalHandIndices: Set<Int>? = null
    ): List<TargetProjection> {
        if (!supports(effect)) return emptyList()
        val battle = context.battle ?: return emptyList()
        val ownDice = battle.rows.filterNot { it.closed }.flatMap { row ->
            row.forPlayer(context.self.id)?.takeUnless { it.withdrawn }?.dice.orEmpty()
        }.filter { legalHandIndices == null || it.handIndex in legalHandIndices }
        return when (effect) {
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                ownDice.mapNotNull { die ->
                    ownDie(context, effect, die.handIndex, die.handIndex)?.tacticalValue
                        ?.let { TargetProjection(die.handIndex, it) }
                }
            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                ownDice.mapNotNull { die ->
                    gust(context, EffectDieChoice(die.handIndex, die.sides, die.value), die.handIndex)
                        ?.tacticalValue?.let { TargetProjection(die.handIndex, it) }
                }
            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3 ->
                battle.rows.filterNot { it.closed }.mapNotNull { row ->
                    strikeRow(context, effect, row.row)?.tacticalValue
                        ?.let { TargetProjection(null, it, row.row) }
                }
            else -> emptyList()
        }
    }

    fun bestTacticalValue(context: DecisionContext, effect: GameEffect): Double? =
        targetProjections(context, effect).maxOfOrNull { it.tacticalValue }

    fun score(context: DecisionContext, effect: GameEffect, base: Int): PriorityScore? =
        bestTacticalValue(context, effect)?.let { projected ->
            BattleProjectedActionScore.fromCompleteProjection(
                projected, "Best projected complete Battle collateral outcome for $effect"
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
