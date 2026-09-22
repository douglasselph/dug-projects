package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.BattleSquare
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.abs

/**
 * Projects one known or expected die value into every currently legal Strike Row.
 *
 * This helper is deliberately information-boundary agnostic: callers say whether
 * [dieValue] is an EXPECTED pre-roll value or an ACTUAL post-roll value. It never
 * rolls a die and never consumes mechanical RNG. B7 uses EXPECTED analysis for the
 * First-Main Draw comparison; B8 uses the same projection for actual placement.
 */
class BattleDiePlacementAnalyzer(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val actionAnalyzer: BattleActionAnalyzer = BattleActionAnalyzer(
        swingEvaluator = BattleSwingEvaluator(policy)
    )
) {
    operator fun invoke(
        context: DecisionContext,
        dieValue: Double,
        mode: BattleAnalysisMode,
        legalRows: Collection<StrikeRow>? = null
    ): List<BattleActionAnalysis<StrikeRow>> {
        require(dieValue > 0.0) {
            "Projected Battle die value must be positive: $dieValue"
        }
        val battle = context.battle ?: return emptyList()
        val actorId = context.self.id
        val allowedRows = legalRows?.toSet()

        return battle.rows.mapNotNull { rowView ->
            val row = rowView.row
            if (allowedRows != null && row !in allowedRows) return@mapNotNull null
            val actor = rowView.forPlayer(actorId) ?: return@mapNotNull null
            if (
                rowView.closed ||
                actor.withdrawn ||
                actor.dice.size >= BattleSquare.MAX_DICE
            ) {
                return@mapNotNull null
            }

            val before = rowAssessor(context, row)
            if (!before.available) return@mapNotNull null

            val after = projectedAfterAddingDie(
                context = context,
                before = before,
                dieValue = dieValue
            )
            actionAnalyzer(
                context = context,
                candidate = BattleActionRealization(
                    realization = row,
                    mode = mode,
                    rowChanges = listOf(
                        BattleActionRowChange(
                            before = BattleActionRowState.from(before),
                            after = after
                        )
                    )
                )
            )
        }
    }

    private fun projectedAfterAddingDie(
        context: DecisionContext,
        before: BattleRowAssessment,
        dieValue: Double
    ): BattleActionRowState {
        val ownAfter = before.ownTotal + dieValue
        val opponents = before.participatingOpponentTotals
            .mapValues { (_, total) -> total.toDouble() }
        val allTotals = opponents.values + ownAfter
        val high = allTotals.maxOrNull()
        val everyoneTied =
            allTotals.size > 1 &&
                high != null &&
                allTotals.all { total -> abs(total - high) <= EPSILON }
        val currentlyWinning =
            !everyoneTied && high != null && abs(ownAfter - high) <= EPSILON

        val scoreBenchmark = opponents.values.maxOrNull()
        val scoreMargin = scoreBenchmark?.let { ownAfter - it }

        val battle = requireNotNull(context.battle)
        val liveThreatTotal = battle
            .row(before.row)
            .players
            .asSequence()
            .filterNot { it.withdrawn }
            .filterNot { it.playerId == context.self.id }
            .filterNot { battle.isDone(it.playerId) }
            .maxOfOrNull { it.total.toDouble() }
        val liveThreatMargin = liveThreatTotal?.let { ownAfter - it }

        val woundRisk =
            !currentlyWinning &&
                !everyoneTied &&
                high != null &&
                high - ownAfter >= WOUND_MARGIN - EPSILON

        val securedForNow =
            currentlyWinning &&
                (liveThreatTotal == null ||
                    requireNotNull(liveThreatMargin) >=
                    policy.battleSecuredLead(context) - EPSILON)

        return BattleActionRowState(
            row = before.row,
            available = true,
            currentlyWinning = currentlyWinning,
            woundRisk = woundRisk,
            securedForNow = securedForNow,
            scoreMargin = scoreMargin,
            liveThreatMargin = liveThreatMargin,
            ownTotal = ownAfter,
            opponentTotals = opponents
        )
    }

    private companion object {
        const val WOUND_MARGIN = 5.0
        const val EPSILON = 1e-9
    }
}
