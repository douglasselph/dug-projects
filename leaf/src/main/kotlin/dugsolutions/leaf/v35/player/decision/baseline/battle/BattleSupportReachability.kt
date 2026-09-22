package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.abs

/** Cumulative ordinary Support projection for one currently unresolved Strike Row. */
data class BattleRowSupportReachability(
    val row: StrikeRow,
    val currentOwnTotal: Double,
    val beeImprovement: Double,
    val expectedButterflyImprovement: Double,
    val analysis: BattleActionAnalysis<StrikeRow>,
    val minimumMeaningfulVpGain: Int
) {
    val cumulativeImprovement: Double =
        beeImprovement + expectedButterflyImprovement

    val projectedOwnTotal: Double =
        currentOwnTotal + cumulativeImprovement

    val projectedVpGain: Int
        get() = analysis.vpImpact.gain

    val meaningfulResultReachable: Boolean
        get() = projectedVpGain >= minimumMeaningfulVpGain
}

/** Reachability summary across all available, non-secured Strike Rows. */
data class BattleSupportReachabilityAssessment(
    val rows: List<BattleRowSupportReachability>,
    val minimumMeaningfulVpGain: Int
) {
    val meaningfulRows: Set<StrikeRow> =
        rows.filter { it.meaningfulResultReachable }.map { it.row }.toSet()

    val hasMeaningfulPath: Boolean
        get() = meaningfulRows.isNotEmpty()
}

/**
 * Estimates the actor's limited cumulative ordinary Support potential.
 *
 * This is deliberately an optimistic aggregate, not a Support-order search. It
 * combines every currently legal Bee at the actor's current Bee value with an
 * expected keep-better contribution from currently legal Butterflies. Worm,
 * Water, Mulch, and Wisp capacity is intentionally excluded; later checkpoints
 * evaluate those resources through their dedicated spending rules.
 *
 * Butterfly projection never consumes mechanical RNG. Multiple Butterflies may
 * be aimed at the same die, but projected improvement is capped by visible die
 * headroom so a cumulative estimate cannot raise a die above its maximum face.
 */
class BattleSupportReachability(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val actionAnalyzer: BattleActionAnalyzer = BattleActionAnalyzer(
        swingEvaluator = BattleSwingEvaluator(policy)
    )
) {
    operator fun invoke(
        context: DecisionContext,
        capacity: BattleSupportCapacity
    ): BattleSupportReachabilityAssessment {
        val battle = requireNotNull(context.battle) {
            "Battle Support reachability requires a Battle decision context"
        }
        val minimumMeaningfulVpGain = policy.battleMinimumMeaningfulVpGain(context)
        val actorId = context.self.id
        val beeImprovement =
            capacity.beeMoves.toDouble() * context.self.board.beeValue

        val rows = battle.rows.mapNotNull { rowView ->
            val before = rowAssessor(context, rowView.row)
            if (!before.available || before.securedForNow) return@mapNotNull null

            val actor = rowView.forPlayer(actorId) ?: return@mapNotNull null
            val butterflyImprovement = expectedButterflyImprovement(
                dice = actor.dice,
                butterflyMoves = capacity.butterflyMoves
            )
            val cumulativeImprovement = beeImprovement + butterflyImprovement
            val after = projectedAfterImprovement(
                context = context,
                before = before,
                improvement = cumulativeImprovement
            )
            val mode = if (butterflyImprovement > EPSILON) {
                BattleAnalysisMode.EXPECTED
            } else {
                BattleAnalysisMode.DETERMINISTIC
            }
            val analysis = actionAnalyzer(
                context = context,
                candidate = BattleActionRealization(
                    realization = rowView.row,
                    mode = mode,
                    rowChanges = listOf(
                        BattleActionRowChange(
                            before = BattleActionRowState.from(before),
                            after = after
                        )
                    )
                )
            )

            BattleRowSupportReachability(
                row = rowView.row,
                currentOwnTotal = before.ownTotal.toDouble(),
                beeImprovement = beeImprovement,
                expectedButterflyImprovement = butterflyImprovement,
                analysis = analysis,
                minimumMeaningfulVpGain = minimumMeaningfulVpGain
            )
        }

        return BattleSupportReachabilityAssessment(
            rows = rows,
            minimumMeaningfulVpGain = minimumMeaningfulVpGain
        )
    }

    private fun expectedButterflyImprovement(
        dice: List<BattleDieView>,
        butterflyMoves: Int
    ): Double {
        if (butterflyMoves == 0 || dice.isEmpty()) return 0.0

        val opportunities = dice.map { die ->
            ButterflyOpportunity(
                expectedGainPerUse = DieValueHeuristics.expectedKeepBestRerollGain(
                    sides = die.sides,
                    value = die.value
                ),
                remainingHeadroom = (die.sides - die.value).coerceAtLeast(0).toDouble()
            )
        }.toMutableList()

        var expectedGain = 0.0
        repeat(butterflyMoves) {
            val bestIndex = opportunities.indices.maxByOrNull { index ->
                opportunities[index].nextGain
            } ?: return@repeat
            val nextGain = opportunities[bestIndex].nextGain
            if (nextGain <= EPSILON) return expectedGain

            expectedGain += nextGain
            opportunities[bestIndex] = opportunities[bestIndex].spend(nextGain)
        }
        return expectedGain
    }

    private fun projectedAfterImprovement(
        context: DecisionContext,
        before: BattleRowAssessment,
        improvement: Double
    ): BattleActionRowState {
        val ownAfter = before.ownTotal + improvement
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

    private data class ButterflyOpportunity(
        val expectedGainPerUse: Double,
        val remainingHeadroom: Double
    ) {
        val nextGain: Double
            get() = minOf(expectedGainPerUse, remainingHeadroom)

        fun spend(gain: Double): ButterflyOpportunity =
            copy(remainingHeadroom = (remainingHeadroom - gain).coerceAtLeast(0.0))
    }

    private companion object {
        const val WOUND_MARGIN = 5.0
        const val EPSILON = 1e-9
    }
}
