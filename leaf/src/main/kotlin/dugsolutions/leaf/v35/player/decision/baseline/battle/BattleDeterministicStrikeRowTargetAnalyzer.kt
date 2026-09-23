package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.abs

/**
 * Deterministic Battle analysis for effects whose downstream choice is a Strike Row.
 *
 * Root & Scoot's row branch withdraws the actor completely from that Strike, so the
 * projection removes the actor's winning/VP/wound participation instead of treating
 * the choice as a generic "needy row" bonus. Vine & Punishment projects the exact
 * capped -3 adjustment to every participating opposing die in the selected row.
 *
 * Legal-row generation remains owned by the effect handlers. This helper only values
 * an already-legal row and never mutates live Battle state.
 */
class BattleDeterministicStrikeRowTargetAnalyzer(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val actionAnalyzer: BattleActionAnalyzer = BattleActionAnalyzer(
        swingEvaluator = BattleSwingEvaluator(policy)
    )
) {
    operator fun invoke(
        context: DecisionContext,
        effect: GameEffect,
        row: StrikeRow
    ): BattleActionAnalysis<StrikeRow>? {
        val before = rowAssessor(context, row)
        if (!before.available) return null

        val after = when (effect) {
            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE ->
                afterActorWithdrawal(context, before)

            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3 ->
                afterOpposingDiceReducedByThree(context, before)

            else -> return null
        }

        return actionAnalyzer(
            context = context,
            candidate = BattleActionRealization(
                realization = row,
                mode = BattleAnalysisMode.DETERMINISTIC,
                rowChanges = listOf(
                    BattleActionRowChange(
                        before = BattleActionRowState.from(before),
                        after = after
                    )
                )
            )
        )
    }

    /**
     * Withdrawal removes the actor from Strike participation. The comparison keeps
     * the row analysis-shaped so shared Battle Swing can express the opportunity cost
     * of abandoning a win/close contest and the genuine benefit of avoiding a Wound.
     */
    private fun afterActorWithdrawal(
        context: DecisionContext,
        before: BattleRowAssessment
    ): BattleActionRowState {
        val opponentTotals = before.participatingOpponentTotals
            .mapValues { (_, total) -> total.toDouble() }
        val benchmark = opponentTotals.values.maxOrNull()
        val battle = requireNotNull(context.battle)
        val liveThreat = battle.row(before.row).players
            .asSequence()
            .filterNot { it.withdrawn }
            .filterNot { it.playerId == context.self.id }
            .filterNot { battle.isDone(it.playerId) }
            .mapNotNull { player -> opponentTotals[player.playerId] }
            .maxOrNull()

        return BattleActionRowState(
            row = before.row,
            available = true,
            currentlyWinning = false,
            woundRisk = false,
            securedForNow = false,
            scoreMargin = benchmark?.let { -it },
            liveThreatMargin = liveThreat?.let { -it },
            ownTotal = 0.0,
            opponentTotals = opponentTotals
        )
    }

    private fun afterOpposingDiceReducedByThree(
        context: DecisionContext,
        before: BattleRowAssessment
    ): BattleActionRowState {
        val battle = requireNotNull(context.battle)
        val rowView = battle.row(before.row)
        val actorId = context.self.id

        val opponentsAfter = before.participatingOpponentTotals.mapValues { (playerId, total) ->
            val playerRow = rowView.forPlayer(playerId)
            val dieReduction = playerRow?.dice.orEmpty().sumOf { die ->
                die.value - (die.value - 3).coerceAtLeast(1)
            }
            total.toDouble() - dieReduction
        }
        val ownAfter = before.ownTotal.toDouble()
        return projectedState(
            context = context,
            row = before.row,
            ownAfter = ownAfter,
            opponentsAfter = opponentsAfter,
            actorId = actorId
        )
    }

    private fun projectedState(
        context: DecisionContext,
        row: StrikeRow,
        ownAfter: Double,
        opponentsAfter: Map<PlayerId, Double>,
        actorId: PlayerId
    ): BattleActionRowState {
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
        val liveThreatTotal = battle.row(row).players
            .asSequence()
            .filterNot { it.withdrawn }
            .filterNot { it.playerId == actorId }
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
            row = row,
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
