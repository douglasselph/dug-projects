package dugsolutions.leaf.v35.player.decision.baseline.scoring

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase

/**
 * C01: immutable, phase-aware explanation of one Human Baseline action candidate.
 *
 * Numeric components share the CURRENT decision-preference unit. Future
 * expected-pip/Battle calibration must convert to that unit before constructing
 * an ActionValue; this type does not pretend legacy priorities are pip values.
 * Random influences are recorded as realized values, never resampled here.
 * This is diagnostic/value infrastructure; it does not change legal actions,
 * mechanical effects, or learned policy behavior.
 */
data class ActionValue<T>(
    val phase: CardPhase,
    val effectiveEffect: GameEffect?,
    val candidate: T,
    val target: String? = null,
    val legal: Boolean,
    val calculatedBenefit: Double,
    val contextualBonuses: Double = 0.0,
    val contextualCosts: Double = 0.0,
    val uncertainty: Double = 0.0,
    val variability: Double = 0.0,
    val notes: List<String> = emptyList()
) {
    init {
        require(listOf(calculatedBenefit, contextualBonuses, contextualCosts, uncertainty, variability)
            .all { it.isFinite() }) { "Action valuation components must be finite" }
        require(contextualBonuses >= 0.0) { "Bonuses must be nonnegative" }
        require(contextualCosts >= 0.0) { "Costs must be nonnegative" }
        require(target == null || target.isNotBlank()) { "Target must not be blank" }
        require(notes.all { it.isNotBlank() }) { "Notes must not contain blank entries" }
    }

    val finalPreference: Double
        get() = if (legal) calculatedBenefit + contextualBonuses - contextualCosts + uncertainty + variability
                else Double.NEGATIVE_INFINITY

    /** Illegal candidates must never be compared or converted into selectable choices. */
    fun requireLegal(): ActionValue<T> = apply {
        require(legal) { "Cannot select an illegal action candidate" }
    }

    companion object {
        /**
         * Faithfully describe an existing integer priority without interpreting it
         * as expected pips or introducing additional randomness.
         * Positive adjustments are bonuses; negative adjustments are costs.
         */
        fun <T> fromPriorityScore(
            phase: CardPhase,
            effectiveEffect: GameEffect?,
            candidate: T,
            score: PriorityScore,
            target: String? = null,
            legal: Boolean = true
        ): ActionValue<T> = ActionValue(
            phase = phase,
            effectiveEffect = effectiveEffect,
            candidate = candidate,
            target = target,
            legal = legal,
            calculatedBenefit = score.base.toDouble(),
            contextualBonuses = score.adjustments.filter { it.amount > 0 }.sumOf { it.amount }.toDouble(),
            contextualCosts = -score.adjustments.filter { it.amount < 0 }.sumOf { it.amount }.toDouble(),
            notes = listOf("Legacy priority points; not yet calibrated expected pips") +
                score.adjustments.map { "${it.reason}: ${it.amount}" }
        )

        fun <T> fromScoredChoice(
            phase: CardPhase,
            effectiveEffect: GameEffect?,
            choice: ScoredChoice<T>,
            target: String? = null
        ): ActionValue<T> = fromPriorityScore(
            phase, effectiveEffect, choice.choice, choice.score, target
        )
    }
}
