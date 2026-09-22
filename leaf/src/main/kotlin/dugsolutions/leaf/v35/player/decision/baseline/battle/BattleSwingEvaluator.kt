package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.abs

/**
 * Human Baseline's named immediate Battle transitions, ordered weakest to strongest.
 *
 * [tier] is multiplied by [HumanBaselinePolicy.battleTransitionScale] to produce the
 * transition base. The default scale is 100, yielding 100/200/300/400/500 for the
 * five positive transitions. These are tactical strategy values, not VP.
 */
enum class BattleTransition(val tier: Int) {
    NONE(0),
    SECURED_CREATED(1),
    WOUND_CREATED(2),
    WOUND_PREVENTED(3),
    TIE_ACHIEVED(4),
    WIN_FLIPPED(5)
}

/**
 * Minimal before/after state required to value an immediate change to one Strike Row.
 *
 * This intentionally uses [Double] margins so expected values such as a D8's 4.5 can
 * pass through tactical analysis without early rounding. [BattleRowAssessment] can be
 * converted directly; later action-analysis checkpoints may construct projected states
 * from expected values without mutating real game state or consuming mechanical RNG.
 */
data class BattleSwingState(
    val row: StrikeRow,
    val available: Boolean,
    val currentlyWinning: Boolean,
    val woundRisk: Boolean,
    val securedForNow: Boolean,
    val scoreMargin: Double?,
    val liveThreatMargin: Double?
) {
    companion object {
        fun from(assessment: BattleRowAssessment): BattleSwingState =
            BattleSwingState(
                row = assessment.row,
                available = assessment.available,
                currentlyWinning = assessment.currentlyWinning,
                woundRisk = assessment.woundRisk,
                securedForNow = assessment.securedForNow,
                scoreMargin = assessment.scoreMargin?.toDouble(),
                liveThreatMargin = assessment.liveThreatMargin?.toDouble()
            )
    }
}

/** One affected row in a hypothetical or actual immediate Battle action. */
data class BattleSwingChange(
    val before: BattleSwingState,
    val after: BattleSwingState
) {
    init {
        require(before.row == after.row) {
            "Battle swing change must compare the same row: ${before.row} vs ${after.row}"
        }
    }
}

/**
 * Tactical value of one row changing from [before] to [after].
 *
 * [transitionBase] and [rawSwing] are signed. Harmful movement is represented as the
 * negative of the score its reverse movement would have received. This keeps swaps and
 * collateral effects symmetric without maintaining a second adverse-transition table.
 */
data class BattleSwing(
    val row: StrikeRow,
    val before: BattleSwingState,
    val after: BattleSwingState,
    val transition: BattleTransition,
    val transitionBase: Double,
    val rawSwing: Double,
    val totalValue: Double
)

/** Multi-row result: one strongest contribution per affected row, summed without a cap. */
data class BattleActionSwing(
    val rowSwings: List<BattleSwing>
) {
    val totalValue: Double = rowSwings.sumOf { it.totalValue }
}

/**
 * Converts immediate before/after Battle-row facts into the shared Human Baseline
 * tactical value described in `doc/HUMAN_BASELINE_BATTLE.md`.
 *
 * Rules implemented here:
 * - named positive transitions rank WIN_FLIPPED > TIE_ACHIEVED > WOUND_PREVENTED >
 *   WOUND_CREATED > SECURED_CREATED;
 * - only the strongest named transition contributes a base for a row;
 * - ordinary transitions add Score-Benchmark margin movement as fine detail;
 * - SECURED_CREATED uses Live-Threat margin movement because secured status is defined
 *   against opponents who can still respond;
 * - harmful movement is the negative of the reverse positive movement;
 * - no-transition movement falls back to the raw Score-Benchmark margin delta;
 * - multi-row action value is the uncapped sum of one contribution per row.
 *
 * The evaluator is deliberately factual tactical analysis. Resource conservation,
 * continuation/tempo, and whether Human Baseline should care about a row belong to
 * higher-level strategy layers.
 */
class BattleSwingEvaluator(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy()
) {
    operator fun invoke(
        context: DecisionContext,
        before: BattleRowAssessment,
        after: BattleRowAssessment
    ): BattleSwing =
        invoke(context, BattleSwingState.from(before), BattleSwingState.from(after))

    operator fun invoke(
        context: DecisionContext,
        before: BattleSwingState,
        after: BattleSwingState
    ): BattleSwing {
        require(before.row == after.row) {
            "Battle swing must compare the same row: ${before.row} vs ${after.row}"
        }
        require(before.available && after.available) {
            "Battle swing requires an available row before and after: ${before.row}"
        }

        val positive = positiveTransition(before, after)
        if (positive != BattleTransition.NONE) {
            return scored(context, before, after, positive, direction = 1.0)
        }

        val reversePositive = positiveTransition(after, before)
        if (reversePositive != BattleTransition.NONE) {
            return scored(context, before, after, reversePositive, direction = -1.0)
        }

        val raw = scoreMarginDelta(before, after)
        return BattleSwing(
            row = before.row,
            before = before,
            after = after,
            transition = BattleTransition.NONE,
            transitionBase = 0.0,
            rawSwing = raw,
            totalValue = raw
        )
    }

    operator fun invoke(
        context: DecisionContext,
        changes: Iterable<BattleSwingChange>
    ): BattleActionSwing =
        BattleActionSwing(
            rowSwings = changes.map { change ->
                invoke(context, change.before, change.after)
            }
        )

    private fun scored(
        context: DecisionContext,
        actualBefore: BattleSwingState,
        actualAfter: BattleSwingState,
        transition: BattleTransition,
        direction: Double
    ): BattleSwing {
        val evaluationBefore = if (direction > 0) actualBefore else actualAfter
        val evaluationAfter = if (direction > 0) actualAfter else actualBefore
        val scale = policy.battleTransitionScale(context).toDouble()
        val unsignedBase = transition.tier * scale
        val unsignedRaw = when (transition) {
            BattleTransition.SECURED_CREATED ->
                liveThreatMarginDelta(evaluationBefore, evaluationAfter)
            else ->
                scoreMarginDelta(evaluationBefore, evaluationAfter)
        }.coerceAtLeast(0.0)

        val base = direction * unsignedBase
        val raw = direction * unsignedRaw
        return BattleSwing(
            row = actualBefore.row,
            before = actualBefore,
            after = actualAfter,
            transition = transition,
            transitionBase = base,
            rawSwing = raw,
            totalValue = base + raw
        )
    }

    /** Returns only the strongest positive transition created by before -> after. */
    private fun positiveTransition(
        before: BattleSwingState,
        after: BattleSwingState
    ): BattleTransition = when {
        !before.currentlyWinning && after.currentlyWinning ->
            BattleTransition.WIN_FLIPPED

        !before.currentlyWinning &&
            before.scoreMargin.isNegative() &&
            !after.currentlyWinning &&
            after.scoreMargin.isZero() ->
            BattleTransition.TIE_ACHIEVED

        before.woundRisk && !after.woundRisk ->
            BattleTransition.WOUND_PREVENTED

        before.currentlyWinning &&
            after.currentlyWinning &&
            before.scoreMargin.isBelow(WOUND_MARGIN) &&
            after.scoreMargin.isAtLeast(WOUND_MARGIN) ->
            BattleTransition.WOUND_CREATED

        !before.securedForNow && after.securedForNow ->
            BattleTransition.SECURED_CREATED

        else -> BattleTransition.NONE
    }

    private fun scoreMarginDelta(
        before: BattleSwingState,
        after: BattleSwingState
    ): Double {
        val beforeMargin = before.scoreMargin ?: return 0.0
        val afterMargin = after.scoreMargin ?: return 0.0
        return afterMargin - beforeMargin
    }

    private fun liveThreatMarginDelta(
        before: BattleSwingState,
        after: BattleSwingState
    ): Double {
        val beforeMargin = before.liveThreatMargin ?: return 0.0
        val afterMargin = after.liveThreatMargin ?: return 0.0
        return afterMargin - beforeMargin
    }

    private fun Double?.isNegative(): Boolean = this != null && this < -EPSILON
    private fun Double?.isZero(): Boolean = this != null && abs(this) <= EPSILON
    private fun Double?.isBelow(value: Double): Boolean = this != null && this < value - EPSILON
    private fun Double?.isAtLeast(value: Double): Boolean = this != null && this >= value - EPSILON

    private companion object {
        /** Actual game-rule Wound margin. */
        const val WOUND_MARGIN: Double = 5.0
        const val EPSILON: Double = 1e-9
    }
}
