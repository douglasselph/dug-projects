package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.ReserveResource
import dugsolutions.leaf.v35.player.decision.baseline.common.ResourceReserveHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import kotlin.math.roundToInt

/**
 * Values one concrete Cultivation Support opportunity.
 *
 * Unlike the earlier flat Support score, this scorer asks whether the specific
 * offered action produces enough visible value to justify using the resource
 * now. Consumable resources are softly penalized when spending them would move
 * below the Human Baseline Cultivation reserve. The penalty is intentionally
 * soft: a clearly valuable Support can still beat [HumanBaselinePolicy.cultivationDoneScore].
 *
 * This scorer evaluates the currently named target (die, Plant, stored Mulch,
 * or Wisp). B5 remains responsible for making later effect/branch choices use
 * compatible target valuation when the Support itself triggers another choice.
 */
object CultivationSupportPriority {
    private const val WATER_REROLL_BASE = 30
    private const val WATER_REROLL_GAIN_MULTIPLIER = 6

    private const val WATER_REFRESH_BASE = 20
    private const val WATER_REFRESH_PLANT_VALUE_DIVISOR = 4
    private const val WATER_REFRESH_BUTTERFLY_BONUS = 8

    private const val MULCH_BASE = 30
    private const val MULCH_EXPECTED_ROLL_MULTIPLIER = 4

    private const val WORM_FLIP_BASE = 30
    private const val WORM_FLIP_CARD_VALUE_DIVISOR = 2

    private const val BUTTERFLY_BASE = 30
    private const val BUTTERFLY_GAIN_MULTIPLIER = 6

    fun score(
        context: DecisionContext,
        action: SupportAction,
        cardScorers: HumanBaselineCardScorerRegistry,
        policy: HumanBaselinePolicy
    ): PriorityScore =
        when (action) {
            is SupportAction.PlayWisp ->
                cardScorers.forWisp(action.card).wispPlayScore(context, action.card)

            is SupportAction.UseWaterReroll -> {
                val expectedGain = DieValueHeuristics.expectedRerollGain(
                    action.die.sides,
                    action.die.value
                )
                PriorityScore(
                    WATER_REROLL_BASE +
                        (expectedGain * WATER_REROLL_GAIN_MULTIPLIER).roundToInt()
                ).withReservePenalty(context, ReserveResource.WATER, policy)
            }

            SupportAction.UseWaterRefresh -> {
                val spentPlantValue = context.self.board.creature
                    .filter { it.isFaceDown }
                    .sumOf { cardScorers.forPlant(it).lossValue(context, it) }
                val spentButterflies = context.self.board.butterflies.count { !it.isFaceUp }
                PriorityScore(
                    WATER_REFRESH_BASE +
                        spentPlantValue / WATER_REFRESH_PLANT_VALUE_DIVISOR +
                        spentButterflies * WATER_REFRESH_BUTTERFLY_BONUS
                ).withReservePenalty(context, ReserveResource.WATER, policy)
            }

            is SupportAction.UseMulch -> {
                val sides = action.token.sides?.value ?: 4
                PriorityScore(
                    MULCH_BASE +
                        (DieValueHeuristics.expectedRoll(sides) * MULCH_EXPECTED_ROLL_MULTIPLIER)
                            .roundToInt()
                ).withReservePenalty(context, ReserveResource.MULCH, policy)
            }

            is SupportAction.UseWormFlip -> {
                val card = context.self.board.creature.firstOrNull { it.id == action.cardId }
                if (card == null || card.isFaceUp) {
                    PriorityScore(0).adjusted(-30, "No spent Plant to refresh")
                } else {
                    val cardValue = cardScorers.forPlant(card).lossValue(context, card)
                    PriorityScore(
                        WORM_FLIP_BASE + cardValue / WORM_FLIP_CARD_VALUE_DIVISOR
                    ).withReservePenalty(context, ReserveResource.WORM, policy)
                }
            }

            is SupportAction.UseButterfly -> {
                val expectedGain = DieValueHeuristics.expectedKeepBestRerollGain(
                    action.die.sides,
                    action.die.value
                )
                PriorityScore(
                    BUTTERFLY_BASE +
                        (expectedGain * BUTTERFLY_GAIN_MULTIPLIER).roundToInt()
                )
            }
        }

    private fun PriorityScore.withReservePenalty(
        context: DecisionContext,
        resource: ReserveResource,
        policy: HumanBaselinePolicy
    ): PriorityScore {
        val targets = policy.protectedCultivationResourceReserve(context)
        val status = ResourceReserveHeuristics.status(context.self.board, resource, targets)
        val penalty = ResourceReserveHeuristics.spendPenalty(
            status = status,
            amount = 1,
            pointsPerMissingUnit = policy.cultivationReserveSpendPenaltyPerUnit(context, resource)
        )
        return if (penalty == 0) this else adjusted(penalty, "Spends into protected $resource reserve")
    }
}
