package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.RootWellBattleChoice
import kotlin.math.abs

/**
 * Expected complete-branch analysis for Root Well's Battle choice.
 *
 * Root Well commits either two own dice or one opponent die before any reroll
 * happens. This helper therefore compares the complete legal branches using fair
 * die expectation only. It never consumes mechanical RNG and never changes the
 * selected targets after a random result becomes known.
 */
class BattleRootWellTargetAnalyzer(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val actionAnalyzer: BattleActionAnalyzer = BattleActionAnalyzer(
        swingEvaluator = BattleSwingEvaluator(policy)
    )
) {
    operator fun invoke(
        context: DecisionContext,
        choice: RootWellBattleChoice
    ): BattleActionAnalysis<RootWellBattleChoice>? {
        val battle = context.battle ?: return null
        val actorId = context.self.id

        val ownChanges = linkedMapOf<dugsolutions.leaf.v35.battle.domain.StrikeRow, Double>()
        val opponentChanges = linkedMapOf<dugsolutions.leaf.v35.battle.domain.StrikeRow, MutableMap<PlayerId, Double>>()

        when (choice) {
            is RootWellBattleChoice.OwnDice -> {
                choice.dice.forEach { dieChoice ->
                    if (dieChoice.ownerId != actorId) return null
                    val change = DieValueHeuristics.expectedRerollGain(
                        dieChoice.die.sides,
                        dieChoice.die.value
                    )
                    ownChanges[dieChoice.row] = (ownChanges[dieChoice.row] ?: 0.0) + change
                }
            }

            is RootWellBattleChoice.OpponentDie -> {
                val dieChoice = choice.die
                if (dieChoice.ownerId == actorId) return null
                val change = DieValueHeuristics.expectedRerollGain(
                    dieChoice.die.sides,
                    dieChoice.die.value
                )
                opponentChanges
                    .getOrPut(dieChoice.row) { linkedMapOf() }
                    .merge(dieChoice.ownerId, change, Double::plus)
            }
        }

        val changedRows = (ownChanges.keys + opponentChanges.keys).distinct()
        if (changedRows.isEmpty()) return null

        val rowChanges = changedRows.map { row ->
            val before = rowAssessor(context, row)
            if (!before.available) return null
            BattleActionRowChange(
                before = BattleActionRowState.from(before),
                after = projectedState(
                    context = context,
                    before = before,
                    ownChange = ownChanges[row] ?: 0.0,
                    opponentChanges = opponentChanges[row].orEmpty()
                )
            )
        }

        return actionAnalyzer(
            context = context,
            candidate = BattleActionRealization(
                realization = choice,
                mode = BattleAnalysisMode.EXPECTED,
                rowChanges = rowChanges
            )
        )
    }

    private fun projectedState(
        context: DecisionContext,
        before: BattleRowAssessment,
        ownChange: Double,
        opponentChanges: Map<PlayerId, Double>
    ): BattleActionRowState {
        val ownAfter = before.ownTotal + ownChange
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
