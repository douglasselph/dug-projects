package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import kotlin.math.abs

/**
 * Expected target analysis for Gust of Petals.
 *
 * The actor chooses which die to reroll before RNG. The Strike Row for the
 * forced opposing rerolls is chosen only after that actor reroll resolves.
 * Target valuation therefore uses honest expectation for the actor reroll and
 * the best currently visible expected later row branch, without consuming RNG
 * or committing the later row choice.
 */
class BattleGustOfPetalsTargetAnalyzer(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val actionAnalyzer: BattleActionAnalyzer = BattleActionAnalyzer(
        swingEvaluator = BattleSwingEvaluator(policy)
    )
) {
    operator fun <T> invoke(
        context: DecisionContext,
        choice: EffectDieChoice,
        realization: T
    ): BattleActionAnalysis<T>? {
        val battle = context.battle ?: return null
        val actorId = context.self.id
        val targetRow = battle.rows.firstOrNull { row ->
            row.forPlayer(actorId)?.dice?.any { it.handIndex == choice.index } == true
        } ?: return null
        if (targetRow.closed) return null

        val ownExpectedChange = DieValueHeuristics.expectedRerollGain(
            sides = choice.sides,
            value = choice.value
        )
        val openRows = battle.rows.filterNot { it.closed }
        if (openRows.isEmpty()) return null

        return openRows.mapNotNull { collateralRow ->
            analyzeBranch(
                context = context,
                realization = realization,
                targetRow = targetRow,
                targetChoice = choice,
                ownExpectedChange = ownExpectedChange,
                collateralRow = collateralRow
            )
        }.maxWithOrNull(
            compareBy<BattleActionAnalysis<T>> { it.tacticalValue }
                .thenBy { it.improvementStepCount }
        )
    }

    private fun <T> analyzeBranch(
        context: DecisionContext,
        realization: T,
        targetRow: BattleRowView,
        targetChoice: EffectDieChoice,
        ownExpectedChange: Double,
        collateralRow: BattleRowView
    ): BattleActionAnalysis<T>? {
        val changedRows = linkedSetOf(targetRow.row, collateralRow.row)
        val rowChanges = changedRows.map { row ->
            val before = rowAssessor(context, row)
            if (!before.available) return null

            val ownChange = if (row == targetRow.row) ownExpectedChange else 0.0
            val opponentChanges = if (row == collateralRow.row) {
                expectedOpponentRerollChanges(
                    context = context,
                    rowView = collateralRow,
                    targetRow = targetRow.row,
                    targetChoice = targetChoice,
                    ownExpectedChange = ownExpectedChange
                )
            } else {
                emptyMap()
            }

            BattleActionRowChange(
                before = BattleActionRowState.from(before),
                after = projectedState(
                    context = context,
                    before = before,
                    ownChange = ownChange,
                    opponentChanges = opponentChanges
                )
            )
        }

        return actionAnalyzer(
            context = context,
            candidate = BattleActionRealization(
                realization = realization,
                mode = BattleAnalysisMode.EXPECTED,
                rowChanges = rowChanges
            )
        )
    }

    /**
     * Mirrors Gust of Petals' forced-target rule using the expected actor reroll.
     * The executor compares each opposing die with the actor's lowest die in the
     * chosen row before any opposing reroll resolves.
     */
    private fun expectedOpponentRerollChanges(
        context: DecisionContext,
        rowView: BattleRowView,
        targetRow: StrikeRow,
        targetChoice: EffectDieChoice,
        ownExpectedChange: Double
    ): Map<PlayerId, Double> {
        val actorId = context.self.id
        val ownDice = rowView.forPlayer(actorId)?.dice.orEmpty()
        if (ownDice.isEmpty()) return emptyMap()

        val lowestOwn = ownDice.minOf { die ->
            if (rowView.row == targetRow && die.handIndex == targetChoice.index) {
                die.value + ownExpectedChange
            } else {
                die.value.toDouble()
            }
        }

        return rowView.players
            .asSequence()
            .filterNot { it.playerId == actorId }
            .associate { player ->
                player.playerId to player.dice.sumOf { die ->
                    if (die.value > lowestOwn) {
                        DieValueHeuristics.expectedRerollGain(die.sides, die.value)
                    } else {
                        0.0
                    }
                }
            }
            .filterValues { abs(it) > EPSILON }
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
