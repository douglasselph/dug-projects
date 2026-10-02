package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCard
import kotlin.math.max

/**
 * Human Baseline valuation for consuming one banked Sunlight during Battle.
 *
 * Sunlight's benefit is the ordinary Battle Main Action it funds, so that action
 * continues to use [BattleMainPriority]. This helper only supplies the visible,
 * shallow preservation value of the token itself. In particular, it makes the
 * last token easier to preserve in an early Battle, makes surplus tokens easier
 * to spend, and largely removes preservation pressure in the final Battle.
 *
 * It does not predict future rolls, hidden cards, future purchases, or future
 * Strike layouts.
 */
object SunlightBattleSpendingValue {
    data class Observation(
        val fundedMainValue: Int,
        val sunlightHeld: Int,
        val battlesRemaining: Int,
        val finalBattle: Boolean,
        val basePreservationValue: Int,
        val battleTimingAdjustment: Int,
        val additionalHoldingAdjustment: Int,
        val preservationValue: Int,
        val netSupportValue: Int
    ) {
        val worthwhile: Boolean get() = netSupportValue >= 0
    }

    fun observe(
        context: DecisionContext,
        roundCard: RoundCard,
        action: BattleMainAction,
        cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
    ): Observation {
        val fundedMainValue = BattleMainPriority.score(context, roundCard, action, cardScorers).total
        val sunlightHeld = context.self.board.sunlight
        val reportedBattlesRemaining = context.progress.battleRoundsRemaining
        val battlesRemaining = reportedBattlesRemaining ?: 2
        val finalBattle = context.progress.isFinalBattleRound ||
            (reportedBattlesRemaining != null && reportedBattlesRemaining <= 1)
        val battleTimingAdjustment = when {
            finalBattle -> FINAL_BATTLE_DISCOUNT
            battlesRemaining >= 3 -> EARLY_BATTLE_PRESERVATION_BONUS
            else -> 0
        }
        val additionalHoldingAdjustment =
            -max(0, sunlightHeld - 1) * ADDITIONAL_SUNLIGHT_DISCOUNT
        val preservationValue = max(
            MINIMUM_PRESERVATION_VALUE,
            BASE_PRESERVATION_VALUE + battleTimingAdjustment + additionalHoldingAdjustment
        )
        return Observation(
            fundedMainValue = fundedMainValue,
            sunlightHeld = sunlightHeld,
            battlesRemaining = battlesRemaining,
            finalBattle = finalBattle,
            basePreservationValue = BASE_PRESERVATION_VALUE,
            battleTimingAdjustment = battleTimingAdjustment,
            additionalHoldingAdjustment = additionalHoldingAdjustment,
            preservationValue = preservationValue,
            netSupportValue = fundedMainValue - preservationValue
        )
    }

    fun score(
        context: DecisionContext,
        roundCard: RoundCard,
        action: BattleMainAction,
        cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
    ): PriorityScore {
        val observation = observe(context, roundCard, action, cardScorers)
        return BattleMainPriority.score(context, roundCard, action, cardScorers)
            .adjusted(
                -observation.basePreservationValue,
                "Preserve one banked Sunlight for a future extra Battle Main"
            )
            .adjusted(
                -observation.battleTimingAdjustment,
                when {
                    observation.finalBattle -> "Final Battle removes most future Sunlight preservation value"
                    observation.battleTimingAdjustment > 0 -> "Early Battle preserves the last Sunlight for later tactical opportunity"
                    else -> "Current Battle timing adds no Sunlight preservation adjustment"
                }
            )
            .adjusted(
                -observation.additionalHoldingAdjustment,
                if (observation.additionalHoldingAdjustment < 0) {
                    "Additional banked Sunlight lowers the marginal preservation cost"
                } else {
                    "No surplus Sunlight preservation adjustment"
                }
            )
    }

    fun observations(
        context: DecisionContext,
        roundCard: RoundCard,
        action: BattleMainAction,
        cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
    ): Map<String, String> {
        val observed = observe(context, roundCard, action, cardScorers)
        return linkedMapOf(
            "decisionFamily" to "sunlight-battle-spend",
            "fundedMainAction" to action.toString(),
            "fundedMainValue" to observed.fundedMainValue.toString(),
            "sunlightHeld" to observed.sunlightHeld.toString(),
            "battlesRemaining" to observed.battlesRemaining.toString(),
            "finalBattle" to observed.finalBattle.toString(),
            "sunlightBasePreservation" to observed.basePreservationValue.toString(),
            "sunlightBattleTimingAdjustment" to observed.battleTimingAdjustment.toString(),
            "sunlightAdditionalHoldingAdjustment" to observed.additionalHoldingAdjustment.toString(),
            "sunlightPreservationValue" to observed.preservationValue.toString(),
            "sunlightNetSupportValue" to observed.netSupportValue.toString()
        )
    }

    private const val BASE_PRESERVATION_VALUE = 55
    private const val EARLY_BATTLE_PRESERVATION_BONUS = 7
    private const val FINAL_BATTLE_DISCOUNT = -40
    private const val ADDITIONAL_SUNLIGHT_DISCOUNT = 10
    private const val MINIMUM_PRESERVATION_VALUE = 5
}
