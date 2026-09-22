package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Policy facts for resolving one Strike immediately with Wisp's Resolve. */
data class BattleImmediateStrikeResolveEvaluation(
    val row: StrikeRow,
    val leadOverEveryOpponent: Boolean,
    val activeContenderSupport: Map<dugsolutions.leaf.v35.player.PlayerId, BattleSupportCapacity>,
    val minimumLead: Int,
    val minimumOpponentSupport: Int,
    val currentStrikeVp: Int
) {
    val passesPolicy: Boolean
        get() = leadOverEveryOpponent &&
            activeContenderSupport.values.all { it.totalMoves >= minimumOpponentSupport }
}

/**
 * Evaluates the dedicated Wisp's Resolve lock-in policy using authoritative Done
 * state and normalized public Support capacity. Opponent Wisp identities remain
 * hidden; capacity counts only the public number of held Wisps.
 */
class BattleImmediateStrikeResolveEvaluator(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val capacityAssessor: BattleSupportCapacityAssessor = BattleSupportCapacityAssessor(),
    private val vpImpact: BattleVpImpact = BattleVpImpact()
) {
    operator fun invoke(
        context: DecisionContext,
        row: StrikeRow
    ): BattleImmediateStrikeResolveEvaluation? {
        val battle = context.battle ?: return null
        val assessment = rowAssessor(context, row)
        if (!assessment.available || assessment.participatingOpponentTotals.isEmpty()) return null

        val minLead = policy.battleImmediateResolveMinLead(context)
        val activeContenders = assessment.participatingOpponentTotals.keys
            .filterNot(battle::isDone)
            .toSet()
        val capacities = capacityAssessor(
            context = context,
            legalChoices = emptyList(),
            relevantLiveThreatOpponentIds = activeContenders
        ).relevantLiveThreatOpponents

        return BattleImmediateStrikeResolveEvaluation(
            row = row,
            leadOverEveryOpponent = assessment.participatingOpponentTotals.values.all {
                assessment.ownTotal - it >= minLead
            },
            activeContenderSupport = capacities,
            minimumLead = minLead,
            minimumOpponentSupport = policy.battleImmediateResolveMinOpponentSupport(context),
            currentStrikeVp = vpImpact(assessment, assessment).beforeVp
        )
    }

    fun evaluateAll(
        context: DecisionContext,
        legalRows: Collection<StrikeRow>
    ): List<BattleImmediateStrikeResolveEvaluation> =
        legalRows.mapNotNull { invoke(context, it) }
}
