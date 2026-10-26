package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.baseline.context.PhaseProximity
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCardType
import kotlin.math.roundToInt

/**
 * Value of ACQUIRING a Sunlight token during Cultivation, not the value of an
 * obsolete Raise +3. Battle spending is evaluated separately by
 * SunlightBattleSpendingValue using the actual funded Main Action.
 *
 * This temporary legacy-score bridge uses the same 35 + 4*expected-pips
 * conversion as DrawPriority, so Sunlight's provisional 3..5 pip-equivalent
 * resource estimate can compete with Draw before the master recalibration.
 * Random decision variability is intentionally deferred to the shared DV layer.
 */
object SunlightTokenPriority {
    private const val DRAW_COMPARISON_ORIGIN = 35
    private const val PRIORITY_PER_PIP = 4
    private const val TOKEN_PIP_ESTIMATE = 4.0

    data class Observation(
        val battlesRemaining: Int,
        val roundsUntilBattle: Int?,
        val sunlightHeld: Int,
        val estimatedPipValue: Double,
        val finalPriority: Int
    )

    fun observe(context: DecisionContext): Observation {
        val remaining = context.progress.battleRoundsRemaining
            ?: context.progress.upcomingRoundTypes.count { it == RoundCardType.BATTLE }
        val untilBattle = PhaseProximity.roundsUntilBattle(context)
        val held = context.self.board.sunlight
        val estimate = when {
            remaining <= 0 -> 0.0
            held >= remaining -> 1.0 // token is surplus relative to remaining Battles
            else -> (TOKEN_PIP_ESTIMATE + (if (untilBattle == 1) 0.5 else 0.0) - held * 1.0)
                .coerceIn(1.0, 5.0)
        }
        return Observation(
            battlesRemaining = remaining,
            roundsUntilBattle = untilBattle,
            sunlightHeld = held,
            estimatedPipValue = estimate,
            finalPriority = if (remaining <= 0) 0
                else DRAW_COMPARISON_ORIGIN + (PRIORITY_PER_PIP * estimate).roundToInt()
        )
    }

    fun score(context: DecisionContext): PriorityScore {
        val observation = observe(context)
        if (observation.battlesRemaining <= 0) return PriorityScore(0)
            .adjusted(0, "No remaining Battle in which to spend a Sunlight token")
        return PriorityScore(DRAW_COMPARISON_ORIGIN)
            .adjusted(
                observation.finalPriority - DRAW_COMPARISON_ORIGIN,
                "Gain one Sunlight token: ${observation.estimatedPipValue} future-action pip equivalents"
            )
    }
}
