package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.context.PhaseProximity
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Ordinary-human prospective value for spending a Cultivation Main Action now
 * to bank one Sunlight-funded Main Action for a later Battle.
 *
 * This is intentionally shallow. It uses only public phase timing, currently
 * held Sunlight, and the visible Battle roles of face-up Plants. It does not
 * predict future purchases, rolls, Strike layouts, or exact future targets.
 */
object SunlightTokenPriority {
    private const val BASE_FUTURE_ACTION_VALUE = 42
    private const val PER_BATTLE_REMAINING_BONUS = 4
    private const val MAX_BATTLE_REMAINING_BONUS = 12
    private const val BATTLE_NEXT_BONUS = 8
    private const val BATTLE_ONE_ROUND_AWAY_BONUS = 4
    private const val STORED_SUNLIGHT_PENALTY = 12
    private const val COVERED_REMAINING_BATTLES_PENALTY = 8
    private const val NORMAL_MAIN_ACTIONS = 2
    private const val MEANINGFUL_BATTLE_PLANT_FLOOR = 45
    private const val MAX_VISIBLE_PLANT_BONUS = 12

    data class Observation(
        val battlesRemaining: Int,
        val roundsUntilBattle: Int?,
        val battleIsNext: Boolean,
        val sunlightHeld: Int,
        val thirdBestFaceUpBattlePlantBase: Int?,
        val baseFutureActionValue: Int,
        val battleProximityAdjustment: Int,
        val battlesRemainingAdjustment: Int,
        val existingSunlightAdjustment: Int,
        val visibleBattlePlantAdjustment: Int,
        val finalPriority: Int
    )

    fun observe(
        context: DecisionContext,
        cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
    ): Observation {
        val battlesRemaining = context.progress.battleRoundsRemaining
            ?: context.progress.upcomingRoundTypes.count { it == RoundCardType.BATTLE }
        val roundsUntilBattle = PhaseProximity.roundsUntilBattle(context)
        val battleIsNext = PhaseProximity.battleIsNext(context)
        val sunlightHeld = context.self.board.sunlight

        if (battlesRemaining <= 0) {
            return Observation(
                battlesRemaining = 0,
                roundsUntilBattle = null,
                battleIsNext = false,
                sunlightHeld = sunlightHeld,
                thirdBestFaceUpBattlePlantBase = null,
                baseFutureActionValue = BASE_FUTURE_ACTION_VALUE,
                battleProximityAdjustment = 0,
                battlesRemainingAdjustment = 0,
                existingSunlightAdjustment = 0,
                visibleBattlePlantAdjustment = 0,
                finalPriority = 0
            )
        }

        val battleProximityAdjustment = when (roundsUntilBattle) {
            1 -> BATTLE_NEXT_BONUS
            2 -> BATTLE_ONE_ROUND_AWAY_BONUS
            else -> 0
        }
        val battlesRemainingAdjustment =
            (battlesRemaining * PER_BATTLE_REMAINING_BONUS).coerceAtMost(MAX_BATTLE_REMAINING_BONUS)

        var existingSunlightAdjustment = -sunlightHeld * STORED_SUNLIGHT_PENALTY
        if (sunlightHeld > 0 && sunlightHeld >= battlesRemaining) {
            existingSunlightAdjustment -= COVERED_REMAINING_BATTLES_PENALTY
        }

        // Two ordinary Battle Main Actions already exist.  A third face-up Plant
        // with meaningful visible Battle value is the clearest shallow signal
        // that another Main Action can unlock something the normal action budget
        // cannot comfortably cover.  This intentionally uses calibrated card
        // role/base values rather than predicting future Strike targets.
        val faceUpBattleBases = context.self.board.creature
            .asSequence()
            .filter { it.isFaceUp }
            .mapNotNull { card -> cardScorers.findByName(card.name)?.battlePlayBase }
            .sortedDescending()
            .toList()
        val thirdBest = faceUpBattleBases.getOrNull(NORMAL_MAIN_ACTIONS)
        val visibleBattlePlantAdjustment = if (battleIsNext && thirdBest != null) {
            ((thirdBest - MEANINGFUL_BATTLE_PLANT_FLOOR) / 3)
                .coerceIn(0, MAX_VISIBLE_PLANT_BONUS)
        } else {
            0
        }

        val finalPriority = (
            BASE_FUTURE_ACTION_VALUE +
                battleProximityAdjustment +
                battlesRemainingAdjustment +
                existingSunlightAdjustment +
                visibleBattlePlantAdjustment
            ).coerceAtLeast(0)

        return Observation(
            battlesRemaining = battlesRemaining,
            roundsUntilBattle = roundsUntilBattle,
            battleIsNext = battleIsNext,
            sunlightHeld = sunlightHeld,
            thirdBestFaceUpBattlePlantBase = thirdBest,
            baseFutureActionValue = BASE_FUTURE_ACTION_VALUE,
            battleProximityAdjustment = battleProximityAdjustment,
            battlesRemainingAdjustment = battlesRemainingAdjustment,
            existingSunlightAdjustment = existingSunlightAdjustment,
            visibleBattlePlantAdjustment = visibleBattlePlantAdjustment,
            finalPriority = finalPriority
        )
    }

    fun score(
        context: DecisionContext,
        cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
    ): PriorityScore {
        val observed = observe(context, cardScorers)
        var score = PriorityScore(observed.baseFutureActionValue)
        if (observed.battlesRemaining <= 0) {
            return score.adjusted(-observed.baseFutureActionValue, "No Battle remains for stored Sunlight")
        }
        if (observed.battlesRemainingAdjustment != 0) {
            score = score.adjusted(
                observed.battlesRemainingAdjustment,
                "${observed.battlesRemaining} future Battle(s) can use banked Sunlight"
            )
        }
        if (observed.battleProximityAdjustment != 0) {
            score = score.adjusted(
                observed.battleProximityAdjustment,
                if (observed.battleIsNext) "Battle is next" else "Battle is one round away"
            )
        }
        if (observed.existingSunlightAdjustment != 0) {
            score = score.adjusted(
                observed.existingSunlightAdjustment,
                "${observed.sunlightHeld} Sunlight already banked; preserve diminishing returns"
            )
        }
        if (observed.visibleBattlePlantAdjustment != 0) {
            score = score.adjusted(
                observed.visibleBattlePlantAdjustment,
                "A third face-up Battle Plant gives extra Main Action capacity visible value"
            )
        }
        return score
    }
}
