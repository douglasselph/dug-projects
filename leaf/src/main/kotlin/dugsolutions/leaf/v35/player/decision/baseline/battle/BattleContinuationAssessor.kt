package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Why Human Baseline should remain active or commit its Final Main. */
enum class BattleContinuationReason {
    NO_RELEVANT_ROWS,
    ALL_RELEVANT_ROWS_SECURED,
    NO_SUPPORT_MOVES,
    INDIVIDUALLY_WORTHWHILE_SUPPORT,
    MEANINGFUL_CUMULATIVE_PATH,
    NO_WORTHWHILE_PATH
}

/** Fresh Step-5 continuation facts; this is never retained between passes. */
data class BattleContinuationAssessment(
    val relevantRows: List<BattleRowAssessment>,
    val supportCapacity: BattleSupportCapacity,
    val reachability: BattleSupportReachabilityAssessment,
    val individuallyWorthwhileSupportExists: Boolean,
    val reason: BattleContinuationReason
) {
    val allRelevantRowsSecured: Boolean =
        relevantRows.isNotEmpty() && relevantRows.all { it.securedForNow }

    val hasSupportMoves: Boolean
        get() = supportCapacity.totalMoves > 0

    val hasMeaningfulCumulativePath: Boolean
        get() = reachability.hasMeaningfulPath

    val shouldContinue: Boolean
        get() = reason == BattleContinuationReason.INDIVIDUALLY_WORTHWHILE_SUPPORT ||
            reason == BattleContinuationReason.MEANINGFUL_CUMULATIVE_PATH
}

/**
 * Applies the Human Baseline Step-5 continuation gate to a fresh Battle snapshot.
 *
 * B10 accepts the individual-worthwhile result as an input because B11/B12 own
 * direct and enabling Support analysis. This helper combines that result with
 * B10 cumulative reachability, but does not choose a Support or Final Main action;
 * B14 owns that orchestration.
 */
class BattleContinuationAssessor(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val capacityAssessor: BattleSupportCapacityAssessor =
        BattleSupportCapacityAssessor(),
    private val supportReachability: BattleSupportReachability =
        BattleSupportReachability(policy)
) {
    operator fun invoke(
        context: DecisionContext,
        legalChoices: Iterable<BattleTurnAction>,
        individuallyWorthwhileSupportExists: Boolean = false
    ): BattleContinuationAssessment {
        val battle = requireNotNull(context.battle) {
            "Battle continuation requires a Battle decision context"
        }
        val choices = legalChoices.toList()
        val relevantRows = battle.rows
            .map { rowAssessor(context, it.row) }
            .filter { it.available }
        val supportCapacity = capacityAssessor(
            context = context,
            legalChoices = choices
        ).own
        val reachability = supportReachability(context, supportCapacity)
        val allRelevantRowsSecured =
            relevantRows.isNotEmpty() && relevantRows.all { it.securedForNow }

        val reason = when {
            relevantRows.isEmpty() -> BattleContinuationReason.NO_RELEVANT_ROWS
            allRelevantRowsSecured -> BattleContinuationReason.ALL_RELEVANT_ROWS_SECURED
            supportCapacity.totalMoves == 0 -> BattleContinuationReason.NO_SUPPORT_MOVES
            individuallyWorthwhileSupportExists ->
                BattleContinuationReason.INDIVIDUALLY_WORTHWHILE_SUPPORT
            reachability.hasMeaningfulPath ->
                BattleContinuationReason.MEANINGFUL_CUMULATIVE_PATH
            else -> BattleContinuationReason.NO_WORTHWHILE_PATH
        }

        return BattleContinuationAssessment(
            relevantRows = relevantRows,
            supportCapacity = supportCapacity,
            reachability = reachability,
            individuallyWorthwhileSupportExists = individuallyWorthwhileSupportExists,
            reason = reason
        )
    }
}
