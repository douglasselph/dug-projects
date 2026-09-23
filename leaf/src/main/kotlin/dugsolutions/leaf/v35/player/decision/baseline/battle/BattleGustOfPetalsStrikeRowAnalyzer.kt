package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.abs

/**
 * Expected downstream Strike-row analysis for Gust of Petals after the actor's
 * first reroll has already resolved.
 *
 * The live Battle context therefore contains the actor's actual rerolled die.
 * For each legal row, this helper identifies the opposing dice that the rules
 * would force to reroll (value greater than the actor's current lowest die in
 * that row), projects only those opponent rerolls by fair expectation, and
 * evaluates the complete resulting row through shared Battle analysis.
 *
 * No mechanical RNG is consumed and no post-reroll result is pre-resolved.
 */
class BattleGustOfPetalsStrikeRowAnalyzer(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val actionAnalyzer: BattleActionAnalyzer = BattleActionAnalyzer(
        swingEvaluator = BattleSwingEvaluator(policy)
    )
) {
    operator fun invoke(
        context: DecisionContext,
        row: StrikeRow
    ): BattleActionAnalysis<StrikeRow>? {
        val battle = context.battle ?: return null
        val rowView = battle.row(row)
        val before = rowAssessor(context, row)
        if (!before.available || rowView.closed) return null

        val actorId = context.self.id
        val ownDice = rowView.forPlayer(actorId)?.dice.orEmpty()
        if (ownDice.isEmpty()) return null
        val lowestOwnValue = ownDice.minOf { it.value }

        val opponentChanges = rowView.players
            .asSequence()
            .filterNot { it.playerId == actorId }
            .associate { player ->
                player.playerId to player.dice.sumOf { die ->
                    if (die.value > lowestOwnValue) {
                        DieValueHeuristics.expectedRerollGain(die.sides, die.value)
                    } else {
                        0.0
                    }
                }
            }
            .filterValues { abs(it) > EPSILON }

        val after = projectedState(
            context = context,
            before = before,
            opponentChanges = opponentChanges
        )

        return actionAnalyzer(
            context = context,
            candidate = BattleActionRealization(
                realization = row,
                mode = BattleAnalysisMode.EXPECTED,
                rowChanges = listOf(
                    BattleActionRowChange(
                        before = BattleActionRowState.from(before),
                        after = after
                    )
                )
            )
        )
    }

    private fun projectedState(
        context: DecisionContext,
        before: BattleRowAssessment,
        opponentChanges: Map<PlayerId, Double>
    ): BattleActionRowState {
        val ownAfter = before.ownTotal.toDouble()
        val opponentsAfter = before.participatingOpponentTotals.mapValues { (playerId, total) ->
            total.toDouble() + (opponentChanges[playerId] ?: 0.0)
        }
        val allTotals = opponentsAfter.values + ownAfter
        val high = allTotals.maxOrNull()
        val everyoneTied =
            allTotals.size > 1 &&
                high != null &&
                allTotals.all { total -> abs(total - high) <= EPSILON }
        val currentlyWinning =
            !everyoneTied && high != null && abs(ownAfter - high) <= EPSILON
        val scoreMargin = opponentsAfter.values.maxOrNull()?.let { ownAfter - it }

        val battle = requireNotNull(context.battle)
        val liveThreatTotal = battle.row(before.row).players
            .asSequence()
            .filterNot { it.withdrawn }
            .filterNot { it.playerId == context.self.id }
            .filterNot { battle.isDone(it.playerId) }
            .mapNotNull { player -> opponentsAfter[player.playerId] }
            .maxOrNull()
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
            opponentTotals = opponentsAfter
        )
    }

    private companion object {
        const val WOUND_MARGIN = 5.0
        const val EPSILON = 1e-9
    }
}
