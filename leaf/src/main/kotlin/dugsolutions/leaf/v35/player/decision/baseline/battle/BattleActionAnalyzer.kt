package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.abs

/** Whether an action realization is exact, expected before RNG, or known after RNG. */
enum class BattleAnalysisMode {
    DETERMINISTIC,
    EXPECTED,
    ACTUAL
}

/**
 * Projection-friendly state used by [BattleActionAnalyzer].
 *
 * It contains the union of facts required by Battle Swing and Strike-VP analysis.
 * Actual [BattleRowAssessment] values convert directly, while expected candidate
 * analysis may supply fractional totals/margins without mutating gameplay state.
 */
data class BattleActionRowState(
    val row: StrikeRow,
    val available: Boolean,
    val currentlyWinning: Boolean,
    val woundRisk: Boolean,
    val securedForNow: Boolean,
    val scoreMargin: Double?,
    val liveThreatMargin: Double?,
    val ownTotal: Double,
    val opponentTotals: Map<PlayerId, Double>
) {
    fun toSwingState(): BattleSwingState =
        BattleSwingState(
            row = row,
            available = available,
            currentlyWinning = currentlyWinning,
            woundRisk = woundRisk,
            securedForNow = securedForNow,
            scoreMargin = scoreMargin,
            liveThreatMargin = liveThreatMargin
        )

    fun toVpState(): BattleVpState =
        BattleVpState(
            row = row,
            available = available,
            currentlyWinning = currentlyWinning,
            ownTotal = ownTotal,
            opponentTotals = opponentTotals
        )

    companion object {
        fun from(assessment: BattleRowAssessment): BattleActionRowState =
            BattleActionRowState(
                row = assessment.row,
                available = assessment.available,
                currentlyWinning = assessment.currentlyWinning,
                woundRisk = assessment.woundRisk,
                securedForNow = assessment.securedForNow,
                scoreMargin = assessment.scoreMargin?.toDouble(),
                liveThreatMargin = assessment.liveThreatMargin?.toDouble(),
                ownTotal = assessment.ownTotal.toDouble(),
                opponentTotals = assessment.participatingOpponentTotals
                    .mapValues { (_, total) -> total.toDouble() }
            )
    }
}

/** One row changed by a candidate realization. */
data class BattleActionRowChange(
    val before: BattleActionRowState,
    val after: BattleActionRowState
) {
    init {
        require(before.row == after.row) {
            "Battle action change must compare the same row: ${before.row} vs ${after.row}"
        }
    }

    companion object {
        fun from(
            before: BattleRowAssessment,
            after: BattleRowAssessment
        ): BattleActionRowChange =
            BattleActionRowChange(
                before = BattleActionRowState.from(before),
                after = BattleActionRowState.from(after)
            )
    }
}

/**
 * One complete legal target/branch/placement realization ready for tactical analysis.
 *
 * [realization] is deliberately generic: later callers may retain an action, target,
 * pair, row, or small action-target object without coupling this shared layer to every
 * Effect request type. [mode] records the information boundary used to produce the
 * projection; the analyzer itself never consumes mechanical RNG.
 */
data class BattleActionRealization<T>(
    val realization: T,
    val mode: BattleAnalysisMode,
    val rowChanges: List<BattleActionRowChange>
)

/** Shared factual analysis of one complete immediate Battle realization. */
data class BattleActionAnalysis<T>(
    val realization: T,
    val mode: BattleAnalysisMode,
    val swing: BattleActionSwing,
    val vpImpact: BattleVpImpactResult,
    val improvementStepCount: Int
) {
    val tacticalValue: Double
        get() = swing.totalValue
}

/**
 * Combines row-level Battle Swing with the simple Strike-VP and improvement-step
 * metrics needed by later Main/Support strategy layers.
 *
 * This class deliberately does NOT decide whether a resource should be spent, whether
 * Final Main should be delayed, or whether an action's intrinsic card value is high.
 * It only describes the immediate projected realization supplied by the caller.
 *
 * Improvement steps are a separate premium-resource gate metric. Unlike Battle Swing,
 * they intentionally count every positive named boundary crossed in a row. Thus a
 * projection from wounded loss to win may count WOUND_PREVENTED + TIE + WIN as three
 * steps even though [BattleSwingEvaluator] correctly scores only WIN_FLIPPED once.
 */
class BattleActionAnalyzer(
    private val swingEvaluator: BattleSwingEvaluator = BattleSwingEvaluator(),
    private val vpImpact: BattleVpImpact = BattleVpImpact()
) {
    operator fun <T> invoke(
        context: DecisionContext,
        candidate: BattleActionRealization<T>
    ): BattleActionAnalysis<T> {
        val swing = swingEvaluator(
            context = context,
            changes = candidate.rowChanges.map { change ->
                BattleSwingChange(
                    before = change.before.toSwingState(),
                    after = change.after.toSwingState()
                )
            }
        )
        return BattleActionAnalysis(
            realization = candidate.realization,
            mode = candidate.mode,
            swing = swing,
            vpImpact = vpImpact(candidate.rowChanges),
            improvementStepCount = candidate.rowChanges.sumOf(::positiveImprovementSteps)
        )
    }

    private fun positiveImprovementSteps(change: BattleActionRowChange): Int {
        val before = change.before
        val after = change.after
        var steps = 0

        if (before.woundRisk && !after.woundRisk) {
            steps++
        }
        if (before.scoreMargin.isNegative() && after.scoreMargin.isNonNegative()) {
            // Crossing the equality boundary matters even when the resulting equal score
            // is already a shared win under multiplayer Strike semantics.
            steps++
        }
        if (!before.currentlyWinning && after.currentlyWinning) {
            steps++
        }
        if (
            before.currentlyWinning &&
            after.currentlyWinning &&
            before.scoreMargin.isBelow(WOUND_MARGIN) &&
            after.scoreMargin.isAtLeast(WOUND_MARGIN)
        ) {
            steps++
        }
        if (!before.securedForNow && after.securedForNow) {
            steps++
        }
        return steps
    }

    private fun Double?.isNegative(): Boolean = this != null && this < -EPSILON
    private fun Double?.isNonNegative(): Boolean = this != null && this >= -EPSILON
    private fun Double?.isBelow(value: Double): Boolean = this != null && this < value - EPSILON
    private fun Double?.isAtLeast(value: Double): Boolean = this != null && this >= value - EPSILON

    private companion object {
        const val WOUND_MARGIN = 5.0
        const val EPSILON = 1e-9
    }
}
